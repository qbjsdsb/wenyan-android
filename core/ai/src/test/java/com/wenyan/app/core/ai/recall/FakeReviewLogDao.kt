package com.wenyan.app.core.ai.recall

import com.wenyan.app.core.database.dao.ReviewLogDao
import com.wenyan.app.core.database.entity.ReviewLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * [ReviewLogDao] 的 Fake 实现，供 [AntiRoteMemorizationTest] 使用。
 *
 * 内部用 `MutableMap<pointId, MutableList<ReviewLogEntity>>` 存储，
 * 支持按 pointId 查询和按 pointIds 批量查询。
 *
 * @param initialLogs 预设的复习日志列表
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
        for (entity in entities) insert(entity)
    }

    override suspend fun deleteById(id: String) {
        // 简化实现：遍历所有列表删除匹配 id 的记录
        for (logs in store.values) {
            logs.removeAll { it.id == id }
        }
    }

    // P1-13 修复：3 处偏离真实 DAO 契约（与 feature/aiassistant 的 Fakes.kt 对齐）：
    // 1. getById: find → firstOrNull（find 在无匹配时抛 NoSuchElementException，偏离 nullable 契约）
    // 2. observeByPoint: 加 sortedByDescending（真实 DAO 的 @Query 有 ORDER BY created_at DESC）
    // 3. observeAll: 加 sortedByDescending（同上）
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
