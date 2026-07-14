package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.AppMetaDao
import com.wenyan.app.core.database.entity.AppMetaEntity
import java.util.concurrent.ConcurrentHashMap

/**
 * [AppMetaDao] 的 Fake 实现，供单元测试使用。
 *
 * 用 ConcurrentHashMap 模拟数据库行为（线程安全，支持协程并发调用）。
 */
class FakeAppMetaDao : AppMetaDao {

    private val store = ConcurrentHashMap<String, AppMetaEntity>()

    override suspend fun upsert(entity: AppMetaEntity) {
        store[entity.key] = entity
    }

    override suspend fun getByKey(key: String): AppMetaEntity? = store[key]
}
