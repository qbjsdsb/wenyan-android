package com.wenyan.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingHubTest {
    @Test
    fun exposesExactlyFourExistingBusinessRoutesInStableOrder() {
        val entries = trainingEntries()
        assertEquals(listOf("快速回忆", "真题作答", "610 写作", "错题修复"), entries.map { it.title })
        assertEquals(
            listOf(
                TopLevelDestination.ROUTE_CARDS,
                ROUTE_QUIZ_PRACTICE,
                TopLevelDestination.ROUTE_ESSAY,
                TopLevelDestination.ROUTE_WRONG_ANSWER,
            ),
            entries.map { it.route },
        )
        assertEquals(4, entries.map { it.route }.distinct().size)
    }

}
