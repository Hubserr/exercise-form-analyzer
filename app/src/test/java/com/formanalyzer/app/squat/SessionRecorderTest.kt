package com.formanalyzer.app.squat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRecorderTest {

    private fun rep(number: Int, vararg errors: SquatErrorType) =
        RepAnalysis(number, 85.0, 50.0, errors.toSet())

    private fun state(completedRep: RepAnalysis? = null) = SquatAnalysisState(
        personDetected = true,
        phase = SquatPhase.STANDING,
        repCount = completedRep?.repNumber ?: 0,
        currentKneeAngleDeg = 170.0,
        activeErrors = emptySet(),
        viewMode = SquatViewMode.SIDE,
        lastCompletedRep = completedRep
    )

    @Test
    fun `finishing a set that never started returns null`() {
        assertNull(SessionRecorder().finish(nowMs = 1000))
    }

    @Test
    fun `only completed repetitions are recorded`() {
        val recorder = SessionRecorder()
        recorder.start(nowMs = 0)
        recorder.record(state())
        recorder.record(state(rep(1)))
        recorder.record(state())
        recorder.record(state(rep(2)))

        assertEquals(2, recorder.finish(nowMs = 10000)!!.totalReps)
    }

    @Test
    fun `summary counts correct reps and each error`() {
        val recorder = SessionRecorder()
        recorder.start(nowMs = 0)
        recorder.record(state(rep(1)))
        recorder.record(state(rep(2, SquatErrorType.INSUFFICIENT_DEPTH)))
        recorder.record(state(rep(3, SquatErrorType.INSUFFICIENT_DEPTH, SquatErrorType.EXCESSIVE_BACK_ROUNDING)))

        val summary = recorder.finish(nowMs = 10000)!!
        assertEquals(3, summary.totalReps)
        assertEquals(1, summary.correctReps)
        assertEquals(2, summary.errorCount(SquatErrorType.INSUFFICIENT_DEPTH))
        assertEquals(1, summary.errorCount(SquatErrorType.EXCESSIVE_BACK_ROUNDING))
        assertEquals(0, summary.errorCount(SquatErrorType.KNEE_VALGUS))
    }

    @Test
    fun `duration is measured from the first start call`() {
        val recorder = SessionRecorder()
        recorder.start(nowMs = 1000)
        recorder.start(nowMs = 5000)   // the set is already running

        val summary = recorder.finish(nowMs = 31000)!!
        assertEquals(1000, summary.startedAtMs)
        assertEquals(30000, summary.durationMs)
    }

    @Test
    fun `reps before the start are ignored`() {
        val recorder = SessionRecorder()
        recorder.record(state(rep(1)))
        recorder.start(nowMs = 0)

        assertEquals(0, recorder.finish(nowMs = 1000)!!.totalReps)
    }

    @Test
    fun `finish starts from scratch`() {
        val recorder = SessionRecorder()
        recorder.start(nowMs = 0)
        recorder.record(state(rep(1)))
        recorder.finish(nowMs = 1000)

        assertFalse(recorder.isStarted)
        recorder.start(nowMs = 2000)
        assertTrue(recorder.finish(nowMs = 3000)!!.reps.isEmpty())
    }
}