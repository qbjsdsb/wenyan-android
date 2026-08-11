package com.wenyan.app.feature.cards

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.cards.CardTemplate
import com.wenyan.app.core.data.cards.ClozeQuoteCard
import com.wenyan.app.core.data.cards.DistinctionCard
import com.wenyan.app.core.data.cards.EssayPointsCard
import com.wenyan.app.core.data.cards.SchoolComparisonCard
import com.wenyan.app.core.data.cards.TermExplanationCard
import com.wenyan.app.core.data.cards.WorkAuthorBidirectionalCard
import com.wenyan.app.core.data.cards.LearningUnitCard
import com.wenyan.app.core.data.repository.CardRepository
import com.wenyan.app.core.data.repository.CardSettingsRepository
import com.wenyan.app.core.data.repository.IntervalPreview
import com.wenyan.app.core.data.repository.SchedulingRepository
import com.wenyan.app.core.data.repository.UnitRatingReceipt
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.data.repository.daysUntilExam
import com.wenyan.app.core.database.entity.CardTemplateType
import com.wenyan.app.core.fsrs.Rating
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Navigation argument used by Today to scope a card session to one knowledge point. */
const val CARDS_TARGET_POINT_ID_ARG = "targetPointId"

/**
 * 记忆卡片模块 ViewModel。
 *
 * v0.8.8 深度打磨(修复 undo 正确性 + 新增 skip/leech 动作):
 *
 * 1. **P0:undo 多步撤销 bug 修复**
 *    - 原实现 `lastRatingWasAgain: Boolean` 只记录最近一次评分,连续撤销(AGAIN→GOOD→undo→undo)
 *      第二次 undo 丢失 AGAIN 回退,导致 sessionAgainCount 统计错误
 *    - 改用 [ratingHistory] 栈(ArrayDeque<RatingStep>),每次评分入栈,undo 出栈
 *    - 栈记录评分类型 + pointId,undo 时精确回退 sessionReviewedCount/sessionAgainCount 两项统计
 *
 * 2. **P0:undo 回退 ratedPointIds**(v0.8.12 已废弃此策略)
 *    - 原实现 undo 不回退 sibling 去重状态(注释"尽力而为"),导致撤销首张 sibling 卡后
 *      重新评分不触发 FSRS(调度被"吞")
 *    - v0.8.8 曾改为 RatingStep 记录 triggeredSchedule,undo 时从 ratedPointIds 移除,
 *      但 v0.8.12 发现这会导致 FSRS 重复调度(stability 异常增长),已回退此策略。
 *      `triggeredSchedule` 字段已于 v0.8.18 清理删除。
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
    private val cardSettingsRepository: CardSettingsRepository,
) : ViewModel() {

    /**
     * 今日任务（v0.9.29 卡片备考系统）。
     *
     * 卡片页顶部展示：今日新卡/复习数量、距考试天数、学习进度。
     * 数据来自 [CardRepository.getTodayStudyQueue]（到期复习 ∪ 每日新卡）
     * 与 [CardSettingsRepository]（每日限额 / 考试日期）。
     */
    val todayPlan: StateFlow<TodayPlanUi> = combine(
        cardRepository.getTodayStudyQueue(),
        cardRepository.getStudyProgress(),
        cardSettingsRepository.cardSettings,
    ) { queue, progress, settings ->
        TodayPlanUi(
            newCardLimit = settings.dailyNewLimit,
            newPointCount = queue.newPoints.size,
            duePointCount = queue.duePoints.size,
            learnedPoints = progress.learnedPoints,
            totalVerifiedPoints = progress.totalVerifiedPoints,
            daysUntilExam = settings.examDateMillis?.let { daysUntilExam(it) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayPlanUi())

    // 翻转状态（UI 交互层，持久化到 SavedStateHandle）
    private val _isFlipped = savedStateHandle.getStateFlow("isFlipped", false)

    // 当前卡片索引（UI 交互层，持久化到 SavedStateHandle）
    private val _currentIndex = savedStateHandle.getStateFlow("currentIndex", 0)

    // 错误提示
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Leech 警告队列(v0.8.12 改为列表,避免连续 Leech 覆盖) */
    private val _leechWarnings = MutableStateFlow<List<LeechWarning>>(emptyList())
    val leechWarnings: StateFlow<List<LeechWarning>> = _leechWarnings.asStateFlow()

    /** 向后兼容:暴露队首 Leech 警告(若有) */
    val leechWarning: StateFlow<LeechWarning?> = _leechWarnings
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** 重试触发器 */
    private val _retryTrigger = MutableStateFlow(0)

    // v0.8.5：会话内状态（非持久化，进程被杀后重置）
    /** 会话内冻结的卡片列表，避免 Flow 重新 emit 导致 currentIndex 错位 */
    @Volatile
    private var sessionCards: List<CardItem>? = null

    /** 已完成调度的 pointId 集合（同 pointId 仅第一次触发 FSRS 调度） */
    private val ratedPointIds = mutableSetOf<String>()
    private val ratedUnitIds = mutableSetOf<String>()

    /**
     * 会话内已复习张数(v0.8.9 持久化到 SavedStateHandle,修复 P2-2)。
     *
     * 含 sibling 卡。进程被杀恢复后保留统计,完成态展示准确。
     */
    private val _sessionReviewedCount = savedStateHandle.getStateFlow("sessionReviewedCount", 0)
    val sessionReviewedCount: StateFlow<Int> = _sessionReviewedCount

    /**
     * 会话内评 AGAIN 的张数(v0.8.9 持久化,修复 P2-2)。
     *
     * 用于完成态"掌握率"计算。进程被杀恢复后保留统计。
     */
    private val _sessionAgainCount = savedStateHandle.getStateFlow("sessionAgainCount", 0)
    val sessionAgainCount: StateFlow<Int> = _sessionAgainCount

    /**
     * 各知识点的上次 failCount(v0.8.12 P1 新增,用于 Leech "新增"检测)。
     *
     * Leech 检测原实现用累计 failCount >= 8 判断,导致一旦达到 8 后每次评分都弹警告。
     * 现改为检测"新增 leech":仅当 oldFailCount < 8 && newFailCount >= 8 时弹警告。
     */
    private val lastFailCounts = mutableMapOf<String, Int>()

    /**
     * 评分历史栈(v0.8.8 新增,替代 v0.8.7 的 `lastRatingWasAgain: Boolean`)。
     *
     * 每次评分/skip 入栈一个 [RatingStep],undo 时出栈并精确回退:
     * - sessionReviewedCount(评分才 +1,skip 不 +1)
     * - sessionAgainCount(AGAIN 评分才 +1)
     *
     * v0.8.12 P0 修复:不再回退 ratedPointIds(避免重新评分导致 FSRS 重复调度,
     * stability 异常增长)。原 `triggeredSchedule` 字段已于 v0.8.18 清理删除。
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
     * v0.8.9:持久化到 SavedStateHandle,进程被杀恢复后会话时长统计仍准确(基于原始开始时间)。
     * 默认值为当前时间(首次进入时初始化),恢复时使用 SavedStateHandle 中的旧值。
     */
    private val _sessionStartTime = savedStateHandle.getStateFlow("sessionStartTime", System.currentTimeMillis())

    // ---------- v0.9.18 新增：手动加入错题本 ----------

    /**
     * 本会话中手动加入错题本的 pointId 集合（v0.9.18 新增）。
     *
     * 持久化到 SavedStateHandle，进程恢复后保留。
     * 序列化格式：逗号分隔的 pointId 列表（pointId 保证不含逗号）。
     */
    private val _manualAddedPointIds = MutableStateFlow(
        savedStateHandle.get<String>("manualAddedPointIds")?.let { raw ->
            // 校验：空字符串或纯逗号时返回 emptySet
            if (raw.isBlank() || raw.all { it == ',' }) emptySet()
            else raw.split(",").filter { it.isNotBlank() }.toSet()
        } ?: emptySet(),
    )

    /** 手动加入错题本操作中（防重入锁，v0.9.18 新增） */
    private val _isAddingBookmark = MutableStateFlow(false)
    val isAddingBookmark: StateFlow<Boolean> = _isAddingBookmark.asStateFlow()

    /** 本会话中手动加入错题本的 pointId 集合（v0.9.18 新增，供 UI 判断按钮状态） */
    val manualAddedPointIds: StateFlow<Set<String>> = _manualAddedPointIds.asStateFlow()

    /** 操作成功消息通道（v0.9.18 新增，供 UI 展示 Snackbar） */
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /** 本会话手动加入错题本次数（v0.9.18 新增，上限 999） */
    private val _sessionManualAddCount = MutableStateFlow(
        (savedStateHandle.get<Int>("sessionManualAddCount") ?: 0).coerceIn(0, 999),
    )
    val sessionManualAddCount: StateFlow<Int> = _sessionManualAddCount.asStateFlow()

    private val _uiState = MutableStateFlow<CardsUiState>(CardsUiState(isLoading = true))
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    /**
     * 当前卡片是否已手动加入错题本（v0.9.18 新增）。
     *
     * 通过 combine _uiState 和 _manualAddedPointIds 自动计算，
     * 当卡片切换或 manualAddedPointIds 变化时自动更新。
     * Sibling 感知：同 pointId 的任意卡被加入，均显示"已加入"。
     * 注意：_uiState 声明必须在 isCurrentCardInWrongBook 之前（Kotlin 无 forward reference）。
     */
    val isCurrentCardInWrongBook: StateFlow<Boolean> = combine(
        _uiState,
        _manualAddedPointIds,
    ) { state, addedIds ->
        val card = state.currentCard ?: return@combine false
        card.pointId.isNotBlank() && card.pointId in addedIds
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * 会话时长(分钟,v0.8.17 P1 修复:改为 StateFlow 暴露)。
     *
     * 原实现 [getSessionDurationMinutes] 是普通函数,在 CardsScreen 的 Crossfade 重组
     * 中被直接调用(`sessionDurationMinutes = viewModel.getSessionDurationMinutes()`)。
     * 这是 Compose 反模式:
     * - 每次父组件重组都会重新执行该函数,读 System.currentTimeMillis() 返回不稳定值
     * - 破坏 Compose 重组跳过机制(参数不稳定 → SessionCompleteState 无谓重组)
     * - 在完成态停留时,时长会随重组不断变化,但 UI 无感知(参数已传入子组件)
     *
     * 现改为 StateFlow,仅在 [_uiState] 或 [_sessionStartTime] 变化时重新计算:
     * - 进入完成态(isFinished=true)时计算一次并缓存
     * - 会话进行中(isFinished=false)返回 0(完成态才展示时长,进行中无需计算)
     * - retry 时 [_sessionStartTime] 重置,StateFlow 重新发射 0(随后 isFinished=true 时再计算)
     *
     * 注:会话时长仅在完成态展示,进入完成态后会话已结束,时长不再增长,无需定期刷新。
     * coerceAtLeast(1) 避免显示 0 分钟(让用户觉得没学到东西)。
     */
    val sessionDurationMinutes: StateFlow<Int> = combine(
        _uiState,
        _sessionStartTime,
    ) { state, startTime ->
        if (state.isFinished) {
            val millis = System.currentTimeMillis() - startTime
            (millis / 60_000L).toInt().coerceAtLeast(1)
        } else {
            0
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /**
     * 当前知识点是否已经在本会话完成过调度。
     *
     * true 时隐藏预期间隔并显示说明：后续 sibling 评分和“回看上一张”后的重新选择
     * 都不会再次写 FSRS。旧实现对回看的首张卡返回 false，导致 UI 重新显示“良好→6天”
     * 等预览，但实际评分会被去重，属于明确误导。
     *
     * 属性名保留以减少 UI/测试改动；语义已经从“是否 sibling”收敛为“是否已调度”。
     */
    val isSiblingAlreadyRated: StateFlow<Boolean> = _uiState
        .map { state ->
            val card = state.currentCard ?: return@map false
            val pointId = card.pointId
            if (card.learningUnitId.isNotBlank()) {
                card.learningUnitId in ratedUnitIds
            } else {
                pointId.isNotBlank() && pointId in ratedPointIds
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    init {
        // v0.9.35 审计修复：会话完成时累加学习时长到 study_progress.totalStudyTime——
        // addStudyTime 原无任何调用方（死链路），设置页"学习进度"总时长恒为 0。
        // v0.9.35 第四轮审计：按 sessionStartTime 结算去重——"完成→撤销→再完成"
        // 时同一会话的时长只累加一次（undo 不改 startTime，retry 会重置 startTime
        // 因此新会话正常累加）。
        viewModelScope.launch {
            var settledStartTime = -1L
            _uiState
                .map { it.isFinished }
                .distinctUntilChanged()
                .drop(1) // 跳过初始发射，只响应"false→true"转变
                .collect { finished ->
                    if (finished) {
                        val start = _sessionStartTime.value
                        if (start == settledStartTime) return@collect
                        settledStartTime = start
                        try {
                            studyProgressRepository.addStudyTime(
                                sessionDurationMinutes.value.coerceAtLeast(1) * 60,
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // 学习时长统计失败不影响复习主流程，仅记录
                            Timber.w(e, "addStudyTime failed on session finish")
                        }
                    }
                }
        }

        viewModelScope.launch {
            // v0.8.20 P1-2 修复 retry-after-error bug:
            // 原结构 `.flatMapLatest { combine... }.catch { emit(...) }.collect {...}`
            // 中 .catch 在 flatMapLatest 外层。当 combine 抛异常时,.catch emit 错误态后
            // 整条 Flow 终止,viewModelScope.launch 协程返回。此后 retry() 触发的
            // _retryTrigger.value++ 无法被任何 collector 接收(retry 只同步设置
            // isLoading=true),导致 UI 永远停留在 loading 态。
            //
            // 修复:把 .catch 移入 flatMapLatest 的 lambda 内部,使其仅终止"本次订阅"
            // 的 inner Flow,而非外层 Flow。外层 Flow 仍由 _retryTrigger 驱动,retry()
            // 触发新值时 flatMapLatest 会创建新的 inner Flow(combine + catch),
            // 实现"出错后 retry 真正重新加载"。
            //
            // 单元测试验证:`加载失败后 retry 清空错误并重新加载`(场景 38)。
            _retryTrigger
                .flatMapLatest {
                    combine(
                        cardRepository.getCardsForReview(),
                        // v0.9.31：今日队列（newPoints 用于标记"新卡"徽章）
                        cardRepository.getTodayStudyQueue(),
                        _isFlipped,
                        _currentIndex,
                    ) { cards, queue, isFlipped, currentIndex ->
                        val newPointIds = queue.newPoints.map { it.id }.toSet()
                        val targetPointId = savedStateHandle.get<String>(CARDS_TARGET_POINT_ID_ARG)
                            ?.takeIf { it.isNotBlank() }
                        val scopedCards = if (targetPointId == null) {
                            cards
                        } else {
                            cards.filter { it.pointId == targetPointId }
                        }
                        // v0.8.5 P0：会话内冻结 cards，避免 Flow 重新 emit 导致错位
                        // v0.8.6 P0:进程被杀后恢复(sessionLoaded=true 但 sessionCards=null)
                        //   此时 currentIndex 可能 >0 但 sessionCards 已丢失,重置避免错位
                        val isFirstLoad = savedStateHandle.get<Boolean>(KEY_SESSION_LOADED) != true
                        val effectiveCards = if (isFirstLoad) {
                            // 首次加载:重新生成 sessionCards
                            // v0.9.37 P1-9:数千张卡的 id 生成(buildString)移出主线程,
                            // 避免冷进入卡片页 combine 在主线程数百 ms 掉帧
                            val newCards = buildSessionCards(scopedCards, newPointIds)
                            // 空队列不能冻结为一次学习会话：共享队列过去会先发人工空初值，
                            // 真实新卡随后到达时因 sessionCards 已冻结为空而永远不可见。
                            // 即使是真空队列，也应允许 60s tick 后新到期卡进入当前页面。
                            if (newCards.isNotEmpty()) {
                                savedStateHandle[KEY_SESSION_LOADED] = true
                                sessionCards = newCards
                            }
                            newCards
                        } else if (sessionCards == null) {
                            // 进程被杀后恢复:sessionLoaded=true 但 sessionCards=null
                            // 重置 currentIndex 避免错位,重新加载 cards
                            //
                            // v0.8.10 P1-E2+F1 修复:
                            // 原实现只重置 currentIndex,保留 sessionReviewedCount/sessionAgainCount,
                            // 导致用户重新评分时统计重复累加(如已评 5 张被杀,恢复后重评 5 张,count=10)。
                            // 同时 ratedPointIds(内存)丢失,sibling 去重失效,可能重复触发 FSRS 调度。
                            //
                            // 修复策略:进程恢复时重置统计计数为 0,避免重复计数。
                            // 会话时长保留(基于原始 sessionStartTime,反映总学习时间)。
                            // ratedPointIds 已在内存丢失(空 set),sibling 去重自然失效,
                            // 但 FSRS 调度由数据库 next_review_at 控制,不会真正重复调度
                            // (已调度的卡 next_review_at > now,不会出现在 getCardsForReview 中)。
                            if (currentIndex > 0) {
                                savedStateHandle["currentIndex"] = 0
                                savedStateHandle["isFlipped"] = false
                            }
                            // 重置统计计数,避免恢复后重新评分导致重复计数
                            savedStateHandle["sessionReviewedCount"] = 0
                            savedStateHandle["sessionAgainCount"] = 0
                            // 清空评分历史栈(内存已丢失,同步清空避免 undo 错位)
                            ratingHistory.clear()
                            val newCards = buildSessionCards(scopedCards, newPointIds)
                            if (newCards.isNotEmpty()) {
                                sessionCards = newCards
                            } else {
                                // 不冻结空恢复结果，允许随后到期/加载完成的卡片进入。
                                savedStateHandle[KEY_SESSION_LOADED] = false
                                sessionCards = null
                            }
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
                    // v0.8.20 P1-2:.catch 必须在 flatMapLatest 内部(见 init 头注释),
                    // 仅终止本次订阅的 inner Flow,使外层 Flow 仍由 _retryTrigger 驱动,
                    // 支持 retry() 重新触发加载。
                    .catch { e ->
                        // v0.8.14 P1-7 修复:原仅取 e.message 丢失堆栈,生产排查困难。
                        // 现加 Log.e 输出完整堆栈,UI 仍只展示友好提示(用户无需看堆栈)。
                        //
                        // v0.8.20 P1-2 统一错误处理:
                        // 原用 `e.message ?: "加载失败"` 直接暴露原始异常文本(可能是英文堆栈
                        // 或类名如 "java.net.SocketTimeoutException: failed to connect"),
                        // 与 KnowledgeViewModel 错误提示不一致(知识模块已用 friendlyErrorMessage
                        // 映射为中文友好提示)。
                        // 现复用 core/common/util/friendlyErrorMessage,与 feature/knowledge 保持
                        // 用户体验一致:网络异常→"网络超时,请检查网络后重试",
                        // 数据库异常→"本地数据异常,请重启 App",未知异常→"加载失败,请重试"。
                        // v0.8.21: 改用 Timber 结构化日志(原 android.util.Log.e)
                        Timber.e(e, "loadCards failed")
                        emit(
                            CardsUiState(
                                error = com.wenyan.app.core.common.util.friendlyErrorMessage(e),
                            ),
                        )
                    }
                }
                .collect { _uiState.value = it }
        }

        // v0.8.6 P0:监听 currentIndex 变化,异步加载当前卡的预期间隔
        // 只在 cards 非空 + currentIndex 有效时加载,避免无效查询
        //
        // v0.8.13 P0-1 修复:collect 改 collectLatest,避免快速评分时预览加载排队。
        // 原实现用 collect,previewIntervals 是 suspend 函数,如果用户快速评 GOOD 推进
        // 多张卡片,前一张的预览加载未完成会阻塞后一张,导致预览显示延迟(看到旧卡的间隔)。
        // collectLatest 在新卡片到来时立即取消旧预览加载协程,保证预览始终对应当前卡。
        viewModelScope.launch {
            _uiState
                .map { it.currentCard to it.currentIndex }
                .distinctUntilChanged()
                .collectLatest { (card, _) ->
                    // v0.9.7 M11 修复:进入 collectLatest 时立即清空预览,
                    // 避免快速切卡时新卡预览加载期间 UI 短暂显示旧卡的"6天/12天"预览(误导用户)。
                    // 原实现仅在 card==null / templateType==null / 加载失败时清空,
                    // 正常加载期间(异步 suspend)仍持有上一张卡的预览。
                    _currentPreviews.value = emptyMap()
                    if (card == null || card.pointId.isBlank()) {
                        return@collectLatest
                    }
                    val templateType = try {
                        CardTemplateType.valueOf(card.cardType)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                    if (templateType == null) {
                        return@collectLatest
                    }
                    try {
                        _currentPreviews.value = if (card.learningUnitId.isNotBlank()) {
                            schedulingRepository.previewLearningUnitIntervals(card.learningUnitId, templateType)
                        } else {
                            schedulingRepository.previewIntervals(card.pointId, templateType)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // 预览失败不阻塞主流程,UI 降级为无预览按钮
                        Timber.w(e, "previewIntervals failed for pointId=${card.pointId}")
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
        // v0.8.14 P0-1 修复:双击/快速连点防护。
        // 原实现从 `uiState.value.currentCard` 读取当前卡,但 _uiState 在 init 协程中
        // 异步更新(combine + collect),两次同步 rateCard 调用之间 _uiState 不会重新 emit,
        // 导致第二次调用仍读到旧卡,同一张卡被评分两次,统计虚高 + 跳过下一张。
        //
        // 修复:从 [sessionCards] + [_currentIndex](SavedStateHandle-backed,同步可见)读取,
        // 第一次调用写入 currentIndex+1 后,第二次调用立即读到新索引对应的卡。
        // sessionCards 可能为 null(首次加载未完成),此时 uiState 仍是 isLoading,UI 不会
        // 显示评分按钮,但兜底 return 防御。
        val current = sessionCards?.getOrNull(_currentIndex.value) ?: return
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
        // v0.8.14 P0-4 修复:无 pointId 卡评 AGAIN 不再记录错题。
        // 原实现(v0.8.10 P2-C3)传入 pointId=null 调用 recordWrongAnswer,但
        // [WrongAnswerRepository] 契约要求"pointId 与 examQuestionId 至少一个非空"。
        // WrongAnswerRepositoryImpl 对 null pointId 的处理是 existing=null,
        // 导致每次评分都 upsert 新记录,无法去重、无法关联知识点、无法通过
        // observeByPoint 查询,成为错题本里的"孤儿数据"。
        //
        // 现恢复 v0.8.5 设计:无 pointId 卡不记录错题(无知识点关联,记录无意义)。
        // TODO:如需记录,需要 schema 改动用 cardId 作为关联键。
        //
        // v0.9.7 B3 修复:无 pointId 卡评分不调用 recordStudySession(streakDays 不更新),
        // 因为 recordStudySession 需要 pointId 作为 last_point_id,空字符串语义错误。
        // 当前 CardRepositoryImpl.generateCardsFromKnowledgePoint 总是设置 pointId,
        // 无 pointId 卡是数据完整性问题,加日志警告便于生产排查。
        if (pointId.isBlank()) {
            Timber.w("Card with blank pointId rated, skip study progress record. cardId=${current.id}, cardType=$cardTypeStr")
            ratingHistory.addLast(RatingStep(rating = rating, pointId = ""))
            return
        }

        // v0.8.5 P0：sibling 去重 — 同 pointId 仅第一次评分触发 FSRS 调度
        // v0.9.7 B1 修复：原实现先 `ratedPointIds.add(pointId)` 再判断 templateType 是否有效,
        // 若 cardTypeStr 是无效枚举名(数据损坏/未来枚举变更),pointId 已加入 ratedPointIds
        // 但 FSRS 调度被跳过。后续同 pointId 卡 shouldSchedule=false,永远不会重试调度,
        // FSRS 数据永久缺失。现将 templateType 解析提前,仅当可调度时才 add。
        val templateType = try {
            CardTemplateType.valueOf(cardTypeStr)
        } catch (e: IllegalArgumentException) {
            // v0.9.7 M9:静默失败改为日志记录,生产环境可排查为何某些卡未触发调度
            Timber.w(e, "Invalid cardType for pointId=$pointId, cardTypeStr=$cardTypeStr, skip FSRS scheduling")
            null
        }
        val unitId = current.learningUnitId
        val shouldSchedule = templateType != null && if (unitId.isNotBlank()) {
            unitId !in ratedUnitIds
        } else {
            pointId !in ratedPointIds
        }
        if (shouldSchedule) {
            if (unitId.isNotBlank()) ratedUnitIds.add(unitId) else ratedPointIds.add(pointId)
        }

        val receipt = if (unitId.isNotBlank() && shouldSchedule) CompletableDeferred<UnitRatingReceipt?>() else null
        ratingHistory.addLast(RatingStep(rating = rating, pointId = pointId, unitId = unitId, receipt = receipt))

        viewModelScope.launch {
            val fsrsRating = when (rating) {
                CardRating.AGAIN -> Rating.AGAIN
                CardRating.HARD -> Rating.HARD
                CardRating.GOOD -> Rating.GOOD
                CardRating.EASY -> Rating.EASY
            }

            // 仅第一次评分触发 FSRS 调度
            // 注:shouldSchedule = pointId !in ratedPointIds && templateType != null,
            // templateType 是 val 且 shouldSchedule 隐含 templateType != null,
            // Kotlin smart cast 可通过 Boolean val 传播推断此处 templateType 非空。
            if (shouldSchedule) {
                val updated = try {
                    if (unitId.isNotBlank()) {
                        val result = schedulingRepository.rateLearningUnit(pointId, unitId, fsrsRating, templateType)
                        receipt?.complete(result)
                        result?.updated?.let { ScheduleOutcome(it.state, it.failCount) }
                    } else {
                        schedulingRepository.rateCard(pointId, fsrsRating, templateType)
                            ?.let { ScheduleOutcome(it.state, it.failCount) }
                    }
                } catch (e: CancellationException) {
                    receipt?.cancel(e)
                    throw e
                } catch (e: Exception) {
                    // v0.8.12 P1-3:错误优先级调度失败 > 学习进度 > 错题,用 hasSchedulingError 标记
                    _errorMessage.value = "评分调度失败：${e.message ?: "未知错误"}"
                    // v0.9.35 审计修复：调度失败回滚 ratedPointIds 标记，
                    // 否则同 pointId 的 sibling 卡本会话永不再触发调度（FSRS 数据永久缺失）
                    if (unitId.isNotBlank()) ratedUnitIds.remove(unitId) else ratedPointIds.remove(pointId)
                    receipt?.complete(null)
                    null
                }

                if (updated != null) {
                    // v0.8.17 P1-3 修复:oldFailCount 计算改为延迟到 updated != null 块内,
                    // 支持进程恢复后 lastFailCounts 为空时从 updated.failCount 反推基准。
                    //
                    // 背景:进程被杀恢复后 lastFailCounts(内存)丢失,原实现 oldFailCount = 0,
                    // 若 DB 中 failCount 已 >= 8(之前已跨 Leech 阈值),评分后 failCount 继续增长,
                    // 触发 `0 < 8 && newFailCount >= 8` → Leech 警告误报(用户之前已见过此警告)。
                    //
                    // v0.9.7 B2 修复:原实现 AGAIN 总是 `failCount - 1`,但 FSRS-6 中
                    // 仅 REVIEW + AGAIN 增加 lapses(=failCount),LEARNING/RELEARNING + AGAIN
                    // 不增加(注释"学习阶段答Again:尚未记住,不构成遗忘")。
                    // 若 RELEARNING + AGAIN 时 updated.failCount 未变,反推 oldFailCount = failCount - 1
                    // 会误算(可能导致 Leech 误报或漏报)。现根据 updated.state 区分:
                    // - state == "REVIEW" + AGAIN:lapses+1,oldFailCount = failCount - 1
                    // - 其他状态 + AGAIN:lapses 不变,oldFailCount = failCount
                    // - 非 AGAIN 评分:lapses 不变,oldFailCount = failCount
                    //
                    // 仅当内存有记录时优先用内存值(更准确,反映本次会话内的连续 AGAIN 序列),
                    // 内存无记录时才反推(进程恢复场景)。
                    val scheduleKey = unitId.ifBlank { pointId }
                    val oldFailCount = lastFailCounts[scheduleKey] ?: when (fsrsRating) {
                        Rating.AGAIN -> if (updated.state == "REVIEW") {
                            (updated.failCount - 1).coerceAtLeast(0)
                        } else {
                            updated.failCount
                        }
                        else -> updated.failCount
                    }

                    // 更新 failCount 跟踪
                    lastFailCounts[scheduleKey] = updated.failCount

                    // v0.8.12 P1-1:Leech 检测改为"新增 leech"
                    // 原实现用累计 failCount >= 8,导致达到阈值后每次评分都弹警告
                    // 现仅当 oldFailCount < 8 && newFailCount >= 8 时弹警告(首次跨阈值)
                    if (oldFailCount < LEECH_THRESHOLD && updated.failCount >= LEECH_THRESHOLD) {
                        // v0.9.37 P2：读改写用 update{}（原子），未来并发写入不丢更新
                        _leechWarnings.update { it + LeechWarning(
                            message = "这张卡片已连续答错 ${updated.failCount} 次，" +
                                "建议查看知识点详情重新理解，或问 AI 助手辅助。",
                            pointId = pointId,
                        ) }
                    }

                    // v0.8.12 P0-2:recordStudySession 移入 if (updated != null) 块
                    // 原实现在 rateCard 失败(updated=null)时仍调用 recordStudySession,
                    // 导致 study_progress 更新(learning streak +1)但 memo_records 未更新(FSRS 失败),
                    // 数据不一致。现仅调度成功后才记录学习进度。
                    try {
                        studyProgressRepository.recordStudySession(pointId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // v0.8.12 P1-3:仅在无更高优先级错误时显示
                        val currentError = _errorMessage.value
                        if (currentError == null ||
                            !currentError.startsWith("评分调度失败")
                        ) {
                            _errorMessage.value = "学习进度记录失败：${e.message ?: "未知错误"}"
                        }
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
                        // v0.8.13 P0-2:用 extractCorrectAnswer 提取真实答案,
                        // DistinctionCard.back 是占位文本"$item1 与 $item2 的区别见要点",
                        // EssayPointsCard.back 是 summary 散文,均不适合作为 correctAnswer。
                        // 现按模板类型提取结构化答案(differences/keyPoints/quote 等)。
                        correctAnswer = extractCorrectAnswer(current),
                        source = WrongAnswerRepository.SOURCE_CARD_AGAIN,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // v0.8.12 P1-3:仅在无更高优先级错误时显示
                    val currentError = _errorMessage.value
                    if (currentError == null ||
                        !currentError.startsWith("评分调度失败")
                    ) {
                        _errorMessage.value = "错题记录失败：${e.message ?: "未知错误"}"
                    }
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
        // v0.8.14 P0-1 修复:与 rateCard 一致,从 sessionCards + _currentIndex 读取,
        // 避免快速连点 skipCard 时同一张卡被跳过多次,索引错位。
        val current = sessionCards?.getOrNull(_currentIndex.value) ?: return
        savedStateHandle["isFlipped"] = false
        savedStateHandle["currentIndex"] = _currentIndex.value + 1
        // skip 不累加 sessionReviewedCount/sessionAgainCount,但入栈供 undo 回退索引
        ratingHistory.addLast(RatingStep(rating = null, pointId = current.pointId))
    }

    /**
     * 回看上一张卡片（仅 UI 回退，不回滚 FSRS 调度）。
     *
     * v0.8.5 P1 新增：
     * - 仅回退 UI 索引，供用户重新查看内容。
     * - FSRS 调度不可逆（已写入 memo_records + review_logs），
     *   回看仅让用户重新查看上一张卡片内容。
     * - 边界：currentIndex == 0 时无操作。
     *
     * v0.8.8 重写:用 [ratingHistory] 栈精确回退两项统计:
     * - sessionReviewedCount(评分才 -1,skip 不 -1)
     * - sessionAgainCount(AGAIN 评分才 -1)
     *
     * v0.8.12 P0 修复:undo **不再回退** ratedPointIds。
     * 原实现(v0.8.8)undo 时从 ratedPointIds 移除 pointId"让重新评分能再触发 FSRS",
     * 但 FSRS 调度不可逆(已写入 DB),重新评分会第二次调用 rateCard,
     * 基于已调度的 stability 再次计算,导致 stability 异常增长,FSRS 数据失真。
     * 现恢复 v0.8.5 设计:undo 仅回退 UI + 统计,ratedPointIds 保持不变,
     * 回看后再次选择评分时 shouldSchedule=false，用户可继续推进，但 FSRS 保持第一次结果。
     * GOOD/HARD/EASY 都是 pass,影响小;AGAIN 已记录错题,用户可从错题本复习。
     */
    fun undo() {
        if (_currentIndex.value <= 0) return
        // v0.8.8:从栈顶弹出最近一步,据此精确回退统计
        val step = ratingHistory.removeLastOrNull() ?: return
        savedStateHandle["currentIndex"] = _currentIndex.value - 1
        savedStateHandle["isFlipped"] = false
        // skip(rating=null)不影响统计,仅回退索引
        if (step.rating != null) {
            savedStateHandle["sessionReviewedCount"] =
                (_sessionReviewedCount.value - 1).coerceAtLeast(0)
            if (step.rating == CardRating.AGAIN) {
                savedStateHandle["sessionAgainCount"] =
                    (_sessionAgainCount.value - 1).coerceAtLeast(0)
            }
        }
        if (step.unitId.isNotBlank() && step.receipt != null) {
            viewModelScope.launch {
                val receipt = runCatching { step.receipt.await() }.getOrNull() ?: return@launch
                if (schedulingRepository.undoLearningUnitRating(receipt)) {
                    ratedUnitIds.remove(step.unitId)
                    lastFailCounts.remove(step.unitId)
                } else {
                    _errorMessage.value = "撤销评分失败，请稍后重试"
                }
            }
        }
        // 旧知识点调度仍不可逆；新学习单元依靠完整 receipt 做精确事务撤销。
    }

    /** 清除错误提示 */
    fun clearError() {
        _errorMessage.value = null
    }

    /** 清除成功消息（v0.9.18 新增，供 UI 消费后调用） */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /** 清除当前 Leech 警告(队首),显示队列中下一个(若有) */
    fun clearLeechWarning() {
        // v0.9.37 P2：读改写用 update{}（原子）
        _leechWarnings.update { it.drop(1) }
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
        ratedUnitIds.clear()
        ratingHistory.clear()
        // v0.8.12 P1:retry 清理 lastFailCounts,避免恢复后 Leech 检测基准错误
        lastFailCounts.clear()
        savedStateHandle["sessionReviewedCount"] = 0
        savedStateHandle["sessionAgainCount"] = 0
        _leechWarnings.value = emptyList()
        _currentPreviews.value = emptyMap()
        // v0.8.12 P1:retry 清除残留错误消息
        _errorMessage.value = null
        savedStateHandle["sessionStartTime"] = System.currentTimeMillis()
        savedStateHandle["currentIndex"] = 0
        savedStateHandle["isFlipped"] = false
        savedStateHandle[KEY_SESSION_LOADED] = false
        // v0.8.13 P0-3:retry 也要重置 isFinished 字段
        // 原实现仅 copy(isLoading=true, error=null),未重置 isFinished,
        // 导致 retry 后到 _retryTrigger Flow 重新 emit 前的窗口期 isFinished 仍为 true。
        // 虽然 isLoading 优先级更高使 Crossfade 显示 loading,但状态不一致,
        // 且若 retry 在 isFinished=true 时被连续调用,第二次 retry 会读到残留 isFinished=true。
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            isFinished = false,
            currentIndex = 0,
            isFlipped = false,
        )
        // v0.9.18: retry 清空手动加入错题本状态（新会话重新开始）
        updateManualAddedPointIds(emptySet())
        _sessionManualAddCount.value = 0
        savedStateHandle["sessionManualAddCount"] = 0
        _successMessage.value = null
        _retryTrigger.value++
    }

    // ---------- v0.9.18 新增：手动加入错题本 ----------

    /**
     * 持久化辅助：每次更新时同步写入 SavedStateHandle（v0.9.18 新增）。
     *
     * 契约：pointId 不含逗号，确保 joinToString(",") 可逆。
     */
    private fun updateManualAddedPointIds(newSet: Set<String>) {
        require(newSet.all { it.indexOf(',') == -1 }) {
            "pointId must not contain comma: ${newSet.first { it.indexOf(',') != -1 }}"
        }
        _manualAddedPointIds.value = newSet
        savedStateHandle["manualAddedPointIds"] = newSet.joinToString(",")
    }

    /**
     * 手动将当前卡片加入错题本（v0.9.18 新增）。
     *
     * 用户在知识卡片页面点击"加入错题本"按钮时调用。
     * 使用 NonCancellable 上下文确保 DB 写入 + 状态更新原子不可分割。
     *
     * 安全设计：
     * - 防重入锁 [_isAddingBookmark]
     * - 防重复加入 [_manualAddedPointIds] 检查
     * - pointId 空值保护
     * - 文本截断（front 200 字符，correctAnswer 500 字符）
     * - 控制字符过滤
     */
    fun addToWrongAnswerBook() {
        val current = sessionCards?.getOrNull(_currentIndex.value) ?: return
        val pointId = current.pointId

        // 1. 检查 pointId 有效性
        if (pointId.isBlank()) {
            _errorMessage.value = "无法加入错题本：知识点关联缺失"
            Timber.w("addToWrongAnswerBook failed: blank pointId, cardId=${current.id}, front=${current.front.take(20)}")
            return
        }

        // 2. 检查是否已加入（防重复）
        if (pointId in _manualAddedPointIds.value) {
            Timber.d("addToWrongAnswerBook skipped: pointId $pointId already in manualAddedPointIds")
            return
        }

        // 3. 防重入锁
        if (_isAddingBookmark.value) {
            Timber.d("addToWrongAnswerBook skipped: isAddingBookmark is true")
            return
        }
        _isAddingBookmark.value = true

        viewModelScope.launch {
            try {
                // DB 写入 + 状态更新在同一 NonCancellable 块内，原子不可分割
                // 注：不切换 Dispatchers.IO，repository 的 suspend 函数自行处理 IO 调度
                withContext(NonCancellable) {
                    // front 截断到 200 字符避免存储过大文本
                    val maxFrontLength = 200
                    val truncatedFront = if (current.front.length > maxFrontLength) {
                        current.front.take(maxFrontLength) + "…"
                    } else {
                        current.front
                    }
                    // 过滤控制字符
                    val sanitizedFront = truncatedFront.filter { it.category != CharCategory.CONTROL }
                    val userAnswer = "手动加入：$sanitizedFront"

                    // correctAnswer 长度限制
                    val maxCorrectAnswerLength = 500
                    val correctAnswer = extractCorrectAnswer(current)
                    val truncatedCorrectAnswer = if (correctAnswer != null && correctAnswer.length > maxCorrectAnswerLength) {
                        correctAnswer.take(maxCorrectAnswerLength) + "…"
                    } else {
                        correctAnswer
                    }
                    // correctAnswer 空安全兜底
                    val safeCorrectAnswer = if (truncatedCorrectAnswer.isNullOrBlank()) {
                        Timber.w("addToWrongAnswerBook: blank correctAnswer for pointId=$pointId, cardId=${current.id}")
                        "（无答案内容）"
                    } else {
                        truncatedCorrectAnswer
                    }

                    wrongAnswerRepository.recordWrongAnswer(
                        pointId = pointId,
                        examQuestionId = null,
                        userAnswer = userAnswer,
                        correctAnswer = safeCorrectAnswer,
                        source = WrongAnswerRepository.SOURCE_CARD_MANUAL,
                    )
                    // 状态更新也放在 NonCancellable 块内
                    updateManualAddedPointIds(_manualAddedPointIds.value + pointId)
                    val newCount = (_sessionManualAddCount.value + 1).coerceIn(0, 999)
                    _sessionManualAddCount.value = newCount
                    savedStateHandle["sessionManualAddCount"] = newCount
                }
                // 独立成功通道（在 NonCancellable 块外，丢失不影响数据完整性）
                _successMessage.value = "已加入错题本"
                Timber.i("addToWrongAnswerBook succeeded: pointId=$pointId, cardId=${current.id}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "addToWrongAnswerBook failed for pointId=$pointId")
                _errorMessage.value = "错题本记录失败：${e.message ?: "未知错误"}"
            } finally {
                _isAddingBookmark.value = false
            }
        }
    }

    /** 将 [CardTemplate] 映射为 UI 层 [CardItem] */
    private fun CardTemplate.toUiItem(index: Int, isNew: Boolean = false): CardItem = CardItem(
        // v0.8.8:稳定 ID(替代 index-based),基于 pointId+类型+内容哈希
        // sibling 卡(同 pointId 同类型)靠 front 内容区分,保证 ID 唯一且稳定
        //
        // v0.8.14 P1-1 修复:原用 `front.take(16).hashCode()` 仅取前 16 字符,
        // 两张同 pointId 卡如果 front 前 16 字符相同(如"建安风骨 — 时代特征"和
        // "建安风骨 — 时代背景"前 16 字符都是"建安风骨 — 时代"),ID 完全相同,
        // 会导致 Compose key 重复以及卡片身份混淆。
        // 现用全文 hashCode,降低碰撞概率(仍非密码学安全,但业务场景足够)。
        id = learningUnitId.ifBlank { buildString {
            append(templateType.name)
            append('_')
            if (pointId.isNotBlank()) append(pointId) else append(front.hashCode())
            append('_')
            append(front.hashCode())
        } },
        front = front,
        back = back,
        cardType = templateType.name,
        pointId = pointId,
        learningUnitId = learningUnitId,
        isNew = isNew,
        template = this,
    )

    /**
     * 批量构建会话卡片（v0.9.37 P1-9）。
     *
     * 纯 CPU 计算（id buildString + 新卡标记），移入 [sessionCardDispatcher]
     * （默认 Default）执行，避免冷进入卡片页时在 combine（Main 上下文）对
     * 数千张卡逐个生成 id 导致掉帧。
     */
    private suspend fun buildSessionCards(
        cards: List<CardTemplate>,
        newPointIds: Set<String>,
    ): List<CardItem> = withContext(sessionCardDispatcher) {
        cards.mapIndexed { index, card ->
            card.toUiItem(index, isNew = card.pointId in newPointIds)
        }
    }

    /**
     * 从卡片模板提取真实的"正确答案"文本,用于错题记录的 correctAnswer 字段(v0.8.13 P0-2)。
     *
     * 原实现所有卡片类型都用 [CardItem.back],但部分模板的 back 是占位文本或非结构化答案:
     * - [DistinctionCard.back] = "$item1 与 $item2 的区别见要点"(占位提示,无信息量)
     * - [EssayPointsCard.back] = summary(可能是散文,但 keyPoints 才是结构化要点)
     * - [SchoolComparisonCard] 无有意义的 back(由 schools 列表组成)
     *
     * 现按模板类型提取最适合作为"正确答案"的文本:
     * - DistinctionCard: differences 列表拼接("要点1; 要点2; 要点3")
     * - EssayPointsCard: keyPoints 带序号拼接("1. 要点1; 2. 要点2")
     * - SchoolComparisonCard: 各流派名称+主张拼接
     * - TermExplanationCard: 优先 fullExplanation(完整解释),降级 back(维度答案)
     * - ClozeQuoteCard: quote(完整名句,含答案)
     * - WorkAuthorBidirectionalCard: back(对应作者/作品名)
     * - template 为 null: back(降级,保证不丢失)
     *
     * @return 真实答案文本(可能为空字符串,recordWrongAnswer 接受 String?),
     *         调用方无需判空,直接传入 correctAnswer 参数即可
     */
    private fun extractCorrectAnswer(card: CardItem): String? {
        val template = card.template ?: return card.back.takeIf { it.isNotBlank() }
        return when (template) {
            is LearningUnitCard -> card.back.takeIf { it.isNotBlank() }
            is DistinctionCard -> {
                // 区分卡:用 differences 列表拼接为完整答案
                // 占位文本"$item1 与 $item2 的区别见要点"无信息量,不能作为 correctAnswer
                if (template.differences.isNotEmpty()) {
                    template.differences.joinToString(separator = "; ")
                } else {
                    card.back.takeIf { it.isNotBlank() }
                }
            }
            is EssayPointsCard -> {
                // 论述要点卡:keyPoints 是结构化要点,比 summary(可能是散文)更适合作为答案
                if (template.keyPoints.isNotEmpty()) {
                    template.keyPoints.mapIndexed { index, point ->
                        "${index + 1}. $point"
                    }.joinToString(separator = "; ")
                } else {
                    card.back.takeIf { it.isNotBlank() }
                }
            }
            is SchoolComparisonCard -> {
                // 流派对照卡:各流派名称 + 主张拼接
                if (template.schools.isNotEmpty()) {
                    template.schools.joinToString(separator = "; ") { school ->
                        "${school.name}: ${school.proposition}"
                    }
                } else {
                    card.back.takeIf { it.isNotBlank() }
                }
            }
            is TermExplanationCard -> {
                // 名词解释卡:优先用 fullExplanation(完整解释,信息密度高),
                // 降级到 back(维度答案,如"1921年")
                template.fullExplanation?.takeIf { it.isNotBlank() } ?: card.back
            }
            is ClozeQuoteCard -> {
                // 名句填空卡:quote 是完整名句(含答案),back 是 blank(仅答案词)
                // 用 quote 让错题本展示完整名句上下文
                template.quote.takeIf { it.isNotBlank() } ?: card.back
            }
            is WorkAuthorBidirectionalCard -> {
                // 作品-作者双向卡:back 是对应作者或作品名,直接用
                card.back
            }
        }
    }

    companion object {
        /** SavedStateHandle key:会话是否已加载(用于检测进程被杀) */
        private const val KEY_SESSION_LOADED = "sessionLoaded"

        /**
         * 会话卡片批量构建的调度器（v0.9.37 P1-9）。
         *
         * 默认 [Dispatchers.Default]（CPU 密集 id 生成移出主线程）；
         * internal 可注入——单测 runTest 虚拟调度器下替换为测试调度器，
         * 避免真实线程异步导致虚拟时间无法等待（与 `uptimeMillis` 同模式）。
         */
        @kotlin.jvm.Volatile
        internal var sessionCardDispatcher: kotlinx.coroutines.CoroutineDispatcher =
            kotlinx.coroutines.Dispatchers.Default

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
// v0.9.37 P2：补 @Immutable（与 TodayPlanUi/CardItem 保持一致，列表项稳定性统一）
@Immutable
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

/**
 * 今日任务（v0.9.29 卡片备考系统）。
 *
 * 卡片页顶部展示：今日新卡/复习数量、距考试天数、学习进度。
 */
@Immutable
data class TodayPlanUi(
    /** 设置中的每日新卡限额（张，默认 60） */
    val newCardLimit: Int = 60,
    /** 今日新知识点数（按限额取整） */
    val newPointCount: Int = 0,
    /** 今日复习知识点数（FSRS 到期） */
    val duePointCount: Int = 0,
    /** 已学知识点数 */
    val learnedPoints: Int = 0,
    /** 总 VERIFIED 知识点数 */
    val totalVerifiedPoints: Int = 0,
    /** 距考试天数（未设置考试日期为 null） */
    val daysUntilExam: Int? = null,
) {
    /** 今日新卡估算张数（知识点 × 6，与每日限额显示一致） */
    val newCardEstimate: Int get() = newPointCount * CARDS_PER_POINT
    /** 今日复习估算张数 */
    val dueCardEstimate: Int get() = duePointCount * CARDS_PER_POINT
    /** 学习进度（0-1），无数据时为 0 */
    val progress: Float
        get() = if (totalVerifiedPoints > 0) {
            (learnedPoints.toFloat() / totalVerifiedPoints.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    private companion object {
        /** 每个知识点约 6 张卡的估算值（与数据层 CARDS_PER_POINT_ESTIMATE 一致） */
        const val CARDS_PER_POINT = 6
    }
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
    /** 稳定学习单元 ID；非空时评分仅写 learning_unit_records。 */
    val learningUnitId: String = "",
    /** 是否为"新卡"（v0.9.31：未学过的知识点，首次进入学习循环） */
    val isNew: Boolean = false,
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
 *
 * v0.8.18 清理:原 `triggeredSchedule: Boolean` 字段已删除。
 * v0.8.12 P0 修复后,undo 不再回退 [CardsViewModel.ratedPointIds](避免重新评分触发
 * FSRS 重复调度导致 stability 异常增长),该字段失去消费者,成为死代码。
 */
private data class RatingStep(
    val rating: CardRating?,
    val pointId: String,
    val unitId: String = "",
    val receipt: CompletableDeferred<UnitRatingReceipt?>? = null,
)

private data class ScheduleOutcome(
    val state: String,
    val failCount: Int,
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
