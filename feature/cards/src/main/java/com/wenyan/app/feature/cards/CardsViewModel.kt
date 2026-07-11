package com.wenyan.app.feature.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.cards.CardTemplate
import com.wenyan.app.core.data.repository.CardRepository
import com.wenyan.app.core.data.repository.SchedulingRepository
import com.wenyan.app.core.database.entity.CardTemplateType
import com.wenyan.app.core.fsrs.Rating
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
 */
@HiltViewModel
class CardsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val schedulingRepository: SchedulingRepository,
) : ViewModel() {

    // 翻转状态（UI 交互层）
    private val _isFlipped = MutableStateFlow(false)

    // 当前卡片索引（UI 交互层）
    private val _currentIndex = MutableStateFlow(0)

    /**
     * 卡片 UI 状态。
     *
     * 合并三路流：
     * - [cardRepository.getCardsForReview]：待复习卡片流（数据层）
     * - [_isFlipped]：翻转状态（交互层）
     * - [_currentIndex]：当前索引（交互层）
     *
     * 当卡片列表变化时，[currentIndex] 自动钳制到有效范围。
     */
    val uiState: StateFlow<CardsUiState> = combine(
        cardRepository.getCardsForReview(),
        _isFlipped,
        _currentIndex,
    ) { cards, isFlipped, currentIndex ->
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
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CardsUiState(isLoading = true),
    )

    /** 翻转当前卡片 */
    fun flipCard() {
        _isFlipped.update { !it }
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
     */
    fun rateCard(rating: CardRating) {
        val current = uiState.value.currentCard ?: return
        val pointId = current.pointId
        val cardTypeStr = current.cardType

        // 先推进 UI（立即响应）
        _isFlipped.update { false }
        _currentIndex.update { it + 1 }

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
            schedulingRepository.rateCard(pointId, fsrsRating, templateType)
        }
    }

    /** 将 [CardTemplate] 映射为 UI 层 [CardItem] */
    private fun CardTemplate.toUiItem(index: Int): CardItem = CardItem(
        id = "${templateType.name}_$index",
        front = front,
        back = back,
        cardType = templateType.name,
        pointId = pointId,
    )
}

// 卡片 UI 状态
data class CardsUiState(
    val isLoading: Boolean = false,
    val cards: List<CardItem> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
) {
    val currentCard: CardItem? get() = cards.getOrNull(currentIndex)
}

// 卡片项（UI 层模型，与 core:data 的 CardTemplate 解耦）
data class CardItem(
    val id: String,
    val front: String,
    val back: String,
    val cardType: String,
    /** 关联知识点 ID（阶段3新增，用于 FSRS 调度回写） */
    val pointId: String = "",
)

// FSRS 评分等级
enum class CardRating {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}
