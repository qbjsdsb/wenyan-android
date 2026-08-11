package com.wenyan.app.core.data.writing
import org.junit.Assert.*
import org.junit.Test
class WritingRubricTest{
 private fun assessment(level:RubricLevel)=RubricAssessment(RubricDimension.entries.map{RubricMark(it,level)})
 @Test fun totalIsTransparentAndNotOfficial(){val a=assessment(RubricLevel.SOLID);assertEquals(21,a.total);assertEquals(28,a.maximum)}
 @Test fun onlyReviewedEvidenceIsCitable(){val x=listOf(EvidenceCandidate("a","REVIEWED"),EvidenceCandidate("b","LEGACY_UNVERIFIED"),EvidenceCandidate("c","UNKNOWN"));assertEquals(listOf("a"),citableEvidence(x).map{it.id});assertEquals(listOf("b","c"),evidenceClues(x).map{it.id})}
 @Test fun firstAndMultipleHistoryAreExplained(){assertTrue(trends(listOf(assessment(RubricLevel.DEVELOPING))).all{it.direction=="首次记录"});assertTrue(trends(listOf(assessment(RubricLevel.NEEDS_WORK),assessment(RubricLevel.SOLID))).all{it.direction=="改善"})}
 @Test fun weakDimensionsCreateOfflineTasks(){assertEquals(7,followUpTasks(assessment(RubricLevel.DEVELOPING)).size)}
 @Test fun assessmentRoundTripsForProcessRecovery(){val a=assessment(RubricLevel.SOLID);assertEquals(a,decodeAssessment(encodeAssessment(a)))}
}
