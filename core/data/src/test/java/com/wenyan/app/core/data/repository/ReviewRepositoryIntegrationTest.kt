package com.wenyan.app.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.ChapterEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 覆盖种子预建 MemoRecord → Room DAO → ReviewRepository 的真实跨层语义。
 *
 * 这条链路不能只靠 selectNewPoints 纯函数测试：首装时每个知识点本来就有 memo_record，
 * 若 DAO 与 Repository 对 NEW 的定义不一致，会让全部知识点绕过每日新卡限额。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReviewRepositoryIntegrationTest {

    private lateinit var db: WenyanDatabase
    private lateinit var repository: ReviewRepository
    private lateinit var externalScope: CoroutineScope

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WenyanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

        repository = ReviewRepository(
            knowledgePointDao = db.knowledgePointDao(),
            memoRecordDao = db.memoRecordDao(),
            cardSettingsRepository = TestCardSettingsRepository(
                CardSettings(
                    dailyNewLimit = 6,
                    frequencyFilter = CardFrequencyFilter.ALL,
                ),
            ),
            externalScope = externalScope,
        )

        db.subjectDao().insert(
            SubjectEntity(
                id = "subject",
                name = "中国古代文学",
                shortName = "古文",
                sortOrder = 1,
            ),
        )
        db.chapterDao().insert(
            ChapterEntity(
                id = "chapter",
                subjectId = "subject",
                parentId = null,
                title = "测试章节",
                sortOrder = 1,
            ),
        )
        db.knowledgePointDao().insertAll(
            listOf(
                knowledgePoint("new"),
                knowledgePoint("review"),
                knowledgePoint("legacy"),
            ),
        )
        db.memoRecordDao().insertAll(
            listOf(
                // 兼容旧版 seed：有记录且 lastReviewAt 是安装时间，但从未评分。
                memo("new", lastReviewAt = 100L),
                memo("review", state = "REVIEW", reps = 1, reviewCount = 1, lastReviewAt = 100L),
                // 兼容旧版本可能遗留的字段不一致：state/reps 仍为 NEW/0，但有真实学习痕迹。
                memo("legacy", reviewCount = 1, lastReviewAt = 100L),
            ),
        )
    }

    @After
    fun teardown() {
        externalScope.cancel()
        db.close()
    }

    @Test
    fun `首装预建 NEW 记录受每日限额控制且不进入到期复习`() = runTest {
        repository.getTodayStudyQueue().test {
            var queue = awaitItem()
            // stateIn 会先发射空初值，随后由 Room 真实查询替换。
            if (queue.totalPoints == 0) queue = awaitItem()
            assertEquals(setOf("review", "legacy"), queue.duePoints.mapTo(mutableSetOf()) { it.id })
            assertEquals(listOf("new"), queue.newPoints.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }

        repository.getReviewQueue().test {
            assertEquals(setOf("review", "legacy"), awaitItem().mapTo(mutableSetOf()) { it.id })
            cancelAndIgnoreRemainingEvents()
        }

        repository.getPendingReviewCount().test {
            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `学习进度只统计有真实学习痕迹的 VERIFIED 知识点`() = runTest {
        repository.getStudyProgress().test {
            assertEquals(StudyProgress(learnedPoints = 2, totalVerifiedPoints = 3), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun knowledgePoint(id: String) = KnowledgePointEntity(
        id = id,
        chapterId = "chapter",
        title = "知识点$id",
        summary = null,
        coreConclusion = "核心结论$id",
        fullContent = "完整内容$id",
        multiPerspectives = null,
        relatedIds = null,
        contrastIds = null,
        extensionIds = null,
        examRecords = null,
        examFrequency = "HIGH",
        termTemplate = null,
        tags = null,
        difficulty = 3,
        createdAt = 1L,
        updatedAt = 1L,
        contentSource = "TEXTBOOK_NATIVE",
        ocrStatus = "VERIFIED",
        sourceFile = null,
        sourcePage = null,
        studyText = null,
    )

    private fun memo(
        id: String,
        state: String = "NEW",
        reps: Int = 0,
        reviewCount: Int = 0,
        lastReviewAt: Long = 0L,
    ) = MemoRecordEntity(
        pointId = id,
        state = state,
        lastReviewAt = lastReviewAt,
        nextReviewAt = 1L,
        reps = reps,
        reviewCount = reviewCount,
    )
}

private class TestCardSettingsRepository(initial: CardSettings) : CardSettingsRepository {
    private val state = MutableStateFlow(initial)
    override val cardSettings: Flow<CardSettings> = state

    override suspend fun setDailyNewLimit(limit: Int) {
        state.value = state.value.copy(dailyNewLimit = limit)
    }

    override suspend fun setFrequencyFilter(filter: CardFrequencyFilter) {
        state.value = state.value.copy(frequencyFilter = filter)
    }

    override suspend fun setSubjectFilters(subjects: Set<String>) {
        state.value = state.value.copy(subjectFilters = subjects)
    }

    override suspend fun setExamDate(millis: Long?) {
        state.value = state.value.copy(examDateMillis = millis)
    }
}
