package com.wenyan.app.core.data.seed

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.dao.ChapterDao
import com.wenyan.app.core.database.dao.ExamCodeHistoryDao
import com.wenyan.app.core.database.dao.ExamQuestionDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.dao.SubjectDao
import com.wenyan.app.core.database.dao.WritingMaterialDao
import com.wenyan.app.core.database.entity.ChapterEntity
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.GraphEdgeEntity
import com.wenyan.app.core.database.entity.GraphEdgeType
import com.wenyan.app.core.database.entity.GraphNodeEntity
import com.wenyan.app.core.database.entity.GraphNodeType
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import com.wenyan.app.core.database.entity.WritingMaterialEntity
import com.wenyan.app.core.data.repository.GraphRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 种子数据加载器（阶段2：数据管线接通）。
 *
 * 职责：
 * - 首次启动时从 assets/seed_data.json 读取种子数据
 * - 按外键顺序导入到 Room 数据库：subjects → chapters → knowledge_points → memo_records → exam_questions → writing_materials
 * - 使用 DataStore 记录是否已完成初始化，避免重复导入
 *
 * 种子数据结构对齐 generate_seed.py 输出格式，覆盖四类：
 * 知识点 / 真题 / 写作素材（卡片由 [com.wenyan.app.core.data.repository.CardRepository] 动态生成，不入库）。
 *
 * P0-D2 修正：导入过程用 [WenyanDatabase.withTransaction] 包裹，确保 7 步原子性。
 * 原实现无事务包裹，中途失败会留下半成品数据 + DataStore 已写"initialized" → 永久半成品。
 *
 * NF-J 修复（种子加载链路稳健性）：
 * - 合并双 DataStore：原先用独立的 `wenyan_seed_prefs` DataStore 记录初始化标志，
 *   与主 `wenyan_preferences` 分离，导致全局偏好分散在两处。现统一注入 Hilt 单例
 *   [DataStore]<[Preferences]>，与 [com.wenyan.app.core.data.di.DataStoreModule] 提供的
 *   主 DataStore 共用一个文件（wenyan_preferences.preferences_pb）。
 * - [isInitialized] / [markInitialized] 加 [IOException] 兜底：DataStore 读写在磁盘
 *   故障/权限异常时抛 IOException，原实现会冒泡到 Application 的 CoroutineExceptionHandler
 *   导致种子加载被吞掉、下次启动仍报错。现 [isInitialized] 异常时假设"未初始化"
 *   让种子重试导入，[markInitialized] 异常时仅 Log.w 不冒泡（App 继续工作，下次启动重试，
 *   种子重复导入用 REPLACE 策略幂等）。
 * - 不修改 DAO 的 `@Insert(onConflict = REPLACE)` 策略：种子加载只在
 *   `isInitialized() == false` 时执行（首次安装），事务包裹确保原子性，首次安装
 *   用户无数据可被覆盖。
 *
 * P1-AUDIT-4 修复：版本感知种子升级。原实现只用 boolean `seed_initialized` 标志，
 * 更新 seed_data.json 后用户不会获得新内容（标志仍为 true）。现改为：
 * - 存储 seed_data.json 的 metadata.version 到 DataStore
 * - 启动时比对存储版本与当前 seed 版本
 * - 版本不一致时重新导入内容表（subjects/chapters/knowledge_points/exam_questions/
 *   writing_materials/graph），用 @Upsert 安全更新内容
 * - **保护 MemoRecord**：升级时跳过已有 MemoRecord 的知识点（保留用户 FSRS 学习进度），
 *   仅为新增知识点创建初始 MemoRecord
 */
@Singleton
class SeedDataLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: DataStore<Preferences>,
    private val database: WenyanDatabase,
    private val examCodeHistoryDao: ExamCodeHistoryDao,
    private val graphRepository: GraphRepository,
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val knowledgePointDao: KnowledgePointDao,
    private val examQuestionDao: ExamQuestionDao,
    private val writingMaterialDao: WritingMaterialDao,
    private val memoRecordDao: MemoRecordDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 确保种子数据已加载：若尚未初始化或种子版本已更新则导入，否则跳过。
     *
     * P1-AUDIT-4 修复：版本感知升级逻辑。
     * - 首次安装（initialized=false）：全量导入，包括 MemoRecord
     * - 种子版本升级（initialized=true 但版本不同）：导入内容表，跳过已有 MemoRecord
     * - 无变化（initialized=true 且版本相同）：跳过
     *
     * NF-J 修复：[isInitialized] 与 [markInitialized] 都已加 IOException 兜底，
     * 此方法自身不会再因 DataStore 故障抛 IOException；[readSeedDataFromAssets]
     * 与 [importToDatabase] 的异常会冒泡到 Application 的 CoroutineExceptionHandler
     * 被吞掉（Log.e），下次启动重试。
     */
    suspend fun ensureSeedDataLoaded() {
        val initialized = isInitialized()
        val seedData = readSeedDataFromAssets()
        val currentVersion = seedData.metadata.version.ifBlank { DEFAULT_SEED_VERSION }
        val storedVersion = getStoredSeedVersion()

        // 主内容（subjects/chapters/knowledge_points/exam_questions/writing_materials）
        // 已初始化且版本一致 → 跳过主流程。但图谱导入可能上次失败，需独立检查。
        val mainContentReady = initialized && storedVersion == currentVersion

        if (mainContentReady && isGraphInitialized()) {
            // 主内容 + 图谱都就绪，直接返回
            return
        }

        if (!mainContentReady) {
            val isUpgrade = initialized && storedVersion != currentVersion
            if (isUpgrade) {
                Log.i(TAG, "Seed version upgrade: $storedVersion → $currentVersion, re-importing content (MemoRecord preserved)")
            }
            importToDatabase(seedData, isUpgrade = isUpgrade)
            markInitialized()
            storeSeedVersion(currentVersion)
        } else {
            Log.i(TAG, "Main content ready but graph not initialized, retrying graph import")
        }

        // v0.7.2 修复：图谱骨架导入移出主事务，用独立事务 + try-catch。
        // 原实现在 [importToDatabase] 的 withTransaction 内调用 [importGraphSkeleton]，
        // 一旦图谱 FK 约束失败（如 SUBJECT_ID 与 seed_data.json 不匹配），
        // 整个事务回滚，909 条知识点全部丢失。
        // 现在主事务已提交 + markInitialized 已执行，图谱失败只影响图谱功能，
        // 知识点不受影响。
        //
        // v0.8.12 修复（P1-1 反向验证发现）：原实现注释说"下次启动重试"，
        // 但实际 [ensureSeedDataLoaded] 在 initialized=true 且版本一致时直接 return，
        // 永不重试图谱导入 → 图谱首次失败就永久缺失。
        // 现新增 [graph_initialized] 独立标志：图谱导入成功才置 true，
        // 下次启动主流程跳过后仍检查 [isGraphInitialized]，false 则重试。
        try {
            database.withTransaction {
                importGraphSkeleton()
                // v0.7.7 新增：从 910 知识点 entities/relations 自动建图。
                // 覆盖率从 4.4%（40 硬编码节点）→ 100%（2123 实体 + 968 边）。
                importGraphFromSeedEntities(seedData)
            }
            markGraphInitialized()
        } catch (e: Exception) {
            Log.w(TAG, "Graph import failed, will retry on next launch (graph_initialized stays false)", e)
        }
    }

    /**
     * 读取 DataStore 中是否已初始化标志。
     *
     * NF-J 修复：DataStore 读写可能抛 [IOException]（磁盘满/权限/文件损坏），
     * 兜底返回 false 让种子重试导入（[importToDatabase] 用事务包裹 + REPLACE 幂等，
     * 重复导入无副作用）。
     */
    private suspend fun isInitialized(): Boolean = try {
        preferencesDataStore.data.map { it[SEED_INITIALIZED_KEY] ?: false }.first()
    } catch (e: IOException) {
        Log.w(TAG, "DataStore read failed, assuming seed not initialized", e)
        false
    }

    /**
     * 标记初始化完成。
     *
     * NF-J 修复：DataStore 写失败时仅 Log.w 不冒泡。原因：
     * 1. 此时种子已成功导入数据库（[importToDatabase] 事务已提交）；
     * 2. 若冒泡到 Application，会被 CoroutineExceptionHandler 吞掉，无意义；
     * 3. 下次启动 [isInitialized] 仍返回 false → 重新导入（REPLACE 幂等，无副作用）。
     * 唯一代价是下次启动会重复跑一次种子导入，可接受。
     */
    private suspend fun markInitialized() {
        try {
            preferencesDataStore.edit { it[SEED_INITIALIZED_KEY] = true }
        } catch (e: IOException) {
            Log.w(TAG, "DataStore write failed, seed will re-import on next launch", e)
        }
    }

    /**
     * 读取 DataStore 中存储的种子版本（P1-AUDIT-4）。
     *
     * 用于版本感知升级：与当前 seed_data.json 的 metadata.version 比对，
     * 不一致时触发重新导入。IOException 时返回空串（视为"未存储版本"→触发导入）。
     */
    private suspend fun getStoredSeedVersion(): String = try {
        preferencesDataStore.data.map { it[SEED_VERSION_KEY] ?: "" }.first()
    } catch (e: IOException) {
        Log.w(TAG, "DataStore read failed for seed version, assuming empty", e)
        ""
    }

    /**
     * 存储种子版本到 DataStore（P1-AUDIT-4）。
     *
     * 与 [markInitialized] 同样的 IOException 兜底策略：写失败仅 Log.w，
     * 下次启动会因版本不匹配重新导入（@Upsert 幂等，无副作用）。
     */
    private suspend fun storeSeedVersion(version: String) {
        try {
            preferencesDataStore.edit { it[SEED_VERSION_KEY] = version }
        } catch (e: IOException) {
            Log.w(TAG, "DataStore write failed for seed version: $version", e)
        }
    }

    /**
     * 读取图谱导入状态（v0.8.12 P1-1 修复）。
     *
     * 与主 [isInitialized] 解耦：主内容（知识点等）导入成功 ≠ 图谱导入成功。
     * 图谱导入在独立事务中，可能因 FK 约束失败而 [ensureSeedDataLoaded] 的
     * try-catch 吞掉异常。若不独立追踪，下次启动会因主 initialized=true 直接 return，
     * 图谱永久缺失。
     *
     * IOException 时返回 false（让图谱重试导入，@Upsert 幂等无副作用）。
     */
    private suspend fun isGraphInitialized(): Boolean = try {
        preferencesDataStore.data.map { it[SEED_GRAPH_INITIALIZED_KEY] ?: false }.first()
    } catch (e: IOException) {
        Log.w(TAG, "DataStore read failed for graph_initialized, assuming false", e)
        false
    }

    /**
     * 标记图谱导入完成（v0.8.12 P1-1 修复）。
     *
     * 仅在 [ensureSeedDataLoaded] 的图谱导入事务成功提交后调用。
     * IOException 时仅 Log.w 不冒泡：下次启动 [isGraphInitialized] 返回 false
     * 会重新导入图谱（@Upsert 幂等，重复节点/边无副作用）。
     */
    private suspend fun markGraphInitialized() {
        try {
            preferencesDataStore.edit { it[SEED_GRAPH_INITIALIZED_KEY] = true }
        } catch (e: IOException) {
            Log.w(TAG, "DataStore write failed for graph_initialized, will retry on next launch", e)
        }
    }

    // 从 assets 读取并解析 seed_data.json
    private fun readSeedDataFromAssets(): SeedData {
        val jsonStr = context.assets.open(SEED_DATA_FILE).bufferedReader().use { it.readText() }
        return json.decodeFromString(SeedData.serializer(), jsonStr)
    }

    /**
     * 将种子数据导入 Room 数据库（阶段2）。
     *
     * 严格按外键依赖顺序导入：
     * 1. 科目（subjects）→ 2. 默认章节（chapters）→ 3. 知识点（knowledge_points）
     *    → 4. 记忆记录（memo_records，初始 state=NEW）→ 5. 真题（exam_questions）
     *    → 6. 写作素材（writing_materials）
     * 7. 保留科目代码历史 + 知识图谱骨架导入
     *
     * 卡片不入库：由 [com.wenyan.app.core.data.repository.CardRepository] 从知识点动态生成。
     *
     * P0-D2 修正：整个导入过程用 [database.withTransaction] 包裹，确保 7 步原子性。
     * 任何一步失败将回滚全部已插入数据，且 markInitialized() 不会被调用（在事务外），
     * 下次启动会重新尝试导入，避免留下"半成品数据 + initialized=true"的永久不一致。
     *
     * P1-AUDIT-4 修复：[isUpgrade] 参数控制 MemoRecord 导入策略。
     * - 首次安装（isUpgrade=false）：为所有知识点创建初始 MemoRecord
     * - 版本升级（isUpgrade=true）：查询已有 MemoRecord 的 point_id，仅为新增知识点
     *   创建 MemoRecord，**不覆盖用户 FSRS 学习进度**（stability/difficulty/nextReviewAt）
     */
    private suspend fun importToDatabase(seedData: SeedData, isUpgrade: Boolean = false) = database.withTransaction {
        val now = System.currentTimeMillis()

        // 步骤1：导入科目
        val subjectEntities = seedData.subjects.map { seed ->
            SubjectEntity(
                id = seed.id,
                name = seed.name,
                shortName = seed.name.take(2),
                sortOrder = SUBJECT_ORDER[seed.code] ?: 99,
            )
        }
        if (subjectEntities.isNotEmpty()) {
            subjectDao.insertAll(subjectEntities)
        }

        // 构建 subjectName → subjectId 映射（供知识点/真题映射）
        val subjectNameToId = seedData.subjects.associate { it.name to it.id }

        // 步骤2：为每科创建默认章节（知识点外键依赖 chapters）
        val defaultChapters = seedData.subjects.map { seed ->
            ChapterEntity(
                id = "chapter_default_${seed.code}",
                subjectId = seed.id,
                parentId = null,
                title = "${seed.name}·默认章节",
                sortOrder = 0,
            )
        }
        if (defaultChapters.isNotEmpty()) {
            chapterDao.insertAll(defaultChapters)
        }

        // 构建 subjectName → chapterId 映射
        val subjectNameToChapterId = seedData.subjects.associate {
            it.name to "chapter_default_${it.code}"
        }

        // 步骤3：导入知识点（按 subject 字段映射到默认章节）
        // 注意：若知识点 subject 不在 subjects 列表中，跳过该知识点（避免外键约束失败）
        val knowledgePointEntities = seedData.knowledgePoints.mapNotNull { seed ->
            val chapterId = subjectNameToChapterId[seed.subject]
                ?: return@mapNotNull null
            KnowledgePointEntity(
                id = seed.id,
                chapterId = chapterId,
                title = seed.title,
                summary = seed.summary,
                coreConclusion = seed.coreConclusion,
                fullContent = seed.fullContent.ifBlank { seed.coreConclusion },
                multiPerspectives = seed.multiPerspectives?.toPerspectiveMap(),
                relatedIds = null,
                contrastIds = null,
                extensionIds = null,
                examRecords = null,
                // v0.7.9 修复：原硬编码 "NEVER"，导致 seed_data.json 的考频数据丢失。
                // 现从 KnowledgePointSeed.examFrequency 读取（generate_seed.py 派生）。
                examFrequency = seed.examFrequency ?: "NEVER",
                termTemplate = null,
                tags = seed.tags,
                difficulty = seed.difficulty,
                createdAt = now,
                updatedAt = now,
                contentSource = "TEXTBOOK_NATIVE",
                ocrStatus = "VERIFIED",
                sourceFile = seed.sourceRef,
                sourcePage = null,
                studyText = seed.studyText,
            )
        }
        if (knowledgePointEntities.isNotEmpty()) {
            knowledgePointDao.insertAll(knowledgePointEntities)
        }

        // 步骤4：为每个知识点创建初始 MemoRecord（state=NEW，立即到期可复习）
        // P1-AUDIT-4 修复：升级时跳过已有 MemoRecord 的知识点，保留用户 FSRS 学习进度。
        // 仅为新增知识点（种子更新后新加的）创建初始 MemoRecord。
        val existingMemoPointIds = if (isUpgrade) {
            memoRecordDao.getExistingPointIds().toSet()
        } else {
            emptySet()
        }
        val memoRecords = knowledgePointEntities
            .filter { kp -> kp.id !in existingMemoPointIds }
            .map { kp ->
                MemoRecordEntity(
                    pointId = kp.id,
                    state = "NEW",
                    stability = 0f,
                    difficulty = 5f,
                    // P2-AUDIT-1 修正：lastReviewAt = 0L 表示"从未复习"，原为 now 语义错误
                    lastReviewAt = 0L,
                    nextReviewAt = now, // 立即到期，新知识点可立即进入复习队列
                    reviewCount = 0,
                    failCount = 0,
                    inPriorityQueue = 0,
                )
            }
        if (memoRecords.isNotEmpty()) {
            memoRecordDao.insertAll(memoRecords)
        }
        if (isUpgrade) {
            Log.i(TAG, "Seed upgrade: created ${memoRecords.size} new MemoRecords, preserved ${existingMemoPointIds.size} existing")
        }

        // 步骤5：导入真题（按 subject 字段映射到 subjectId）
        // 注意：若真题 subject 不在 subjects 列表中，跳过该真题（避免外键约束失败）
        val examQuestionEntities = seedData.examQuestions.mapNotNull { seed ->
            val subjectId = subjectNameToId[seed.subject]
                ?: return@mapNotNull null
            ExamQuestionEntity(
                id = seed.id,
                year = seed.year,
                subjectId = subjectId,
                questionType = seed.questionType,
                content = seed.content,
                score = seed.score,
                angle = null,
                relatedPointIds = null,
                answerFramework = seed.answerFramework,
                notes = null,
                createdAt = now,
                examPaperCode = seed.examPaperCode,
                answerStatus = if (seed.answerFramework != null) "HAS_ANSWER" else "NO_ANSWER",
                materialText = null,
                sourceFile = null,
                sourcePage = null,
            )
        }
        if (examQuestionEntities.isNotEmpty()) {
            examQuestionDao.insertAll(examQuestionEntities)
        }

        // 步骤6：导入写作素材
        val writingMaterialEntities = seedData.writingMaterials.map { seed ->
            WritingMaterialEntity(
                id = seed.id,
                category = seed.category,
                subCategory = seed.subCategory,
                content = seed.content,
                source = seed.source,
                tags = seed.tags,
                createdAt = now,
            )
        }
        if (writingMaterialEntities.isNotEmpty()) {
            writingMaterialDao.insertAll(writingMaterialEntities)
        }

        // 步骤7：保留科目代码历史（图谱骨架导入移至主事务外，见 [ensureSeedDataLoaded]）
        examCodeHistoryDao.insertAll(ExamCodeHistoryData.EXAM_CODE_HISTORY)
    }

    /**
     * 导入知识图谱骨架数据。
     *
     * 包括：
     * - 南师大现当代文学考点骨架（13 位作家节点 + 12 条作家关系）
     * - 体裁×时段二维矩阵骨架（v0.7.6 细化：11 节点 + 36 关系，含 7 个文学史分期）
     * - 文学流派/社团骨架（v0.7.3 新增：14 节点 + 18 关系）
     * - 跨类边（v0.7.6 新增：35 条，作家-流派 PARTICIPATED_IN + 作家-体裁 BELONGS_TO）
     *
     * v0.7.3 更新：节点总数从 13 扩充至 40+，大部分节点关联真实知识点 ID，
     * 用户点击图谱节点可跳转到对应知识点详情页。
     *
     * v0.7.6 更新：节点总数扩至 50+，关系总数扩至 100+，补足跨类边形成完整的
     * "作家 ↔ 流派 ↔ 体裁 ↔ 时段"知识网络，配合时间轴布局让用户直观把握文学史经纬。
     */
    private suspend fun importGraphSkeleton() {
        // 南师大现当代文学考点骨架
        GraphSkeleton.SOUTHERN_NORMAL_AUTHORS.forEach { graphRepository.insertNode(it) }
        GraphSkeleton.SOUTHERN_NORMAL_RELATIONS.forEach { graphRepository.insertEdge(it) }

        // 体裁×时段二维矩阵骨架
        GraphSkeleton.GENRE_PERIOD_MATRIX.forEach { graphRepository.insertNode(it) }
        GraphSkeleton.GENRE_PERIOD_RELATIONS.forEach { graphRepository.insertEdge(it) }

        // v0.7.3 新增：文学流派/社团骨架
        GraphSkeleton.LITERARY_SCHOOLS.forEach { graphRepository.insertNode(it) }
        GraphSkeleton.LITERARY_SCHOOL_RELATIONS.forEach { graphRepository.insertEdge(it) }

        // v0.7.6 新增：跨类边（作家-流派 PARTICIPATED_IN + 作家-体裁 BELONGS_TO）
        GraphSkeleton.CROSS_CATEGORY_RELATIONS.forEach { graphRepository.insertEdge(it) }
    }

    /**
     * 从种子知识点的 entities/relations 自动生成图谱节点和边（v0.7.7 新增）。
     *
     * **核心改进**：v0.7.6 之前图谱只有 40 个硬编码节点（覆盖率 4.4%），
     * 用户反馈"知识图谱还是一团糟，不够帮助学习"。调研发现 seed_data.json
     * 已为 910 知识点 100% 提取了实体（3336 条，去重 2123 个）和关系（968 条），
     * 但 SeedDataLoader 完全未解析这些字段。
     *
     * 本方法遍历所有知识点的 entities/relations，自动建图：
     *
     * 1. **实体去重建节点**：按 normalized（规范化名）去重，同名实体合并为一个节点。
     *    - 节点 ID = "auto-" + 稳定哈希（normalized + type）
     *    - 节点 type = entity.type 映射到 GraphNodeType
     *    - 节点 color = 按 type 分配分类色（作家粉/作品橙/概念蓝/流派紫等）
     *    - 节点 relatedPointId = 该实体首次出现的知识点 ID（点击节点可跳转）
     *    - 节点 subjectId = 该实体首次出现的知识点所属科目
     *    - 节点 metadata = {"sourceKpIds": "kp_00001,kp_00013,..."} 记录关联知识点
     *
     * 2. **关系去重建边**：按 (from, relation, to) 去重，相同三元组合并为一条边。
     *    - 边 ID = "auto-edge-" + 稳定哈希
     *    - 边 sourceId/targetId = 通过实体名查找节点 ID
     *    - 边 type = relation 字段映射到 GraphEdgeType（AUTHORED→AUTHORED 等）
     *
     * 3. **科目映射**：通过 subjectName→subjectId 映射，确保节点 subjectId FK 有效。
     *
     * **结果**：图谱节点数从 40 → 2123+，边数从 100 → 1000+，覆盖率 4.4% → 100%。
     * 用户可在知识图谱中看到所有考研知识点涉及的作家、作品、概念、流派及其关系。
     */
    private suspend fun importGraphFromSeedEntities(seedData: SeedData) {
        // 构建 subjectName → subjectId 映射
        val subjectNameToId = seedData.subjects.associate { it.name to it.id }

        // ── 1. 实体去重建节点 ──
        // key = normalized|type，value = 节点数据
        data class EntityKey(val normalized: String, val type: String)
        data class EntityAccumulator(
            val name: String,
            val type: String,
            val firstKpId: String,
            val firstSubjectId: String?,
            val sourceKpIds: MutableList<String> = mutableListOf(),
            // v0.7.9 新增：考频（取关联知识点中的最高频，HIGH>MEDIUM>LOW>NEVER）
            var examFrequency: String = "NEVER",
        )

        val entityMap = mutableMapOf<EntityKey, EntityAccumulator>()
        for (kp in seedData.knowledgePoints) {
            val subjectId = subjectNameToId[kp.subject]
            val entities = kp.entities ?: continue
            // v0.7.9：从知识点读取考频（修复前 KnowledgePointSeed 未解析此字段，恒为 NEVER）
            val kpExamFrequency = kp.examFrequency ?: "NEVER"
            for (entity in entities) {
                val normalized = entity.normalized?.ifBlank { null } ?: entity.name
                if (normalized.isBlank()) continue
                val type = entity.type.ifBlank { "CONCEPT" }
                val key = EntityKey(normalized, type)
                val acc = entityMap[key]
                if (acc != null) {
                    acc.sourceKpIds.add(kp.id)
                    // 考频取最高（实体出现在多个知识点时，任一高频即该实体高频）
                    if (examFrequencyPriority(kpExamFrequency) > examFrequencyPriority(acc.examFrequency)) {
                        acc.examFrequency = kpExamFrequency
                    }
                } else {
                    entityMap[key] = EntityAccumulator(
                        name = entity.name,
                        type = type,
                        firstKpId = kp.id,
                        firstSubjectId = subjectId,
                        sourceKpIds = mutableListOf(kp.id),
                        examFrequency = kpExamFrequency,
                    )
                }
            }
        }

        // 生成节点并写入数据库
        var nodeCount = 0
        for ((_, acc) in entityMap) {
            val nodeId = generateAutoNodeId(acc.type, acc.name)
            val nodeType = mapEntityTypeToNodeType(acc.type)
            val color = getNodeColorByType(nodeType)
            val relatedPointId = if (acc.sourceKpIds.size == 1) acc.firstKpId else null
            val node = GraphNodeEntity(
                id = nodeId,
                type = nodeType.name,
                label = acc.name,
                subtitle = null,
                color = color,
                relatedPointId = relatedPointId,
                subjectId = acc.firstSubjectId,
                metadata = mapOf(
                    "sourceKpIds" to acc.sourceKpIds.joinToString(","),
                    "sourceKpCount" to acc.sourceKpIds.size.toString(),
                    "examFrequency" to acc.examFrequency,
                    "auto" to "true",
                ),
                prerequisites = null,
            )
            graphRepository.insertNode(node)
            nodeCount++
        }
        Log.i(TAG, "Auto graph: imported $nodeCount nodes from ${seedData.knowledgePoints.size} knowledge points")

        // ── 2. 关系去重建边 ──
        // 构建 normalized|type → nodeId 映射（用于边端点查找）
        val entityToNodeId = mutableMapOf<EntityKey, String>()
        for ((key, acc) in entityMap) {
            entityToNodeId[key] = generateAutoNodeId(acc.type, acc.name)
        }
        // 同时支持仅按 name 查找（关系中的 from/to 只有名字，无 type）
        val nameToNodeId = mutableMapOf<String, String>()
        for ((key, acc) in entityMap) {
            // 同名不同 type 时，优先 AUTHOR > WORK > MOVEMENT > CONCEPT > CHARACTER
            val priority = when (acc.type) {
                "AUTHOR" -> 0
                "WORK" -> 1
                "MOVEMENT" -> 2
                "SCHOOL" -> 3
                "CONCEPT" -> 4
                "CHARACTER" -> 5
                else -> 9
            }
            val existing = nameToNodeId[acc.name]
            if (existing == null) {
                nameToNodeId[acc.name] = entityToNodeId[key]!!
            }
        }

        // 收集所有关系并去重
        data class RelationKey(val from: String, val relation: String, val to: String)
        val relationSet = mutableSetOf<RelationKey>()
        for (kp in seedData.knowledgePoints) {
            val relations = kp.relations ?: continue
            for (rel in relations) {
                val from = rel.resolvedFrom ?: continue
                val to = rel.resolvedTo ?: continue
                val relation = rel.resolvedRelation ?: continue
                if (from.isBlank() || to.isBlank() || relation.isBlank()) continue
                relationSet.add(RelationKey(from, relation, to))
            }
        }

        // 生成边并写入数据库
        var edgeCount = 0
        var skippedCount = 0
        for (rel in relationSet) {
            val sourceId = nameToNodeId[rel.from]
            val targetId = nameToNodeId[rel.to]
            if (sourceId == null || targetId == null) {
                skippedCount++
                continue
            }
            val edgeType = mapRelationToEdgeType(rel.relation)
            val edge = GraphEdgeEntity(
                id = generateAutoEdgeId(rel.from, rel.relation, rel.to),
                sourceId = sourceId,
                targetId = targetId,
                type = edgeType.name,
                label = null,
            )
            graphRepository.insertEdge(edge)
            edgeCount++
        }
        Log.i(TAG, "Auto graph: imported $edgeCount edges (skipped $skippedCount with missing endpoints)")
    }

    /**
     * 生成自动节点的稳定 ID。
     *
     * ID 格式："auto-" + type 前缀 + normalized 的稳定哈希。
     * 稳定性保证：相同 (type, name) 总是生成相同 ID，支持 @Upsert 幂等。
     */
    private fun generateAutoNodeId(type: String, name: String): String {
        val typePrefix = when (type.uppercase()) {
            "AUTHOR" -> "au"
            "WORK" -> "wk"
            "CONCEPT" -> "cp"
            "CHARACTER" -> "ch"
            "MOVEMENT" -> "mv"
            "SCHOOL" -> "sc"
            else -> "ot"
        }
        val hash = name.hashCode().toString(16).let { if (it.startsWith("-")) "n" + it.substring(1) else it }
        return "auto-$typePrefix-$hash"
    }

    /**
     * 生成自动边的稳定 ID。
     */
    private fun generateAutoEdgeId(from: String, relation: String, to: String): String {
        val key = "$from|$relation|$to"
        val hash = key.hashCode().toString(16).let { if (it.startsWith("-")) "n" + it.substring(1) else it }
        return "auto-edge-$hash"
    }

    /** 将 seed entity.type 映射到 GraphNodeType 枚举 */
    private fun mapEntityTypeToNodeType(entityType: String): GraphNodeType {
        return when (entityType.uppercase()) {
            "AUTHOR" -> GraphNodeType.AUTHOR
            "WORK" -> GraphNodeType.WORK
            "MOVEMENT" -> GraphNodeType.MOVEMENT
            "SCHOOL" -> GraphNodeType.SCHOOL
            "CHARACTER" -> GraphNodeType.CONCEPT // 文学人物归入概念类
            "CONCEPT" -> GraphNodeType.CONCEPT
            "EVENT" -> GraphNodeType.CONCEPT
            "LOCATION" -> GraphNodeType.CONCEPT
            "PERIODICAL" -> GraphNodeType.WORK
            else -> GraphNodeType.CONCEPT
        }
    }

    /** 按 GraphNodeType 分配分类色（与 GraphSkeleton 一致） */
    private fun getNodeColorByType(type: GraphNodeType): Int {
        return when (type) {
            GraphNodeType.AUTHOR -> 0xFFE91E63.toInt()      // 粉
            GraphNodeType.WORK -> 0xFFFF9800.toInt()         // 橙
            GraphNodeType.SCHOOL -> 0xFF9C27B0.toInt()       // 紫
            GraphNodeType.MOVEMENT -> 0xFF9C27B0.toInt()     // 紫
            GraphNodeType.CONCEPT -> 0xFF2196F3.toInt()      // 蓝
            GraphNodeType.KNOWLEDGE_POINT -> 0xFF4CAF50.toInt() // 绿
        }
    }

    /** 将 seed relation 映射到 GraphEdgeType 枚举 */
    private fun mapRelationToEdgeType(relation: String): GraphEdgeType {
        return when (relation.uppercase()) {
            "AUTHORED" -> GraphEdgeType.AUTHORED
            "BELONGS_TO", "RELATED_TO", "MEMBER_OF", "INCLUDED_IN", "INCLUDES", "CONTAINS" -> GraphEdgeType.BELONGS_TO
            "PARTICIPATED_IN" -> GraphEdgeType.PARTICIPATED_IN
            "INFLUENCED_BY" -> GraphEdgeType.INFLUENCED_BY
            "CONTRASTED_WITH" -> GraphEdgeType.COMPARED_WITH
            "PROPOSED", "FOUNDED", "ESTABLISHED" -> GraphEdgeType.PARTICIPATED_IN
            "COMPILED", "EDITED", "TRANSLATED", "REVISED", "DIRECTED" -> GraphEdgeType.AUTHORED
            "SELECTED_INTO" -> GraphEdgeType.BELONGS_TO
            "REPRESENTS" -> GraphEdgeType.RELATED_CONCEPT
            "SUCCEEDS", "LEADS", "ORIGINATES" -> GraphEdgeType.PRECEDES
            else -> GraphEdgeType.RELATED_CONCEPT
        }
    }

    /**
     * 考频优先级权重（v0.7.9 新增）。
     *
     * 用于实体节点考频派生：一个实体出现在多个知识点时，取最高频。
     * HIGH(4) > MEDIUM(3) > LOW(2) > NEVER(1)。
     */
    private fun examFrequencyPriority(frequency: String): Int = when (frequency.uppercase()) {
        "HIGH" -> 4
        "MEDIUM" -> 3
        "LOW" -> 2
        else -> 1
    }

    companion object {
        private const val TAG = "SeedDataLoader"
        private const val SEED_DATA_FILE = "seed_data.json"
        // NF-DS7 修复：Key 命名统一为 XXX_KEY 后缀式，与 ThemeRepositoryImpl 一致。
        private val SEED_INITIALIZED_KEY = booleanPreferencesKey("seed_initialized")
        /** P1-AUDIT-4：种子版本号，用于版本感知升级 */
        private val SEED_VERSION_KEY = stringPreferencesKey("seed_version")
        /** v0.8.12 P1-1：图谱独立初始化标志，与主 seed_initialized 解耦 */
        private val SEED_GRAPH_INITIALIZED_KEY = booleanPreferencesKey("seed_graph_initialized")
        /** seed_data.json metadata.version 为空时的默认版本（视为首次安装） */
        private const val DEFAULT_SEED_VERSION = "v1"

        /** 科目排序顺序（按考研重要性排列：古代→现当代→外国→理论） */
        private val SUBJECT_ORDER = mapOf(
            "ancient" to 1,
            "modern" to 2,
            "foreign" to 3,
            "theory" to 4,
        )
    }
}

/**
 * 种子数据根结构，对应 assets/seed_data.json。
 *
 * 字段对齐 generate_seed.py 输出格式（8个字段）：
 * metadata / subjects / knowledge_points / exam_questions /
 * cards / writing_materials / graph_nodes / graph_edges
 *
 * 其中 cards / graph_nodes / graph_edges 由 App 侧动态生成或已在
 * [GraphSkeleton] 中预置，本类只解析前5个业务必需字段。
 */
@kotlinx.serialization.Serializable
data class SeedData(
    val metadata: SeedMetadata = SeedMetadata(),
    val subjects: List<SubjectSeed> = emptyList(),
    @SerialName("knowledge_points")
    val knowledgePoints: List<KnowledgePointSeed> = emptyList(),
    @SerialName("exam_questions")
    val examQuestions: List<ExamQuestionSeed> = emptyList(),
    @SerialName("writing_materials")
    val writingMaterials: List<WritingMaterialSeed> = emptyList(),
)

/** 种子数据元信息（版本/生成时间，仅记录用） */
@kotlinx.serialization.Serializable
data class SeedMetadata(
    val version: String = "",
    @SerialName("generated_at")
    val generatedAt: String = "",
    val description: String = "",
)

/** 科目种子数据（对应 SubjectEntity） */
@kotlinx.serialization.Serializable
data class SubjectSeed(
    val id: String,
    val name: String,
    val code: String,
)

/**
 * 知识点种子数据（对应 KnowledgePointEntity）。
 *
 * generate_seed.py 输出的字段用 subject（科目名）而非 chapter_id，
 * 导入时需按 subject 映射到对应默认章节。
 */
@kotlinx.serialization.Serializable
data class KnowledgePointSeed(
    val id: String,
    val title: String,
    val summary: String? = null,
    @SerialName("core_conclusion")
    val coreConclusion: String,
    @SerialName("full_content")
    val fullContent: String = "",
    /** 科目名：古代文学 / 现当代文学 / 外国文学 / 文学理论 */
    val subject: String,
    val tags: List<String>? = null,
    val difficulty: Int = 3,
    @SerialName("source_ref")
    val sourceRef: String? = null,
    val confidence: Double = 1.0,
    /** 学习文本（逐字校对的教材原文，导入到 KnowledgePointEntity.studyText） */
    @SerialName("study_text")
    val studyText: String? = null,
    /**
     * 考频（v0.7.9 修复：原 KnowledgePointSeed 未解析此字段，导致 importToDatabase 硬编码 NEVER，
     * seed_data.json 中 8 LOW + 3 MEDIUM 考频信息完全丢失）。
     *
     * generate_seed.py 根据真题内容派生：HIGH/MEDIUM/LOW/NEVER。
     * 导入到 KnowledgePointEntity.examFrequency，并作为图谱节点考频的数据源。
     */
    @SerialName("exam_frequency")
    val examFrequency: String? = null,
    /** 多维视角分析（不同教材来源的核心结论） */
    @SerialName("multi_perspectives")
    val multiPerspectives: List<MultiPerspectiveSeed>? = null,
    /**
     * 实体列表（v0.7.7 新增：知识图谱自动建图数据源）。
     *
     * generate_seed.py 已为 910 知识点 100% 提取实体（共 3336 条，去重 2123 个）：
     * - AUTHOR（678）：作家/学者
     * - WORK（783）：作品/著作
     * - CONCEPT（581）：文学概念/流派/体裁
     * - CHARACTER（63）：文学人物
     * - MOVEMENT（13）：文学运动/流派
     *
     * SeedDataLoader 用此字段自动生成 [GraphNodeEntity]（每个去重实体一个节点）。
     */
    val entities: List<EntitySeed>? = null,
    /**
     * 关系列表（v0.7.7 新增：知识图谱自动建图数据源）。
     *
     * generate_seed.py 已提取 968 条三元组关系：
     * - AUTHORED（708）：作家→作品
     * - PROPOSED（58）：提出者→理论
     * - COMPILED（16）：编者→作品
     * - PARTICIPATED_IN（5）/ FOUNDED（4）/ MEMBER_OF（3）等
     *
     * SeedDataLoader 用此字段自动生成 [GraphEdgeEntity]。
     */
    val relations: List<RelationSeed>? = null,
)

/**
 * 实体种子数据（v0.7.7 新增）。
 *
 * 对应 seed_data.json knowledge_points[].entities[] 数组元素。
 * 示例：{"name":"鲁迅","type":"AUTHOR","normalized":"鲁迅"}
 */
@kotlinx.serialization.Serializable
data class EntitySeed(
    val name: String,
    val type: String = "CONCEPT",
    /** 规范化名称（用于去重，缺失时回退到 name） */
    val normalized: String? = null,
)

/**
 * 关系种子数据（v0.7.7 新增）。
 *
 * 对应 seed_data.json knowledge_points[].relations[] 数组元素。
 * 支持两种结构（generate_seed.py 历史版本兼容）：
 * - {from, relation, to}：主结构（965 条）
 * - {type, from, to}：旧结构（83 条，type 字段值如 AUTHORED/RELATED_TO）
 * - {subject, predicate, object, metadata}：SPO 结构（3 条，少数）
 *
 * 统一解析为 from/relation/to 三字段：relation 取 `relation` 或 `type` 字段。
 */
@kotlinx.serialization.Serializable
data class RelationSeed(
    val from: String? = null,
    val relation: String? = null,
    val to: String? = null,
    /** 旧结构字段（与 relation 互斥，优先用 relation） */
    val type: String? = null,
    /** SPO 结构字段（subject/predicate/object，优先级最低） */
    val subject: String? = null,
    val predicate: String? = null,
    val `object`: String? = null,
) {
    /** 统一获取关系类型（relation > type > predicate） */
    val resolvedRelation: String?
        get() = relation ?: type ?: predicate

    /** 统一获取起点（from > subject） */
    val resolvedFrom: String?
        get() = from ?: subject

    /** 统一获取终点（to > object） */
    val resolvedTo: String?
        get() = to ?: `object`
}

/** 多维视角单条数据（对应 seed_data.json 的 multi_perspectives 数组元素） */
@kotlinx.serialization.Serializable
data class MultiPerspectiveSeed(
    val source: String = "",
    @SerialName("core_conclusion")
    val coreConclusion: String = "",
    @SerialName("full_content")
    val fullContent: String = "",
    @SerialName("source_file")
    val sourceFile: String? = null,
    @SerialName("is_main")
    val isMain: Boolean = false,
    @SerialName("is_conclusion_base")
    val isConclusionBase: Boolean = false,
)

/**
 * 将多维视角列表转为 Map<source, coreConclusion>。
 *
 * Entity.multiPerspectives 声明为 Map<String, String>?，
 * 而 seed_data.json 中是 List<{source, core_conclusion, ...}>。
 * 此扩展函数完成类型转换：
 * - key = source（若为空或"其他"，用"视角N"占位）
 * - value = coreConclusion（若为空，取 fullContent 前 200 字符）
 *
 * 重复的 source 会追加序号后缀（如"马工程(2)"）避免 key 覆盖。
 */
private fun List<MultiPerspectiveSeed>.toPerspectiveMap(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    var fallbackIndex = 1
    for (p in this) {
        val conclusion = p.coreConclusion.ifBlank {
            p.fullContent.take(200)
        }.ifBlank { continue }
        val rawSource = p.source.ifBlank { "其他" }
        // 重复 source 追加序号
        val key = if (result.containsKey(rawSource)) {
            "$rawSource(${result.keys.count { it.startsWith(rawSource) } + 1})"
        } else {
            rawSource
        }
        result[key] = conclusion
        fallbackIndex++
    }
    return result.ifEmpty { mapOf("视角1" to this.firstOrNull()?.coreConclusion.orEmpty()) }
        .filterValues { it.isNotBlank() }
}

/**
 * 真题种子数据（对应 ExamQuestionEntity）。
 *
 * generate_seed.py 用 subject（科目名）而非 subject_id，
 * 导入时需按 subject 映射到对应 subject_id。
 */
@kotlinx.serialization.Serializable
data class ExamQuestionSeed(
    val id: String,
    val year: Int,
    /** 科目名：古代文学 / 现当代文学 / 外国文学 / 文学理论 */
    val subject: String,
    @SerialName("question_type")
    val questionType: String,
    val content: String,
    val score: Int = 0,
    @SerialName("exam_paper_code")
    val examPaperCode: String? = null,
    @SerialName("answer_framework")
    val answerFramework: String? = null,
)

/** 写作素材种子数据（对应 WritingMaterialEntity） */
@kotlinx.serialization.Serializable
data class WritingMaterialSeed(
    val id: String,
    val category: String,
    @SerialName("sub_category")
    val subCategory: String? = null,
    val content: String,
    val source: String? = null,
    val tags: String? = null,
)
