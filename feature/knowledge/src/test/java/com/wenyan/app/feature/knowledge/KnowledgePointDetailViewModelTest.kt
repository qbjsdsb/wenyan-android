package com.wenyan.app.feature.knowledge

import androidx.lifecycle.SavedStateHandle
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * [KnowledgePointDetailViewModel] 单元测试(v0.8.19 P1-REL-2 新增)。
 *
 * 覆盖范围:
 * - pointId 为空时显示 notFound
 * - pointId 不存在(返回 null)时显示 notFound
 * - 正常加载时显示详情(含来源溯源)
 * - 关联/对比/延伸知识点正确分组(P1-DATA-4 合并查询的 ViewModel 层验证)
 * - 不存在的关联 ID 被 mapNotNull 过滤
 * - 错题关联:仅展示未解决错题(resolvedAt == null)
 * - markWrongAnswerResolved 调用仓库 markResolved
 * - markWrongAnswerResolved 异常时不崩溃(吞异常 + Log.w)
 * - retry 触发重新订阅
 *
 * 用 StandardTestDispatcher + advanceUntilIdle 控制协程执行时序,
 * 读 uiState.value 断言最终状态(与 CardsViewModelTest 一致,避免 Turbine block
 * 内 advanceUntilIdle 的 receiver 解析问题)。
 *
 * 注:测试用真实 [com.wenyan.app.core.data.repository.KnowledgeRepository] + Fake DAOs
 * (通过 [buildKnowledgeRepository]),顺带覆盖 Repository 的 observeKnowledgePointDetail
 * 合并逻辑(relatedPoints 分组等)。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KnowledgePointDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var knowledgePointDao: FakeKnowledgePointDao
    private lateinit var dataSourceDao: FakeDataSourceDao
    private lateinit var wrongAnswerRepository: FakeKnowledgeWrongAnswerRepository

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)

        knowledgePointDao = FakeKnowledgePointDao()
        dataSourceDao = FakeDataSourceDao()
        wrongAnswerRepository = FakeKnowledgeWrongAnswerRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(pointId: String?): KnowledgePointDetailViewModel {
        val savedStateHandle = if (pointId != null) {
            SavedStateHandle(mapOf("pointId" to pointId))
        } else {
            SavedStateHandle()
        }
        return KnowledgePointDetailViewModel(
            savedStateHandle = savedStateHandle,
            knowledgeRepository = buildKnowledgeRepository(knowledgePointDao, dataSourceDao),
            wrongAnswerRepository = wrongAnswerRepository,
        )
    }

    // ── pointId 为空 ──────────────────────────────────────────

    @Test
    fun uiState_blankPointId_showsNotFound() = runTest(testDispatcher) {
        val viewModel = createViewModel(pointId = null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.notFound)
        assertNull(state.detail)
    }

    // ── pointId 不存在 ────────────────────────────────────────

    @Test
    fun uiState_pointIdNotFound_showsNotFound() = runTest(testDispatcher) {
        knowledgePointDao.setPoints(emptyMap()) // 知识点表为空
        val viewModel = createViewModel(pointId = "ghost")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.notFound)
        assertNull(state.detail)
    }

    // ── 正常加载 ──────────────────────────────────────────────

    @Test
    fun uiState_pointExists_loadsDetailWithSources() = runTest(testDispatcher) {
        val point = makePoint(id = "kp_1", title = "建安风骨")
        knowledgePointDao.setPoints(mapOf("kp_1" to point))
        dataSourceDao.setSourcesForPoint("kp_1", listOf(makeDataSource("ds_1", "kp_1")))
        val viewModel = createViewModel(pointId = "kp_1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.notFound)
        assertNotNull(state.detail)
        assertEquals("建安风骨", state.point?.title)
        assertEquals(1, state.sources.size)
        assertEquals("ds_1", state.sources[0].id)
    }

    @Test
    fun uiState_pointWithRelatedContrastExtension_groupsCorrectly() = runTest(testDispatcher) {
        val main = makePoint(
            id = "kp_main",
            title = "主知识点",
            relatedIds = listOf("rel_1", "rel_2", "rel_3"),
            contrastIds = listOf("con_1", "con_2"),
            extensionIds = listOf("ext_1"),
        )
        knowledgePointDao.setPoints(
            mapOf(
                "kp_main" to main,
                "rel_1" to makePoint(id = "rel_1", title = "关联1"),
                "rel_2" to makePoint(id = "rel_2", title = "关联2"),
                "rel_3" to makePoint(id = "rel_3", title = "关联3"),
                "con_1" to makePoint(id = "con_1", title = "对比1"),
                "con_2" to makePoint(id = "con_2", title = "对比2"),
                "ext_1" to makePoint(id = "ext_1", title = "延伸1"),
            ),
        )
        val viewModel = createViewModel(pointId = "kp_main")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.detail?.relatedPoints?.size)
        assertEquals(2, state.detail?.contrastPoints?.size)
        assertEquals(1, state.detail?.extensionPoints?.size)
        assertEquals("关联1", state.detail?.relatedPoints?.first()?.title)
        assertEquals("对比1", state.detail?.contrastPoints?.first()?.title)
        assertEquals("延伸1", state.detail?.extensionPoints?.first()?.title)
    }

    @Test
    fun uiState_relatedIdsContainsNonExistentId_filteredOut() = runTest(testDispatcher) {
        val main = makePoint(
            id = "kp_main",
            title = "主",
            relatedIds = listOf("exists", "ghost"),
        )
        knowledgePointDao.setPoints(
            mapOf(
                "kp_main" to main,
                "exists" to makePoint(id = "exists", title = "存在的"),
            ),
        )
        val viewModel = createViewModel(pointId = "kp_main")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // ghost 被 mapNotNull 过滤,只剩 1 个
        assertEquals(1, state.detail?.relatedPoints?.size)
        assertEquals("存在的", state.detail?.relatedPoints?.first()?.title)
    }

    // ── 错题关联 ──────────────────────────────────────────────

    @Test
    fun uiState_hasUnresolvedWrongAnswers_showsInState() = runTest(testDispatcher) {
        val point = makePoint(id = "kp_1")
        knowledgePointDao.setPoints(mapOf("kp_1" to point))

        val now = System.currentTimeMillis()
        val unresolved = makeWrongAnswer(
            id = "wa_1",
            pointId = "kp_1",
            resolvedAt = null,
            wrongCount = 2,
            lastWrongAt = now,
        )
        val resolved = makeWrongAnswer(
            id = "wa_2",
            pointId = "kp_1",
            resolvedAt = now - 1000,
            wrongCount = 1,
            lastWrongAt = now - 2000,
        )
        wrongAnswerRepository.setWrongAnswersForPoint("kp_1", listOf(unresolved, resolved))

        val viewModel = createViewModel(pointId = "kp_1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // 仅未解决错题(resolvedAt == null)出现在 uiState
        assertEquals(1, state.wrongAnswers.size)
        assertEquals("wa_1", state.wrongAnswers[0].id)
        assertEquals(2, state.wrongAnswers[0].wrongCount)
    }

    @Test
    fun uiState_noWrongAnswers_emptyList() = runTest(testDispatcher) {
        val point = makePoint(id = "kp_1")
        knowledgePointDao.setPoints(mapOf("kp_1" to point))
        val viewModel = createViewModel(pointId = "kp_1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.wrongAnswers.isEmpty())
    }

    @Test
    fun uiState_markResolvedInRepository_wrongAnswerRemovedFromUiState() = runTest(testDispatcher) {
        // 验证 markResolved 后 Flow 自动刷新,错题从 uiState 移除
        val point = makePoint(id = "kp_1")
        knowledgePointDao.setPoints(mapOf("kp_1" to point))
        val now = System.currentTimeMillis()
        val wrong = makeWrongAnswer(id = "wa_1", pointId = "kp_1", resolvedAt = null, lastWrongAt = now)
        wrongAnswerRepository.setWrongAnswersForPoint("kp_1", listOf(wrong))

        val viewModel = createViewModel(pointId = "kp_1")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.wrongAnswers.size)

        // 模拟 markResolved 后仓库数据变化(实际生产中 markResolved 写 DB,Flow 自动刷新)
        wrongAnswerRepository.setWrongAnswersForPoint("kp_1", emptyList())
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.wrongAnswers.size)
    }

    // ── markWrongAnswerResolved ───────────────────────────────

    @Test
    fun markWrongAnswerResolved_callsRepositoryMarkResolved() = runTest(testDispatcher) {
        val viewModel = createViewModel(pointId = "kp_1")
        viewModel.markWrongAnswerResolved("wa_1")
        advanceUntilIdle()

        assertEquals(1, wrongAnswerRepository.resolvedIds.size)
        assertEquals("wa_1", wrongAnswerRepository.resolvedIds[0])
    }

    @Test
    fun markWrongAnswerResolved_repositoryThrows_doesNotCrash() = runTest(testDispatcher) {
        wrongAnswerRepository.markResolvedThrowable = RuntimeException("DB error")
        val viewModel = createViewModel(pointId = "kp_1")

        // 不应抛异常(被 try-catch 吞掉 + Log.w)
        viewModel.markWrongAnswerResolved("wa_1")
        advanceUntilIdle()

        // markResolved 被调用但抛异常,resolvedIds 不应记录(在抛异常前 return)
        assertEquals(0, wrongAnswerRepository.resolvedIds.size)
    }

    // ── retry ─────────────────────────────────────────────────

    @Test
    fun retry_reloadesDetailAfterPointBecomesAvailable() = runTest(testDispatcher) {
        // 初始无知识点 → notFound
        val viewModel = createViewModel(pointId = "kp_1")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.notFound)

        // 模拟知识点出现(如种子数据加载完成)
        knowledgePointDao.setPoints(mapOf("kp_1" to makePoint(id = "kp_1", title = "新加载")))

        // 触发重试
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.notFound)
        assertEquals("新加载", state.point?.title)
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
        ocrStatus = "VERIFIED",
        sourceFile = null,
        sourcePage = null,
        studyText = null,
    )

    private fun makeDataSource(
        id: String,
        pointId: String,
        sourceFile: String = "test.pdf",
    ) = com.wenyan.app.core.database.entity.DataSourceEntity(
        id = id,
        knowledgePointId = pointId,
        examQuestionId = null,
        sourceFile = sourceFile,
        sourcePage = 42,
        contentSource = "TEXTBOOK_NATIVE",
        ocrStatus = "VERIFIED",
        createdAt = System.currentTimeMillis(),
    )

    private fun makeWrongAnswer(
        id: String,
        pointId: String,
        resolvedAt: Long?,
        wrongCount: Int = 1,
        lastWrongAt: Long = System.currentTimeMillis(),
        source: String = WrongAnswerRepository.SOURCE_CARD_AGAIN,
    ) = WrongAnswerEntity(
        id = id,
        pointId = pointId,
        examQuestionId = null,
        userAnswer = "用户错误答案",
        correctAnswer = "正确答案",
        source = source,
        wrongCount = wrongCount,
        lastWrongAt = lastWrongAt,
        resolvedAt = resolvedAt,
        aiExplanation = null,
        createdAt = System.currentTimeMillis(),
    )
}
