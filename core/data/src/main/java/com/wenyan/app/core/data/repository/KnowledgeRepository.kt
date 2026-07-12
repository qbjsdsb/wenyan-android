package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.DataSourceDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 知识点仓库（阶段5新增，详情页专用）。
 *
 * 职责：
 * - 观察单个知识点详情（含来源溯源列表）
 * - 批量查询关联/对比/延伸知识点标题
 *
 * 与 [ReviewRepository] 职责分离：本仓库面向知识点详情浏览，
 * [ReviewRepository] 面向 FSRS 复习队列。
 *
 * 数据来源：
 * - [KnowledgePointDao]：知识点主表
 * - [DataSourceDao]：资料来源溯源表（Spec 新增）
 */
@Singleton
class KnowledgeRepository @Inject constructor(
    private val knowledgePointDao: KnowledgePointDao,
    private val dataSourceDao: DataSourceDao,
) {

    /**
     * 观察知识点详情（含来源溯源列表）。
     *
     * @param pointId 知识点 ID
     * @return 知识点 + 来源列表的合并流，知识点不存在时返回 null
     */
    fun observeKnowledgePointDetail(pointId: String): Flow<KnowledgePointDetail?> =
        combine(
            knowledgePointDao.observeById(pointId),
            dataSourceDao.observeByKnowledgePoint(pointId),
        ) { point, sources ->
            if (point == null) {
                null
            } else {
                KnowledgePointDetail(
                    point = point,
                    sources = sources,
                )
            }
        }.mapLatest { detail ->
            if (detail == null) {
                null
            } else {
                // 批量查询关联/对比/延伸知识点标题
                val relatedIds = detail.point.relatedIds.orEmpty()
                val contrastIds = detail.point.contrastIds.orEmpty()
                val extensionIds = detail.point.extensionIds.orEmpty()
                detail.copy(
                    relatedPoints = if (relatedIds.isEmpty()) emptyList() else knowledgePointDao.getByIds(relatedIds),
                    contrastPoints = if (contrastIds.isEmpty()) emptyList() else knowledgePointDao.getByIds(contrastIds),
                    extensionPoints = if (extensionIds.isEmpty()) emptyList() else knowledgePointDao.getByIds(extensionIds),
                )
            }
        }

    /** 单次获取知识点（非流式） */
    suspend fun getById(pointId: String): KnowledgePointEntity? = knowledgePointDao.getById(pointId)
}

/**
 * 知识点详情（含来源溯源 + 关联知识点标题）。
 */
data class KnowledgePointDetail(
    val point: KnowledgePointEntity,
    val sources: List<DataSourceEntity>,
    val relatedPoints: List<KnowledgePointEntity> = emptyList(),
    val contrastPoints: List<KnowledgePointEntity> = emptyList(),
    val extensionPoints: List<KnowledgePointEntity> = emptyList(),
)
