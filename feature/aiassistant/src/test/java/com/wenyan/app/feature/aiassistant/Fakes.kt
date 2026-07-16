package com.wenyan.app.feature.aiassistant

import com.wenyan.app.core.ai.AiService
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.ReviewLogDao
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import com.wenyan.app.core.database.entity.ReviewLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * [AiService] 的 Fake 实现，供 [AiAssistantViewModelTest] 使用。
 *
 * - [response]：chat() 返回的内容
 * - [available]：isAvailable() 返回的值
 * - [throwException]：非 null 时 chat() 抛异常（测试异常处理）
 */
class FakeAiService(
    var response: String = "默认 AI 回复",
    var available: Boolean = true,
    var throwException: Throwable? = null,
) : AiService {

    override fun chat(query: String): Flow<String> = flow {
        throwException?.let { throw it }
        emit(response)
    }

    override fun chatResult(query: String): Flow<Result<String>> = flow {
        throwException?.let { emit(Result.failure(it)); return@flow }
        emit(Result.success(response))
    }

    override fun isAvailable(): Flow<Boolean> = flowOf(available)
}

/**
 * [KnowledgePointDao] 的 Fake 实现，供 [AiAssistantViewModelTest] 使用。
 *
 * 只实现 [searchByKeyword]，其余方法返回默认值。
 */
class FakeKnowledgePointDao(
    private val searchResults: List<KnowledgePointEntity> = emptyList(),
) : KnowledgePointDao {

    override suspend fun insert(entity: KnowledgePointEntity) {}
    override suspend fun insertAll(entities: List<KnowledgePointEntity>) {}
    override suspend fun update(entity: KnowledgePointEntity) {}
    override suspend fun deleteById(id: String) {}
    override suspend fun getById(id: String): KnowledgePointEntity? = null
    override suspend fun getByIds(ids: List<String>): List<KnowledgePointEntity> = emptyList()
    override fun observeById(id: String): Flow<KnowledgePointEntity?> = flowOf(null)
    override fun observeByChapter(chapterId: String): Flow<List<KnowledgePointEntity>> = flowOf(emptyList())
    override fun observeByExamFrequency(frequency: String): Flow<List<KnowledgePointEntity>> = flowOf(emptyList())
    override fun observeByOcrStatus(status: String): Flow<List<KnowledgePointEntity>> = flowOf(emptyList())
    override fun observeByContentSource(source: String): Flow<List<KnowledgePointEntity>> = flowOf(emptyList())
    override suspend fun countByChapter(chapterId: String): Int = 0
    override fun observeAll(): Flow<List<KnowledgePointEntity>> = flowOf(searchResults)
    override fun observeVerifiedForReview(): Flow<List<KnowledgePointEntity>> = flowOf(emptyList())
    override fun observeVerifiedWithSubject(): Flow<List<KnowledgePointWithSubject>> = flowOf(emptyList())
    override suspend fun updateOcrStatus(id: String, status: String) {}

    override suspend fun searchByKeyword(keyword: String, limit: Int): List<KnowledgePointEntity> {
        return searchResults.filter { entity ->
            entity.title.contains(keyword, ignoreCase = true) ||
                entity.coreConclusion.contains(keyword, ignoreCase = true) ||
                entity.fullContent.contains(keyword, ignoreCase = true) ||
                (entity.studyText?.contains(keyword, ignoreCase = true) ?: false)
        }.take(limit)
    }
}

/**
 * [ReviewLogDao] 的 Fake 实现，供 [AiAssistantViewModelTest] 使用。
 */
class FakeReviewLogDao(
    initialLogs: List<ReviewLogEntity> = emptyList(),
) : ReviewLogDao {

    private val store = mutableMapOf<String, MutableList<ReviewLogEntity>>()

    init {
        for (log in initialLogs) {
            store.getOrPut(log.pointId) { mutableListOf() }.add(log)
        }
    }

    override suspend fun insert(entity: ReviewLogEntity) {
        store.getOrPut(entity.pointId) { mutableListOf() }.add(entity)
    }

    override suspend fun insertAll(entities: List<ReviewLogEntity>) {
        for (entity in entities) {
            insert(entity)
        }
    }

    override suspend fun deleteById(id: String) {
        store.values.forEach { list -> list.removeAll { it.id == id } }
    }

    override suspend fun getById(id: String): ReviewLogEntity? {
        return store.values.flatten().firstOrNull { it.id == id }
    }

    override fun observeByPoint(pointId: String): Flow<List<ReviewLogEntity>> {
        return flowOf(store[pointId]?.sortedByDescending { it.createdAt }?.toList() ?: emptyList())
    }

    override fun observeAll(): Flow<List<ReviewLogEntity>> {
        return flowOf(store.values.flatten().sortedByDescending { it.createdAt })
    }

    override suspend fun countByPoint(pointId: String): Int {
        return store[pointId]?.size ?: 0
    }

    override suspend fun getByPointOrderByCreatedDesc(pointId: String): List<ReviewLogEntity> {
        return store[pointId]?.sortedByDescending { it.createdAt }?.toList() ?: emptyList()
    }

    override suspend fun getByPointIds(pointIds: List<String>): List<ReviewLogEntity> {
        return store.filterKeys { it in pointIds }.values.flatten()
    }
}
