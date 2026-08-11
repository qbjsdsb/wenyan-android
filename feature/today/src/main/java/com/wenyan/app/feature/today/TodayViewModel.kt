package com.wenyan.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.DailyPlanRepository
import com.wenyan.app.core.database.entity.DailyPlanWithTasks
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

interface TodayPlanSource {
    fun observe(date: String): Flow<DailyPlanWithTasks?>
}

@Singleton
class PersistedTodayPlanSource @Inject constructor(
    private val repository: DailyPlanRepository,
) : TodayPlanSource {
    override fun observe(date: String): Flow<DailyPlanWithTasks?> = repository.observe(date)
}

interface TodayDateSource { fun today(): LocalDate }

@Singleton
class TodayDateProvider @Inject constructor() : TodayDateSource {
    private val zone = ZoneId.of("Asia/Taipei")
    override fun today(): LocalDate = LocalDate.now(Clock.systemUTC().withZone(zone))
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TodaySourceModule {
    @Binds abstract fun bindTodayPlanSource(implementation: PersistedTodayPlanSource): TodayPlanSource
    @Binds abstract fun bindTodayDateSource(implementation: TodayDateProvider): TodayDateSource
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    source: TodayPlanSource,
    dateProvider: TodayDateSource,
) : ViewModel() {
    private val today = dateProvider.today()

    val uiState = source.observe(today.toString())
        .map { value -> value?.let { TodayPlanMapper.map(it, today) } ?: TodayUiState(date = today.toString()) }
        .onStart { emit(TodayUiState(isLoading = true, date = today.toString())) }
        .catch { emit(TodayUiState(date = today.toString(), error = it.message ?: "今日计划加载失败")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState(isLoading = true))
}
