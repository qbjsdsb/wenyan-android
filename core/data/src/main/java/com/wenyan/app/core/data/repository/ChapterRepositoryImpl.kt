package com.wenyan.app.core.data.repository

import com.wenyan.app.core.data.util.catchAndLog
import com.wenyan.app.core.database.dao.ChapterDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.SubjectDao
import com.wenyan.app.core.database.entity.ChapterEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ChapterRepository] 生产实现（ADR-001 B1.2）。
 *
 * 通过 [ChapterDao] / [SubjectDao] / [KnowledgePointDao] 组合提供章节树读取能力。
 * DAO 由 [com.wenyan.app.core.database.di.DatabaseModule] 提供。
 *
 * 异常处理与 [KnowledgeRepository] / [WrongAnswerRepositoryImpl] 一致：
 * Flow 用 catchAndLog 兜底，suspend 函数抛出由调用方处理。
 */
@Singleton
class ChapterRepositoryImpl @Inject constructor(
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val knowledgePointDao: KnowledgePointDao,
) : ChapterRepository {

    override fun observeSubjects(): Flow<List<SubjectEntity>> =
        subjectDao.observeAll()
            .catchAndLog(TAG, "observeSubjects") { emptyList() }

    override fun observeRootChapters(subjectId: String): Flow<List<ChapterEntity>> =
        chapterDao.observeRoots(subjectId)
            .catchAndLog(TAG, "observeRootChapters") { emptyList() }

    override fun observeChapters(subjectId: String): Flow<List<ChapterEntity>> =
        chapterDao.observeBySubject(subjectId)
            .catchAndLog(TAG, "observeChapters") { emptyList() }

    override fun observeChildren(parentId: String): Flow<List<ChapterEntity>> =
        chapterDao.observeChildren(parentId)
            .catchAndLog(TAG, "observeChildren") { emptyList() }

    override fun observeTree(rootId: String): Flow<List<ChapterEntity>> =
        chapterDao.observeTree(rootId)
            .catchAndLog(TAG, "observeTree") { emptyList() }

    override fun observeKnowledgePointsByChapter(chapterId: String): Flow<List<KnowledgePointEntity>> =
        knowledgePointDao.observeByChapter(chapterId)
            .catchAndLog(TAG, "observeKnowledgePointsByChapter") { emptyList() }

    override suspend fun countNonRootChapters(): Int =
        chapterDao.countNonRootChapters()

    private companion object {
        const val TAG = "ChapterRepository"
    }
}
