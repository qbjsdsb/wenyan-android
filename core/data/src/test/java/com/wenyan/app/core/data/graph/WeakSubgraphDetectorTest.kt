package com.wenyan.app.core.data.graph

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WeakSubgraphDetector 单元测试（指向 main 源码）。
 *
 * 验证 checklist 项：
 * - C4.10: 验证识别R值最低的连通子图（BFS连通分量）
 * - C4.11: 验证优先推送该子图的卡片（getDailyPriorityCards返回prioritizedNodeIds）
 * - C4.12: 验证图谱可视化颜色映射R值（验证averageR计算正确）
 * - 验证子图>20节点时isTooLarge=true，prioritizedNodeIds按考频排序取前10
 * - 验证空图谱返回空结果
 *
 * 使用 [FakeGraphRepository] 实现 main 的 [GraphRepository] 接口。
 * R 值通过 retrievabilityMap 模拟，考频通过 examFrequencyMap 模拟。
 *
 * C4 修复说明：main 源码按 exam_frequency 排序（Spec 要求），非 retrievability。
 */
class WeakSubgraphDetectorTest {

    // C4.10: 验证识别R值最低的连通子图（BFS连通分量）
    @Test
    fun c4_10_detectsWeakestConnectedSubgraph() = runBlocking {
        // 两个连通分量：
        // 分量A（R较高）：a1(R=0.8) - a2(R=0.9)
        // 分量B（R较低）：b1(R=0.2) - b2(R=0.3)
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("a1", "节点A1"),
                testNode("a2", "节点A2"),
                testNode("b1", "节点B1"),
                testNode("b2", "节点B2"),
            ),
            edges = listOf(
                testEdge("a1", "a2"),
                testEdge("b1", "b2"),
            ),
            retrievabilityMap = mapOf(
                "a1" to 0.8f,
                "a2" to 0.9f,
                "b1" to 0.2f,
                "b2" to 0.3f,
            ),
        )
        val detector = WeakSubgraphDetector(repo)

        val result = detector.detectWeakSubgraph().first()

        // 应识别 R 值最低的连通子图（分量B）
        assertTrue("应识别R最低的连通子图", result.nodeIds.contains("b1"))
        assertTrue("应识别R最低的连通子图", result.nodeIds.contains("b2"))
        assertFalse("不应包含高R分量节点", result.nodeIds.contains("a1"))
        assertFalse("不应包含高R分量节点", result.nodeIds.contains("a2"))
    }

    // C4.10 补充：BFS 连通性验证（间接相连的节点也应在同一分量）
    @Test
    fun c4_10_bfsConnectivity_includesIndirectlyConnectedNodes() = runBlocking {
        // A - B - C 链式连接，D 独立
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("a", "节点A"),
                testNode("b", "节点B"),
                testNode("c", "节点C"),
                testNode("d", "节点D"),
            ),
            edges = listOf(
                testEdge("a", "b"),
                testEdge("b", "c"),
            ),
            retrievabilityMap = mapOf(
                "a" to 0.9f,
                "b" to 0.2f,
                "c" to 0.3f,
                "d" to 0.1f,
            ),
        )
        val detector = WeakSubgraphDetector(repo)

        val result = detector.detectWeakSubgraph().first()

        // D 独立且 R=0.1 最低，应被选为最弱子图
        assertTrue("独立低R节点应被识别", result.nodeIds.contains("d"))
        assertEquals(1, result.nodeIds.size)
    }

    // C4.11: 验证优先推送该子图的卡片（getDailyPriorityCards返回prioritizedNodeIds）
    @Test
    fun c4_11_getDailyPriorityCards_returnsPrioritizedNodeIds() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("weak1", "弱节点1"),
                testNode("weak2", "弱节点2"),
                testNode("strong", "强节点"),
            ),
            edges = listOf(
                testEdge("weak1", "weak2"),
            ),
            retrievabilityMap = mapOf(
                "weak1" to 0.2f,
                "weak2" to 0.3f,
                "strong" to 0.9f,
            ),
        )
        val detector = WeakSubgraphDetector(repo)

        val priorityCards = detector.getDailyPriorityCards().first()
        val subgraph = detector.detectWeakSubgraph().first()

        // getDailyPriorityCards 应返回 prioritizedNodeIds
        assertEquals(subgraph.prioritizedNodeIds, priorityCards)
        assertTrue("优先卡片应包含弱子图节点", priorityCards.isNotEmpty())
    }

    // C4.12: 验证图谱可视化颜色映射R值（验证averageR计算正确）
    @Test
    fun c4_12_averageR_calculatedCorrectly() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("n1", "节点1"),
                testNode("n2", "节点2"),
                testNode("n3", "节点3"),
            ),
            edges = listOf(
                testEdge("n1", "n2"),
                testEdge("n2", "n3"),
            ),
            retrievabilityMap = mapOf(
                "n1" to 0.2f,
                "n2" to 0.4f,
                "n3" to 0.6f,
            ),
        )
        val detector = WeakSubgraphDetector(repo)

        val result = detector.detectWeakSubgraph().first()

        // averageR = (0.2 + 0.4 + 0.6) / 3 = 0.4
        assertEquals(0.4f, result.averageR, 0.001f)
    }

    // C4.12 补充：averageR 用于颜色映射（R越低越红色）
    @Test
    fun c4_12_averageR_lowValueIndicatesWeakSubgraph() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(testNode("weak", "弱节点")),
            retrievabilityMap = mapOf("weak" to 0.1f),
        )
        val detector = WeakSubgraphDetector(repo)

        val result = detector.detectWeakSubgraph().first()

        assertTrue("低R子图 averageR 应较低（用于红色映射）", result.averageR < 0.3f)
    }

    // C4 修复验证：子图>20节点时按考频排序取前10（非按R值排序）
    @Test
    fun subgraphTooLarge_prioritizedByExamFrequency_takesTop10() = runBlocking {
        // 创建 25 个连通节点
        // 考频分布：n1-n5=HIGH, n6-n10=MEDIUM, n11-n15=LOW, n16-n25=NEVER
        val nodes = (1..25).map { i ->
            testNode("n$i", "节点$i")
        }
        val edges = (1..24).map { i ->
            testEdge("n$i", "n${i + 1}")
        }
        val examFrequencyMap = (1..25).associate { i ->
            "n$i" to when {
                i <= 5 -> "HIGH"
                i <= 10 -> "MEDIUM"
                i <= 15 -> "LOW"
                else -> "NEVER"
            }
        }
        // R 值反向设置（n1 最高 R，n25 最低 R），验证排序按考频而非 R
        val retrievabilityMap = (1..25).associate { i ->
            "n$i" to (1.0f - i * 0.03f)
        }
        val repo = FakeGraphRepository(
            nodes = nodes,
            edges = edges,
            retrievabilityMap = retrievabilityMap,
            examFrequencyMap = examFrequencyMap,
        )
        val detector = WeakSubgraphDetector(repo)

        val result = detector.detectWeakSubgraph().first()

        assertTrue("子图>20节点时 isTooLarge 应为 true", result.isTooLarge)
        assertEquals(25, result.nodeIds.size)
        assertEquals("子图过大时 prioritizedNodeIds 应取前10", 10, result.prioritizedNodeIds.size)

        // 验证取的是考频最高的前10个（n1~n10：HIGH + MEDIUM）
        for (i in 1..10) {
            assertTrue(
                "prioritizedNodeIds 应包含考频最高的前10个节点（n$i）",
                result.prioritizedNodeIds.contains("n$i"),
            )
        }
        // n11 为 LOW，不应在优先列表中
        assertFalse(
            "考频为 LOW 的节点不应在 prioritizedNodeIds 中",
            result.prioritizedNodeIds.contains("n11"),
        )
    }

    // 验证子图≤20节点时isTooLarge=false，prioritizedNodeIds包含全部
    @Test
    fun subgraphNotTooLarge_isTooLargeFalseAndPrioritizedIncludesAll() = runBlocking {
        val nodes = (1..15).map { i ->
            testNode("n$i", "节点$i")
        }
        val edges = (1..14).map { i ->
            testEdge("n$i", "n${i + 1}")
        }
        val retrievabilityMap = (1..15).associate { i ->
            "n$i" to (i * 0.05f)
        }
        val repo = FakeGraphRepository(
            nodes = nodes,
            edges = edges,
            retrievabilityMap = retrievabilityMap,
        )
        val detector = WeakSubgraphDetector(repo)

        val result = detector.detectWeakSubgraph().first()

        assertFalse("子图≤20节点时 isTooLarge 应为 false", result.isTooLarge)
        assertEquals(15, result.nodeIds.size)
        assertEquals("子图不过大时 prioritizedNodeIds 应包含全部节点", 15, result.prioritizedNodeIds.size)
    }

    // 验证空图谱返回空结果
    @Test
    fun emptyGraph_returnsEmptyResult() = runBlocking {
        val repo = FakeGraphRepository()
        val detector = WeakSubgraphDetector(repo)

        val result = detector.detectWeakSubgraph().first()

        assertTrue("空图谱 nodeIds 应为空", result.nodeIds.isEmpty())
        assertEquals(0f, result.averageR, 0.001f)
        assertFalse("空图谱 isTooLarge 应为 false", result.isTooLarge)
        assertTrue("空图谱 prioritizedNodeIds 应为空", result.prioritizedNodeIds.isEmpty())
    }

    // 验证常量值
    @Test
    fun constants_areCorrect() {
        assertEquals(20, WeakSubgraphDetector.MAX_SUBGRAPH_SIZE)
        assertEquals(10, WeakSubgraphDetector.PRIORITY_SUBSET_SIZE)
    }
}
