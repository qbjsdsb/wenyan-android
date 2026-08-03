package com.wenyan.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CardSettingsRepository] 的 DataStore Preferences 实现（v0.9.29）。
 *
 * 键：
 * - card_daily_new_limit: Int（默认 60）
 * - card_frequency_filter: String（枚举 name，默认 HIGH_MEDIUM）
 * - card_subject_filters: Set<String>（默认四科全选）
 * - card_exam_date: Long（毫秒时间戳；缺省 null = 未设置）
 *
 * 设计约束（仿 ThemeRepositoryImpl）：
 * - @Singleton：避免多实例无谓开销，DataStore 本身保证数据一致
 * - runCatching 容错：枚举 valueOf 遇到非法值降级默认，防止设置损坏导致崩溃
 * - Flow 加 .catch：DataStore IO 异常（磁盘满/文件损坏）降级默认值，不白屏
 */
@Singleton
class CardSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : CardSettingsRepository {

    private companion object {
        private const val TAG = "CardSettingsRepository"
        private val KEY_DAILY_NEW_LIMIT = intPreferencesKey("card_daily_new_limit")
        private val KEY_FREQUENCY_FILTER = stringPreferencesKey("card_frequency_filter")
        private val KEY_SUBJECT_FILTERS = stringSetPreferencesKey("card_subject_filters")
        private val KEY_EXAM_DATE = longPreferencesKey("card_exam_date")

        /** 每日新卡数可设范围（张） */
        const val MIN_LIMIT = 10
        const val MAX_LIMIT = 200
    }

    override val cardSettings: Flow<CardSettings> = dataStore.data.map { prefs ->
        CardSettings(
            dailyNewLimit = prefs[KEY_DAILY_NEW_LIMIT]
                ?.takeIf { it in MIN_LIMIT..MAX_LIMIT }
                ?: CardSettings().dailyNewLimit,
            frequencyFilter = prefs[KEY_FREQUENCY_FILTER]?.let { parseFilter(it) }
                ?: CardFrequencyFilter.HIGH_MEDIUM,
            subjectFilters = prefs[KEY_SUBJECT_FILTERS]
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?.ifEmpty { CardSettings.DEFAULT_SUBJECTS }
                ?: CardSettings.DEFAULT_SUBJECTS,
            examDateMillis = prefs[KEY_EXAM_DATE],
        )
    }.catch { e ->
        Timber.e(e, "$TAG cardSettings failed: ${e.message}")
        emit(CardSettings())
    }

    override suspend fun setDailyNewLimit(limit: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_DAILY_NEW_LIMIT] = limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        }
    }

    override suspend fun setFrequencyFilter(filter: CardFrequencyFilter) {
        dataStore.edit { prefs ->
            prefs[KEY_FREQUENCY_FILTER] = filter.name
        }
    }

    override suspend fun setSubjectFilters(subjects: Set<String>) {
        dataStore.edit { prefs ->
            prefs[KEY_SUBJECT_FILTERS] = subjects.ifEmpty { CardSettings.DEFAULT_SUBJECTS }
        }
    }

    override suspend fun setExamDate(millis: Long?) {
        dataStore.edit { prefs ->
            if (millis == null) {
                prefs.remove(KEY_EXAM_DATE)
            } else {
                prefs[KEY_EXAM_DATE] = millis
            }
        }
    }

    /** 解析考频枚举，非法值降级为 HIGH_MEDIUM（仿 ThemeRepositoryImpl 容错）。 */
    private fun parseFilter(name: String): CardFrequencyFilter? =
        runCatching { CardFrequencyFilter.valueOf(name) }.getOrNull()
}
