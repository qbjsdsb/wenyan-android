package com.wenyan.app.core.fsrs

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * 学习阶段枚举
 *
 * 对应spec.md第282-287行（考研倒计时驱动保持率）。
 * 根据距考研天数划分三个学习阶段，每个阶段使用不同的全局保持率。
 *
 * @property BASIC     基础阶段：距考研>180天，全局保持率0.85
 * @property INTENSIVE 强化阶段：距考研90-180天，全局保持率0.90
 * @property SPRINT    冲刺阶段：距考研<90天，全局保持率0.95
 */
enum class StudyPhase {
    BASIC,
    INTENSIVE,
    SPRINT
}

/**
 * 考研倒计时管理器
 *
 * 对应Task 15全部三个子任务：
 * - SubTask 15.1: 考研倒计时驱动动态保持率（基础0.85→强化0.90→冲刺0.95）
 * - SubTask 15.2: 阶段切换平滑过渡（7天内逐步过渡，每天调整10%卡片）
 * - SubTask 15.3: 内容类型与全局保持率冲突处理（卡片级预设优先，取较高值不降级）
 *
 * 考研日期计算规则：每年12月倒数第二个周末（周六）。
 * 例如2026年考研日期为2026年12月某日（12月最后一个周六往前推一周）。
 */
object ExamCountdownManager {

    /** 基础阶段全局保持率 */
    const val RETENTION_BASIC = 0.85f

    /** 强化阶段全局保持率 */
    const val RETENTION_INTENSIVE = 0.90f

    /** 冲刺阶段全局保持率 */
    const val RETENTION_SPRINT = 0.95f

    /** 基础→强化阶段切换边界（天） */
    const val PHASE_BOUNDARY_INTENSIVE = 180

    /** 强化→冲刺阶段切换边界（天） */
    const val PHASE_BOUNDARY_SPRINT = 90

    /** 阶段切换平滑过渡天数 */
    const val TRANSITION_DAYS = 7

    // ===================== SubTask 15.1: 考研倒计时驱动动态保持率 =====================

    /**
     * 计算指定年份的考研日期（每年12月倒数第二个周六）
     *
     * 考研日期规则：12月最后一个周六往前推一周。
     * 例如：12月31日是周六 → 考研日期 = 12月31日 - 7天 = 12月24日
     *
     * @param year 年份
     * @return 该年考研日期（LocalDate）
     */
    fun getExamDate(year: Int): LocalDate {
        val dec31 = LocalDate.of(year, 12, 31)
        // 找到12月最后一个周六
        val lastSaturday = dec31.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        // 倒数第二个周六 = 最后一个周六 - 1周
        return lastSaturday.minusWeeks(1)
    }

    /**
     * 计算距考研的天数
     *
     * 自动判断使用当年还是次年考研日期：
     * - 如果今天的年份的考研日期尚未过去，使用当年的
     * - 如果今天的年份的考研日期已过去，使用次年的
     *
     * @param today 今天的日期
     * @return 距考研天数（正整数；考研当天返回0；已过返回负数但按0处理）
     */
    fun getDaysToExam(today: LocalDate): Int {
        val currentYearExamDate = getExamDate(today.year)
        val examDate = if (today.isAfter(currentYearExamDate)) {
            // 当年考研已过，使用次年考研日期
            getExamDate(today.year + 1)
        } else {
            currentYearExamDate
        }
        return ChronoUnit.DAYS.between(today, examDate).toInt().coerceAtLeast(0)
    }

    /**
     * 根据距考研天数获取学习阶段
     *
     * - 距考研>180天 → BASIC（基础阶段）
     * - 距考研90-180天 → INTENSIVE（强化阶段）
     * - 距考研<90天 → SPRINT（冲刺阶段）
     *
     * @param daysToExam 距考研天数
     * @return 学习阶段
     */
    fun getStudyPhase(daysToExam: Int): StudyPhase {
        return when {
            daysToExam > PHASE_BOUNDARY_INTENSIVE -> StudyPhase.BASIC
            daysToExam >= PHASE_BOUNDARY_SPRINT -> StudyPhase.INTENSIVE
            else -> StudyPhase.SPRINT
        }
    }

    /**
     * 根据距考研天数获取全局保持率
     *
     * - 基础阶段（>180天）→ 0.85
     * - 强化阶段（90-180天）→ 0.90
     * - 冲刺阶段（<90天）→ 0.95
     *
     * @param daysToExam 距考研天数
     * @return 全局保持率
     */
    fun getGlobalRetention(daysToExam: Int): Float {
        return when (getStudyPhase(daysToExam)) {
            StudyPhase.BASIC -> RETENTION_BASIC
            StudyPhase.INTENSIVE -> RETENTION_INTENSIVE
            StudyPhase.SPRINT -> RETENTION_SPRINT
        }
    }

    // ===================== SubTask 15.2: 阶段切换平滑过渡 =====================

    /**
     * 计算阶段切换过渡因子（0.0-1.0）
     *
     * 对应spec.md第289-293行（阶段切换边界平滑过渡）：
     * - 阶段切换边界（180天/90天）时不立即跳变所有卡片保持率
     * - 新调度卡片使用新保持率，已有卡片按原调度完成当前周期
     * - 切换后7天内逐步过渡（每天调整10%的卡片），避免复习量突增
     *
     * @param lastPhaseChangeDate 上次阶段切换日期（null表示从未切换或刚启动）
     * @param today               今天日期
     * @return 过渡因子（0.0=刚切换，无卡片使用新保持率；1.0=过渡完成，全部使用新保持率）
     *         7天内每天递增约1/7≈0.143（即每天约14.3%的卡片过渡，近似10%的调整幅度）
     */
    fun getTransitionFactor(lastPhaseChangeDate: LocalDate?, today: LocalDate): Float {
        if (lastPhaseChangeDate == null) {
            // 从未切换过，返回1.0（全部使用当前阶段保持率）
            return 1.0f
        }
        val daysSinceChange = ChronoUnit.DAYS.between(lastPhaseChangeDate, today).toInt()
        if (daysSinceChange >= TRANSITION_DAYS) {
            // 过渡期已过（7天以上），返回1.0（全部使用新保持率）
            return 1.0f
        }
        if (daysSinceChange <= 0) {
            // 刚切换或未来日期，返回0.0（无卡片使用新保持率）
            return 0.0f
        }
        // 7天内线性过渡：第1天≈0.143，第2天≈0.286，...，第7天=1.0
        return daysSinceChange.toFloat() / TRANSITION_DAYS
    }

    /**
     * 获取平滑过渡后的有效全局保持率
     *
     * 结合过渡因子，在旧保持率和新保持率之间线性插值。
     * transitionFactor=0时返回旧保持率，transitionFactor=1时返回新保持率。
     *
     * @param oldRetention        旧阶段的全局保持率
     * @param newRetention        新阶段的全局保持率
     * @param transitionFactor    过渡因子（0.0-1.0）
     * @return 有效全局保持率（在旧值和新值之间线性插值）
     */
    fun getTransitionedRetention(
        oldRetention: Float,
        newRetention: Float,
        transitionFactor: Float
    ): Float {
        val factor = transitionFactor.coerceIn(0f, 1f)
        return oldRetention + (newRetention - oldRetention) * factor
    }

    /**
     * 判断今天是否是阶段切换日
     *
     * 用于检测是否需要记录新的阶段切换日期。
     * 当当前阶段与昨天的阶段不同时，今天是阶段切换日。
     *
     * @param today 今天的日期
     * @return true表示今天是阶段切换日（需要记录lastPhaseChangeDate）
     */
    fun isPhaseChangeDay(today: LocalDate): Boolean {
        val yesterday = today.minusDays(1)
        val todayDays = getDaysToExam(today)
        val yesterdayDays = getDaysToExam(yesterday)
        return getStudyPhase(todayDays) != getStudyPhase(yesterdayDays)
    }

    // ===================== SubTask 15.3: 内容类型与全局保持率冲突处理 =====================

    /**
     * 解决卡片级档位保持率与全局保持率的冲突
     *
     * 对应spec.md第301-305行（内容类型与全局保持率冲突）：
     * - 卡片级预设优先于全局保持率
     * - 作品背诵卡片(0.95)在基础阶段(全局0.85) → 取较高值0.95，不降级
     * - 全局保持率仅作为未指定类型卡片的默认值
     *
     * 与FsrsWrapper.resolveRetention方法逻辑一致，此处提供独立入口方便非Wrapper场景调用。
     *
     * @param cardTier        卡片所属档位
     * @param globalRetention 全局保持率（由考研倒计时阶段决定）
     * @return 最终使用的保持率（取较高值，卡片级档位优先，不降级）
     */
    fun resolveRetention(cardTier: MemoryTier, globalRetention: Float): Float {
        val cardRetention = TIER_CONFIGS[cardTier]?.targetRetention ?: 0.90f
        // 取较高值，卡片级档位优先，不降级
        return maxOf(cardRetention, globalRetention)
    }

    /**
     * 获取卡片的有效保持率（综合档位、全局保持率和平滑过渡）
     *
     * 完整的保持率决策流程：
     * 1. 获取当前阶段的过渡后全局保持率
     * 2. 与卡片档位保持率取较高值（卡片级优先，不降级）
     *
     * @param cardTier             卡片所属档位
     * @param today                今天日期
     * @param lastPhaseChangeDate  上次阶段切换日期
     * @return 卡片的有效保持率
     */
    fun getEffectiveRetention(
        cardTier: MemoryTier,
        today: LocalDate,
        lastPhaseChangeDate: LocalDate? = null
    ): Float {
        val daysToExam = getDaysToExam(today)
        val currentPhase = getStudyPhase(daysToExam)
        val newGlobalRetention = getGlobalRetention(daysToExam)

        // 如果有阶段切换记录，计算过渡后的全局保持率
        val effectiveGlobalRetention = if (lastPhaseChangeDate != null) {
            val transitionFactor = getTransitionFactor(lastPhaseChangeDate, today)
            if (transitionFactor < 1.0f) {
                // 直接计算阶段切换前一天的实际阶段作为"旧阶段"
                // 这能正确处理前进(BASIC→INTENSIVE→SPRINT)和回退(SPRINT→BASIC跨年)两种场景
                val dayBeforeChange = lastPhaseChangeDate.minusDays(1)
                val oldPhase = getStudyPhase(getDaysToExam(dayBeforeChange))
                val oldGlobalRetention = getRetentionForPhase(oldPhase)
                getTransitionedRetention(oldGlobalRetention, newGlobalRetention, transitionFactor)
            } else {
                newGlobalRetention
            }
        } else {
            newGlobalRetention
        }

        // 卡片级档位保持率与全局保持率取较高值（不降级）
        return resolveRetention(cardTier, effectiveGlobalRetention)
    }

    // ===================== 内部工具方法 =====================

    /**
     * 获取指定阶段对应的全局保持率
     */
    private fun getRetentionForPhase(phase: StudyPhase): Float {
        return when (phase) {
            StudyPhase.BASIC -> RETENTION_BASIC
            StudyPhase.INTENSIVE -> RETENTION_INTENSIVE
            StudyPhase.SPRINT -> RETENTION_SPRINT
        }
    }
}
