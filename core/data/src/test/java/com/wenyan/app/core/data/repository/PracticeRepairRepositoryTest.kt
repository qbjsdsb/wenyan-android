package com.wenyan.app.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.DailyPlanEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PracticeRepairRepositoryTest {
    private lateinit var db: WenyanDatabase
    private lateinit var repository: PracticeRepairRepository

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), WenyanDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = PracticeRepairRepository(db)
        val sql = db.openHelper.writableDatabase
        sql.execSQL("INSERT INTO subjects(id,name,short_name,sort_order) VALUES('s','s','s',0)")
        sql.execSQL("INSERT INTO exam_questions(id,year,subject_id,question_type,content,score,created_at,exam_paper_code,content_status) VALUES('q',2025,'s','SHORT_ANSWER','q',10,1,'805','REVIEWED')")
        sql.execSQL("INSERT INTO practice_attempts(id,question_id,attempt_type,user_keywords,outline,body,started_at,completed_at,elapsed_ms,error_reasons,repair_state,created_at,updated_at) VALUES('a','q','EXAM_OUTLINE','','outline','',1000,1000,0,'[\"WEAK_STRUCTURE\"]','CANDIDATE',1000,1000)")
    }

    @After fun tearDown() = db.close()

    @Test fun `repair enters tomorrow once and never mutates today`() = runTest {
        db.dailyPlanDao().insertIfAbsent(DailyPlanEntity("today", "1970-01-01", 1, null, "{}", "v", "ACTIVE"))
        db.dailyPlanDao().insertIfAbsent(DailyPlanEntity("tomorrow", "1970-01-02", 1, null, "{}", "v", "ACTIVE"))

        val first = repository.scheduleForLaterDate("a", LocalDate.parse("1970-01-02"), 2000)
        val repeated = repository.scheduleForLaterDate("a", LocalDate.parse("1970-01-02"), 3000)

        assertEquals(first.id, repeated.id)
        assertEquals(0, db.dailyTaskDao().getByPlan("today").size)
        assertEquals(1, db.dailyTaskDao().getByPlan("tomorrow").size)
        assertEquals("REPAIR", first.taskType)
        assertEquals("q", first.contentId)
        assertEquals("SCHEDULED", db.practiceAttemptDao().getById("a")?.repairState)
    }

    @Test fun `same-day scheduling is rejected without partial writes`() = runTest {
        db.dailyPlanDao().insertIfAbsent(DailyPlanEntity("today", "1970-01-01", 1, null, "{}", "v", "ACTIVE"))
        val failure = runCatching {
            repository.scheduleForLaterDate("a", LocalDate.parse("1970-01-01"), 2000)
        }.exceptionOrNull()
        assertNotNull(failure)
        assertEquals(0, db.dailyTaskDao().getByPlan("today").size)
        assertEquals("CANDIDATE", db.practiceAttemptDao().getById("a")?.repairState)
    }
}
