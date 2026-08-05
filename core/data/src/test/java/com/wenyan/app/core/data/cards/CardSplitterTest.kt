package com.wenyan.app.core.data.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        // v0.9.35 体验改进：正面含"第X点 · 共N点"总上下文（N=实际分句数）
        cards.forEachIndexed { index, card ->
            assertTrue("无标签时维度应为第N点", card.front.contains("第"))
            assertTrue(
                "卡 ${index + 1} 应含'共3点'（front=${card.front}）",
                card.front.contains("共三点"),
            )
        }
        assertEquals("第一点 · 共三点", cards.first().front.substringAfter(" — "))
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
        // v0.9.35 裁剪感知：前 5 张编号卡显示"共5点"（而非原始 7 句数），与裁剪后卡数一致
        cards.take(5).forEachIndexed { index, card ->
            assertTrue(
                "编号卡 ${index + 1} 应含'共5点'（front=${card.front}）",
                card.front.contains("共五点"),
            )
        }
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

    // ===================== v0.8.15 P1-5 回归：合并卡 category 一致性 =====================

    /**
     * v0.8.15 P1-5 回归验证：合并卡（"其他要点"）的 [TermExplanationCard.category]
     * 必须与首卡一致，避免类别切换误读。
     *
     * 修复前：[CardSplitter.splitTermExplanation] 合并尾段时调用 [buildTermDimensionCard]
     * 未传 category 参数，默认 SOCIETY。若原名词是作品类（WORK），首卡显示
     * "（名词解释 · 作品类）"，合并卡显示"（名词解释 · 社团类）"，类别切换让用户
     * 误以为拆卡错误。
     *
     * 本测试覆盖两个方向：
     * - WORK 类（含"内容"标签）+ 8 维度 → 合并卡 category == WORK
     * - SOCIETY 类（含"刊物"标签）+ 8 维度 → 合并卡 category == SOCIETY
     */
    @Test
    fun splitTermExplanation_mergedTailKeepsCategory_workClass() {
        // 作品类（"内容"标签触发 WORK）+ 8 个维度（>6 触发合并）
        val definition = """
            时代：20世纪30年代。
            代表作家：沈从文。
            内容：湘西世界的田园牧歌与文明冲突。
            风格：抒情诗化的散文化叙事。
            特色：融合乡土写实与浪漫抒情。
            意义：京派文学代表作品。
            影响：深远影响乡土文学传统。
            区别：与左翼文学的政治批判不同。
        """.trimIndent()
        val cards = CardSplitter.splitTermExplanation("边城", definition)
        assertEquals("8 维度应合并为 6 张", 6, cards.size)
        assertTrue(
            "最后一张应为'其他要点'",
            cards.last().front.contains("其他要点"),
        )
        // 所有卡都应是 TermExplanationCard
        cards.forEach { assertTrue("应为 TermExplanationCard", it is TermExplanationCard) }
        val termCards = cards.map { it as TermExplanationCard }
        // P1-5 关键断言：所有卡 category 一致（包括合并卡）
        val firstCategory = termCards.first().category
        assertEquals("首卡应为 WORK（含'内容'标签）", TermCategory.WORK, firstCategory)
        termCards.forEachIndexed { index, card ->
            assertEquals(
                "卡 #${index + 1}（front=${card.front}）category 应与首卡一致",
                firstCategory,
                card.category,
            )
        }
        // 特别验证合并卡（最后一张）保持 WORK
        assertEquals(
            "合并卡 category 应为 WORK（与首卡一致，P1-5 修复）",
            TermCategory.WORK,
            termCards.last().category,
        )
    }

    @Test
    fun splitTermExplanation_mergedTailKeepsCategory_societyClass() {
        // 社团类（"刊物"+"主张"标签触发 SOCIETY）+ 8 个维度（>6 触发合并）
        val definition = """
            时代：1921年。
            地点：北京。
            代表作家：郑振铎、沈雁冰、叶绍钧。
            刊物：《小说月报》。
            主张：为人生而艺术。
            风格：现实主义创作倾向。
            特色：注重社会问题反映。
            意义：中国第一个新文学社团。
        """.trimIndent()
        val cards = CardSplitter.splitTermExplanation("文学研究会", definition)
        assertEquals("8 维度应合并为 6 张", 6, cards.size)
        val termCards = cards.map { it as TermExplanationCard }
        val firstCategory = termCards.first().category
        assertEquals("首卡应为 SOCIETY（含'刊物'+'主张'标签）", TermCategory.SOCIETY, firstCategory)
        // P1-5 关键断言：所有卡 category 一致（包括合并卡）
        termCards.forEachIndexed { index, card ->
            assertEquals(
                "卡 #${index + 1}（front=${card.front}）category 应与首卡一致",
                firstCategory,
                card.category,
            )
        }
        assertEquals(
            "合并卡 category 应为 SOCIETY（与首卡一致）",
            TermCategory.SOCIETY,
            termCards.last().category,
        )
    }

    /**
     * 验证：合并卡不附带 society/work 结构化字段（与首张 sibling 卡行为一致）。
     *
     * v0.8.9 P2-8 修复后：非首张 sibling 卡不附带 society/work（避免冗余展示）。
     * 合并卡作为最后一张 sibling 卡，也应不附带 society/work，只展示 back 内容。
     */
    @Test
    fun splitTermExplanation_mergedTailDoesNotAttachStructuredFields() {
        val definition = """
            时代：1921年。
            地点：北京。
            代表作家：郑振铎。
            刊物：《小说月报》。
            主张：为人生而艺术。
            风格：现实主义。
            特色：关注社会。
            意义：新文学社团代表。
            影响：推动文学发展。
            区别：与创造社不同。
        """.trimIndent()
        val cards = CardSplitter.splitTermExplanation("文学研究会", definition)
        assertEquals(6, cards.size)
        val termCards = cards.map { it as TermExplanationCard }
        // 首卡应附带 society 结构化字段（概览卡）
        assertNotNull("首卡应附带 society 字段（概览卡）", termCards.first().society)
        // 合并卡（最后一张）不应附带 society/work（与后续 sibling 卡行为一致）
        assertNull(
            "合并卡不应附带 society 字段（避免冗余）",
            termCards.last().society,
        )
        assertNull(
            "合并卡不应附带 work 字段（避免冗余）",
            termCards.last().work,
        )
    }

    // ===================== v0.8.16 P2-B 修复："作者" 标签 + 作品类识别 =====================

    /**
     * v0.8.16 P2-B 修复验证：作品类名词仅靠"作者"标签（无"内容"标签）应正确识别为 WORK。
     *
     * 修复前：[determineCategory] 仅靠"内容"标签识别作品类。
     * 若 seed 数据有"作者：沈从文"但无"内容"标签（如《边城》名词解释只列作者/风格/影响），
     * 会被误判为 SOCIETY，导致：
     * - 渲染时显示"（名词解释 · 社团类）"误导用户
     * - 调用 buildSocietyFields 而非 buildWorkFields，"作者"维度被丢弃
     *   （SocietyTermFields 无 author 字段，members 字段从"代表作家"取，"作者"维度丢失）
     *
     * 修复后：
     * - TERM_LABELS 新增 "作者" to "作者" 映射
     * - determineCategory 中 "作者" in names → WORK
     * - buildWorkFields 中 author 优先取 "作者" 维度
     */
    @Test
    fun splitTermExplanation_authorTagTriggersWorkClass() {
        // 作品类（仅"作者"标签，无"内容"标签）+ 5 个维度（不触发合并，便于断言）
        val definition = """
            作者：沈从文。
            时代：20世纪30年代。
            风格：抒情诗化叙事。
            特色：融合乡土写实与浪漫抒情。
            意义：京派文学代表作品。
        """.trimIndent()
        val cards = CardSplitter.splitTermExplanation("边城", definition)
        assertEquals("5 维度不合并", 5, cards.size)
        val termCards = cards.map { it as TermExplanationCard }
        // P2-B 关键断言：仅靠"作者"标签识别为 WORK（原会误判为 SOCIETY）
        assertEquals(
            "仅靠'作者'标签应识别为 WORK（P2-B 修复）",
            TermCategory.WORK,
            termCards.first().category,
        )
        // 首卡应附带 work 结构化字段（不是 society）
        assertNotNull("首卡应附带 work 字段", termCards.first().work)
        assertNull("首卡不应附带 society 字段（已识别为 WORK）", termCards.first().society)
        // work.author 应为"沈从文"（优先取"作者"维度）
        assertEquals(
            "work.author 应为'沈从文'（从'作者'维度提取）",
            "沈从文",
            termCards.first().work?.author,
        )
        // 应有一张卡的维度是"作者"（front 含"— 作者"）
        assertTrue(
            "应有一张卡维度为'作者'，fronts=${termCards.map { it.front }}",
            termCards.any { it.front.contains("— 作者") },
        )
    }

    /**
     * 验证：社团类名词有"代表作家"标签但无"作者"标签时，仍正确识别为 SOCIETY。
     *
     * 回归测试：P2-B 新增"作者"→WORK 映射不应破坏既有社团类识别。
     * "文学研究会" definition 含"代表作家：郑振铎"但无"作者"标签，应仍为 SOCIETY。
     */
    @Test
    fun splitTermExplanation_representativeWriterTagKeepsSocietyClass() {
        val definition = """
            时代：1921年。
            地点：北京。
            代表作家：郑振铎、沈雁冰。
            刊物：《小说月报》。
            主张：为人生而艺术。
        """.trimIndent()
        val cards = CardSplitter.splitTermExplanation("文学研究会", definition)
        assertEquals(5, cards.size)
        val termCards = cards.map { it as TermExplanationCard }
        assertEquals(
            "社团类（含'刊物'+'主张'，无'作者'）应为 SOCIETY",
            TermCategory.SOCIETY,
            termCards.first().category,
        )
        assertNotNull("首卡应附带 society 字段", termCards.first().society)
        // society.members 应从"代表作家"维度提取
        assertEquals(
            "society.members 应为'郑振铎、沈雁冰'（从'代表作家'维度提取）",
            "郑振铎、沈雁冰",
            termCards.first().society?.members,
        )
    }
}
