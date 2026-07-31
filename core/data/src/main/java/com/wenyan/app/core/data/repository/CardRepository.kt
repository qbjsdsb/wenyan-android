package com.wenyan.app.core.data.repository

import com.wenyan.app.core.data.cards.CardSplitter
import com.wenyan.app.core.data.cards.CardTemplate
import com.wenyan.app.core.data.cards.DistinctionCard
import com.wenyan.app.core.data.cards.EssayPointsCard
import com.wenyan.app.core.data.util.catchAndLog
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 卡片仓库接口(Task 18.3)。
 *
 * 职责:
 * - 获取今日待复习卡片([getCardsForReview]):将 ocr_status='VERIFIED' 的知识点
 *   经 [CardSplitter] 按最小信息原则拆分后转为卡片流。
 *
 * 抽象为接口便于测试替换(Fake 实现),生产环境由 [CardRepositoryImpl] 实现。
 * 与 [SchedulingRepository] / [ExamRepository] 同一约定。
 *
 * @see CardRepositoryImpl
 */
interface CardRepository {

    /**
     * 获取今日待复习卡片流。
     *
     * 取 ocr_status='VERIFIED' 的知识点(PENDING 不进复习队列,防背错字),
     * 逐个调用 [CardRepositoryImpl.generateCardsFromKnowledgePoint] 生成卡片后展平。
     *
     * @return 今日待复习卡片流(已按最小信息原则拆分)
     */
    fun getCardsForReview(): Flow<List<CardTemplate>>
}

/**
 * 卡片仓库实现([CardRepository] 接口的生产实现)。
 *
 * 遵循 Wozniak 20条规则:严格最小信息原则,避免集合题。
 * 通过构造函数注入 [KnowledgePointDao](Hilt @Inject),与 [ReviewRepository] 同一约定。
 *
 * P1 审计修复:map 内含 suspend DAO 查询(generateCardsFromKnowledgePoint → getByIds),
 * 加 .catchAndLog 降级为空列表,避免卡片复习界面崩溃。
 *
 * P0 修复(v0.7.2):原 [getCardsForReview] 只查 ocr_status='VERIFIED' 的知识点,
 * 完全不读 memo_records.next_review_at,导致 FSRS 调度被旁路——用户每次打开卡片页
 * 看到全部 909 知识点拆出的 ~4500 张卡,评分后卡片不消失,FSRS 形同摆设。
 * 现改为复用 [ReviewRepository.getReviewQueue](已实现 due 过滤 + 60s tickFlow 刷新),
 * 仅对到期知识点拆卡,真正实现 FSRS 间隔重复。
 */
@Singleton
class CardRepositoryImpl @Inject constructor(
    private val knowledgePointDao: KnowledgePointDao,
    private val reviewRepository: ReviewRepository,
) : CardRepository {

    private companion object {
        private const val TAG = "CardRepositoryImpl"
    }

    /**
     * 获取今日待复习卡片流(P0 修复)。
     *
     * 复用 [ReviewRepository.getReviewQueue]:仅返回 ocr_status='VERIFIED' 且到期
     * (next_review_at <= 当前时间)的知识点,并每 60s 自动刷新让新到期卡片进入队列。
     * 逐个调用 [generateCardsFromKnowledgePoint] 生成卡片后展平。
     *
     * v0.9.7 M2 修复:sibling 卡打散顺序。
     * 原实现 `duePoints.map { generateCardsFromKnowledgePoint(it) }.flatten()` 会把
     * 同一知识点的 5-6 张卡(名词解释拆卡 + 论述要点卡 + 区分卡)连续排列,
     * 用户可能连续答 5 个"建安风骨-时代/代表作家/风格/意义/影响",体验差(疲劳 + FSRS 调度被稀释)。
     * 现按 pointId 分组后交错排列(round-robin),让同 pointId 的卡分散到队列不同位置,
     * 用户先看到不同知识点的首卡,再看到次卡,类似 Anki 的"混合复习"模式。
     *
     * [generateCardsFromKnowledgePoint] 为 suspend 函数(需查询对比知识点标题),
     * 此处利用 [Flow.map] 的 suspend lambda + [Iterable.map] 的 inline 特性
     * 在 Flow 链内安全调用 suspend 函数。
     *
     * @return 今日待复习卡片流(已按最小信息原则拆分 + sibling 打散)
     */
    override fun getCardsForReview(): Flow<List<CardTemplate>> =
        reviewRepository.getReviewQueue().map { duePoints ->
            // Iterable.map 是 inline 函数,其 lambda 在 suspend 上下文中可调用 suspend 函数
            val cardsByPoint = duePoints.map { generateCardsFromKnowledgePoint(it) }
            interleaveSiblingCards(cardsByPoint)
        }.catchAndLog(TAG, "getCardsForReview") { emptyList() }

    /**
     * 按知识点分组交错排列 sibling 卡(v0.9.7 M2)。
     *
     * 输入:[[A1,A2,A3], [B1,B2], [C1,C2,C3,C4]]
     * 输出:[A1,B1,C1,A2,B2,C2,A3,C3,C4]
     *
     * 算法:round-robin 从每个分组取首张,该组取空后跳过。
     * 保证同 pointId 的卡分散到队列不同位置,避免连续 5-6 张同知识点。
     */
    private fun interleaveSiblingCards(cardsByPoint: List<List<CardTemplate>>): List<CardTemplate> {
        if (cardsByPoint.isEmpty()) return emptyList()
        // 单组时无需打散(避免无谓拷贝)
        if (cardsByPoint.size == 1) return cardsByPoint.first()

        val result = ArrayList<CardTemplate>(cardsByPoint.sumOf { it.size })
        val queues = cardsByPoint.map { it.toMutableList() }.toMutableList()
        // 按每组剩余卡数降序排序,优先取出卡多的组(让长序列先出现,均衡分布)
        // 注:每次循环重新判断,避免迭代器并发修改
        while (queues.any { it.isNotEmpty() }) {
            queues.removeAll { it.isEmpty() }
            if (queues.isEmpty()) break
            // 按 size 降序取首张(让卡多的组先出,均衡打散)
            queues.sortedByDescending { it.size }.forEach { queue ->
                if (queue.isNotEmpty()) {
                    result.add(queue.removeAt(0))
                }
            }
        }
        return result
    }

    /**
     * 根据知识点自动生成卡片(Task 18.3)。
     *
     * 生成策略(遵循 Wozniak 最小信息原则):
     * 1. 名词解释:以 [KnowledgePointEntity.title] 为名词、
     *    [KnowledgePointEntity.fullContent](缺省取 coreConclusion)为解释,
     *    调 [CardSplitter.splitTermExplanation] 拆成5-6张。
     * 2. 易混淆区分:若 [KnowledgePointEntity.contrastIds] 非空,通过 DAO 批量查询
     *    对比知识点的真实标题后生成区分卡(不再用 ID 占位)。
     *
     * @param knowledgePoint 知识点实体
     * @return 自动生成的卡片列表
     */
    suspend fun generateCardsFromKnowledgePoint(knowledgePoint: KnowledgePointEntity): List<CardTemplate> {
        val cards = mutableListOf<CardTemplate>()
        val pointId = knowledgePoint.id

        // 1. 名词解释拆卡(最小信息原则:5-6张)
        val definition = knowledgePoint.fullContent.ifBlank {
            knowledgePoint.coreConclusion
        }
        if (definition.isNotBlank()) {
            cards.addAll(
                CardSplitter.splitTermExplanation(
                    term = knowledgePoint.title,
                    definition = definition,
                    pointId = pointId,
                    fullExplanation = knowledgePoint.coreConclusion.takeIf { it.isNotBlank() && it != definition },
                    studyText = knowledgePoint.studyText,
                ),
            )
        }

        // 2. 论述要点卡:coreConclusion 作为论述题,summary 拆为关键词
        // v0.8.5 修复：原按 '。'、'；'、'，'、'\n' 切分，会把"建安风骨，源于汉末"切成两个无效片段
        // （"建安风骨"和"源于汉末"）。现仅按句末标点（。；！？\n）切分，保留分句完整性。
        val summary = knowledgePoint.summary
        if (!summary.isNullOrBlank()) {
            cards.add(
                EssayPointsCard(
                    front = knowledgePoint.title,
                    back = summary,
                    pointId = pointId,
                    question = knowledgePoint.title,
                    keyPoints = summary.split('。', '；', ';', '！', '？', '!', '?', '\n')
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it.length >= 2 },
                ),
            )
        }

        // 3. 易混淆区分卡:对比项 ID 非空时,查询真实标题后生成
        val contrastIds = knowledgePoint.contrastIds
        if (!contrastIds.isNullOrEmpty()) {
            val distinctionCards = buildDistinctionFromContrast(
                mainTerm = knowledgePoint.title,
                contrastIds = contrastIds,
                pointId = pointId,
            )
            cards.addAll(distinctionCards)
        }

        return cards
    }

    /**
     * 由对比知识点 ID 列表生成区分卡。
     *
     * 通过 [KnowledgePointDao.getByIds] 批量查询对比知识点的真实标题,
     * 确保区分卡正面显示"建安风骨 与 正始风骨"而非"建安风骨 与 kp_0123"。
     * 若某个对比 ID 无法解析(已删除/无效),跳过该对比项而非用 ID 占位。
     */
    private suspend fun buildDistinctionFromContrast(
        mainTerm: String,
        contrastIds: List<String>,
        pointId: String = "",
    ): List<DistinctionCard> {
        val contrastPoints = knowledgePointDao.getByIds(contrastIds)
        val idToTitle = contrastPoints.associateBy { it.id }

        return contrastIds.mapNotNull { contrastId ->
            val contrastTitle = idToTitle[contrastId]?.title ?: return@mapNotNull null

            DistinctionCard(
                front = "区分：$mainTerm 与 $contrastTitle",
                back = "$mainTerm 与 $contrastTitle 的区别见要点",
                pointId = pointId,
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
