package com.wenyan.app.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [detectScrollDirection] 纯函数的单元测试。
 *
 * 覆盖场景：
 * - index 优先：跨 item 滚动方向由 index 差值决定
 * - offset 阈值防抖：同一 index 内 +10/-10px 阈值
 * - 列表顶部边界：index=0, offset=0 时无滚动
 * - 列表底部边界：index 不变时 offset 微调
 * - 自定义阈值
 */
class ScrollDirectionDetectorTest {

    // region index 优先（跨 item 滚动）

    @Test
    fun `index increases → DOWN`() {
        val result = detectScrollDirection(
            index = 3, offset = 0,
            previousIndex = 2, previousOffset = 0,
        )
        assertEquals(ScrollDirection.DOWN, result)
    }

    @Test
    fun `index decreases → UP`() {
        val result = detectScrollDirection(
            index = 2, offset = 0,
            previousIndex = 3, previousOffset = 0,
        )
        assertEquals(ScrollDirection.UP, result)
    }

    @Test
    fun `index increases by 2 → DOWN (even with offset suggesting otherwise)`() {
        // index 优先：即使 offset 减小，index 增大仍判定为 DOWN
        val result = detectScrollDirection(
            index = 5, offset = 20,
            previousIndex = 3, previousOffset = 100,
        )
        assertEquals(ScrollDirection.DOWN, result)
    }

    @Test
    fun `index decreases by 2 → UP (even with offset suggesting otherwise)`() {
        // index 优先：即使 offset 增大，index 减小仍判定为 UP
        val result = detectScrollDirection(
            index = 3, offset = 100,
            previousIndex = 5, previousOffset = 20,
        )
        assertEquals(ScrollDirection.UP, result)
    }

    // endregion

    // region 同一 index 内：offset 变化 + 防抖阈值

    @Test
    fun `same index, offset increase above threshold → DOWN`() {
        val result = detectScrollDirection(
            index = 2, offset = 50,
            previousIndex = 2, previousOffset = 35,
        )
        assertEquals(ScrollDirection.DOWN, result)
    }

    @Test
    fun `same index, offset decrease above threshold → UP`() {
        val result = detectScrollDirection(
            index = 2, offset = 20,
            previousIndex = 2, previousOffset = 50,
        )
        assertEquals(ScrollDirection.UP, result)
    }

    @Test
    fun `same index, offset exactly at threshold → IDLE (not above)`() {
        val result = detectScrollDirection(
            index = 2, offset = 45,
            previousIndex = 2, previousOffset = 35,
        )
        assertEquals(ScrollDirection.IDLE, result)
    }

    @Test
    fun `same index, offset exactly at negative threshold → IDLE`() {
        val result = detectScrollDirection(
            index = 2, offset = 25,
            previousIndex = 2, previousOffset = 35,
        )
        assertEquals(ScrollDirection.IDLE, result)
    }

    @Test
    fun `same index, offset just above threshold (+11px) → DOWN`() {
        val result = detectScrollDirection(
            index = 2, offset = 46,
            previousIndex = 2, previousOffset = 35,
        )
        assertEquals(ScrollDirection.DOWN, result)
    }

    @Test
    fun `same index, offset just below negative threshold (-11px) → UP`() {
        val result = detectScrollDirection(
            index = 2, offset = 24,
            previousIndex = 2, previousOffset = 35,
        )
        assertEquals(ScrollDirection.UP, result)
    }

    // endregion

    // region 边界情况

    @Test
    fun `list top boundary → IDLE`() {
        // index=0, offset=0 且上一帧相同：没有可滚动的空间
        val result = detectScrollDirection(
            index = 0, offset = 0,
            previousIndex = 0, previousOffset = 0,
        )
        assertEquals(ScrollDirection.IDLE, result)
    }

    @Test
    fun `list bottom boundary → IDLE`() {
        // index 不变，offset 微调在阈值内：到达底部时没有更多内容
        val result = detectScrollDirection(
            index = 10, offset = 120,
            previousIndex = 10, previousOffset = 115,
        )
        assertEquals(ScrollDirection.IDLE, result)
    }

    @Test
    fun `same index and offset → IDLE`() {
        val result = detectScrollDirection(
            index = 4, offset = 200,
            previousIndex = 4, previousOffset = 200,
        )
        assertEquals(ScrollDirection.IDLE, result)
    }

    // endregion

    // region 自定义阈值

    @Test
    fun `custom threshold 5px, offset increase 6px → DOWN`() {
        val result = detectScrollDirection(
            index = 2, offset = 41,
            previousIndex = 2, previousOffset = 35,
            threshold = 5,
        )
        assertEquals(ScrollDirection.DOWN, result)
    }

    @Test
    fun `custom threshold 20px, offset increase 15px → IDLE`() {
        val result = detectScrollDirection(
            index = 2, offset = 50,
            previousIndex = 2, previousOffset = 35,
            threshold = 20,
        )
        assertEquals(ScrollDirection.IDLE, result)
    }

    @Test
    fun `custom threshold 0px, any offset change → detected`() {
        val result = detectScrollDirection(
            index = 2, offset = 36,
            previousIndex = 2, previousOffset = 35,
            threshold = 0,
        )
        assertEquals(ScrollDirection.DOWN, result)
    }

    // endregion
}
