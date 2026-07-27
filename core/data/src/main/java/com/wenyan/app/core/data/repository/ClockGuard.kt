package com.wenyan.app.core.data.repository

import com.wenyan.app.core.database.dao.AppMetaDao
import com.wenyan.app.core.database.entity.AppMetaEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 时钟守卫（NF-B / P0-E4 修复：FSRS 时钟回拨防护）。
 *
 * 问题：[SchedulingRepository] 与 [GraphRepositoryImpl] 用 `System.currentTimeMillis()`
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
 * 容差 [TOLERANCE_MS] = 60 秒：NTP 同步正常波动 < 1 分钟，超出视为异常回拨。
 *
 * 不变性：
 * - lastKnown 单调不减（仅在 current > lastKnown - TOLERANCE 时更新为 current）
 * - 时钟前移时 lastKnown 跟进，下次回拨到此前移前的时刻会被检测为回拨（保守）
 *
 * 不实现 SystemClock.elapsedRealtime() 方案的理由：
 * - elapsedRealtime 不受系统时间调整影响，但**重启后归零**，无法跨重启持久化
 * - 跨重启的 FSRS 调度需要绝对时间（nextReviewAt 存的是 millis），用 elapsedRealtime
 *   需额外记录 boot 时间映射，复杂度高于 lastKnown 方案
 *
 * @property appMetaDao 应用元数据 DAO（读写 last_known_timestamp_ms）
 */
@Singleton
class ClockGuard @Inject constructor(
    private val appMetaDao: AppMetaDao,
) {
    /**
     * 返回当前有效时间戳（millis）。
     *
     * 检测时钟回拨：若 current < lastKnown - [TOLERANCE_MS]，返回 lastKnown + 1 并更新 DB；
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
    suspend fun effectiveNowMillis(): Long {
        val current = System.currentTimeMillis()
        val lastKnown = appMetaDao.getByKey(KEY_LAST_KNOWN_TS)?.longValue
        return if (lastKnown != null && current < lastKnown - TOLERANCE_MS) {
            // P1-AUDIT-2 修正：lastKnown + 1 确保单调递增,避免回拨期间 elapsedDays = 0
            val effective = lastKnown + 1
            // v0.8.21: Log.w → Timber.w（tag 自动推断为 "ClockGuard"）
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
