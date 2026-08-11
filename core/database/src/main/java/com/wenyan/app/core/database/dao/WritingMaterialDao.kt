package com.wenyan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.Transaction
import com.wenyan.app.core.database.entity.WritingMaterialEntity
import com.wenyan.app.core.database.entity.WritingMaterialWithSources
import kotlinx.coroutines.flow.Flow

/**
 * 写作素材表 DAO。
 */
@Dao
interface WritingMaterialDao {

    @Upsert
    suspend fun insert(entity: WritingMaterialEntity)

    @Upsert
    suspend fun insertAll(entities: List<WritingMaterialEntity>)

    @Update
    suspend fun update(entity: WritingMaterialEntity)

    @Query("DELETE FROM writing_materials WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM writing_materials WHERE id = :id")
    suspend fun getById(id: String): WritingMaterialEntity?

    @Query("SELECT * FROM writing_materials WHERE category = :category ORDER BY created_at DESC")
    fun observeByCategory(category: String): Flow<List<WritingMaterialEntity>>

    /**
     * 按标签模糊查询写作素材。
     *
     * P1-2 修复：加 `ESCAPE '\\'` 子句，调用方需在传入 [tag] 前转义 LIKE 通配符
     * （`%` / `_` / `\`），否则这些字符会被当通配符解释，导致查询结果错误。
     *
     * 转义参考实现（与 [com.wenyan.app.core.ai.RagEngine] 内部一致）：
     * ```kotlin
     * fun escapeLikeWildcards(input: String): String =
     *     input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
     * ```
     *
     * @param tag 已转义的标签字符串
     */
    @Query("SELECT * FROM writing_materials WHERE tags LIKE '%' || :tag || '%' ESCAPE '\\' ORDER BY created_at DESC")
    fun observeByTag(tag: String): Flow<List<WritingMaterialEntity>>

    @Query("SELECT * FROM writing_materials ORDER BY created_at DESC")
    fun observeAll(): Flow<List<WritingMaterialEntity>>

    @Transaction
    @Query("SELECT * FROM writing_materials ORDER BY created_at DESC")
    fun observeAllWithSources(): Flow<List<WritingMaterialWithSources>>
}
