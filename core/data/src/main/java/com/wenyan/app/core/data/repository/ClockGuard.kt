package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.AppMetaDao
import com.wenyan.app.core.database.entity.AppMetaEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 时钟守卫接口（v0.9.5 follow-up #1 提取接口,便于 ViewModel 层注入与测试替换）。
 *
 * 问题：[SchedulingRepository] 用 `System.currentTimeMillis()`
 * / `LocalDateTime.now()` 计算 FSRS 到期与可提取性 R，用户改系统时间（手动调时 /
 * 时区切换 / NTP 异常）会导致：
 * - **时钟回拨**：FSRS 认为卡片刚复习过 → 间隔变短 → 卡片"永久消失"（nextReviewAt 在过去）
 * - **时钟前移**：FSRS 认为卡片已过期很久 → 间隔变长 → 卡片"无限到期"
 *   （nextReviewAt 在远未来，永远不进入复习队列）
 *
 * 修复策略：在 app_meta 表存 `last_known_timestamp_ms`，每次取当前时间时：
 * 1. 读 lastKnown（首次启动为 null，跳过检查）
 * 2. current = System.currentTimeMillis()
 * 3. 若 `current < lastKnown - [TOLERANCE_MS]`（回拨超 1 分钟，超过 NTP 正常波动）：
 *    - Log.w 告警 + 返回 lastKnown（不让时钟倒退）
 * 4. 否则（正常推进 / 前移 / 微小回拨）：
 *    - upsert lastKnown = current（更新已知最大时间）
 *    - 返回 current
 *
 * v0.9.5 follow-up #1: 提取为接口,使 [WrongAnswerViewModel] 可注入 ClockGuard
 * (DUE 过滤时间源与 [SchedulingRepository.rateWrongAnswer] 对齐),且测试可用
 * FakeClockGuard 替换,无需依赖 Room AppMetaDao。
 *
 * @see ClockGuardImpl 生产实现
 */
interface ClockGuard {

    /**
     * 返回当前有效时间戳（millis）。
     *
     * 检测时钟回拨：若 current < lastKnown - TOLERANCE_MS，返回 lastKnown + 1 并更新 DB；
     * 否则更新 lastKnown = current 并返回 current。
     *
     * P1-AUDIT-2 修正：回拨期间返回 `lastKnown + 1`（而非固定 `lastKnown`）并更新 DB。
     * 原实现返回固定 `lastKnown` 且不更新 DB,导致回拨期间多次调用 `effectiveNowMillis()`
     * 返回相同值 → FSRS 计算 `elapsedDays = daysBetween(lastReview, now)` 时
     * `now == lastReview == lastKnown` → `elapsedDays = 0`,误判卡片"刚复习过"。
     *
     * 修正后每次调用返回 `lastKnown + 1` 并写入 DB,确保连续调用严格单调递增。
     * 1ms 递增对 FSRS `elapsedDays`（以天为单位）无影响,但避免了 `elapsedDays = 0` 的误判。
     *
     * @return 单调不减的有效时间戳（回拨期间每次调用递增 1ms）
     */
    suspend fun effectiveNowMillis(): Long
}

/**
 * [ClockGuard] 生产实现（v0.9.5 follow-up #1 从原 ClockGuard 类重命名）。
 *
 * @property appMetaDao 应用元数据 DAO（读写 last_known_timestamp_ms）
 */
@Singleton
class ClockGuardImpl @Inject constructor(
    private val appMetaDao: AppMetaDao,
) : ClockGuard {

    override suspend fun effectiveNowMillis(): Long {
        val current = System.currentTimeMillis()
        val lastKnown = appMetaDao.getByKey(KEY_LAST_KNOWN_TS)?.longValue
        return if (lastKnown != null && current < lastKnown - TOLERANCE_MS) {
            // P1-AUDIT-2 修正：lastKnown + 1 确保单调递增,避免回拨期间 elapsedDays = 0
            val effective = lastKnown + 1
            // v0.8.21: Log.w → Timber.w（tag 自动推断为 "ClockGuardImpl"）
            Timber.w(
                "Clock rollback detected: current=$current, lastKnown=$lastKnown " +
                    "(rollback=${lastKnown - current}ms > tolerance=$TOLERANCE_MS ms), " +
                    "using effective=$effective (lastKnown+1 for monotonic increment)",
            )
            appMetaDao.upsert(
                AppMetaEntity(
                    key = KEY_LAST_KNOWN_TS,
                    longValue = effective,
                    stringValue = null,
                ),
            )
            effective
        } else {
            appMetaDao.upsert(
                AppMetaEntity(
                    key = KEY_LAST_KNOWN_TS,
                    longValue = current,
                    stringValue = null,
                ),
            )
            current
        }
    }

    companion object {
        private const val KEY_LAST_KNOWN_TS = "last_known_timestamp_ms"

        /** 时钟回拨容差（ms）：超过此值视为异常回拨，NTP 正常波动 < 1 分钟 */
        private const val TOLERANCE_MS = 60_000L
    }
}
