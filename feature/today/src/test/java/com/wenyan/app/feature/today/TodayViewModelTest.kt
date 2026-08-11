package com.wenyan.app.feature.today

import app.cash.turbine.test
import com.wenyan.app.core.database.entity.DailyPlanWithTasks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TodayViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val date = LocalDate.of(2026, 8, 11)
    private val dateSource = object : TodayDateSource { override fun today() = date }

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `loading then empty consumes persisted source`() = runTest(dispatcher) {
        val values = MutableSharedFlow<DailyPlanWithTasks?>(extraBufferCapacity = 1)
        val viewModel = TodayViewModel(object : TodayPlanSource { override fun observe(date: String) = values }, dateSource)
        viewModel.uiState.test {
            assertTrue(awaitItem().isLoading)
            values.emit(null)
            val empty = awaitItem()
            assertEquals("2026-08-11", empty.date)
            assertTrue(empty.tasks.isEmpty())
        }
    }

    @Test fun `source failure becomes error state`() = runTest(dispatcher) {
        val viewModel = TodayViewModel(
            object : TodayPlanSource { override fun observe(date: String): Flow<DailyPlanWithTasks?> = flow { throw IllegalStateException("db") } },
            dateSource,
        )
        viewModel.uiState.test {
            assertTrue(awaitItem().isLoading)
            val next = awaitItem()
            val error = if (next.error != null) next else awaitItem()
            assertNotNull(error.error)
        }
    }
}
