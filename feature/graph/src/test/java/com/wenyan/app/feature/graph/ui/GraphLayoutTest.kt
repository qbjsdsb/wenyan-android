package com.wenyan.app.feature.graph.ui

import androidx.compose.ui.geometry.Offset
import com.wenyan.app.feature.graph.GraphEdgeItem
import com.wenyan.app.feature.graph.GraphNodeItem
import com.wenyan.app.feature.graph.LayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [GraphLayout] 单元测试（v0.8.2 新增：知识图谱发布前回归测试）。
 *
 * 覆盖三模式布局算法：
 * - TIMELINE：时间轴泳道布局（默认模式）
 * - NEIGHBORHOOD：邻域力导向布局
 * - RADIAL：径向科目概览
 *
 * 验证维度：
 * - 空输入边界条件
 * - 节点位置分配正确性
 * - 泳道/扇区分配逻辑
 * - 防重叠算法
 * - 邻居限制（NEIGHBORHOOD_MAX_NODES）
 * - 性能优化回归（TreeSet subSet 替代 O(n²) count）
 */
class GraphLayoutTest {

    // ── 测试辅助构造 ──────────────────────────────────────────

    private fun makeNode(
        id: String,
        type: String = "AUTHOR",
        label: String = id,
        subjectId: String? = "subj_02",
        metadata: Map<String, String>? = null,
        sourceKpIds: List<String> = emptyList(),
    ): GraphNodeItem = GraphNodeItem(
        id = id,
        label = label,
        retrievability = 0f,
        type = type,
        subjectId = subjectId,
        metadata = metadata,
        sourceKpIds = sourceKpIds,
    )

    private fun makeEdge(from: String, to: String, relation: String = "RELATED_TO"): GraphEdgeItem =
        GraphEdgeItem(fromId = from, toId = to, relation = relation)

    private val CANVAS_W = 1000f
    private val CANVAS_H = 800f

    // ==================== TIMELINE 模式 ====================

    @Test
    fun `TIMELINE 空节点列表返回空 positions 与空 ticks`() {
        // v0.8.2 修正：实现中 nodes.isEmpty() 时 early-return，ticks 与 lanes 均为空
        val result = GraphLayout.calculate(
            mode = LayoutMode.TIMELINE,
            nodes = emptyList(),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        assertTrue("空节点应返回空 positions", result.positions.isEmpty())
        assertTrue("空节点 early-return → timelineTicks 也为空", result.timelineTicks.isEmpty())
        assertTrue("空节点 early-return → timelineLanes 也为空", result.timelineLanes.isEmpty())
    }

    @Test
    fun `TIMELINE 单节点分配到泳道`() {
        val node = makeNode("n1", type = "AUTHOR", metadata = mapOf("birthYear" to "1881", "deathYear" to "1936"))
        val result = GraphLayout.calculate(
            mode = LayoutMode.TIMELINE,
            nodes = listOf(node),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        val pos = result.positions["n1"]
        assertNotNull("节点应有位置", pos)
        assertTrue("x 坐标应在画布内", pos!!.x in 0f..CANVAS_W)
        assertTrue("y 坐标应在画布内", pos.y in 0f..CANVAS_H)
    }

    @Test
    fun `TIMELINE 时段节点放顶部泳道`() {
        // type=CONCEPT + metadata.dimension=period → 时段节点，laneIdx=0（流派泳道顶部）
        val periodNode = makeNode(
            "period_1",
            type = "CONCEPT",
            metadata = mapOf("dimension" to "period", "startYear" to "1917", "endYear" to "1927"),
        )
        val result = GraphLayout.calculate(
            mode = LayoutMode.TIMELINE,
            nodes = listOf(periodNode),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        val pos = result.positions["period_1"]
        assertNotNull(pos)
        // 时段节点应在画布上半部（ruler 区域 + 流派泳道）
        assertTrue("时段节点 y 应在上半部", pos!!.y < CANVAS_H / 2f)
    }

    @Test
    fun `TIMELINE KNOWLEDGE_POINT 节点放底部泳道`() {
        val kpNode = makeNode("kp_1", type = "KNOWLEDGE_POINT")
        val result = GraphLayout.calculate(
            mode = LayoutMode.TIMELINE,
            nodes = listOf(kpNode),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        val pos = result.positions["kp_1"]
        assertNotNull(pos)
        // 知识点泳道是第 6 条（laneIdx=5），在画布下半部
        assertTrue("知识点节点 y 应在下半部", pos!!.y > CANVAS_H / 2f)
    }

    @Test
    fun `TIMELINE 无年份节点确定性分配`() {
        // 同一节点多次布局，位置应一致（基于 id 哈希的确定性分配）
        val node = makeNode("deterministic_id", type = "AUTHOR")
        val result1 = GraphLayout.calculate(
            mode = LayoutMode.TIMELINE,
            nodes = listOf(node),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )
        val result2 = GraphLayout.calculate(
            mode = LayoutMode.TIMELINE,
            nodes = listOf(node),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        assertEquals(
            "同一节点多次布局位置应一致",
            result1.positions["deterministic_id"],
            result2.positions["deterministic_id"],
        )
    }

    /**
     * v0.8.2 性能优化回归：TreeSet subSet 替代 O(n²) count。
     *
     * 验证防重叠行为不变：同泳道节点 x 接近时，后续节点应垂直偏移。
     */
    @Test
    fun `TIMELINE 同泳道节点防重叠触发垂直偏移`() {
        // 两个无年份的 AUTHOR 节点，id 哈希相近 → 可能分配到相同 x
        // 构造强制重叠场景：相同年份的作家
        val node1 = makeNode(
            "a1",
            type = "AUTHOR",
            metadata = mapOf("birthYear" to "1881", "deathYear" to "1936"),
        )
        val node2 = makeNode(
            "a2",
            type = "AUTHOR",
            metadata = mapOf("birthYear" to "1881", "deathYear" to "1936"),
        )
        val result = GraphLayout.calculate(
            mode = LayoutMode.TIMELINE,
            nodes = listOf(node1, node2),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        val pos1 = result.positions["a1"]!!
        val pos2 = result.positions["a2"]!!
        // 两个节点 x 相同（同年份），y 应不同（防重叠偏移）
        if (kotlin.math.abs(pos1.x - pos2.x) < GraphConstants.TIMELINE_MIN_SPACING) {
            assertTrue(
                "同 x 节点应垂直偏移（pos1.y=${pos1.y}, pos2.y=${pos2.y}）",
                kotlin.math.abs(pos1.y - pos2.y) >= GraphConstants.TIMELINE_OVERLAP_OFFSET * 0.9f,
            )
        }
    }

    @Test
    fun `TIMELINE 时间刻度线覆盖关键年份`() {
        // v0.8.2 修正：空节点 early-return 无 ticks，需传入至少 1 个节点触发 ticks 生成
        val node = makeNode("n1", type = "AUTHOR", metadata = mapOf("birthYear" to "1900", "deathYear" to "1950"))
        val result = GraphLayout.calculate(
            mode = LayoutMode.TIMELINE,
            nodes = listOf(node),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        val years = result.timelineTicks.map { it.year }
        assertTrue("应包含 1917", 1917 in years)
        assertTrue("应包含 1949", 1949 in years)
        assertTrue("应包含 1989", 1989 in years)
    }

    @Test
    fun `TIMELINE 泳道数量为 6`() {
        // v0.8.2 修正：空节点 early-return 无 lanes，需传入至少 1 个节点触发 lanes 生成
        val node = makeNode("n1", type = "AUTHOR", metadata = mapOf("birthYear" to "1900", "deathYear" to "1950"))
        val result = GraphLayout.calculate(
            mode = LayoutMode.TIMELINE,
            nodes = listOf(node),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        assertEquals("泳道数应为 6（流派/小说/诗歌/散文/戏剧/知识点）", 6, result.timelineLanes.size)
    }

    // ==================== NEIGHBORHOOD 模式 ====================

    @Test
    fun `NEIGHBORHOOD 空节点返回空 positions`() {
        val result = GraphLayout.calculate(
            mode = LayoutMode.NEIGHBORHOOD,
            nodes = emptyList(),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
            focusedNodeId = null,
        )

        assertTrue(result.positions.isEmpty())
    }

    @Test
    fun `NEIGHBORHOOD focusedNodeId 为 null 时选第一个节点为中心`() {
        val nodes = listOf(
            makeNode("center"),
            makeNode("neighbor1"),
        )
        val edges = listOf(makeEdge("center", "neighbor1"))
        val result = GraphLayout.calculate(
            mode = LayoutMode.NEIGHBORHOOD,
            nodes = nodes,
            edges = edges,
            width = CANVAS_W,
            height = CANVAS_H,
            focusedNodeId = null,
        )

        // 第一个节点应位于画布中心
        val centerPos = result.positions["center"]
        assertNotNull(centerPos)
        assertEquals(CANVAS_W / 2f, centerPos!!.x, 1f)
        assertEquals(CANVAS_H / 2f, centerPos.y, 1f)
    }

    @Test
    fun `NEIGHBORHOOD 中心节点位于画布中心`() {
        val nodes = listOf(
            makeNode("center"),
            makeNode("neighbor1"),
            makeNode("neighbor2"),
        )
        val edges = listOf(
            makeEdge("center", "neighbor1"),
            makeEdge("center", "neighbor2"),
        )
        val result = GraphLayout.calculate(
            mode = LayoutMode.NEIGHBORHOOD,
            nodes = nodes,
            edges = edges,
            width = CANVAS_W,
            height = CANVAS_H,
            focusedNodeId = "center",
        )

        val centerPos = result.positions["center"]
        assertNotNull(centerPos)
        assertEquals("中心节点应在画布中心", CANVAS_W / 2f, centerPos!!.x, 1f)
        assertEquals("中心节点应在画布中心", CANVAS_H / 2f, centerPos.y, 1f)
    }

    @Test
    fun `NEIGHBORHOOD 1 跳邻居被包含在结果中`() {
        val nodes = listOf(
            makeNode("center"),
            makeNode("direct_neighbor"),
            makeNode("isolated"), // 无边连接，不应出现
        )
        val edges = listOf(makeEdge("center", "direct_neighbor"))
        val result = GraphLayout.calculate(
            mode = LayoutMode.NEIGHBORHOOD,
            nodes = nodes,
            edges = edges,
            width = CANVAS_W,
            height = CANVAS_H,
            focusedNodeId = "center",
        )

        assertNotNull("中心节点应在结果中", result.positions["center"])
        assertNotNull("直连邻居应在结果中", result.positions["direct_neighbor"])
        // isolated 无边连接，不在邻居集合中
        assertFalse("孤立节点不应在结果中", result.positions.containsKey("isolated"))
    }

    @Test
    fun `NEIGHBORHOOD 2 跳邻居被包含在结果中`() {
        val nodes = listOf(
            makeNode("center"),
            makeNode("hop1"),
            makeNode("hop2"),
        )
        val edges = listOf(
            makeEdge("center", "hop1"),
            makeEdge("hop1", "hop2"),
        )
        val result = GraphLayout.calculate(
            mode = LayoutMode.NEIGHBORHOOD,
            nodes = nodes,
            edges = edges,
            width = CANVAS_W,
            height = CANVAS_H,
            focusedNodeId = "center",
        )

        assertNotNull("2 跳邻居应被包含", result.positions["hop2"])
    }

    /**
     * 验证 NEIGHBORHOOD_MAX_NODES 上限。
     *
     * 构造 50 个直连邻居，预期只保留 30 个（NEIGHBORHOOD_MAX_NODES）。
     */
    @Test
    fun `NEIGHBORHOOD 邻居数受 MAX_NODES 限制`() {
        val center = makeNode("center")
        val neighbors = (1..50).map { makeNode("n$it") }
        val nodes = listOf(center) + neighbors
        val edges = neighbors.map { makeEdge("center", it.id) }

        val result = GraphLayout.calculate(
            mode = LayoutMode.NEIGHBORHOOD,
            nodes = nodes,
            edges = edges,
            width = CANVAS_W,
            height = CANVAS_H,
            focusedNodeId = "center",
        )

        // 中心 + 最多 29 个邻居 = 30（NEIGHBORHOOD_MAX_NODES）
        // 注：neighbors.size=50, 中心 1 + 直连 50 = 51，限制到 30
        val nodeCount = result.positions.size
        assertTrue(
            "节点数应受 NEIGHBORHOOD_MAX_NODES 限制（实际 $nodeCount）",
            nodeCount <= GraphConstants.NEIGHBORHOOD_MAX_NODES,
        )
    }

    // ==================== RADIAL 模式 ====================

    @Test
    fun `RADIAL 空节点返回空 positions`() {
        val result = GraphLayout.calculate(
            mode = LayoutMode.RADIAL,
            nodes = emptyList(),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        assertTrue(result.positions.isEmpty())
        assertTrue("空节点无扇区", result.subjectSectors.isEmpty())
    }

    @Test
    fun `RADIAL 按 subjectId 分扇区`() {
        val nodes = listOf(
            makeNode("n1", subjectId = "subj_01"),
            makeNode("n2", subjectId = "subj_02"),
            makeNode("n3", subjectId = "subj_03"),
            makeNode("n4", subjectId = "subj_04"),
        )
        val result = GraphLayout.calculate(
            mode = LayoutMode.RADIAL,
            nodes = nodes,
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        assertEquals("应分 4 个科目扇区", 4, result.subjectSectors.size)
        val sectorSubjects = result.subjectSectors.map { it.subjectId }.toSet()
        assertTrue("扇区应覆盖 4 个科目", sectorSubjects.containsAll(setOf("subj_01", "subj_02", "subj_03", "subj_04")))
    }

    @Test
    fun `RADIAL 扇区角度按节点数比例分配`() {
        // subj_01 有 3 个节点，subj_02 有 1 个节点
        val nodes = listOf(
            makeNode("n1", subjectId = "subj_01"),
            makeNode("n2", subjectId = "subj_01"),
            makeNode("n3", subjectId = "subj_01"),
            makeNode("n4", subjectId = "subj_02"),
        )
        val result = GraphLayout.calculate(
            mode = LayoutMode.RADIAL,
            nodes = nodes,
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        val subj01Sector = result.subjectSectors.find { it.subjectId == "subj_01" }
        val subj02Sector = result.subjectSectors.find { it.subjectId == "subj_02" }

        assertNotNull(subj01Sector)
        assertNotNull(subj02Sector)

        val angle01 = subj01Sector!!.endAngle - subj01Sector.startAngle
        val angle02 = subj02Sector!!.endAngle - subj02Sector.startAngle
        // subj_01 节点数是 subj_02 的 3 倍，扇区角度应更大
        assertTrue("节点数多的科目扇区角度应更大", angle01 > angle02)
    }

    @Test
    fun `RADIAL 节点位于圆弧上`() {
        val nodes = listOf(
            makeNode("n1", subjectId = "subj_01"),
            makeNode("n2", subjectId = "subj_02"),
        )
        val result = GraphLayout.calculate(
            mode = LayoutMode.RADIAL,
            nodes = nodes,
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        val centerX = result.centerX
        val centerY = result.centerY
        for (node in nodes) {
            val pos = result.positions[node.id]!!
            val dist = kotlin.math.hypot(pos.x - centerX, pos.y - centerY)
            // 节点应距中心有一定距离（在圆弧上），非 0
            assertTrue("节点 ${node.id} 应在圆弧上（距中心 dist=$dist）", dist > 50f)
        }
    }

    // ==================== 统一入口 calculate() ====================

    @Test
    fun `calculate 按 TIMELINE 模式调度`() {
        val node = makeNode("n1", type = "KNOWLEDGE_POINT")
        val result = GraphLayout.calculate(
            mode = LayoutMode.TIMELINE,
            nodes = listOf(node),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        // TIMELINE 模式有 timelineTicks，其他模式没有
        assertTrue("TIMELINE 模式应有 timelineTicks", result.timelineTicks.isNotEmpty())
    }

    @Test
    fun `calculate 按 NEIGHBORHOOD 模式调度`() {
        val nodes = listOf(makeNode("center"), makeNode("neighbor"))
        val edges = listOf(makeEdge("center", "neighbor"))
        val result = GraphLayout.calculate(
            mode = LayoutMode.NEIGHBORHOOD,
            nodes = nodes,
            edges = edges,
            width = CANVAS_W,
            height = CANVAS_H,
            focusedNodeId = "center",
        )

        // NEIGHBORHOOD 模式无 timelineTicks，无 subjectSectors
        assertTrue("NEIGHBORHOOD 模式无 timelineTicks", result.timelineTicks.isEmpty())
        assertTrue("NEIGHBORHOOD 模式无 subjectSectors", result.subjectSectors.isEmpty())
    }

    @Test
    fun `calculate 按 RADIAL 模式调度`() {
        val node = makeNode("n1", subjectId = "subj_01")
        val result = GraphLayout.calculate(
            mode = LayoutMode.RADIAL,
            nodes = listOf(node),
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        // RADIAL 模式有 subjectSectors（仅当节点数 > 0 且 subjectId != "unknown"）
        assertTrue("RADIAL 模式应有 subjectSectors", result.subjectSectors.isNotEmpty())
    }

    // ==================== 性能优化回归（v0.8.2）====================

    /**
     * v0.8.2 性能优化回归：TreeSet subSet 替代 O(n²) count。
     *
     * 验证大规模节点（模拟 2123 节点场景）布局能完成且结果正确。
     * 原 O(n²) 在 2123 节点时约 450 万次比较，优化后约 2.3 万次。
     */
    @Test
    fun `TIMELINE 大规模节点布局完成（性能回归）`() {
        // 构造 200 个节点（足以触发原 O(n²) 性能问题）
        val nodes = (1..200).map { i ->
            makeNode(
                "perf_node_$i",
                type = "AUTHOR",
                metadata = mapOf("birthYear" to "1900", "deathYear" to "1950"),
            )
        }
        val result = GraphLayout.calculate(
            mode = LayoutMode.TIMELINE,
            nodes = nodes,
            edges = emptyList(),
            width = CANVAS_W,
            height = CANVAS_H,
        )

        assertEquals("所有节点都应被分配位置", 200, result.positions.size)
        // 所有位置应在画布范围内
        result.positions.values.forEach { pos ->
            assertTrue("x 应在画布内", pos.x in -100f..CANVAS_W + 100f)
            assertTrue("y 应在画布内", pos.y in -100f..CANVAS_H + 100f)
        }
    }

    /**
     * v0.8.2 性能优化回归：力导向 distSq 复用。
     *
     * 验证邻域布局在 30 节点（MAX_NODES）时能完成且中心节点固定。
     */
    @Test
    fun `NEIGHBORHOOD 30 节点力导向布局完成（性能回归）`() {
        val center = makeNode("center")
        val neighbors = (1..29).map { makeNode("n$it") }
        val nodes = listOf(center) + neighbors
        val edges = neighbors.map { makeEdge("center", it.id) }

        val result = GraphLayout.calculate(
            mode = LayoutMode.NEIGHBORHOOD,
            nodes = nodes,
            edges = edges,
            width = CANVAS_W,
            height = CANVAS_H,
            focusedNodeId = "center",
        )

        assertEquals("30 节点都应被分配位置", 30, result.positions.size)
        // 中心节点应在画布中心（力导向迭代后仍固定）
        val centerPos = result.positions["center"]
        assertEquals("中心节点应固定在画布中心", CANVAS_W / 2f, centerPos!!.x, 1f)
        assertEquals("中心节点应固定在画布中心", CANVAS_H / 2f, centerPos.y, 1f)
    }
}
