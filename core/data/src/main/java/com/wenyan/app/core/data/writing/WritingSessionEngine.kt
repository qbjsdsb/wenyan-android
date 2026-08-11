package com.wenyan.app.core.data.writing

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

enum class WritingMode(val durationMs: Long) { OUTLINE_10_MIN(600_000), MICRO_30_MIN(1_800_000), FULL_TIMED(3_600_000) }
data class PersistedTimer(val startedAtWallMs: Long?, val elapsedBeforePauseMs: Long, val paused: Boolean)
fun elapsedMs(timer: PersistedTimer, nowWallMs: Long): Long = if (timer.paused || timer.startedAtWallMs == null) timer.elapsedBeforePauseMs else timer.elapsedBeforePauseMs + (nowWallMs - timer.startedAtWallMs).coerceAtLeast(0)
fun pause(timer: PersistedTimer, nowWallMs: Long) = PersistedTimer(null, elapsedMs(timer, nowWallMs), true)
fun resume(timer: PersistedTimer, nowWallMs: Long) = PersistedTimer(nowWallMs, timer.elapsedBeforePauseMs, false)
data class WritingDraft(val promptAnalysis: String, val thesis: String, val outlineJson: String, val evidenceRefsJson: String, val body: String)
fun WritingDraft.hasUnsavedContent() = listOf(promptAnalysis, thesis, outlineJson, evidenceRefsJson, body).any { it.isNotBlank() && it != "[]" && it != "{}" }

private val evidenceJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
fun decodeEvidenceRefs(value: String): List<String> = runCatching {
    evidenceJson.decodeFromString<List<String>>(value)
}.getOrDefault(emptyList()).distinct()
fun encodeEvidenceRefs(ids: List<String>): String = evidenceJson.encodeToString(ids.distinct())
