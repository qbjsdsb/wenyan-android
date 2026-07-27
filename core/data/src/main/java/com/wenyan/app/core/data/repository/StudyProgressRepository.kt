package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.StudyProgressDao
import com.wenyan.app.core.database.entity.StudyProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 学习进度仓库(P0 v0.7.2 新增,接通 study_progress 死表)。
 *
 * 职责:
 * - 维护单行学习进度记录(id="default")
 * - 记录上次学习的知识点 ID + 访问时间
 * - 计算连续学习天数(streak_days)
 * - 提供累计学习时长查询
 *
 * 接入点:
 * - [recordStudySession] 在卡片复习/知识点浏览时调用,更新 last_point_id + last_visited_at
 * - [observeProgress] 在 SettingsScreen 展示学习概览
 */
@Singleton
class StudyProgressRepository @Inject constructor(
    private val studyProgressDao: StudyProgressDao,
) {

    private companion object {
        private const val DEFAULT_ID = "default"
    }

    /**
     * 观察学习进度(单行记录)。
     *
     * 首次访问时自动创建初始记录(streak_days=0)。
     */
    fun observeProgress(): Flow<StudyProgressEntity> =
        studyProgressDao.observeById(DEFAULT_ID).map { entity ->
            entity ?: StudyProgressEntity(
                id = DEFAULT_ID,
                lastPointId = null,
                lastVisitedAt = null,
                totalStudyTime = 0,
                streakDays = 0,
                lastCheckIn = null,
            )
        }.catch { e ->
            // v0.8.21: Log.e → Timber.e（tag 自动推断为 "StudyProgressRepository"）
            Timber.e(e, "observeProgress failed")
            StudyProgressEntity(
                id = DEFAULT_ID,
                lastPointId = null,
                lastVisitedAt = null,
                totalStudyTime = 0,
                streakDays = 0,
                lastCheckIn = null,
            )
        }

    /**
     * 记录一次学习会话(P0 v0.7.2)。
     *
     * 在卡片复习评分 / 知识点浏览时调用:
     * - 更新 last_point_id + last_visited_at
     * - 按天计算 streak:若上次签到是昨天,streak+1;若是今天,不变;否则重置为 1
     *
     * @param pointId 当前学习的知识点 ID
     */
    suspend fun recordStudySession(pointId: String) {
        val now = System.currentTimeMillis()
        val current = studyProgressDao.getById(DEFAULT_ID)
        val currentStreak = current?.streakDays ?: 0
        val newStreak = calculateStreak(current?.lastCheckIn, now, currentStreak)
        val updated = StudyProgressEntity(
            id = DEFAULT_ID,
            lastPointId = pointId,
            lastVisitedAt = now,
            totalStudyTime = current?.totalStudyTime ?: 0,
            streakDays = newStreak,
            lastCheckIn = now,
        )
        studyProgressDao.upsert(updated)
    }

    /**
     * 累加学习时长(秒)。
     *
     * @param additionalSeconds 本次学习时长(秒)
     */
    suspend fun addStudyTime(additionalSeconds: Int) {
        if (additionalSeconds <= 0) return
        val current = studyProgressDao.getById(DEFAULT_ID)
        val updated = (current ?: StudyProgressEntity(
            id = DEFAULT_ID,
            lastPointId = null,
            lastVisitedAt = null,
            totalStudyTime = 0,
            streakDays = 0,
            lastCheckIn = null,
        )).copy(
            totalStudyTime = (current?.totalStudyTime ?: 0) + additionalSeconds,
        )
        studyProgressDao.upsert(updated)
    }

    /**
     * 计算连续学习天数。
     *
     * @param lastCheckIn    上次签到时间戳,null 表示首次
     * @param now            当前时间戳
     * @param currentStreak  当前连续天数(用于 +1 或保持)
     * @return 新的连续天数
     *
     * 规则:
     * - null(首次) → 1
     * - 同一天 → currentStreak(不变)
     * - 昨天签到过 → currentStreak + 1
     * - 早于昨天(中断) → 1
     *
     * 使用日期归零后比较,避免昨晚23:00→今晨01:00 被误判为同一天或中断。
     */
    private fun calculateStreak(lastCheckIn: Long?, now: Long, currentStreak: Int): Int {
        if (lastCheckIn == null) return 1

        val nowMidnight = toMidnightMillis(now)
        val lastMidnight = toMidnightMillis(lastCheckIn)
        val dayDiffMillis = nowMidnight - lastMidnight
        val dayDiff = (dayDiffMillis / (24 * 60 * 60 * 1000)).toInt()

        return when {
            dayDiff == 0 -> currentStreak // 同一天
            dayDiff == 1 -> currentStreak + 1 // 昨天签到过
            else -> 1 // 中断
        }
    }

    /** 将时间戳归零到当天 00:00:00,用于按自然日计算天数差 */
    private fun toMidnightMillis(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
