package com.wenyan.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [CardSettingsRepositoryImpl] 单元测试（v0.9.29 卡片备考系统）。
 *
 * 覆盖：
 * - 默认值：dailyNewLimit=60 / HIGH_MEDIUM / 四科全选 / examDate=null
 * - setDailyNewLimit 持久化 + 越界 clamp（10-200）
 * - setFrequencyFilter 持久化
 * - setSubjectFilters 持久化（空集回退默认四科）
 * - setExamDate 持久化 + null 清除
 * - 非法考频值容错（DataStore 被写入非法枚举名 → 降级 HIGH_MEDIUM）
 */
class CardSettingsRepositoryImplTest {

    @Test
    fun `默认设置返回 60 张 HIGH_MEDIUM 四科全选 无考试日期`() = runTest {
        val repo = CardSettingsRepositoryImpl(FakeDataStore())
        val settings = repo.cardSettings.first()

        assertEquals(60, settings.dailyNewLimit)
        assertEquals(CardFrequencyFilter.HIGH_MEDIUM, settings.frequencyFilter)
        assertEquals(CardSettings.DEFAULT_SUBJECTS, settings.subjectFilters)
        assertNull(settings.examDateMillis)
    }

    @Test
    fun `setDailyNewLimit 持久化自定义值`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = CardSettingsRepositoryImpl(fakeStore)

        repo.setDailyNewLimit(80)
        val settings = repo.cardSettings.first()
        assertEquals(80, settings.dailyNewLimit)
    }

    @Test
    fun `setDailyNewLimit 越界值被 clamp 到 10-200`() = runTest {
        val repo = CardSettingsRepositoryImpl(FakeDataStore())

        repo.setDailyNewLimit(5)
        assertEquals(10, repo.cardSettings.first().dailyNewLimit)

        repo.setDailyNewLimit(500)
        assertEquals(200, repo.cardSettings.first().dailyNewLimit)
    }

    @Test
    fun `setFrequencyFilter 持久化 HIGH`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = CardSettingsRepositoryImpl(fakeStore)

        repo.setFrequencyFilter(CardFrequencyFilter.HIGH)
        assertEquals(CardFrequencyFilter.HIGH, repo.cardSettings.first().frequencyFilter)
    }

    @Test
    fun `setSubjectFilters 持久化单科`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = CardSettingsRepositoryImpl(fakeStore)

        repo.setSubjectFilters(setOf("中国古代文学"))
        assertEquals(setOf("中国古代文学"), repo.cardSettings.first().subjectFilters)
    }

    @Test
    fun `setSubjectFilters 空集回退默认四科`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = CardSettingsRepositoryImpl(fakeStore)

        repo.setSubjectFilters(emptySet())
        assertEquals(CardSettings.DEFAULT_SUBJECTS, repo.cardSettings.first().subjectFilters)
    }

    @Test
    fun `setExamDate 持久化并清除`() = runTest {
        val fakeStore = FakeDataStore()
        val repo = CardSettingsRepositoryImpl(fakeStore)

        repo.setExamDate(1_752_710_400_000L)
        assertEquals(1_752_710_400_000L, repo.cardSettings.first().examDateMillis)

        repo.setExamDate(null)
        assertNull(repo.cardSettings.first().examDateMillis)
    }

    @Test
    fun `非法考频值降级为 HIGH_MEDIUM`() = runTest {
        val fakeStore = FakeDataStore()
        // 直接写入非法枚举名（模拟 DataStore 被外部写入损坏值）
        fakeStore.state.value = preferencesOf(
            stringPreferencesKey("card_frequency_filter") to "NOT_A_FILTER",
        )

        val repo = CardSettingsRepositoryImpl(fakeStore)
        assertEquals(
            CardFrequencyFilter.HIGH_MEDIUM,
            repo.cardSettings.first().frequencyFilter,
        )
    }

    @Test
    fun `非法新卡限额降级为默认 60`() = runTest {
        val fakeStore = FakeDataStore()
        // 直接写入越界值（模拟损坏数据）
        fakeStore.state.value = preferencesOf(
            intPreferencesKey("card_daily_new_limit") to -1,
        )

        val repo = CardSettingsRepositoryImpl(fakeStore)
        assertEquals(60, repo.cardSettings.first().dailyNewLimit)
    }
}

/** 内存版 DataStore（仿 ThemeRepositoryImplTest.FakeDataStore）。 */
private class FakeDataStore : DataStore<Preferences> {
    val state = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        val newValue = transform(state.value)
        state.value = newValue
        return newValue
    }
}
