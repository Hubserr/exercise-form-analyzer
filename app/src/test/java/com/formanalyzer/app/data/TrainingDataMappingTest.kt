package com.formanalyzer.app.data

import com.formanalyzer.app.squat.RepAnalysis
import com.formanalyzer.app.squat.SquatErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingDataMappingTest {

    private val converters = Converters()

    @Test
    fun `errors survive a round trip through the converter`() {
        val errors = setOf(SquatErrorType.INSUFFICIENT_DEPTH, SquatErrorType.EXCESSIVE_BACK_ROUNDING)

        assertEquals(errors, converters.stringToErrors(converters.errorsToString(errors)))
    }

    @Test
    fun `no errors are stored as an empty string`() {
        assertEquals("", converters.errorsToString(emptySet()))
        assertTrue(converters.stringToErrors("").isEmpty())
    }

    @Test
    fun `a repetition survives a round trip through the entity`() {
        val rep = RepAnalysis(3, 84.5, 120.0, setOf(SquatErrorType.KNEE_VALGUS))

        val entity = rep.toEntity(sessionId = 7)

        assertEquals(7, entity.sessionId)
        assertEquals(rep, entity.toRepAnalysis())
    }
    @Test
    fun `saved session keeps its data and orders reps by number`() {
        val loaded = SessionWithReps(
            session = TrainingSessionEntity(id = 5, startedAtMs = 1_000, durationMs = 60_000),
            reps = listOf(
                RepEntity(sessionId = 5, repNumber = 2, minKneeAngleDeg = 95.0,
                    maxTrunkLeanVelocityDegPerSec = 40.0,
                    errors = setOf(SquatErrorType.INSUFFICIENT_DEPTH)),
                RepEntity(sessionId = 5, repNumber = 1, minKneeAngleDeg = 80.0,
                    maxTrunkLeanVelocityDegPerSec = 30.0, errors = emptySet())
            )
        )

        val saved = loaded.toSavedSession()

        assertEquals(5, saved.id)
        assertEquals(60_000, saved.summary.durationMs)
        assertEquals(listOf(1, 2), saved.summary.reps.map { it.repNumber })
        assertEquals(1, saved.summary.correctReps)
        assertEquals(1, saved.summary.errorCount(SquatErrorType.INSUFFICIENT_DEPTH))
    }




}