package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import com.wenyan.app.core.data.repository.WritingEvidenceDetail
import com.wenyan.app.core.data.repository.WritingEvidenceItem
import com.wenyan.app.core.data.repository.WritingEvidenceSource
import com.wenyan.app.core.data.repository.WritingSessionStore
import com.wenyan.app.core.data.writing.WritingClock
import com.wenyan.app.core.data.writing.decodeEvidenceRefs
import com.wenyan.app.core.database.entity.ContentReviewStatus
import com.wenyan.app.core.database.entity.WritingSessionEntity
import com.wenyan.app.core.database.entity.WritingSessionState
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

    @Test fun `new session seeded from material keeps full prompt and reviewed evidence`() = runTest(dispatcher) {
        val store = FakeStore(null)
        val reviewed = WritingEvidenceItem(
            "reviewed", "已核素材", "预览", ContentReviewStatus.REVIEWED, emptyList(),
        )
        val viewModel = WritingEditorViewModel(
            SavedStateHandle(mapOf("materialId" to "reviewed")),
            store,
            FakeEvidence(
                listOf(reviewed),
                mapOf("reviewed" to WritingEvidenceDetail("reviewed", "已核素材", "完整素材正文", ContentReviewStatus.REVIEWED)),
            ),
            FakeClock(),
        )
        runCurrent()

        assertEquals("已核素材\n\n完整素材正文", viewModel.state.value.session?.promptSnapshot)
        assertEquals(listOf("reviewed"), decodeEvidenceRefs(viewModel.state.value.session?.evidenceRefsJson.orEmpty()))
    }

    @Test fun `draft can start the monotonic writing timer`() = runTest(dispatcher) {
        val clock = FakeClock(wall = 5_000, monotonic = 2_000)
        val viewModel = WritingEditorViewModel(
            SavedStateHandle(), FakeStore(null), FakeEvidence(emptyList()), clock,
        )
        runCurrent()

        viewModel.resume()

        assertEquals(WritingSessionState.RUNNING.name, viewModel.state.value.session?.state)
        assertEquals(5_000L, viewModel.state.value.session?.startedAt)
    }

    @Test fun `complete stops timer persists elapsed and makes session read only`() = runTest(dispatcher) {
        val clock = FakeClock(wall = 10_000, monotonic = 1_000)
        val store = FakeStore(session("running", state = WritingSessionState.RUNNING.name, startedAt = 9_000))
        val viewModel = WritingEditorViewModel(
            SavedStateHandle(mapOf("sessionId" to "running")), store, FakeEvidence(emptyList()), clock,
        )
        runCurrent()

        clock.monotonic = 2_000
        clock.wall = 11_000
        viewModel.complete()
        advanceTimeBy(750)
        runCurrent()

        val completed = store.saved.last()
        assertEquals(WritingSessionState.COMPLETED.name, completed.state)
        assertEquals(2_000L, completed.elapsedBeforePauseMs)
        assertEquals(11_000L, completed.completedAt)
        val bodyBefore = viewModel.state.value.session?.body
        viewModel.edit { it.copy(body = "不得修改") }
        assertEquals(bodyBefore, viewModel.state.value.session?.body)
    }

    @Test fun `initial load failure retry actually reloads the session`() = runTest(dispatcher) {
        val store = FakeStore(null).apply { failGet = true }
        val viewModel = WritingEditorViewModel(
            SavedStateHandle(), store, FakeEvidence(emptyList()), FakeClock(),
        )
        runCurrent()
        assertTrue(viewModel.state.value.session == null)
        assertTrue(viewModel.state.value.saveError != null)

        store.failGet = false
        viewModel.retrySave()
        runCurrent()

        assertTrue(viewModel.state.value.loaded)
        assertTrue(viewModel.state.value.session != null)
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
        var failGet = false
        private val completed = MutableStateFlow<List<WritingSessionEntity>>(emptyList())
        override suspend fun create(session: WritingSessionEntity) { current = session }
        override suspend fun save(session: WritingSessionEntity) { current = session; saved += session }
        override suspend fun get(id: String): WritingSessionEntity? {
            if (failGet) error("load")
            return current?.takeIf { it.id == id }
        }
        override fun observe(id: String): Flow<WritingSessionEntity?> = MutableStateFlow(current)
        override fun observeCompleted(): Flow<List<WritingSessionEntity>> = completed
    }

    private class FakeEvidence(
        items: List<WritingEvidenceItem>,
        private val details: Map<String, WritingEvidenceDetail> = emptyMap(),
    ) : WritingEvidenceSource {
        override val evidence = MutableStateFlow(items)
        override suspend fun get(id: String): WritingEvidenceDetail? = details[id]
            ?: evidence.value.firstOrNull { it.id == id }?.let {
                WritingEvidenceDetail(it.id, it.title, it.preview, it.reviewStatus)
            }
    }

    private class FakeClock(var wall: Long = 1_000, var monotonic: Long = 1_000) : WritingClock {
        override fun wallTimeMs() = wall
        override fun monotonicMs() = monotonic
    }
}
