package com.wenyan.app.core.data.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 30 - CardSplitter 单元测试。
 *
 * 验证 checklist C3.20-C3.23 项：名词解释拆卡 5-6 张（建安风骨 6 张）、
 * 避免集合题（唐宋八大家 8 人按 3 分组 = 3 组枚举卡）、
 * 易混淆内容自动生成区分卡（苏轼/苏辙/苏洵 → 两两区分卡 3 张）、
 * 结构化标签拆分、无标签按分句拆分、超过 6 张合并尾段为"其他要点"。
 *
 * 运行环境：纯 JVM 单元测试（core:data/src/test）。
 */
class CardSplitterTest {

    // ===================== C3.20: 名词解释拆卡 5-6 张 =====================

    /** C3.20: 名词解释拆卡 5-6 张（建安风骨使用结构化标签，拆成 6 张） */
    @Test
    fun splitTermExplanation_returns5to6Cards() {
        val definition = """
            时代：建安年间（196-220年）。
            代表作家：曹操、曹丕、曹植与建安七子。
            风格：慷慨悲凉，刚健有力。
            意义：标志文人诗歌创作自觉，奠定五言诗基础。
            区别：与正始诗歌的玄虚晦涩不同，建安风骨更贴近现实。
            影响：对后世边塞诗与陈子昂、李白等产生深远影响。
        """.trimIndent()
        val cards = CardSplitter.splitTermExplanation("建安风骨", definition)
        assertTrue("拆卡数量应在 5-6 张之间，实际 ${cards.size}", cards.size in 5..6)
    }

    // ===================== C3.21: 避免集合题 =====================

    /** C3.21: 避免集合题（唐宋八大家 8 人按 COLLECTION_GROUP_SIZE=3 分组 = 3 组枚举卡） */
    @Test
    fun splitCollection_avoidsCollectionQuestion_8members3groups() {
        val members = listOf("韩愈", "柳宗元", "欧阳修", "苏洵", "苏轼", "苏辙", "王安石", "曾巩")
        val cards = CardSplitter.splitCollection("唐宋八大家", members)
        assertEquals(3, cards.size)
        cards.forEach { assertTrue("应为 EssayPointsCard", it is EssayPointsCard) }
        // 每组最多 3 人
        cards.forEach { card ->
            val essayCard = card as EssayPointsCard
            assertTrue("每组最多 3 人，实际 ${essayCard.keyPoints.size}", essayCard.keyPoints.size <= 3)
        }
    }

    // ===================== C3.22: 易混淆区分卡 =====================

    /** C3.22: 易混淆内容自动生成区分卡（苏轼/苏辙/苏洵同姓 → 两两区分卡 3 张） */
    @Test
    fun generateDistinctionCards_suFamily_generates3Cards() {
        val items = listOf("苏轼", "苏辙", "苏洵")
        val cards = CardSplitter.generateDistinctionCards(items)
        // 同姓"苏"3 人两两组合 C(3,2) = 3 张区分卡
        assertEquals(3, cards.size)
        cards.forEach { assertTrue("应为 DistinctionCard", it is DistinctionCard) }
    }

    // ===================== C3.23: 建安风骨拆成 6 张 =====================

    /** C3.23: 验证建安风骨使用结构化标签 definition 拆成 6 张（时代/代表作家/风格特征/文学史意义/区别/对后世影响） */
    @Test
    fun splitTermExplanation_jianAnFengGu_6cardsWithDimensions() {
        val definition = """
            时代：建安年间（196-220年）。
            代表作家：曹操、曹丕、曹植与建安七子。
            风格：慷慨悲凉，刚健有力。
            意义：标志文人诗歌自觉，奠定五言诗基础。
            区别：与正始诗歌玄虚晦涩不同。
            影响：对后世边塞诗影响深远。
        """.trimIndent()
        val cards = CardSplitter.splitTermExplanation("建安风骨", definition)
        assertEquals(6, cards.size)
        val dimensions = cards.map { it.front.substringAfter(" — ") }
        assertTrue("应含维度: 时代", "时代" in dimensions)
        assertTrue("应含维度: 代表作家", "代表作家" in dimensions)
        assertTrue("应含维度: 风格特征", "风格特征" in dimensions)
        assertTrue("应含维度: 文学史意义", "文学史意义" in dimensions)
        assertTrue("应含维度: 区别", "区别" in dimensions)
        assertTrue("应含维度: 对后世影响", "对后世影响" in dimensions)
    }

    // ===================== 无标签时按分句拆分 =====================

    /** 无结构化标签时按句末标点切分为分句，每句一张卡（不足 5 张不强行拆分） */
    @Test
    fun splitTermExplanation_noLabels_splitsBySentence() {
        val definition = "建安风骨形成于汉末建安年间。以曹氏父子和建安七子为主要创作者。其诗风慷慨悲凉刚健有力。"
        val cards = CardSplitter.splitTermExplanation("建安风骨", definition)
        // 3 个分句 → 3 张卡（不足 5 张不强行拆分）
        assertEquals(3, cards.size)
        cards.forEach { assertTrue("无标签时维度应为第N点", it.front.contains("第")) }
    }

    // ===================== 超过 6 张合并尾段 =====================

    /** 超过 6 张时合并尾段（7 个分句 → 6 张，最后一张为"其他要点"） */
    @Test
    fun splitTermExplanation_moreThan6_mergesTail() {
        // 无标签的 7 个分句（避免命中结构化标签关键词）
        val definition = listOf(
            "概述其基本内涵。",
            "介绍其形成背景。",
            "列举主要创作者。",
            "分析其艺术表现。",
            "评价其历史地位。",
            "探讨其学术价值。",
            "与其他流派对比。"
        ).joinToString(separator = "")
        val cards = CardSplitter.splitTermExplanation("某名词", definition)
        assertEquals(6, cards.size)
        assertTrue("最后一张应为合并的'其他要点'", cards.last().front.contains("其他要点"))
    }

    // ===================== v0.8.10 修复：结构化标签 >6 维度不再被截断 =====================

    /**
     * v0.8.10 P1-D1 修复验证：结构化标签超过 6 个维度时，不应被 [parseStructuredDimensions] 截断，
     * 而应提取全部维度后由 [splitTermExplanation] 的 `trimmed` 逻辑合并尾段为"其他要点"。
     *
     * 原实现 `parseStructuredDimensions` 内有 `if (result.size >= TARGET_SPLIT_MAX) break`，
     * 导致超过 6 个维度（如 10 个标签）时第 7 个及之后的维度被直接丢弃，信息丢失；
     * 同时 `trimmed` 合并分支因 `cards.size` 永远 ≤6 而成为死代码。
     *
     * 现修复后：10 个结构化标签 → 全部提取 → trimmed 合并为 6 张（前 5 + "其他要点"）。
     */
    @Test
    fun splitTermExplanation_structuredLabelsMoreThan6_notTruncated() {
        // 10 个不同的结构化标签（均命中 TERM_LABELS，去重后维度唯一）
        val definition = """
            时代：汉末建安年间。
            地点：中原地区。
            代表作家：曹操、曹丕、曹植。
            刊物：《建安诗集》。
            主张：回归风雅传统。
            风格：慷慨悲凉。
            特色：刚健有力。
            意义：奠定五言诗基础。
            影响：深远影响后世边塞诗。
            区别：与正始诗歌不同。
        """.trimIndent()
        val cards = CardSplitter.splitTermExplanation("建安风骨", definition)
        // 应合并为 6 张（前 5 张 + "其他要点"），而非截断为 6 张丢失后 4 个维度
        assertEquals("应合并为 6 张", 6, cards.size)
        // 最后一张应为合并的"其他要点"，且包含被合并的多个维度内容
        val lastCard = cards.last()
        assertTrue(
            "最后一张应为'其他要点'，实际 front=${lastCard.front}",
            lastCard.front.contains("其他要点"),
        )
        // 验证被合并的尾段确实包含后几个维度的内容（未被丢弃）
        assertTrue(
            "合并的尾段应包含'影响'维度内容，实际 back=${lastCard.back}",
            lastCard.back.contains("深远影响"),
        )
        assertTrue(
            "合并的尾段应包含'区别'维度内容，实际 back=${lastCard.back}",
            lastCard.back.contains("正始诗歌"),
        )
    }
}
