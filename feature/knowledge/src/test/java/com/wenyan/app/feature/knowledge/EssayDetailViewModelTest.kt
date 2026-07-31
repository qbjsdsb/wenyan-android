package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
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
 * [EssayDetailViewModel] 单元测试(v0.9.8 论述题板块新增)。
 *
 * 覆盖范围:
 * - examQuestionId 为空时显示 notFound
 * - examQuestionId 不存在(返回 null)时显示 notFound
 * - 正常加载:essay 实体填充到 uiState
 * - angle JSON 解析成功 → EssayAngle 填充
 * - angle JSON 为 null/空/格式错误 → angle=null(UI 优雅降级)
 * - notes JSON 解析成功 → EssayNotes 填充
 * - notes JSON 为 null/空/格式错误 → notes=null(UI 优雅降级)
 * - 关联知识点聚合:relatedPointIds + evidences.linkedKnowledgePointId 合并去重
 * - 关联知识点为空时 relatedPoints=emptyList
 * - retry 触发重新订阅
 *
 * 用 StandardTestDispatcher + advanceUntilIdle 控制协程执行时序,
 * 读 uiState.value 断言最终状态(与 KnowledgePointDetailViewModelTest 一致)。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EssayDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var knowledgePointDao: FakeKnowledgePointDao
    private lateinit var dataSourceDao: FakeDataSourceDao
    private lateinit var examQuestionDao: FakeExamQuestionDao

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)

        knowledgePointDao = FakeKnowledgePointDao()
        dataSourceDao = FakeDataSourceDao()
        examQuestionDao = FakeExamQuestionDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(examQuestionId: String?): EssayDetailViewModel {
        val savedStateHandle = if (examQuestionId != null) {
            SavedStateHandle(mapOf("examQuestionId" to examQuestionId))
        } else {
            SavedStateHandle()
        }
        return EssayDetailViewModel(
            savedStateHandle = savedStateHandle,
            knowledgeRepository = buildKnowledgeRepository(knowledgePointDao, dataSourceDao, examQuestionDao),
        )
    }

    // ── examQuestionId 为空 ───────────────────────────────────

    @Test
    fun uiState_blankExamQuestionId_showsNotFound() = runTest(testDispatcher) {
        val viewModel = createViewModel(examQuestionId = null)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.notFound)
        assertNull(state.essay)
    }

    // ── examQuestionId 不存在 ─────────────────────────────────

    @Test
    fun uiState_examQuestionIdNotFound_showsNotFound() = runTest(testDispatcher) {
        examQuestionDao.setEssays(emptyList())
        val viewModel = createViewModel(examQuestionId = "ghost")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.notFound)
        assertNull(state.essay)
    }

    // ── 正常加载 ──────────────────────────────────────────────

    @Test
    fun uiState_essayExists_loadsEssay() = runTest(testDispatcher) {
        val essay = makeEssay(
            id = "eq_0038",
            year = 2008,
            score = 25,
            content = "试论述五位女作家的创作...",
        )
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_0038")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.notFound)
        assertNotNull(state.essay)
        assertEquals("eq_0038", state.essay?.id)
        assertEquals(2008, state.essay?.year)
        assertEquals(25, state.essay?.score)
    }

    // ── angle JSON 解析 ───────────────────────────────────────

    @Test
    fun uiState_validAngleJson_parsesAngle() = runTest(testDispatcher) {
        val angleJson = """
            {
              "questionType": "比较型",
              "coreKeywords": ["冰心", "丁玲", "萧红"],
              "limitKeywords": ["不同时期"],
              "task": "比较异同+梳理演变",
              "breakthroughAngles": ["①时代背景...", "②女性意识..."],
              "angleRationale": "四维度比较",
              "argumentPath": {
                "thesis": "总论点",
                "points": [{"label": "分1", "content": "冰心"}],
                "conclusion": "结论"
              }
            }
        """.trimIndent()
        val essay = makeEssay(id = "eq_1", angle = angleJson)
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val angle = viewModel.uiState.value.angle
        assertNotNull(angle)
        assertEquals("比较型", angle?.questionType)
        assertEquals(3, angle?.coreKeywords?.size)
        assertEquals("冰心", angle?.coreKeywords?.first())
        assertNotNull(angle?.argumentPath)
        assertEquals("总论点", angle?.argumentPath?.thesis)
        assertEquals(1, angle?.argumentPath?.points?.size)
        assertEquals("分1", angle?.argumentPath?.points?.first()?.label)
    }

    @Test
    fun uiState_nullAngleJson_angleIsNull() = runTest(testDispatcher) {
        val essay = makeEssay(id = "eq_1", angle = null)
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.angle)
    }

    @Test
    fun uiState_blankAngleJson_angleIsNull() = runTest(testDispatcher) {
        val essay = makeEssay(id = "eq_1", angle = "")
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.angle)
    }

    /**
     * 优雅降级:JSON 格式错误不崩溃,angle=null(UI 隐藏审题区块)。
     */
    @Test
    fun uiState_malformedAngleJson_angleIsNull_noCrash() = runTest(testDispatcher) {
        val essay = makeEssay(id = "eq_1", angle = "{ this is not valid json }}}")
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        // 不崩溃 + angle=null
        assertNull(viewModel.uiState.value.angle)
        // essay 本身仍加载成功
        assertNotNull(viewModel.uiState.value.essay)
    }

    /**
     * 优雅降级:JSON 缺少未知字段不报错(ignoreUnknownKeys=true)。
     */
    @Test
    fun uiState_angleJsonWithUnknownFields_parsesSuccessfully() = runTest(testDispatcher) {
        val angleJson = """
            {
              "questionType": "演变型",
              "futureField": "未知字段不应破坏解析",
              "anotherUnknown": 123
            }
        """.trimIndent()
        val essay = makeEssay(id = "eq_1", angle = angleJson)
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val angle = viewModel.uiState.value.angle
        assertNotNull(angle)
        assertEquals("演变型", angle?.questionType)
    }

    // ── notes JSON 解析 ───────────────────────────────────────

    @Test
    fun uiState_validNotesJson_parsesNotes() = runTest(testDispatcher) {
        val notesJson = """
            {
              "evidences": [
                {"type": "WORK_TEXT", "label": "作品原文", "content": "...", "linkedKnowledgePointId": "kp_00595"}
              ],
              "crossValidation": {"textbookComparison": "教材对比", "scholarComparison": "学者对比"},
              "referenceLinks": [{"label": "中国作家网", "url": "https://example.com"}],
              "knowledgeGaps": [{"author": "萧红", "note": "建议补充"}]
            }
        """.trimIndent()
        val essay = makeEssay(id = "eq_1", notes = notesJson)
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val notes = viewModel.uiState.value.notes
        assertNotNull(notes)
        assertEquals(1, notes?.evidences?.size)
        assertEquals("WORK_TEXT", notes?.evidences?.first()?.type)
        assertEquals("kp_00595", notes?.evidences?.first()?.linkedKnowledgePointId)
        assertNotNull(notes?.crossValidation)
        assertEquals("教材对比", notes?.crossValidation?.textbookComparison)
        assertEquals(1, notes?.referenceLinks?.size)
        assertEquals("https://example.com", notes?.referenceLinks?.first()?.url)
        assertEquals(1, notes?.knowledgeGaps?.size)
        assertEquals("萧红", notes?.knowledgeGaps?.first()?.author)
    }

    @Test
    fun uiState_nullNotesJson_notesIsNull() = runTest(testDispatcher) {
        val essay = makeEssay(id = "eq_1", notes = null)
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.notes)
    }

    @Test
    fun uiState_malformedNotesJson_notesIsNull_noCrash() = runTest(testDispatcher) {
        val essay = makeEssay(id = "eq_1", notes = "not a json")
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.notes)
        assertNotNull(viewModel.uiState.value.essay)
    }

    // ── 关联知识点聚合 ───────────────────────────────────────

    /**
     * 关联知识点来源 1:essay.relatedPointIds。
     */
    @Test
    fun uiState_relatedPoints_fromRelatedPointIds() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(
            mapOf(
                "kp_a" to makeKp("kp_a", "知识点A"),
                "kp_b" to makeKp("kp_b", "知识点B"),
            ),
        )
        val essay = makeEssay(id = "eq_1", relatedPointIds = listOf("kp_a", "kp_b"))
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val related = viewModel.uiState.value.relatedPoints
        assertEquals(2, related.size)
        assertTrue(related.any { it.id == "kp_a" })
        assertTrue(related.any { it.id == "kp_b" })
    }

    /**
     * 关联知识点来源 2:notes.evidences.linkedKnowledgePointId。
     */
    @Test
    fun uiState_relatedPoints_fromEvidencesLinkedKpId() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(
            mapOf("kp_ev" to makeKp("kp_ev", "依据关联知识点")),
        )
        val notesJson = """
            {
              "evidences": [
                {"type": "WORK_TEXT", "linkedKnowledgePointId": "kp_ev"}
              ]
            }
        """.trimIndent()
        val essay = makeEssay(id = "eq_1", relatedPointIds = null, notes = notesJson)
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val related = viewModel.uiState.value.relatedPoints
        assertEquals(1, related.size)
        assertEquals("kp_ev", related[0].id)
    }

    /**
     * 两个来源合并去重:relatedPointIds 和 evidences.linkedKnowledgePointId
     * 指向同一知识点时,relatedPoints 只出现一次。
     */
    @Test
    fun uiState_relatedPoints_mergedAndDeduplicated() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(
            mapOf(
                "kp_shared" to makeKp("kp_shared", "共享知识点"),
                "kp_only_related" to makeKp("kp_only_related", "仅 relatedPointIds"),
            ),
        )
        val notesJson = """
            {
              "evidences": [
                {"linkedKnowledgePointId": "kp_shared"}
              ]
            }
        """.trimIndent()
        // relatedPointIds 和 evidences 都含 kp_shared
        val essay = makeEssay(
            id = "eq_1",
            relatedPointIds = listOf("kp_shared", "kp_only_related"),
            notes = notesJson,
        )
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val related = viewModel.uiState.value.relatedPoints
        // kp_shared 去重,只出现一次;共 2 个知识点
        assertEquals(2, related.size)
        assertEquals(1, related.count { it.id == "kp_shared" })
    }

    @Test
    fun uiState_noRelatedPoints_emptyList() = runTest(testDispatcher) {
        val essay = makeEssay(id = "eq_1", relatedPointIds = null, notes = null)
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.relatedPoints.isEmpty())
    }

    /**
     * relatedPointIds 中的知识点不存在于知识点表时,被过滤掉(不报错)。
     */
    @Test
    fun uiState_relatedPoints_partialExist_filtersMissing() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(mapOf("kp_a" to makeKp("kp_a", "存在")))
        val essay = makeEssay(id = "eq_1", relatedPointIds = listOf("kp_a", "kp_ghost"))
        examQuestionDao.setEssays(listOf(essay))

        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val related = viewModel.uiState.value.relatedPoints
        assertEquals(1, related.size)
        assertEquals("kp_a", related[0].id)
    }

    // ── retry ─────────────────────────────────────────────────

    @Test
    fun retry_reloadesEssayAfterItBecomesAvailable() = runTest(testDispatcher) {
        // 初始无论述题 → notFound
        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.notFound)

        // 模拟论述题出现
        examQuestionDao.setEssays(listOf(makeEssay(id = "eq_1")))

        // 触发重试
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.notFound)
        assertNotNull(state.essay)
    }

    @Test
    fun retry_setsIsLoadingTrueImmediately() = runTest(testDispatcher) {
        examQuestionDao.setEssays(listOf(makeEssay(id = "eq_1")))
        val viewModel = createViewModel(examQuestionId = "eq_1")
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.retry()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    // ── 工厂方法 ──────────────────────────────────────────────

    private fun makeKp(
        id: String,
        title: String,
    ) = KnowledgePointEntity(
        id = id,
        chapterId = "ch1",
        title = title,
        summary = null,
        coreConclusion = "核心结论",
        fullContent = "",
        multiPerspectives = null,
        relatedIds = null,
        contrastIds = null,
        extensionIds = null,
        examRecords = null,
        examFrequency = "NEVER",
        termTemplate = null,
        tags = null,
        difficulty = 3,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        contentSource = "TEXTBOOK_NATIVE",
        ocrStatus = "VERIFIED",
        sourceFile = null,
        sourcePage = null,
        studyText = null,
    )

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
