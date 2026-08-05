package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.entity.KnowledgePointEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 拆卡缓存键测试（v0.9.37 P0-2）。
 *
 * 覆盖 [cardCacheKey] 的键语义：
 * - 同一批知识点（id+updatedAt 相同）→ 键相等 → 缓存命中（不重复拆卡）
 * - 知识点集合变化（新增/移除）→ 键变化 → 重新拆卡
 * - 知识点内容更新（updatedAt 刷新，如种子升级/OCR 校对）→ 键变化 → 重新拆卡
 * - 仅 memo_records 变化（用户评分）不经过本键 → 不影响缓存（评分不会触发重拆）
 */
class CardRepositoryCacheKeyTest {

    private fun kp(
        id: String,
        updatedAt: Long = 1000L,
    ) = KnowledgePointEntity(
        id = id,
        chapterId = "ch1",
        title = "知识点 $id",
        summary = "摘要",
        coreConclusion = "结论",
        fullContent = "内容",
        multiPerspectives = null,
        relatedIds = null,
        contrastIds = null,
        extensionIds = null,
        examRecords = null,
        examFrequency = "NEVER",
        termTemplate = null,
        tags = null,
        difficulty = 3,
        createdAt = 1000L,
        updatedAt = updatedAt,
        contentSource = "TEXTBOOK_NATIVE",
        ocrStatus = "VERIFIED",
        sourceFile = null,
        sourcePage = null,
        studyText = null,
    )

    @Test
    fun `相同队列键相等_缓存命中`() {
        val queue1 = listOf(kp("p1", 1000L), kp("p2", 2000L))
        val queue2 = listOf(kp("p1", 1000L), kp("p2", 2000L))
        assertEquals("相同 id+updatedAt 应产生相同缓存键", cardCacheKey(queue1), cardCacheKey(queue2))
    }

    @Test
    fun `队列顺序不影响键`() {
        val a = listOf(kp("p1", 1000L), kp("p2", 2000L))
        val b = listOf(kp("p2", 2000L), kp("p1", 1000L))
        assertEquals("顺序无关（队列本身有序，此处防御性验证）", cardCacheKey(a), cardCacheKey(b))
    }

    @Test
    fun `知识点集合变化键变化`() {
        val base = listOf(kp("p1", 1000L))
        val withNew = listOf(kp("p1", 1000L), kp("p3", 3000L))
        assertNotEquals("新增知识点应改变缓存键", cardCacheKey(base), cardCacheKey(withNew))
    }

    @Test
    fun `知识点内容更新键变化`() {
        val old = listOf(kp("p1", 1000L))
        val refreshed = listOf(kp("p1", 9999L)) // updatedAt 刷新（种子升级/校对）
        assertNotEquals("updatedAt 变化应改变缓存键（触发重新拆卡）", cardCacheKey(old), cardCacheKey(refreshed))
    }
}
