package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.ApiConfigDao
import com.wenyan.app.core.database.entity.ApiConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * [ApiConfigDao] 的 Fake 实现，供单元测试使用。
 *
 * 用内存 MutableStateFlow 模拟数据库行为。
 */
class FakeApiConfigDao : ApiConfigDao {

    private val configs = MutableStateFlow<List<ApiConfigEntity>>(emptyList())

    override suspend fun insert(entity: ApiConfigEntity) {
        configs.value = configs.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun insertAll(entities: List<ApiConfigEntity>) {
        // REPLACE 语义：替换已有的同 ID 记录
        val newIds = entities.map { it.id }.toSet()
        configs.value = configs.value.filterNot { it.id in newIds } + entities
    }

    override suspend fun update(entity: ApiConfigEntity) {
        configs.value = configs.value.map { if (it.id == entity.id) entity else it }
    }

    override suspend fun deleteById(id: String) {
        configs.value = configs.value.filterNot { it.id == id }
    }

    override suspend fun getById(id: String): ApiConfigEntity? =
        configs.value.find { it.id == id }

    override suspend fun getCurrent(): ApiConfigEntity? =
        configs.value.find { it.isCurrent == 1 }

    override fun observeCurrent(): Flow<ApiConfigEntity?> =
        configs.map { list -> list.find { it.isCurrent == 1 } }

    override suspend fun setCurrent(id: String) {
        configs.value = configs.value.map { config ->
            config.copy(isCurrent = if (config.id == id) 1 else 0)
        }
    }

    override fun observeEnabled(): Flow<List<ApiConfigEntity>> =
        configs.map { list -> list.filter { it.isEnabled == 1 } }

    override fun observeAll(): Flow<List<ApiConfigEntity>> = configs
}
