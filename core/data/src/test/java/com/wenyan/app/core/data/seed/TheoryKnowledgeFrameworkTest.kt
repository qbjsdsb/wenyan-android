package com.wenyan.app.core.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TheoryKnowledgeFrameworkTest {

    @Test
    fun `190 个文学理论知识点均有且仅有一个显式归属`() {
        assertEquals(190, TheoryKnowledgeFramework.assignments.size)
        assertTrue(TheoryKnowledgeFramework.validate(TheoryKnowledgeFramework.assignments.keys).isEmpty())
    }

    @Test
    fun `理论概念边界归类正确`() {
        assertEquals(
            "theory_foundations_subject",
            TheoryKnowledgeFramework.assignments["kp_00727"],
        )
        assertEquals(
            "theory_foundations_concepts",
            TheoryKnowledgeFramework.assignments["kp_00736"],
        )
        assertEquals(
            "theory_criticism_standards",
            TheoryKnowledgeFramework.assignments["kp_00739"],
        )
        assertEquals(
            "theory_criticism_modes",
            TheoryKnowledgeFramework.assignments["kp_00748"],
        )
        assertEquals(
            "theory_work_text",
            TheoryKnowledgeFramework.assignments["kp_00750"],
        )
        assertEquals(
            "theory_work_poetry",
            TheoryKnowledgeFramework.assignments["kp_00764"],
        )
        assertEquals(
            "theory_work_narrative",
            TheoryKnowledgeFramework.assignments["kp_00780"],
        )
        assertEquals(
            "theory_foundations_author",
            TheoryKnowledgeFramework.assignments["kp_00789"],
        )
        assertEquals(
            "theory_reception_theory",
            TheoryKnowledgeFramework.assignments["kp_00794"],
        )
        assertEquals(
            "theory_criticism_ideology",
            TheoryKnowledgeFramework.assignments["kp_00800"],
        )
        assertEquals(
            "theory_history_literary",
            TheoryKnowledgeFramework.assignments["kp_00809"],
        )
        assertEquals(
            "theory_history_schools",
            TheoryKnowledgeFramework.assignments["kp_00817"],
        )
        assertEquals(
            "theory_activity_marxism",
            TheoryKnowledgeFramework.assignments["kp_00842"],
        )
        assertEquals(
            "theory_creation_process",
            TheoryKnowledgeFramework.assignments["kp_00857"],
        )
        assertEquals(
            "theory_creation_types",
            TheoryKnowledgeFramework.assignments["kp_00870"],
        )
        assertEquals(
            "theory_work_genre",
            TheoryKnowledgeFramework.assignments["kp_00874"],
        )
        assertEquals(
            "theory_reception_process",
            TheoryKnowledgeFramework.assignments["kp_00892"],
        )
        assertEquals(
            "theory_criticism_standards",
            TheoryKnowledgeFramework.assignments["kp_00901"],
        )
        assertEquals(
            "theory_history_supplement",
            TheoryKnowledgeFramework.assignments["kp_00929"],
        )
    }

    @Test
    fun `文学理论一级章节数量与审计清单一致`() {
        val nodeById = TheoryKnowledgeFramework.nodes.associateBy { it.id }

        fun rootOf(nodeId: String): String {
            var current = nodeId
            while (true) {
                current = nodeById[current]?.parentId ?: return current
            }
        }

        val counts = TheoryKnowledgeFramework.assignments.values
            .groupingBy(::rootOf)
            .eachCount()

        assertEquals(
            mapOf(
                "theory_foundations" to 35,
                "theory_activity" to 15,
                "theory_creation" to 17,
                "theory_work" to 55,
                "theory_reception" to 18,
                "theory_criticism" to 22,
                "theory_history" to 28,
            ),
            counts,
        )
    }

    @Test
    fun `文学理论框架节点 ID 唯一且父级全部存在`() {
        val ids = TheoryKnowledgeFramework.nodes.map { it.id }
        val nodeIds = ids.toSet()

        assertEquals(ids.size, nodeIds.size)
        assertTrue(
            TheoryKnowledgeFramework.nodes
                .filter { it.parentId != null }
                .all { node -> node.parentId?.let(nodeIds::contains) == true },
        )
        assertEquals(
            TheoryKnowledgeFramework.SUBJECT_NAME,
            KnowledgeFrameworkRegistry.find("theory", "文学理论")?.subjectName,
        )
    }

    @Test
    fun `框架缺少或多出知识点时校验明确报告`() {
        val errors = TheoryKnowledgeFramework.validate(
            TheoryKnowledgeFramework.assignments.keys
                .minus("kp_00727")
                .plus("kp_99999"),
        )

        assertTrue(errors.any { it.startsWith("知识点未归类") })
        assertTrue(errors.any { it.startsWith("框架包含不存在的知识点") })
    }
}
