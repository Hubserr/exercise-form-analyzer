package com.formanalyzer.app.squat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalorieEstimatorTest {

    private val delta = 1e-9

    private fun summary(durationMs: Long, reps: Int) = SessionSummary(
        startedAtMs = 0,
        durationMs = durationMs,
        reps = List(reps) { RepAnalysis(it + 1, 85.0, 50.0, emptySet()) }
    )

    private val fullMale = BodyProfile(weightKg = 80.0, heightCm = 180.0, ageYears = 25, sex = Sex.MALE)

    @Test
    fun `Mifflin-St Jeor for a man`() {
        // 10 * 80 + 6.25 * 180 - 5 * 25 + 5 = 1805
        assertEquals(1805.0, CalorieEstimator.basalMetabolicRate(fullMale)!!, delta)
    }

    @Test
    fun `Mifflin-St Jeor for a woman`() {
        val woman = BodyProfile(weightKg = 60.0, heightCm = 165.0, ageYears = 30, sex = Sex.FEMALE)
        // 10 * 60 + 6.25 * 165 - 5 * 30 - 161 = 1320.25
        assertEquals(1320.25, CalorieEstimator.basalMetabolicRate(woman)!!, delta)
    }


    @Test
    fun `BMR needs all four fields`() {
        assertNull(CalorieEstimator.basalMetabolicRate(fullMale.copy(sex = null)))
        assertNull(CalorieEstimator.basalMetabolicRate(fullMale.copy(ageYears = null)))
    }
    @Test
    fun `full profile uses the corrected MET formula`() {
        val estimate = CalorieEstimator.estimate(summary(durationMs = 60_000, reps = 10), fullMale)

        assertEquals(CalorieMethod.BODY_PROFILE, estimate.method)
        assertEquals(CalorieEstimator.SQUAT_MET * 1805.0 / 24.0 / 60.0, estimate.kcal, delta)
    }

    @Test
    fun `incomplete profile falls back to the per-rep default`() {
        val estimate = CalorieEstimator.estimate(
            summary(durationMs = 60_000, reps = 10), fullMale.copy(ageYears = null)
        )

        assertEquals(CalorieMethod.PER_REP_DEFAULT, estimate.method)
    }



    @Test
    fun `no profile data gives a flat value per repetition`() {
        val estimate = CalorieEstimator.estimate(summary(durationMs = 60_000, reps = 10), BodyProfile())

        assertEquals(CalorieMethod.PER_REP_DEFAULT, estimate.method)
        assertEquals(10 * CalorieEstimator.DEFAULT_KCAL_PER_REP, estimate.kcal, delta)
    }
}