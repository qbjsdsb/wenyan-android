package com.wenyan.app.feature.quiz

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 错题本 ViewModel(NF-PP5 Wave 3.2)。
 *
 * 功能:
 * - 观察 [WrongAnswerRepository.observeUnresolved] / [WrongAnswerRepository.observeAll],
 *   通过 [filter] 切换
 * - [markResolved]:标记错题为已解决(从"未解决"列表移除)
 * - [deleteById]:永久删除错题记录
 *
 * @property wrongAnswerRepository 错题仓库
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WrongAnswerViewModel @Inject constructor(
    private val wrongAnswerRepository: WrongAnswerRepository,
) : ViewModel() {

    /** 当前过滤模式(默认未解决,这是用户最常看的视图) */
    private val _filter = MutableStateFlow(WrongAnswerFilter.UNRESOLVED)
    val filter: StateFlow<WrongAnswerFilter> = _filter.asStateFlow()

    /** 错误提示(markResolved / deleteById 失败时设置) */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 错题列表 UI 状态。
     *
     * [filter] 切换时通过 [flatMapLatest] 自动取消上一个订阅,
     * 切换到对应的 observe 流。
     */
    val uiState: StateFlow<WrongAnswerUiState> = _filter
        .flatMapLatest { currentFilter ->
            when (currentFilter) {
                WrongAnswerFilter.UNRESOLVED -> wrongAnswerRepository.observeUnresolved()
                WrongAnswerFilter.ALL -> wrongAnswerRepository.observeAll()
            }
        }
        .catch { e ->
            emit(emptyList())
            _errorMessage.value = "加载失败：${e.message ?: "未知错误"}"
        }
        .map { items -> WrongAnswerUiState(items = items.map { it.toUiItem() }) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, WrongAnswerUiState(isLoading = true))

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
                _errorMessage.value = "标记失败：${e.message ?: "未知错误"}"
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
                _errorMessage.value = "删除失败：${e.message ?: "未知错误"}"
            }
        }
    }

    /** 清除错误提示 */
    fun clearError() {
        _errorMessage.value = null
    }

    /** 将 [WrongAnswerEntity] 转换为 UI 列表项 */
    private fun WrongAnswerEntity.toUiItem(): WrongAnswerItem = WrongAnswerItem(
        id = id,
        pointId = pointId,
        examQuestionId = examQuestionId,
        userAnswer = userAnswer,
        correctAnswer = correctAnswer,
        source = source,
        wrongCount = wrongCount,
        lastWrongAt = lastWrongAt,
        isResolved = resolvedAt != null,
        createdAt = createdAt,
    )
}

/** 错题过滤模式 */
enum class WrongAnswerFilter {
    /** 未解决(resolvedAt IS NULL) */
    UNRESOLVED,

    /** 全部(含已解决) */
    ALL,
}

/** 错题本 UI 状态 */
data class WrongAnswerUiState(
    val isLoading: Boolean = false,
    val items: List<WrongAnswerItem> = emptyList(),
)

/**
 * 错题列表项(与 [WrongAnswerEntity] 解耦的 UI 层模型)。
 *
 * @property isResolved 是否已解决(从 resolvedAt 派生)
 */
@Immutable
data class WrongAnswerItem(
    val id: String,
    val pointId: String?,
    val examQuestionId: String?,
    val userAnswer: String,
    val correctAnswer: String?,
    val source: String,
    val wrongCount: Int,
    val lastWrongAt: Long,
    val isResolved: Boolean,
    val createdAt: Long,
)
