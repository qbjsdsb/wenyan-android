package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.common.util.friendlyErrorMessage
import com.wenyan.app.core.data.repository.WritingEvidenceDetail
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private val evidenceRepository: WritingEvidenceSource,
    private val clock: WritingClock,
) : ViewModel() {
    private val id = savedStateHandle.get<String>(KEY_SESSION_ID)
        ?: UUID.randomUUID().toString().also { savedStateHandle[KEY_SESSION_ID] = it }
    private val materialId = savedStateHandle.get<String>(KEY_MATERIAL_ID)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    private val _state = MutableStateFlow(WritingEditorState())
    val state: StateFlow<WritingEditorState> = _state.asStateFlow()
    private var loadJob: Job? = null
    private var activeTimer: ActiveWritingTimer? = null
    private val autosave = WritingAutosaveController(viewModelScope, store) { result ->
        _state.value = _state.value.copy(
            saving = false,
            saveError = result.exceptionOrNull()?.let(::friendlyErrorMessage),
        )
    }

    init {
        loadSession()
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

    private fun loadSession() {
        loadJob?.cancel()
        _state.value = _state.value.copy(loaded = false, saveError = null)
        loadJob = viewModelScope.launch {
            val session = try {
                val existing = store.get(id)
                existing ?: run {
                    val selectedMaterial = materialId?.let { targetId ->
                        evidenceRepository.get(targetId)
                    }
                    emptySession(id, selectedMaterial).also { store.create(it) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.value = _state.value.copy(saveError = friendlyErrorMessage(error), loaded = true)
                return@launch
            }
            activateTimerIfRunning(session)
            _state.value = _state.value.copy(
                session = session,
                elapsedMs = restoredElapsed(session),
                loaded = true,
            )
        }
    }

    fun edit(transform: (WritingSessionEntity) -> WritingSessionEntity) {
        val current = _state.value.session ?: return
        if (current.state == WritingSessionState.COMPLETED.name ||
            current.state == WritingSessionState.DISCARDED.name
        ) return
        val next = transform(current).copy(updatedAt = clock.wallTimeMs())
        _state.value = _state.value.copy(session = next, saving = true, saveError = null)
        autosave.submit(next)
    }

    fun retrySave() {
        if (_state.value.session == null) loadSession() else autosave.retry()
    }
    fun refreshElapsed() { activeTimer?.let { _state.value = _state.value.copy(elapsedMs = it.elapsedAt(clock.monotonicMs())) } }
    fun flushAndThen(action: () -> Unit) = viewModelScope.launch {
        val result = autosave.flush()
        if (result.isSuccess) {
            action()
        } else {
            _state.value = _state.value.copy(
                saving = false,
                saveError = result.exceptionOrNull()?.let(::friendlyErrorMessage) ?: "离线保存失败，请重试",
            )
        }
    }
    fun selectMode(mode: WritingMode) = edit { it.copy(mode = mode.name, targetDurationMs = mode.durationMs) }
    fun discard() {
        val current = _state.value.session ?: return
        if (current.state == WritingSessionState.COMPLETED.name ||
            current.state == WritingSessionState.DISCARDED.name
        ) return
        val elapsed = activeTimer?.elapsedAt(clock.monotonicMs()) ?: _state.value.elapsedMs
        activeTimer = null
        edit {
            it.copy(
                state = WritingSessionState.DISCARDED.name,
                elapsedBeforePauseMs = elapsed,
                startedAt = null,
                pausedAt = clock.wallTimeMs(),
            )
        }
        _state.value = _state.value.copy(elapsedMs = elapsed)
    }
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

    fun resume() {
        val current = _state.value.session ?: return
        if (current.state == WritingSessionState.COMPLETED.name ||
            current.state == WritingSessionState.DISCARDED.name
        ) return
        edit {
            val now = clock.wallTimeMs()
            val resumed = it.copy(state = WritingSessionState.RUNNING.name, startedAt = now, pausedAt = null)
            activeTimer = ActiveWritingTimer(resumed.toTimer(), now, clock.monotonicMs())
            resumed
        }
    }

    fun complete() {
        val current = _state.value.session ?: return
        if (current.state == WritingSessionState.COMPLETED.name ||
            current.state == WritingSessionState.DISCARDED.name
        ) return
        val now = clock.wallTimeMs()
        val elapsed = activeTimer?.elapsedAt(clock.monotonicMs()) ?: _state.value.elapsedMs
        activeTimer = null
        edit {
            it.copy(
                state = WritingSessionState.COMPLETED.name,
                elapsedBeforePauseMs = elapsed,
                startedAt = null,
                pausedAt = now,
                completedAt = now,
            )
        }
        _state.value = _state.value.copy(elapsedMs = elapsed)
        viewModelScope.launch { autosave.flush() }
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

    private fun emptySession(id: String, material: WritingEvidenceDetail?): WritingSessionEntity {
        val now = clock.wallTimeMs()
        val promptSnapshot = material?.let { selected ->
            buildString {
                selected.title?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    append(it)
                    append("\n\n")
                }
                append(selected.content.ifBlank { "610 写作练习" })
            }
        } ?: "610 写作练习"
        val evidenceRefs = material
            ?.takeIf { it.isCitable }
            ?.let { encodeEvidenceRefs(listOf(it.id)) }
            ?: "[]"
        return WritingSessionEntity(
            id, null, null, null, WritingMode.MICRO_30_MIN.name, promptSnapshot, "", "", "[]", evidenceRefs, "",
            WritingSessionState.DRAFT.name, WritingMode.MICRO_30_MIN.durationMs, null, 0, null, now, null, "", now, now,
        )
    }

    private companion object {
        const val KEY_SESSION_ID = "sessionId"
        const val KEY_MATERIAL_ID = "materialId"
    }
}
