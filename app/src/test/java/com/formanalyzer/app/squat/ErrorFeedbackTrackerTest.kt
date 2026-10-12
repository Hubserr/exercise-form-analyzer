package com.formanalyzer.app.squat

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ErrorFeedbackTrackerTest{

    private fun state(
        vararg errors: SquatErrorType,
        repCompleted: Boolean = false
    ) = SquatAnalysisState (
        personDetected = true,
        phase = SquatPhase.ASCENDING,
        repCount = 0,
        currentKneeAngleDeg = 120.0,
        activeErrors = errors.toSet(),
        viewMode = SquatViewMode.SIDE,
        lastCompletedRep = if(repCompleted){
            RepAnalysis(1,80.0,0.0,errors.toSet())
        }else{
            null
        }
    )

    @Test
    fun `no active errors means nothing to announce`(){
        assertTrue(ErrorFeedbackTracker().newErrors(state()).isEmpty())
    }
    @Test
    fun `error active in many frames is announced only once`(){
        val tracker = ErrorFeedbackTracker()
        val back = SquatErrorType.EXCESSIVE_BACK_ROUNDING

        assertEquals(setOf(back),tracker.newErrors(state(back)))
        assertTrue(tracker.newErrors(state(back)).isEmpty())
        assertTrue(tracker.newErrors(state(back)).isEmpty())

    }

    @Test
    fun `a second error in the same rep is announced too`(){
        val tracker = ErrorFeedbackTracker()
        val back = SquatErrorType.EXCESSIVE_BACK_ROUNDING
        val valgus = SquatErrorType.KNEE_VALGUS
        tracker.newErrors(state(back))

        val fresh = tracker.newErrors(state(back,valgus))
        assertEquals(setOf(valgus),fresh)
    }
    @Test
    fun `the same error is announced again in the next rep`(){
        val tracker = ErrorFeedbackTracker()
        val back = SquatErrorType.EXCESSIVE_BACK_ROUNDING
        tracker.newErrors(state(back))
        tracker.newErrors(state(repCompleted = true))

        assertEquals(setOf(back),tracker.newErrors(state(back)))

    }
    @Test
    fun `error detected in the rep-completing frame is still announced`(){
        val tracker = ErrorFeedbackTracker()
        val back = SquatErrorType.EXCESSIVE_BACK_ROUNDING

        assertEquals(setOf(back),tracker.newErrors(state(back, repCompleted = true)))

    }



}