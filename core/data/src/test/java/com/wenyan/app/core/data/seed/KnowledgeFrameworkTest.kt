package com.wenyan.app.core.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeFrameworkTest {

    @Test
    fun `框架节点 ID 唯一且父级全部存在`() {
        val ids = KnowledgeFramework.nodes.map { it.id }

        assertEquals("章节节点 ID 不应重复", ids.size, ids.toSet().size)
        val nodeIds = ids.toSet()
        assertTrue(
            "所有非根节点的 parentId 都应指向框架内节点",
            KnowledgeFramework.nodes
                .filter { it.parentId != null }
                .all { node -> node.parentId?.let(nodeIds::contains) == true },
        )
    }

    @Test
    fun `181 个现当代知识点均有且仅有一个显式归属`() {
        assertEquals(181, KnowledgeFramework.assignments.size)
        assertTrue(KnowledgeFramework.validate(KnowledgeFramework.assignments.keys).isEmpty())
        assertEquals("modern_history", KnowledgeFramework.assignments["kp_00578"])
        assertEquals("modern_first_lu", KnowledgeFramework.assignments["kp_00614"])
        assertEquals("modern_since_poetry", KnowledgeFramework.assignments["kp_00922"])
        assertEquals("modern_hongkong", KnowledgeFramework.assignments["kp_00958"])
    }

    @Test
    fun `种子缺少知识点时校验明确报告未归类`() {
        val errors = KnowledgeFramework.validate(setOf("kp_00578"))

        assertTrue(errors.any { it.startsWith("知识点未归类") })
        assertTrue(errors.any { it.startsWith("框架包含不存在的知识点") })
    }
}
