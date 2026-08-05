package com.wenyan.app.core.data.repository

import androidx.compose.runtime.Immutable
import com.wenyan.app.core.data.util.catchAndLog
import com.wenyan.app.core.database.dao.DataSourceDao
import com.wenyan.app.core.database.dao.ExamQuestionDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointListItem
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
    private val examQuestionDao: ExamQuestionDao,
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
     * 列表展示 lean 投影（v0.9.37 P1-2）。
     *
     * 与 [getVerifiedWithSubject] 语义一致（VERIFIED + 科目名 + updated_at DESC），
     * 但底层走 [KnowledgePointDao.observeVerifiedListItem]：只查展示列，
     * 不加载 full_content/study_text/multi_perspectives 等大文本列。
     * 知识点列表 UI 应使用本方法（列表卡片只用 title/summary/考频/科目）。
     *
     * [getVerifiedWithSubject] 保留给复习拆卡等需要全字段的场景。
     */
    fun getVerifiedListItems(): Flow<List<KnowledgePointListItem>> =
        knowledgePointDao.observeVerifiedListItem()
            .catchAndLog(TAG, "getVerifiedListItems") { emptyList() }

    /**
     * 列表搜索 lean 投影（v0.9.37 P1-2）。
     *
     * 与 [searchVerifiedWithSubject] 语义一致（VERIFIED + 四字段 LIKE + 转义），
     * 但只查展示列（搜索匹配在 SQL 内完成）。
     *
     * @param keyword 搜索关键词(已转义 % 和 _,非空)
     * @throws IllegalArgumentException 如果 keyword 为空或纯空白
     */
    fun searchVerifiedListItems(keyword: String): Flow<List<KnowledgePointListItem>> {
        require(keyword.isNotBlank()) {
            "keyword must not be blank; use getVerifiedListItems() for unfiltered list. " +
                "Blank keyword causes SQL LIKE '%%' to exclude NULL fields, silently losing points."
        }
        return knowledgePointDao.observeSearchListItem(keyword)
            .catchAndLog(TAG, "searchVerifiedListItems") { emptyList() }
    }

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

    // ── 论述题板块（v0.9.8 新增，对应 docs/design/essay-module-design.md） ─────

    /**
     * 观察与某知识点关联的论述题（v0.9.8 新增）。
     *
     * 实现：先查全部 ESSAY 题（[ExamQuestionDao.observeAllEssays]，134 题），
     * 在内存中按 `relatedPointIds.contains(pointId)` 过滤。
     *
     * 内存过滤而非 SQL LIKE 的原因（与 [ExamQuestionDao.observeAllEssays] 注释一致）：
     * - SQL LIKE 对 JSON 数组无原生支持，"kp_1" 会误匹配 "kp_10/kp_100" 子串
     * - 内存过滤精确匹配 List<String> contains
     * - 134 题规模下内存过滤 < 5ms，无性能差异
     *
     * @param pointId 知识点 ID
     * @return 关联该知识点的论述题列表（按年份倒序），无关联时返回空列表
     */
    fun observeRelatedEssays(pointId: String): Flow<List<ExamQuestionEntity>> =
        examQuestionDao.observeAllEssays()
            .mapLatest { allEssays ->
                allEssays.filter { it.relatedPointIds?.contains(pointId) == true }
            }
            .catchAndLog(TAG, "observeRelatedEssays pointId=$pointId") { emptyList() }

    /**
     * 观察所有论述题（v0.9.8 Phase 2 新增，供论述题列表页使用）。
     *
     * 返回全部 ESSAY 题按年份倒序，调用方在内存中按年份/科目/是否有审题思路筛选。
     * 与 [observeRelatedEssays] 共享同一 DAO 流，无需重复查询。
     *
     * @return 所有论述题列表（按年份倒序、试卷代码 ASC）
     */
    fun observeAllEssays(): Flow<List<ExamQuestionEntity>> =
        examQuestionDao.observeAllEssays()
            .catchAndLog(TAG, "observeAllEssays") { emptyList() }

    /**
     * 观察"真题背题"题目（v0.9.33 新增，供名词解释/简答背题专项使用）。
     *
     * 仅查询 [types] 指定题型（TERM_EXPLANATION / SHORT_ANSWER），
     * 从数据层排除 ESSAY——论述题由 [observeAllEssays] 独立板块承载，
     * 避免"真题"概念在两个入口重复展示。
     *
     * 筛选（题型 Tab / 科目 / 年份）由 ViewModel 在内存完成（346 条 < 5ms，
     * 与 [observeRelatedEssays] 的内存过滤策略一致）。
     *
     * @param types 题型白名单，如 listOf("TERM_EXPLANATION", "SHORT_ANSWER")
     * @return 按年份倒序的真题列表
     */
    fun observePracticeQuestions(types: List<String>): Flow<List<ExamQuestionEntity>> =
        examQuestionDao.observeByQuestionTypes(types)
            .catchAndLog(TAG, "observePracticeQuestions types=$types") { emptyList() }

    /**
     * 观察单个论述题（v0.9.8 新增，供论述题详情页使用）。
     *
     * @param examQuestionId 真题 ID（如 eq_0038）
     * @return 论述题实体，不存在时返回 null
     */
    fun observeEssayById(examQuestionId: String): Flow<ExamQuestionEntity?> =
        examQuestionDao.observeById(examQuestionId)
            .catchAndLog(TAG, "observeEssayById id=$examQuestionId") { null }

    /**
     * 批量查询知识点（v0.9.8 新增，供论述题详情页"关联知识点"区块使用）。
     *
     * @param pointIds 知识点 ID 列表（来自 `ExamQuestionEntity.relatedPointIds` + 依据 JSON 的 linkedKnowledgePointId）
     * @return 知识点实体列表（顺序与入参一致，不存在的 ID 被过滤）
     */
    suspend fun getKnowledgePointsByIds(pointIds: List<String>): List<KnowledgePointEntity> {
        if (pointIds.isEmpty()) return emptyList()
        // 去重后一次查询，保留入参顺序（与 observeKnowledgePointDetail 的内存分组策略一致）
        val uniqueIds = pointIds.distinct()
        val pointsById = knowledgePointDao.getByIds(uniqueIds).associateBy { it.id }
        return uniqueIds.mapNotNull { pointsById[it] }
    }
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
