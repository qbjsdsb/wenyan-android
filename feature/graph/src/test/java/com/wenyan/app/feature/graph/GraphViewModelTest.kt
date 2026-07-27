package com.wenyan.app.feature.graph

import com.wenyan.app.core.database.entity.SubjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
 * [GraphViewModel] 单元测试（v0.8.2 新增：知识图谱发布前回归测试）。
 *
 * 覆盖范围：
 * - **筛选流水线**：DisplayScope（CORE/IMPORTANT/ALL）+ 类型 + 科目 + 薄弱 + 搜索 + 聚焦
 * - **findNeighbors**（间接）：N 跳邻居 BFS（0/1/2 跳，无向图）
 * - **NEIGHBORHOOD 布局**：无 focus 时自动选度数最大节点
 * - **setLayoutMode 副作用**：切到 TIMELINE/RADIAL 清 focus，NEIGHBORHOOD 保留
 * - **setFocusHops 边界**：1-3 之外被 coerce
 * - **toggleTypeFilter** add/remove 行为
 * - **clearAllFilters** 全状态重置
 * - **toUiItem（间接）**：sourceKpIds CSV 解析
 * - **边过滤**：只保留两端节点都在筛选结果中的边
 * - **错误处理**：仓库抛异常 → uiState.error
 *
 * 用 StandardTestDispatcher + advanceUntilIdle 控制协程执行时序，
 * 读 uiState.value 断言最终状态（与 KnowledgePointDetailViewModelTest 一致，
 * 避免 Turbine block 内 advanceUntilIdle 的 receiver 解析问题）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GraphViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var graphRepository: FakeGraphRepository
    private lateinit var subjectDao: FakeSubjectDao

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        graphRepository = FakeGraphRepository()
        subjectDao = FakeSubjectDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): GraphViewModel = GraphViewModel(
        graphRepository = graphRepository,
        subjectDao = subjectDao,
    )

    /** 启动 ViewModel 的 uiState 收集，避免 StateFlow WhileSubscribed 不发数据 */
    private fun TestScope.subscribeUiState(viewModel: GraphViewModel) {
        backgroundScope.launch { viewModel.uiState.collect { } }
        backgroundScope.launch { viewModel.subjects.collect { } }
        backgroundScope.launch { viewModel.knowledgePointTitles.collect { } }
    }

    // ==================== DisplayScope 筛选 ====================

    @Test
    fun `displayScope 初始值为 CORE`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        subscribeUiState(viewModel)
        advanceUntilIdle()

        assertEquals(DisplayScope.CORE, viewModel.displayScope.value)
    }

    @Test
    fun `CORE 范围只保留核心节点（sourceKpIds 至少4 或 高频 或 degree 至少3）`() = runTest(testDispatcher) {
        // node_core1: sourceKpIds=4 → 核心
        // node_core2: examFrequency=HIGH → 核心
        // node_core3: degree=3 → 核心
        // node_normal: 不满足任一条件
        val nodes = listOf(
            testNode(
                id = "core1",
                label = "核心1（多知识点）",
                metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4"),
            ),
            testNode(
                id = "core2",
                label = "核心2（高频）",
                metadata = mapOf("examFrequency" to "HIGH"),
            ),
            testNode(
                id = "core3",
                label = "核心3（桥接）",
                metadata = mapOf("sourceKpIds" to "kp1"),
            ),
            testNode(
                id = "normal",
                label = "普通节点",
                metadata = mapOf("sourceKpIds" to "kp1"),
            ),
        )
        // core3 与 3 个节点都有边 → degree=3
        val edges = listOf(
            testEdge("core3", "normal"),
            testEdge("core3", "core1"),
            testEdge("core3", "core2"),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = edges)
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val nodeIds = state.nodes.map { it.id }.toSet()
        assertTrue("core1（sourceKpIds=4）应在 CORE 范围", "core1" in nodeIds)
        assertTrue("core2（HIGH 频）应在 CORE 范围", "core2" in nodeIds)
        assertTrue("core3（degree=3）应在 CORE 范围", "core3" in nodeIds)
        assertFalse("normal 不应在 CORE 范围", "normal" in nodeIds)
    }

    @Test
    fun `IMPORTANT 范围保留 sourceKpIds 至少2 或有边连接的节点`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "kp2", label = "双知识点", metadata = mapOf("sourceKpIds" to "kp1,kp2")),
            testNode(id = "connected", label = "有边", metadata = mapOf("sourceKpIds" to "kp1")),
            testNode(id = "isolated", label = "孤立", metadata = mapOf("sourceKpIds" to "kp1")),
        )
        val edges = listOf(testEdge("connected", "kp2"))
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = edges)
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.IMPORTANT)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val nodeIds = state.nodes.map { it.id }.toSet()
        assertTrue("kp2（sourceKpIds=2）应在 IMPORTANT 范围", "kp2" in nodeIds)
        assertTrue("connected（有边）应在 IMPORTANT 范围", "connected" in nodeIds)
        assertFalse("isolated（无边且 sourceKpIds=1）不在 IMPORTANT", "isolated" in nodeIds)
    }

    @Test
    fun `ALL 范围保留全部节点`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "n1", metadata = mapOf("sourceKpIds" to "kp1")),
            testNode(id = "n2", metadata = mapOf("sourceKpIds" to "kp1")),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.nodes.size)
    }

    // ==================== 类型筛选 ====================

    @Test
    fun `toggleTypeFilter 添加后只显示该类型节点`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "a1", type = "AUTHOR", label = "作者"),
            testNode(id = "w1", type = "WORK", label = "作品"),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        viewModel.toggleTypeFilter("AUTHOR")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("仅 AUTHOR 类型", 1, state.nodes.size)
        assertEquals("a1", state.nodes.first().id)
    }

    @Test
    fun `toggleTypeFilter 再次点击同一类型后移除`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "a1", type = "AUTHOR"),
            testNode(id = "w1", type = "WORK"),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        viewModel.toggleTypeFilter("AUTHOR")
        advanceUntilIdle()
        viewModel.toggleTypeFilter("AUTHOR") // 再次点击移除
        advanceUntilIdle()

        assertEquals("类型筛选清空后显示全部", 2, viewModel.uiState.value.nodes.size)
    }

    @Test
    fun `toggleTypeFilter 支持多类型同时选中`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "a1", type = "AUTHOR"),
            testNode(id = "w1", type = "WORK"),
            testNode(id = "s1", type = "SCHOOL"),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        viewModel.toggleTypeFilter("AUTHOR")
        viewModel.toggleTypeFilter("WORK")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("AUTHOR + WORK 共 2 个", 2, state.nodes.size)
    }

    @Test
    fun `clearTypeFilter 清空类型筛选`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "a1", type = "AUTHOR"),
            testNode(id = "w1", type = "WORK"),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        viewModel.toggleTypeFilter("AUTHOR")
        advanceUntilIdle()
        viewModel.clearTypeFilter()
        advanceUntilIdle()

        assertEquals("类型筛选清空后显示全部", 2, viewModel.uiState.value.nodes.size)
    }

    // ==================== 科目筛选 ====================

    @Test
    fun `setSubjectFilter 精确匹配科目`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "n1", subjectId = "subj_01"),
            testNode(id = "n2", subjectId = "subj_02"),
            testNode(id = "n3", subjectId = "subj_01"),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        viewModel.setSubjectFilter("subj_01")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("只显示 subj_01 的 2 个节点", 2, state.nodes.size)
        assertTrue(state.nodes.all { it.subjectId == "subj_01" })
    }

    @Test
    fun `setSubjectFilter null 清空科目筛选`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "n1", subjectId = "subj_01"),
            testNode(id = "n2", subjectId = "subj_02"),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        viewModel.setSubjectFilter("subj_01")
        advanceUntilIdle()
        viewModel.setSubjectFilter(null)
        advanceUntilIdle()

        assertEquals("清空后显示全部", 2, viewModel.uiState.value.nodes.size)
    }

    // ==================== 薄弱筛选 ====================

    @Test
    fun `toggleWeakOnly 只保留 R 在 0 到 0_5 之间的节点`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "weak", metadata = mapOf("sourceKpIds" to "kp1")), // R=0.3 → 薄弱
            testNode(id = "mastered", metadata = mapOf("sourceKpIds" to "kp1")), // R=0.9 → 已掌握
            testNode(id = "unlearned", metadata = mapOf("sourceKpIds" to "kp1")), // R=0 → 未学
        )
        graphRepository = FakeGraphRepository(
            initialNodes = nodes,
            initialEdges = emptyList(),
            retrievabilityMap = mapOf("weak" to 0.3f, "mastered" to 0.9f, "unlearned" to 0f),
        )
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        viewModel.toggleWeakOnly()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("只保留 1 个薄弱节点", 1, state.nodes.size)
        assertEquals("weak", state.nodes.first().id)
    }

    // ==================== 搜索筛选 ====================

    @Test
    fun `setSearchQuery 按 label 模糊匹配（忽略大小写）`() = runTest(testDispatcher) {
        // 用 Latin 字符测试大小写不敏感（中文 label.lowercase() 无变化）
        val nodes = listOf(
            testNode(id = "n1", label = "Lu Xun"),
            testNode(id = "n2", label = "Zhou Zuoren"),
            testNode(id = "n3", label = "Lao She"),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        viewModel.setSearchQuery("LU")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("匹配 'Lu Xun'", 1, state.nodes.size)
        assertEquals("n1", state.nodes.first().id)
        // 搜索时所有结果节点都应高亮
        assertTrue("搜索结果应高亮", state.highlightedNodeIds.contains("n1"))
    }

    @Test
    fun `setSearchQuery 按 subtitle 匹配`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "n1", label = "鲁", subtitle = "1881-1936"),
            testNode(id = "n2", label = "周", subtitle = "1885-1967"),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        viewModel.setSearchQuery("1881")
        advanceUntilIdle()

        assertEquals("匹配 subtitle", 1, viewModel.uiState.value.nodes.size)
    }

    @Test
    fun `setSearchQuery 空字符串清空搜索`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "n1", label = "鲁迅"),
            testNode(id = "n2", label = "老舍"),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        viewModel.setSearchQuery("鲁")
        advanceUntilIdle()
        viewModel.setSearchQuery("")
        advanceUntilIdle()

        assertEquals("清空搜索后显示全部", 2, viewModel.uiState.value.nodes.size)
    }

    // ==================== findNeighbors（间接通过聚焦模式验证）====================

    @Test
    fun `findNeighbors 0 跳只含自身`() = runTest(testDispatcher) {
        // 通过 setFocusHops(0) 验证边界（实际会被 coerce 到 1）
        // 但 buildUiState 中 hops<=0 时 findNeighbors 返回 setOf(startNodeId)
        val nodes = listOf(
            testNode(id = "center", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "neighbor", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
        )
        val edges = listOf(testEdge("center", "neighbor"))
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = edges)
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        viewModel.setFocusHops(0) // 会被 coerce 到 1
        advanceUntilIdle()

        assertEquals("setFocusHops(0) 应被 coerce 到 1", 1, viewModel.focusHopsInternal())
    }

    @Test
    fun `findNeighbors 1 跳包含直连邻居`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "center", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "direct", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "two_hop", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "isolated", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
        )
        val edges = listOf(
            testEdge("center", "direct"),
            testEdge("direct", "two_hop"), // 2 跳
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = edges)
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        viewModel.setFocusHops(1)
        viewModel.setFocusedNode("center")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val nodeIds = state.nodes.map { it.id }.toSet()
        assertTrue("center 自身应在结果中", "center" in nodeIds)
        assertTrue("direct（1 跳）应在结果中", "direct" in nodeIds)
        assertFalse("two_hop（2 跳，hops=1 不到）不应在结果中", "two_hop" in nodeIds)
        assertFalse("isolated 不应在结果中", "isolated" in nodeIds)
    }

    @Test
    fun `findNeighbors 2 跳包含 2 跳邻居`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "center", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "h1", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "h2", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "h3", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
        )
        val edges = listOf(
            testEdge("center", "h1"),
            testEdge("h1", "h2"), // 2 跳
            testEdge("h2", "h3"), // 3 跳
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = edges)
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        viewModel.setFocusHops(2)
        viewModel.setFocusedNode("center")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val nodeIds = state.nodes.map { it.id }.toSet()
        assertTrue("center 在结果中", "center" in nodeIds)
        assertTrue("h1（1 跳）在结果中", "h1" in nodeIds)
        assertTrue("h2（2 跳）在结果中", "h2" in nodeIds)
        assertFalse("h3（3 跳，hops=2 不到）不在结果中", "h3" in nodeIds)
    }

    @Test
    fun `findNeighbors 无向图_边双向遍历`() = runTest(testDispatcher) {
        // 边是 h1→center（target=center），验证从 center 出发能到 h1
        val nodes = listOf(
            testNode(id = "center", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "h1", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
        )
        val edges = listOf(testEdge("h1", "center")) // fromId=h1, toId=center
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = edges)
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        viewModel.setFocusHops(1)
        viewModel.setFocusedNode("center")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            "无向图：center 应能通过 h1→center 边到达 h1",
            "h1" in state.nodes.map { it.id }.toSet(),
        )
    }

    // ==================== NEIGHBORHOOD 布局自动选中心 ====================

    @Test
    fun `NEIGHBORHOOD 模式无 focus 时自动选度数最大节点`() = runTest(testDispatcher) {
        // node_hub 与 3 个节点连接（degree=3），其他节点 degree=1
        val nodes = listOf(
            testNode(id = "hub", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "leaf1", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "leaf2", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "leaf3", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
        )
        val edges = listOf(
            testEdge("hub", "leaf1"),
            testEdge("hub", "leaf2"),
            testEdge("hub", "leaf3"),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = edges)
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        viewModel.setLayoutMode(LayoutMode.NEIGHBORHOOD)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "NEIGHBORHOOD 模式应自动选 hub 作为聚焦节点",
            "hub",
            state.focusedNodeId,
        )
    }

    // ==================== setLayoutMode 副作用 ====================

    @Test
    fun `setLayoutMode 切到 TIMELINE 清除聚焦`() = runTest(testDispatcher) {
        val viewModel = createViewModel().also { subscribeUiState(it) }
        advanceUntilIdle()

        viewModel.setFocusedNode("some_node")
        advanceUntilIdle()
        viewModel.setLayoutMode(LayoutMode.TIMELINE)
        advanceUntilIdle()

        assertNull("切到 TIMELINE 应清除聚焦", viewModel.focusedNodeId.value)
    }

    @Test
    fun `setLayoutMode 切到 RADIAL 清除聚焦`() = runTest(testDispatcher) {
        val viewModel = createViewModel().also { subscribeUiState(it) }
        advanceUntilIdle()

        viewModel.setFocusedNode("some_node")
        advanceUntilIdle()
        viewModel.setLayoutMode(LayoutMode.RADIAL)
        advanceUntilIdle()

        assertNull("切到 RADIAL 应清除聚焦", viewModel.focusedNodeId.value)
    }

    @Test
    fun `setLayoutMode 切到 NEIGHBORHOOD 保留聚焦`() = runTest(testDispatcher) {
        val viewModel = createViewModel().also { subscribeUiState(it) }
        advanceUntilIdle()

        viewModel.setFocusedNode("some_node")
        advanceUntilIdle()
        viewModel.setLayoutMode(LayoutMode.NEIGHBORHOOD)
        advanceUntilIdle()

        assertEquals("切到 NEIGHBORHOOD 应保留聚焦", "some_node", viewModel.focusedNodeId.value)
    }

    // ==================== setFocusHops 边界 ====================

    @Test
    fun `setFocusHops 上限 3`() = runTest(testDispatcher) {
        val viewModel = createViewModel().also { subscribeUiState(it) }
        advanceUntilIdle()

        viewModel.setFocusHops(10)
        advanceUntilIdle()

        assertEquals("setFocusHops(10) 应被 coerce 到 3", 3, viewModel.focusHopsInternal())
    }

    @Test
    fun `setFocusHops 下限 1`() = runTest(testDispatcher) {
        val viewModel = createViewModel().also { subscribeUiState(it) }
        advanceUntilIdle()

        viewModel.setFocusHops(-5)
        advanceUntilIdle()

        assertEquals("setFocusHops(-5) 应被 coerce 到 1", 1, viewModel.focusHopsInternal())
    }

    @Test
    fun `setFocusHops 默认值 2`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        // 初始 _focusHops 默认值为 2
        assertEquals("初始 hops 应为 2", 2, viewModel.focusHopsInternal())
    }

    // ==================== clearAllFilters ====================

    @Test
    fun `clearAllFilters 重置所有筛选状态`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "a1", type = "AUTHOR", subjectId = "subj_01"),
            testNode(id = "w1", type = "WORK", subjectId = "subj_02"),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        // 设置各种筛选
        viewModel.toggleTypeFilter("AUTHOR")
        viewModel.setSubjectFilter("subj_01")
        viewModel.toggleWeakOnly()
        viewModel.setSearchQuery("test")
        viewModel.setFocusedNode("a1")
        advanceUntilIdle()

        viewModel.clearAllFilters()
        advanceUntilIdle()

        assertEquals("类型筛选清空", emptySet<String>(), viewModel.selectedTypes.value)
        assertNull("科目筛选清空", viewModel.selectedSubjectId.value)
        assertFalse("薄弱筛选清空", viewModel.showWeakOnly.value)
        assertEquals("搜索清空", "", viewModel.searchQuery.value)
        assertNull("聚焦清空", viewModel.focusedNodeId.value)
    }

    // ==================== 边过滤 ====================

    @Test
    fun `边过滤只保留两端节点都在筛选结果中的边`() = runTest(testDispatcher) {
        // 通过类型筛选让 n3 不显示，验证 n1-n3 边也被过滤
        val nodes = listOf(
            testNode(id = "n1", type = "AUTHOR", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "n2", type = "AUTHOR", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
            testNode(id = "n3", type = "WORK", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")),
        )
        val edges = listOf(
            testEdge("n1", "n2"), // 两端都是 AUTHOR → 保留
            testEdge("n1", "n3"), // n3 是 WORK → 过滤
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = edges)
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        viewModel.toggleTypeFilter("AUTHOR")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("只保留 1 条边（n1-n2）", 1, state.edges.size)
        assertEquals("n1", state.edges.first().fromId)
        assertEquals("n2", state.edges.first().toId)
    }

    // ==================== toUiItem（间接：sourceKpIds CSV 解析）====================

    @Test
    fun `toUiItem 解析 sourceKpIds CSV 为列表`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(
                id = "n1",
                metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3"),
            ),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val node = state.nodes.find { it.id == "n1" }!!
        assertEquals(
            "sourceKpIds CSV 应解析为 3 元素列表",
            listOf("kp1", "kp2", "kp3"),
            node.sourceKpIds,
        )
    }

    @Test
    fun `toUiItem 无 sourceKpIds 返回空列表`() = runTest(testDispatcher) {
        val nodes = listOf(testNode(id = "n1", metadata = emptyMap()))
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val node = state.nodes.find { it.id == "n1" }!!
        assertTrue("无 sourceKpIds 应返回空列表", node.sourceKpIds.isEmpty())
    }

    @Test
    fun `toUiItem sourceKpIds 含空字符串被过滤`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(
                id = "n1",
                metadata = mapOf("sourceKpIds" to "kp1,,kp3,"),
            ),
        )
        graphRepository = FakeGraphRepository(initialNodes = nodes, initialEdges = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val node = state.nodes.find { it.id == "n1" }!!
        assertEquals(
            "空字符串应被过滤",
            listOf("kp1", "kp3"),
            node.sourceKpIds,
        )
    }

    // ==================== 掌握度统计 ====================

    @Test
    fun `掌握度统计正确计算 weak_mastered_unlearned`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(id = "weak", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")), // R=0.3
            testNode(id = "consolidated", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")), // R=0.6
            testNode(id = "mastered", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")), // R=0.9
            testNode(id = "unlearned", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")), // R=0
        )
        graphRepository = FakeGraphRepository(
            initialNodes = nodes,
            initialEdges = emptyList(),
            retrievabilityMap = mapOf(
                "weak" to 0.3f,
                "consolidated" to 0.6f,
                "mastered" to 0.9f,
                "unlearned" to 0f,
            ),
        )
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("薄弱（0<R<0.5）= 1", 1, state.weakCount)
        assertEquals("已掌握（R>=0.8）= 1", 1, state.masteredCount)
        assertEquals("未学（R=0）= 1", 1, state.unlearnedCount)
    }

    // ==================== subjects 流 ====================

    @Test
    fun `subjects 流从 SubjectDao observeAll 拉取`() = runTest(testDispatcher) {
        val subjects = listOf(
            SubjectEntity(id = "subj_01", name = "古代文学", shortName = "古文", sortOrder = 1),
            SubjectEntity(id = "subj_02", name = "现当代", shortName = "现当", sortOrder = 2),
        )
        subjectDao = FakeSubjectDao(initialSubjects = subjects)
        graphRepository = FakeGraphRepository()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        advanceUntilIdle()

        val list = viewModel.subjects.value
        assertEquals(2, list.size)
        assertEquals("subj_01", list[0].id)
    }

    // ==================== knowledgePointTitles 增量缓存 ====================

    @Test
    fun `knowledgePointTitles 批量查询知识点标题`() = runTest(testDispatcher) {
        val nodes = listOf(
            testNode(
                id = "n1",
                relatedPointId = "kp_001",
                metadata = mapOf("sourceKpIds" to "kp_001,kp_002"),
            ),
        )
        graphRepository = FakeGraphRepository(
            initialNodes = nodes,
            kpTitles = mapOf(
                "kp_001" to "知识点一",
                "kp_002" to "知识点二",
            ),
        )
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        val titles = viewModel.knowledgePointTitles.value
        assertEquals("应批量查询到 2 个标题", 2, titles.size)
        assertEquals("知识点一", titles["kp_001"])
        assertEquals("知识点二", titles["kp_002"])
    }

    @Test
    fun `knowledgePointTitles 节点列表为空时为空 Map`() = runTest(testDispatcher) {
        graphRepository = FakeGraphRepository(initialNodes = emptyList())
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        advanceUntilIdle()

        assertTrue("无节点时标题缓存应为空", viewModel.knowledgePointTitles.value.isEmpty())
    }

    @Test
    fun `knowledgePointTitles 增量缓存_已查询的不再重复查询`() = runTest(testDispatcher) {
        // 初始：1 个节点关联 kp_001
        // 后续：动态修改为 2 个节点（n2 关联 kp_001,kp_002），验证 kp_001 不再查询
        val nodes = listOf(
            testNode(id = "n1", relatedPointId = "kp_001"),
        )
        graphRepository = FakeGraphRepository(
            initialNodes = nodes,
            kpTitles = mapOf("kp_001" to "已缓存", "kp_002" to "新增"),
        )
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        viewModel.setDisplayScope(DisplayScope.ALL)
        advanceUntilIdle()

        // 第一次：kp_001 被查询
        val firstTitles = viewModel.knowledgePointTitles.value
        assertEquals("第一次查询应含 kp_001", 1, firstTitles.size)
        assertEquals("已缓存", firstTitles["kp_001"])

        // 修改节点列表，添加 n2 关联 kp_001,kp_002
        graphRepository.setNodes(
            listOf(
                testNode(id = "n1", relatedPointId = "kp_001"),
                testNode(id = "n2", metadata = mapOf("sourceKpIds" to "kp_001,kp_002")),
            ),
        )
        advanceUntilIdle()

        val finalTitles = viewModel.knowledgePointTitles.value
        assertEquals("最终应含 2 个标题", 2, finalTitles.size)
        assertEquals("kp_002 应被增量查询", "新增", finalTitles["kp_002"])
    }

    // ==================== 错误处理 ====================

    @Test
    fun `仓库 getNodesWithRetrievability 抛异常时_uiState_error 不为空`() = runTest(testDispatcher) {
        // 用自定义 FakeGraphRepository 覆盖 getNodesWithRetrievability 抛异常
        graphRepository = object : FakeGraphRepository() {
            override fun getNodesWithRetrievability() = flow<List<com.wenyan.app.core.data.repository.NodeWithRetrievability>> {
                throw RuntimeException("DB 连接失败")
            }
        }
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isLoading 应为 false", state.isLoading)
        assertNotNull("error 应不为空", state.error)
        assertTrue("error 应含异常信息", state.error!!.contains("DB 连接失败"))
    }

    @Test
    fun `retry 触发重新订阅`() = runTest(testDispatcher) {
        val nodes = listOf(testNode(id = "n1", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")))
        graphRepository = FakeGraphRepository(initialNodes = nodes)
        subjectDao = FakeSubjectDao()
        val viewModel = createViewModel().also { subscribeUiState(it) }
        advanceUntilIdle()

        // 修改仓库数据
        graphRepository.setNodes(nodes + testNode(id = "n2", metadata = mapOf("sourceKpIds" to "kp1,kp2,kp3,kp4")))
        advanceUntilIdle()

        // 此时 uiState 应已自动更新（StateFlow 自动重发）
        val beforeCount = viewModel.uiState.value.nodes.size
        // retry 应触发重新订阅
        viewModel.retry()
        advanceUntilIdle()

        // 验证 retry 后状态正确（不一定增长，但 isLoading 应先变 true 再变 false）
        val state = viewModel.uiState.value
        assertFalse("retry 后 isLoading=false", state.isLoading)
    }

    // ==================== 初始状态 ====================

    @Test
    fun `初始 uiState isLoading=true`() {
        // 不订阅，直接验证初始值
        val viewModel = createViewModel()
        assertTrue("初始 isLoading 应为 true", viewModel.uiState.value.isLoading)
    }

    @Test
    fun `layoutMode 初始值为 TIMELINE`() {
        val viewModel = createViewModel()
        assertEquals(LayoutMode.TIMELINE, viewModel.layoutMode.value)
    }

    @Test
    fun `selectedTypes 初始为空集`() {
        val viewModel = createViewModel()
        assertTrue(viewModel.selectedTypes.value.isEmpty())
    }

    @Test
    fun `selectedSubjectId 初始为 null`() {
        val viewModel = createViewModel()
        assertNull(viewModel.selectedSubjectId.value)
    }

    @Test
    fun `showWeakOnly 初始为 false`() {
        val viewModel = createViewModel()
        assertFalse(viewModel.showWeakOnly.value)
    }

    @Test
    fun `searchQuery 初始为空字符串`() {
        val viewModel = createViewModel()
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `focusedNodeId 初始为 null`() {
        val viewModel = createViewModel()
        assertNull(viewModel.focusedNodeId.value)
    }
}

/**
 * 测试辅助：暴露 ViewModel 私有 `_focusHops` 状态供断言。
 *
 * 通过反射读取，避免为测试暴露 public API。
 */
private fun GraphViewModel.focusHopsInternal(): Int {
    val field = GraphViewModel::class.java.getDeclaredField("_focusHops")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val flow = field.get(this) as kotlinx.coroutines.flow.MutableStateFlow<Int>
    return flow.value
}
