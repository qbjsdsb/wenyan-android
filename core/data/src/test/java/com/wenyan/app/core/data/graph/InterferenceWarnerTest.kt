package com.wenyan.app.core.data.graph

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InterferenceWarner 单元测试（指向 main 源码）。
 *
 * 验证 checklist 项：
 * - C4.13: 验证检测连续复习相邻节点（同一流派两个作家）
 * - C4.14: 验证主动插入区分卡（generateDistinctionCard返回对比数据）
 * - C4.15: 验证提示用户注意区分（suggestion="建议复习区分卡"）
 * - 验证无相邻节点连续复习时返回null
 * - 验证少于2个节点时返回null
 *
 * 使用 [FakeGraphRepository] 实现 main 的 [GraphRepository] 接口。
 * 相邻关系通过 edges 列表模拟（main 源码通过 getAllEdges 检测相邻）。
 */
class InterferenceWarnerTest {

    // C4.13: 验证检测连续复习相邻节点（同一流派两个作家）
    @Test
    fun c4_13_detectsConsecutiveAdjacentNodes() = runBlocking {
        // 黄庭坚和秦观同属江西诗派（通过边连接）
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("huangtingjian", "黄庭坚"),
                testNode("qinguan", "秦观"),
            ),
            edges = listOf(
                testEdge("huangtingjian", "qinguan", "SAME_PERIOD"),
            ),
        )
        val warner = InterferenceWarner(repo)

        val warning = warner.checkInterference(listOf("huangtingjian", "qinguan")).first()

        assertNotNull("连续复习相邻节点应返回警告", warning)
        assertEquals("huangtingjian", warning!!.node1Id)
        assertEquals("qinguan", warning.node2Id)
        // relationType 应为边的类型
        assertEquals("SAME_PERIOD", warning.relationType)
    }

    // C4.13 补充：非相邻节点连续复习不触发警告
    @Test
    fun c4_13_noWarning_forNonAdjacentNodes() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("libai", "李白"),
                testNode("dufu", "杜甫"),
                testNode("wangwei", "王维"),
            ),
            edges = listOf(
                testEdge("libai", "dufu", "SAME_PERIOD"),
            ),
        )
        val warner = InterferenceWarner(repo)

        // 王维和李白不相邻（无直接边连接）
        val warning = warner.checkInterference(listOf("wangwei", "libai")).first()

        assertNull("非相邻节点连续复习不应返回警告", warning)
    }

    // C4.14: 验证主动插入区分卡（generateDistinctionCard返回对比数据）
    @Test
    fun c4_14_generateDistinctionCard_returnsComparisonData() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("huangtingjian", "黄庭坚", subtitle = "1045-1105"),
                testNode("qinguan", "秦观", subtitle = "1049-1100"),
            ),
        )
        val warner = InterferenceWarner(repo)

        val card = warner.generateDistinctionCard("huangtingjian", "qinguan").first()

        assertEquals("黄庭坚", card.item1)
        assertEquals("秦观", card.item2)
        assertTrue("区分卡应包含差异点列表", card.differences.isNotEmpty())
        // subtitle 不同应被检测到
        assertTrue("应检测到 subtitle 差异", card.differences.any { it.contains("1045") || it.contains("1049") })
    }

    // C4.14 补充：区分卡差异点应包含具体内容
    @Test
    fun c4_14_distinctionCard_differencesNotEmpty() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("n1", "节点1"),
                testNode("n2", "节点2"),
            ),
        )
        val warner = InterferenceWarner(repo)

        val card = warner.generateDistinctionCard("n1", "n2").first()

        assertTrue("差异点列表应非空", card.differences.isNotEmpty())
        for (diff in card.differences) {
            assertTrue("每个差异点应非空", diff.isNotBlank())
        }
    }

    // C4.14 补充：metadata 差异应被检测到
    @Test
    fun c4_14_distinctionCard_detectsMetadataDifferences() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("n1", "节点1", metadata = mapOf("流派" to "江西诗派")),
                testNode("n2", "节点2", metadata = mapOf("流派" to "婉约派")),
            ),
        )
        val warner = InterferenceWarner(repo)

        val card = warner.generateDistinctionCard("n1", "n2").first()

        assertTrue("应检测到 metadata 差异", card.differences.any { it.contains("江西诗派") || it.contains("婉约派") })
    }

    // C4.15: 验证提示用户注意区分（suggestion="建议复习区分卡"）
    @Test
    fun c4_15_suggestion_isReviewDistinctionCard() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("huangtingjian", "黄庭坚"),
                testNode("qinguan", "秦观"),
            ),
            edges = listOf(
                testEdge("huangtingjian", "qinguan", "SAME_PERIOD"),
            ),
        )
        val warner = InterferenceWarner(repo)

        val warning = warner.checkInterference(listOf("huangtingjian", "qinguan")).first()

        assertNotNull(warning)
        assertEquals("建议复习区分卡", warning!!.suggestion)
    }

    // 验证无相邻节点连续复习时返回null
    @Test
    fun noAdjacentNodes_returnsNull() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("n1", "节点1"),
                testNode("n2", "节点2"),
                testNode("n3", "节点3"),
            ),
            // 无边
        )
        val warner = InterferenceWarner(repo)

        val warning = warner.checkInterference(listOf("n1", "n2", "n3")).first()

        assertNull("无相邻节点连续复习时应返回null", warning)
    }

    // 验证少于2个节点时返回null
    @Test
    fun fewerThanTwoNodes_returnsNull() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(testNode("n1", "节点1")),
        )
        val warner = InterferenceWarner(repo)

        val warningEmpty = warner.checkInterference(emptyList()).first()
        assertNull("空列表应返回null", warningEmpty)

        val warningSingle = warner.checkInterference(listOf("n1")).first()
        assertNull("单个节点应返回null", warningSingle)
    }

    // 验证多个连续复习中能检测到任一相邻对
    @Test
    fun detectsAdjacentPair_inMultipleConsecutiveReviews() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("a", "节点A"),
                testNode("b", "节点B"),
                testNode("c", "节点C"),
                testNode("d", "节点D"),
            ),
            edges = listOf(
                testEdge("c", "d", "SAME_SCHOOL"),
            ),
        )
        val warner = InterferenceWarner(repo)

        // a → b 不相邻，b → c 不相邻，c → d 相邻
        val warning = warner.checkInterference(listOf("a", "b", "c", "d")).first()

        assertNotNull("应检测到 c-d 相邻对", warning)
        assertEquals("c", warning!!.node1Id)
        assertEquals("d", warning.node2Id)
        assertEquals("SAME_SCHOOL", warning.relationType)
    }
}
