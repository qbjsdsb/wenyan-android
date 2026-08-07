package com.wenyan.app.feature.knowledge

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * 论述题审题思路解析模型（v0.9.8 新增）。
 *
 * 对应 `ExamQuestionEntity.angle` 字段的 JSON 结构，
 * 定义见 [docs/design/essay-module-design.md] 3.3 节。
 *
 * JSON 示例：
 * ```json
 * {
 *   "questionType": "比较型",
 *   "coreKeywords": ["冰心", "丁玲", ...],
 *   "limitKeywords": ["创作", "不同时期"],
 *   "task": "比较异同 + 梳理演变",
 *   "breakthroughAngles": ["①时代背景...", ...],
 *   "angleRationale": "从'时代—意识—作品—地位'四维度...",
 *   "argumentPath": { "thesis": "...", "points": [...], "conclusion": "..." }
 * }
 * ```
 *
 * 所有字段可空：未填充 angle JSON 的论述题解析后各字段为 null，
 * UI 优雅降级（隐藏审题思路区块）。
 */
@Serializable
@Immutable
data class EssayAngle(
    /** 题型分类：比较型 / 演变型 / 作品分析型 / 理论应用型 / 评价型 / 综合型 */
    val questionType: String? = null,
    /** 核心关键词（审题时必须覆盖的概念） */
    val coreKeywords: List<String>? = null,
    /** 限制关键词（审题时必须注意的限定条件） */
    val limitKeywords: List<String>? = null,
    /** 任务要求（题目要你做什么：比较/梳理/分析/评价） */
    val task: String? = null,
    /** 突破角度（3-5 个答题切入点，含编号①②③） */
    val breakthroughAngles: List<String>? = null,
    /** 角度选择理由（为什么选这些角度，帮用户理解审题逻辑） */
    val angleRationale: String? = null,
    /** 论证路径（总-分-总结构） */
    val argumentPath: EssayArgumentPath? = null,
)

/**
 * 论证路径（总-分-总结构）。
 *
 * 对应 [EssayAngle.argumentPath] 字段，帮助用户理解答题骨架：
 * - [thesis]：总论点（开头段的核心判断）
 * - [points]：分论点列表（正文段落，每个含 label + content）
 * - [conclusion]：结论（结尾段的升华或总结）
 */
@Serializable
@Immutable
data class EssayArgumentPath(
    val thesis: String? = null,
    val points: List<EssayArgumentPoint>? = null,
    val conclusion: String? = null,
)

/**
 * 分论点（论证路径的一个段落）。
 *
 * @param label 段落标签（如"总述（同）"、"分1·冰心"、"总结（异+演变）"）
 * @param content 段落内容（答题要点，1-2 句精炼表述）
 */
@Serializable
@Immutable
data class EssayArgumentPoint(
    val label: String? = null,
    val content: String? = null,
)

/**
 * 论述题依据与交叉验证解析模型（v0.9.8 新增）。
 *
 * 对应 `ExamQuestionEntity.notes` 字段的 JSON 结构，
 * 定义见 [docs/design/essay-module-design.md] 3.4 节。
 *
 * JSON 示例：
 * ```json
 * {
 *   "evidences": [{ "type": "WORK_TEXT", "label": "作品原文", "content": "...", "source": "...", "linkedKnowledgePointId": "kp_00595" }],
 *   "crossValidation": { "textbookComparison": "...", "scholarComparison": "..." },
 *   "referenceLinks": [{ "label": "中国作家网·...", "url": "https://..." }],
 *   "knowledgeGaps": [{ "author": "萧红", "note": "项目暂无萧红独立知识点..." }]
 * }
 * ```
 *
 * 关键约束：依据必须真实，不能 AI 编造（详见设计文档 5.3 节）。
 * 所有字段可空，UI 优雅降级（隐藏依据区块）。
 */
@Serializable
@Immutable
data class EssayNotes(
    /** 依据列表（作品原文 / 学者观点 / 教材共识） */
    val evidences: List<EssayEvidence>? = null,
    /** 交叉验证（教材对比 + 学者对比） */
    val crossValidation: EssayCrossValidation? = null,
    /** 参考链接（可点击的交叉验证 URL） */
    val referenceLinks: List<EssayReferenceLink>? = null,
    /** 知识盲点检测（项目暂无的知识点提醒补充） */
    val knowledgeGaps: List<EssayKnowledgeGap>? = null,
)

/**
 * 依据项（论述题答案的支撑材料）。
 *
 * @param type 依据类型：WORK_TEXT（作品原文）/ SCHOLAR_OPINION（学者观点）/ TEXTBOOK_CONSENSUS（教材共识）
 * @param label 显示标签（如"作品原文"、"学者观点"）
 * @param content 依据内容（引用原文或观点表述）
 * @param source 来源（如"冰心《繁星·春水》人民文学出版社"）
 * @param linkedKnowledgePointId 关联知识点 ID（点击可跳转知识点详情）
 */
@Serializable
@Immutable
data class EssayEvidence(
    val type: String? = null,
    val label: String? = null,
    val content: String? = null,
    val source: String? = null,
    @SerialName("linkedKnowledgePointId")
    val linkedKnowledgePointId: String? = null,
)

/**
 * 交叉验证（多教材/多学者对比）。
 *
 * 帮助用户理解同一知识点在不同教材/学者间的定位差异，
 * 培养交叉验证的学术习惯。
 *
 * @param textbookComparison 教材对比（如钱理群 vs 丁帆的定位差异）
 * @param scholarComparison 学者对比（如女性主义视角 vs 文学史主流视角）
 */
@Serializable
@Immutable
data class EssayCrossValidation(
    val textbookComparison: String? = null,
    val scholarComparison: String? = null,
)

/**
 * 参考链接（可点击的交叉验证 URL）。
 *
 * 链接来源限于权威开放资源：中国作家网 / 国家哲学社科文献库 / CNKI 等。
 * 不含需要付费或登录的资源，确保用户可直接访问验证。
 *
 * @param label 显示文本（如"中国作家网·茹志鹃：历史褶皱里的文学烛照"）
 * @param url 完整 URL
 */
@Serializable
@Immutable
data class EssayReferenceLink(
    val label: String? = null,
    val url: String? = null,
)

/**
 * 知识盲点检测（项目暂无的知识点提醒）。
 *
 * 当论述题涉及的知识点不在项目当前知识点库中时，
 * 提醒用户该知识点缺失，建议补充。
 *
 * @param author 涉及的作家/概念名（如"萧红"）
 * @param note 补充建议（如"建议补充'萧红《生死场》《呼兰河传》与散文化叙事'知识点"）
 */
@Serializable
@Immutable
data class EssayKnowledgeGap(
    val author: String? = null,
    val note: String? = null,
)

/**
 * JSON 解析器（宽松模式，忽略未知字段）。
 *
 * 与 [com.wenyan.app.core.data.seed.SeedDataLoader] 的 Json 配置一致，
 * 允许 seed_data.json 演进时新增字段而不破坏旧版本解析。
 */
private val essayJson = Json { ignoreUnknownKeys = true }

/**
 * 安全解析 angle JSON（v0.9.8 新增）。
 *
 * 解析失败时返回 null（UI 隐藏审题思路区块），
 * 不抛异常（JSON 格式错误不应崩溃）。
 *
 * v0.9.8 审查修复：补 Timber.w 日志（原静默吞异常，与 EssayDetailViewModel KDoc
 * 声明的"Timber.w 日志"不符，且与 v0.9.7 M9 修复模式一致——静默失败不利于排查
 * seed_data.json 格式错误）。
 */
internal fun parseEssayAngle(json: String?): EssayAngle? {
    if (json.isNullOrBlank()) return null
    return try {
        essayJson.decodeFromString<EssayAngle>(json)
    } catch (e: Exception) {
        Timber.w(e, "parseEssayAngle failed: json=%s", json.take(200))
        null
    }
}

/**
 * 安全解析 notes JSON（v0.9.8 新增）。
 *
 * 解析失败时返回 null（UI 隐藏依据区块），
 * 不抛异常。日志策略同 [parseEssayAngle]。
 */
internal fun parseEssayNotes(json: String?): EssayNotes? {
    if (json.isNullOrBlank()) return null
    return try {
        essayJson.decodeFromString<EssayNotes>(json)
    } catch (e: Exception) {
        Timber.w(e, "parseEssayNotes failed: json=%s", json.take(200))
        null
    }
}
