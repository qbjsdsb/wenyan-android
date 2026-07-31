package com.wenyan.app.feature.knowledge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [EssayDetailModels] JSON 解析单元测试(v0.9.8 论述题板块新增)。
 *
 * 覆盖 [parseEssayAngle] 和 [parseEssayNotes] 的:
 * - 完整 JSON 解析
 * - null/blank 输入返回 null
 * - 格式错误返回 null(优雅降级)
 * - 未知字段不破坏解析(ignoreUnknownKeys=true)
 * - 部分字段缺失时其余字段正常解析
 */
class EssayDetailModelsTest {

    // ── parseEssayAngle ───────────────────────────────────────

    @Test
    fun parseEssayAngle_null_returnsNull() {
        assertNull(parseEssayAngle(null))
    }

    @Test
    fun parseEssayAngle_blank_returnsNull() {
        assertNull(parseEssayAngle(""))
        assertNull(parseEssayAngle("   "))
    }

    @Test
    fun parseEssayAngle_completeJson_parsesAllFields() {
        val json = """
            {
              "questionType": "比较型",
              "coreKeywords": ["冰心", "丁玲", "萧红"],
              "limitKeywords": ["不同时期"],
              "task": "比较异同+梳理演变",
              "breakthroughAngles": ["①时代背景", "②女性意识"],
              "angleRationale": "四维度",
              "argumentPath": {
                "thesis": "总论点",
                "points": [
                  {"label": "分1", "content": "冰心"},
                  {"label": "分2", "content": "丁玲"}
                ],
                "conclusion": "结论"
              }
            }
        """.trimIndent()

        val angle = parseEssayAngle(json)
        assertNotNull(angle)
        assertEquals("比较型", angle!!.questionType)
        assertEquals(3, angle.coreKeywords?.size)
        assertEquals("冰心", angle.coreKeywords?.first())
        assertEquals(1, angle.limitKeywords?.size)
        assertEquals("比较异同+梳理演变", angle.task)
        assertEquals(2, angle.breakthroughAngles?.size)
        assertEquals("四维度", angle.angleRationale)
        assertNotNull(angle.argumentPath)
        assertEquals("总论点", angle.argumentPath?.thesis)
        assertEquals(2, angle.argumentPath?.points?.size)
        assertEquals("分1", angle.argumentPath?.points?.first()?.label)
        assertEquals("冰心", angle.argumentPath?.points?.first()?.content)
        assertEquals("结论", angle.argumentPath?.conclusion)
    }

    @Test
    fun parseEssayAngle_unknownFields_ignored() {
        val json = """
            {
              "questionType": "演变型",
              "unknownField1": "should be ignored",
              "unknownField2": 12345
            }
        """.trimIndent()

        val angle = parseEssayAngle(json)
        assertNotNull(angle)
        assertEquals("演变型", angle!!.questionType)
    }

    @Test
    fun parseEssayAngle_partialFields_parsesAvailable() {
        val json = """{"questionType": "评价型"}"""

        val angle = parseEssayAngle(json)
        assertNotNull(angle)
        assertEquals("评价型", angle!!.questionType)
        assertNull(angle.coreKeywords)
        assertNull(angle.task)
        assertNull(angle.argumentPath)
    }

    @Test
    fun parseEssayAngle_malformedJson_returnsNull() {
        assertNull(parseEssayAngle("{ this is not valid json"))
        assertNull(parseEssayAngle("}{}{"))
        assertNull(parseEssayAngle("just a string"))
    }

    @Test
    fun parseEssayAngle_emptyObject_allFieldsNull() {
        val angle = parseEssayAngle("{}")
        assertNotNull(angle)
        assertNull(angle!!.questionType)
        assertNull(angle.coreKeywords)
        assertNull(angle.argumentPath)
    }

    // ── parseEssayNotes ───────────────────────────────────────

    @Test
    fun parseEssayNotes_null_returnsNull() {
        assertNull(parseEssayNotes(null))
    }

    @Test
    fun parseEssayNotes_blank_returnsNull() {
        assertNull(parseEssayNotes(""))
        assertNull(parseEssayNotes("   "))
    }

    @Test
    fun parseEssayNotes_completeJson_parsesAllFields() {
        val json = """
            {
              "evidences": [
                {
                  "type": "WORK_TEXT",
                  "label": "作品原文",
                  "content": "原文内容...",
                  "source": "冰心《繁星·春水》",
                  "linkedKnowledgePointId": "kp_00595"
                },
                {
                  "type": "SCHOLAR_OPINION",
                  "label": "学者观点",
                  "content": "学者认为..."
                }
              ],
              "crossValidation": {
                "textbookComparison": "钱理群 vs 丁帆",
                "scholarComparison": "女性主义 vs 主流"
              },
              "referenceLinks": [
                {"label": "中国作家网·茹志鹃", "url": "https://www.example.com/1"},
                {"label": "国家哲学社科文献库", "url": "https://www.example.com/2"}
              ],
              "knowledgeGaps": [
                {"author": "萧红", "note": "建议补充萧红独立知识点"}
              ]
            }
        """.trimIndent()

        val notes = parseEssayNotes(json)
        assertNotNull(notes)
        assertEquals(2, notes!!.evidences?.size)
        assertEquals("WORK_TEXT", notes.evidences?.first()?.type)
        assertEquals("kp_00595", notes.evidences?.first()?.linkedKnowledgePointId)
        assertEquals("SCHOLAR_OPINION", notes.evidences?.last()?.type)
        assertNull(notes.evidences?.last()?.linkedKnowledgePointId)

        assertNotNull(notes.crossValidation)
        assertEquals("钱理群 vs 丁帆", notes.crossValidation?.textbookComparison)
        assertEquals("女性主义 vs 主流", notes.crossValidation?.scholarComparison)

        assertEquals(2, notes.referenceLinks?.size)
        assertEquals("https://www.example.com/1", notes.referenceLinks?.first()?.url)

        assertEquals(1, notes.knowledgeGaps?.size)
        assertEquals("萧红", notes.knowledgeGaps?.first()?.author)
    }

    @Test
    fun parseEssayNotes_unknownFields_ignored() {
        val json = """
            {
              "futureField": "unknown",
              "evidences": []
            }
        """.trimIndent()

        val notes = parseEssayNotes(json)
        assertNotNull(notes)
        assertEquals(0, notes!!.evidences?.size)
        assertNull(notes.crossValidation)
    }

    @Test
    fun parseEssayNotes_partialFields_parsesAvailable() {
        val json = """{"referenceLinks": [{"label": "链接", "url": "https://x.com"}]}"""

        val notes = parseEssayNotes(json)
        assertNotNull(notes)
        assertNull(notes!!.evidences)
        assertNull(notes.crossValidation)
        assertEquals(1, notes.referenceLinks?.size)
        assertEquals("链接", notes.referenceLinks?.first()?.label)
        assertNull(notes.knowledgeGaps)
    }

    @Test
    fun parseEssayNotes_malformedJson_returnsNull() {
        assertNull(parseEssayNotes("{ invalid"))
        assertNull(parseEssayNotes("}{}{"))
        assertNull(parseEssayNotes("12345"))
    }

    @Test
    fun parseEssayNotes_emptyObject_allFieldsNull() {
        val notes = parseEssayNotes("{}")
        assertNotNull(notes)
        assertNull(notes!!.evidences)
        assertNull(notes.crossValidation)
        assertNull(notes.referenceLinks)
        assertNull(notes.knowledgeGaps)
    }
}
