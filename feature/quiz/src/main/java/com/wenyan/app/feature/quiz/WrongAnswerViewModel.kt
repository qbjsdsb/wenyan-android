package com.wenyan.app.feature.quiz

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.common.util.friendlyErrorMessage
import com.wenyan.app.core.data.repository.ClockGuard
import com.wenyan.app.core.data.repository.SchedulingRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.database.entity.WrongAnswerWithDetails
import com.wenyan.app.core.fsrs.Rating
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 错题本 ViewModel(NF-PP5 Wave 3.2)。
 *
 * 功能:
 * - 观察 [WrongAnswerRepository.observeUnresolved] / [WrongAnswerRepository.observeAll] /
 *   [WrongAnswerRepository.observeDueWrongAnswers],通过 [filter] 切换
 * - [markResolved]:标记错题为已解决(从"未解决"列表移除)
 * - [deleteById]:永久删除错题记录
 * - [rateWrongAnswer]（v0.9.4 新增）:FSRS 评分调度,更新错题的 sched_* 字段
 *
 * v0.9.4 新增:
 * - [WrongAnswerFilter.DUE] 过滤模式:仅显示 sched_next_review_at <= now 的待复习错题
 * - [rateWrongAnswer] 方法:调用 [SchedulingRepository.rateWrongAnswer] 进行 FSRS 调度
 * - [WrongAnswerItem] 增加 sched_* 字段:展示调度状态和下次复习时间
 *
 * v0.8.21 修复 B1+M1+M2(对照 feature/knowledge v0.8.17 修复模式):
 * - **B1**:原 `catch` 在 `flatMapLatest` 外层,异常触发后整流终止,
 *   `retry()` 通过切换 filter 再切回触发重订阅的 hack 失效(外层流已 cancel)。
 *   现引入 [retryTrigger],catch 移入 flatMapLatest 内部,仅终止 inner Flow,
 *   外层流仍由 retryTrigger 驱动,retry() 真正生效。
 * - **M1**:catch 内加 `Log.e` 输出完整堆栈,生产排查不丢上下文
 *   (对照 feature/cards v0.8.14 P1-7 修复)。
 * - **M2**:catch 用 raw `e.message` 改为 [friendlyErrorMessage],
 *   统一中文友好提示(网络/数据库/超时/未知),
 *   与 feature/knowledge + feature/cards 错误处理一致。
 * - 重构为 MutableStateFlow + collect 模式(对照 KnowledgeViewModel v0.8.13 P1-4),
 *   retry() 立即设置 isLoading=true 让 UI 即时反馈,保留 filter 不清空。
 *
 * @property wrongAnswerRepository 错题仓库
 * @property schedulingRepository FSRS 调度仓库（v0.9.4 新增,用于错题评分调度）
 * @property clockGuard 时钟守卫（v0.9.5 follow-up #1 新增,DUE 过滤时间源与评分调度对齐）
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WrongAnswerViewModel @Inject constructor(
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val schedulingRepository: SchedulingRepository,
    private val clockGuard: ClockGuard,
) : ViewModel() {

    /** 当前过滤模式(默认未解决,这是用户最常看的视图) */
    private val _filter = MutableStateFlow(WrongAnswerFilter.UNRESOLVED)
    val filter: StateFlow<WrongAnswerFilter> = _filter.asStateFlow()

    /** 错误提示(markResolved / deleteById 失败时设置) */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 重试触发器(v0.8.21 新增,替代 v0.8.4 的"切换 filter 再切回"hack)。
     *
     * 自增整数,每次 [retry] 时 +1,触发 [uiState] 的 flatMapLatest 重新订阅。
     */
    private val _retryTrigger = MutableStateFlow(0)

    /**
     * 评分防重入锁（v0.9.22 P2-3 新增）。
     *
     * 背景：DUE 模式下评分后 DB 流刷新前卡片仍停留在原地，用户快速连点两个
     * 评分按钮会对同一错题调用两次 rateWrongAnswer，导致 FSRS 重复调度、
     * 间隔异常。CardsScreen 评分后立即推进索引天然防重，错题本 DUE 模式无此保护。
     *
     * viewModelScope 默认 Main dispatcher，isRating 读写均在主线程串行执行，
     * 无需 Mutex/@Volatile。
     */
    private var isRating = false

    /**
     * 错题列表 UI 状态(v0.8.21 重构为 MutableStateFlow + collect)。
     *
     * 合并 [retryTrigger] + [filter] 触发 [flatMapLatest] 订阅对应的 observe 流。
     * [filter] 切换时通过 [flatMapLatest] 自动取消上一个订阅;
     * [retry] 时通过 [_retryTrigger] 重新触发订阅。
     *
     * v0.8.21 修复 B1+M1+M2:
     * - catch 移入 flatMapLatest 内部,仅终止本次 inner Flow,
     *   外层仍由 retryTrigger 驱动,支持 retry() 重新触发加载。
     * - 加 Log.e + friendlyErrorMessage,与 feature/knowledge + feature/cards 一致。
     * - catch 时保留已有 items,避免数据库偶发异常导致列表瞬间清空
     *   (与 KnowledgeViewModel catch 保留 knowledgePoints 策略一致)。
     */
    private val _uiState = MutableStateFlow<WrongAnswerUiState>(WrongAnswerUiState(isLoading = true))
    val uiState: StateFlow<WrongAnswerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // v0.8.21 修复 B1:catch 必须在 flatMapLatest 内部,仅终止本次 inner Flow,
            // 外层 Flow 仍由 retryTrigger + filter 驱动,支持 retry() 重新触发加载。
            // 原实现 catch 在 flatMapLatest 外层,异常触发后整流终止,
            // retry() 切换 filter 触发的重订阅无法被任何 collector 接收。
            // 同 feature/knowledge v0.8.17 + feature/cards v0.8.20 修复模式。
            combine(_retryTrigger, _filter) { _, currentFilter -> currentFilter }
                .flatMapLatest { currentFilter ->
                    val flow = when (currentFilter) {
                        WrongAnswerFilter.UNRESOLVED -> wrongAnswerRepository.observeUnresolved()
                        WrongAnswerFilter.ALL -> wrongAnswerRepository.observeAll()
                        // v0.9.5 follow-up #1: 用 ClockGuard.effectiveNowMillis() 替代
                        // System.currentTimeMillis(),与 SchedulingRepository.rateWrongAnswer
                        // 时间源对齐。时钟回拨时两者用同一 lastKnown,避免 DUE 列表与
                        // 评分调度的 now 不一致导致错题评分后立即又出现在 DUE 列表。
                        WrongAnswerFilter.DUE -> wrongAnswerRepository.observeDueWrongAnswers(
                            clockGuard.effectiveNowMillis(),
                        )
                    }
                    flow.map { items -> WrongAnswerUiState(items = items.map { it.toUiItem() }) }
                        // v0.8.21 修复 B1+M1+M2:catch 移入 flatMapLatest 内部,
                        // 仅终止本次 inner Flow,外层仍由 retryTrigger + filter 驱动。
                        // 加 Log.e + friendlyErrorMessage,与 feature/knowledge + feature/cards 一致。
                        // catch 时保留已有 items,避免列表瞬间清空。
                        .catch { e ->
                            Timber.e(e, "loadWrongAnswers failed: filter=$currentFilter")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = friendlyErrorMessage(e),
                            )
                        }
                }
                .collect { _uiState.value = it }
        }
    }

    /** 切换过滤模式 */
    fun setFilter(newFilter: WrongAnswerFilter) {
        _filter.value = newFilter
    }

    /**
     * 标记错题为已解决。
     *
     * 失败时设置 errorMessage,不阻塞 UI(用户可重试)。
     */
    fun markResolved(id: String) {
        viewModelScope.launch {
            try {
                wrongAnswerRepository.markResolved(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "标记失败：${friendlyErrorMessage(e)}"
            }
        }
    }

    /**
     * 永久删除错题记录。
     *
     * 失败时设置 errorMessage,不阻塞 UI。
     */
    fun deleteById(id: String) {
        viewModelScope.launch {
            try {
                wrongAnswerRepository.deleteById(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "删除失败：${friendlyErrorMessage(e)}"
            }
        }
    }

    /** 清除错误提示 */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * FSRS 评分调度（v0.9.4 新增）。
     *
     * 调用 [SchedulingRepository.rateWrongAnswer] 更新错题的 sched_* 字段。
     * 评分后错题的 sched_next_review_at 更新,从 DUE 列表移除（若已不再到期）。
     *
     * v0.9.22 P2-3：增加防重入锁 [isRating]。DUE 模式下 DB 流刷新前卡片停留原地，
     * 连点两个评分按钮会对同一错题重复调用本方法（FSRS 重复调度）。锁在
     * viewModelScope.launch 期间保持，finally 中释放（异常也不卡死）。
     *
     * 失败时设置 errorMessage,不阻塞 UI。
     *
     * @param id     错题 ID
     * @param rating FSRS 评分（AGAIN/HARD/GOOD/EASY）
     */
    fun rateWrongAnswer(id: String, rating: Rating) {
        if (isRating) return
        isRating = true
        viewModelScope.launch {
            try {
                schedulingRepository.rateWrongAnswer(id, rating)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "rateWrongAnswer failed: id=$id, rating=$rating")
                _errorMessage.value = "评分失败：${friendlyErrorMessage(e)}"
            } finally {
                isRating = false
            }
        }
    }

    /**
     * 重试加载(v0.8.21 重构)。
     *
     * 立即设置 isLoading=true 并清空 error,保留 filter 不变,让 UI 立即显示 loading 反馈;
     * 再增加 [_retryTrigger] 触发数据流重新订阅。
     *
     * 原实现(v0.8.4)通过切换 filter 再切回触发重订阅,但 B1 bug 下 catch 已终止整流,
     * 此 hack 失效。重构后 catch 移入 flatMapLatest 内部,retry() 通过 retryTrigger
     * 真正生效(同 feature/knowledge v0.8.13 P1-4 模式)。
     */
    fun retry() {
        _errorMessage.value = null
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        _retryTrigger.value++
    }

    /** 将 [WrongAnswerWithDetails] 转换为 UI 列表项（v0.9.2：含题目文本；v0.9.4：含调度字段） */
    private fun WrongAnswerWithDetails.toUiItem(): WrongAnswerItem {
        val entity = wrongAnswer
        return WrongAnswerItem(
            id = entity.id,
            pointId = entity.pointId,
            examQuestionId = entity.examQuestionId,
            questionTitle = questionTitle,
            userAnswer = entity.userAnswer,
            correctAnswer = entity.correctAnswer,
            source = entity.source,
            wrongCount = entity.wrongCount,
            lastWrongAt = entity.lastWrongAt,
            isResolved = entity.resolvedAt != null,
            createdAt = entity.createdAt,
            // v0.9.4：FSRS 调度字段
            schedState = entity.schedState,
            schedNextReviewAt = entity.schedNextReviewAt,
            schedLastReviewAt = entity.schedLastReviewAt,
            schedReps = entity.schedReps,
            schedLapses = entity.schedLapses,
        )
    }
}

/** 错题过滤模式 */
enum class WrongAnswerFilter {
    /** 未解决(resolvedAt IS NULL) */
    UNRESOLVED,

    /** 全部(含已解决) */
    ALL,

    /** 待复习(sched_next_review_at <= now 且 resolvedAt IS NULL)（v0.9.4 新增） */
    DUE,
}

/** 错题本 UI 状态 */
data class WrongAnswerUiState(
    val isLoading: Boolean = false,
    val items: List<WrongAnswerItem> = emptyList(),
    /** v0.8.4 新增：加载失败错误信息（原 catch emit emptyList 把失败伪装为空状态） */
    val error: String? = null,
)

/**
 * 错题列表项(与 [WrongAnswerEntity] 解耦的 UI 层模型)。
 *
 * @property questionTitle 题目文本(v0.9.2 新增,JOIN 关联表获取:
 *   卡片来源=知识点 title,真题来源=真题 content,理论不应为 null 但兜底处理)
 * @property isResolved 是否已解决(从 resolvedAt 派生)
 * @property schedState FSRS 调度状态（v0.9.4 新增：NEW/LEARNING/REVIEW/RELEARNING）
 * @property schedNextReviewAt 下次复习时间戳（v0.9.4 新增，0=立即到期）
 * @property schedLastReviewAt 上次复习时间戳（v0.9.4 新增，0=从未复习）
 * @property schedReps 总复习次数（v0.9.4 新增）
 * @property schedLapses 遗忘次数（v0.9.4 新增）
 */
@Immutable
data class WrongAnswerItem(
    val id: String,
    val pointId: String?,
    val examQuestionId: String?,
    val questionTitle: String?,
    val userAnswer: String,
    val correctAnswer: String?,
    val source: String,
    val wrongCount: Int,
    val lastWrongAt: Long,
    val isResolved: Boolean,
    val createdAt: Long,
    // v0.9.4：FSRS 调度字段
    val schedState: String = "NEW",
    val schedNextReviewAt: Long = 0L,
    val schedLastReviewAt: Long = 0L,
    val schedReps: Int = 0,
    val schedLapses: Int = 0,
)
