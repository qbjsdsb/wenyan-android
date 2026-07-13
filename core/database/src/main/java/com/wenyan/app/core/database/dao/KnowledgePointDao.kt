package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: KnowledgePointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

    @Query("SELECT * FROM knowledge_points WHERE exam_frequency = :frequency")
    fun observeByExamFrequency(frequency: String): Flow<List<KnowledgePointEntity>>

    /** 按 OCR 状态查询（索引 ocr_status） */
    @Query("SELECT * FROM knowledge_points WHERE ocr_status = :status")
    fun observeByOcrStatus(status: String): Flow<List<KnowledgePointEntity>>

    /** 按内容来源查询 */
    @Query("SELECT * FROM knowledge_points WHERE content_source = :source")
    fun observeByContentSource(source: String): Flow<List<KnowledgePointEntity>>

    @Query("SELECT COUNT(*) FROM knowledge_points WHERE chapter_id = :chapterId")
    suspend fun countByChapter(chapterId: String): Int

    @Query("SELECT * FROM knowledge_points")
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
     * @param keyword 搜索关键词
     * @return 匹配的知识点列表，按 updated_at DESC 排序
     */
    @Query(
        "SELECT * FROM knowledge_points WHERE " +
            "title LIKE '%' || :keyword || '%' OR " +
            "core_conclusion LIKE '%' || :keyword || '%' OR " +
            "full_content LIKE '%' || :keyword || '%' OR " +
            "study_text LIKE '%' || :keyword || '%' " +
            "ORDER BY updated_at DESC LIMIT :limit",
    )
    suspend fun searchByKeyword(keyword: String, limit: Int = 5): List<KnowledgePointEntity>

    /**
     * 查询所有 VERIFIED 知识点，附带科目名（P1 修复）。
     *
     * 通过 JOIN chapters + subjects 一次查询获取科目名，避免 N+1。
     * 用 INNER JOIN：若知识点无对应科目（数据异常），不显示在列表中
     * （强制数据完整性，比显示"未知科目"更好）。
     *
     * 关联路径：knowledge_points.chapter_id → chapters.subject_id → subjects.id
     *
     * @return 知识点 + 科目名的关联列表，按 updated_at DESC 排序
     */
    @Query(
        "SELECT kp.*, s.name AS subject_name " +
            "FROM knowledge_points kp " +
            "INNER JOIN chapters c ON kp.chapter_id = c.id " +
            "INNER JOIN subjects s ON c.subject_id = s.id " +
            "WHERE kp.ocr_status = 'VERIFIED' " +
            "ORDER BY kp.updated_at DESC",
    )
    fun observeVerifiedWithSubject(): Flow<List<KnowledgePointWithSubject>>
}
