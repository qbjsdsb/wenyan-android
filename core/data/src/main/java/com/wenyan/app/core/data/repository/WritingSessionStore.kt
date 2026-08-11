package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.WritingSessionDao
import com.wenyan.app.core.database.entity.WritingSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface WritingSessionStore {
    suspend fun create(session: WritingSessionEntity)
    suspend fun save(session: WritingSessionEntity)
    suspend fun get(id: String): WritingSessionEntity?
    fun observe(id: String): Flow<WritingSessionEntity?>
    fun observeCompleted(): Flow<List<WritingSessionEntity>>
}

@Singleton
class WritingSessionStoreImpl @Inject constructor(private val dao: WritingSessionDao) : WritingSessionStore {
    override suspend fun create(session: WritingSessionEntity) = dao.insert(session)
    override suspend fun save(session: WritingSessionEntity) { check(dao.update(session) == 1) { "Writing session no longer exists" } }
    override suspend fun get(id: String) = dao.getById(id)
    override fun observe(id: String) = dao.observeById(id)
    override fun observeCompleted() = dao.observeCompleted()
}
