package com.wenyan.app.core.data.seed

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import com.wenyan.app.core.common.model.ContentSource
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.dao.ChapterDao
import com.wenyan.app.core.database.dao.DataSourceDao
import com.wenyan.app.core.database.dao.ExamCodeHistoryDao
import com.wenyan.app.core.database.dao.ExamQuestionDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.dao.SubjectDao
import com.wenyan.app.core.database.dao.WritingMaterialDao
import com.wenyan.app.core.database.entity.ChapterEntity
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import com.wenyan.app.core.database.entity.WritingMaterialEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** 提升此值可在 seed 内容版本不变时触发一次安全重导。 */
internal const val CURRENT_SEED_IMPORT_SCHEMA_VERSION = 3

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
 * P0-D2 修正：导入过程用 [WenyanDatabase.withTransaction] 包裹，确保全部导入步骤原子性。
 * 原实现无事务包裹，中途失败会留下半成品数据 + DataStore 已写"initialized" → 永久半成品。
 *
 * NF-J 修复（种子加载链路稳健性）：
 * - 合并双 DataStore：原先用独立的 `wenyan_seed_prefs` DataStore 记录初始化标志，
 *   与主 `wenyan_preferences` 分离，导致全局偏好分散在两处。现统一注入 Hilt 单例
 *   [DataStore]<[Preferences]>，与 [com.wenyan.app.core.data.di.DataStoreModule] 提供的
 *   主 DataStore 共用一个文件（wenyan_preferences.preferences_pb）。
 * - [isInitialized] / [storeSeedState] 加 [IOException] 兜底：DataStore 读写在磁盘
 *   故障/权限异常时抛 IOException，原实现会冒泡到 Application 的 CoroutineExceptionHandler
 *   导致种子加载被吞掉、下次启动仍报错。现 [isInitialized] 异常时假设"未初始化"
 *   让种子重试导入，[storeSeedState] 异常时仅 Log.w 不冒泡（App 继续工作，下次启动重试，
 *   内容表 Upsert 幂等，MemoRecord 按数据库已有 ID 保留）。
 * - 不修改 DAO 的 `@Insert(onConflict = REPLACE)` 策略：种子加载只在
 *   `isInitialized() == false` 时执行（首次安装），事务包裹确保原子性，首次安装
 *   用户无数据可被覆盖。
 *
 * P1-AUDIT-4 修复：版本感知种子升级。原实现只用 boolean `seed_initialized` 标志，
 * 更新 seed_data.json 后用户不会获得新内容（标志仍为 true）。现改为：
 * - 存储 seed_data.json 的 metadata.version 到 DataStore
 * - 独立存储导入器 schema，字段消费逻辑变化时无需伪造 seed 内容版本
 * - 启动时比对内容版本与导入 schema；任一不一致时重新导入内容表
 *   （subjects/chapters/knowledge_points/exam_questions/writing_materials），用 @Upsert 安全更新内容
 * - **保护 MemoRecord**：升级时跳过已有 MemoRecord 的知识点（保留用户 FSRS 学习进度），
 *   仅为新增知识点创建初始 MemoRecord
 */
@Singleton
class SeedDataLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: DataStore<Preferences>,
    private val database: WenyanDatabase,
    private val examCodeHistoryDao: ExamCodeHistoryDao,
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val dataSourceDao: DataSourceDao,
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
     * v0.9.37 P0-1 优化：已初始化时**先轻量读种子版本**（[parseSeedVersionFromJson]
     * 只解析 metadata 壳，不构建 960 个实体对象），版本命中则完全跳过全量
     * 反序列化 5.3MB seed_data.json——原实现每次冷启动都全量解析，仅为了比对
     * 版本号，浪费 IO/CPU。首次安装/升级路径保持单次全量解析（不重复）。
     *
     * NF-J 修复：[isInitialized] 与 [storeSeedState] 都已加 IOException 兜底，
     * 此方法自身不会再因 DataStore 故障抛 IOException；[readSeedDataFromAssets]
     * 与 [importToDatabase] 的异常会冒泡到 Application 的 CoroutineExceptionHandler
     * 被吞掉（Log.e），下次启动重试。
     */
    suspend fun ensureSeedDataLoaded() {
        val initialized = isInitialized()

        if (initialized) {
            val storedVersion = getStoredSeedVersion()
            val storedImportSchemaVersion = getStoredImportSchemaVersion()
            // v0.9.37 P0-1：命中路径只做轻量版本解析，跳过 5.3MB 全量反序列化
            val currentVersion = readSeedVersionFromAssets().ifBlank { DEFAULT_SEED_VERSION }
            val importRequired = shouldImportSeedData(
                initialized = true,
                storedContentVersion = storedVersion,
                currentContentVersion = currentVersion,
                storedImportSchemaVersion = storedImportSchemaVersion,
            )
            if (!importRequired) return
            Timber.i(
                "Seed import upgrade: content $storedVersion → $currentVersion, " +
                    "schema $storedImportSchemaVersion → $CURRENT_SEED_IMPORT_SCHEMA_VERSION " +
                    "(MemoRecord preserved)",
            )
            val seedData = readSeedDataFromAssets()
            importToDatabase(seedData, isUpgrade = true)
            storeSeedState(currentVersion)
            return
        }

        // 首次安装：全量导入（含 MemoRecord）
        val seedData = readSeedDataFromAssets()
        val currentVersion = seedData.metadata.version.ifBlank { DEFAULT_SEED_VERSION }
        importToDatabase(seedData, isUpgrade = false)
        storeSeedState(currentVersion)
    }

    /**
     * 读取 DataStore 中是否已初始化标志。
     *
     * NF-J 修复：DataStore 读写可能抛 [IOException]（磁盘满/权限/文件损坏），
     * 兜底返回 false 让种子重试导入（[importToDatabase] 用事务包裹 + Upsert，
     * MemoRecord 会查询已有 ID，重复导入不会重置学习进度）。
     */
    private suspend fun isInitialized(): Boolean = try {
        preferencesDataStore.data.map { it[SEED_INITIALIZED_KEY] ?: false }.first()
    } catch (e: IOException) {
        Timber.w(e, "DataStore read failed, assuming seed not initialized")
        false
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
        Timber.w(e, "DataStore read failed for seed version, assuming empty")
        ""
    }

    /**
     * 读取种子导入器 schema 版本。
     *
     * 它与内容版本分离：当 JSON 内容不变、但 App 开始消费此前已存在的字段（例如
     * textbook_sources）时，只提升导入 schema 即可让老用户安全重导，无需伪造内容版本。
     */
    private suspend fun getStoredImportSchemaVersion(): Int = try {
        preferencesDataStore.data.map { it[SEED_IMPORT_SCHEMA_VERSION_KEY] ?: 0 }.first()
    } catch (e: IOException) {
        Timber.w(e, "DataStore read failed for seed import schema, assuming 0")
        0
    }

    /**
     * 原子保存“已初始化 + 种子内容版本 + 导入 schema”。
     *
     * 三个字段必须在同一次 DataStore edit 中提交，避免进程在多次写入之间终止后出现
     * initialized=true 但版本/schema 未更新的撕裂状态。
     * 写失败时数据库事务已经成功；下次启动可安全重导，MemoRecord 会按数据库事实保留。
     */
    private suspend fun storeSeedState(version: String) {
        try {
            preferencesDataStore.edit {
                it[SEED_INITIALIZED_KEY] = true
                it[SEED_VERSION_KEY] = version
                it[SEED_IMPORT_SCHEMA_VERSION_KEY] = CURRENT_SEED_IMPORT_SCHEMA_VERSION
            }
        } catch (e: IOException) {
            Timber.w(e, "DataStore write failed for seed state: $version; safe re-import will run next launch")
        }
    }

    // 从 assets 读取并解析 seed_data.json
    private fun readSeedDataFromAssets(): SeedData {
        val jsonStr = context.assets.open(SEED_DATA_FILE).bufferedReader().use { it.readText() }
        return json.decodeFromString(SeedData.serializer(), jsonStr)
    }

    /**
     * 轻量读取种子版本（v0.9.37 P0-1）。
     *
     * 只解析 JSON 顶层的 metadata 壳（[SeedVersionShell]），**不构建**
     * subjects/knowledgePoints/examQuestions/writingMaterials 共 960+ 实体对象。
     * 用于 [ensureSeedDataLoaded] 的版本命中判断，避免每次冷启动全量反序列化。
     */
    private fun readSeedVersionFromAssets(): String {
        val jsonStr = context.assets.open(SEED_DATA_FILE).bufferedReader().use { it.readText() }
        return parseSeedVersionFromJson(jsonStr)
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
     * P0-D2 修正：整个导入过程用 [database.withTransaction] 包裹，确保全部步骤原子性。
     * 任何一步失败将回滚全部已插入数据，且 storeSeedState() 不会被调用（在事务外），
     * 下次启动会重新尝试导入，避免留下"半成品数据 + initialized=true"的永久不一致。
     *
     * P1-AUDIT-4 修复：[isUpgrade] 参数控制 MemoRecord 导入策略。
     * 无论 DataStore 判断为首次安装还是升级，都查询已有 MemoRecord 的 point_id，仅为缺失
     * 知识点创建记录，**不覆盖用户 FSRS 学习进度**（stability/difficulty/nextReviewAt）。
     */
    private suspend fun importToDatabase(seedData: SeedData, isUpgrade: Boolean = false) {
        val frameworkErrors = KnowledgeFrameworkRegistry.definitions.flatMap { framework ->
            val pointIds = seedData.knowledgePoints
                .filter { it.subject == framework.subjectName }
                .map { it.id }
                .toSet()
            if (pointIds.isEmpty()) {
                emptyList()
            } else {
                framework.validate(pointIds).map { "${framework.subjectName}: $it" }
            }
        }
        check(frameworkErrors.isEmpty()) {
            "知识框架校验失败: ${frameworkErrors.joinToString("；")}"
        }

        database.withTransaction {
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

        // 步骤2（ADR-001 B1.3 章节树）：为每科创建根章节 + 已审核的显式框架
        // 根章节：parentId=null，title=科目名（不再叫"默认章节"，作为章节树根）
        // 显式框架可以有多级节点；未来新增且尚未注册框架的科目暂使用 PERIOD_CHAPTERS 兼容分组。
        val allChapters = mutableListOf<ChapterEntity>()
        // subjectName → 根章节ID 映射（供知识点兜底分配）
        val subjectNameToRootChapterId = mutableMapOf<String, String>()

        for (seed in seedData.subjects) {
            val rootChapterId = "chapter_default_${seed.code}"
            subjectNameToRootChapterId[seed.name] = rootChapterId
            allChapters.add(
                ChapterEntity(
                    id = rootChapterId,
                    subjectId = seed.id,
                    parentId = null,
                    title = seed.name,
                    sortOrder = 0,
                ),
            )
            val framework = KnowledgeFrameworkRegistry.find(seed.code, seed.name)
            if (framework != null) {
                // 已完成审核的科目使用显式多级框架，不再依赖标题关键词猜测。
                allChapters += framework.nodes.map { node ->
                    ChapterEntity(
                        id = node.id,
                        subjectId = seed.id,
                        parentId = node.parentId ?: rootChapterId,
                        title = node.title,
                        sortOrder = node.sortOrder,
                    )
                }
            } else {
                // 尚未注册显式框架的科目保留原有时段规则，避免提前改变数据语义。
                val periods = PERIOD_CHAPTERS[seed.name] ?: emptyList()
                for ((idx, period) in periods.withIndex()) {
                    allChapters.add(
                        ChapterEntity(
                            id = "chapter_${seed.code}_$idx",
                            subjectId = seed.id,
                            parentId = rootChapterId,
                            title = period.title,
                            sortOrder = idx + 1,
                        ),
                    )
                }
            }
        }
        if (allChapters.isNotEmpty()) {
            chapterDao.insertAll(allChapters)
        }

        // 构建 subjectName → chapterId 映射（兼容旧逻辑，指向根章节）
        val subjectNameToChapterId = subjectNameToRootChapterId.toMap()

        // 步骤3：导入知识点（已注册科目使用显式归属，未来新增科目按兼容规则兜底）
        // 注意：若知识点 subject 不在 subjects 列表中，跳过该知识点（避免外键约束失败）

        // v0.9.1 修复：基于 tags 派生知识点间关联关系（relatedIds）。
        // 根因：B2 增强了关联模块 UI，但此处原硬编码 relatedIds=null，导致
        // KnowledgeRepository.observeKnowledgePointDetail 短路返回空列表，
        // UI RelatedPointsSection 永远不渲染（用户报告"关联知识点模块找不见"）。
        // 派生规则：同 subject + 共享 ≥1 tag → RELATED，按共享 tag 数降序取前 5。
        val relatedIdsMap = computeRelatedIdsByTags(seedData.knowledgePoints)

        val knowledgePointEntities = seedData.knowledgePoints.mapNotNull { seed ->
            val rootChapterId = subjectNameToChapterId[seed.subject]
                ?: return@mapNotNull null
            val subjectSeed = seedData.subjects.first { it.name == seed.subject }
            // 已注册科目必须命中显式框架；未注册科目才允许使用兼容规则.
            val framework = KnowledgeFrameworkRegistry.find(subjectSeed.code, seed.subject)
            val chapterId = if (framework != null) {
                framework.assignments[seed.id]
                    ?: error("${framework.subjectName}知识点 ${seed.id} 没有框架归属")
            } else {
                val periodChapterId = matchPeriodChapter(
                    subjectName = seed.subject,
                    subjectCode = subjectSeed.code,
                    title = seed.title,
                    tags = seed.tags,
                )
                periodChapterId ?: rootChapterId
            }
            KnowledgePointEntity(
                id = seed.id,
                chapterId = chapterId,
                title = seed.title,
                summary = seed.summary,
                coreConclusion = seed.coreConclusion,
                fullContent = seed.fullContent.ifBlank { seed.coreConclusion },
                multiPerspectives = seed.multiPerspectives?.toPerspectiveMap(),
                relatedIds = relatedIdsMap[seed.id],
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
                contentSource = seed.contentSourceCode(),
                ocrStatus = "VERIFIED",
                sourceFile = seed.resolvedSourceFiles().joinToString("；").takeIf { it.isNotBlank() },
                sourcePage = null,
                studyText = seed.studyText,
            )
        }
        if (knowledgePointEntities.isNotEmpty()) {
            knowledgePointDao.insertAll(knowledgePointEntities)
        }

        // v3 框架升级：已审核科目的旧版“时段”节点不再作为主框架。仅删除已经没有
        // 知识点引用的旧节点；若存在用户自行创建的知识点，则保留节点，避免外键级联
        // 删除用户内容。MemoRecord 只引用知识点 ID，不因章节重归类而改变。
        KnowledgeFrameworkRegistry.definitions
            .flatMap { it.legacyChapterIds }
            .distinct()
            .forEach { legacyChapterId ->
                if (knowledgePointDao.countByChapter(legacyChapterId) == 0) {
                    chapterDao.deleteById(legacyChapterId)
                }
            }

        // 步骤3.1：导入可追溯教材来源。旧 seed 常用“其他”占位，这种值不写入来源表，
        // 防止 UI/AI 把占位文本包装成精确引用。App 管理的来源先删后建，种子升级时
        // 已移除或改名的教材不会残留；非 seed 前缀的用户来源不受影响。
        dataSourceDao.deleteManagedKnowledgePointSources()
        val importedKnowledgePointIds = knowledgePointEntities.mapTo(mutableSetOf()) { it.id }
        val dataSourceEntities = buildSeedDataSourceEntities(
            seeds = seedData.knowledgePoints,
            importedKnowledgePointIds = importedKnowledgePointIds,
            createdAt = now,
        )
        if (dataSourceEntities.isNotEmpty()) {
            dataSourceDao.insertAll(dataSourceEntities)
        }
        val declaredConflictCount = seedData.knowledgePoints.count { it.conflictFlag }
        val verifiableConflictCount = seedData.knowledgePoints.count { it.hasVerifiableTextbookConflict() }
        if (verifiableConflictCount > 0) {
            Timber.i("Seed import: marked $verifiableConflictCount knowledge points as verifiable textbook conflicts")
        }
        val unverifiedConflictCount = declaredConflictCount - verifiableConflictCount
        if (unverifiedConflictCount > 0) {
            Timber.w(
                "Seed import: ignored $unverifiedConflictCount conflict flags " +
                    "without at least two traceable textbook sources",
            )
        }

        // 步骤4：为每个知识点创建初始 MemoRecord（state=NEW，等待每日新卡选择）
        // P1-AUDIT-4 修复：升级时跳过已有 MemoRecord 的知识点，保留用户 FSRS 学习进度。
        // 仅为新增知识点（种子更新后新加的）创建初始 MemoRecord。
        // 数据库才是进度是否存在的事实来源。不能依赖 DataStore 的 isUpgrade：若数据库事务
        // 已成功但 seed 状态写入失败，下次启动会再次被视为“首次安装”；旧逻辑会为空集并
        // Upsert 全部初始记录，覆盖用户在两次启动之间产生的 FSRS 进度。
        val existingMemoPointIds = memoRecordDao.getExistingPointIds().toSet()
        val memoRecords = knowledgePointEntities
            .missingMemoRecords(existingMemoPointIds)
            .map { kp ->
                MemoRecordEntity(
                    pointId = kp.id,
                    state = "NEW",
                    stability = 0f,
                    difficulty = 5f,
                    // P2-AUDIT-1 修正：lastReviewAt = 0L 表示"从未复习"，原为 now 语义错误
                    lastReviewAt = 0L,
                    // 保留可立即开始的时间戳；ReviewRepository/DAO 会按 pristine NEW 语义
                    // 将其交给每日新卡限额处理，而不是归入已学习卡的到期复习队列。
                    nextReviewAt = now,
                    reviewCount = 0,
                    failCount = 0,
                    inPriorityQueue = 0,
                )
            }
        if (memoRecords.isNotEmpty()) {
            memoRecordDao.insertAll(memoRecords)
        }
        if (isUpgrade || existingMemoPointIds.isNotEmpty()) {
            Timber.i("Seed import: created ${memoRecords.size} new MemoRecords, preserved ${existingMemoPointIds.size} existing")
        }

        // 步骤5：导入真题（按 subject 字段映射到 subjectId）
        // 注意：若真题 subject 不在 subjects 列表中，跳过该真题（避免外键约束失败）
        //
        // v0.9.8 论述题板块：派生 related_point_ids（参考 computeRelatedIdsByTags 模式），
        // seed 中已手工标注的（如示例题 eq_0038/eq_0182/eq_0254）优先使用 seed 值，
        // 其余论述题由 computeExamQuestionRelatedPoints 派生。
        // angle/notes 字段透传（seed 中填充的示例题保留，未填充的为 null）。
        val essayRelatedPointsMap = computeExamQuestionRelatedPoints(
            essays = seedData.examQuestions.filter { it.questionType == "ESSAY" },
            knowledgePoints = seedData.knowledgePoints,
        )
        val examQuestionEntities = seedData.examQuestions.mapNotNull { seed ->
            val subjectId = subjectNameToId[seed.subject]
                ?: return@mapNotNull null
            // 优先用 seed 中手工标注的 relatedPointIds；否则用派生的（仅论述题有派生）
            val relatedPointIds = seed.relatedPointIds
                ?: essayRelatedPointsMap[seed.id]
            ExamQuestionEntity(
                id = seed.id,
                year = seed.year,
                subjectId = subjectId,
                questionType = seed.questionType,
                content = seed.content,
                score = seed.score,
                angle = seed.angle,
                relatedPointIds = relatedPointIds,
                answerFramework = seed.answerFramework,
                notes = seed.notes,
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

        // 步骤7：保留科目代码历史
        examCodeHistoryDao.insertAll(ExamCodeHistoryData.EXAM_CODE_HISTORY)
        }
    }

    companion object {
        private const val SEED_DATA_FILE = "seed_data.json"
        // NF-DS7 修复：Key 命名统一为 XXX_KEY 后缀式，与 ThemeRepositoryImpl 一致。
        private val SEED_INITIALIZED_KEY = booleanPreferencesKey("seed_initialized")
        /** P1-AUDIT-4：种子版本号，用于版本感知升级 */
        private val SEED_VERSION_KEY = stringPreferencesKey("seed_version")
        /** 导入器 schema：字段消费逻辑变化时触发重导，但不冒充 seed 内容版本。 */
        private val SEED_IMPORT_SCHEMA_VERSION_KEY = intPreferencesKey("seed_import_schema_version")
        /** seed_data.json metadata.version 为空时的默认版本（视为首次安装） */
        private const val DEFAULT_SEED_VERSION = "v1"

        /** 每个知识点最多关联的知识点数（v0.9.1：避免 UI 列表过长，取共享 tag 最多的前 5 个） */
        private const val MAX_RELATED_POINTS = 5

        /** 科目排序顺序（按考研重要性排列：古代→现当代→外国→理论） */
        private val SUBJECT_ORDER = mapOf(
            "ancient" to 1,
            "modern" to 2,
            "foreign" to 3,
            "theory" to 4,
        )

        /**
         * 章节树预定义时段（ADR-001 B1.3）。
         *
         * 为每个科目定义文学史标准分期，用于从 tags/title 自动分类知识点到章节树。
         * 匹配规则：知识点的 tags + title 任一包含某时段的关键词，则归入该时段子章节。
         * 未匹配的知识点保留在根章节（科目级"全部"）。
         *
         * 时段定义基于南师大文学院现当代文学考研（050106）教材结构。
         *
         * 可见性为 internal 以便 [SeedDataLoaderTest] 直接验证匹配逻辑，
         * 无需通过 assets 加载完整 seed_data.json（library 模块测试无法访问 app 模块 assets）。
         */
        internal val PERIOD_CHAPTERS: Map<String, List<PeriodChapter>> = mapOf(
            "中国古代文学" to listOf(
                PeriodChapter("先秦文学", listOf("先秦", "诗经", "楚辞", "诸子", "左传", "国语", "战国策", "屈原", "离骚")),
                PeriodChapter("秦汉文学", listOf("秦汉", "汉赋", "史记", "班固", "乐府", "汉乐府")),
                PeriodChapter("魏晋南北朝文学", listOf("魏晋", "南北朝", "建安", "三曹", "陶渊明", "谢灵运", "志怪", "世说新语", "古诗十九首")),
                PeriodChapter("隋唐五代文学", listOf("隋唐", "唐代", "唐诗", "五代", "李白", "杜甫", "白居易", "韩愈", "古文运动", "传奇")),
                PeriodChapter("宋辽金文学", listOf("宋代", "宋词", "辽金", "苏轼", "李清照", "辛弃疾", "陆游", "话本")),
                PeriodChapter("元代文学", listOf("元代", "元杂剧", "元曲", "关汉卿", "王实甫", "散曲", "西厢记")),
                PeriodChapter("明代文学", listOf("明代", "三国演义", "水浒传", "西游记", "牡丹亭", "拟话本", "公安派", "前后七子")),
                PeriodChapter("清代文学", listOf("清代", "红楼梦", "聊斋", "儒林外史", "桐城派", "龚自珍", "纳兰")),
            ),
            "中国现当代文学" to listOf(
                PeriodChapter("五四文学革命", listOf("五四", "文学革命", "新文化", "胡适", "陈独秀", "狂人日记")),
                PeriodChapter("二十年代文学", listOf("文学研究会", "创造社", "语丝", "新月", "周作人", "郁达夫", "沉沦")),
                PeriodChapter("三十年代文学", listOf("左翼", "茅盾", "巴金", "老舍", "曹禺", "新月派", "雷雨", "骆驼祥子", "家", "子夜")),
                PeriodChapter("四十年代文学", listOf("抗战", "解放区", "赵树理", "孙犁", "艾青", "穆旦", "九叶", "围城")),
                PeriodChapter("十七年文学", listOf("十七年", "50年代", "60年代", "柳青", "杨沫", "郭小川", "创业史", "青春之歌")),
                PeriodChapter("新时期文学", listOf("新时期", "80年代", "寻根", "先锋", "朦胧诗", "王蒙", "贾平凹", "莫言", "红高粱", "伤痕", "反思")),
                PeriodChapter("九十年代以来", listOf("90年代", "新写实", "网络文学", "余华", "阎连科", "私人化", "身体写作")),
            ),
            "外国文学" to listOf(
                PeriodChapter("古代文学", listOf("古希腊", "罗马", "荷马", "伊利亚特", "奥德赛", "悲剧", "埃斯库罗斯", "索福克勒斯")),
                PeriodChapter("中世纪文学", listOf("中世纪", "但丁", "神曲", "骑士", "英雄史诗")),
                PeriodChapter("文艺复兴时期", listOf("文艺复兴", "莎士比亚", "塞万提斯", "堂吉诃德", "哈姆雷特", "十日谈")),
                PeriodChapter("17世纪文学", listOf("17世纪", "古典主义", "莫里哀", "弥尔顿", "失乐园")),
                PeriodChapter("18世纪文学", listOf("18世纪", "启蒙", "歌德", "卢梭", "菲尔丁", "少年维特", "浮士德")),
                PeriodChapter("19世纪浪漫主义", listOf("浪漫主义", "雨果", "拜伦", "雪莱", "普希金", "巴黎圣母院", "悲惨世界")),
                PeriodChapter("19世纪现实主义", listOf("现实主义", "巴尔扎克", "托尔斯泰", "陀思妥耶夫斯基", "狄更斯", "安娜", "战争与和平", "罪与罚", "红与黑")),
                PeriodChapter("20世纪现代主义", listOf("现代主义", "卡夫卡", "意识流", "乔伊斯", "福克纳", "存在主义", "荒诞", "变形记", "等待戈多", "百年孤独")),
            ),
            "文学理论" to listOf(
                PeriodChapter("文学本质论", listOf("本质", "意识形态", "审美", "反映论", "社会性", "认识论")),
                PeriodChapter("文学创作论", listOf("创作", "灵感", "想象", "构思", "题材", "作家", "天才")),
                PeriodChapter("文学作品论", listOf("作品", "文本", "叙事", "结构", "意境", "典型", "情节", "语言", "风格")),
                PeriodChapter("文学接受论", listOf("接受", "读者", "阐释", "期待视野", "接受美学", "阅读")),
                PeriodChapter("文学批评", listOf("批评", "批评学", "流派", "思潮", "韦勒克", "卡勒", "新批评", "结构主义")),
                PeriodChapter("文学发展论", listOf("发展", "起源", "演变", "继承", "革新", "文学史")),
            ),
        )

        /**
         * 章节树时段定义（ADR-001 B1.3）。
         *
         * @property title 时段名称（用作章节标题）
         * @property keywords 匹配关键词列表（知识点 tags/title 包含任一关键词则归入此时段）
         */
        internal data class PeriodChapter(
            val title: String,
            val keywords: List<String>,
        )

        /**
         * 为知识点匹配最合适的时段子章节 ID。
         *
         * 匹配规则：遍历 [PERIOD_CHAPTERS] 中该科目的时段，知识点 tags + title 任一包含某时段关键词，
         * 则返回该时段子章节 ID。多个匹配取第一个。无匹配返回 null（留根章节）。
         *
         * 放在 companion object 中以便单元测试直接调用（[SeedDataLoaderTest]），
         * 无需构造完整实例（Context/DB/DAO 等依赖）。
         *
         * @param subjectName 科目名（如"中国古代文学"）
         * @param subjectCode 科目代码（如"ancient"）
         * @param title 知识点标题
         * @param tags 知识点标签列表（可为空）
         * @return 子章节 ID（如"chapter_ancient_0"），或 null 表示留根章节
         */
        internal fun matchPeriodChapter(
            subjectName: String,
            subjectCode: String,
            title: String,
            tags: List<String>?,
        ): String? {
            val periods = PERIOD_CHAPTERS[subjectName] ?: return null
            // 合并 title + tags 作为待匹配文本
            val haystacks = buildList {
                add(title)
                tags?.let { addAll(it) }
            }
            for ((idx, period) in periods.withIndex()) {
                for (keyword in period.keywords) {
                    if (haystacks.any { it.contains(keyword) }) {
                        return "chapter_${subjectCode}_$idx"
                    }
                }
            }
            return null
        }

        /**
         * 基于 tags 派生知识点间关联关系（v0.9.1 修复：关联知识点模块不可见）。
         *
         * 根因：B2 增强了关联模块 UI（RelationshipType 视觉编码 + RelatedPointItem 信息密度），
         * 但 [importToDatabase] 步骤3 原硬编码 `relatedIds = null`，导致
         * [com.wenyan.app.core.data.repository.KnowledgeRepository.observeKnowledgePointDetail]
         * 短路返回空列表，UI RelatedPointsSection 永远不渲染。
         *
         * 派生规则：
         * - 同 subject + 共享 ≥1 tag → RELATED（关联）
         * - 按共享 tag 数降序（共享越多越关联），id 升序（稳定排序），取前 [MAX_RELATED_POINTS] 个
         * - 无 tags 或无共享 tag 的 KP，不出现在结果中（relatedIds 保持 null，UI 不渲染该区块）
         *
         * 不派生 CONTRAST/EXTENSION：
         * - CONTRAST 需要语义分析（如"现实主义" vs "浪漫主义"），tags 重叠反而表示关联而非对比
         * - EXTENSION 需要跨科目知识图谱，当前 seed 数据无此信息
         * - 未来可由 AI 管线（LLM 从 full_content 派生）或手动标注补充
         *
         * 放在 companion object 中以便单元测试直接调用（与 [matchPeriodChapter] 模式一致）。
         *
         * 复杂度：O(n²) 每个 subject 内，n_max=460（中国古代文学），约 21 万次比较，可接受。
         *
         * @param seeds 种子知识点列表
         * @return KP id → relatedIds 映射（仅包含有关联的 KP）
         */
        internal fun computeRelatedIdsByTags(
            seeds: List<KnowledgePointSeed>,
        ): Map<String, List<String>> {
            val bySubject = seeds.groupBy { it.subject }
            val result = mutableMapOf<String, List<String>>()

            for ((_, subjectSeeds) in bySubject) {
                // 构建 tag → [KP ids] 倒排索引
                val tagIndex = mutableMapOf<String, MutableList<String>>()
                for (seed in subjectSeeds) {
                    for (tag in seed.tags.orEmpty()) {
                        tagIndex.getOrPut(tag) { mutableListOf() }.add(seed.id)
                    }
                }

                // 对每个 KP，统计与其他 KP 的共享 tag 数
                for (seed in subjectSeeds) {
                    val myTags = seed.tags.orEmpty()
                    if (myTags.isEmpty()) continue

                    val sharedCounts = mutableMapOf<String, Int>()
                    for (tag in myTags) {
                        for (otherId in tagIndex[tag].orEmpty()) {
                            if (otherId != seed.id) {
                                sharedCounts[otherId] = (sharedCounts[otherId] ?: 0) + 1
                            }
                        }
                    }

                    // 按共享 tag 数降序，id 升序（稳定排序），取前 N
                    val related = sharedCounts.entries
                        .sortedWith(
                            compareByDescending<Map.Entry<String, Int>> { it.value }
                                .thenBy { it.key },
                        )
                        .take(MAX_RELATED_POINTS)
                        .map { it.key }

                    if (related.isNotEmpty()) {
                        result[seed.id] = related
                    }
                }
            }

            return result
        }

        /**
         * 派生论述题→知识点关联（v0.9.8 新增，对应设计文档 3.5 节）。
         *
         * 算法：对每道论述题，扫描所有同科目知识点的 `title + tags`，
         * 若知识点标题或任一 tag 在论述题 `content + answerFramework` 中出现 → 建立关联，
         * 按匹配数降序取前 [MAX_RELATED_POINTS]（5）个。
         *
         * 与 [computeRelatedIdsByTags] 的区别：
         * - 知识点→知识点关联：基于共享 tag 计数（双向对称）
         * - 论述题→知识点关联：基于知识点 title/tags 在题目文本中出现（单向）
         *
         * 已在 seed_data.json 中手工标注 `related_point_ids` 的题目（如示例题）不会被覆盖
         * （调用方在导入时优先使用 seed 值，仅 seed 为 null 时才用本函数派生结果）。
         *
         * 复杂度：O(E × K × T)，其中 E 为 ESSAY 题数、K 为知识点数、T 为平均 tag 数，
         * 运行时随种子规模增长，当前规模下可接受（< 100ms）。
         *
         * 放在 companion object 中以便单元测试直接调用（与 [computeRelatedIdsByTags] 一致）。
         *
         * @param essays 论述题种子列表（仅 ESSAY 类型，调用方负责过滤）
         * @param knowledgePoints 全部知识点种子列表
         * @return examQuestionId → relatedKnowledgePointIds 映射（仅包含有关联的论述题）
         */
        internal fun computeExamQuestionRelatedPoints(
            essays: List<ExamQuestionSeed>,
            knowledgePoints: List<KnowledgePointSeed>,
        ): Map<String, List<String>> {
            val result = mutableMapOf<String, List<String>>()
            // 按科目分组知识点，缩小扫描范围（论述题科目 = 知识点科目才关联）
            val kpsBySubject = knowledgePoints.groupBy { it.subject }

            for (essay in essays) {
                val subjectKps = kpsBySubject[essay.subject].orEmpty()
                if (subjectKps.isEmpty()) continue

                // 题目文本（content + answerFramework）合并扫描，转小写避免大小写问题（中文无影响，英文作家名如 Lu Xun 可能）
                val essayText = (essay.content + " " + (essay.answerFramework.orEmpty())).lowercase()

                // 对每个知识点统计匹配数（title 出现算 2 分，tag 出现算 1 分，title 优先级更高）
                val scored = mutableListOf<Pair<String, Int>>()
                for (kp in subjectKps) {
                    var score = 0
                    // title 匹配（权重 2）：如 "鲁迅《狂人日记》" 出现 "鲁迅" 即匹配
                    val title = kp.title.trim()
                    if (title.isNotEmpty() && title.length >= 2 && essayText.contains(title.lowercase())) {
                        score += 2
                    }
                    // tag 匹配（权重 1）：如 "鲁迅" tag 出现
                    for (tag in kp.tags.orEmpty()) {
                        val trimmedTag = tag.trim()
                        // 过滤过短 tag（如单字 "水"），避免误匹配
                        if (trimmedTag.length >= 2 && essayText.contains(trimmedTag.lowercase())) {
                            score += 1
                        }
                    }
                    if (score > 0) {
                        scored.add(kp.id to score)
                    }
                }

                // 按分数降序 + id 升序（稳定排序），取前 MAX_RELATED_POINTS
                val related = scored
                    .sortedWith(
                        compareByDescending<Pair<String, Int>> { it.second }
                            .thenBy { it.first },
                    )
                    .take(MAX_RELATED_POINTS)
                    .map { it.first }

                if (related.isNotEmpty()) {
                    result[essay.id] = related
                }
            }

            return result
        }
    }
}

/**
 * 种子数据根结构，对应 assets/seed_data.json。
 *
 * 字段对齐 generate_seed.py 输出格式：
 * metadata / subjects / knowledge_points / exam_questions / writing_materials
 *
 * 其中 cards 由 App 侧动态生成（CardRepository），本类只解析业务必需字段。
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

/** 种子 JSON 版本轻量解析壳（v0.9.37 P0-1）：只保留 metadata，其余字段忽略。 */
@kotlinx.serialization.Serializable
private data class SeedVersionShell(
    @SerialName("metadata") val metadata: SeedMetadata? = null,
)

/**
 * 从种子 JSON 字符串轻量解析版本号（v0.9.37 P0-1）。
 *
 * 用 [SeedVersionShell] 解析：`ignoreUnknownKeys=true` 下跳过
 * subjects/knowledge_points/exam_questions/writing_materials 等大数组，
 * **不构建 960+ 实体对象**（仅 tokenize 跳过）。
 *
 * internal 暴露给 [SeedDataLoaderTest]：用含巨大 knowledge_points 数组的
 * JSON 验证只返回 metadata.version 且不抛异常。
 */
internal fun parseSeedVersionFromJson(
    jsonStr: String,
    json: Json = Json { ignoreUnknownKeys = true },
): String {
    val shell = json.decodeFromString(SeedVersionShell.serializer(), jsonStr)
    return shell.metadata?.version.orEmpty()
}

/** 内容版本或导入 schema 任一变化时都必须重导；首次安装始终导入。 */
internal fun shouldImportSeedData(
    initialized: Boolean,
    storedContentVersion: String,
    currentContentVersion: String,
    storedImportSchemaVersion: Int,
    currentImportSchemaVersion: Int = CURRENT_SEED_IMPORT_SCHEMA_VERSION,
): Boolean = !initialized ||
    storedContentVersion != currentContentVersion ||
    storedImportSchemaVersion != currentImportSchemaVersion

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
    /** 交叉校验声称不同教材存在实质表述差异；展示前仍需验证来源是否可追溯。 */
    @SerialName("conflict_flag")
    val conflictFlag: Boolean = false,
    /** 合并前的来源数量，仅用于数据质量审计。 */
    @SerialName("source_count")
    val sourceCount: Int = 0,
    /** 教材/专著来源列表；“其他”等占位值会在导入时过滤。 */
    @SerialName("textbook_sources")
    val textbookSources: List<String> = emptyList(),
    /** 内容合并时间，仅保留种子元数据兼容。 */
    @SerialName("merged_at")
    val mergedAt: String? = null,
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
     * SeedDataLoader 不再消费此字段（v0.9.3 移除知识图谱 UI 后图谱表已 DROP），
     * 保留解析以兼容 seed_data.json 格式，未来可用于其他可视化或检索功能。
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
     * SeedDataLoader 不再消费此字段（v0.9.3 移除知识图谱 UI 后图谱表已 DROP），
     * 保留解析以兼容 seed_data.json 格式。
     */
    val relations: List<RelationSeed>? = null,
)

/**
 * 返回可向用户展示、可供 RAG 引用的真实来源。
 *
 * `source_ref` 是早期格式，`textbook_sources` 是当前格式；合并并去重以兼容历史 seed。
 * “其他/未知/待补”只表示管线没有精确来源，不能作为书名或页码展示。
 */
internal fun KnowledgePointSeed.resolvedSourceFiles(): List<String> =
    (listOfNotNull(sourceRef) + textbookSources)
        .map { it.trim() }
        .filter { it.isNotEmpty() && it !in UNKNOWN_SOURCE_MARKERS }
        .distinct()

private val UNKNOWN_SOURCE_MARKERS = setOf("其他", "未知", "待补", "无", "N/A")

/**
 * 只有至少两个可追溯且不同的教材来源，冲突标记才可以向用户展示。
 *
 * 当前历史 seed 存在 `conflict_flag=true` 但唯一来源为“其他”的记录；直接展示会把无来源
 * 的管线标记包装成有证据的教材分歧，因此必须降级并等待数据修复。
 */
internal fun KnowledgePointSeed.hasVerifiableTextbookConflict(): Boolean =
    conflictFlag && resolvedSourceFiles().size >= 2

/** 使用现有 String 列保存经来源验证的教材冲突状态，无需数据库迁移。 */
internal fun KnowledgePointSeed.contentSourceCode(): String =
    if (hasVerifiableTextbookConflict()) {
        ContentSource.TEXTBOOK_CONFLICT
    } else {
        ContentSource.TEXTBOOK_NATIVE
    }

/** 将 seed 教材列表映射为已有 data_sources 表的稳定记录。 */
internal fun buildSeedDataSourceEntities(
    seeds: List<KnowledgePointSeed>,
    importedKnowledgePointIds: Set<String>,
    createdAt: Long,
): List<DataSourceEntity> = seeds
    .filter { it.id in importedKnowledgePointIds }
    .flatMap { seed ->
        seed.resolvedSourceFiles().mapIndexed { index, sourceFile ->
            DataSourceEntity(
                id = "seed-kp-source:${seed.id}:$index",
                knowledgePointId = seed.id,
                examQuestionId = null,
                sourceFile = sourceFile,
                sourcePage = null,
                contentSource = seed.contentSourceCode(),
                ocrStatus = "VERIFIED",
                createdAt = createdAt,
            )
        }
    }

/** 仅返回数据库中尚无记忆记录的知识点，防止重复 seed 导入覆盖用户进度。 */
internal fun List<KnowledgePointEntity>.missingMemoRecords(
    existingMemoPointIds: Set<String>,
): List<KnowledgePointEntity> = filterNot { it.id in existingMemoPointIds }

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
    /**
     * 答题思路 JSON（v0.9.8 新增，对应 docs/design/essay-module-design.md 3.3 节）。
     *
     * 仅论述题（ESSAY）填充，结构：
     * ```json
     * {
     *   "questionType": "比较型",
     *   "coreKeywords": [...],
     *   "limitKeywords": [...],
     *   "task": "...",
     *   "breakthroughAngles": [...],
     *   "angleRationale": "...",
     *   "argumentPath": { "thesis": "...", "points": [...], "conclusion": "..." }
     * }
     * ```
     *
     * 其他题型或未填充的论述题为 null，UI 优雅降级（隐藏思路区块）。
     */
    val angle: String? = null,
    /**
     * 依据与交叉验证 JSON（v0.9.8 新增，对应 docs/design/essay-module-design.md 3.4 节）。
     *
     * 仅论述题（ESSAY）填充，结构：
     * ```json
     * {
     *   "evidences": [{ "type": "WORK_TEXT|SCHOLAR_OPINION|TEXTBOOK_CONSENSUS", ... }],
     *   "crossValidation": { "textbookComparison": "...", "scholarComparison": "..." },
     *   "referenceLinks": [{ "label": "...", "url": "..." }],
     *   "knowledgeGaps": [{ "author": "...", "note": "..." }]
     * }
     * ```
     *
     * 关键约束：依据必须真实，不能 AI 编造（详见设计文档 5.3 节）。
     * 其他题型或未填充的论述题为 null，UI 优雅降级（隐藏依据区块）。
     */
    val notes: String? = null,
    /**
     * 关联知识点 ID 列表（v0.9.8 新增）。
     *
     * seed_data.json 中可手工标注（如示例题 eq_0038/eq_0182/eq_0254），
     * 其余题目由 [SeedDataLoader.computeExamQuestionRelatedPoints] 派生填充。
     */
    @SerialName("related_point_ids")
    val relatedPointIds: List<String>? = null,
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
