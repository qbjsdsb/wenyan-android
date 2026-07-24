package com.wenyan.app.core.data.repository

import androidx.compose.runtime.Immutable
import com.wenyan.app.core.data.util.catchAndLog
import com.wenyan.app.core.database.dao.DataSourceDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 知识点仓库（阶段5新增，详情页专用；v0.8.19 扩展为知识点浏览总入口）。
 *
 * 职责：
 * - 观察单个知识点详情（含来源溯源列表）
 * - 批量查询关联/对比/延伸知识点标题
 * - 提供已 VERIFIED 知识点列表(含科目名)供浏览(v0.8.19 从 [ReviewRepository] 迁入)
 * - 提供关键词搜索(含科目名)供搜索框使用(v0.8.19 新增)
 *
 * 与 [ReviewRepository] 职责分离（v0.8.19 架构修复，对应 AGENTS.md 第 9.4 条 P4）：
 * - 本仓库面向知识点浏览(列表 / 详情 / 搜索)
 * - [ReviewRepository] 面向 FSRS 复习队列(到期卡片 / 待校对)
 *
 * 原实现 `getVerifiedWithSubject()` 放在 [ReviewRepository] 是历史遗留
 * (SESSION_LOG L504 记录的"架构职责不完美"问题)，本次迁移修正。
 *
 * 数据来源：
 * - [KnowledgePointDao]：知识点主表
 * - [DataSourceDao]：资料来源溯源表（Spec 新增）
 *
 * P1 审计修复：mapLatest 内含 suspend DAO 查询（getByIds），
 * 加 .catchAndLog 降级为 null，避免详情页崩溃。
 *
 * v0.8.19 P1-DATA-4 优化：[observeKnowledgePointDetail] 将三次 getByIds 合并为一次，
 * 减少数据库往返次数（3→1），提升详情页加载速度。
 */
@Singleton
class KnowledgeRepository @Inject constructor(
    private val knowledgePointDao: KnowledgePointDao,
    private val dataSourceDao: DataSourceDao,
) {

    private companion object {
        private const val TAG = "KnowledgeRepository"
    }

    /**
     * 观察知识点详情（含来源溯源列表）。
     *
     * v0.8.19 P1-DATA-4 优化：原实现分别对 relatedIds / contrastIds / extensionIds
     * 调用三次 `getByIds`,触发最多 3 次 DB 往返。现合并三组 ID 去重后一次查询,
     * 在内存中按 ID 分组到三个列表,减少 DB 往返(3→1)。
     *
     * 内存分组成本可忽略(三组 ID 总数通常 <20,Map 查找 O(1)),
     * 收益是减少 2 次 DB 往返(每次 ~1-5ms,共省 2-10ms)。
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
                // v0.8.19 P1-DATA-4: 合并三组 ID 去重后一次查询,内存分组到三个列表
                val relatedIds = detail.point.relatedIds.orEmpty()
                val contrastIds = detail.point.contrastIds.orEmpty()
                val extensionIds = detail.point.extensionIds.orEmpty()
                val allIds = (relatedIds + contrastIds + extensionIds).distinct()
                if (allIds.isEmpty()) {
                    detail
                } else {
                    val allPoints = knowledgePointDao.getByIds(allIds)
                    val pointById = allPoints.associateBy { it.id }
                    detail.copy(
                        relatedPoints = relatedIds.mapNotNull { pointById[it] },
                        contrastPoints = contrastIds.mapNotNull { pointById[it] },
                        extensionPoints = extensionIds.mapNotNull { pointById[it] },
                    )
                }
            }
        }.catchAndLog(TAG, "observeKnowledgePointDetail") { null }

    /** 单次获取知识点（非流式） */
    suspend fun getById(pointId: String): KnowledgePointEntity? = knowledgePointDao.getById(pointId)

    /**
     * 获取所有已 VERIFIED 的知识点，附带科目名（v0.8.19 从 [ReviewRepository] 迁入）。
     *
     * 供知识点浏览界面 [com.wenyan.app.feature.knowledge.KnowledgeViewModel] 的分类筛选使用。
     *
     * 架构修复（对应 AGENTS.md 第 9.4 条 P4）：
     * - 原实现放在 [ReviewRepository]（职责是 FSRS 复习队列），知识点浏览与复习无关
     * - [KnowledgeViewModel] 注入 [ReviewRepository] 仅为调用此方法，职责混乱
     * - 现迁移到本仓库（知识点浏览总入口），[KnowledgeViewModel] 改注入 [KnowledgeRepository]
     *
     * @return 已 VERIFIED 知识点 + 科目名(可能为 null)列表,按 updated_at DESC 排序
     */
    fun getVerifiedWithSubject(): Flow<List<KnowledgePointWithSubject>> =
        knowledgePointDao.observeVerifiedWithSubject()
            .catchAndLog(TAG, "getVerifiedWithSubject") { emptyList() }

    /**
     * 关键词搜索已 VERIFIED 知识点(附带科目名,v0.8.19 新增)。
     *
     * 供 [com.wenyan.app.feature.knowledge.KnowledgeViewModel] 搜索框使用。
     *
     * 搜索范围:title / core_conclusion / full_content / study_text 四字段 LIKE 搜索。
     * SQLite LIKE 对中文友好,无需分词。
     *
     * 转义:调用方需用 [escapeLikeWildcards] 转义 % 和 _ 通配符,
     * 避免"100%"匹配"1000"等问题(与 [KnowledgePointDao.searchByKeyword] 一致)。
     *
     * v0.8.20 P1-DATA-1 修复:加 [require] 校验 keyword 非空。
     * 原仅注释说明"不应传空字符串",但无运行时校验,调用方违规时静默返回错误结果
     * (空字符串 SQL `LIKE '%%'` 仅匹配非 NULL 字段,会丢失 title/core_conclusion
     * 为 NULL 的知识点,与 [getVerifiedWithSubject] 行为不一致)。
     * 现 require 在函数调用时立即抛 [IllegalArgumentException],开发期即可发现。
     *
     * @param keyword 搜索关键词(已转义 % 和 _,非空)
     * @return 匹配的知识点 + 科目名列表,按 updated_at DESC 排序
     * @throws IllegalArgumentException 如果 keyword 为空或纯空白
     */
    fun searchVerifiedWithSubject(keyword: String): Flow<List<KnowledgePointWithSubject>> {
        require(keyword.isNotBlank()) {
            "keyword must not be blank; use getVerifiedWithSubject() for unfiltered list. " +
                "Blank keyword causes SQL LIKE '%%' to exclude NULL fields, silently losing points."
        }
        return knowledgePointDao.observeSearchWithSubject(keyword)
            .catchAndLog(TAG, "searchVerifiedWithSubject") { emptyList() }
    }

    /**
     * 转义 LIKE 通配符(v0.8.19 新增,供搜索框使用)。
     *
     * 将 % 和 _ 转义为 \% 和 \_,配合 SQL ESCAPE '\\' 子句,
     * 让这些字符作为字面量匹配而非通配符。
     *
     * 例如:输入"100%" → 输出"100\%",SQL `LIKE '%100\%%' ESCAPE '\\'`
     * 匹配包含"100%"的字段值,而非"1000"等。
     *
     * 与 [com.wenyan.app.core.ai.RagEngine] 的 escapeLikeWildcards 实现一致。
     */
    fun escapeLikeWildcards(input: String): String =
        input.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
}

/**
 * 知识点详情（含来源溯源 + 关联知识点标题）。
 */
@Immutable
data class KnowledgePointDetail(
    val point: KnowledgePointEntity,
    val sources: List<DataSourceEntity>,
    val relatedPoints: List<KnowledgePointEntity> = emptyList(),
    val contrastPoints: List<KnowledgePointEntity> = emptyList(),
    val extensionPoints: List<KnowledgePointEntity> = emptyList(),
)
