package com.wenyan.app.core.data.cards

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 卡片质量检查（用户要求"一张一张看，严谨仔细"）。
 *
 * 用真实 [CardSplitter] 对 seed_data.json 全部知识点实际拆卡，
 * 逐张检查：
 * - front/back 空白
 * - back 为占位文本（"见要点"等，AI 补全前不应出现在生产库）
 * - back 过短（<2 字）或超长（>500 字，拆卡失败整段压一张）
 * - front 格式异常（term 空、维度空）
 * - 拆卡数量分布（0 张 / 1 张 / 过多）
 * - 结构化标签解析截断问题（内容含后续标签文本）
 */
class CardQualityInspectionTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun findSeedFile(): File {
        val candidates = listOf(
            File("app/src/main/assets/seed_data.json"),
            File("../app/src/main/assets/seed_data.json"),
            File("../../app/src/main/assets/seed_data.json"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("找不到 seed_data.json（cwd=${System.getProperty("user.dir")}）")
    }

    private fun loadKnowledgePoints(): List<JsonObject> {
        val text = findSeedFile().readText()
        val root = json.parseToJsonElement(text).jsonObject
        val arr = root.getValue("knowledge_points") as kotlinx.serialization.json.JsonArray
        return arr.map { it.jsonObject }
    }

    @Test
    fun `逐张检查全部知识点拆卡质量`() {
        val kps = loadKnowledgePoints()
        assertTrue("知识点数应 >0", kps.isNotEmpty())
        println("知识点总数: ${kps.size}")

        var totalCards = 0
        val problems = mutableListOf<String>()
        val cardCountDist = mutableMapOf<Int, Int>()
        val singleCardPoints = mutableListOf<String>()
        val longBackCards = mutableListOf<String>()

        for (kp in kps) {
            val id = kp["id"]?.jsonPrimitive?.content ?: "?"
            val title = kp["title"]?.jsonPrimitive?.content ?: ""
            val summary = kp["summary"]?.jsonPrimitive?.content ?: ""
            val coreConclusion = kp["core_conclusion"]?.jsonPrimitive?.content ?: ""
            val studyText = kp["study_text"]?.jsonPrimitive?.content ?: ""
            val fullContent = kp["full_content"]?.jsonPrimitive?.content ?: ""

            // 与 CardRepositoryImpl.generateCardsFromKnowledgePoint 一致：名词解释拆卡
            val definition = fullContent.ifBlank { coreConclusion }
            val cards = if (definition.isNotBlank()) {
                CardSplitter.splitTermExplanation(
                    term = title,
                    definition = definition,
                    pointId = id,
                    fullExplanation = coreConclusion.takeIf { it.isNotBlank() && it != definition },
                    studyText = studyText,
                )
            } else {
                emptyList()
            }

            totalCards += cards.size
            cardCountDist[cards.size] = (cardCountDist[cards.size] ?: 0) + 1
            if (cards.size == 1) {
                singleCardPoints += "$id $title（full_content ${fullContent.length} 字）"
            }

            // --- 逐张检查 ---
            cards.forEachIndexed { idx, card ->
                val front = card.front
                val back = card.back
                val label = "[$id] $title 卡${idx + 1}/${cards.size}"

                if (front.isBlank()) problems += "$label: front 空白"
                if (back.isBlank()) problems += "$label: back 空白"
                if (front.length > 60) problems += "$label: front 超长(${front.length}字): $front"
                if (back.length < 2) problems += "$label: back 过短: '$back'"
                if (back.length > 500) {
                    problems += "$label: back 超长(${back.length}字)"
                    longBackCards += "$label（${back.length}字）: ${back.take(80)}..."
                }
                if (back.contains("见要点")) problems += "$label: back 为占位文本(见要点)"
                if (front.endsWith("— ")) problems += "$label: front 维度为空: '$front'"
                if (front.contains("null")) problems += "$label: front 含 null: '$front'"
            }

            // 论述要点卡检查（与 generateCardsFromKnowledgePoint 第 2 步一致）
            if (summary.isNotBlank()) {
                val keyPoints = summary.split('。', '；', ';', '！', '？', '!', '?', '\n')
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it.length >= 2 }
                if (keyPoints.isEmpty()) {
                    problems += "[$id] $title: 论述要点卡 keyPoints 为空（summary 无法切分）"
                }
            }
        }

        println("总名词解释卡: $totalCards")
        println("每知识点拆卡数分布: ${cardCountDist.toSortedMap()}")

        println("\n=== 只拆 1 张的知识点（${singleCardPoints.size} 个）===")
        singleCardPoints.take(40).forEach { println("  $it") }

        println("\n=== back 超长卡（>500 字，${longBackCards.size} 张）===")
        longBackCards.forEach { println("  $it") }

        if (problems.isEmpty()) {
            println("\n✅ 未发现其他质量问题")
        } else {
            println("\n❌ 问题汇总 ${problems.size} 条:")
            problems.take(80).forEach { println("  - $it") }
        }
    }
}
