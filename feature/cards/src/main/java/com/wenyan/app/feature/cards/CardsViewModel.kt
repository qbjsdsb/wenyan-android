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
import kotlinx.coroutines.flow.collectLatest
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
) : ViewModel() {

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

    /** 已评分的 pointId 集合（sibling 去重，同 pointId 仅第一次触发 FSRS 调度） */
    private val ratedPointIds = mutableSetOf<String>()

    /**
     * 每个 pointId 的"首张评分卡" cardId(v0.8.13 P1-1 新增)。
     *
     * 用于 [isSiblingAlreadyRated] 区分"undo 回到刚评过的首张卡"和"后续 sibling 卡":
     * - 用户评 GOOD 卡 A(p1) → 推进到 sibling 卡 B(p1) → 显示 sibling 提示(正确)
     * - 用户 undo → 回到卡 A(p1) → 卡 A 的 pointId 在 ratedPointIds 中,
     *   但卡 A 是首张评分卡不是 sibling,不应显示提示
     *
     * 通过比较 [CardItem.id] 与 [ratedPointFirstCardIds][pointId] 判断:
     * - 相等:当前卡是该 pointId 的首张评分卡(undo 回退场景),不是 sibling
     * - 不等:当前卡是后续 sibling 卡,显示提示
     *
     * 与 [ratedPointIds] 一致,undo 不回退(避免重新评分时 sibling 判断错乱),
     * retry 时清空。
     */
    private val ratedPointFirstCardIds = mutableMapOf<String, String>()

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

    private val _uiState = MutableStateFlow<CardsUiState>(CardsUiState(isLoading = true))
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

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
     * 当前卡片是否为"已评分 sibling 卡"(v0.8.9 新增,修复 P1-2)。
     *
     * - true:当前卡 [CardItem.pointId] 已在 [ratedPointIds] 中(同知识点的兄弟卡已评分过)
     * - UI 据此隐藏 [currentPreviews] 的预期间隔显示,避免误导用户
     *   (sibling 卡评分不会触发 FSRS 调度,显示"GOOD→6天"是误导)
     * - UI 改为显示"已调度(同知识点首卡已评分)"提示
     *
     * v0.8.13 P1-1 修复:区分"undo 回到刚评过的首张卡"和"后续 sibling 卡"。
     * 原实现仅判断 pointId in ratedPointIds,导致用户 undo 回到首张评分卡时
     * 也显示 sibling 提示(语义错误:首张卡不是 sibling,它是被评分的那张)。
     * 现增加 [ratedPointFirstCardIds] 判断:若当前卡 id == 该 pointId 的首张评分卡 id,
     * 说明是 undo 回退场景,不是 sibling,返回 false。
     *
     * 注意:声明顺序必须在 [_uiState] 和 [ratedPointIds] 之后,否则初始化时
     * 它们还未初始化(backing field 为 null),会导致 NPE。
     * stateIn 持有 viewModelScope 和 SharingStarted.Eagerly,会立即开始收集,
     * 但 map 的 lambda 是惰性执行的(Flow emit 时才执行),此时 [ratedPointIds]
     * 已完成初始化。
     */
    val isSiblingAlreadyRated: StateFlow<Boolean> = _uiState
        .map { state ->
            val card = state.currentCard ?: return@map false
            val pointId = card.pointId
            if (pointId.isBlank() || pointId !in ratedPointIds) return@map false
            // v0.8.13 P1-1:当前卡是该 pointId 的首张评分卡(undo 回退场景),不是 sibling
            val firstCardId = ratedPointFirstCardIds[pointId]
            firstCardId != null && firstCardId != card.id
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

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
                    // v0.8.14 P1-7 修复:原仅取 e.message 丢失堆栈,生产排查困难。
                    // 现加 Log.e 输出完整堆栈,UI 仍只展示 message(用户无需看堆栈)。
                    android.util.Log.e("CardsViewModel", "loadCards failed", e)
                    emit(CardsUiState(error = e.message ?: "加载失败"))
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
                    if (card == null || card.pointId.isBlank()) {
                        _currentPreviews.value = emptyMap()
                        return@collectLatest
                    }
                    val templateType = try {
                        CardTemplateType.valueOf(card.cardType)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                    if (templateType == null) {
                        _currentPreviews.value = emptyMap()
                        return@collectLatest
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
        if (pointId.isBlank()) {
            ratingHistory.addLast(RatingStep(rating = rating, pointId = ""))
            return
        }

        // v0.8.5 P0：sibling 去重 — 同 pointId 仅第一次评分触发 FSRS 调度
        val shouldSchedule = pointId !in ratedPointIds
        if (shouldSchedule) {
            ratedPointIds.add(pointId)
            // v0.8.13 P1-1:记录该 pointId 的首张评分卡 id,
            // 供 isSiblingAlreadyRated 区分 undo 回退场景
            ratedPointFirstCardIds[pointId] = current.id
        }

        // v0.8.8:入栈评分历史,undo 时据此回退 sessionReviewedCount/sessionAgainCount
        ratingHistory.addLast(RatingStep(rating = rating, pointId = pointId))

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
                    val updated = try {
                        schedulingRepository.rateCard(pointId, fsrsRating, templateType)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // v0.8.12 P1-3:错误优先级调度失败 > 学习进度 > 错题,用 hasSchedulingError 标记
                        _errorMessage.value = "评分调度失败：${e.message ?: "未知错误"}"
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
                        // 修复:从 updated.failCount 反推 oldFailCount:
                        // - AGAIN 评分:FSRS-6 中 lapses+1,所以 oldFailCount = updated.failCount - 1
                        // - GOOD/HARD/EASY 评分:lapses 不变,oldFailCount = updated.failCount
                        //   (但此时 updated.failCount 不会刚跨阈值,因为非 AGAIN 评分不增加 failCount,
                        //    若 updated.failCount >= 8 说明之前已 >= 8,oldFailCount 也 >= 8,不触发警告)
                        //
                        // 仅当内存有记录时优先用内存值(更准确,反映本次会话内的连续 AGAIN 序列),
                        // 内存无记录时才反推(进程恢复场景)。
                        val oldFailCount = lastFailCounts[pointId] ?: when (fsrsRating) {
                            Rating.AGAIN -> (updated.failCount - 1).coerceAtLeast(0)
                            else -> updated.failCount
                        }

                        // 更新 failCount 跟踪
                        lastFailCounts[pointId] = updated.failCount

                        // v0.8.12 P1-1:Leech 检测改为"新增 leech"
                        // 原实现用累计 failCount >= 8,导致达到阈值后每次评分都弹警告
                        // 现仅当 oldFailCount < 8 && newFailCount >= 8 时弹警告(首次跨阈值)
                        if (oldFailCount < LEECH_THRESHOLD && updated.failCount >= LEECH_THRESHOLD) {
                            _leechWarnings.value = _leechWarnings.value + LeechWarning(
                                message = "这张卡片已连续答错 ${updated.failCount} 次，" +
                                    "建议查看知识点详情重新理解，或问 AI 助手辅助。",
                                pointId = pointId,
                            )
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
                            if (_errorMessage.value == null ||
                                !_errorMessage.value!!.startsWith("评分调度失败")
                            ) {
                                _errorMessage.value = "学习进度记录失败：${e.message ?: "未知错误"}"
                            }
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
                    if (_errorMessage.value == null ||
                        !_errorMessage.value!!.startsWith("评分调度失败")
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
     * 撤销上一张卡片（仅 UI 回退，不回滚 FSRS 调度）。
     *
     * v0.8.5 P1 新增：
     * - 参考 Anki 的 Z 键撤销，但简化为仅回退 UI 索引。
     * - FSRS 调度不可逆（已写入 memo_records + review_logs），
     *   撤销仅让用户回看上一张卡片内容。
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
     * 重新评分时 shouldSchedule=false(调度被"吞"),用户 UI 回退但 FSRS 保持第一次结果。
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
        // v0.8.12 P0:不再回退 ratedPointIds,避免重新评分导致 FSRS 重复调度
    }

    /** 清除错误提示 */
    fun clearError() {
        _errorMessage.value = null
    }

    /** 清除当前 Leech 警告(队首),显示队列中下一个(若有) */
    fun clearLeechWarning() {
        _leechWarnings.value = _leechWarnings.value.drop(1)
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
        // v0.8.13 P1-1:同步清空首张评分卡记录,避免 retry 后 isSiblingAlreadyRated 误判
        ratedPointFirstCardIds.clear()
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
        _retryTrigger.value++
    }

    /** 将 [CardTemplate] 映射为 UI 层 [CardItem] */
    private fun CardTemplate.toUiItem(index: Int): CardItem = CardItem(
        // v0.8.8:稳定 ID(替代 index-based),基于 pointId+类型+内容哈希
        // sibling 卡(同 pointId 同类型)靠 front 内容区分,保证 ID 唯一且稳定
        //
        // v0.8.14 P1-1 修复:原用 `front.take(16).hashCode()` 仅取前 16 字符,
        // 两张同 pointId 卡如果 front 前 16 字符相同(如"建安风骨 — 时代特征"和
        // "建安风骨 — 时代背景"前 16 字符都是"建安风骨 — 时代"),ID 完全相同,
        // 导致 ratedPointFirstCardIds 判断错乱、Compose key 重复。
        // 现用全文 hashCode,降低碰撞概率(仍非密码学安全,但业务场景足够)。
        id = buildString {
            append(templateType.name)
            append('_')
            if (pointId.isNotBlank()) append(pointId) else append(front.hashCode())
            append('_')
            append(front.hashCode())
        },
        front = front,
        back = back,
        cardType = templateType.name,
        pointId = pointId,
        template = this,
    )

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
 *
 * v0.8.18 清理:原 `triggeredSchedule: Boolean` 字段已删除。
 * v0.8.12 P0 修复后,undo 不再回退 [CardsViewModel.ratedPointIds](避免重新评分触发
 * FSRS 重复调度导致 stability 异常增长),该字段失去消费者,成为死代码。
 */
private data class RatingStep(
    val rating: CardRating?,
    val pointId: String,
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
