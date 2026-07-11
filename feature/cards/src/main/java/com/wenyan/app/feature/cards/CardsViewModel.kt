package com.wenyan.app.feature.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.cards.CardTemplate
import com.wenyan.app.core.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 记忆卡片模块 ViewModel。
 *
 * 管理 UI 状态：当前卡片 + 翻转状态 + FSRS 评分交互。
 *
 * 数据来源：[CardRepository.getCardsForReview] 返回已验证知识点经
 * [com.wenyan.app.core.data.cards.CardSplitter] 拆分的卡片流。
 *
 * 评分调度（rateCard）当前仅推进卡片索引。完整的 FSRS 调度需要：
 * 1. 在 DataModule 中提供 FsrsWrapper（需三档参数 TierFsrsConfig）
 * 2. 新增 SchedulingRepository：将 CardTemplate 映射回 MemoRecord，
 *    调用 FsrsWrapper.schedule() 后持久化到 memo_records 表
 * 3. 将 FlashCard（FSRS内部模型）与 CardTemplate（UI模型）解耦
 * 以上属于独立 Task，不在本次 P1 连通范围内。
 */
@HiltViewModel
class CardsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
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
     * FSRS 评分（Again/Hard/Good/Easy），推进到下一张卡。
     *
     * TODO（FSRS调度集成Task）：
     * - 将评分映射为 [com.wenyan.app.core.fsrs.Rating]
     * - 通过 SchedulingRepository 查找当前卡片对应的 MemoRecord
     * - 调用 FsrsWrapper.schedule(flashCard, rating) 计算新调度
     * - 持久化更新后的 MemoRecord（stability/difficulty/dueDate/state）
     * - 根据评分档位（TIER_EXACT/TIER_FRAMEWORK/TIER_UNDERSTAND）应用三档参数
     */
    fun rateCard(rating: CardRating) {
        _isFlipped.update { false }
        _currentIndex.update { it + 1 }
    }

    /** 将 [CardTemplate] 映射为 UI 层 [CardItem] */
    private fun CardTemplate.toUiItem(index: Int): CardItem = CardItem(
        id = "${templateType.name}_$index",
        front = front,
        back = back,
        cardType = templateType.name,
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
)

// FSRS 评分等级
enum class CardRating {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}
