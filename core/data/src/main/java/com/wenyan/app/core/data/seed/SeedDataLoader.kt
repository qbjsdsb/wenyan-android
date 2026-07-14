package com.wenyan.app.core.data.seed

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
 *   用户无数据可被覆盖。"版本感知种子升级"（升级时按 seed_data.json 版本号增量导入）
 *   应作为独立的 P1 feature，不在本修复范围。
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
     * 确保种子数据已加载：若尚未初始化则读取并导入，已初始化则跳过。
     *
     * NF-J 修复：[isInitialized] 与 [markInitialized] 都已加 IOException 兜底，
     * 此方法自身不会再因 DataStore 故障抛 IOException；[readSeedDataFromAssets]
     * 与 [importToDatabase] 的异常会冒泡到 Application 的 CoroutineExceptionHandler
     * 被吞掉（Log.e），下次启动重试。
     */
    suspend fun ensureSeedDataLoaded() {
        val initialized = isInitialized()
        if (initialized) return

        val seedData = readSeedDataFromAssets()
        importToDatabase(seedData)
        markInitialized()
    }

    /**
     * 读取 DataStore 中是否已初始化标志。
     *
     * NF-J 修复：DataStore 读写可能抛 [IOException]（磁盘满/权限/文件损坏），
     * 兜底返回 false 让种子重试导入（[importToDatabase] 用事务包裹 + REPLACE 幂等，
     * 重复导入无副作用）。
     */
    private suspend fun isInitialized(): Boolean = try {
        preferencesDataStore.data.map { it[KEY_SEED_INITIALIZED] ?: false }.first()
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
            preferencesDataStore.edit { it[KEY_SEED_INITIALIZED] = true }
        } catch (e: IOException) {
            Log.w(TAG, "DataStore write failed, seed will re-import on next launch", e)
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
     */
    private suspend fun importToDatabase(seedData: SeedData) = database.withTransaction {
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
                multiPerspectives = null,
                relatedIds = null,
                contrastIds = null,
                extensionIds = null,
                examRecords = null,
                examFrequency = "NEVER",
                termTemplate = null,
                tags = seed.tags,
                difficulty = seed.difficulty,
                createdAt = now,
                updatedAt = now,
                contentSource = "TEXTBOOK_NATIVE",
                ocrStatus = "VERIFIED",
                sourceFile = seed.sourceRef,
                sourcePage = null,
                studyText = null,
            )
        }
        if (knowledgePointEntities.isNotEmpty()) {
            knowledgePointDao.insertAll(knowledgePointEntities)
        }

        // 步骤4：为每个知识点创建初始 MemoRecord（state=NEW，立即到期可复习）
        val memoRecords = knowledgePointEntities.map { kp ->
            MemoRecordEntity(
                pointId = kp.id,
                state = "NEW",
                stability = 0.0,
                difficulty = 5.0,
                lastReviewAt = now,
                nextReviewAt = now, // 立即到期，新知识点可立即进入复习队列
                reviewCount = 0,
                failCount = 0,
                history = null,
                inPriorityQueue = 0,
            )
        }
        if (memoRecords.isNotEmpty()) {
            memoRecordDao.insertAll(memoRecords)
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
                sampleEssay = seed.sampleEssay,
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

        // 步骤7：保留科目代码历史 + 知识图谱骨架导入
        examCodeHistoryDao.insertAll(ExamCodeHistoryData.EXAM_CODE_HISTORY)
        importGraphSkeleton()
    }

    /**
     * 导入知识图谱骨架数据。
     *
     * 包括：
     * - 南师大现当代文学考点骨架（7 位作家节点 + 作家关系）
     * - 体裁×时段二维矩阵骨架（6 节点 + 8 关系）
     */
    private suspend fun importGraphSkeleton() {
        // 南师大现当代文学考点骨架
        GraphSkeleton.SOUTHERN_NORMAL_AUTHORS.forEach { graphRepository.insertNode(it) }
        GraphSkeleton.SOUTHERN_NORMAL_RELATIONS.forEach { graphRepository.insertEdge(it) }

        // 体裁×时段二维矩阵骨架
        GraphSkeleton.GENRE_PERIOD_MATRIX.forEach { graphRepository.insertNode(it) }
        GraphSkeleton.GENRE_PERIOD_RELATIONS.forEach { graphRepository.insertEdge(it) }
    }

    companion object {
        private const val TAG = "SeedDataLoader"
        private const val SEED_DATA_FILE = "seed_data.json"
        private val KEY_SEED_INITIALIZED = booleanPreferencesKey("seed_initialized")

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
    val cards: List<kotlinx.serialization.json.JsonElement> = emptyList(),
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
)

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
    @SerialName("sample_essay")
    val sampleEssay: String? = null,
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
