package com.wenyan.app.core.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SeedDataLoader] 章节树生成逻辑测试（ADR-001 B1.3 验收）。
 *
 * **测试策略**：直接验证 [SeedDataLoader.PERIOD_CHAPTERS] 结构与
 * [SeedDataLoader.matchPeriodChapter] 匹配逻辑，不通过 `context.assets.open()`
 * 加载完整 seed_data.json。
 *
 * 不用 assets 加载的理由：
 * - core/data 是 library 模块，测试时无法访问 app 模块的 `src/main/assets/seed_data.json`
 * - Robolectric 的 AssetManager 在 library 模块测试中默认查 `src/main/assets/`，
 *   而真实 seed_data.json 在 app 模块，强行复制到 core/data 会污染生产代码
 * - 章节树生成的核心逻辑全在 [PERIOD_CHAPTERS] + [matchPeriodChapter]，
 *   直接测试这两个 API 即可覆盖 B1.3 的逻辑验收
 *
 * **完整流水线验证**（SeedData → DB 章节树）由以下测试覆盖：
 * - [com.wenyan.app.core.data.repository.ChapterRepositoryImplTest]：验证
 *   `observeTree` 递归 CTE、`observeChildren`、`countNonRootChapters` 在层级数据上正确工作
 * - G1 emulator 实测：验证 App 启动后真实 seed_data.json 加载、章节树生成、
 *   知识点按 tags/title 分配到子章节
 *
 * 验收对齐 ADR-001 B1.3：
 * - 至少一科有 parent_id IS NOT NULL 子章节：由 PERIOD_CHAPTERS 4 科全有子章节保证
 * - 章节树生成：由 matchPeriodChapter 返回正确子章节 ID 验证
 * - 知识点 chapterId 不为 null：未匹配时 importToDatabase 兜底指向根章节
 *
 * **实现说明**：[matchPeriodChapter] 与 [PeriodChapter] 已移入 companion object
 * （纯函数不依赖实例状态），测试可直接 `SeedDataLoader.matchPeriodChapter(...)`
 * 调用，无需构造完整实例（Context/DB/DAO 等依赖）。
 */
class SeedDataLoaderTest {

    /**
     * 场景 1（B1.3 核心验收）：PERIOD_CHAPTERS 覆盖全部 4 个科目。
     *
     * 若缺少某科目，该科目的知识点将全部留根章节（无层级树）。
     */
    @Test
    fun `PERIOD_CHAPTERS 覆盖全部 4 个科目`() {
        val expectedSubjects = listOf(
            "中国古代文学",
            "中国现当代文学",
            "外国文学",
            "文学理论",
        )
        for (subject in expectedSubjects) {
            assertNotNull(
                "PERIOD_CHAPTERS 应包含科目: $subject",
                SeedDataLoader.PERIOD_CHAPTERS[subject],
            )
            assertTrue(
                "$subject 应至少有 1 个时段子章节",
                SeedDataLoader.PERIOD_CHAPTERS[subject]!!.isNotEmpty(),
            )
        }
    }

    /**
     * 场景 2：中国古代文学有 8 个时段子章节（先秦/秦汉/魏晋南北朝/隋唐五代/宋辽金/元代/明代/清代）。
     *
     * 验证 PERIOD_CHAPTERS["中国古代文学"] 的时段数与命名正确。
     */
    @Test
    fun `中国古代文学有 8 个时段子章节`() {
        val periods = SeedDataLoader.PERIOD_CHAPTERS["中国古代文学"]!!
        assertEquals("中国古代文学应有 8 个时段", 8, periods.size)

        val expectedTitles = listOf(
            "先秦文学", "秦汉文学", "魏晋南北朝文学", "隋唐五代文学",
            "宋辽金文学", "元代文学", "明代文学", "清代文学",
        )
        val actualTitles = periods.map { it.title }
        for (expected in expectedTitles) {
            assertTrue(
                "应包含时段: $expected，实际=$actualTitles",
                actualTitles.contains(expected),
            )
        }
    }

    /**
     * 场景 3：中国现当代文学有 7 个时段子章节。
     */
    @Test
    fun `中国现当代文学有 7 个时段子章节`() {
        val periods = SeedDataLoader.PERIOD_CHAPTERS["中国现当代文学"]!!
        assertEquals("中国现当代文学应有 7 个时段", 7, periods.size)
    }

    /**
     * 场景 4：外国文学有 8 个时段子章节。
     */
    @Test
    fun `外国文学有 8 个时段子章节`() {
        val periods = SeedDataLoader.PERIOD_CHAPTERS["外国文学"]!!
        assertEquals("外国文学应有 8 个时段", 8, periods.size)
    }

    /**
     * 场景 5：文学理论有 6 个时段子章节。
     */
    @Test
    fun `文学理论有 6 个时段子章节`() {
        val periods = SeedDataLoader.PERIOD_CHAPTERS["文学理论"]!!
        assertEquals("文学理论应有 6 个时段", 6, periods.size)
    }

    /**
     * 场景 6（B1.3 核心验收）：matchPeriodChapter 按 tags 关键词返回正确子章节 ID。
     *
     * 验证知识点"诗经"（tags 含"先秦"）匹配到"先秦文学"子章节（idx=0）。
     * 期望 chapter ID = "chapter_ancient_0"。
     */
    @Test
    fun `matchPeriodChapter 按 tags 匹配先秦文学`() {
        val chapterId = SeedDataLoader.matchPeriodChapter(
            subjectName = "中国古代文学",
            subjectCode = "ancient",
            title = "诗经",
            tags = listOf("先秦", "诗三百"),
        )
        assertEquals(
            "诗经（tags 含先秦）应匹配到 chapter_ancient_0（先秦文学）",
            "chapter_ancient_0",
            chapterId,
        )
    }

    /**
     * 场景 7：matchPeriodChapter 按 title 关键词返回正确子章节 ID。
     *
     * 验证知识点"李白"（title 含"李白"）匹配到"隋唐五代文学"子章节（idx=3）。
     * 期望 chapter ID = "chapter_ancient_3"。
     */
    @Test
    fun `matchPeriodChapter 按 title 匹配隋唐五代文学`() {
        val chapterId = SeedDataLoader.matchPeriodChapter(
            subjectName = "中国古代文学",
            subjectCode = "ancient",
            title = "李白",
            tags = listOf("诗人"),
        )
        assertEquals(
            "李白（title 含李白）应匹配到 chapter_ancient_3（隋唐五代文学）",
            "chapter_ancient_3",
            chapterId,
        )
    }

    /**
     * 场景 8：matchPeriodChapter 对未匹配关键词返回 null（留根章节）。
     *
     * 验证知识点"文学常识汇总"（tags=["通识"]，无匹配关键词）返回 null。
     * importToDatabase 中 null 兜底指向根章节，保证 chapterId 不为 null。
     */
    @Test
    fun `matchPeriodChapter 未匹配返回 null`() {
        val chapterId = SeedDataLoader.matchPeriodChapter(
            subjectName = "中国古代文学",
            subjectCode = "ancient",
            title = "文学常识汇总",
            tags = listOf("通识"),
        )
        assertNull(
            "未匹配关键词应返回 null（兜底留根章节）",
            chapterId,
        )
    }

    /**
     * 场景 9：matchPeriodChapter 对 tags=null 容错（仅按 title 匹配）。
     *
     * 验证知识点 title 含关键词时仍能匹配，tags=null 不抛异常。
     */
    @Test
    fun `matchPeriodChapter tags 为 null 时按 title 匹配`() {
        val chapterId = SeedDataLoader.matchPeriodChapter(
            subjectName = "中国现当代文学",
            subjectCode = "modern",
            title = "狂人日记与五四文学革命",
            tags = null,
        )
        assertEquals(
            "title 含'五四'应匹配到 chapter_modern_0（五四文学革命）",
            "chapter_modern_0",
            chapterId,
        )
    }

    /**
     * 场景 10：matchPeriodChapter 对未知科目返回 null。
     *
     * 验证科目名不在 PERIOD_CHAPTERS 时返回 null（不抛异常）。
     */
    @Test
    fun `matchPeriodChapter 未知科目返回 null`() {
        val chapterId = SeedDataLoader.matchPeriodChapter(
            subjectName = "未知科目",
            subjectCode = "unknown",
            title = "任意标题",
            tags = listOf("任意tag"),
        )
        assertNull("未知科目应返回 null", chapterId)
    }

    /**
     * 场景 11：matchPeriodChapter 多关键词匹配取第一个时段。
     *
     * 验证当 tags 同时匹配多个时段时，取 PERIOD_CHAPTERS 中第一个匹配的时段
     * （如 tags=["先秦", "唐诗"]，应匹配 idx=0 先秦文学，而非 idx=3 隋唐五代）。
     */
    @Test
    fun `matchPeriodChapter 多匹配取第一个时段`() {
        val chapterId = SeedDataLoader.matchPeriodChapter(
            subjectName = "中国古代文学",
            subjectCode = "ancient",
            title = "跨时代知识点",
            tags = listOf("先秦", "唐诗"),
        )
        assertEquals(
            "多匹配应取第一个时段（先秦文学 idx=0）",
            "chapter_ancient_0",
            chapterId,
        )
    }

    /**
     * 场景 12（B1.3 完整性）：4 科目 × 各自时段数 = 总非根章节数。
     *
     * 计算 PERIOD_CHAPTERS 中所有科目的时段总数，验证：
     * - 古代 8 + 现当代 7 + 外国 8 + 理论 6 = 29 个非根章节
     * - 这与 SeedDataLoader.importToDatabase 生成到 DB 的 chapters 表非根章节数一致
     */
    @Test
    fun `PERIOD_CHAPTERS 总时段数等于 29`() {
        val totalPeriods = SeedDataLoader.PERIOD_CHAPTERS.values.sumOf { it.size }
        assertEquals(
            "4 科目总时段数应为 29（8+7+8+6）",
            29,
            totalPeriods,
        )
    }

    /**
     * 场景 13：每个时段的 keywords 列表非空（保证可匹配）。
     *
     * 若某时段 keywords 为空，该时段永远不会被匹配到，成为"死章节"。
     */
    @Test
    fun `每个时段的 keywords 列表非空`() {
        for ((subject, periods) in SeedDataLoader.PERIOD_CHAPTERS) {
            for ((idx, period) in periods.withIndex()) {
                assertTrue(
                    "$subject 时段[$idx] ${period.title} 的 keywords 不应为空",
                    period.keywords.isNotEmpty(),
                )
            }
        }
    }

    // ── v0.9.1 computeRelatedIdsByTags 测试 ──────────────────────

    /** 构造测试用 KnowledgePointSeed（仅填必要字段） */
    private fun kpSeed(
        id: String,
        subject: String = "中国古代文学",
        tags: List<String>? = listOf("default"),
        title: String = "测试知识点 $id",
    ) = KnowledgePointSeed(
        id = id,
        title = title,
        coreConclusion = "结论",
        subject = subject,
        tags = tags,
    )

    @Test
    fun `computeRelatedIdsByTags 同 subject 共享 1 个 tag 产生关联`() {
        val seeds = listOf(
            kpSeed("kp_1", tags = listOf("诗经", "先秦")),
            kpSeed("kp_2", tags = listOf("诗经", "风雅颂")),
            kpSeed("kp_3", tags = listOf("楚辞", "先秦")),
        )
        val result = SeedDataLoader.computeRelatedIdsByTags(seeds)

        // kp_1 与 kp_2 共享"诗经"，与 kp_3 共享"先秦"
        assertTrue("kp_1 应有关联", result.containsKey("kp_1"))
        assertTrue("kp_1 应关联到 kp_2", result["kp_1"]!!.contains("kp_2"))
        assertTrue("kp_1 应关联到 kp_3", result["kp_1"]!!.contains("kp_3"))
    }

    @Test
    fun `computeRelatedIdsByTags 不同 subject 即使共享 tag 也无关联`() {
        val seeds = listOf(
            kpSeed("kp_1", subject = "中国古代文学", tags = listOf("现实主义")),
            kpSeed("kp_2", subject = "外国文学", tags = listOf("现实主义")),
        )
        val result = SeedDataLoader.computeRelatedIdsByTags(seeds)

        // 跨科目不派生关联
        assertTrue("kp_1 不应有关联", !result.containsKey("kp_1"))
        assertTrue("kp_2 不应有关联", !result.containsKey("kp_2"))
    }

    @Test
    fun `computeRelatedIdsByTags 同 subject 无共享 tag 无关联`() {
        val seeds = listOf(
            kpSeed("kp_1", tags = listOf("诗经")),
            kpSeed("kp_2", tags = listOf("楚辞")),
        )
        val result = SeedDataLoader.computeRelatedIdsByTags(seeds)

        assertTrue("kp_1 不应有关联", !result.containsKey("kp_1"))
        assertTrue("kp_2 不应有关联", !result.containsKey("kp_2"))
    }

    @Test
    fun `computeRelatedIdsByTags tags 为 null 无关联`() {
        val seeds = listOf(
            kpSeed("kp_1", tags = null),
            kpSeed("kp_2", tags = listOf("诗经")),
        )
        val result = SeedDataLoader.computeRelatedIdsByTags(seeds)

        assertTrue("kp_1 (tags=null) 不应有关联", !result.containsKey("kp_1"))
    }

    @Test
    fun `computeRelatedIdsByTags 共享 tag 数多的排前面`() {
        val seeds = listOf(
            kpSeed("kp_main", tags = listOf("A", "B", "C")),
            kpSeed("kp_share3", tags = listOf("A", "B", "C")), // 共享 3 个
            kpSeed("kp_share1", tags = listOf("A")),           // 共享 1 个
            kpSeed("kp_share2", tags = listOf("A", "B")),      // 共享 2 个
        )
        val result = SeedDataLoader.computeRelatedIdsByTags(seeds)
        val related = result["kp_main"]!!

        assertEquals("kp_main 应有 3 个关联", 3, related.size)
        assertEquals("第一个应为共享 3 个的 kp_share3", "kp_share3", related[0])
        assertEquals("第二个应为共享 2 个的 kp_share2", "kp_share2", related[1])
        assertEquals("第三个应为共享 1 个的 kp_share1", "kp_share1", related[2])
    }

    @Test
    fun `computeRelatedIdsByTags 最多返回 5 个关联`() {
        // 构造 7 个 KP，全部共享 tag "A"，main 应只取前 5 个
        val seeds = (1..7).map { kpSeed("kp_$it", tags = listOf("A")) } +
            kpSeed("kp_main", tags = listOf("A"))
        val result = SeedDataLoader.computeRelatedIdsByTags(seeds)
        val related = result["kp_main"]!!

        assertEquals("最多 5 个关联", 5, related.size)
    }

    @Test
    fun `computeRelatedIdsByTags 自身不在关联列表中`() {
        val seeds = listOf(
            kpSeed("kp_1", tags = listOf("A")),
            kpSeed("kp_2", tags = listOf("A")),
        )
        val result = SeedDataLoader.computeRelatedIdsByTags(seeds)

        assertTrue("kp_1 不应关联到自己", !result["kp_1"]!!.contains("kp_1"))
        assertTrue("kp_2 不应关联到自己", !result["kp_2"]!!.contains("kp_2"))
    }

    @Test
    fun `computeRelatedIdsByTags 空列表返回空 map`() {
        val result = SeedDataLoader.computeRelatedIdsByTags(emptyList())
        assertTrue("空列表应返回空 map", result.isEmpty())
    }

    // ── v0.9.37 P0-1：轻量版本解析（parseSeedVersionFromJson）测试 ──────

    /**
     * 场景 1：含巨大 knowledge_points 数组的 JSON 只返回 metadata.version。
     *
     * 验证 [parseSeedVersionFromJson] 用 SeedVersionShell 壳解析：不构建
     * 960+ 实体对象，仅跳过未知字段（ignoreUnknownKeys），返回版本号。
     * 若解析器试图全量反序列化 knowledge_points（字段不匹配会抛错），
     * 本测试将失败——从而回归保护"轻量解析"行为。
     */
    @Test
    fun `parseSeedVersionFromJson 跳过巨大 body 只返回版本号`() {
        // 模拟真实结构：metadata + 大数组（此处用 200 个简化实体验证跳过能力）
        val bigBody = buildString {
            append("""{"metadata":{"version":"2.18.0","generated_at":"2026-01-01","description":"测试"},"subjects":[],""")
            append(""""knowledge_points":[""")
            repeat(200) { i ->
                if (i > 0) append(",")
                append("""{"id":"kp_$i","title":"测试知识点 $i","core_conclusion":"结论","subject":"中国古代文学","tags":["先秦"]}""")
            }
            append("""],"exam_questions":[],"writing_materials":[]}""")
        }

        val version = parseSeedVersionFromJson(bigBody)
        assertEquals("应轻量解析出 metadata.version", "2.18.0", version)
    }

    /**
     * 场景 2：metadata 缺失时返回空串（调用方兜底 DEFAULT_SEED_VERSION）。
     */
    @Test
    fun `parseSeedVersionFromJson metadata 缺失返回空串`() {
        val version = parseSeedVersionFromJson("""{"subjects":[]}""")
        assertEquals("metadata 缺失应返回空串", "", version)
    }

    /**
     * 场景 3：非 JSON 输入抛出异常（调用方在 ensureSeedDataLoaded 中依赖
     * 异常冒泡到 Application 处理器后下次重试，不在此吞掉）。
     */
    @Test(expected = Exception::class)
    fun `parseSeedVersionFromJson 非法 JSON 抛异常`() {
        parseSeedVersionFromJson("not-json{")
    }
}
