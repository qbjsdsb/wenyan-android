package com.wenyan.app.core.data.util

import com.wenyan.app.core.database.entity.ExamCodeHistoryEntity

/**
 * 科目代码判定逻辑（SubTask 26.2 / 26.3）。
 *
 * 处理南京师范大学现当代文学考研科目代码历史变动：
 * - 610 语义翻转：2025年=文学基础，2026年=专业写作
 * - 801 语义翻转：2025年=专业写作，2026年=文学基础
 *
 * 通过 exam_code_history 表联合 year + exam_paper_code 判定科目。
 * 年份代码缺失时不猜测科目名称，显示"年份待核实"。
 */
object ExamCodeResolver {

    /**
     * 判定某年某试卷代码对应的科目信息（SubTask 26.2）。
     *
     * 逻辑：
     * 1. 查找 exam_code 匹配且 year 在 valid_from_year 到 valid_to_year 之间的记录
     * 2. 找到 → displayName = "代码 科目名称（年份年代码）"，isVerified = true
     * 3. 未找到 → displayName = "试卷代码XXXX（年份待核实）"，isVerified = false
     * 4. 检测语义翻转：如果同一 exam_code 在不同年份有不同 subject_name，
     *    返回 warningMessage = "代码含义已变更"
     *
     * @param examCode 试卷代码（如 "610" / "801"）
     * @param year 真题年份
     * @param history 科目代码历史记录列表
     * @return 科目判定结果
     */
    fun resolveSubject(
        examCode: String,
        year: Int,
        history: List<ExamCodeHistoryEntity>,
    ): SubjectResolution {
        // 1. 查找 exam_code 匹配且 year 在 valid_from_year 到 valid_to_year 之间的记录
        val matched = history.find { record ->
            record.examCode == examCode &&
                year >= record.validFromYear &&
                (record.validToYear?.let { year <= it } ?: true)
        }

        // 4. 检测语义翻转：同一 exam_code 在不同年份是否有不同 subject_name
        val hasSemanticChange = history
            .filter { it.examCode == examCode }
            .map { it.subjectName }
            .distinct()
            .size > 1

        return if (matched != null) {
            // 2. 找到 → displayName = "代码 科目名称（年份年代码）"
            SubjectResolution(
                displayName = "${examCode} ${matched.subjectName}（${year}年代码）",
                subjectName = matched.subjectName,
                direction = matched.direction,
                isVerified = true,
                warningMessage = if (hasSemanticChange) "${examCode}代码含义已变更" else null,
            )
        } else {
            // 3. 未找到 → displayName = "试卷代码XXXX（年份待核实）"
            SubjectResolution(
                displayName = "试卷代码${examCode}（年份待核实）",
                subjectName = "",
                direction = null,
                isVerified = false,
                warningMessage = "该年份科目代码待核实，请以官方招生目录为准",
            )
        }
    }

    /**
     * 跨年份真题对比，检测代码含义是否变更（SubTask 26.3）。
     *
     * 如果 year1 和 year2 的 exam_code 对应不同 subject_name，
     * hasSemanticChange = true，并返回警告信息。
     *
     * 示例：2025年（610=文学基础）与2026年（610=专业写作）对比时，
     * 返回 hasSemanticChange=true，warningMessage="610代码含义已变更"。
     *
     * @param examCode 试卷代码
     * @param year1 年份1
     * @param year2 年份2
     * @param history 科目代码历史记录列表
     * @return 对比结果
     */
    fun compareAcrossYears(
        examCode: String,
        year1: Int,
        year2: Int,
        history: List<ExamCodeHistoryEntity>,
    ): ComparisonResult {
        val subject1 = resolveSubjectName(examCode, year1, history)
        val subject2 = resolveSubjectName(examCode, year2, history)

        val hasChange = subject1 != null &&
            subject2 != null &&
            subject1 != subject2

        return ComparisonResult(
            hasSemanticChange = hasChange,
            year1Subject = subject1 ?: "未知",
            year2Subject = subject2 ?: "未知",
            warningMessage = if (hasChange) "${examCode}代码含义已变更" else null,
        )
    }

    /**
     * 内部方法：获取某年某代码的科目名称。
     *
     * @return 科目名称；若该年份代码缺失则返回 null
     */
    private fun resolveSubjectName(
        examCode: String,
        year: Int,
        history: List<ExamCodeHistoryEntity>,
    ): String? {
        return history.find { record ->
            record.examCode == examCode &&
                year >= record.validFromYear &&
                // P0-T2 修正：原 `record.validToYear!!` 在 null 检查后冗余，
                // 但用 let + Elvis 更安全（避免 !! 在重构时遗留 NPE 风险）。
                (record.validToYear?.let { year <= it } ?: true)
        }?.subjectName
    }
}

/**
 * 科目判定结果（SubTask 26.2）。
 *
 * @property displayName 显示名称，如"610 文学基础（2022年代码）"
 * @property subjectName 科目名称，如"文学基础"
 * @property direction 方向：专一/专二/复试
 * @property isVerified 是否已核实：true=已核实，false=年份待核实
 * @property warningMessage 警告信息，如"610代码含义已变更"；null 表示无警告
 */
data class SubjectResolution(
    val displayName: String,
    val subjectName: String,
    val direction: String?,
    val isVerified: Boolean,
    val warningMessage: String?,
)

/**
 * 跨年份对比结果（SubTask 26.3）。
 *
 * @property hasSemanticChange 代码含义是否变更
 * @property year1Subject 年份1的科目名称
 * @property year2Subject 年份2的科目名称
 * @property warningMessage 警告信息，如"610代码含义已变更"
 */
data class ComparisonResult(
    val hasSemanticChange: Boolean,
    val year1Subject: String,
    val year2Subject: String,
    val warningMessage: String?,
)
