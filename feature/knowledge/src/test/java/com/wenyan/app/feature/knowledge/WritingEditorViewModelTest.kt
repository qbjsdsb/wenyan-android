package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import com.wenyan.app.core.data.repository.WritingEvidenceItem
import com.wenyan.app.core.data.repository.WritingEvidenceSource
import com.wenyan.app.core.data.repository.WritingSessionStore
import com.wenyan.app.core.data.writing.WritingClock
import com.wenyan.app.core.data.writing.decodeEvidenceRefs
import com.wenyan.app.core.database.entity.ContentReviewStatus
import com.wenyan.app.core.database.entity.WritingSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WritingEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `saved session id restores draft and only reviewed evidence can be cited`() = runTest(dispatcher) {
        val store = FakeStore(session("saved", body = "进程前草稿"))
        val reviewed = WritingEvidenceItem("reviewed", "已核素材", "内容", ContentReviewStatus.REVIEWED, emptyList())
        val legacy = WritingEvidenceItem("legacy", null, "待核", ContentReviewStatus.LEGACY_UNVERIFIED, emptyList())
        val viewModel = WritingEditorViewModel(
            SavedStateHandle(mapOf("sessionId" to "saved")), store, FakeEvidence(listOf(reviewed, legacy)), FakeClock(),
        )
        runCurrent()

        assertEquals("进程前草稿", viewModel.state.value.session?.body)
        viewModel.toggleEvidence(legacy)
        assertTrue(decodeEvidenceRefs(viewModel.state.value.session!!.evidenceRefsJson).isEmpty())
        viewModel.toggleEvidence(reviewed)
        advanceTimeBy(750)
        runCurrent()
        assertEquals(listOf("reviewed"), decodeEvidenceRefs(store.saved.last().evidenceRefsJson))
    }

    @Test fun `pause uses monotonic elapsed despite wall clock rollback`() = runTest(dispatcher) {
        val clock = FakeClock(wall = 10_000, monotonic = 1_000)
        val store = FakeStore(session("saved", state = "RUNNING", startedAt = 9_000, elapsed = 500))
        val viewModel = WritingEditorViewModel(
            SavedStateHandle(mapOf("sessionId" to "saved")), store, FakeEvidence(emptyList()), clock,
        )
        runCurrent()
        assertEquals(1_500, viewModel.state.value.elapsedMs)

        clock.wall = 100 // simulate a wall-clock rollback after restore
        clock.monotonic = 2_000
        viewModel.pause()
        assertEquals(2_500L, viewModel.state.value.session?.elapsedBeforePauseMs)
    }

    private fun session(
        id: String,
        body: String = "",
        state: String = "DRAFT",
        startedAt: Long? = null,
        elapsed: Long = 0,
    ) = WritingSessionEntity(
        id, null, null, null, "MICRO_30_MIN", "题目快照", "", "", "[]", "[]", body,
        state, 1_800_000, startedAt, elapsed, null, 1, null, "", 1, 1,
    )

    private class FakeStore(initial: WritingSessionEntity?) : WritingSessionStore {
        private var current = initial
        val saved = mutableListOf<WritingSessionEntity>()
        private val completed = MutableStateFlow<List<WritingSessionEntity>>(emptyList())
        override suspend fun create(session: WritingSessionEntity) { current = session }
        override suspend fun save(session: WritingSessionEntity) { current = session; saved += session }
        override suspend fun get(id: String) = current?.takeIf { it.id == id }
        override fun observe(id: String): Flow<WritingSessionEntity?> = MutableStateFlow(current)
        override fun observeCompleted(): Flow<List<WritingSessionEntity>> = completed
    }

    private class FakeEvidence(items: List<WritingEvidenceItem>) : WritingEvidenceSource {
        override val evidence = MutableStateFlow(items)
    }

    private class FakeClock(var wall: Long = 1_000, var monotonic: Long = 1_000) : WritingClock {
        override fun wallTimeMs() = wall
        override fun monotonicMs() = monotonic
    }
}
