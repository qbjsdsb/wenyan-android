package com.wenyan.app.core.data.graph

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PrerequisiteChecker 单元测试（指向 main 源码）。
 *
 * 验证 checklist 项：
 * - C4.5: 验证prerequisites字段检测（节点有前置依赖时返回非空列表）
 * - C4.6: 验证学习新卡前检查前置节点可提取性R
 * - C4.7: 验证R<0.7时canStartLearning=false，blockedBy非空
 * - C4.8: 验证R≥0.7时canStartLearning=true，blockedBy为空
 * - C4.9: 验证"江西诗派"案例（前置黄庭坚/杜甫/宋诗特点的R值检查）
 * - 验证generateReviewQueueForPrerequisites返回blockedBy列表
 *
 * 使用 [FakeGraphRepository] 实现 main 的 [GraphRepository] 接口，
 * 通过 retrievabilityMap 模拟 FSRS 可提取性 R 值。
 */
class PrerequisiteCheckerTest {

    // C4.5: 验证prerequisites字段检测（节点有前置依赖时返回非空列表）
    @Test
    fun c4_5_prerequisites_nonEmpty_whenNodeHasPrerequisites() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("jiangxi_school", "江西诗派", prerequisites = listOf("huangtingjian", "dufu")),
                testNode("huangtingjian", "黄庭坚"),
                testNode("dufu", "杜甫"),
            ),
            retrievabilityMap = mapOf(
                "huangtingjian" to 0.8f,
                "dufu" to 0.9f,
            ),
        )
        val checker = PrerequisiteChecker(repo)

        val result = checker.checkPrerequisites("jiangxi_school").first()

        assertTrue("有前置依赖时 prerequisites 应为非空列表", result.prerequisites.isNotEmpty())
        assertEquals(2, result.prerequisites.size)
    }

    // C4.5 补充：无前置依赖时返回空列表
    @Test
    fun c4_5_prerequisites_empty_whenNodeHasNoPrerequisites() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("dufu", "杜甫"),
            ),
        )
        val checker = PrerequisiteChecker(repo)

        val result = checker.checkPrerequisites("dufu").first()

        assertTrue("无前置依赖时 prerequisites 应为空列表", result.prerequisites.isEmpty())
    }

    // C4.6: 验证学习新卡前检查前置节点可提取性R
    @Test
    fun c4_6_checksRetrievability_beforeLearningNewCard() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("target", "目标节点", prerequisites = listOf("prereq1", "prereq2")),
                testNode("prereq1", "前置节点1"),
                testNode("prereq2", "前置节点2"),
            ),
            retrievabilityMap = mapOf(
                "prereq1" to 0.5f,
                "prereq2" to 0.9f,
            ),
        )
        val checker = PrerequisiteChecker(repo)

        val result = checker.checkPrerequisites("target").first()

        // 验证每个前置节点都有 retrievability 值
        for (prereq in result.prerequisites) {
            assertTrue("前置节点应包含 retrievability 值", prereq.retrievability >= 0f)
        }
        // 验证 needsReview 字段正确反映 R 值
        val lowRPrereq = result.prerequisites.find { it.nodeId == "prereq1" }!!
        assertTrue("R < 阈值时 needsReview 应为 true", lowRPrereq.needsReview)
        val highRPrereq = result.prerequisites.find { it.nodeId == "prereq2" }!!
        assertFalse("R ≥ 阈值时 needsReview 应为 false", highRPrereq.needsReview)
    }

    // C4.7: 验证R<0.7时canStartLearning=false，blockedBy非空
    @Test
    fun c4_7_canStartLearning_false_whenRetrievabilityBelowThreshold() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("target", "目标节点", prerequisites = listOf("prereq_low")),
                testNode("prereq_low", "低R前置节点"),
            ),
            retrievabilityMap = mapOf("prereq_low" to 0.3f),
        )
        val checker = PrerequisiteChecker(repo)

        val result = checker.checkPrerequisites("target").first()

        assertFalse("R < 0.7 时 canStartLearning 应为 false", result.canStartLearning)
        assertTrue("R < 0.7 时 blockedBy 应非空", result.blockedBy.isNotEmpty())
        assertTrue("blockedBy 应包含低R节点", result.blockedBy.contains("prereq_low"))
    }

    // C4.8: 验证R≥0.7时canStartLearning=true，blockedBy为空
    @Test
    fun c4_8_canStartLearning_true_whenRetrievabilityAtOrAboveThreshold() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("target", "目标节点", prerequisites = listOf("prereq_high")),
                testNode("prereq_high", "高R前置节点"),
            ),
            retrievabilityMap = mapOf("prereq_high" to 0.75f),
        )
        val checker = PrerequisiteChecker(repo)

        val result = checker.checkPrerequisites("target").first()

        assertTrue("R ≥ 0.7 时 canStartLearning 应为 true", result.canStartLearning)
        assertTrue("R ≥ 0.7 时 blockedBy 应为空", result.blockedBy.isEmpty())
    }

    // C4.8 边界：R=0.7（恰好等于阈值）时 canStartLearning=true
    @Test
    fun c4_8_boundary_retrievabilityExactlyAtThreshold() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("target", "目标节点", prerequisites = listOf("prereq_exact")),
                testNode("prereq_exact", "阈值前置节点"),
            ),
            retrievabilityMap = mapOf("prereq_exact" to 0.7f),
        )
        val checker = PrerequisiteChecker(repo)

        val result = checker.checkPrerequisites("target").first()

        assertTrue("R = 0.7（等于阈值）时 canStartLearning 应为 true", result.canStartLearning)
        assertTrue("R = 0.7 时 blockedBy 应为空", result.blockedBy.isEmpty())
    }

    // C4.9: 验证"江西诗派"案例（前置黄庭坚/杜甫/宋诗特点的R值检查）
    @Test
    fun c4_9_jiangxiSchoolCase_prerequisitesIncludeHuangTingjianAndDuFu() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("jiangxi_school", "江西诗派", prerequisites = listOf("huangtingjian", "dufu", "song_poetry")),
                testNode("huangtingjian", "黄庭坚"),
                testNode("dufu", "杜甫"),
                testNode("song_poetry", "宋诗特点"),
            ),
            retrievabilityMap = mapOf(
                "huangtingjian" to 0.8f,
                "dufu" to 0.6f,
                "song_poetry" to 0.5f,
            ),
        )
        val checker = PrerequisiteChecker(repo)

        val result = checker.checkPrerequisites("jiangxi_school").first()

        // 验证前置包含黄庭坚、杜甫、宋诗特点
        val prereqIds = result.prerequisites.map { it.nodeId }
        assertTrue("江西诗派前置应包含黄庭坚", prereqIds.contains("huangtingjian"))
        assertTrue("江西诗派前置应包含杜甫", prereqIds.contains("dufu"))
        assertTrue("江西诗派前置应包含宋诗特点", prereqIds.contains("song_poetry"))

        // 杜甫 R=0.6 < 0.7，应被阻止
        assertTrue("杜甫 R<0.7 应在 blockedBy 中", result.blockedBy.contains("dufu"))
        // 宋诗特点 R=0.5 < 0.7，应被阻止
        assertTrue("宋诗特点 R<0.7 应在 blockedBy 中", result.blockedBy.contains("song_poetry"))
        // 黄庭坚 R=0.8 ≥ 0.7，不应被阻止
        assertFalse("黄庭坚 R≥0.7 不应在 blockedBy 中", result.blockedBy.contains("huangtingjian"))
        // 有被阻止的前置，不能开始学习
        assertFalse("有前置R<0.7时 canStartLearning 应为 false", result.canStartLearning)
    }

    // 验证 generateReviewQueueForPrerequisites 返回 blockedBy 列表
    @Test
    fun generateReviewQueueForPrerequisites_returnsBlockedByList() = runBlocking {
        val repo = FakeGraphRepository(
            nodes = listOf(
                testNode("target", "目标节点", prerequisites = listOf("prereq1", "prereq2", "prereq3")),
                testNode("prereq1", "前置1"),
                testNode("prereq2", "前置2"),
                testNode("prereq3", "前置3"),
            ),
            retrievabilityMap = mapOf(
                "prereq1" to 0.3f,
                "prereq2" to 0.5f,
                "prereq3" to 0.9f,
            ),
        )
        val checker = PrerequisiteChecker(repo)

        val reviewQueue = checker.generateReviewQueueForPrerequisites("target").first()

        // prereq1(R=0.3) 和 prereq2(R=0.5) 低于阈值，应在复习队列中
        assertEquals(2, reviewQueue.size)
        assertTrue(reviewQueue.contains("prereq1"))
        assertTrue(reviewQueue.contains("prereq2"))
        // prereq3(R=0.9) 高于阈值，不在复习队列中
        assertFalse(reviewQueue.contains("prereq3"))
    }

    // 验证 RETRIEVABILITY_THRESHOLD 常量值
    @Test
    fun retrievabilityThreshold_constantIsCorrect() {
        assertEquals(0.7f, PrerequisiteChecker.RETRIEVABILITY_THRESHOLD, 0.001f)
    }
}
