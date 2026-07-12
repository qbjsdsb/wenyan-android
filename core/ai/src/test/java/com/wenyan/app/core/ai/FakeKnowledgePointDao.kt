package com.wenyan.app.core.ai

import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * [KnowledgePointDao] 的 Fake 实现，供 [RagEngineTest] 使用。
 *
 * 只实现 [searchByKeyword]，其余方法返回默认值（RagEngine 不调用）。
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
    override suspend fun updateOcrStatus(id: String, status: String) {}

    override suspend fun searchByKeyword(keyword: String, limit: Int): List<KnowledgePointEntity> {
        // 模拟 LIKE 搜索：返回包含关键词的结果
        return searchResults.filter { entity ->
            entity.title.contains(keyword, ignoreCase = true) ||
                entity.coreConclusion.contains(keyword, ignoreCase = true) ||
                entity.fullContent.contains(keyword, ignoreCase = true) ||
                (entity.studyText?.contains(keyword, ignoreCase = true) ?: false)
        }.take(limit)
    }
}
