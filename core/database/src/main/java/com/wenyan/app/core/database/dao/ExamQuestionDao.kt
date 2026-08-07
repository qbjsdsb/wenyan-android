package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 真题表 DAO（Task 11）。
 *
 * 含按 exam_paper_code 索引查询（SubTask 11.6）。
 */
@Dao
interface ExamQuestionDao {

    // P1 修正:原用 @Insert(REPLACE),DELETE+INSERT 会触发子表 CASCADE
    // (data_sources/ai_grading_records 级联删除,template_fills SET_NULL)。
    // 改用 @Upsert(INSERT ... ON CONFLICT DO UPDATE)。
    @Upsert
    suspend fun insert(entity: ExamQuestionEntity)

    @Upsert
    suspend fun insertAll(entities: List<ExamQuestionEntity>)

    @Update
    suspend fun update(entity: ExamQuestionEntity)

    @Query("DELETE FROM exam_questions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM exam_questions WHERE id = :id")
    suspend fun getById(id: String): ExamQuestionEntity?

    @Query("SELECT * FROM exam_questions WHERE id = :id")
    fun observeById(id: String): Flow<ExamQuestionEntity?>

    @Query("SELECT * FROM exam_questions WHERE subject_id = :subjectId ORDER BY year DESC")
    fun observeBySubject(subjectId: String): Flow<List<ExamQuestionEntity>>

    @Query("SELECT * FROM exam_questions WHERE year = :year ORDER BY subject_id ASC")
    fun observeByYear(year: Int): Flow<List<ExamQuestionEntity>>

    @Query("SELECT * FROM exam_questions WHERE question_type = :type ORDER BY year DESC")
    fun observeByQuestionType(type: String): Flow<List<ExamQuestionEntity>>

    /**
     * 查询指定多个题型的真题（v0.9.33 真题背题新增）。
     *
     * 用于"名词解释 + 简答"背题专项：`IN (:types)` 严格限定题型，
     * 从数据层排除 ESSAY（论述题有独立板块，避免两处重复展示）。
     *
     * 索引：question_type 已有单列索引（v0.9.24），IN 子句可用。
     * 数据量 346 条；保留稳定 ORDER BY（year DESC, exam_paper_code ASC, id ASC）
     * 保证前后题导航顺序不随查询漂移。
     */
    @Query("SELECT * FROM exam_questions WHERE question_type IN (:types) ORDER BY year DESC, exam_paper_code ASC, id ASC")
    fun observeByQuestionTypes(types: List<String>): Flow<List<ExamQuestionEntity>>

    /**
     * 查询所有论述题（v0.9.8 论述题板块新增）。
     *
     * 返回全部 ESSAY 题按年份倒序，调用方在内存中按 `related_point_ids`
     * 过滤出与某知识点关联的题目（当前数据量下无需 SQL LIKE）。
     *
     * 不在 SQL 层用 `related_point_ids LIKE '%pointId%'` 的原因：
     * - SQL LIKE 对 JSON 数组无原生支持，会出现 "kp_1" 误匹配 "kp_10/kp_100" 子串
     * - 内存过滤可精确匹配 List<String> contains
     * - 当前数据量下性能无差异
     */
    @Query("SELECT * FROM exam_questions WHERE question_type = 'ESSAY' ORDER BY year DESC, exam_paper_code ASC")
    fun observeAllEssays(): Flow<List<ExamQuestionEntity>>

    /** 按试卷代码查询（索引 exam_paper_code，SubTask 11.6） */
    @Query("SELECT * FROM exam_questions WHERE exam_paper_code = :code ORDER BY year DESC")
    fun observeByExamPaperCode(code: String): Flow<List<ExamQuestionEntity>>

    /** 按答案状态查询 */
    @Query("SELECT * FROM exam_questions WHERE answer_status = :status ORDER BY year DESC")
    fun observeByAnswerStatus(status: String): Flow<List<ExamQuestionEntity>>

    @Query("SELECT DISTINCT year FROM exam_questions ORDER BY year DESC")
    fun observeYears(): Flow<List<Int>>

    @Query("SELECT COUNT(*) FROM exam_questions WHERE subject_id = :subjectId")
    suspend fun countBySubject(subjectId: String): Int
}
