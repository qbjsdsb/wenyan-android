package com.wenyan.app.core.data.seed

import com.wenyan.app.core.database.entity.ExamCodeHistoryEntity

/**
 * 科目代码历史种子数据（Task 26.1）。
 *
 * 基于南京师范大学现当代文学考研科目代码变动历史：
 * - 610 语义翻转：2025年及以前=文学基础（专一），2026年及以后=专业写作（专一）
 * - 801 语义翻转：2025年及以前=专业写作（专二），2026年及以后=文学基础（专二）
 * - 805/806/807：早期科目代码（2000年前），数据需进一步核实
 * - F008：复试科目代码
 *
 * 【SubTask 26.4 修正说明】
 * 原"610文学基础真题"表述已修正为"文学基础真题（当年试卷代码610）"。
 * - 2025年及以前的610真题归入"文学基础"分类
 * - 2026年及以后的610真题归入"专业写作"分类
 * - 不混淆两个年份的610真题：通过 exam_code_history 表联合 year + exam_paper_code 判定
 *
 * 示例：
 * - 2022年610真题 → "610 文学基础（2022年代码）"
 * - 2026年610真题 → "610 专业写作（2026年代码）"
 * - 2025年（610=文学基础）与2026年（610=专业写作）对比时提示"610代码含义已变更"
 */
object ExamCodeHistoryData {

    /** 当前种子数据生成时间戳 */
    private val SEED_CREATED_AT: Long = System.currentTimeMillis()

    /**
     * 科目代码历史记录列表。
     *
     * 覆盖 610 / 801 / 805 / 806 / 807 / F008 各年份含义。
     */
    val EXAM_CODE_HISTORY: List<ExamCodeHistoryEntity> = listOf(
        // ---------- 610 语义翻转 ----------
        // 2025年及以前：610 = 文学基础（专一）
        ExamCodeHistoryEntity(
            id = "exam-code-hist-610-wenxuejichu-2022-2025",
            examCode = "610",
            subjectName = "文学基础",
            validFromYear = 2022,
            validToYear = 2025,
            direction = "专一",
            createdAt = SEED_CREATED_AT,
        ),
        // 2026年及以后：610 = 专业写作（专一）—— 语义翻转
        ExamCodeHistoryEntity(
            id = "exam-code-hist-610-zhuanyexiezuo-2026",
            examCode = "610",
            subjectName = "专业写作",
            validFromYear = 2026,
            validToYear = null, // 至今有效
            direction = "专一",
            createdAt = SEED_CREATED_AT,
        ),
        // ---------- 801 语义翻转 ----------
        // 2025年及以前：801 = 专业写作（专二）
        ExamCodeHistoryEntity(
            id = "exam-code-hist-801-zhuanyexiezuo-2022-2025",
            examCode = "801",
            subjectName = "专业写作",
            validFromYear = 2022,
            validToYear = 2025,
            direction = "专二",
            createdAt = SEED_CREATED_AT,
        ),
        // 2026年及以后：801 = 文学基础（专二）—— 语义翻转
        ExamCodeHistoryEntity(
            id = "exam-code-hist-801-wenxuejichu-2026",
            examCode = "801",
            subjectName = "文学基础",
            validFromYear = 2026,
            validToYear = null, // 至今有效
            direction = "专二",
            createdAt = SEED_CREATED_AT,
        ),
        // ---------- 早期科目代码（2000年前，需核实）----------
        // 805 文学理论与外国文学（专一，早期，需核实）
        ExamCodeHistoryEntity(
            id = "exam-code-hist-805-wenxuelilun-early",
            examCode = "805",
            subjectName = "文学理论与外国文学",
            validFromYear = 1990,
            validToYear = 1999,
            direction = "专一",
            createdAt = SEED_CREATED_AT,
        ),
        // 806 中国文学史（专一，早期，需核实）
        ExamCodeHistoryEntity(
            id = "exam-code-hist-806-zhongguowenxueshi-early",
            examCode = "806",
            subjectName = "中国文学史",
            validFromYear = 1990,
            validToYear = 1999,
            direction = "专一",
            createdAt = SEED_CREATED_AT,
        ),
        // 807 文学评论（专一，早期，需核实）
        ExamCodeHistoryEntity(
            id = "exam-code-hist-807-wenxuepinglun-early",
            examCode = "807",
            subjectName = "文学评论",
            validFromYear = 1990,
            validToYear = 1999,
            direction = "专一",
            createdAt = SEED_CREATED_AT,
        ),
        // F008 复试（早期，需核实）
        ExamCodeHistoryEntity(
            id = "exam-code-hist-f008-fushi-early",
            examCode = "F008",
            subjectName = "复试",
            validFromYear = 1990,
            validToYear = 1999,
            direction = "复试",
            createdAt = SEED_CREATED_AT,
        ),
    )
}
