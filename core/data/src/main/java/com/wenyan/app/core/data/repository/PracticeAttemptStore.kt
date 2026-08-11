package com.wenyan.app.core.data.repository

import androidx.room.withTransaction
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.PracticeAttemptEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow

interface PracticeAttemptStore {
    suspend fun save(attempt: PracticeAttemptEntity)
    suspend fun get(id: String): PracticeAttemptEntity?
    fun observeSession(sessionId: String): Flow<List<PracticeAttemptEntity>>
}

@Singleton
class PracticeAttemptStoreImpl @Inject constructor(
    private val database: WenyanDatabase,
) : PracticeAttemptStore {
    private val saveMutex = Mutex()

    override suspend fun save(attempt: PracticeAttemptEntity) = saveMutex.withLock {
        database.withTransaction {
            val dao = database.practiceAttemptDao()
            val existing = dao.getById(attempt.id)
            if (existing == null) {
                dao.insert(attempt)
            } else {
                require(existing.questionId == attempt.questionId) { "attempt question cannot change" }
                check(dao.update(attempt.copy(createdAt = existing.createdAt)) == 1) { "attempt update failed" }
            }
        }
    }

    override suspend fun get(id: String): PracticeAttemptEntity? = database.practiceAttemptDao().getById(id)
    override fun observeSession(sessionId: String): Flow<List<PracticeAttemptEntity>> =
        database.practiceAttemptDao().observeBySession(sessionId)
}
