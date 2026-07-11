package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wenyan.app.core.database.entity.ExamCodeHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 科目代码变动历史表 DAO（Spec 新增表）。
 */
@Dao
interface ExamCodeHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExamCodeHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExamCodeHistoryEntity>)

    @Update
    suspend fun update(entity: ExamCodeHistoryEntity)

    @Query("DELETE FROM exam_code_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM exam_code_history WHERE id = :id")
    suspend fun getById(id: String): ExamCodeHistoryEntity?

    @Query("SELECT * FROM exam_code_history WHERE exam_code = :code ORDER BY valid_from_year ASC")
    fun observeByCode(code: String): Flow<List<ExamCodeHistoryEntity>>

    /** 查询某年份有效的所有科目代码 */
    @Query(
        "SELECT * FROM exam_code_history " +
            "WHERE valid_from_year <= :year " +
            "AND (valid_to_year IS NULL OR valid_to_year >= :year) " +
            "ORDER BY exam_code ASC",
    )
    fun observeValidInYear(year: Int): Flow<List<ExamCodeHistoryEntity>>

    @Query("SELECT * FROM exam_code_history ORDER BY valid_from_year ASC")
    fun observeAll(): Flow<List<ExamCodeHistoryEntity>>
}
