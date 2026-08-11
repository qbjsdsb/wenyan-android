package com.wenyan.app.feature.settings

import com.wenyan.app.core.database.dao.StudyProgressDao
import com.wenyan.app.core.database.entity.StudyProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * [StudyProgressDao] 的 Fake 实现,供 settings 模块测试使用(v0.8.13 P2-1 新增)。
 *
 * 仅 stub [observeById](StudyProgressViewModel 间接调用),其他方法抛异常。
 * 通过 [entity] 可控注入学习进度数据(单行记录,id="default")。
 */
class FakeStudyProgressDao(
    initialEntity: StudyProgressEntity? = null,
) : StudyProgressDao {

    private val _entity = MutableStateFlow(initialEntity)

    /** 当前学习进度记录(测试可读写) */
    var entity: StudyProgressEntity?
        get() = _entity.value
        set(value) { _entity.value = value }

    /** 模拟 DAO 观察流失败的异常；null 表示正常观察。 */
    var observeFailure: Throwable? = null

    override suspend fun upsert(entity: StudyProgressEntity) {
        throw UnsupportedOperationException("upsert not used in settings tests")
    }

    override suspend fun update(entity: StudyProgressEntity) {
        throw UnsupportedOperationException("update not used in settings tests")
    }

    override suspend fun deleteById(id: String) {
        throw UnsupportedOperationException("deleteById not used in settings tests")
    }

    override suspend fun getById(id: String): StudyProgressEntity? =
        throw UnsupportedOperationException("getById not used in settings tests")

    override fun observeById(id: String): Flow<StudyProgressEntity?> =
        observeFailure?.let { failure -> flow { throw failure } } ?: _entity.asStateFlow()
}
