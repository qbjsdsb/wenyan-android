package com.wenyan.app.feature.knowledge

import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        assertEquals("kp1", result[0].point.id)
    }

    @Test
    fun filterByCategory_MODERN_returnsOnlyModernPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "中国现当代文学"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.MODERN)
        assertEquals(1, result.size)
        assertEquals("kp2", result[0].point.id)
    }

    @Test
    fun filterByCategory_FOREIGN_returnsOnlyForeignPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "外国文学"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.FOREIGN)
        assertEquals(1, result.size)
        assertEquals("kp2", result[0].point.id)
    }

    @Test
    fun filterByCategory_THEORY_returnsOnlyTheoryPoints() {
        val points = listOf(
            makePointWithSubject("kp1", "中国古代文学"),
            makePointWithSubject("kp2", "文学理论"),
        )
        val result = KnowledgeViewModel.filterByCategory(points, KnowledgeCategory.THEORY)
        assertEquals(1, result.size)
        assertEquals("kp2", result[0].point.id)
    }

    @Test
    fun toUiItem_subjectTakesSubjectNameNotContentSource() {
        val pointWithSubject = KnowledgePointWithSubject(
            point = makePoint(
                id = "kp1",
                contentSource = "TEXTBOOK_NATIVE",
            ),
            subjectName = "中国古代文学",
        )
        val uiItem = KnowledgeViewModel.toUiItem(pointWithSubject)
        assertEquals("中国古代文学", uiItem.subject)
        assertNotEquals("TEXTBOOK_NATIVE", uiItem.subject)
    }

    @Test
    fun toUiItem_summaryFallsBackToCoreConclusion() {
        val longCoreConclusion = "这是一段很长的核心结论，超过一百字需要被截断。".repeat(5)
        val pointWithSubject = KnowledgePointWithSubject(
            point = makePoint(
                id = "kp1",
                summary = null,
                coreConclusion = longCoreConclusion,
            ),
            subjectName = "中国古代文学",
        )
        val uiItem = KnowledgeViewModel.toUiItem(pointWithSubject)
        assertNotNull(uiItem.summary)
        assertTrue("summary should be at most 100 chars", uiItem.summary.length <= 100)
        assertEquals(longCoreConclusion.take(100), uiItem.summary)
    }

    @Test
    fun toUiItem_summaryNotNullUsesSummaryDirectly() {
        val pointWithSubject = KnowledgePointWithSubject(
            point = makePoint(
                id = "kp1",
                summary = "人工编写的简短摘要",
                coreConclusion = "这是很长的核心结论，不应该被使用".repeat(10),
            ),
            subjectName = "中国古代文学",
        )
        val uiItem = KnowledgeViewModel.toUiItem(pointWithSubject)
        assertEquals("人工编写的简短摘要", uiItem.summary)
    }

    @Test
    fun filterByCategory_emptyList_returnsEmptyList() {
        val points = emptyList<KnowledgePointWithSubject>()
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
        val pointWithSubject = KnowledgePointWithSubject(
            point = makePoint(id = "kp1"),
            subjectName = null,
        )
        val uiItem = KnowledgeViewModel.toUiItem(pointWithSubject)
        assertEquals("未知科目", uiItem.subject)
    }

    private fun makePointWithSubject(
        id: String,
        subjectName: String?,
    ) = KnowledgePointWithSubject(
        point = makePoint(id = id),
        subjectName = subjectName,
    )

    private fun makePoint(
        id: String = "kp1",
        title: String = "测试知识点",
        summary: String? = "测试摘要",
        coreConclusion: String = "测试核心结论",
        contentSource: String? = "TEXTBOOK_NATIVE",
    ) = KnowledgePointEntity(
        id = id,
        chapterId = "ch1",
        title = title,
        summary = summary,
        coreConclusion = coreConclusion,
        fullContent = "",
        multiPerspectives = null,
        relatedIds = null,
        contrastIds = null,
        extensionIds = null,
        examRecords = null,
        examFrequency = "NEVER",
        termTemplate = null,
        tags = null,
        difficulty = 3,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        contentSource = contentSource,
        ocrStatus = "VERIFIED",
        sourceFile = null,
        sourcePage = null,
        studyText = null,
    )
}
