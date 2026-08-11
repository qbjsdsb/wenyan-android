package com.wenyan.app.core.data.writing

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

interface WritingClock {
    fun wallTimeMs(): Long
    fun monotonicMs(): Long
}

@Singleton
class SystemWritingClock @Inject constructor() : WritingClock {
    override fun wallTimeMs() = System.currentTimeMillis()
    override fun monotonicMs() = SystemClock.elapsedRealtime()
}

class ActiveWritingTimer(
    persisted: PersistedTimer,
    wallNowMs: Long,
    monotonicNowMs: Long,
) {
    private val baseElapsed = elapsedMs(persisted, wallNowMs)
    private val monotonicAnchor = monotonicNowMs
    fun elapsedAt(monotonicNowMs: Long): Long = baseElapsed + (monotonicNowMs - monotonicAnchor).coerceAtLeast(0)
}
