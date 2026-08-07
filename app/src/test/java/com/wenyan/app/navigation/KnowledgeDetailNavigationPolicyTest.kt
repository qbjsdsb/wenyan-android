package com.wenyan.app.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeDetailNavigationPolicyTest {

    @Test
    fun `different knowledge points from detail must push a new entry`() {
        assertFalse(
            shouldSkipKnowledgeDetailNavigation(
                currentDestinationRoute = ROUTE_KNOWLEDGE_DETAIL_PATTERN,
                currentPointId = "kp_01026",
                targetPointId = "kp_01027",
            ),
        )
        assertFalse(
            shouldUseKnowledgeDetailSingleTop(ROUTE_KNOWLEDGE_DETAIL_PATTERN),
        )
    }

    @Test
    fun `same knowledge point does not create a duplicate entry`() {
        assertTrue(
            shouldSkipKnowledgeDetailNavigation(
                currentDestinationRoute = ROUTE_KNOWLEDGE_DETAIL_PATTERN,
                currentPointId = "kp_01026",
                targetPointId = " kp_01026 ",
            ),
        )
    }

    @Test
    fun `entering detail from another destination can use singleTop`() {
        assertFalse(
            shouldSkipKnowledgeDetailNavigation(
                currentDestinationRoute = "knowledge",
                currentPointId = null,
                targetPointId = "kp_01026",
            ),
        )
        assertTrue(shouldUseKnowledgeDetailSingleTop("knowledge"))
    }

    @Test
    fun `blank point id is ignored`() {
        assertTrue(
            shouldSkipKnowledgeDetailNavigation(
                currentDestinationRoute = "knowledge",
                currentPointId = null,
                targetPointId = "   ",
            ),
        )
    }
}
