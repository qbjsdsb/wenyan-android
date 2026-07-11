package com.wenyan.app.core.data.seed

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.wenyan.app.core.database.dao.ExamCodeHistoryDao
import com.wenyan.app.core.data.repository.GraphRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 种子数据加载器。
 *
 * 职责：
 * - 首次启动时从 assets/seed_data.json 读取种子数据
 * - 导入到 Room 数据库（后续 Task 11-13 添加 Entity 后实现具体导入逻辑）
 * - 使用 DataStore 记录是否已完成初始化，避免重复导入
 *
 * 种子数据结构镜像 Spec 4.3 节，覆盖四类：知识点 / 真题 / 卡片 / 写作素材。
 * 后续 Phase 1 产出的种子数据可直接替换 assets/seed_data.json 即可更新。
 */
@Singleton
class SeedDataLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val examCodeHistoryDao: ExamCodeHistoryDao,
    private val graphRepository: GraphRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // DataStore 实例，记录初始化状态
    private val Context.seedDataStore: DataStore<Preferences> by preferencesDataStore(
        name = SEED_PREFERENCES_NAME,
    )

    /**
     * 确保种子数据已加载：若尚未初始化则读取并导入，已初始化则跳过。
     */
    suspend fun ensureSeedDataLoaded() {
        val initialized = isInitialized()
        if (initialized) return

        val seedData = readSeedDataFromAssets()
        importToDatabase(seedData)
        markInitialized()
    }

    // 读取 DataStore 中是否已初始化标志
    private suspend fun isInitialized(): Boolean =
        context.seedDataStore.data.map { it[KEY_SEED_INITIALIZED] ?: false }.first()

    // 标记初始化完成
    private suspend fun markInitialized() {
        context.seedDataStore.edit { it[KEY_SEED_INITIALIZED] = true }
    }

    // 从 assets 读取并解析 seed_data.json
    private fun readSeedDataFromAssets(): SeedData {
        val jsonStr = context.assets.open(SEED_DATA_FILE).bufferedReader().use { it.readText() }
        return json.decodeFromString(SeedData.serializer(), jsonStr)
    }

    // 将种子数据导入 Room 数据库
    // TODO Task 11-13：在 Entity / DAO 就绪后实现其他种子数据导入逻辑
    private suspend fun importToDatabase(seedData: SeedData) {
        // seedData.knowledgePoints -> knowledgePointDao.insertAll(...)
        // seedData.examQuestions -> examQuestionDao.insertAll(...)
        // seedData.cards -> cardDao.insertAll(...)
        // seedData.writingMaterials -> writingMaterialDao.insertAll(...)

        // Task 26：导入科目代码历史数据，支持 610/801 语义翻转判定
        examCodeHistoryDao.insertAll(ExamCodeHistoryData.EXAM_CODE_HISTORY)

        // Task 19：导入知识图谱骨架数据（Spec 第 307-342 行，功能性知识图谱）
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
        private const val SEED_DATA_FILE = "seed_data.json"
        private const val SEED_PREFERENCES_NAME = "wenyan_seed_prefs"
        private val KEY_SEED_INITIALIZED = booleanPreferencesKey("seed_initialized")
    }
}

/**
 * 种子数据根结构，对应 assets/seed_data.json。
 *
 * 四个数组分别对应 Spec 4.3 节四类数据，初始为空占位，
 * 后续 Task 9 填充真实种子数据。
 */
@kotlinx.serialization.Serializable
data class SeedData(
    @SerialName("knowledge_points")
    val knowledgePoints: List<kotlinx.serialization.json.JsonElement> = emptyList(),
    @SerialName("exam_questions")
    val examQuestions: List<kotlinx.serialization.json.JsonElement> = emptyList(),
    val cards: List<kotlinx.serialization.json.JsonElement> = emptyList(),
    @SerialName("writing_materials")
    val writingMaterials: List<kotlinx.serialization.json.JsonElement> = emptyList(),
)
