package com.wenyan.app.feature.today

import androidx.compose.runtime.Immutable
import com.wenyan.app.core.database.entity.DailyPlanWithTasks
import com.wenyan.app.core.database.entity.DailyTaskEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class TodayGroup { DUE, REPAIR, NEW, OUTPUT, WRITING, OTHER }
enum class TodayDestination { CARDS, QUIZ, WRITING_MATERIALS }

@Immutable
data class TodayTaskUi(
    val id: String,
    val title: String,
    val group: TodayGroup,
    val estimatedMinutes: Int,
    val completed: Boolean,
    val destination: TodayDestination,
    val contentId: String?,
)

@Immutable
data class TodayUiState(
    val isLoading: Boolean = false,
    val date: String = "",
    val countdownDays: Long? = null,
    val estimatedMinutes: Int = 0,
    val tasks: List<TodayTaskUi> = emptyList(),
    val isFinished: Boolean = false,
    val infeasibleMessage: String? = null,
    val error: String? = null,
) {
    val nextTask: TodayTaskUi? get() = tasks.firstOrNull { !it.completed }
}

internal object TodayPlanMapper {
    private val examDatePattern = Regex("\\\"examDate\\\"\\s*:\\s*\\\"(\\d{4}-\\d{2}-\\d{2})\\\"")

    fun map(value: DailyPlanWithTasks, today: LocalDate): TodayUiState {
        val visible = value.tasks.filter { it.status == "PENDING" || it.status == "DONE" }
        val tasks = visible.map(::mapTask)
        val examDate = examDatePattern.find(value.plan.settingsSnapshot)?.groupValues?.get(1)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return TodayUiState(
            date = value.plan.planDate,
            countdownDays = examDate?.let { ChronoUnit.DAYS.between(today, it).coerceAtLeast(0) },
            estimatedMinutes = tasks.filterNot { it.completed }.sumOf { it.estimatedMinutes },
            tasks = tasks,
            isFinished = tasks.isNotEmpty() && tasks.all { it.completed },
            infeasibleMessage = infeasibleMessage(value.plan.status),
        )
    }

    private fun infeasibleMessage(status: String): String? {
        if (!status.startsWith("INFEASIBLE")) return null
        val issue = status.substringAfter(':', "")
        return when {
            "EXAM_PASSED" in issue -> "考试日期已过，今日计划未生成"
            "ZERO_QUOTA" in issue -> "今日任务配额为 0，请调整卡片设置"
            "NO_TRUSTED_NEW_CONTENT" in issue -> "暂无可信的新内容，先复习已有知识点"
            "OUTPUT_UNAVAILABLE" in issue -> "当前没有可用的今日学习内容"
            else -> issue.ifBlank { "今日计划无法满足全部约束" }
        }
    }

    private fun mapTask(task: DailyTaskEntity): TodayTaskUi {
        val group = when (task.taskType) {
            "DUE" -> TodayGroup.DUE
            "REPAIR" -> TodayGroup.REPAIR
            "NEW" -> TodayGroup.NEW
            "OUTPUT", "SPECIAL_SESSION" -> TodayGroup.OUTPUT
            "WRITING" -> TodayGroup.WRITING
            else -> TodayGroup.OTHER
        }
        val destination = when (group) {
            TodayGroup.DUE, TodayGroup.REPAIR, TodayGroup.NEW -> TodayDestination.CARDS
            TodayGroup.OUTPUT -> TodayDestination.QUIZ
            TodayGroup.WRITING, TodayGroup.OTHER -> TodayDestination.WRITING_MATERIALS
        }
        return TodayTaskUi(
            id = task.id,
            title = displayTitle(task),
            group = group,
            estimatedMinutes = task.estimatedMinutes,
            completed = task.status == "DONE",
            destination = destination,
            contentId = task.contentId,
        )
    }

    private fun displayTitle(task: DailyTaskEntity): String {
        val label = when (task.taskType) {
            "DUE" -> "到期复习"
            "REPAIR" -> "遗忘修复"
            "NEW" -> "新学内容"
            "OUTPUT", "SPECIAL_SESSION" -> "输出训练"
            "WRITING" -> "专业写作"
            else -> null
        }
        return if (label != null && task.stableId.startsWith("card:")) {
            listOfNotNull(label, task.contentId).joinToString(" · ")
        } else {
            task.stableId
        }
    }
}
