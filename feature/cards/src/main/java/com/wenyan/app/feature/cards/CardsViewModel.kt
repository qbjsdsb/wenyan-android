package com.wenyan.app.feature.cards

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.cards.CardTemplate
import com.wenyan.app.core.data.repository.CardRepository
import com.wenyan.app.core.data.repository.IntervalPreview
import com.wenyan.app.core.data.repository.SchedulingRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.database.entity.CardTemplateType
import com.wenyan.app.core.fsrs.Rating
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 记忆卡片模块 ViewModel。
 *
 * v0.8.8 深度打磨(修复 undo 正确性 + 新增 skip/leech 动作):
 *
 * 1. **P0:undo 多步撤销 bug 修复**
 *    - 原实现 `lastRatingWasAgain: Boolean` 只记录最近一次评分,连续撤销(AGAIN→GOOD→undo→undo)
 *      第二次 undo 丢失 AGAIN 回退,导致 sessionAgainCount 统计错误
 *    - 改用 [ratingHistory] 栈(ArrayDeque<RatingStep>),每次评分入栈,undo 出栈
 *    - 栈记录评分类型 + pointId + 是否触发了 FSRS 调度,undo 时精确回退三项统计
 *
 * 2. **P0:undo 回退 ratedPointIds**
 *    - 原实现 undo 不回退 sibling 去重状态(注释"尽力而为"),导致撤销首张 sibling 卡后
 *      重新评分不触发 FSRS(调度被"吞")
 *    - 现在 RatingStep 记录 triggeredSchedule,undo 时从 ratedPointIds 移除,重新评分可再触发
 *
 * 3. **P1:跳过功能**
 *    - 新增 [skipCard]():不评分推进到下一张,不影响 FSRS 和会话统计
 *    - 适用场景:卡片内容有误/临时不想答,避免乱评污染 FSRS 数据
 *    - skip 也入栈 ratingHistory(rating=null),支持 undo 回退到被跳过的卡
 *
 * 4. **P1:Leech 警告携带 pointId**
 *    - [leechWarning] 从 String? 改为 [LeechWarning]?,携带 pointId 供 UI 跳转知识点详情
 *    - 用户可点击"查看知识点"直接跳到 detail 页处理(拆卡/重写)
 *
 * v0.8.6-v0.8.7 已完成(保留):
 * - 评分按钮显示预期间隔(参考 Anki "10m / 4d / 8d")
 * - Leech 检测(failCount >= 8 提示)
 * - 会话恢复(进程被杀后不错位)
 * - 会话时长统计 + "返回学习"退出按钮
 * - FSRS 调度粒度修复(sibling 去重)
 * - 会话内 cards 冻结(避免 Flow 重新 emit 错位)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CardsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val cardRepository: CardRepository,
    private val schedulingRepository: SchedulingRepository,
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val studyProgressRepository: com.wenyan.app.core.data.repository.StudyProgressRepository,
) : ViewModel() {

    // 翻转状态（UI 交互层，持久化到 SavedStateHandle）
    private val _isFlipped = savedStateHandle.getStateFlow("isFlipped", false)

    // 当前卡片索引（UI 交互层，持久化到 SavedStateHandle）
    private val _currentIndex = savedStateHandle.getStateFlow("currentIndex", 0)

    // 错误提示
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Leech 警告(v0.8.6 创建,v0.8.8 携带 pointId):某知识点 failCount 达阈值时弹提示 */
    private val _leechWarning = MutableStateFlow<LeechWarning?>(null)
    val leechWarning: StateFlow<LeechWarning?> = _leechWarning.asStateFlow()

    /**
     * 当前卡片是否为"已评分 sibling 卡"(v0.8.9 新增,修复 P1-2)。
     *
     * - true:当前卡 [CardItem.pointId] 已在 [ratedPointIds] 中(同知识点的兄弟卡已评分过)
     * - UI 据此隐藏 [currentPreviews] 的预期间隔显示,避免误导用户
     *   (sibling 卡评分不会触发 FSRS 调度,显示"GOOD→6天"是误导)
     * - UI 改为显示"已调度(同知识点首卡已评分)"提示
     *
     * 用 derivedStateOf 不持久化,因为 [ratedPointIds] 本身不持久化。
     */
    val isSiblingAlreadyRated: StateFlow<Boolean> = _uiState
        .map { it.currentCard?.pointId in ratedPointIds }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    /** 重试触发器 */
    private val _retryTrigger = MutableStateFlow(0)

    // v0.8.5：会话内状态（非持久化，进程被杀后重置）
    /** 会话内冻结的卡片列表，避免 Flow 重新 emit 导致 currentIndex 错位 */
    @Volatile
    private var sessionCards: List<CardItem>? = null

    /** 已评分的 pointId 集合（sibling 去重，同 pointId 仅第一次触发 FSRS 调度） */
    private val ratedPointIds = mutableSetOf<String>()

    /**
     * 会话内已复习张数(v0.8.9 持久化到 SavedStateHandle,修复 P2-2)。
     *
     * 含 sibling 卡。进程被杀恢复后保留统计,完成态展示准确。
     */
    private val _sessionReviewedCount = savedStateHandle.getStateFlow("sessionReviewedCount", 0)
    val sessionReviewedCount: StateFlow<Int> = _sessionReviewedCount.asStateFlow()

    /**
     * 会话内评 AGAIN 的张数(v0.8.9 持久化,修复 P2-2)。
     *
     * 用于完成态"掌握率"计算。进程被杀恢复后保留统计。
     */
    private val _sessionAgainCount = savedStateHandle.getStateFlow("sessionAgainCount", 0)
    val sessionAgainCount: StateFlow<Int> = _sessionAgainCount.asStateFlow()

    /**
     * 评分历史栈(v0.8.8 新增,替代 v0.8.7 的 `lastRatingWasAgain: Boolean`)。
     *
     * 每次评分/skip 入栈一个 [RatingStep],undo 时出栈并精确回退:
     * - sessionReviewedCount(评分才 +1,skip 不 +1)
     * - sessionAgainCount(AGAIN 评分才 +1)
     * - ratedPointIds(triggeredSchedule=true 时移除,让重新评分能再触发 FSRS)
     *
     * 原实现 `lastRatingWasAgain` 是单个布尔值,连续撤销(AGAIN→GOOD→undo→undo)时
     * 第二次 undo 丢失 AGAIN 回退。栈结构保证多步撤销每步都能精确回退。
     */
    private val ratingHistory = ArrayDeque<RatingStep>()

    /**
     * 当前卡片 4 档评分的预期间隔(v0.8.6 新增)。
     *
     * 进入新卡片时异步加载,UI 据此在评分按钮上显示"1分钟 / 6天 / 12天"。
     * 参考 Anki "10m / 4d / 8d" 设计,让用户在评分前理解每个评分的后果。
     *
     * 加载失败或 pointId 为空时为空 Map,UI 不显示预览(降级为纯文字按钮)。
     */
    private val _currentPreviews = MutableStateFlow<Map<Rating, IntervalPreview>>(emptyMap())
    val currentPreviews: StateFlow<Map<Rating, IntervalPreview>> = _currentPreviews.asStateFlow()

    /**
     * 会话开始时间戳(v0.8.6 新增,v0.8.9 持久化到 SavedStateHandle 修复 P2-1)。
     *
     * 用 System.currentTimeMillis() 而非 clockGuard,因为:
     * - 仅用于展示,不参与 FSRS 调度计算
     * - clockGuard 检测回拨是为了保护 FSRS,会话时长展示用真实墙钟更直观
     *
     * v0.8.9:持久化到 SavedStateHandle,进程被杀恢复后会话时长统计准确。
     * 默认值为当前时间(首次进入时初始化),恢复时使用 SavedStateHandle 中的旧值。
     */
    private val _sessionStartTime = savedStateHandle.getStateFlow("sessionStartTime", System.currentTimeMillis())

    private val _uiState = MutableStateFlow<CardsUiState>(CardsUiState(isLoading = true))
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _retryTrigger
                .flatMapLatest {
                    combine(
                        cardRepository.getCardsForReview(),
                        _isFlipped,
                        _currentIndex,
                    ) { cards, isFlipped, currentIndex ->
                        // v0.8.5 P0：会话内冻结 cards，避免 Flow 重新 emit 导致错位
                        // v0.8.6 P0:进程被杀后恢复(sessionLoaded=true 但 sessionCards=null)
                        //   此时 currentIndex 可能 >0 但 sessionCards 已丢失,重置避免错位
                        val isFirstLoad = savedStateHandle.get<Boolean>(KEY_SESSION_LOADED) != true
                        val effectiveCards = if (isFirstLoad) {
                            // 首次加载:重新生成 sessionCards
                            savedStateHandle[KEY_SESSION_LOADED] = true
                            val newCards = cards.mapIndexed { index, card -> card.toUiItem(index) }
                            sessionCards = newCards
                            newCards
                        } else if (sessionCards == null) {
                            // 进程被杀后恢复:sessionLoaded=true 但 sessionCards=null
                            // 重置 currentIndex 避免错位,重新加载 cards
                            if (currentIndex > 0) {
                                savedStateHandle["currentIndex"] = 0
                                savedStateHandle["isFlipped"] = false
                            }
                            val newCards = cards.mapIndexed { index, card -> card.toUiItem(index) }
                            sessionCards = newCards
                            newCards
                        } else {
                            // 正常评分中:用冻结的 sessionCards
                            sessionCards!!
                        }

                        val isFinished = effectiveCards.isNotEmpty() && currentIndex >= effectiveCards.size
                        val safeIndex = if (effectiveCards.isEmpty()) {
                            0
                        } else {
                            currentIndex.coerceIn(0, effectiveCards.size - 1)
                        }
                        CardsUiState(
                            isLoading = false,
                            cards = effectiveCards,
                            currentIndex = safeIndex,
                            isFlipped = isFlipped,
                            isFinished = isFinished,
                        )
                    }
                }
                .catch { e ->
                    emit(CardsUiState(error = e.message ?: "加载失败"))
                }
                .collect { _uiState.value = it }
        }

        // v0.8.6 P0:监听 currentIndex 变化,异步加载当前卡的预期间隔
        // 只在 cards 非空 + currentIndex 有效时加载,避免无效查询
        viewModelScope.launch {
            _uiState
                .map { it.currentCard to it.currentIndex }
                .distinctUntilChanged()
                .collect { (card, _) ->
                    if (card == null || card.pointId.isBlank()) {
                        _currentPreviews.value = emptyMap()
                        return@collect
                    }
                    val templateType = try {
                        CardTemplateType.valueOf(card.cardType)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                    if (templateType == null) {
                        _currentPreviews.value = emptyMap()
                        return@collect
                    }
                    try {
                        _currentPreviews.value = schedulingRepository.previewIntervals(
                            pointId = card.pointId,
                            cardType = templateType,
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // 预览失败不阻塞主流程,UI 降级为无预览按钮
                        _currentPreviews.value = emptyMap()
                    }
                }
        }
    }

    /** 翻转当前卡片 */
    fun flipCard() {
        savedStateHandle["isFlipped"] = !_isFlipped.value
    }

    /**
     * FSRS 评分（Again/Hard/Good/Easy），推进到下一张卡并异步完成调度回写。
     *
     * v0.8.5 P0 修复（sibling 去重）：
     * - 同一知识点的多张卡（如"建安风骨-时代"+"建安风骨-代表作家"）共享同一 pointId。
     * - 仅第一次评分触发 schedulingRepository.rateCard（FSRS 调度），
     *   后续同 pointId 的卡片仅推进 UI + 记录错题（若 AGAIN）。
     * - 参考 Anki sibling burying：同 note 的兄弟卡同日仅复习一张，
     *   避免重复评分导致 stability 虚高。
     *
     * v0.8.5 P1：会话统计
     * - 每次评分累加 sessionReviewedCount
     * - AGAIN 评分累加 sessionAgainCount
     *
     * v0.8.6 P0:Leech 检测
     * - rateCard 后检查返回的 failCount,达到阈值时弹 leechWarning
     *
     * v0.8.8:评分入栈 [ratingHistory],undo 时精确回退
     *
     * v0.8.9 P1 修复(报告 P1-4):
     * - recordStudySession 移到独立 try-catch,失败不影响 Leech 检测
     * - Leech 检测紧跟 rateCard 成功后,即使 studyProgress 失败也能弹出警告
     * - 错误消息区分来源:调度失败 vs 学习进度记录失败 vs 错题记录失败(报告 P2-9)
     */
    fun rateCard(rating: CardRating) {
        val current = uiState.value.currentCard ?: return
        val pointId = current.pointId
        val cardTypeStr = current.cardType

        // 先推进 UI（立即响应）
        savedStateHandle["isFlipped"] = false
        savedStateHandle["currentIndex"] = _currentIndex.value + 1

        // 会话统计
        savedStateHandle["sessionReviewedCount"] = _sessionReviewedCount.value + 1
        if (rating == CardRating.AGAIN) {
            savedStateHandle["sessionAgainCount"] = _sessionAgainCount.value + 1
        }

        // 无 pointId 的卡片仅推进索引 + 入栈(undo 需回退 sessionReviewedCount)
        if (pointId.isBlank()) {
            ratingHistory.addLast(RatingStep(rating = rating, pointId = "", triggeredSchedule = false))
            return
        }

        // v0.8.5 P0：sibling 去重 — 同 pointId 仅第一次评分触发 FSRS 调度
        val shouldSchedule = pointId !in ratedPointIds
        if (shouldSchedule) {
            ratedPointIds.add(pointId)
        }

        // v0.8.8:入栈评分历史,undo 时据此回退 sessionReviewedCount/sessionAgainCount/ratedPointIds
        ratingHistory.addLast(RatingStep(rating = rating, pointId = pointId, triggeredSchedule = shouldSchedule))

        viewModelScope.launch {
            val fsrsRating = when (rating) {
                CardRating.AGAIN -> Rating.AGAIN
                CardRating.HARD -> Rating.HARD
                CardRating.GOOD -> Rating.GOOD
                CardRating.EASY -> Rating.EASY
            }

            // 仅第一次评分触发 FSRS 调度
            if (shouldSchedule) {
                val templateType = try {
                    CardTemplateType.valueOf(cardTypeStr)
                } catch (e: IllegalArgumentException) {
                    null
                }
                if (templateType != null) {
                    // v0.8.9 P1-4:把 Leech 检测和 studyProgress 解耦
                    // 原实现把 recordStudySession 放在 rateCard 与 Leech 检测之间,
                    // 若 studyProgress 抛异常会跳过 Leech 检测,用户该看到警告却看不到
                    val updated = try {
                        schedulingRepository.rateCard(pointId, fsrsRating, templateType)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _errorMessage.value = "评分调度失败：${e.message ?: "未知错误"}"
                        null
                    }

                    // Leech 检测(独立于 studyProgress,确保一定能执行)
                    // v0.8.8:携带 pointId 供 UI 跳转知识点详情
                    if (updated != null && updated.failCount >= LEECH_THRESHOLD) {
                        _leechWarning.value = LeechWarning(
                            message = buildString {
                                append("这张卡片已复习 ${updated.failCount} 次仍记不住。")
                                append("建议拆分为更小的卡片,或联系 AI 助手辅助理解。")
                            },
                            pointId = pointId,
                        )
                    }

                    // 学习进度记录(独立 try-catch,失败不影响 Leech 检测/错题记录)
                    try {
                        studyProgressRepository.recordStudySession(pointId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // v0.8.9 P2-9:文案明确区分学习进度记录失败(非调度失败)
                        _errorMessage.value = "学习进度记录失败：${e.message ?: "未知错误"}"
                    }
                }
            }

            // AGAIN 评分时记录错题（无论是否触发调度，错题记录独立）
            if (fsrsRating == Rating.AGAIN) {
                try {
                    wrongAnswerRepository.recordWrongAnswer(
                        pointId = pointId,
                        examQuestionId = null,
                        userAnswer = "（评分AGAIN：未回忆）",
                        correctAnswer = current.back,
                        source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _errorMessage.value = "错题记录失败：${e.message ?: "未知错误"}"
                }
            }
        }
    }

    /**
     * 跳过当前卡片(v0.8.8 新增)。
     *
     * 不评分推进到下一张,不影响 FSRS 调度和会话统计(sessionReviewedCount/sessionAgainCount 不变)。
     *
     * 适用场景:
     * - 卡片内容有误或无法理解,不想乱评污染 FSRS 数据
     * - 临时不想答某张卡,想先看后面的
     *
     * skip 也入栈 [ratingHistory](rating=null),支持 undo 回退到被跳过的卡。
     * 与 [undo] 配合:skip 后 undo 回到被跳过的卡,可正常评分。
     */
    fun skipCard() {
        val current = uiState.value.currentCard ?: return
        savedStateHandle["isFlipped"] = false
        savedStateHandle["currentIndex"] = _currentIndex.value + 1
        // skip 不累加 sessionReviewedCount/sessionAgainCount,但入栈供 undo 回退索引
        ratingHistory.addLast(RatingStep(rating = null, pointId = current.pointId, triggeredSchedule = false))
    }

    /**
     * 撤销上一张卡片（仅 UI 回退，不回滚 FSRS 调度）。
     *
     * v0.8.5 P1 新增：
     * - 参考 Anki 的 Z 键撤销，但简化为仅回退 UI 索引。
     * - FSRS 调度不可逆（已写入 memo_records + review_logs），
     *   撤销仅让用户回看上一张卡片内容。
     * - 边界：currentIndex == 0 时无操作。
     *
     * v0.8.8 重写:用 [ratingHistory] 栈精确回退三项统计:
     * - sessionReviewedCount(评分才 -1,skip 不 -1)
     * - sessionAgainCount(AGAIN 评分才 -1)
     * - ratedPointIds(triggeredSchedule=true 时移除 pointId,让重新评分能再触发 FSRS)
     *
     * 修复 v0.8.7 的 bug:原 `lastRatingWasAgain: Boolean` 只记录最近一次评分,
     * 连续撤销(AGAIN→GOOD→undo GOOD→undo AGAIN)时第二次 undo 丢失 AGAIN 回退。
     */
    fun undo() {
        if (_currentIndex.value <= 0) return
        // v0.8.8:从栈顶弹出最近一步,据此精确回退
        val step = ratingHistory.removeLastOrNull() ?: return
        savedStateHandle["currentIndex"] = _currentIndex.value - 1
        savedStateHandle["isFlipped"] = false
        // skip(rating=null)不影响统计,仅回退索引
        if (step.rating != null) {
            // v0.8.9:SavedStateHandle-backed StateFlow,通过 savedStateHandle 写入
            savedStateHandle["sessionReviewedCount"] =
                (_sessionReviewedCount.value - 1).coerceAtLeast(0)
            if (step.rating == CardRating.AGAIN) {
                savedStateHandle["sessionAgainCount"] =
                    (_sessionAgainCount.value - 1).coerceAtLeast(0)
            }
        }
        // 回退 sibling 去重状态:若此步触发了 FSRS 调度,移除 pointId 让重新评分能再触发
        if (step.triggeredSchedule && step.pointId.isNotBlank()) {
            ratedPointIds.remove(step.pointId)
        }
    }

    /** 清除错误提示 */
    fun clearError() {
        _errorMessage.value = null
    }

    /** 清除 Leech 警告(v0.8.6) */
    fun clearLeechWarning() {
        _leechWarning.value = null
    }

    /**
     * 获取会话时长(分钟,v0.8.6 新增)。
     *
     * 用于完成态显示"本次用时 X 分钟"。基于 [_sessionStartTime] 计算,
     * 至少 1 分钟(避免显示 0 分钟让用户觉得没学到东西)。
     *
     * v0.8.9:[_sessionStartTime] 持久化到 SavedStateHandle,进程被杀恢复后
     * 会话时长统计仍准确(基于原始开始时间)。
     */
    fun getSessionDurationMinutes(): Int {
        val millis = System.currentTimeMillis() - _sessionStartTime.value
        return (millis / 60_000L).toInt().coerceAtLeast(1)
    }

    /**
     * 重试加载。
     *
     * v0.8.5：重置会话状态（sessionCards + ratedPointIds + 统计），
     * 让用户从全新队列开始。
     *
     * v0.8.6:重置 sessionStartTime 和 leechWarning。
     *
     * v0.8.8:清空 [ratingHistory] 栈。
     *
     * v0.8.9:SavedStateHandle-backed StateFlow 通过 savedStateHandle 写入。
     */
    fun retry() {
        sessionCards = null
        ratedPointIds.clear()
        ratingHistory.clear()
        savedStateHandle["sessionReviewedCount"] = 0
        savedStateHandle["sessionAgainCount"] = 0
        _leechWarning.value = null
        _currentPreviews.value = emptyMap()
        savedStateHandle["sessionStartTime"] = System.currentTimeMillis()
        savedStateHandle["currentIndex"] = 0
        savedStateHandle["isFlipped"] = false
        savedStateHandle[KEY_SESSION_LOADED] = false
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        _retryTrigger.value++
    }

    /** 将 [CardTemplate] 映射为 UI 层 [CardItem] */
    private fun CardTemplate.toUiItem(index: Int): CardItem = CardItem(
        // v0.8.8:稳定 ID(替代 index-based),基于 pointId+类型+内容哈希
        // sibling 卡(同 pointId 同类型)靠 front 内容区分,保证 ID 唯一且稳定
        id = buildString {
            append(templateType.name)
            append('_')
            if (pointId.isNotBlank()) append(pointId) else append(front.hashCode())
            append('_')
            append(front.take(16).hashCode())
        },
        front = front,
        back = back,
        cardType = templateType.name,
        pointId = pointId,
        template = this,
    )

    companion object {
        /** SavedStateHandle key:会话是否已加载(用于检测进程被杀) */
        private const val KEY_SESSION_LOADED = "sessionLoaded"

        /**
         * Leech 阈值(参考 Anki 默认 8 次)。
         *
         * 当某知识点 failCount >= 此值时,提示用户"这张卡复习 N 次仍记不住"。
         * Anki 默认 8 次,文研 App 沿用此值。
         */
        private const val LEECH_THRESHOLD = 8
    }
}

// 卡片 UI 状态
data class CardsUiState(
    val isLoading: Boolean = false,
    val cards: List<CardItem> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    /** 牌组是否已完成（currentIndex >= cards.size 时为 true） */
    val isFinished: Boolean = false,
    /** 加载失败时的错误信息 */
    val error: String? = null,
) {
    val currentCard: CardItem? get() = cards.getOrNull(currentIndex)
}

// 卡片项（UI 层模型，与 core:data 的 CardTemplate 解耦）
@Immutable
data class CardItem(
    val id: String,
    val front: String,
    val back: String,
    val cardType: String,
    /** 关联知识点 ID（用于 FSRS 调度回写） */
    val pointId: String = "",
    val template: CardTemplate? = null,
)

// FSRS 评分等级
enum class CardRating {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}

/**
 * 评分历史步骤(v0.8.8 新增)。
 *
 * 每次 [CardsViewModel.rateCard] 或 [CardsViewModel.skipCard] 入栈一个 [RatingStep],
 * [CardsViewModel.undo] 时出栈并据此精确回退统计。
 *
 * @property rating 评分类型(null 表示 skip,跳过不评分)
 * @property pointId 关联知识点 ID(空字符串表示无 pointId 的卡)
 * @property triggeredSchedule 此步是否触发了 FSRS 调度(首次评分同 pointId 时为 true)
 *                             undo 时据此决定是否从 ratedPointIds 移除,让重新评分能再触发
 */
private data class RatingStep(
    val rating: CardRating?,
    val pointId: String,
    val triggeredSchedule: Boolean,
)

/**
 * Leech 警告(v0.8.8 新增,替代 v0.8.6 的纯 String)。
 *
 * 携带 [pointId] 供 UI 跳转知识点详情,让用户可直接处理(拆卡/重写)。
 *
 * @property message 警告文案(如"这张卡片已复习 8 次仍记不住...")
 * @property pointId 触发 Leech 的知识点 ID,UI 据此导航到 detail 页
 */
data class LeechWarning(
    val message: String,
    val pointId: String,
)
