package com.wenyan.app.feature.cards

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.cards.CardTemplate
import com.wenyan.app.core.data.repository.CardRepository
import com.wenyan.app.core.data.repository.SchedulingRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 记忆卡片模块 ViewModel。
 *
 * 管理 UI 状态：当前卡片 + 翻转状态 + FSRS 评分交互。
 *
 * 数据来源：[CardRepository.getCardsForReview] 返回已验证知识点经
 * [com.wenyan.app.core.data.cards.CardSplitter] 拆分的卡片流。
 *
 * 评分调度（阶段3接通）：
 * - [rateCard] 先推进卡片索引（保证 UI 流畅），再异步调用
 *   [SchedulingRepository.rateCard] 完成 FSRS 调度回写。
 * - 评分档位（AGAIN/HARD/GOOD/EASY）由 [CardRating] → [Rating] 映射。
 * - tier 由 [CardTemplateType] 推断，SchedulingRepository 内部按 tier 构造 FsrsWrapper。
 *
 * 进程被杀恢复（NF-L2 修复）：
 * - [isFlipped] + [currentIndex] 持久化到 [SavedStateHandle]，进程被杀后恢复卡片位置。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CardsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val cardRepository: CardRepository,
    private val schedulingRepository: SchedulingRepository,
) : ViewModel() {

    // 翻转状态（UI 交互层，NF-L2 修复：持久化到 SavedStateHandle）
    private val _isFlipped = savedStateHandle.getStateFlow("isFlipped", false)

    // 当前卡片索引（UI 交互层，NF-L2 修复：持久化到 SavedStateHandle）
    private val _currentIndex = savedStateHandle.getStateFlow("currentIndex", 0)

    // 错误提示（P1-NEW-4 新增，用于 rateCard 调度失败时反馈）
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 重试触发器（P0-6 新增）。点击重试时自增，[flatMapLatest] 会重新订阅数据流。
     */
    private val _retryTrigger = MutableStateFlow(0)

    /**
     * 卡片 UI 状态。
     *
     * 合并三路流：
     * - [cardRepository.getCardsForReview]：待复习卡片流（数据层）
     * - [_isFlipped]：翻转状态（交互层）
     * - [_currentIndex]：当前索引（交互层）
     *
     * 当卡片列表变化时，[currentIndex] 自动钳制到有效范围。
     *
     * P1-NEW-4 修正：新增 [CardsUiState.isFinished] 标记牌组完成。
     * 原实现 currentIndex 持续累加超过 cards.size 后 currentCard 为 null，UI 显示空白但无完成态，
     * 用户无法区分"加载中"和"已完成"。现当 currentIndex >= cards.size 时标记 isFinished=true。
     *
     * P0-6 修复：加 [catch] 捕获数据流异常，避免异常冒泡导致 app 崩溃。
     *
     * P1-4 修复：原 [stateIn] 模式 retry() 后 UI 仍显示旧 error 状态无 loading 反馈，
     * 现改为 MutableStateFlow + [collect]，retry() 可立即设置 isLoading=true，
     * 保留 cards/currentIndex/isFlipped 等其他字段不清空。
     */
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
                        val isFinished = cards.isNotEmpty() && currentIndex >= cards.size
                        val safeIndex = if (cards.isEmpty()) {
                            0
                        } else {
                            currentIndex.coerceIn(0, cards.size - 1)
                        }
                        CardsUiState(
                            isLoading = false,
                            cards = cards.mapIndexed { index, card -> card.toUiItem(index) },
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
    }

    /** 翻转当前卡片 */
    fun flipCard() {
        savedStateHandle["isFlipped"] = !_isFlipped.value
    }

    /**
     * FSRS 评分（Again/Hard/Good/Easy），推进到下一张卡并异步完成调度回写。
     *
     * 流程：
     * 1. 读取当前卡片的 pointId 和 cardType
     * 2. 先推进索引 + 重置翻转状态（保证 UI 立即响应）
     * 3. 异步调用 SchedulingRepository.rateCard 完成 FSRS 调度
     *
     * 无 pointId 的卡片（pointId 为空）仅推进索引，不触发调度。
     *
     * P1-NEW-4 修正：schedulingRepository.rateCard 加 try/catch。
     * 原实现若调度回写抛异常（如数据库写入失败），异常会冒泡到协程异常处理器，
     * 用户无任何反馈且 FSRS 状态可能不一致。现捕获异常并设置 errorMessage，
     * UI 可据此提示用户"评分已记录但调度失败，请重试"。
     * 注意：即使调度失败也不回滚 UI 索引（用户已心理上"翻过"这张卡，回滚会造成困惑）。
     */
    fun rateCard(rating: CardRating) {
        val current = uiState.value.currentCard ?: return
        val pointId = current.pointId
        val cardTypeStr = current.cardType

        // 先推进 UI（立即响应）
        savedStateHandle["isFlipped"] = false
        savedStateHandle["currentIndex"] = _currentIndex.value + 1

        // 无 pointId 的卡片仅推进索引
        if (pointId.isBlank()) return

        // 异步完成 FSRS 调度回写
        viewModelScope.launch {
            val fsrsRating = when (rating) {
                CardRating.AGAIN -> Rating.AGAIN
                CardRating.HARD -> Rating.HARD
                CardRating.GOOD -> Rating.GOOD
                CardRating.EASY -> Rating.EASY
            }
            val templateType = try {
                CardTemplateType.valueOf(cardTypeStr)
            } catch (e: IllegalArgumentException) {
                null
            } ?: return@launch
            try {
                schedulingRepository.rateCard(pointId, fsrsRating, templateType)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = "评分调度失败：${e.message ?: "未知错误"}"
            }
        }
    }

    /** 清除错误提示（P1-NEW-4 新增） */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 重试加载（P0-6 新增，P1-4 增强）。
     *
     * P1-4 修复：先立即设置 isLoading=true 并清空 error，保留 cards/currentIndex/isFlipped 不变，
     * 让 UI 立即显示 loading 反馈；再增加 [_retryTrigger] 触发数据流重新订阅。
     */
    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        _retryTrigger.value++
    }

    /** 将 [CardTemplate] 映射为 UI 层 [CardItem] */
    private fun CardTemplate.toUiItem(index: Int): CardItem = CardItem(
        id = "${templateType.name}_$index",
        front = front,
        back = back,
        cardType = templateType.name,
        pointId = pointId,
        template = this,
    )
}

// 卡片 UI 状态
data class CardsUiState(
    val isLoading: Boolean = false,
    val cards: List<CardItem> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    /** 牌组是否已完成（P1-NEW-4 新增，currentIndex >= cards.size 时为 true） */
    val isFinished: Boolean = false,
    /** 加载失败时的错误信息（P0-6 新增） */
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
    /** 关联知识点 ID（阶段3新增，用于 FSRS 调度回写） */
    val pointId: String = "",
    /**
     * 原始卡片模板（阶段5新增，用于 [CardContent] 结构化渲染）。
     * 为 null 时降级为 front/back 纯文本展示。
     */
    val template: CardTemplate? = null,
)

// FSRS 评分等级
enum class CardRating {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}
