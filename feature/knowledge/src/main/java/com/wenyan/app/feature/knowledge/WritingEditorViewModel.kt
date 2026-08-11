package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.WritingEvidenceItem
import com.wenyan.app.core.data.repository.WritingEvidenceSource
import com.wenyan.app.core.data.repository.WritingSessionStore
import com.wenyan.app.core.data.writing.*
import com.wenyan.app.core.database.entity.WritingSessionEntity
import com.wenyan.app.core.database.entity.WritingSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class WritingEditorState(
    val session: WritingSessionEntity? = null,
    val evidence: List<WritingEvidenceItem> = emptyList(),
    val trends: List<DimensionTrend> = emptyList(),
    val elapsedMs: Long = 0,
    val saving: Boolean = false,
    val saveError: String? = null,
    val loaded: Boolean = false,
)

@HiltViewModel
class WritingEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val store: WritingSessionStore,
    evidenceRepository: WritingEvidenceSource,
    private val clock: WritingClock,
) : ViewModel() {
    private val id = savedStateHandle.get<String>(KEY_SESSION_ID)
        ?: UUID.randomUUID().toString().also { savedStateHandle[KEY_SESSION_ID] = it }
    private val _state = MutableStateFlow(WritingEditorState())
    val state: StateFlow<WritingEditorState> = _state.asStateFlow()
    private var activeTimer: ActiveWritingTimer? = null
    private val autosave = WritingAutosaveController(viewModelScope, store) { result ->
        _state.value = _state.value.copy(saving = false, saveError = result.exceptionOrNull()?.message)
    }

    init {
        viewModelScope.launch {
            runCatching {
                val existing = store.get(id)
                existing ?: emptySession(id).also { store.create(it) }
            }.onSuccess { session ->
                activateTimerIfRunning(session)
                _state.value = _state.value.copy(
                    session = session,
                    elapsedMs = restoredElapsed(session),
                    loaded = true,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(saveError = error.message, loaded = true)
            }
        }
        viewModelScope.launch {
            evidenceRepository.evidence.collect { evidence -> _state.value = _state.value.copy(evidence = evidence) }
        }
        viewModelScope.launch {
            store.observeCompleted().collect { sessions ->
                val history = sessions.map { decodeAssessment(it.selfAssessmentJson) }.filter { it.marks.isNotEmpty() }
                _state.value = _state.value.copy(trends = trends(history))
            }
        }
    }

    fun edit(transform: (WritingSessionEntity) -> WritingSessionEntity) {
        val current = _state.value.session ?: return
        val next = transform(current).copy(updatedAt = clock.wallTimeMs())
        _state.value = _state.value.copy(session = next, saving = true, saveError = null)
        autosave.submit(next)
    }

    fun retrySave() = autosave.retry()
    fun refreshElapsed() { activeTimer?.let { _state.value = _state.value.copy(elapsedMs = it.elapsedAt(clock.monotonicMs())) } }
    fun flushAndThen(action: () -> Unit) = viewModelScope.launch { autosave.flush(); action() }
    fun selectMode(mode: WritingMode) = edit { it.copy(mode = mode.name, targetDurationMs = mode.durationMs) }
    fun discard() = edit { it.copy(state = WritingSessionState.DISCARDED.name) }
    fun rate(dimension: RubricDimension, level: RubricLevel) = updateMark(dimension) { it.copy(level = level) }
    fun note(dimension: RubricDimension, note: String) = updateMark(dimension) { it.copy(note = note) }

    fun toggleEvidence(item: WritingEvidenceItem) {
        if (!item.isCitable) return
        edit { session ->
            val current = decodeEvidenceRefs(session.evidenceRefsJson)
            val next = if (item.id in current) current - item.id else current + item.id
            session.copy(evidenceRefsJson = encodeEvidenceRefs(next))
        }
    }

    fun pause() {
        val elapsed = activeTimer?.elapsedAt(clock.monotonicMs()) ?: _state.value.elapsedMs
        activeTimer = null
        edit {
            it.copy(
                state = WritingSessionState.PAUSED.name,
                elapsedBeforePauseMs = elapsed,
                startedAt = null,
                pausedAt = clock.wallTimeMs(),
            )
        }
        _state.value = _state.value.copy(elapsedMs = elapsed)
    }

    fun resume() = edit {
        val now = clock.wallTimeMs()
        val resumed = it.copy(state = WritingSessionState.RUNNING.name, startedAt = now, pausedAt = null)
        activeTimer = ActiveWritingTimer(resumed.toTimer(), now, clock.monotonicMs())
        resumed
    }

    fun complete() {
        pause()
        edit { it.copy(state = WritingSessionState.COMPLETED.name, completedAt = clock.wallTimeMs()) }
    }

    private fun updateMark(dimension: RubricDimension, transform: (RubricMark) -> RubricMark) = edit { session ->
        val assessment = decodeAssessment(session.selfAssessmentJson)
        val current = assessment.marks.firstOrNull { it.dimension == dimension }
            ?: RubricMark(dimension, RubricLevel.NEEDS_WORK)
        val marks = assessment.marks.filterNot { it.dimension == dimension } + transform(current)
        session.copy(selfAssessmentJson = encodeAssessment(RubricAssessment(marks)))
    }

    private fun activateTimerIfRunning(session: WritingSessionEntity) {
        if (session.state == WritingSessionState.RUNNING.name) {
            activeTimer = ActiveWritingTimer(session.toTimer(), clock.wallTimeMs(), clock.monotonicMs())
        }
    }

    private fun restoredElapsed(session: WritingSessionEntity) = elapsedMs(session.toTimer(), clock.wallTimeMs())
    private fun WritingSessionEntity.toTimer() = PersistedTimer(startedAt, elapsedBeforePauseMs, state == WritingSessionState.PAUSED.name)

    private fun emptySession(id: String): WritingSessionEntity {
        val now = clock.wallTimeMs()
        return WritingSessionEntity(
            id, null, null, null, WritingMode.MICRO_30_MIN.name, "610 写作练习", "", "", "[]", "[]", "",
            WritingSessionState.DRAFT.name, WritingMode.MICRO_30_MIN.durationMs, null, 0, null, now, null, "", now, now,
        )
    }

    private companion object {
        const val KEY_SESSION_ID = "sessionId"
    }
}
