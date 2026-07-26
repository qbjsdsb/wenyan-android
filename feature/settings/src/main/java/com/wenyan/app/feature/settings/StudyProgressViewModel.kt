package com.wenyan.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.StudyProgressRepository
import com.wenyan.app.core.database.entity.StudyProgressEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 学习进度 ViewModel(P0 v0.7.2 新增)。
 *
 * 为 [SettingsScreen] 的 [StudyProgressCard] 提供学习进度数据。
 * 观察单行 study_progress 记录,卡片复习评分时由 CardsViewModel 写入。
 *
 * v0.8.13 P2-1 修复:用 sealed [StudyProgressUiState] 区分 Loading / Loaded。
 * 原实现 `progress: StateFlow<StudyProgressEntity?>` 的 `initialValue = null`
 * 与"加载中"语义重合,UI 在加载阶段把 null 当成"streak=0",误导用户
 * 显示"连续学习 0 天 / 开始今天的学习吧"。现用 [StudyProgressUiState.Loading]
 * 显式表示加载态,首条 DB emit 后转为 [StudyProgressUiState.Loaded]。
 *
 * 注:[StudyProgressRepository.observeProgress] 永不 emit null(null 行已转为默认实体),
 * 故无需 NoData 态——"新用户未学习"对应 [StudyProgressUiState.Loaded] 中
 * streakDays=0/lastVisitedAt=null 的默认实体,UI 已据此隐藏"上次学习"行。
 */
@HiltViewModel
class StudyProgressViewModel @Inject constructor(
    studyProgressRepository: StudyProgressRepository,
) : ViewModel() {

    val uiState: StateFlow<StudyProgressUiState> = studyProgressRepository.observeProgress()
        .map<StudyProgressEntity, StudyProgressUiState> { entity ->
            StudyProgressUiState.Loaded(entity)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudyProgressUiState.Loading,
        )
}

/**
 * 学习进度 UI 状态(v0.8.13 P2-1 新增)。
 *
 * - [Loading]: 初始值,DB 首条 emit 前
 * - [Loaded]: DB emit 后(含默认空实体,即新用户未学习场景)
 */
sealed interface StudyProgressUiState {
    data object Loading : StudyProgressUiState
    data class Loaded(val entity: StudyProgressEntity) : StudyProgressUiState
}
