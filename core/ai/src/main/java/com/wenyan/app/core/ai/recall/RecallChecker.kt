package com.wenyan.app.core.ai.recall

import com.wenyan.app.core.ai.AiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 主动回忆检测引擎（三层渐进式方案）。
 *
 * Spec 第 359-385 行、设计文档 3.3.5 节（第 849-918 行）：
 *
 * 三层检测机制（阈值对齐设计文档，NF-A2 修正后 L2 增加 GOOD 档）：
 * - L1 关键词匹配 + 同义词词典（名词解释/术语，本地 <10ms）
 *     覆盖率 < 30% → Again
 *     覆盖率 30-60% → Hard
 *     覆盖率 60-85% → Good
 *     覆盖率 ≥ 85% → Easy
 * - L2 Jaccard 相似度（论述题/分析题，本地 <10ms）
 *     覆盖率 < 60% → Hard
 *     覆盖率 60-75% → 部分正确（触发 L3）
 *     覆盖率 75-85% → Good（较好但不完美，不触发 L3）
 *     覆盖率 ≥ 85% → Easy
 * - L3 LLM 异步评估（L2 判定"部分正确"时触发，在线 3-5 秒）
 *     输出 0-100 分及理由
 *     不阻塞用户复习流程
 *     分数 < 60 → Again, 60-75 → Hard, 75-90 → Good, ≥ 90 → Easy
 *
 * NF-A2 修正：原 L2 在 60-85% 范围统一返回 HARD（触发 L3）。问题：
 * - 若 L3 失败降级为 L2 结果，75-85% 相似度的答案被错误归为 HARD（过严）
 * - 75-85% 是"较好但不完美"，语义更接近 GOOD 而非 HARD
 * 修正方案：将 L3 触发范围从 60-85% 收窄到 60-75%，75-85% 直接返回 GOOD（不依赖 L3）。
 * 60-75% 仍走 L3（部分正确需要 LLM 精细评估）。
 *
 * 阶段4实现变更：
 * - L2 从 BGE-small-zh 模型改为 Jaccard 相似度（Android 端不适合加载嵌入模型）
 * - L3 接入 AiService.chatResult() 调用 LLM API（P1-5：原 chat() 错误吞噬导致失败时 score 误判为 0）
 */
@Singleton
class RecallChecker @Inject constructor(
    private val aiService: AiService,
) {

    /**
     * 检测用户主动回忆质量。
     *
     * @param userAnswer 用户答案
     * @param correctAnswer 正确答案
     * @param questionType 题目类型（名词解释走 L1，论述题走 L2/L3）
     * @return 检测结果，包含层级、覆盖率、评分和理由
     */
    fun checkRecall(
        userAnswer: String,
        correctAnswer: String,
        questionType: QuestionType,
    ): Flow<RecallResult> = flow {
        when (questionType) {
            QuestionType.TERM_EXPLANATION -> {
                emit(checkL1Keyword(userAnswer, correctAnswer))
            }
            QuestionType.ESSAY -> {
                val l2Result = checkL2Semantic(userAnswer, correctAnswer)
                if (l2Result.rating == RecallRating.HARD && l2Result.coverage in PARTIAL_CORRECT_RANGE) {
                    // L2 判定"部分正确"（覆盖率 60-75%）时触发 L3 LLM 评估
                    // NF-A2 修正：L3 触发范围从 60-85% 收窄到 60-75%，
                    // 75-85% 直接返回 GOOD（不依赖 L3，避免 L3 失败时降级过严）。
                    // P0-A1 修正：原实现 if/else 两分支均 emit(l2Result)，L3 从未触发。
                    // 现 if 分支调用 checkL3Llm()，LLM 失败时降级为 L2 结果，不阻塞复习流程。
                    val l3Result = try {
                        checkL3Llm(userAnswer, correctAnswer)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    }
                    emit(l3Result ?: l2Result)
                } else {
                    emit(l2Result)
                }
            }
        }
    }

    /**
     * L3 LLM 异步评估（单独调用，不阻塞主流程）。
     *
     * 在 L2 判定"部分正确"后，可通过此方法获取 L3 精确评估。
     *
     * P1-5 修正：改用 [AiService.chatResult] 区分成功/失败。失败时抛异常，
     * 由 [checkRecall] 的 try-catch 捕获并降级为 L2 结果（不阻塞复习流程）。
     * 原实现用 [AiService.chat]，错误字符串被当作 LLM 回复解析，score 误判为 0 → AGAIN。
     *
     * @param userAnswer 用户答案
     * @param correctAnswer 正确答案
     * @return L3 检测结果
     * @throws Exception AI 调用失败时抛出（含差异化错误信息）
     */
    suspend fun checkL3Llm(userAnswer: String, correctAnswer: String): RecallResult {
        val prompt = buildL3Prompt(userAnswer, correctAnswer)
        val result = aiService.chatResult(prompt).first()
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: IllegalStateException("L3 LLM 评估失败")
        }
        val response = result.getOrThrow()
        val (score, reason) = parseL3Response(response)

        val rating = when {
            score < L3_THRESHOLD_AGAIN -> RecallRating.AGAIN   // <60
            score < L3_THRESHOLD_HARD -> RecallRating.HARD     // 60-75
            score < L3_THRESHOLD_GOOD -> RecallRating.GOOD     // 75-90
            else -> RecallRating.EASY                           // ≥90
        }

        return RecallResult(
            level = RecallLevel.L3,
            coverage = score / 100f,
            rating = rating,
            score = score,
            reason = reason,
        )
    }

    // ── L1: 关键词匹配 + 同义词词典 ───────────────────────────────

    /**
     * L1 关键词匹配检测（设计文档第 859-864 行）。
     */
    private fun checkL1Keyword(userAnswer: String, correctAnswer: String): RecallResult {
        val keywords = extractKeywords(correctAnswer)
        val expandedKeywords = expandWithSynonyms(keywords)
        val coverage = calculateKeywordCoverage(userAnswer, expandedKeywords)

        val rating = when {
            coverage < L1_THRESHOLD_AGAIN -> RecallRating.AGAIN   // <30%
            coverage < L1_THRESHOLD_HARD -> RecallRating.HARD     // 30-60%
            coverage < L1_THRESHOLD_GOOD -> RecallRating.GOOD     // 60-85%
            else -> RecallRating.EASY                               // ≥85%
        }

        return RecallResult(
            level = RecallLevel.L1,
            coverage = coverage,
            rating = rating,
            score = null,
            reason = null,
        )
    }

    // ── L2: Jaccard 相似度 ───────────────────────────────────────

    /**
     * L2 语义相似度检测（阶段4改为 Jaccard 相似度）。
     *
     * NF-A2 修正后阈值（设计文档第 872 行 + 审计 NF-A2 增补）：
     *   覆盖率 < 60% → Hard（明显不足）
     *   覆盖率 60-75% → Hard（部分正确，触发 L3 LLM 精细评估）
     *   覆盖率 75-85% → Good（较好但不完美，不触发 L3）
     *   覆盖率 ≥ 85% → Easy
     *
     * 原 L2 在 60-85% 范围统一返回 HARD：若 L3 失败降级，75-85% 相似度
     * 被错误归为 HARD（过严）。修正后 75-85% 直接返回 GOOD，避免依赖 L3。
     *
     * v0.8.16 P1-7 修正：Jaccard 长度偏差修复。
     * 原 Jaccard = |A ∩ B| / |A ∪ B| 在长度差异大时严重偏低：
     * - 用户答案 100 字 vs 正确答案 500 字，即使 100 字全部命中
     *   Jaccard = 100 / 500 = 0.2（被误判 HARD）
     * - 这违反"主动回忆"检测的初衷：用户应该被鼓励用自己的话简洁作答
     *
     * 修正方案：取 Jaccard 与包含率（containment）的最大值作为最终相似度
     * - containment = |A ∩ B| / |B|（B = 正确答案的 bigram 集合）
     * - containment 不受用户答案长度影响：用户答案越短，只要覆盖正确答案要点
     *   就能获得高 containment
     * - 取 max(Jaccard, containment) 兼顾两种情况：
     *   - 用户答案长且内容相近 → Jaccard 高
     *   - 用户答案短但覆盖要点 → containment 高
     *
     * 仍保留 Jaccard 作为基础：包含率会因 bigram 重复而虚高
     * （"苏轼苏轼苏轼"包含所有正确答案的苏轼 bigram），Jaccard 抑制这类作弊。
     */
    private fun checkL2Semantic(userAnswer: String, correctAnswer: String): RecallResult {
        val similarity = calculateSemanticSimilarity(userAnswer, correctAnswer)

        val rating = when {
            similarity < L2_THRESHOLD_HARD -> RecallRating.HARD              // <60%
            similarity < L2_THRESHOLD_PARTIAL -> RecallRating.HARD           // 60-75% 部分正确,触发 L3
            similarity < L2_THRESHOLD_GOOD -> RecallRating.GOOD              // 75-85% 较好但不完美
            else -> RecallRating.EASY                                        // ≥85%
        }

        return RecallResult(
            level = RecallLevel.L2,
            coverage = similarity,
            rating = rating,
            score = null,
            reason = null,
        )
    }

    // ── 私有辅助方法 ──────────────────────────────────────────────

    /**
     * 提取正确答案中的关键词。
     *
     * 按标点分词，过滤停用词，保留 ≥2 字的词。
     */
    private fun extractKeywords(correctAnswer: String): List<String> {
        return correctAnswer
            .split("，", "。", "、", "；", "：", "（", "）", "(", ")", " ", "\n")
            .map { it.trim() }
            .filter { it.length >= 2 && it !in STOP_WORDS }
    }

    /**
     * 同义词词典扩展。
     *
     * 硬编码常见文学同义词映射（如"苏轼"="苏东坡"="子瞻"）。
     */
    private fun expandWithSynonyms(keywords: List<String>): List<String> {
        val expanded = mutableListOf<String>()
        for (keyword in keywords) {
            expanded.add(keyword)
            SYNONYM_MAP[keyword]?.let { expanded.addAll(it) }
        }
        return expanded.distinct()
    }

    /** 计算关键词覆盖率 */
    private fun calculateKeywordCoverage(userAnswer: String, keywords: List<String>): Float {
        if (keywords.isEmpty()) return 0f
        val matched = keywords.count { keyword ->
            userAnswer.contains(keyword.trim(), ignoreCase = true)
        }
        return matched.toFloat() / keywords.size
    }

    /**
     * 计算语义相似度（v0.8.16 P1-7：Jaccard + 包含率混合）。
     *
     * 触发条件：用户答案 bigram 数 ≤ 正确答案 bigram 数时取 max(Jaccard, containment)
     * 否则取 Jaccard。
     *
     * 设计动机：
     * - 用户答案短（如简洁作答）→ Jaccard 严重偏低（union 被 correct 撑大）
     *   → 取 max(Jaccard, containment) 让短答案命中要点时获高相似度
     * - 用户答案长（如添加"代表""特征"等冗余词）→ 仍用 Jaccard
     *   抑制"加水"作弊（用冗余 bigram 提高覆盖率）
     *
     * 注意：边界判定基于 bigram 集合大小，不是字符数。
     * 集合大小不受重复字符影响（bigram 是 Set），更准确反映"信息量"。
     *
     * @return 相似度，取值范围 [0, 1]
     */
    private fun calculateSemanticSimilarity(userAnswer: String, correctAnswer: String): Float {
        val userBigrams = extractBigrams(userAnswer)
        val correctBigrams = extractBigrams(correctAnswer)
        if (userBigrams.isEmpty() || correctBigrams.isEmpty()) return 0f

        val intersection = userBigrams.intersect(correctBigrams).size
        if (intersection == 0) return 0f

        val union = userBigrams.union(correctBigrams).size
        val jaccard = intersection.toFloat() / union

        // 仅在用户答案"短"于正确答案时使用 containment 修正长度偏差
        // 用户答案"长"于正确答案时，Jaccard 仍能正确惩罚"加水"行为
        return if (userBigrams.size <= correctBigrams.size) {
            // containment = |user ∩ correct| / |correct|
            // 不除 |user|，让短答案也能获得高相似度（如用户简短作答命中要点）
            val containment = intersection.toFloat() / correctBigrams.size
            maxOf(jaccard, containment)
        } else {
            jaccard
        }
    }

    /**
     * 计算 Jaccard 相似度（分词后交集/并集）。
     *
     * Jaccard = |A ∩ B| / |A ∪ B|
     *
     * 使用字符级 bigram（2-gram）作为分词单元，适合中文文本。
     *
     * v0.8.16 P1-7：保留供测试直接调用，但生产代码改用 [calculateSemanticSimilarity]。
     */
    private fun calculateJaccardSimilarity(text1: String, text2: String): Float {
        val set1 = extractBigrams(text1)
        val set2 = extractBigrams(text2)
        if (set1.isEmpty() || set2.isEmpty()) return 0f

        val intersection = set1.intersect(set2).size
        val union = set1.union(set2).size
        return intersection.toFloat() / union
    }

    /** 提取字符级 bigram（2-gram） */
    private fun extractBigrams(text: String): Set<String> {
        val cleaned = text.filter { !it.isWhitespace() && it !in "，。、；：（）(),." }
        if (cleaned.length < 2) return setOf(cleaned)
        return cleaned.windowed(2).toSet()
    }

    /** 构建 L3 LLM 评估 prompt */
    private fun buildL3Prompt(userAnswer: String, correctAnswer: String): String {
        return """请评估用户答案与正确答案的匹配度。

【用户答案】
$userAnswer

【正确答案】
$correctAnswer

请返回以下 JSON 格式（不要包含其他内容）：
{"score": 0-100的整数, "reason": "简短评估理由"}

评分标准：
- 90-100：完全正确，覆盖所有要点
- 75-89：基本正确，遗漏少量要点
- 60-74：部分正确，遗漏重要要点
- 0-59：大部分错误或偏离"""
    }

    /**
     * 解析 L3 LLM 返回的 JSON。
     *
     * 用正则提取 score 和 reason，不依赖严格 JSON（LLM 可能加额外文本）。
     *
     * v0.8.16 P1-2 修复：原正则只匹配 `"score": 整数`，LLM 常见返回变体无法解析：
     * - `"score": 85.0`（带小数，常见于 deepseek/glm）
     * - `"score":85`（无空格）
     * - `score: 85`（无引号，markdown 表格风格）
     * - `{"score" : 85, ...}`（多余空格）
     * 解析失败 fallback score=0 → 误判为 AGAIN（<60），用户复习间隔被错误重置。
     *
     * 现正则改为支持小数 + 可选引号 + 灵活空格，截断小数部分取整数。
     * 同时新增 fallback：若 score 解析失败，尝试匹配中文"评分：85"或"得分 85"模式，
     * 最后 fallback 仍为 0 但 reason 标注"解析失败"便于排查。
     */
    private fun parseL3Response(response: String): Pair<Int, String> {
        // 主正则：支持 "score": 85 / "score":85.0 / score: 85 等变体
        val scoreRegex = Regex("(?:\"score\"|score)\\s*[:：]\\s*\"?(\\d+(?:\\.\\d+)?)\"?")
        val scoreMatch = scoreRegex.find(response)
        val score = scoreMatch?.groupValues?.get(1)?.toFloatOrNull()?.toInt() ?: 0

        // reason 正则：支持 "reason": "..." / reason: "..." / reason："..."
        val reasonRegex = Regex("(?:\"reason\"|reason)\\s*[:：]\\s*\"([^\"]+)\"")
        val reasonMatch = reasonRegex.find(response)
        val reason = if (reasonMatch != null) {
            reasonMatch.groupValues[1]
        } else if (score == 0 && scoreMatch == null) {
            "LLM 响应解析失败，无法提取 score"
        } else {
            "LLM 评估完成"
        }

        return Pair(score.coerceIn(0, 100), reason)
    }

    companion object {
        // L1 阈值（对齐设计文档第 864 行）
        private const val L1_THRESHOLD_AGAIN = 0.30f   // <30% → Again
        private const val L1_THRESHOLD_HARD = 0.60f    // 30-60% → Hard
        private const val L1_THRESHOLD_GOOD = 0.85f    // 60-85% → Good

        // L2 阈值（NF-A2 修正：增加 GOOD 档，L3 触发范围从 60-85% 收窄到 60-75%）
        private const val L2_THRESHOLD_HARD = 0.60f    // <60% → Hard（明显不足）
        private const val L2_THRESHOLD_PARTIAL = 0.75f // 60-75% → Hard（部分正确，触发 L3）
        private const val L2_THRESHOLD_GOOD = 0.85f    // 75-85% → Good（较好但不完美，不触发 L3）

        // L3 阈值（设计文档第 881 行）
        private const val L3_THRESHOLD_AGAIN = 60      // <60 → Again
        private const val L3_THRESHOLD_HARD = 75       // 60-75 → Hard
        private const val L3_THRESHOLD_GOOD = 90       // 75-90 → Good

        /** L2 部分正确范围（触发 L3）— NF-A2 修正：从 0.60f..0.85f 收窄到 0.60f..0.75f */
        private val PARTIAL_CORRECT_RANGE = 0.60f..0.75f

        /** 常见停用词（过滤无意义的关键词） */
        private val STOP_WORDS = setOf(
            "的是", "是一", "一种", "一个", "可以", "以及", "对于",
            "在此", "如下", "主要", "通常", "一般", "作为", "称为",
            "属于", "具有", "包括", "包含", "其中", "其他", "所以",
            "因为", "如果", "虽然", "但是", "而且", "并且", "或者",
        )

        /** 常见文学同义词映射 */
        private val SYNONYM_MAP: Map<String, List<String>> = mapOf(
            "苏轼" to listOf("苏东坡", "子瞻", "东坡"),
            "李白" to listOf("诗仙", "太白", "青莲居士"),
            "杜甫" to listOf("诗圣", "子美", "少陵野老"),
            "白居易" to listOf("乐天", "香山居士"),
            "王维" to listOf("摩诘", "诗佛"),
            "韩愈" to listOf("退之", "昌黎"),
            "柳宗元" to listOf("子厚", "柳河东"),
            "欧阳修" to listOf("永叔", "醉翁", "六一居士"),
            "《诗经》" to listOf("诗经", "诗三百"),
            "《楚辞》" to listOf("楚辞"),
            "《红楼梦》" to listOf("红楼梦", "石头记"),
            "《西游记》" to listOf("西游记"),
            "《三国演义》" to listOf("三国演义", "三国"),
            "《水浒传》" to listOf("水浒传", "水浒"),
        )
    }
}

/**
 * 题目类型。
 *
 * @property TERM_EXPLANATION 名词解释/术语（走 L1 关键词匹配）
 * @property ESSAY 论述题/分析题（走 L2 语义相似度 + L3 LLM 评估）
 */
enum class QuestionType {
    TERM_EXPLANATION,
    ESSAY,
}

/**
 * 检测层级。
 *
 * @property L1 关键词匹配 + 同义词词典（本地 <10ms）
 * @property L2 Jaccard 相似度（本地 <10ms）
 * @property L3 LLM 异步评估（在线 3-5 秒）
 */
enum class RecallLevel {
    L1,
    L2,
    L3,
}

/**
 * 回忆评分（对接 FSRS Rating）。
 */
enum class RecallRating {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}

/**
 * 回忆检测结果。
 *
 * @param level 检测层级（L1/L2/L3）
 * @param coverage 覆盖率 0-1（L1 关键词覆盖率 / L2 Jaccard 相似度 / L3 分数百分比）
 * @param rating 评分（对接 FSRS Rating）
 * @param score L3 时为 0-100 分，L1/L2 时为 null
 * @param reason L3 时为评估理由，L1/L2 时为 null
 */
data class RecallResult(
    val level: RecallLevel,
    val coverage: Float,
    val rating: RecallRating,
    val score: Int?,
    val reason: String?,
)
