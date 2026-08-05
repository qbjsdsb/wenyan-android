package com.wenyan.app.core.data.repository

import com.wenyan.app.core.data.cards.CardSplitter
import com.wenyan.app.core.data.cards.CardTemplate
import com.wenyan.app.core.data.cards.DistinctionCard
import com.wenyan.app.core.data.cards.EssayPointsCard
import com.wenyan.app.core.data.util.catchAndLog
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
     * v0.9.29：内部使用"今日学习队列"（到期复习 ∪ 每日新卡，按考频/科目筛选 + 每日限额）。
     *
     * @return 今日待复习卡片流(已按最小信息原则拆分)
     */
    fun getCardsForReview(): Flow<List<CardTemplate>>

    /**
     * 今日学习队列（v0.9.29 卡片备考系统）。
     *
     * 到期复习知识点 + 每日新卡知识点（按考频/科目筛选 + 每日限额）。
     * 供"今日任务"横幅展示（新卡/复习张数）。
     */
    fun getTodayStudyQueue(): Flow<TodayStudyQueue>

    /**
     * 学习进度（v0.9.29）：已学 / 总 VERIFIED 知识点数。
     */
    fun getStudyProgress(): Flow<StudyProgress>
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
     * 复用 [ReviewRepository.getTodayStudyQueue]:仅返回 ocr_status='VERIFIED' 且到期
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
     * v0.9.37 P0-2 优化:
     * - **拆卡缓存**:上游队列内容(id+updatedAt)未变化时直接复用上次拆卡结果,
     *   消除"每次评分触发 memo_records 变化 → due 重发 → 全量重拆数千张卡"的空耗
     *   (原结果被 CardsViewModel 的 sessionCards 冻结后丢弃,纯浪费)。
     * - **移出主线程**:拆卡([CardSplitter])与 sibling 打散为纯 CPU 计算,
     *   用 [withContext] 切到 [Dispatchers.Default],避免冷进入卡片页时
     *   主线程数百 ms 掉帧。
     *
     * [generateCardsFromKnowledgePoint] 为 suspend 函数(需查询对比知识点标题),
     * 此处利用 [Flow.map] 的 suspend lambda + [Iterable.map] 的 inline 特性
     * 在 Flow 链内安全调用 suspend 函数。
     *
     * @return 今日待复习卡片流(已按最小信息原则拆分 + sibling 打散)
     */
    override fun getCardsForReview(): Flow<List<CardTemplate>> =
        reviewRepository.getTodayStudyQueue().map { queue ->
            val points = queue.duePoints + queue.newPoints
            buildCardsIfChanged(points)
        }.catchAndLog(TAG, "getCardsForReview") { emptyList() }

    /**
     * 拆卡缓存（v0.9.37 P0-2）。
     *
     * 键 = 知识点 (id, updatedAt) 列表：内容或集合变化（评分/到期/升级）时重拆，
     * 未变化（如评分仅改 memo_records，不影响知识点内容）直接返回缓存，
     * 避免每次评分后对全部到期/新卡知识点全量重拆。
     *
     * 线程安全：Flow.map 的 lambda 在收集协程串行执行，无并发写；
     * [@Volatile] 双字段保证跨线程可见性一致（缓存写入在 Default 调度器内）。
     */
    @Volatile
    private var cachedCardKey: List<Pair<String, Long>>? = null
    @Volatile
    private var cachedCards: List<CardTemplate> = emptyList()

    private suspend fun buildCardsIfChanged(points: List<KnowledgePointEntity>): List<CardTemplate> {
        val key = cardCacheKey(points)
        if (key == cachedCardKey) return cachedCards
        // 拆卡 + 打散是纯 CPU 计算，移出主线程（Room suspend 内部自行切 IO）
        val built = withContext(Dispatchers.Default) {
            val cardsByPoint = points.map { generateCardsFromKnowledgePoint(it) }
            interleaveSiblingCards(cardsByPoint)
        }
        cachedCardKey = key
        cachedCards = built
        return built
    }

    // v0.9.29：今日任务数据委托给 ReviewRepository
    override fun getTodayStudyQueue(): Flow<TodayStudyQueue> =
        reviewRepository.getTodayStudyQueue()

    override fun getStudyProgress(): Flow<StudyProgress> =
        reviewRepository.getStudyProgress()

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

/**
 * 拆卡缓存键（v0.9.37 P0-2，internal 供单测）。
 *
 * 键 = 知识点 (id, updatedAt) **按 id 排序**列表：知识点集合或内容
 * （updatedAt 随种子升级/校对刷新）变化时键变 → 重新拆卡；仅 memo_records
 * 变化（评分）不影响键 → 命中缓存，消除每次评分后对全部到期/新卡知识点
 * 全量重拆数千张卡的空耗。
 *
 * 排序保证键**顺序无关**：即使上游队列排序策略未来调整（如 SQL 排序不稳定），
 * 也不会触发无谓的整批重拆（队列本身有序，但防御性排序成本可忽略）。
 */
internal fun cardCacheKey(points: List<KnowledgePointEntity>): List<Pair<String, Long>> =
    points.map { it.id to it.updatedAt }.sortedBy { it.first }
