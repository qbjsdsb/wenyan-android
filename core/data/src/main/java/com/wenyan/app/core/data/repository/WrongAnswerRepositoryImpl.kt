package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.WrongAnswerDao
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.database.entity.WrongAnswerWithDetails
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 错题本仓库实现(NF-PP5 Wave 2.4)。
 *
 * 持久化答错题目到 wrong_answers 表。重复答错时递增 wrongCount(不重复插入),
 * markResolved 后该错题不再出现在 observeUnresolved 中。
 *
 * v0.9.2：observeAll/observeUnresolved 返回 [WrongAnswerWithDetails]（JOIN 关联表
 * 获取题目文本），供 UI 渲染题目区。
 *
 * @property wrongAnswerDao 错题 DAO
 */
@Singleton
class WrongAnswerRepositoryImpl @Inject constructor(
    private val wrongAnswerDao: WrongAnswerDao,
) : WrongAnswerRepository {

    override fun observeAll(): Flow<List<WrongAnswerWithDetails>> =
        wrongAnswerDao.observeAll()

    override fun observeUnresolved(): Flow<List<WrongAnswerWithDetails>> =
        wrongAnswerDao.observeUnresolved()

    override fun observeByPoint(pointId: String): Flow<List<WrongAnswerEntity>> =
        wrongAnswerDao.observeByPoint(pointId)

    override fun observeByExamQuestion(examQuestionId: String): Flow<List<WrongAnswerEntity>> =
        wrongAnswerDao.observeByExamQuestion(examQuestionId)

    override suspend fun recordWrongAnswer(
        pointId: String?,
        examQuestionId: String?,
        userAnswer: String,
        correctAnswer: String?,
        source: String,
    ): String {
        val now = System.currentTimeMillis()

        // 1. 查找已有未解决错题(按来源区分查询路径)
        //    - 卡片来源(source = CARD_AGAIN):按 pointId 查
        //    - 真题来源(source = QUIZ_WRONG):按 examQuestionId 查
        val existing: WrongAnswerEntity? = when {
            pointId != null -> wrongAnswerDao.findUnresolvedByPointAndSource(pointId, source)
            examQuestionId != null -> wrongAnswerDao.findUnresolvedByExamQuestionAndSource(examQuestionId, source)
            else -> null
        }

        // 2. 已有记录 → 递增 wrongCount + 重置 resolvedAt = NULL
        if (existing != null) {
            wrongAnswerDao.incrementWrongCount(existing.id, now)
            return existing.id
        }

        // 3. 新记录 → upsert WrongAnswerEntity
        val id = UUID.randomUUID().toString()
        wrongAnswerDao.upsert(
            WrongAnswerEntity(
                id = id,
                pointId = pointId,
                examQuestionId = examQuestionId,
                userAnswer = userAnswer,
                correctAnswer = correctAnswer,
                source = source,
                wrongCount = 1,
                lastWrongAt = now,
                resolvedAt = null,
                aiExplanation = null,
                createdAt = now,
            ),
        )
        return id
    }

    override suspend fun markResolved(id: String) {
        wrongAnswerDao.markResolved(id, System.currentTimeMillis())
    }

    override suspend fun deleteById(id: String) {
        wrongAnswerDao.deleteById(id)
    }

    override suspend fun countUnresolved(): Int =
        wrongAnswerDao.countUnresolved()
}
