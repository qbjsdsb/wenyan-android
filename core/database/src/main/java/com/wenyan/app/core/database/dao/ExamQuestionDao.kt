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
