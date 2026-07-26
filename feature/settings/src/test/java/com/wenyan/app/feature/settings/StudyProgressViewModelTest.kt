package com.wenyan.app.feature.settings

import com.wenyan.app.core.data.repository.StudyProgressRepository
import com.wenyan.app.core.database.entity.StudyProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [StudyProgressViewModel] 单元测试(v0.8.13 P2-1 新增)。
 *
 * 覆盖:
 * - 初始 uiState 为 [StudyProgressUiState.Loading](DB 首条 emit 前)
 * - DB emit null(空表)时转为 [StudyProgressUiState.Loaded] 含默认空实体
 *   (而非 NoData——repository 已把 null 转为默认实体,见 [StudyProgressRepository.observeProgress])
 * - DB emit 非空实体时转为 [StudyProgressUiState.Loaded]
 *
 * 用 StandardTestDispatcher + advanceUntilIdle 控制协程执行时序,
 * 读 uiState.value 断言最终状态(与 KnowledgePointDetailViewModelTest 一致)。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudyProgressViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var dao: FakeStudyProgressDao

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        dao = FakeStudyProgressDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): StudyProgressViewModel =
        StudyProgressViewModel(StudyProgressRepository(dao))

    @Test
    fun uiState_initialValue_isLoading() = runTest(testDispatcher) {
        // 不 advanceUntilIdle,读初始值
        val viewModel = createViewModel()
        // 收集一次让 stateIn 启动(WhileSubscribed 需要 collector)
        backgroundScope.launch { viewModel.uiState.collect { } }
        // 初始值应为 Loading(还未 advanceUntilIdle,DB Flow 未 emit)
        assertTrue(
            "uiState should be Loading before first DB emit",
            viewModel.uiState.value is StudyProgressUiState.Loading,
        )
    }

    @Test
    fun uiState_dbEmitsNull_transitionsToLoadedWithDefaultEntity() = runTest(testDispatcher) {
        // DAO 返回 null(空表场景)
        dao.entity = null
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("should be Loaded", state is StudyProgressUiState.Loaded)
        val entity = (state as StudyProgressUiState.Loaded).entity
        // repository 把 null 转为默认实体(streak=0)
        assertEquals(0, entity.streakDays)
        assertEquals(null, entity.lastVisitedAt)
    }

    @Test
    fun uiState_dbEmitsEntity_transitionsToLoaded() = runTest(testDispatcher) {
        val progress = StudyProgressEntity(
            id = "default",
            lastPointId = "kp_1",
            lastVisitedAt = 1700000000000L,
            totalStudyTime = 7200,
            streakDays = 5,
            lastCheckIn = 1700000000000L,
        )
        dao.entity = progress
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("should be Loaded", state is StudyProgressUiState.Loaded)
        val entity = (state as StudyProgressUiState.Loaded).entity
        assertEquals(5, entity.streakDays)
        assertEquals(7200, entity.totalStudyTime)
        assertEquals("kp_1", entity.lastPointId)
    }

    @Test
    fun uiState_dbUpdates_reflectsNewValue() = runTest(testDispatcher) {
        // 初始空表
        dao.entity = null
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        // 确认初始 Loaded(默认空实体)
        assertEquals(0, (viewModel.uiState.value as StudyProgressUiState.Loaded).entity.streakDays)

        // 模拟用户学习后写入(streak=3)
        dao.entity = StudyProgressEntity(
            id = "default",
            lastPointId = "kp_42",
            lastVisitedAt = 1700000000000L,
            totalStudyTime = 3600,
            streakDays = 3,
            lastCheckIn = 1700000000000L,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("should remain Loaded", state is StudyProgressUiState.Loaded)
        val loaded = state as StudyProgressUiState.Loaded
        assertEquals(3, loaded.entity.streakDays)
        assertEquals("kp_42", loaded.entity.lastPointId)
    }
}
