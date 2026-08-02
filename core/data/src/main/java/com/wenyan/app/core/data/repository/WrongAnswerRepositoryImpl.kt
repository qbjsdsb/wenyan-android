package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.WrongAnswerDao
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.database.entity.WrongAnswerWithDetails
import kotlinx.coroutines.flow.Flow
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
 * v0.9.22（P2-5）：recordWrongAnswer 改用 [ClockGuard] 时间源，与 FSRS 调度
 * （SchedulingRepository.rateWrongAnswer 用 ClockGuard.effectiveNowMillis）对齐，
 * 避免时钟回拨时错题时间戳与调度时间源不一致。
 *
 * @property wrongAnswerDao 错题 DAO
 * @property clockGuard 时钟守卫（检测回拨，返回单调不减的有效时间戳）
 */
@Singleton
class WrongAnswerRepositoryImpl @Inject constructor(
    private val wrongAnswerDao: WrongAnswerDao,
    private val clockGuard: ClockGuard,
) : WrongAnswerRepository {

    override fun observeAll(): Flow<List<WrongAnswerWithDetails>> =
        wrongAnswerDao.observeAll()

    override fun observeUnresolved(): Flow<List<WrongAnswerWithDetails>> =
        wrongAnswerDao.observeUnresolved()

    override fun observeDueWrongAnswers(now: Long): Flow<List<WrongAnswerWithDetails>> =
        wrongAnswerDao.observeDueWrongAnswers(now)

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
        // v0.9.22 P2-5：时间源改用 ClockGuard（原 System.currentTimeMillis）。
        // recordWrongAnswer 与 rateWrongAnswer（FSRS 调度）现在使用同一时间源，
        // 时钟回拨时错题时间戳与调度 DUE 过滤一致。
        val now = clockGuard.effectiveNowMillis()

        // v0.9.22 P2-4：查找 + 递增/插入整体放入一个 DAO @Transaction，
        // 杜绝并发下两个线程都查到 null 后各自 insert 的重复插入窗口。
        return wrongAnswerDao.recordWrongAnswer(
            pointId = pointId,
            examQuestionId = examQuestionId,
            userAnswer = userAnswer,
            correctAnswer = correctAnswer,
            source = source,
            now = now,
        )
    }

    override suspend fun markResolved(id: String) {
        wrongAnswerDao.markResolved(id, clockGuard.effectiveNowMillis())
    }

    override suspend fun deleteById(id: String) {
        wrongAnswerDao.deleteById(id)
    }

    override suspend fun countUnresolved(): Int =
        wrongAnswerDao.countUnresolved()
}
