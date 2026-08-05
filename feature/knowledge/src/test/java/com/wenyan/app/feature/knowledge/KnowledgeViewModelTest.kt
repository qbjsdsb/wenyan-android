package com.wenyan.app.feature.knowledge

import com.wenyan.app.core.database.entity.KnowledgePointListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KnowledgeViewModel 单元测试（P1 修复）。
 *
 * 覆盖：
 * - filterByCategory 按 subjectName.contains(keyword) 筛选
 * - toUiItem 的 subject 取 subjectName 而非 contentSource
 *
 * 用 JUnit 原生断言（项目无 Google Truth 依赖，参考 AiAssistantViewModelTest）。
 */
class KnowledgeViewModelTest {

    @Test
    fun filterByCategory_ALL_returnsAllPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "中国现当代文学"),
            makePointWithSubject("kp3", "外国文学"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.ALL)
        assertEquals(3, result.size)
    }

    @Test
    fun filterByCategory_ANCIENT_returnsOnlyAncientPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "中国现当代文学"),
            makePointWithSubject("kp3", "外国文学"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.ANCIENT)
        assertEquals(1, result.size)
        assertEquals("kp1", result[0].id)
    }

    @Test
    fun filterByCategory_MODERN_returnsOnlyModernPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "中国现当代文学"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.MODERN)
        assertEquals(1, result.size)
        assertEquals("kp2", result[0].id)
    }

    @Test
    fun filterByCategory_FOREIGN_returnsOnlyForeignPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "外国文学"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.FOREIGN)
        assertEquals(1, result.size)
        assertEquals("kp2", result[0].id)
    }

    @Test
    fun filterByCategory_THEORY_returnsOnlyTheoryPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "文学理论"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.THEORY)
        assertEquals(1, result.size)
        assertEquals("kp2", result[0].id)
    }

    @Test
    fun toUiItem_subjectTakesSubjectNameNotContentSource() {
        // lean 投影无 contentSource 字段；验证 subject 取 subjectName
        val item = makeListItem(subjectName = "中国古代文学")
        val uiItem = KnowledgeViewModel.toUiItem(item)
        assertEquals("中国古代文学", uiItem.subject)
    }

    @Test
    fun toUiItem_summaryFallsBackToCoreConclusion() {
        val longCoreConclusion = "这是一段很长的核心结论，超过一百字需要被截断。".repeat(5)
        val item = makeListItem(
            summary = null,
            coreConclusion = longCoreConclusion,
        )
        val uiItem = KnowledgeViewModel.toUiItem(item)
        assertNotNull(uiItem.summary)
        assertTrue("summary should be at most 100 chars", uiItem.summary.length <= 100)
        assertEquals(longCoreConclusion.take(100), uiItem.summary)
    }

    @Test
    fun toUiItem_summaryNotNullUsesSummaryDirectly() {
        val item = makeListItem(
            summary = "人工编写的简短摘要",
            coreConclusion = "这是很长的核心结论，不应该被使用".repeat(10),
        )
        val uiItem = KnowledgeViewModel.toUiItem(item)
        assertEquals("人工编写的简短摘要", uiItem.summary)
    }

    @Test
    fun filterByCategory_emptyList_returnsEmptyList() {
        val points = emptyList<KnowledgePointListItem>()
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.ANCIENT)
        assertTrue("empty list should return empty", result.isEmpty())
    }

    @Test
    fun filterByCategory_subjectNameNotMatchingAnyCategory_returnsEmpty() {
        val points = listOf(
            makePointWithSubject("kp1", "未知科目"),
        )
        // 逐个测试非 ALL 分类
        KnowledgeCategory.entries.filter { it != KnowledgeCategory.ALL }.forEach { category ->
            val result = KnowledgeViewModel.filterByCategory(points, category)
            assertTrue(
                "subjectName '未知科目' should not match category $category",
                result.isEmpty(),
            )
        }
    }

    // P1-AUDIT-5 测试：null subjectName（LEFT JOIN 无有效科目关联的知识点）

    @Test
    fun filterByCategory_ALL_withNullSubjectName_returnsAllPoints() {
        // null subjectName 在 ALL 分类下应显示（不静默丢失数据）
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", subjectName = null),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.ALL)
        assertEquals(2, result.size)
    }

    @Test
    fun filterByCategory_nonAll_withNullSubjectName_excludesNullPoints() {
        // null subjectName 在具体分类下应排除（无法匹配 keyword）
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", subjectName = null),
        )
        KnowledgeCategory.entries.filter { it != KnowledgeCategory.ALL }.forEach { category ->
            val result = KnowledgeViewModel.filterByCategory(points, category)
            assertEquals(
                "null subjectName should be excluded from category $category",
                if (category == KnowledgeCategory.ANCIENT) 1 else 0,
                result.size,
            )
        }
    }

    @Test
    fun toUiItem_nullSubjectName_fallsBackToUnknown() {
        val item = makeListItem(subjectName = null)
        val uiItem = KnowledgeViewModel.toUiItem(item)
        assertEquals("未知科目", uiItem.subject)
    }

    // v0.8.20 P1-2 测试:toUiItem 透传 examFrequency,UI 层映射为中文 chip

    @Test
    fun toUiItem_passesThroughExamFrequency_high() {
        val item = makeListItem(examFrequency = "HIGH")
        val uiItem = KnowledgeViewModel.toUiItem(item)
        assertEquals("HIGH", uiItem.examFrequency)
    }

    @Test
    fun toUiItem_passesThroughExamFrequency_never() {
        val item = makeListItem(examFrequency = "NEVER")
        val uiItem = KnowledgeViewModel.toUiItem(item)
        assertEquals("NEVER", uiItem.examFrequency)
    }

    private fun makePointWithSubject(
        id: String,
        subjectName: String?,
    ) = KnowledgePointListItem(
        id = id,
        title = "测试知识点",
        summary = "测试摘要",
        coreConclusion = "测试核心结论",
        examFrequency = "NEVER",
        subjectName = subjectName,
    )

    private fun makeListItem(
        id: String = "kp1",
        subjectName: String? = "中国古代文学",
        summary: String? = "测试摘要",
        coreConclusion: String = "测试核心结论",
        examFrequency: String = "NEVER",
    ) = KnowledgePointListItem(
        id = id,
        title = "测试知识点",
        summary = summary,
        coreConclusion = coreConclusion,
        examFrequency = examFrequency,
        subjectName = subjectName,
    )
}
