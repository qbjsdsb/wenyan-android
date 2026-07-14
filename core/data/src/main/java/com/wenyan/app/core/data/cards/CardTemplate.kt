package com.wenyan.app.core.data.cards

import androidx.compose.runtime.Immutable
import com.wenyan.app.core.database.entity.CardTemplateType

/**
 * 卡片模板 sealed class（Task 17.2-17.7）。
 *
 * 6种文学专用卡片模板，遵循 Wozniak 最小信息原则（一个名词解释拆5-6张卡）。
 * 每种模板定义正反面内容组织方式，与设计文档3.3.2节5种背诵模式正交。
 *
 * 架构说明：本 sealed class 位于 core:data 模块（而非 feature:cards），
 * 因为 [com.wenyan.app.core.data.cards.CardSplitter] 与
 * [com.wenyan.app.core.data.repository.CardRepository] 均在 core:data 层
 * 需返回 [CardTemplate]，而 core:data 不能反向依赖 feature:cards。
 * feature:cards 的 CardRenderer 通过 core:data 依赖访问本类。
 *
 * @property front 卡片正面内容（问题/提示）
 * @property back 卡片背面内容（答案）
 * @property templateType 模板类型，对应 [CardTemplateType]
 * @property pointId 关联知识点 ID（阶段3新增，用于 FSRS 调度回写 memo_records）
 */
@Immutable
sealed class CardTemplate {
    abstract val front: String
    abstract val back: String
    abstract val templateType: CardTemplateType
    abstract val pointId: String
}

/**
 * 名词解释卡类别（Task 17.2）。
 * - [SOCIETY] 社团类：时间/地点/人物/刊物/主张/贡献
 * - [WORK] 作品类：作者/年代/内容/特色/影响
 */
enum class TermCategory {
    SOCIETY,
    WORK,
}

/** 社团类名词解释字段（category == SOCIETY 时使用） */
data class SocietyTermFields(
    val time: String = "",         // 时间
    val place: String = "",        // 地点
    val members: String = "",      // 人物
    val publication: String = "",  // 刊物
    val proposition: String = "",  // 主张
    val contribution: String = "", // 贡献
)

/** 作品类名词解释字段（category == WORK 时使用） */
data class WorkTermFields(
    val author: String = "",    // 作者
    val era: String = "",       // 年代
    val content: String = "",   // 内容
    val feature: String = "",   // 特色
    val influence: String = "", // 影响
)

/**
 * 名词解释卡（Task 17.2）。
 *
 * 社团类（如"文学研究会"）含 [society] 字段；
 * 作品类（如《边城》）含 [work] 字段。
 * 渲染时按 [category] 分条列出对应字段。
 */
@Immutable
data class TermExplanationCard(
    override val front: String,
    override val back: String,
    override val templateType: CardTemplateType = CardTemplateType.TERM_EXPLANATION,
    override val pointId: String = "",
    val category: TermCategory,
    /** 社团类字段（category == SOCIETY 时非空） */
    val society: SocietyTermFields? = null,
    /** 作品类字段（category == WORK 时非空） */
    val work: WorkTermFields? = null,
) : CardTemplate()

/**
 * Cloze名句填空卡（Task 17.3）。
 *
 * - [quote]：完整名句（正面挖空呈现）
 * - [blank]：填空答案
 * - [hint]：语法情感提示（辅助回忆，不直接给答案）
 */
@Immutable
data class ClozeQuoteCard(
    override val front: String,
    override val back: String,
    override val templateType: CardTemplateType = CardTemplateType.CLOZE_QUOTE,
    override val pointId: String = "",
    val quote: String,
    val blank: String,
    val hint: String,
) : CardTemplate()

/**
 * 作品-作者双向卡（Task 17.4）。
 *
 * 自动生成正反两张：正面问作品背面答作者，正面问作者背面答作品。
 * 调用方通过 [createBidirectionalPair] 一次性生成正反两张。
 */
@Immutable
data class WorkAuthorBidirectionalCard(
    override val front: String,
    override val back: String,
    override val templateType: CardTemplateType = CardTemplateType.WORK_AUTHOR_BIDIRECTIONAL,
    override val pointId: String = "",
    val work: String,
    val author: String,
) : CardTemplate() {
    companion object {
        /**
         * 由作品-作者关系生成正反两张卡片。
         * - 正向卡：front=作品名，back=作者
         * - 反向卡：front=作者，back=代表作品
         *
         * @param pointId 关联知识点 ID（两张卡共享同一知识点，用于 FSRS 调度回写）
         */
        fun createBidirectionalPair(
            work: String,
            author: String,
            pointId: String = "",
        ): List<WorkAuthorBidirectionalCard> =
            listOf(
                WorkAuthorBidirectionalCard(
                    front = work,
                    back = author,
                    pointId = pointId,
                    work = work,
                    author = author,
                ),
                WorkAuthorBidirectionalCard(
                    front = author,
                    back = work,
                    pointId = pointId,
                    work = work,
                    author = author,
                ),
            )
    }
}

/**
 * 论述要点卡（Task 17.5）。
 *
 * 遵循 Wozniak 规则：背面放 [keyPoints] 关键词提示而非完整答案，
 * 训练考场组织能力。适配 Outline 提纲背诵模式。
 */
@Immutable
data class EssayPointsCard(
    override val front: String,
    override val back: String,
    override val templateType: CardTemplateType = CardTemplateType.ESSAY_POINTS,
    override val pointId: String = "",
    val question: String,
    /** 关键词提示列表（背面展示，非完整答案） */
    val keyPoints: List<String>,
) : CardTemplate()

/**
 * 流派对照卡（Task 17.6）。
 *
 * 表格化对比多个流派（如京派/海派/新月派/象征派），
 * [schools] 列表每个元素为一个流派信息。
 */
@Immutable
data class SchoolComparisonCard(
    override val front: String,
    override val back: String,
    override val templateType: CardTemplateType = CardTemplateType.SCHOOL_COMPARISON,
    override val pointId: String = "",
    val schools: List<SchoolInfo>,
) : CardTemplate()

/** 流派信息（用于 [SchoolComparisonCard] 表格化对比） */
data class SchoolInfo(
    val name: String,            // 流派名（京派/海派/新月派/象征派）
    val period: String,          // 时期
    val representatives: String, // 代表作家
    val proposition: String,     // 主张
    val features: String,        // 特色
)

/**
 * 区分卡（Task 17.7）。
 *
 * 易混淆作家/作品对比，正反面都出（[item1]/[item2] 互为正反）。
 * 由 [com.wenyan.app.core.data.cards.CardSplitter.generateDistinctionCards] 自动生成。
 */
@Immutable
data class DistinctionCard(
    override val front: String,
    override val back: String,
    override val templateType: CardTemplateType = CardTemplateType.DISTINCTION,
    override val pointId: String = "",
    val item1: String,
    val item2: String,
    /** 区别要点列表 */
    val differences: List<String>,
) : CardTemplate()
