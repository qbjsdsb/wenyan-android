package com.wenyan.app.core.data.repository

import app.cash.turbine.test
import com.wenyan.app.core.database.dao.DataSourceDao
import com.wenyan.app.core.database.dao.ExamQuestionDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [KnowledgeRepository] 单元测试(v0.8.19 P1-REL-2 新增)。
 *
 * 覆盖范围:
 * - [KnowledgeRepository.observeKnowledgePointDetail]: 主知识点 + 来源 + 关联/对比/延伸分组
 *   - 三组 ID 合并去重后一次查询(P1-DATA-4 优化)
 *   - 不存在的 ID 被 mapNotNull 过滤
 *   - 三组 ID 有重叠时正确分组(同一 ID 在 related 和 contrast 中各出现一次)
 * - [KnowledgeRepository.escapeLikeWildcards]: % _ \ 转义
 * - [KnowledgeRepository.getVerifiedWithSubject]: 仅返回 VERIFIED 知识点
 * - [KnowledgeRepository.searchVerifiedWithSubject]: 关键词搜索
 *
 * 用 in-package Fake DAOs(stub 用到的方法,其他抛 UnsupportedOperationException),
 * 避免依赖 feature:knowledge 模块的 Fakes(core:data 模块测试不应依赖 feature 层)。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KnowledgeRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var knowledgePointDao: FakeKpDao
    private lateinit var dataSourceDao: FakeDsDao
    private lateinit var examQuestionDao: FakeExamQuestionDao
    private lateinit var repository: KnowledgeRepository

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)

        knowledgePointDao = FakeKpDao()
        dataSourceDao = FakeDsDao()
        examQuestionDao = FakeExamQuestionDao()
        repository = KnowledgeRepository(knowledgePointDao, dataSourceDao, examQuestionDao)

        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── observeKnowledgePointDetail ───────────────────────────

    @Test
    fun observeKnowledgePointDetail_pointNotFound_returnsNull() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(emptyMap())

        repository.observeKnowledgePointDetail("ghost").test {
            advanceUntilIdle()
            assertNull(awaitItem())
        }
    }

    @Test
    fun observeKnowledgePointDetail_pointExists_returnsDetailWithSources() = runTest(testDispatcher) {
        val point = makePoint(id = "kp_1", title = "建安风骨")
        knowledgePointDao.setPoints(mapOf("kp_1" to point))
        dataSourceDao.setSourcesForPoint("kp_1", listOf(makeDataSource("ds_1", "kp_1")))

        repository.observeKnowledgePointDetail("kp_1").test {
            advanceUntilIdle()
            val detail = awaitItem()
            assertNotNull(detail)
            assertEquals("建安风骨", detail?.point?.title)
            assertEquals(1, detail?.sources?.size)
            assertEquals("ds_1", detail?.sources?.first()?.id)
            // 无关联/对比/延伸
            assertTrue(detail?.relatedPoints?.isEmpty() == true)
            assertTrue(detail?.contrastPoints?.isEmpty() == true)
            assertTrue(detail?.extensionPoints?.isEmpty() == true)
        }
    }

    @Test
    fun observeKnowledgePointDetail_withRelatedContrastExtension_groupsCorrectly() = runTest(testDispatcher) {
        val main = makePoint(
            id = "kp_main",
            relatedIds = listOf("rel_1", "rel_2"),
            contrastIds = listOf("con_1"),
            extensionIds = listOf("ext_1", "ext_2"),
        )
        knowledgePointDao.setPoints(
            mapOf(
                "kp_main" to main,
                "rel_1" to makePoint(id = "rel_1", title = "关联1"),
                "rel_2" to makePoint(id = "rel_2", title = "关联2"),
                "con_1" to makePoint(id = "con_1", title = "对比1"),
                "ext_1" to makePoint(id = "ext_1", title = "延伸1"),
                "ext_2" to makePoint(id = "ext_2", title = "延伸2"),
            ),
        )

        repository.observeKnowledgePointDetail("kp_main").test {
            advanceUntilIdle()
            val detail = awaitItem()
            assertEquals(2, detail?.relatedPoints?.size)
            assertEquals(1, detail?.contrastPoints?.size)
            assertEquals(2, detail?.extensionPoints?.size)
            assertEquals("关联1", detail?.relatedPoints?.first()?.title)
            assertEquals("对比1", detail?.contrastPoints?.first()?.title)
            assertEquals("延伸1", detail?.extensionPoints?.first()?.title)
        }
    }

    /**
     * P1-DATA-4 核心测试:三组 ID 有重叠时,合并去重后一次查询,
     * 但分组时按原 ID 列表顺序映射(同一 ID 在多组中出现时各出现一次)。
     */
    @Test
    fun observeKnowledgePointDetail_overlappingIds_groupedToAllMatchingLists() = runTest(testDispatcher) {
        // "shared" 同时在 related 和 contrast 中
        val main = makePoint(
            id = "kp_main",
            relatedIds = listOf("shared", "rel_only"),
            contrastIds = listOf("shared", "con_only"),
            extensionIds = emptyList(),
        )
        knowledgePointDao.setPoints(
            mapOf(
                "kp_main" to main,
                "shared" to makePoint(id = "shared", title = "共享"),
                "rel_only" to makePoint(id = "rel_only", title = "仅关联"),
                "con_only" to makePoint(id = "con_only", title = "仅对比"),
            ),
        )

        repository.observeKnowledgePointDetail("kp_main").test {
            advanceUntilIdle()
            val detail = awaitItem()
            // shared 在 related 和 contrast 中各出现一次(分组逻辑)
            assertEquals(2, detail?.relatedPoints?.size)
            assertEquals(2, detail?.contrastPoints?.size)
            // getByIds 只查询一次(shared 去重),但分组时映射到两个列表
            assertEquals(1, knowledgePointDao.getByIdsCalls.size)
            // 一次调用包含 3 个去重 ID(shared, rel_only, con_only)
            assertEquals(setOf("shared", "rel_only", "con_only"), knowledgePointDao.getByIdsCalls.first().toSet())
        }
    }

    @Test
    fun observeKnowledgePointDetail_nonExistentRelatedId_filteredOut() = runTest(testDispatcher) {
        val main = makePoint(
            id = "kp_main",
            relatedIds = listOf("exists", "ghost"),
        )
        knowledgePointDao.setPoints(
            mapOf(
                "kp_main" to main,
                "exists" to makePoint(id = "exists", title = "存在的"),
            ),
        )

        repository.observeKnowledgePointDetail("kp_main").test {
            advanceUntilIdle()
            val detail = awaitItem()
            // ghost 不存在,getByIds 返回 [exists],mapNotNull 过滤 ghost
            assertEquals(1, detail?.relatedPoints?.size)
            assertEquals("存在的", detail?.relatedPoints?.first()?.title)
        }
    }

    @Test
    fun observeKnowledgePointDetail_emptyIdLists_noGetByIdsCall() = runTest(testDispatcher) {
        val main = makePoint(id = "kp_main", relatedIds = null, contrastIds = null, extensionIds = null)
        knowledgePointDao.setPoints(mapOf("kp_main" to main))

        repository.observeKnowledgePointDetail("kp_main").test {
            advanceUntilIdle()
            val detail = awaitItem()
            assertNotNull(detail)
            // 三组 ID 都为空时,不调用 getByIds(短路返回 detail)
            assertEquals(0, knowledgePointDao.getByIdsCalls.size)
        }
    }

    // ── observeRelatedEssays (v0.9.8 新增) ─────────────────────

    /**
     * v0.9.8: observeRelatedEssays 内存过滤 — 仅返回 relatedPointIds 包含 pointId 的论述题。
     *
     * 验证要点:
     * - 包含目标 pointId 的题目进入结果
     * - 不包含的题目被过滤
     * - relatedPointIds 为 null 的题目被过滤(避免 NPE)
     */
    @Test
    fun observeRelatedEssays_filtersByPointId() = runTest(testDispatcher) {
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_1", year = 2020, relatedPointIds = listOf("kp_main", "kp_other")),
                makeEssay(id = "eq_2", year = 2019, relatedPointIds = listOf("kp_other")),
                makeEssay(id = "eq_3", year = 2018, relatedPointIds = null),
                makeEssay(id = "eq_4", year = 2021, relatedPointIds = listOf("kp_main")),
            ),
        )

        repository.observeRelatedEssays("kp_main").test {
            advanceUntilIdle()
            val result = awaitItem()
            // 仅 eq_1 和 eq_4 包含 kp_main，按年份倒序 eq_4(2021) 在 eq_1(2020) 前
            assertEquals(2, result.size)
            assertEquals("eq_4", result[0].id)
            assertEquals("eq_1", result[1].id)
        }
    }

    @Test
    fun observeRelatedEssays_noMatches_returnsEmpty() = runTest(testDispatcher) {
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_1", relatedPointIds = listOf("kp_other")),
                makeEssay(id = "eq_2", relatedPointIds = null),
            ),
        )

        repository.observeRelatedEssays("kp_main").test {
            advanceUntilIdle()
            val result = awaitItem()
            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun observeRelatedEssays_emptyDao_returnsEmpty() = runTest(testDispatcher) {
        examQuestionDao.setEssays(emptyList())

        repository.observeRelatedEssays("kp_main").test {
            advanceUntilIdle()
            val result = awaitItem()
            assertTrue(result.isEmpty())
        }
    }

    /**
     * 验证 SQL LIKE 误匹配的规避:kp_1 不应匹配 kp_10/kp_100。
     *
     * 这是内存过滤而非 SQL LIKE 的核心动机(见 DAO 注释)。
     */
    @Test
    fun observeRelatedEssays_exactMatch_kp1DoesNotMatchKp10() = runTest(testDispatcher) {
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_1", relatedPointIds = listOf("kp_1")),
                makeEssay(id = "eq_2", relatedPointIds = listOf("kp_10")),
                makeEssay(id = "eq_3", relatedPointIds = listOf("kp_100")),
            ),
        )

        repository.observeRelatedEssays("kp_1").test {
            advanceUntilIdle()
            val result = awaitItem()
            // 仅 eq_1 精确匹配 kp_1，SQL LIKE '%kp_1%' 会误匹配全部 3 个
            assertEquals(1, result.size)
            assertEquals("eq_1", result[0].id)
        }
    }

    // ── observeEssayById (v0.9.8 新增) ────────────────────────

    @Test
    fun observeEssayById_exists_returnsEssay() = runTest(testDispatcher) {
        val essay = makeEssay(id = "eq_0038", year = 2008, score = 25)
        examQuestionDao.setEssays(listOf(essay))

        repository.observeEssayById("eq_0038").test {
            advanceUntilIdle()
            val result = awaitItem()
            assertNotNull(result)
            assertEquals("eq_0038", result?.id)
            assertEquals(2008, result?.year)
        }
    }

    @Test
    fun observeEssayById_notExists_returnsNull() = runTest(testDispatcher) {
        examQuestionDao.setEssays(emptyList())

        repository.observeEssayById("ghost").test {
            advanceUntilIdle()
            assertNull(awaitItem())
        }
    }

    // ── getKnowledgePointsByIds (v0.9.8 新增) ─────────────────

    @Test
    fun getKnowledgePointsByIds_emptyInput_returnsEmpty() = runTest(testDispatcher) {
        val result = repository.getKnowledgePointsByIds(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun getKnowledgePointsByIds_allExist_preservesOrder() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(
            mapOf(
                "kp_a" to makePoint(id = "kp_a", title = "A"),
                "kp_b" to makePoint(id = "kp_b", title = "B"),
                "kp_c" to makePoint(id = "kp_c", title = "C"),
            ),
        )

        // 入参顺序与 map 顺序不同，验证结果保留入参顺序
        val result = repository.getKnowledgePointsByIds(listOf("kp_c", "kp_a", "kp_b"))
        assertEquals(3, result.size)
        assertEquals("kp_c", result[0].id)
        assertEquals("kp_a", result[1].id)
        assertEquals("kp_b", result[2].id)
    }

    @Test
    fun getKnowledgePointsByIds_partialExist_filtersMissing() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(
            mapOf(
                "kp_a" to makePoint(id = "kp_a", title = "A"),
                "kp_c" to makePoint(id = "kp_c", title = "C"),
            ),
        )

        val result = repository.getKnowledgePointsByIds(listOf("kp_a", "kp_ghost", "kp_c"))
        // kp_ghost 不存在被过滤
        assertEquals(2, result.size)
        assertEquals("kp_a", result[0].id)
        assertEquals("kp_c", result[1].id)
    }

    /**
     * 验证去重:同一 ID 在入参中出现多次时，结果中也只出现一次，
     * 且调用 DAO 时传入去重后的列表。
     */
    @Test
    fun getKnowledgePointsByIds_deduplicatesIds() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(
            mapOf("kp_a" to makePoint(id = "kp_a", title = "A")),
        )

        val result = repository.getKnowledgePointsByIds(listOf("kp_a", "kp_a", "kp_a"))
        assertEquals(1, result.size)
        // DAO 收到去重后的列表(只调用一次 getByIds)
        assertEquals(1, knowledgePointDao.getByIdsCalls.size)
        assertEquals(listOf("kp_a"), knowledgePointDao.getByIdsCalls.last())
    }

    // ── escapeLikeWildcards ───────────────────────────────────

    @Test
    fun escapeLikeWildcards_escapesPercent() {
        assertEquals("100\\%", repository.escapeLikeWildcards("100%"))
    }

    @Test
    fun escapeLikeWildcards_escapesUnderscore() {
        assertEquals("a\\_b", repository.escapeLikeWildcards("a_b"))
    }

    @Test
    fun escapeLikeWildcards_escapesBackslash() {
        assertEquals("a\\\\b", repository.escapeLikeWildcards("a\\b"))
    }

    @Test
    fun escapeLikeWildcards_mixedWildcards() {
        assertEquals("100\\%\\_50\\_", repository.escapeLikeWildcards("100%_50_"))
    }

    @Test
    fun escapeLikeWildcards_plainText_noChange() {
        assertEquals("建安风骨", repository.escapeLikeWildcards("建安风骨"))
    }

    @Test
    fun escapeLikeWildcards_emptyString() {
        assertEquals("", repository.escapeLikeWildcards(""))
    }

    // ── getVerifiedWithSubject ────────────────────────────────

    @Test
    fun getVerifiedWithSubject_returnsOnlyVerifiedPoints() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(
            mapOf(
                "v1" to makePoint(id = "v1", title = "已校对", ocrStatus = "VERIFIED"),
                "v2" to makePoint(id = "v2", title = "已校对2", ocrStatus = "VERIFIED"),
                "p1" to makePoint(id = "p1", title = "待校对", ocrStatus = "PENDING"),
            ),
        )

        repository.getVerifiedWithSubject().test {
            advanceUntilIdle()
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.point.ocrStatus == "VERIFIED" })
        }
    }

    // ── searchVerifiedWithSubject ─────────────────────────────

    @Test
    fun searchVerifiedWithSubject_matchesTitle() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(
            mapOf(
                "kp1" to makePoint(id = "kp1", title = "建安风骨", coreConclusion = "结论A"),
                "kp2" to makePoint(id = "kp2", title = "正始风骨", coreConclusion = "结论B"),
            ),
        )

        repository.searchVerifiedWithSubject("建安").test {
            advanceUntilIdle()
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("kp1", result[0].point.id)
        }
    }

    @Test
    fun searchVerifiedWithSubject_matchesCoreConclusion() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(
            mapOf(
                "kp1" to makePoint(id = "kp1", title = "A", coreConclusion = "建安风骨的特点"),
                "kp2" to makePoint(id = "kp2", title = "B", coreConclusion = "其他"),
            ),
        )

        repository.searchVerifiedWithSubject("建安").test {
            advanceUntilIdle()
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("kp1", result[0].point.id)
        }
    }

    @Test
    fun searchVerifiedWithSubject_excludesPendingPoints() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(
            mapOf(
                "v1" to makePoint(id = "v1", title = "建安风骨", ocrStatus = "VERIFIED"),
                "p1" to makePoint(id = "p1", title = "建安风骨", ocrStatus = "PENDING"),
            ),
        )

        repository.searchVerifiedWithSubject("建安").test {
            advanceUntilIdle()
            val result = awaitItem()
            // PENDING 不进搜索结果
            assertEquals(1, result.size)
            assertEquals("v1", result[0].point.id)
        }
    }

    @Test
    fun searchVerifiedWithSubject_noMatch_returnsEmpty() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(
            mapOf(
                "kp1" to makePoint(id = "kp1", title = "建安风骨"),
            ),
        )

        repository.searchVerifiedWithSubject("不存在的关键词").test {
            advanceUntilIdle()
            val result = awaitItem()
            assertTrue(result.isEmpty())
        }
    }

    /**
     * v0.8.20 P1-DATA-1 测试:空关键词抛 [IllegalArgumentException]。
     *
     * 验证 require 防御:调用方违规传空字符串时立即抛异常(开发期发现),
     * 而非静默返回错误结果(SQL LIKE '%%' 会丢失 NULL 字段的知识点)。
     */
    @Test(expected = IllegalArgumentException::class)
    fun searchVerifiedWithSubject_blankKeyword_throwsIllegalArgument() {
        repository.searchVerifiedWithSubject("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun searchVerifiedWithSubject_whitespaceKeyword_throwsIllegalArgument() {
        repository.searchVerifiedWithSubject("   ")
    }

    // ── 工厂方法 ──────────────────────────────────────────────

    private fun makePoint(
        id: String = "kp_1",
        title: String = "测试知识点",
        summary: String? = "测试摘要",
        coreConclusion: String = "测试核心结论",
        relatedIds: List<String>? = null,
        contrastIds: List<String>? = null,
        extensionIds: List<String>? = null,
        ocrStatus: String = "VERIFIED",
    ) = KnowledgePointEntity(
        id = id,
        chapterId = "ch1",
        title = title,
        summary = summary,
        coreConclusion = coreConclusion,
        fullContent = "",
        multiPerspectives = null,
        relatedIds = relatedIds,
        contrastIds = contrastIds,
        extensionIds = extensionIds,
        examRecords = null,
        examFrequency = "NEVER",
        termTemplate = null,
        tags = null,
        difficulty = 3,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        contentSource = "TEXTBOOK_NATIVE",
        ocrStatus = ocrStatus,
        sourceFile = null,
        sourcePage = null,
        studyText = null,
    )

    private fun makeDataSource(
        id: String,
        pointId: String,
        sourceFile: String = "test.pdf",
    ) = DataSourceEntity(
        id = id,
        knowledgePointId = pointId,
        examQuestionId = null,
        sourceFile = sourceFile,
        sourcePage = 42,
        contentSource = "TEXTBOOK_NATIVE",
        ocrStatus = "VERIFIED",
        createdAt = System.currentTimeMillis(),
    )

    /**
     * 构造测试用论述题(v0.9.8 新增)。
     *
     * 默认为 ESSAY 题型,year=2020, score=20, relatedPointIds=null,
     * 可通过参数覆盖。
     */
    private fun makeEssay(
        id: String = "eq_test",
        year: Int = 2020,
        subjectId: String = "subj_xd",
        content: String = "测试论述题内容",
        score: Int = 20,
        relatedPointIds: List<String>? = null,
        angle: String? = null,
        notes: String? = null,
        answerFramework: String? = null,
    ) = ExamQuestionEntity(
        id = id,
        year = year,
        subjectId = subjectId,
        questionType = "ESSAY",
        content = content,
        score = score,
        angle = angle,
        relatedPointIds = relatedPointIds,
        answerFramework = answerFramework,
        notes = notes,
        createdAt = System.currentTimeMillis(),
        examPaperCode = null,
        answerStatus = null,
        materialText = null,
        sourceFile = null,
        sourcePage = null,
    )
}

/**
 * [KnowledgePointDao] 的 Fake 实现(仅 stub [KnowledgeRepository] 用到的方法)。
 *
 * 与 feature:knowledge 模块的 FakeKnowledgePointDao 类似,但放在 core:data 测试包内,
 * 避免 core:data 测试依赖 feature 层。
 *
 * 额外记录 [getByIdsCalls],用于断言 P1-DATA-4 的"合并三组 ID 一次查询"行为。
 */
private class FakeKpDao(
    initialPoints: Map<String, KnowledgePointEntity> = emptyMap(),
) : KnowledgePointDao {

    private val _pointsById = MutableStateFlow(initialPoints)

    /** 设置知识点表内容(覆盖) */
    fun setPoints(points: Map<String, KnowledgePointEntity>) {
        _pointsById.value = points
    }

    /** 记录所有 getByIds 调用的 ids 参数(供 P1-DATA-4 断言) */
    val getByIdsCalls: MutableList<List<String>> = mutableListOf()

    override suspend fun insert(entity: KnowledgePointEntity) =
        throw UnsupportedOperationException()

    override suspend fun insertAll(entities: List<KnowledgePointEntity>) =
        throw UnsupportedOperationException()

    override suspend fun update(entity: KnowledgePointEntity) =
        throw UnsupportedOperationException()

    override suspend fun deleteById(id: String) = throw UnsupportedOperationException()

    override suspend fun getById(id: String): KnowledgePointEntity? = _pointsById.value[id]

    override suspend fun getByIds(ids: List<String>): List<KnowledgePointEntity> {
        getByIdsCalls.add(ids)
        return ids.mapNotNull { _pointsById.value[it] }
    }

    override fun observeById(id: String): Flow<KnowledgePointEntity?> =
        _pointsById.map { it[id] }

    override fun observeByChapter(chapterId: String): Flow<List<KnowledgePointEntity>> =
        throw UnsupportedOperationException()

    override fun observeByExamFrequency(frequency: String): Flow<List<KnowledgePointEntity>> =
        throw UnsupportedOperationException()

    override fun observeByOcrStatus(status: String): Flow<List<KnowledgePointEntity>> =
        _pointsById.map { values -> values.values.filter { it.ocrStatus == status } }

    override fun observeByContentSource(source: String): Flow<List<KnowledgePointEntity>> =
        throw UnsupportedOperationException()

    override suspend fun countByChapter(chapterId: String): Int = throw UnsupportedOperationException()

    override fun observeAll(): Flow<List<KnowledgePointEntity>> = throw UnsupportedOperationException()

    override fun observeVerifiedForReview(): Flow<List<KnowledgePointEntity>> =
        _pointsById.map { values ->
            values.values.filter { it.ocrStatus == "VERIFIED" }.sortedByDescending { it.updatedAt }
        }

    override suspend fun updateOcrStatus(id: String, status: String) =
        throw UnsupportedOperationException()

    override suspend fun searchByKeyword(keyword: String, limit: Int): List<KnowledgePointEntity> =
        throw UnsupportedOperationException()

    override fun observeSearchWithSubject(keyword: String): Flow<List<KnowledgePointWithSubject>> =
        _pointsById.map { values ->
            values.values
                .filter { it.ocrStatus == "VERIFIED" }
                .filter { point ->
                    point.title.contains(keyword) ||
                        point.coreConclusion.contains(keyword) ||
                        point.fullContent.contains(keyword) ||
                        (point.studyText?.contains(keyword) == true)
                }
                .sortedByDescending { it.updatedAt }
                .map { KnowledgePointWithSubject(point = it, subjectName = "中国古代文学") }
        }

    override fun observeVerifiedWithSubject(): Flow<List<KnowledgePointWithSubject>> =
        _pointsById.map { values ->
            values.values
                .filter { it.ocrStatus == "VERIFIED" }
                .sortedByDescending { it.updatedAt }
                .map { KnowledgePointWithSubject(point = it, subjectName = "中国古代文学") }
        }
}

/**
 * [DataSourceDao] 的 Fake 实现(仅 stub observeByKnowledgePoint)。
 */
private class FakeDsDao(
    initialSourcesByPoint: Map<String, List<DataSourceEntity>> = emptyMap(),
) : DataSourceDao {

    private val _sourcesByPoint = MutableStateFlow(initialSourcesByPoint)

    fun setSourcesForPoint(pointId: String, sources: List<DataSourceEntity>) {
        _sourcesByPoint.value = _sourcesByPoint.value + (pointId to sources)
    }

    override suspend fun insert(entity: DataSourceEntity) = throw UnsupportedOperationException()

    override suspend fun insertAll(entities: List<DataSourceEntity>) = throw UnsupportedOperationException()

    override suspend fun update(entity: DataSourceEntity) = throw UnsupportedOperationException()

    override suspend fun deleteById(id: String) = throw UnsupportedOperationException()

    override suspend fun getById(id: String): DataSourceEntity? = throw UnsupportedOperationException()

    override fun observeByKnowledgePoint(pointId: String): Flow<List<DataSourceEntity>> =
        _sourcesByPoint.map { it[pointId] ?: emptyList() }

    override fun observeByExamQuestion(questionId: String): Flow<List<DataSourceEntity>> =
        throw UnsupportedOperationException()

    override fun observeByContentSource(source: String): Flow<List<DataSourceEntity>> =
        throw UnsupportedOperationException()

    override fun observeByOcrStatus(status: String): Flow<List<DataSourceEntity>> =
        throw UnsupportedOperationException()

    override fun observeAll(): Flow<List<DataSourceEntity>> = throw UnsupportedOperationException()
}

/**
 * [ExamQuestionDao] 的 Fake 实现(v0.9.8 新增)。
 *
 * 仅 stub [KnowledgeRepository] 论述题板块用到的方法:
 * - [observeAllEssays]:返回注入的论述题列表(供 observeRelatedEssays 内存过滤)
 * - [observeById]:按 ID 查找(供 observeEssayById)
 *
 * 其他方法抛 [UnsupportedOperationException],避免静默返回错误默认值。
 *
 * 通过 [setEssays] 可控注入论述题数据,支持知识点关联 + 单题查询场景测试。
 */
private class FakeExamQuestionDao(
    initialEssays: List<ExamQuestionEntity> = emptyList(),
) : ExamQuestionDao {

    private val _essays = MutableStateFlow(initialEssays)

    /** 设置论述题列表(覆盖) */
    fun setEssays(essays: List<ExamQuestionEntity>) {
        _essays.value = essays
    }

    override suspend fun insert(entity: ExamQuestionEntity) =
        throw UnsupportedOperationException()

    override suspend fun insertAll(entities: List<ExamQuestionEntity>) =
        throw UnsupportedOperationException()

    override suspend fun update(entity: ExamQuestionEntity) =
        throw UnsupportedOperationException()

    override suspend fun deleteById(id: String) =
        throw UnsupportedOperationException()

    override suspend fun getById(id: String): ExamQuestionEntity? =
        _essays.value.firstOrNull { it.id == id }

    override fun observeById(id: String): Flow<ExamQuestionEntity?> =
        _essays.map { essays -> essays.firstOrNull { it.id == id } }

    override fun observeBySubject(subjectId: String): Flow<List<ExamQuestionEntity>> =
        throw UnsupportedOperationException()

    override fun observeByYear(year: Int): Flow<List<ExamQuestionEntity>> =
        throw UnsupportedOperationException()

    override fun observeByQuestionType(type: String): Flow<List<ExamQuestionEntity>> =
        throw UnsupportedOperationException()

    /**
     * 按多个题型过滤（v0.9.33 真题背题新增）。
     *
     * 生产 SQL 为 `question_type IN (:types) ORDER BY year DESC, exam_paper_code ASC, id ASC`；
     * 测试中保持相同语义：先按题型过滤，再按 year DESC 排序。
     */
    override fun observeByQuestionTypes(types: List<String>): Flow<List<ExamQuestionEntity>> =
        _essays.map { essays ->
            essays
                .filter { it.questionType in types }
                .sortedWith(
                    compareByDescending<ExamQuestionEntity> { it.year }
                        .thenBy { it.examPaperCode }
                        .thenBy { it.id }
                )
        }

    /**
     * 返回注入的全部 ESSAY 题(生产按 year DESC 排序,测试中也保持该顺序)。
     *
     * 测试通过 [setEssays] 注入时自行按年份倒序,与生产 SQL ORDER BY year DESC 一致。
     */
    override fun observeAllEssays(): Flow<List<ExamQuestionEntity>> =
        _essays.map { it.sortedByDescending { e -> e.year } }

    override fun observeByExamPaperCode(code: String): Flow<List<ExamQuestionEntity>> =
        throw UnsupportedOperationException()

    override fun observeByAnswerStatus(status: String): Flow<List<ExamQuestionEntity>> =
        throw UnsupportedOperationException()

    override fun observeYears(): Flow<List<Int>> =
        throw UnsupportedOperationException()

    override suspend fun countBySubject(subjectId: String): Int =
        throw UnsupportedOperationException()
}
