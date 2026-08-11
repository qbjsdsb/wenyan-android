package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.KnowledgePointDetail
import com.wenyan.app.core.data.repository.KnowledgeRepository
import com.wenyan.app.core.data.repository.KnowledgeProgressSource
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 知识点详情 ViewModel（阶段5新增，Spec C1.27 多教材对照 + C7.2 来源溯源）。
 *
 * 通过 [SavedStateHandle] 从导航参数获取 pointId，
 * 观察 [KnowledgeRepository.observeKnowledgePointDetail] 获取详情。
 *
 * v0.8.19 P1-UI-6 修复(NF-L7):
 * - 原 `val pointId: String = savedStateHandle["pointId"] ?: ""` 是一次性读取,
 *   后续不观察 SavedStateHandle 变化。同路由实例下 pointId 变化不更新。
 * - 现改为 `savedStateHandle.getStateFlow("pointId", "")`,在 flatMapLatest 中订阅,
 *   pointId 变化时自动重新订阅详情 Flow。当前导航会为详情路径保留独立的 back stack entry，
 *   该观察仍提升了状态恢复和未来 SharedViewModel 复用时的健壮性。
 *
 * v0.8.19 P1-REL-1 新增错题关联:
 * - 注入 [WrongAnswerRepository],combine 到 [uiState]
 * - UI 展示该知识点的未解决错题列表(wrongCount / lastWrongAt / userAnswer)
 * - 用户可在详情页直接看到"这题我错过几次",无需跳转到错题本
 *
 * UI 状态含：
 * - 知识点主信息（title/summary/coreConclusion/studyText）
 * - 来源溯源列表（data_sources 表）
 * - 关联/对比/延伸知识点标题
 * - 错题记录列表(v0.8.19 新增,仅未解决的错题)
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class KnowledgePointDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val knowledgeRepository: KnowledgeRepository,
    private val knowledgeProgressRepository: KnowledgeProgressSource,
    private val wrongAnswerRepository: WrongAnswerRepository,
) : ViewModel() {

    private val revealedLayersKey = "revealed_study_layers"
    internal val revealedStudyLayers: StateFlow<Set<KnowledgeStudyLayer>> =
        savedStateHandle.getStateFlow<ArrayList<String>>(revealedLayersKey, arrayListOf())
            .map(::decodeStudyLayers)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    internal fun revealStudyLayer(layer: KnowledgeStudyLayer) {
        val current = savedStateHandle.get<ArrayList<String>>(revealedLayersKey).orEmpty()
        if (layer.name !in current) {
            savedStateHandle[revealedLayersKey] = ArrayList(current + layer.name)
        }
    }

    /**
     * 从导航参数获取知识点 ID(v0.8.19 P1-UI-6 改为 StateFlow)。
     *
     * 原实现 `val pointId: String = savedStateHandle["pointId"] ?: ""` 是一次性读取,
     * 后续不观察 SavedStateHandle 变化。同路由实例下 pointId 变化不更新。
     *
     * 现改为 StateFlow,在 [uiState] 的 flatMapLatest 中订阅,
     * pointId 变化时自动重新订阅详情 Flow。
     *
     * 保留 `val pointId: String` 属性(读当前值),供 UI 或外部访问(如 Deep Link)。
     */
    private val pointIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("pointId", "")

    /** 当前知识点 ID(便捷访问,读 [pointIdFlow] 当前值) */
    val pointId: String get() = pointIdFlow.value

    /**
     * 重试触发器（v0.8.3 新增：支持 ErrorState 的 onRetry）。
     *
     * 自增整数，每次 [retry] 时 +1，触发 [uiState] 的 FlatMapLatest 重新订阅 Flow。
     */
    private val retryTrigger = MutableStateFlow(0)

    /**
     * 详情 UI 状态。
     *
     * v0.8.19 重构:
     * - 用 combine(retryTrigger, pointIdFlow) 触发 flatMapLatest,
     *   pointId 变化或 retry 时重新订阅详情 Flow + 错题 Flow
     * - 详情 Flow 与错题 Flow 用 combine 合并,任一变化时更新 uiState
     *
     * v0.8.3 重构：用 flatMapLatest 替代直接 stateIn + catch，支持 retry。
     * 原 catch 后流终止无法重试，现通过 retryTrigger 触发重新订阅。
     *
     * v0.8.13 P0-2/P0-3 修复:重构为 MutableStateFlow + collect 模式(与
     * [KnowledgeViewModel] 一致),解决两个问题:
     * 1. retry() 不立即显示 loading:原 stateIn 不暴露 setter,retry() 只触发
     *    retryTrigger++,uiState 仍是 error 状态,UI 不会立即显示 loading。
     *    现 retry() 先设置 isLoading=true 让 UI 立即显示 loading 反馈。
     * 2. catch 用 raw e.message 违反 P1-5:原 `e.message ?: "加载失败"` 可能
     *    展示英文堆栈("SQLiteException: no such table..."),对用户不友好。
     *    现改用 [friendlyErrorMessage] 映射为中文提示。
     * 同时 catch 时保留已有 detail/wrongAnswers,避免数据库偶发异常导致详情页
     * 内容瞬间清空(与 KnowledgeViewModel catch 保留 knowledgePoints 策略一致)。
     */
    private val _uiState = MutableStateFlow<KnowledgePointDetailUiState>(KnowledgePointDetailUiState(isLoading = true))
    val uiState: StateFlow<KnowledgePointDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // v0.8.17 修复 B1:catch 必须在 flatMapLatest 内部,仅终止本次 inner Flow,
            // 外层 Flow 仍由 retryTrigger 驱动,支持 retry() 重新触发加载。
            // 原实现 catch 在 flatMapLatest 外层,异常触发后整流终止,retry() 永久失效
            // (同 feature/cards v0.8.20 P1-2 修复模式)。
            // 同时修复 M4:加 Log.e 输出完整堆栈,生产排查不丢上下文
            // (对照 feature/cards v0.8.14 P1-7 修复)。
            combine(retryTrigger, pointIdFlow) { _, pointId -> pointId }
                .flatMapLatest { pointId ->
                    if (pointId.isBlank()) {
                        flowOf(KnowledgePointDetailUiState(isLoading = false, notFound = true))
                    } else {
                        // v0.9.8：三流合并 — 知识点详情 + 错题 + 关联论述题
                        combine(
                            knowledgeRepository.observeKnowledgePointDetail(pointId),
                            wrongAnswerRepository.observeByPoint(pointId),
                            knowledgeRepository.observeRelatedEssays(pointId),
                            knowledgeProgressRepository.observe(pointId),
                        ) { detail, wrongAnswers, relatedEssays, learningUnits ->
                            if (detail == null) {
                                KnowledgePointDetailUiState(isLoading = false, notFound = true)
                            } else {
                                // 仅展示未解决的错题(resolvedAt == null)
                                val unresolved = wrongAnswers.filter { it.resolvedAt == null }
                                KnowledgePointDetailUiState(
                                    isLoading = false,
                                    detail = detail,
                                    wrongAnswers = unresolved,
                                    relatedEssays = relatedEssays,
                                    progress = calculateKnowledgeProgress(learningUnits, System.currentTimeMillis()),
                                )
                            }
                        }
                    }
                    // v0.8.13 P0-3 修复:catch 时保留已有 detail/wrongAnswers,
                    // 避免数据库偶发异常导致详情页内容瞬间清空,用户丢失正在浏览的上下文。
                    // v0.8.17 修复 B1 + M4:catch 移入 flatMapLatest 内部 + 加 Log.e。
                    .catch { e ->
                        Timber.e(e, "loadKnowledgePointDetail failed: pointId=$pointId")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = friendlyErrorMessage(e),
                        )
                    }
                }
                .collect { _uiState.value = it }
        }
    }

    /**
     * 重试加载（v0.8.3 新增：供 ErrorState 的 onRetry 调用）。
     *
     * v0.8.13 P0-2 修复:retry() 立即设置 isLoading=true 并清空 error,
     * 保留 detail/wrongAnswers 不变,让 UI 立即显示 loading 反馈。
     * 原 retry() 只触发 [retryTrigger]++,但 uiState 仍是 error 状态,
     * UI 不会立即显示 loading,用户以为没响应。再增加 retryTrigger++
     * 触发数据流重新订阅。
     */
    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        retryTrigger.value++
    }

    /**
     * 标记错题为已解决(v0.8.19 P1-REL-1 新增)。
     *
     * 用户在详情页查看错题后,确认已掌握可点击"标记已解决",
     * 该错题从 [uiState.wrongAnswers] 中移除(Flow 自动刷新)。
     *
     * v0.8.19 P1-REL-2 修复:原 `catch (_: Exception) {}` 静默吞异常,
     * 与项目其他模块(CardsViewModel 用 Log.e)不一致,生产排查困难。
     * 现加 Log.w 输出异常堆栈,UI 仍不弹错误(标记失败不影响主流程,
     * 用户可重试或查看错题本处理)。
     *
     * @param wrongAnswerId 错题记录 ID
     */
    fun markWrongAnswerResolved(wrongAnswerId: String) {
        viewModelScope.launch {
            try {
                wrongAnswerRepository.markResolved(wrongAnswerId)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // 标记失败不影响主流程,Flow 会保持当前状态
                // 用户可重试或查看错题本处理
                Timber.w(e, "markWrongAnswerResolved failed: id=$wrongAnswerId")
            }
        }
    }
}

/**
 * 知识点详情 UI 状态。
 *
 * P1-3 新增 [error] 字段：数据流加载失败时携带错误信息，UI 据此提示用户。
 * v0.8.19 新增 [wrongAnswers] 字段:该知识点的未解决错题列表,供 UI 展示。
 * v0.9.8 新增 [relatedEssays] 字段:该知识点关联的论述题列表,供 UI 展示
 *（点击跳转论述题详情页,实现"知识点串联"核心价值）。
 */
data class KnowledgePointDetailUiState(
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val detail: KnowledgePointDetail? = null,
    /** 加载失败时的错误信息（P1-3 新增） */
    val error: String? = null,
    /** 未解决错题列表(v0.8.19 P1-REL-1 新增,按 lastWrongAt DESC) */
    val wrongAnswers: List<WrongAnswerEntity> = emptyList(),
    /** 关联论述题列表(v0.9.8 新增,按年份倒序,点击跳转论述题详情) */
    val relatedEssays: List<ExamQuestionEntity> = emptyList(),
    val progress: KnowledgeProgressUiModel? = null,
) {
    /** 知识点实体（便捷访问） */
    val point: KnowledgePointEntity? get() = detail?.point

    /** 来源列表 */
    val sources: List<DataSourceEntity> get() = detail?.sources.orEmpty()
}
