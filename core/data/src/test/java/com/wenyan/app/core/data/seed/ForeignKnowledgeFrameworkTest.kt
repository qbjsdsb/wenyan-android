package com.wenyan.app.core.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForeignKnowledgeFrameworkTest {

    @Test
    fun `124 个外国文学知识点均有且仅有一个显式归属`() {
        assertEquals(124, ForeignKnowledgeFramework.assignments.size)
        assertTrue(ForeignKnowledgeFramework.validate(ForeignKnowledgeFramework.assignments.keys).isEmpty())
    }

    @Test
    fun `文学史分期与思潮边界归类正确`() {
        assertEquals(
            "foreign_late19_context",
            ForeignKnowledgeFramework.assignments["kp_00461"],
        )
        assertEquals(
            "foreign_late19_aesthetic_symbolist",
            ForeignKnowledgeFramework.assignments["kp_00575"],
        )
        assertEquals(
            "foreign_realism_britain",
            ForeignKnowledgeFramework.assignments["kp_00561"],
        )
        assertEquals(
            "foreign_ancient_tragedy",
            ForeignKnowledgeFramework.assignments["kp_00577"],
        )
        assertEquals(
            "foreign_modern_english",
            ForeignKnowledgeFramework.assignments["kp_00923"],
        )
        assertEquals(
            "foreign_late19_aesthetic_symbolist",
            ForeignKnowledgeFramework.assignments["kp_00926"],
        )
        assertEquals(
            "foreign_realism_britain",
            ForeignKnowledgeFramework.assignments["kp_00927"],
        )
        assertEquals(
            "foreign_realism_russia",
            ForeignKnowledgeFramework.assignments["kp_00928"],
        )
        assertEquals(
            "foreign_overview",
            ForeignKnowledgeFramework.assignments["kp_00938"],
        )
    }

    @Test
    fun `外国文学一级章节数量与审计清单一致`() {
        val nodeById = ForeignKnowledgeFramework.nodes.associateBy { it.id }

        fun rootOf(nodeId: String): String {
            var current = nodeId
            while (true) {
                current = nodeById[current]?.parentId ?: return current
            }
        }

        val counts = ForeignKnowledgeFramework.assignments.values
            .groupingBy(::rootOf)
            .eachCount()

        assertEquals(
            mapOf(
                "foreign_overview" to 1,
                "foreign_ancient" to 10,
                "foreign_medieval" to 3,
                "foreign_renaissance" to 5,
                "foreign_classicism" to 2,
                "foreign_enlightenment" to 4,
                "foreign_romanticism" to 5,
                "foreign_realism" to 25,
                "foreign_late_nineteenth" to 18,
                "foreign_modernism" to 19,
                "foreign_late_twentieth" to 32,
            ),
            counts,
        )
    }

    @Test
    fun `外国框架节点 ID 唯一且父级全部存在`() {
        val ids = ForeignKnowledgeFramework.nodes.map { it.id }
        val nodeIds = ids.toSet()

        assertEquals(ids.size, nodeIds.size)
        assertTrue(
            ForeignKnowledgeFramework.nodes
                .filter { it.parentId != null }
                .all { node -> node.parentId?.let(nodeIds::contains) == true },
        )
        assertEquals(
            ForeignKnowledgeFramework.SUBJECT_NAME,
            KnowledgeFrameworkRegistry.find("foreign", "外国文学")?.subjectName,
        )
    }

    @Test
    fun `框架缺少或多出知识点时校验明确报告`() {
        val errors = ForeignKnowledgeFramework.validate(
            ForeignKnowledgeFramework.assignments.keys
                .minus("kp_00461")
                .plus("kp_99999"),
        )

        assertTrue(errors.any { it.startsWith("知识点未归类") })
        assertTrue(errors.any { it.startsWith("框架包含不存在的知识点") })
    }
}
