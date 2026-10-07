package com.formanalyzer.app.squat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class SquatAnalyzerTest {

    // Smoothing disabled (alpha = 1), each fame shows exactly the given angle
    private val config = SquatAnalyzerConfig(smoothingAlpha = 1.0)

    /** Builds 33 landmarks of a side-vie pose with the given knee angle */
    private fun poseWithKneeAngle(angleDeg: Double, visibility: Double = 0.9): List<PoseLandmarkPoint> {
        val rad = Math.toRadians(angleDeg)
        val shoulder = PoseLandmarkPoint(0.5, 0.1, visibility = visibility)
        val hip = PoseLandmarkPoint(0.5, 0.3, visibility = visibility)
        val knee = PoseLandmarkPoint(0.5, 0.5, visibility = visibility)
        // Ankle placed so that the hip-knee-ankle angle equals angleDeg
        val ankle = PoseLandmarkPoint(
            0.5 + 0.2 * sin(rad), 0.5 - 0.2 * cos(rad), visibility = visibility
        )
        return List(33) { index ->
            when (index) {
                PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.RIGHT_SHOULDER -> shoulder
                PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.RIGHT_HIP -> hip
                PoseLandmarkIndex.LEFT_KNEE, PoseLandmarkIndex.RIGHT_KNEE -> knee
                PoseLandmarkIndex.LEFT_ANKLE, PoseLandmarkIndex.RIGHT_ANKLE -> ankle
                else -> PoseLandmarkPoint(0.5, 0.5, visibility = visibility)
            }
        }
    }

    /** Feeds a sequence of knee angles, one frame each, and returns all states */
    private fun SquatAnalyzer.feed(vararg angles: Double): List<SquatAnalysisState> =
        angles.map { processFrame(poseWithKneeAngle(it)) }

    @Test
    fun `deep rep is counted without errors`() {
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        val states = analyzer.feed(170.0, 140.0, 110.0, 85.0, 80.0, 90.0, 120.0, 150.0, 165.0)

        val rep = states.last().lastCompletedRep
        assertNotNull(rep)
        assertEquals(1, rep!!.repNumberval)
        assertEquals(80.0, rep.minKneeAngleDeg, 1e-6)
        assertTrue(rep.isCorrect)
        assertEquals(1, states.last().repCount)
        assertEquals(SquatPhase.STANDING, states.last().phase)
    }

    @Test
    fun `shallow rep reports insufficient depth`() {
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        val states = analyzer.feed(170.0, 140.0, 120.0, 100.0, 110.0, 140.0, 165.0)

        val rep = states.last().lastCompletedRep!!
        assertEquals(setOf(SquatErrorType.INSUFFICIENT_DEPTH), rep.errors)
    }

    @Test
    fun `small bump during descent does not end the descent early`() {
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        // 102 -> 104 is a +2 deg bump, below the 5 deg bottom hysteresis.
        val states = analyzer.feed(170.0, 140.0, 120.0, 102.0, 104.0, 95.0, 85.0, 80.0, 90.0, 130.0, 165.0)

        val rep = states.last().lastCompletedRep!!
        assertEquals(80.0, rep.minKneeAngleDeg, 1e-6)
        assertTrue(rep.isCorrect)
    }

    @Test
    fun `jitter while standing does not start a rep`() {
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        val states = analyzer.feed(170.0, 165.0, 155.0, 162.0, 158.0, 168.0)

        assertTrue(states.all { it.phase == SquatPhase.STANDING })
        assertEquals(0, states.last().repCount)
    }

    @Test
    fun `two reps are counted`() {
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        analyzer.feed(170.0, 140.0, 85.0, 80.0, 90.0, 165.0)
        val states = analyzer.feed(140.0, 85.0, 80.0, 90.0, 165.0)

        assertEquals(2, states.last().repCount)
        assertEquals(2, states.last().lastCompletedRep!!.repNumberval)
    }

    @Test
    fun `front view never reports depth`() {
        val analyzer = SquatAnalyzer(SquatViewMode.FRONT, config)
        val states = analyzer.feed(170.0, 140.0, 120.0, 100.0, 110.0, 140.0, 165.0)

        assertTrue(states.last().lastCompletedRep!!.isCorrect)
    }

    @Test
    fun `low visibility means person not detected and keeps rep count`() {
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        analyzer.feed(170.0, 140.0, 80.0, 90.0, 165.0)
        val state = analyzer.processFrame(poseWithKneeAngle(170.0, visibility = 0.1))

        assertFalse(state.personDetected)
        assertNull(state.currentKneeAngleDeg)
        assertEquals(1, state.repCount)
    }

    @Test
    fun `reset clears rep count`() {
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        analyzer.feed(170.0, 140.0, 80.0, 90.0, 165.0)
        analyzer.reset()
        val state = analyzer.processFrame(poseWithKneeAngle(170.0))

        assertEquals(0, state.repCount)
    }

    @Test
    fun `auto mode currently reports side view`() {
        val analyzer = SquatAnalyzer(SquatViewMode.AUTO, config)
        val state = analyzer.processFrame(poseWithKneeAngle(170.0))

        assertEquals(SquatViewMode.SIDE, state.viewMode)
    }



    private fun frontPose(kneeShift: Double): List<PoseLandmarkPoint> {
        val points = MutableList(33) { PoseLandmarkPoint(0.5, 0.5, visibility = 0.9) }
        points[PoseLandmarkIndex.LEFT_SHOULDER] = PoseLandmarkPoint(0.55, 0.2, visibility = 0.9)
        points[PoseLandmarkIndex.RIGHT_SHOULDER] = PoseLandmarkPoint(0.45, 0.2, visibility = 0.9)
        points[PoseLandmarkIndex.LEFT_HIP] = PoseLandmarkPoint(0.55, 0.5, visibility = 0.9)
        points[PoseLandmarkIndex.RIGHT_HIP] = PoseLandmarkPoint(0.45, 0.5, visibility = 0.9)
        points[PoseLandmarkIndex.LEFT_KNEE] = PoseLandmarkPoint(0.55 - kneeShift, 0.7, visibility = 0.9)
        points[PoseLandmarkIndex.RIGHT_KNEE] = PoseLandmarkPoint(0.45 + kneeShift, 0.7, visibility = 0.9)
        points[PoseLandmarkIndex.LEFT_ANKLE] = PoseLandmarkPoint(0.55, 0.9, visibility = 0.9)
        points[PoseLandmarkIndex.RIGHT_ANKLE] = PoseLandmarkPoint(0.45, 0.9, visibility = 0.9)
        return points
    }

    private fun SquatAnalyzer.feedFront(vararg kneeShifts: Double): List<SquatAnalysisState> =
        kneeShifts.map { processFrame(frontPose(it)) }


    private val cavingIn = doubleArrayOf(0.0, 0.03, 0.06, 0.08, 0.06, 0.0)
    private val pushedOut = doubleArrayOf(0.0, -0.03, -0.06, -0.08, -0.06, 0.0)

    @Test
    fun `knees caving in are reported as valgus in front view`() {
        val analyzer = SquatAnalyzer(SquatViewMode.FRONT, config)
        val states = analyzer.feedFront(*cavingIn)

        assertEquals(setOf(SquatErrorType.KNEE_VALGUS), states.last().lastCompletedRep!!.errors)
    }

    @Test
    fun `knees pushed out are never reported as valgus`() {
        val analyzer = SquatAnalyzer(SquatViewMode.FRONT, config)
        val states = analyzer.feedFront(*pushedOut)

        assertTrue(states.last().lastCompletedRep!!.isCorrect)
    }

    @Test
    fun `valgus below threshold is not reported`() {

        val analyzer = SquatAnalyzer(SquatViewMode.FRONT, config.copy(kneeValgusDeviationDeg = 40.0))
        val states = analyzer.feedFront(*cavingIn)

        assertTrue(states.last().lastCompletedRep!!.isCorrect)
    }

    @Test
    fun `side view never reports valgus`() {
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        val states = analyzer.feedFront(*cavingIn)

        assertFalse(SquatErrorType.KNEE_VALGUS in states.last().lastCompletedRep!!.errors)
    }

}