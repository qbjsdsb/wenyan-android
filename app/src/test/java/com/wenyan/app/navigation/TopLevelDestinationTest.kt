package com.wenyan.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelDestinationTest {
    @Test fun `top level is exactly today knowledge training and my`() {
        assertEquals(
            listOf("today", "knowledge", "training", "my"),
            TopLevelDestination.destinations.map { it.route },
        )
    }

    @Test fun `all legacy entries map to a retained top level parent`() {
        val expected = mapOf(
            "knowledge_detail/{pointId}" to "knowledge",
            "daily_cards/task-1/point-1" to "today",
            "daily_cards_fullscreen" to "today",
            "essay" to "training", "cards" to "training", "quiz_practice" to "training",
            "essay_detail/eq-1" to "training", "writing_materials" to "training",
            "writing_editor?materialId=wm-1" to "training", "wrong_answer" to "my", "settings" to "my",
            "about" to "my", "api_config" to "my",
        )
        assertEquals(expected, expected.mapValues { TopLevelDestination.parentRouteFor(it.key) })
    }
}
