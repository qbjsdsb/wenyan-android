package com.wenyan.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.common.util.friendlyErrorMessage
import com.wenyan.app.core.data.repository.DailyPlanRepository
import com.wenyan.app.core.database.entity.DailyPlanWithTasks
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

interface TodayPlanSource {
    fun observe(date: String): Flow<DailyPlanWithTasks?>

    /** Ensure the date has a persisted plan before [observe] starts consuming it. */
    suspend fun ensure(date: String) = Unit
}

@Singleton
class PersistedTodayPlanSource @Inject constructor(
    private val repository: DailyPlanRepository,
    private val factory: TodayPlanFactory,
) : TodayPlanSource {
    override fun observe(date: String): Flow<DailyPlanWithTasks?> = repository.observe(date)

    override suspend fun ensure(date: String) {
        // Read the source snapshot before entering getOrCreate's Room transaction. The
        // transaction callback must only validate/insert the already-built draft; collecting
        // Room-backed flows inside it can wait on the same database executor indefinitely.
        val draft = factory.create(date)
        val persisted = repository.getOrCreate(date) { draft }
        if (persisted.plan.status == "EMPTY" && persisted.tasks.isEmpty() && draft.tasks.isNotEmpty()) {
            repository.fillEmpty(date, draft)
        }
    }
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
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel @Inject constructor(
    source: TodayPlanSource,
    dateProvider: TodayDateSource,
) : ViewModel() {
    private val today = dateProvider.today()
    private val retryTrigger = MutableStateFlow(0)

    val uiState = retryTrigger.flatMapLatest {
        source.observe(today.toString())
            .map { value -> value?.let { TodayPlanMapper.map(it, today) } ?: TodayUiState(date = today.toString()) }
            .onStart {
                emit(TodayUiState(isLoading = true, date = today.toString()))
                source.ensure(today.toString())
            }
            .catch { emit(TodayUiState(date = today.toString(), error = friendlyErrorMessage(it))) }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState(isLoading = true))

    fun retry() {
        retryTrigger.update { it + 1 }
    }
}

/** Small write-only bridge used by navigation destinations after a study session finishes. */
@HiltViewModel
class DailyTaskCompletionViewModel @Inject constructor(
    private val repository: DailyPlanRepository,
) : ViewModel() {
    fun markDone(taskId: String) {
        if (taskId.isBlank()) return
        viewModelScope.launch {
            try {
                repository.markDone(taskId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Completion is a best-effort side effect after leaving a study screen.
                // A database failure must not crash the navigation host or cancel siblings.
                Timber.w(e, "mark daily task done failed: $taskId")
            }
        }
    }
}
