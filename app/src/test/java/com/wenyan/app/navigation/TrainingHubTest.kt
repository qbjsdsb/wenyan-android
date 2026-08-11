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
                ROUTE_WRITING_MATERIALS,
                TopLevelDestination.ROUTE_WRONG_ANSWER,
            ),
            entries.map { it.route },
        )
        assertEquals(4, entries.map { it.route }.distinct().size)
    }

    @Test
    fun writingEditorRouteWithoutMaterialUsesBlankSession() {
        assertEquals(ROUTE_WRITING_EDITOR, writingEditorRoute(null))
        assertEquals(ROUTE_WRITING_EDITOR, writingEditorRoute(" "))
    }

    @Test
    fun quizPracticeDetailRouteEncodesQuestionAndFilterValues() {
        assertEquals(
            "quiz_practice_detail/q%2F1?type=SHORT_ANSWER&subject=sub%201&year=2020&paper=610",
            quizPracticeDetailRoute("q/1", "SHORT_ANSWER", "sub 1", 2020, "610"),
        )
        assertEquals(null, quizPracticeDetailRoute(" ", null, null, null, null))
    }

}
