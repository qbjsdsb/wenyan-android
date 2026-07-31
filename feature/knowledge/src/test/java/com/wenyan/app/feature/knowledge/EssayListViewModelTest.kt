package com.wenyan.app.feature.knowledge

import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [EssayListViewModel] 单元测试（v0.9.8 Phase 2 论述题列表新增）。
 *
 * 覆盖范围:
 * - 初始加载:essays + subjects 合并到 uiState,isLoading=false
 * - 科目名映射:subjectId → subjectName(未知 subjectId 回退"未知科目")
 * - 年份筛选:只返回匹配年份的论述题
 * - 科目筛选:只返回匹配 subjectId 的论述题
 * - 仅显示有审题思路筛选:angle 非空的论述题
 * - 三维筛选叠加:年份 + 科目 + onlyWithAngle
 * - availableYears 从全量数据提取(不受筛选影响)
 * - totalCount(全量) vs filteredCount(筛选后)
 * - 筛选无匹配:essays=emptyList,filteredCount=0
 * - 筛选状态独立 StateFlow:selectYear/selectSubject/toggleOnlyWithAngle/clearFilters
 * - contentPreview 截断到 MAX_PREVIEW_LENGTH(80)
 * - hasAngle/hasNotes/relatedPointCount 字段正确填充
 *
 * 用 StandardTestDispatcher + advanceUntilIdle 控制协程执行时序,
 * 读 uiState.value 断言最终状态(与 EssayDetailViewModelTest 一致)。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EssayListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var knowledgePointDao: FakeKnowledgePointDao
    private lateinit var dataSourceDao: FakeDataSourceDao
    private lateinit var examQuestionDao: FakeExamQuestionDao
    private lateinit var chapterRepository: FakeChapterRepository

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)

        knowledgePointDao = FakeKnowledgePointDao()
        dataSourceDao = FakeDataSourceDao()
        examQuestionDao = FakeExamQuestionDao()
        chapterRepository = FakeChapterRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): EssayListViewModel = EssayListViewModel(
        knowledgeRepository = buildKnowledgeRepository(knowledgePointDao, dataSourceDao, examQuestionDao),
        chapterRepository = chapterRepository,
    )

    // ── 初始加载 ──────────────────────────────────────────────

    @Test
    fun uiState_initialLoad_showsAllEssaysWithSubjectNames() = runTest(testDispatcher) {
        chapterRepository.subjects = listOf(
            makeSubject("subj_xd", "中国现当代文学"),
            makeSubject("subj_gd", "中国古代文学"),
        )
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_1", year = 2020, subjectId = "subj_xd"),
                makeEssay(id = "eq_2", year = 2019, subjectId = "subj_gd"),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.essays.size)
        assertEquals(2, state.totalCount)
        assertEquals(2, state.filteredCount)
        // 年份倒序:2020 在前
        assertEquals("eq_1", state.essays[0].id)
        assertEquals("中国现当代文学", state.essays[0].subjectName)
        assertEquals("eq_2", state.essays[1].id)
        assertEquals("中国古代文学", state.essays[1].subjectName)
    }

    @Test
    fun uiState_emptyEssays_isLoadingFalse_emptyList() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.essays.isEmpty())
        assertEquals(0, state.totalCount)
    }

    // ── 科目名映射 ────────────────────────────────────────────

    @Test
    fun uiState_unknownSubjectId_fallsBackToUnknownLabel() = runTest(testDispatcher) {
        examQuestionDao.setEssays(listOf(makeEssay(id = "eq_1", subjectId = "subj_ghost")))

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertEquals("未知科目", viewModel.uiState.value.essays[0].subjectName)
    }

    // ── 年份筛选 ──────────────────────────────────────────────

    @Test
    fun selectYear_filtersToMatchingYearOnly() = runTest(testDispatcher) {
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_2020", year = 2020),
                makeEssay(id = "eq_2019", year = 2019),
                makeEssay(id = "eq_2018", year = 2018),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.selectYear(2019)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.totalCount)
        assertEquals(1, state.filteredCount)
        assertEquals("eq_2019", state.essays[0].id)
    }

    @Test
    fun selectYear_null_clearsYearFilter() = runTest(testDispatcher) {
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_2020", year = 2020),
                makeEssay(id = "eq_2019", year = 2019),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.selectYear(2020)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.filteredCount)

        viewModel.selectYear(null)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.filteredCount)
    }

    // ── 科目筛选 ──────────────────────────────────────────────

    @Test
    fun selectSubject_filtersToMatchingSubjectOnly() = runTest(testDispatcher) {
        chapterRepository.subjects = listOf(
            makeSubject("subj_xd", "现当代"),
            makeSubject("subj_gd", "古代"),
        )
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_xd1", subjectId = "subj_xd"),
                makeEssay(id = "eq_xd2", subjectId = "subj_xd"),
                makeEssay(id = "eq_gd1", subjectId = "subj_gd"),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.selectSubject("subj_xd")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.totalCount)
        assertEquals(2, state.filteredCount)
        assertTrue(state.essays.all { it.subjectName == "现当代" })
    }

    // ── 仅显示有审题思路筛选 ──────────────────────────────────

    @Test
    fun toggleOnlyWithAngle_filtersToEssaysWithAngle() = runTest(testDispatcher) {
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_with_angle", angle = """{"questionType":"比较型"}"""),
                makeEssay(id = "eq_no_angle", angle = null),
                makeEssay(id = "eq_blank_angle", angle = ""),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertFalse(viewModel.onlyWithAngle.value)
        assertEquals(3, viewModel.uiState.value.filteredCount)

        viewModel.toggleOnlyWithAngle()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.totalCount)
        assertEquals(1, state.filteredCount)
        assertEquals("eq_with_angle", state.essays[0].id)
        assertTrue(state.essays[0].hasAngle)
    }

    // ── 三维筛选叠加 ──────────────────────────────────────────

    @Test
    fun filters_combined_yearSubjectAngle_allApplied() = runTest(testDispatcher) {
        chapterRepository.subjects = listOf(makeSubject("subj_xd", "现当代"))
        examQuestionDao.setEssays(
            listOf(
                // 匹配全部三维
                makeEssay(id = "hit", year = 2020, subjectId = "subj_xd", angle = """{"questionType":"比较型"}"""),
                // 年份不匹配
                makeEssay(id = "miss_year", year = 2019, subjectId = "subj_xd", angle = """{"questionType":"比较型"}"""),
                // 科目不匹配
                makeEssay(id = "miss_subj", year = 2020, subjectId = "subj_gd", angle = """{"questionType":"比较型"}"""),
                // 无审题思路
                makeEssay(id = "miss_angle", year = 2020, subjectId = "subj_xd", angle = null),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.selectYear(2020)
        viewModel.selectSubject("subj_xd")
        viewModel.toggleOnlyWithAngle()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4, state.totalCount)
        assertEquals(1, state.filteredCount)
        assertEquals("hit", state.essays[0].id)
    }

    // ── availableYears ────────────────────────────────────────

    @Test
    fun availableYears_extractedFromAllEssays_descendingDistinct() = runTest(testDispatcher) {
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_1", year = 2018),
                makeEssay(id = "eq_2", year = 2020),
                makeEssay(id = "eq_3", year = 2019),
                makeEssay(id = "eq_4", year = 2020), // 重复年份
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val years = viewModel.uiState.value.availableYears
        assertEquals(listOf(2020, 2019, 2018), years)
    }

    /**
     * availableYears 不受筛选影响:即使筛选后只剩一道题,
     * 年份选项仍包含全部年份(用户可随时切换筛选,不会丢失选项)。
     */
    @Test
    fun availableYears_notAffectedByFilter() = runTest(testDispatcher) {
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_2020", year = 2020),
                makeEssay(id = "eq_2019", year = 2019),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.selectYear(2020)
        advanceUntilIdle()

        // 筛选后 filteredCount=1,但 availableYears 仍是 2 个年份
        assertEquals(1, viewModel.uiState.value.filteredCount)
        assertEquals(2, viewModel.uiState.value.availableYears.size)
    }

    // ── 筛选无匹配 ────────────────────────────────────────────

    @Test
    fun filters_noMatch_emptyEssays_nonZeroTotal() = runTest(testDispatcher) {
        examQuestionDao.setEssays(listOf(makeEssay(id = "eq_1", year = 2020)))

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.selectYear(1999) // 不存在的年份
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.totalCount)
        assertEquals(0, state.filteredCount)
        assertTrue(state.essays.isEmpty())
    }

    // ── clearFilters ──────────────────────────────────────────

    @Test
    fun clearFilters_resetsAllThreeFilters() = runTest(testDispatcher) {
        chapterRepository.subjects = listOf(makeSubject("subj_xd", "现当代"))
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_1", year = 2020, subjectId = "subj_xd", angle = """{"questionType":"比较型"}"""),
                makeEssay(id = "eq_2", year = 2019, subjectId = "subj_gd", angle = null),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.selectYear(2020)
        viewModel.selectSubject("subj_xd")
        viewModel.toggleOnlyWithAngle()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.filteredCount)

        viewModel.clearFilters()
        advanceUntilIdle()

        assertNull(viewModel.selectedYear.value)
        assertNull(viewModel.selectedSubjectId.value)
        assertFalse(viewModel.onlyWithAngle.value)
        assertEquals(2, viewModel.uiState.value.filteredCount)
    }

    // ── EssayListItem 字段填充 ─────────────────────────────────

    @Test
    fun essayItem_contentPreview_truncatedToMaxLength() = runTest(testDispatcher) {
        val longContent = "这".repeat(100) // 100 字,超过 MAX_PREVIEW_LENGTH(80)
        examQuestionDao.setEssays(listOf(makeEssay(id = "eq_1", content = longContent)))

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val item = viewModel.uiState.value.essays[0]
        assertEquals(80, item.contentPreview.length)
    }

    @Test
    fun essayItem_hasAngleAndHasNotes_correctlyReflected() = runTest(testDispatcher) {
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_full", angle = """{"questionType":"比较型"}""", notes = """{"evidences":[]}"""),
                makeEssay(id = "eq_empty", angle = null, notes = null),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val items = viewModel.uiState.value.essays
        val full = items.first { it.id == "eq_full" }
        val empty = items.first { it.id == "eq_empty" }
        assertTrue(full.hasAngle)
        assertTrue(full.hasNotes)
        assertFalse(empty.hasAngle)
        assertFalse(empty.hasNotes)
    }

    @Test
    fun essayItem_relatedPointCount_reflectsRelatedPointIdsSize() = runTest(testDispatcher) {
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_3pts", relatedPointIds = listOf("kp_a", "kp_b", "kp_c")),
                makeEssay(id = "eq_0pts", relatedPointIds = null),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val items = viewModel.uiState.value.essays
        assertEquals(3, items.first { it.id == "eq_3pts" }.relatedPointCount)
        assertEquals(0, items.first { it.id == "eq_0pts" }.relatedPointCount)
    }

    // ── 非 ESSAY 题型不出现 ───────────────────────────────────

    /**
     * FakeExamQuestionDao.observeAllEssays 只返回 questionType="ESSAY" 的题,
     * 验证 ViewModel 不会把 TERM_EXPLANATION / SHORT_ANSWER 混入论述题列表。
     */
    @Test
    fun uiState_onlyEssays_nonEssayTypesExcluded() = runTest(testDispatcher) {
        examQuestionDao.setEssays(
            listOf(
                makeEssay(id = "eq_1", questionType = "ESSAY"),
                makeEssay(id = "term_1", questionType = "TERM_EXPLANATION"),
                makeEssay(id = "short_1", questionType = "SHORT_ANSWER"),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.totalCount)
        assertEquals("eq_1", state.essays[0].id)
    }

    // ── retry ─────────────────────────────────────────────────

    /**
     * retry 触发 flatMapLatest 重新订阅:初始无数据 → 添加数据 → retry → 加载成功。
     */
    @Test
    fun retry_reloadesEssaysAfterTheyBecomeAvailable() = runTest(testDispatcher) {
        // 初始无论述题
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.totalCount)

        // 模拟论述题出现
        examQuestionDao.setEssays(listOf(makeEssay(id = "eq_1")))
        advanceUntilIdle()
        // stateIn + WhileSubscribed 无新订阅时不重新查询,retry 触发重新订阅
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.totalCount)
        assertEquals("eq_1", state.essays[0].id)
    }

    // ── 工厂方法 ──────────────────────────────────────────────

    private fun makeSubject(
        id: String,
        name: String,
        sortOrder: Int = 0,
    ) = SubjectEntity(
        id = id,
        name = name,
        shortName = name.take(2),
        sortOrder = sortOrder,
    )

    private fun makeEssay(
        id: String = "eq_test",
        year: Int = 2020,
        subjectId: String = "subj_xd",
        content: String = "测试论述题内容",
        score: Int = 20,
        questionType: String = "ESSAY",
        relatedPointIds: List<String>? = null,
        angle: String? = null,
        notes: String? = null,
        answerFramework: String? = null,
    ) = ExamQuestionEntity(
        id = id,
        year = year,
        subjectId = subjectId,
        questionType = questionType,
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
