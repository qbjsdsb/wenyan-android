package com.wenyan.app.core.data.writing

import com.wenyan.app.core.data.repository.WritingSessionStore
import com.wenyan.app.core.database.entity.WritingSessionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WritingAutosaveControllerTest {
    @Test fun `debounce saves only latest and retry recovers`() = runTest {
        val store = FakeStore()
        val results = mutableListOf<Result<Unit>>()
        val controller = WritingAutosaveController(this, store, 500, results::add)

        controller.submit(entity("a"))
        advanceTimeBy(300)
        controller.submit(entity("b"))
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf("b"), store.saved.map { it.body })

        store.fail = true
        controller.submit(entity("c"))
        advanceTimeBy(500)
        runCurrent()
        assertTrue(results.last().isFailure)
        store.fail = false
        controller.retry()
        runCurrent()
        assertEquals("c", store.saved.last().body)
    }

    @Test fun `flush persists latest edit before leaving`() = runTest {
        val store = FakeStore()
        val controller = WritingAutosaveController(this, store, 10_000) {}
        controller.submit(entity("not-yet-debounced"))
        assertTrue(controller.flush().isSuccess)
        assertEquals("not-yet-debounced", store.saved.single().body)
    }

    @Test fun `flush reports failure so caller can keep the editor open`() = runTest {
        val store = FakeStore().apply { fail = true }
        val controller = WritingAutosaveController(this, store, 10_000) {}
        controller.submit(entity("failed"))

        assertTrue(controller.flush().isFailure)
        assertTrue(store.saved.isEmpty())
    }

    private fun entity(body: String) = WritingSessionEntity(
        "id", null, null, null, "MICRO_30_MIN", "prompt", "", "", "[]", "[]", body,
        "DRAFT", 1_800_000, null, 0, null, 0, null, "", 0, 0,
    )

    private class FakeStore : WritingSessionStore {
        val saved = mutableListOf<WritingSessionEntity>()
        var fail = false
        override suspend fun create(session: WritingSessionEntity) = Unit
        override suspend fun save(session: WritingSessionEntity) { if (fail) error("disk"); saved += session }
        override suspend fun get(id: String): WritingSessionEntity? = null
        override fun observe(id: String): Flow<WritingSessionEntity?> = emptyFlow()
        override fun observeCompleted(): Flow<List<WritingSessionEntity>> = emptyFlow()
    }
}
