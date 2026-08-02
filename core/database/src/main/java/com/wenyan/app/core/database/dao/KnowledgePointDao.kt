package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import kotlinx.coroutines.flow.Flow

/**
 * 知识点表 DAO（Task 12）。
 *
 * 含按 ocr_status 索引查询（SubTask 12.6）。
 */
@Dao
interface KnowledgePointDao {

    // P0 修正:原用 @Insert(REPLACE),DELETE+INSERT 会触发子表 CASCADE
    // (memo_records/review_logs/data_sources 全部级联删除),
    // 静默清空用户 FSRS 调度数据与复习历史。改用 @Upsert。
    @Upsert
    suspend fun insert(entity: KnowledgePointEntity)

    @Upsert
    suspend fun insertAll(entities: List<KnowledgePointEntity>)

    @Update
    suspend fun update(entity: KnowledgePointEntity)

    @Query("DELETE FROM knowledge_points WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM knowledge_points WHERE id = :id")
    suspend fun getById(id: String): KnowledgePointEntity?

    /** 批量查询知识点（用于区分卡对比项标题查询，避免 N+1 问题） */
    @Query("SELECT * FROM knowledge_points WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<KnowledgePointEntity>

    @Query("SELECT * FROM knowledge_points WHERE id = :id")
    fun observeById(id: String): Flow<KnowledgePointEntity?>

    @Query("SELECT * FROM knowledge_points WHERE chapter_id = :chapterId ORDER BY created_at ASC")
    fun observeByChapter(chapterId: String): Flow<List<KnowledgePointEntity>>

    @Query("SELECT * FROM knowledge_points WHERE exam_frequency = :frequency ORDER BY created_at ASC")
    fun observeByExamFrequency(frequency: String): Flow<List<KnowledgePointEntity>>

    /** 按 OCR 状态查询（索引 ocr_status） */
    @Query("SELECT * FROM knowledge_points WHERE ocr_status = :status ORDER BY created_at ASC")
    fun observeByOcrStatus(status: String): Flow<List<KnowledgePointEntity>>

    /** 按内容来源查询 */
    @Query("SELECT * FROM knowledge_points WHERE content_source = :source ORDER BY created_at ASC")
    fun observeByContentSource(source: String): Flow<List<KnowledgePointEntity>>

    @Query("SELECT COUNT(*) FROM knowledge_points WHERE chapter_id = :chapterId")
    suspend fun countByChapter(chapterId: String): Int

    @Query("SELECT * FROM knowledge_points ORDER BY created_at ASC")
    fun observeAll(): Flow<List<KnowledgePointEntity>>

    /** 查询所有 OCR 已校验（VERIFIED）的知识点，用于 FSRS 复习队列（过滤 PENDING） */
    @Query("SELECT * FROM knowledge_points WHERE ocr_status = 'VERIFIED' ORDER BY updated_at DESC")
    fun observeVerifiedForReview(): Flow<List<KnowledgePointEntity>>

    /** 更新知识点的 OCR 状态（PENDING -> VERIFIED 激活），同时刷新 updated_at */
    @Query(
        "UPDATE knowledge_points SET ocr_status = :status, " +
            "updated_at = (CAST(strftime('%s', 'now') AS INTEGER) * 1000) WHERE id = :id",
    )
    suspend fun updateOcrStatus(id: String, status: String)

    /**
     * 全文关键词搜索（阶段4新增，RAG 检索用）。
     *
     * 在 title / core_conclusion / full_content / study_text 四个字段中做 LIKE 搜索。
     * SQLite LIKE 对中文友好，无需分词。
     *
     * NF-BB1 修复：加 ESCAPE '\\' 子句，配合调用方转义 % 和 _ 通配符。
     * 原查询未转义，搜索"100%"会匹配"1000"等（% 被当通配符）。
     * 调用方（RagEngine）需在传入前 escapeLikeWildcards(keyword)。
     *
     * @param keyword 搜索关键词（已转义 % 和 _）
     * @return 匹配的知识点列表，按 updated_at DESC 排序
     */
    @Query(
        // v0.9.26 修复：RAG 检索过滤 ocr_status='VERIFIED'（未校对知识点不进 AI 上下文）。
        // 原实现无过滤，PENDING 未校对数据可能被喂给 LLM；与 observeSearchWithSubject 一致。
        "SELECT * FROM knowledge_points WHERE " +
            "ocr_status = 'VERIFIED' AND (" +
            "title LIKE '%' || :keyword || '%' ESCAPE '\\' OR " +
            "core_conclusion LIKE '%' || :keyword || '%' ESCAPE '\\' OR " +
            "full_content LIKE '%' || :keyword || '%' ESCAPE '\\' OR " +
            "study_text LIKE '%' || :keyword || '%' ESCAPE '\\' " +
            ") ORDER BY updated_at DESC LIMIT :limit",
    )
    suspend fun searchByKeyword(keyword: String, limit: Int = 5): List<KnowledgePointEntity>

    /**
     * 全文关键词搜索(带科目名,Flow 版本,v0.8.19 新增)。
     *
     * 供 [com.wenyan.app.feature.knowledge.KnowledgeViewModel] 搜索框使用。
     * 与 [searchByKeyword] 的区别:
     * - 返回 [KnowledgePointWithSubject](含科目名,供 UI 分类标签展示)
     * - 返回 Flow(数据库变更时自动刷新搜索结果)
     * - 仅搜索 VERIFIED 知识点(与 [observeVerifiedWithSubject] 一致,未校对的不进搜索)
     * - 无 limit(搜索结果由 ViewModel 控制数量)
     *
     * NF-BB1:同样加 ESCAPE '\\',调用方需转义 % 和 _。
     *
     * @param keyword 搜索关键词(已转义 % 和 _),空字符串时返回所有 VERIFIED 知识点
     * @return 匹配的知识点 + 科目名列表,按 updated_at DESC 排序
     */
    @Query(
        "SELECT kp.*, s.name AS subject_name " +
            "FROM knowledge_points kp " +
            "LEFT JOIN chapters c ON kp.chapter_id = c.id " +
            "LEFT JOIN subjects s ON c.subject_id = s.id " +
            "WHERE kp.ocr_status = 'VERIFIED' AND (" +
            "kp.title LIKE '%' || :keyword || '%' ESCAPE '\\' OR " +
            "kp.core_conclusion LIKE '%' || :keyword || '%' ESCAPE '\\' OR " +
            "kp.full_content LIKE '%' || :keyword || '%' ESCAPE '\\' OR " +
            "kp.study_text LIKE '%' || :keyword || '%' ESCAPE '\\' " +
            ") ORDER BY kp.updated_at DESC",
    )
    fun observeSearchWithSubject(keyword: String): Flow<List<KnowledgePointWithSubject>>

    /**
     * 查询所有 VERIFIED 知识点，附带科目名（P1 修复）。
     *
     * 通过 LEFT JOIN chapters + subjects 一次查询获取科目名，避免 N+1。
     *
     * P1-AUDIT-5 修正：原用 INNER JOIN，若知识点无对应 chapter 或 chapter 无对应 subject
     * （数据异常），该知识点会被静默排除，用户在列表中无感知地"丢失"知识点。
     * 改用 LEFT JOIN 后，无有效关联的知识点依然返回，subject_name 为 null，
     * UI 层 fallback 显示"未知科目"，确保数据不丢失。
     *
     * 关联路径：knowledge_points.chapter_id → chapters.subject_id → subjects.id
     *
     * @return 知识点 + 科目名（可能为 null）的关联列表，按 updated_at DESC 排序
     */
    @Query(
        "SELECT kp.*, s.name AS subject_name " +
            "FROM knowledge_points kp " +
            "LEFT JOIN chapters c ON kp.chapter_id = c.id " +
            "LEFT JOIN subjects s ON c.subject_id = s.id " +
            "WHERE kp.ocr_status = 'VERIFIED' " +
            "ORDER BY kp.updated_at DESC",
    )
    fun observeVerifiedWithSubject(): Flow<List<KnowledgePointWithSubject>>
}
