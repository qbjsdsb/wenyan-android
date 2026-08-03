package com.wenyan.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.CardFrequencyFilter
import com.wenyan.app.core.data.repository.CardSettings
import com.wenyan.app.core.data.repository.CardSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 卡片备考设置 ViewModel（v0.9.29）。
 *
 * 暴露 [cardSettings]（默认 60 张/天 + HIGH_MEDIUM + 四科全选 + 考试日期可设），
 * 提供各字段 setter 持久化到 [CardSettingsRepository]。
 */
@HiltViewModel
class CardSettingsViewModel @Inject constructor(
    private val cardSettingsRepository: CardSettingsRepository,
) : ViewModel() {

    val cardSettings: StateFlow<CardSettings> = cardSettingsRepository.cardSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardSettings())

    fun setDailyNewLimit(limit: Int) {
        viewModelScope.launch { cardSettingsRepository.setDailyNewLimit(limit) }
    }

    fun setFrequencyFilter(filter: CardFrequencyFilter) {
        viewModelScope.launch { cardSettingsRepository.setFrequencyFilter(filter) }
    }

    fun setSubjectFilters(subjects: Set<String>) {
        viewModelScope.launch { cardSettingsRepository.setSubjectFilters(subjects) }
    }

    fun setExamDate(millis: Long?) {
        viewModelScope.launch { cardSettingsRepository.setExamDate(millis) }
    }
}
