package com.wenyan.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.StudyProgressRepository
import com.wenyan.app.core.database.entity.StudyProgressEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 学习进度 ViewModel(P0 v0.7.2 新增)。
 *
 * 为 [SettingsScreen] 的 [StudyProgressCard] 提供学习进度数据。
 * 观察单行 study_progress 记录,卡片复习评分时由 CardsViewModel 写入。
 */
@HiltViewModel
class StudyProgressViewModel @Inject constructor(
    studyProgressRepository: StudyProgressRepository,
) : ViewModel() {

    val progress: StateFlow<StudyProgressEntity?> = studyProgressRepository.observeProgress()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
