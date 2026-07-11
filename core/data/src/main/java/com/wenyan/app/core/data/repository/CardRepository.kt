package com.wenyan.app.core.data.repository

import com.wenyan.app.core.data.cards.CardSplitter
import com.wenyan.app.core.data.cards.CardTemplate
import com.wenyan.app.core.data.cards.DistinctionCard
import com.wenyan.app.core.data.cards.EssayPointsCard
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 卡片仓库（Task 18.3）。
 *
 * 职责：
 * - 获取今日待复习卡片（[getCardsForReview]）：将 ocr_status='VERIFIED' 的知识点
 *   经 [CardSplitter] 按最小信息原则拆分后转为卡片流。
 * - 根据知识点自动生成卡片（[generateCardsFromKnowledgePoint]）：
 *   名词解释拆5-6张、集合题转分组枚举、易混淆生成区分卡。
 *
 * 遵循 Wozniak 20条规则：严格最小信息原则，避免集合题。
 * 通过构造函数注入 [KnowledgePointDao]（Hilt @Inject），与 [ReviewRepository] 同一约定。
 */
@Singleton
class CardRepository @Inject constructor(
    private val knowledgePointDao: KnowledgePointDao,
) {

    /**
     * 获取今日待复习卡片流。
     *
     * 取 ocr_status='VERIFIED' 的知识点（PENDING 不进复习队列，防背错字），
     * 逐个调用 [generateCardsFromKnowledgePoint] 生成卡片后展平。
     *
     * [generateCardsFromKnowledgePoint] 为 suspend 函数（需查询对比知识点标题），
     * 此处利用 [Flow.map] 的 suspend lambda + [Iterable.map] 的 inline 特性
     * 在 Flow 链内安全调用 suspend 函数。
     *
     * @return 今日待复习卡片流（已按最小信息原则拆分）
     */
    fun getCardsForReview(): Flow<List<CardTemplate>> =
        knowledgePointDao.observeVerifiedForReview().map { verifiedPoints ->
            // Iterable.map 是 inline 函数，其 lambda 在 suspend 上下文中可调用 suspend 函数
            verifiedPoints.map { generateCardsFromKnowledgePoint(it) }.flatten()
        }

    /**
     * 根据知识点自动生成卡片（Task 18.3）。
     *
     * 生成策略（遵循 Wozniak 最小信息原则）：
     * 1. 名词解释：以 [KnowledgePointEntity.title] 为名词、
     *    [KnowledgePointEntity.fullContent]（缺省取 coreConclusion）为解释，
     *    调 [CardSplitter.splitTermExplanation] 拆成5-6张。
     * 2. 易混淆区分：若 [KnowledgePointEntity.contrastIds] 非空，通过 DAO 批量查询
     *    对比知识点的真实标题后生成区分卡（不再用 ID 占位）。
     *
     * @param knowledgePoint 知识点实体
     * @return 自动生成的卡片列表
     */
    suspend fun generateCardsFromKnowledgePoint(knowledgePoint: KnowledgePointEntity): List<CardTemplate> {
        val cards = mutableListOf<CardTemplate>()

        // 1. 名词解释拆卡（最小信息原则：5-6张）
        val definition = knowledgePoint.fullContent.ifBlank {
            knowledgePoint.coreConclusion
        }
        if (definition.isNotBlank()) {
            cards.addAll(
                CardSplitter.splitTermExplanation(knowledgePoint.title, definition),
            )
        }

        // 2. 论述要点卡：coreConclusion 作为论述题，summary 拆为关键词
        val summary = knowledgePoint.summary
        if (!summary.isNullOrBlank()) {
            cards.add(
                EssayPointsCard(
                    front = knowledgePoint.title,
                    back = summary,
                    question = knowledgePoint.title,
                    keyPoints = summary.split('。', '；', '，', '\n')
                        .map { it.trim() }
                        .filter { it.isNotBlank() },
                ),
            )
        }

        // 3. 易混淆区分卡：对比项 ID 非空时，查询真实标题后生成
        val contrastIds = knowledgePoint.contrastIds
        if (!contrastIds.isNullOrEmpty()) {
            val distinctionCards = buildDistinctionFromContrast(
                mainTerm = knowledgePoint.title,
                contrastIds = contrastIds,
            )
            cards.addAll(distinctionCards)
        }

        return cards
    }

    /**
     * 由对比知识点 ID 列表生成区分卡。
     *
     * 通过 [KnowledgePointDao.getByIds] 批量查询对比知识点的真实标题，
     * 确保区分卡正面显示"建安风骨 与 正始风骨"而非"建安风骨 与 kp_0123"。
     * 若某个对比 ID 无法解析（已删除/无效），跳过该对比项而非用 ID 占位。
     */
    private suspend fun buildDistinctionFromContrast(
        mainTerm: String,
        contrastIds: List<String>,
    ): List<DistinctionCard> {
        val contrastPoints = knowledgePointDao.getByIds(contrastIds)
        val idToTitle = contrastPoints.associateBy { it.id }

        return contrastIds.mapNotNull { contrastId ->
            val contrastTitle = idToTitle[contrastId]?.title ?: return@mapNotNull null

            DistinctionCard(
                front = "区分：$mainTerm 与 $contrastTitle",
                back = "$mainTerm 与 $contrastTitle 的区别见要点",
                item1 = mainTerm,
                item2 = contrastTitle,
                differences = listOf(
                    "$mainTerm 与 $contrastTitle 定义/范畴不同",
                    "$mainTerm 与 $contrastTitle 代表人物/作品对比",
                    "$mainTerm 与 $contrastTitle 文学主张/风格对比",
                ),
            )
        }
    }
}
