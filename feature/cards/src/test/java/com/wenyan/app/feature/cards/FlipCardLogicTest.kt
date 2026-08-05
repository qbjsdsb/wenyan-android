package com.wenyan.app.feature.cards

import org.junit.Assert.assertEquals
import org.junit.Test

class FlipCardLogicTest {

    @Test
    fun shouldShowBack_rotationZero_returnsFalse() {
        assertEquals(false, shouldShowBack(0f))
    }

    @Test
    fun shouldShowBack_rotationLessThan90_returnsFalse() {
        assertEquals(false, shouldShowBack(45f))
        assertEquals(false, shouldShowBack(89f))
        assertEquals(false, shouldShowBack(89.9f))
    }

    @Test
    fun shouldShowBack_rotationExactly90_returnsFalse() {
        // 边界：90° 时正面仍可见（卡侧宽度=0 但还未翻过去）
        assertEquals(false, shouldShowBack(90f))
    }

    @Test
    fun shouldShowBack_rotationJustOver90_returnsTrue() {
        assertEquals(true, shouldShowBack(90.1f))
        assertEquals(true, shouldShowBack(91f))
    }

    @Test
    fun shouldShowBack_rotation180_returnsTrue() {
        assertEquals(true, shouldShowBack(180f))
    }

    @Test
    fun shouldShowBack_rotationFallsBackFromFlipped_returnsFalse() {
        // 从 180° 翻回 0° 时，过 90° 应立即显示正面
        assertEquals(true, shouldShowBack(135f))
        assertEquals(false, shouldShowBack(45f))
    }

    @Test
    fun flipScale_startAndEndRemainFullSize_midpointPullsBackSlightly() {
        assertEquals(1f, flipScale(0f), 0.0001f)
        assertEquals(0.975f, flipScale(90f), 0.0001f)
        assertEquals(1f, flipScale(180f), 0.0001f)
    }

    @Test
    fun flipScale_clampsOutOfRangeAngles() {
        assertEquals(1f, flipScale(-20f), 0.0001f)
        assertEquals(1f, flipScale(220f), 0.0001f)
    }
}
