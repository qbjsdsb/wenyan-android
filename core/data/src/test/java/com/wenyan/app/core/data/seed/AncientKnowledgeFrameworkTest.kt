package com.wenyan.app.core.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AncientKnowledgeFrameworkTest {

    @Test
    fun `498 个古代文学知识点均有且仅有一个显式归属`() {
        assertEquals(498, AncientKnowledgeFramework.assignments.size)
        assertTrue(AncientKnowledgeFramework.validate(AncientKnowledgeFramework.assignments.keys).isEmpty())
    }

    @Test
    fun `跨时期边界知识点归入语义正确的章节`() {
        assertEquals(
            "ancient_near_modern_overview",
            AncientKnowledgeFramework.assignments["kp_00084"],
        )
        assertEquals(
            "ancient_liao_jin",
            AncientKnowledgeFramework.assignments["kp_00130"],
        )
        assertEquals(
            "ancient_wei_jin_buddhism",
            AncientKnowledgeFramework.assignments["kp_00254"],
        )
        assertEquals(
            "ancient_wei_jin_poetry",
            AncientKnowledgeFramework.assignments["kp_00027"],
        )
        assertEquals(
            "ancient_wei_jin_social",
            AncientKnowledgeFramework.assignments["kp_00269"],
        )
        assertEquals(
            "ancient_yuan_sanqu",
            AncientKnowledgeFramework.assignments["kp_00314"],
        )
        assertEquals(
            "ancient_wei_jin_poetry",
            AncientKnowledgeFramework.assignments["kp_00912"],
        )
        assertEquals(
            "ancient_qing_fiction",
            AncientKnowledgeFramework.assignments["kp_00914"],
        )
        assertEquals(
            "ancient_ming_drama",
            AncientKnowledgeFramework.assignments["kp_00131"],
        )
        assertEquals(
            "ancient_sui_tang_fiction",
            AncientKnowledgeFramework.assignments["kp_00960"],
        )
        assertEquals(
            "ancient_pre_qin_thinkers",
            AncientKnowledgeFramework.assignments["kp_00961"],
        )
        assertEquals(
            "ancient_wei_jin_poetry",
            AncientKnowledgeFramework.assignments["kp_00965"],
        )
        assertEquals(
            "ancient_sui_tang_fiction",
            AncientKnowledgeFramework.assignments["kp_00972"],
        )
        assertEquals(
            "ancient_near_modern_fiction",
            AncientKnowledgeFramework.assignments["kp_00982"],
        )
        assertEquals(
            "ancient_pre_qin_history",
            AncientKnowledgeFramework.assignments["kp_00984"],
        )
        assertEquals(
            "ancient_sui_tang_middle_late",
            AncientKnowledgeFramework.assignments["kp_00986"],
        )
        assertEquals(
            "ancient_song_southern",
            AncientKnowledgeFramework.assignments["kp_00989"],
        )
        assertEquals(
            "ancient_near_modern_fiction",
            AncientKnowledgeFramework.assignments["kp_00993"],
        )
    }

    @Test
    fun `古代框架节点 ID 唯一且父级全部存在`() {
        val ids = AncientKnowledgeFramework.nodes.map { it.id }
        val nodeIds = ids.toSet()

        assertEquals(ids.size, nodeIds.size)
        assertTrue(
            AncientKnowledgeFramework.nodes
                .filter { it.parentId != null }
                .all { node -> node.parentId?.let(nodeIds::contains) == true },
        )
        assertEquals(
            AncientKnowledgeFramework.SUBJECT_NAME,
            KnowledgeFrameworkRegistry.find("ancient", "中国古代文学")?.subjectName,
        )
    }

    @Test
    fun `一级章节覆盖审计后的 498 个知识点`() {
        val nodeById = AncientKnowledgeFramework.nodes.associateBy { it.id }

        fun rootOf(nodeId: String): String {
            var current = nodeId
            while (true) {
                current = nodeById[current]?.parentId ?: return current
            }
        }

        val counts = AncientKnowledgeFramework.assignments.values
            .groupingBy(::rootOf)
            .eachCount()

        assertEquals(
            mapOf(
                "ancient_research" to 15,
                "ancient_pre_qin" to 59,
                "ancient_qin_han" to 63,
                "ancient_wei_jin" to 72,
                "ancient_sui_tang" to 31,
                "ancient_song_liao_jin" to 57,
                "ancient_yuan" to 61,
                "ancient_ming" to 35,
                "ancient_qing" to 67,
                "ancient_near_modern" to 38,
            ),
            counts,
        )
    }

    @Test
    fun `框架缺少或多出知识点时校验明确报告`() {
        val errors = AncientKnowledgeFramework.validate(
            AncientKnowledgeFramework.assignments.keys
                .minus("kp_00001")
                .plus("kp_99999"),
        )

        assertTrue(errors.any { it.startsWith("知识点未归类") })
        assertTrue(errors.any { it.startsWith("框架包含不存在的知识点") })
    }
}
