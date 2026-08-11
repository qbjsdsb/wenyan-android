package com.wenyan.app.core.data.writing

import com.wenyan.app.core.data.repository.WritingSessionStore
import com.wenyan.app.core.database.entity.WritingSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class WritingAutosaveController(
    private val scope: CoroutineScope,
    private val store: WritingSessionStore,
    private val debounceMs: Long = 750,
    private val onResult: (Result<Unit>) -> Unit,
) {
    private val saveMutex = Mutex()
    private var pending: Job? = null
    private var latest: WritingSessionEntity? = null
    private var generation = 0L

    fun submit(session: WritingSessionEntity) {
        latest = session
        val requestedGeneration = ++generation
        pending?.cancel()
        pending = scope.launch {
            delay(debounceMs)
            persist(session, requestedGeneration)
        }
    }

    fun retry() {
        val session = latest ?: return
        val requestedGeneration = ++generation
        pending?.cancel()
        pending = scope.launch { persist(session, requestedGeneration) }
    }

    suspend fun flush(): Result<Unit> {
        val session = latest ?: return Result.success(Unit)
        val requestedGeneration = ++generation
        pending?.cancel()
        return persist(session, requestedGeneration)
    }

    private suspend fun persist(session: WritingSessionEntity, requestedGeneration: Long): Result<Unit> {
        val result = withContext(NonCancellable) {
            saveMutex.withLock { runCatching { store.save(session) } }
        }
        if (requestedGeneration == generation) onResult(result)
        return result
    }
}
