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

    // Smoothing disabled (alpha = 1), each fame shows exactly the given angle.
    // Synthetic frames are only 33 ms apart, so the minimum interval between reps is switched off
    // here and tested separately below.
    private val config = SquatAnalyzerConfig(smoothingAlpha = 1.0, minRepIntervalMs = 0L)

    private val frameMs =33L
    private var clockMs = 0L

    private fun SquatAnalyzer.step(pose: List<PoseLandmarkPoint>): SquatAnalysisState{
        clockMs+=frameMs
        return processFrame(pose, timestampMs =clockMs)
    }

    /** Builds 33 landmarks of a side-vie pose with the given knee angle */
    private fun poseWithKneeAngle(
        angleDeg: Double,
        visibility: Double = 0.9,
        trunkLeanDeg: Double=0.0
    ): List<PoseLandmarkPoint> {
        val rad = Math.toRadians(angleDeg)
        val leanRad = Math.toRadians(trunkLeanDeg)
        // Shoulder placed so that the hip->shoulder segment leans trunkLeanDeg from vertical.
        val shoulder = PoseLandmarkPoint(0.5+0.2*sin(leanRad), 0.3-0.2*cos(leanRad), visibility = visibility)
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
        angles.map { step(poseWithKneeAngle(it)) }

    private fun SquatAnalyzer.feedFront(vararg kneeShifts: Double): List<SquatAnalysisState> =
        kneeShifts.map { step(frontPose(it)) }

    private fun SquatAnalyzer.feedWithLean(
        kneeAngles: DoubleArray,
        trunkLeans: DoubleArray
    ): List<SquatAnalysisState> =
        kneeAngles.zip(trunkLeans).map{
            (knee,lean)->step(poseWithKneeAngle(knee,trunkLeanDeg=lean))
        }

    @Test
    fun `deep rep is counted without errors`() {
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        val states = analyzer.feed(170.0, 140.0, 110.0, 85.0, 80.0, 90.0, 120.0, 150.0, 165.0)

        val rep = states.last().lastCompletedRep
        assertNotNull(rep)
        assertEquals(1, rep!!.repNumber)
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
        assertEquals(2, states.last().lastCompletedRep!!.repNumber)
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
        val state = analyzer.step(poseWithKneeAngle(170.0, visibility = 0.1))

        assertFalse(state.personDetected)
        assertNull(state.currentKneeAngleDeg)
        assertEquals(1, state.repCount)
    }

    @Test
    fun `reset clears rep count`() {
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        analyzer.feed(170.0, 140.0, 80.0, 90.0, 165.0)
        analyzer.reset()
        val state = analyzer.step(poseWithKneeAngle(170.0))

        assertEquals(0, state.repCount)
    }

    @Test
    fun `auto mode starts in side view`() {
        val analyzer = SquatAnalyzer(SquatViewMode.AUTO, config)
        val state = analyzer.step(poseWithKneeAngle(170.0))

        assertEquals(SquatViewMode.SIDE, state.viewMode)
    }

    @Test
    fun `auto mode switches to front after stable standing frames`() {
        val analyzer = SquatAnalyzer(SquatViewMode.AUTO, config)
        val states = analyzer.feedFront(0.0, 0.0, 0.0, 0.0, 0.0)

        assertEquals(SquatViewMode.SIDE, states[3].viewMode)   // 4 frames - not yet
        assertEquals(SquatViewMode.FRONT, states[4].viewMode)  // 5th frame - switched
    }

    @Test
    fun `a single side-looking frame cancels a pending switch`() {
        val analyzer = SquatAnalyzer(SquatViewMode.AUTO, config)
        analyzer.feedFront(0.0, 0.0, 0.0, 0.0)
        analyzer.step(poseWithKneeAngle(170.0))
        val states = analyzer.feedFront(0.0, 0.0, 0.0, 0.0)

        assertEquals(SquatViewMode.SIDE, states.last().viewMode)
    }

    @Test
    fun `view never changes during a repetition`() {
        val analyzer = SquatAnalyzer(SquatViewMode.AUTO, config)
        val start = analyzer.step(poseWithKneeAngle(170.0))
        // Front-looking frames, but they form a rep (FPPA 147 -> 136 -> 147 -> 180).
        val rep = analyzer.feedFront(0.06, 0.08, 0.06, 0.0)

        assertEquals(SquatViewMode.SIDE, start.viewMode)
        assertTrue(rep.all { it.viewMode == SquatViewMode.SIDE })
        assertNotNull(rep.last().lastCompletedRep)
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

    // A full deep rep (minimum 80 deg, no depth error).
    private val repKneeAngles = doubleArrayOf(170.0, 140.0, 110.0, 85.0, 80.0, 90.0, 120.0, 150.0, 165.0)

    @Test
    fun `gradual hip hinge is not reported as back rounding`() {
        // 2 deg per 33 ms frame = about 61 deg/s, below the 100 deg/s threshold.
        val leans = doubleArrayOf(0.0, 2.0, 4.0, 6.0, 8.0, 6.0, 4.0, 2.0, 0.0)
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        val states = analyzer.feedWithLean(repKneeAngles, leans)

        assertTrue(states.last().lastCompletedRep!!.isCorrect)
    }

    @Test
    fun `sudden trunk drop is reported as back rounding`() {
        // 4 -> 14 deg in one 33 ms frame = about 303 deg/s.
        val leans = doubleArrayOf(0.0, 2.0, 4.0, 14.0, 14.0, 10.0, 6.0, 2.0, 0.0)
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        val states = analyzer.feedWithLean(repKneeAngles, leans)

        assertEquals(
            setOf(SquatErrorType.EXCESSIVE_BACK_ROUNDING),
            states.last().lastCompletedRep!!.errors
        )
    }

    @Test
    fun `back rounding stays reported until the rep ends`() {
        val leans = doubleArrayOf(0.0, 2.0, 4.0, 14.0, 14.0, 10.0, 6.0, 2.0, 0.0)
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        val states = analyzer.feedWithLean(repKneeAngles, leans)

        assertTrue(states.drop(3).all {
            SquatErrorType.EXCESSIVE_BACK_ROUNDING in it.activeErrors
        })
    }

    @Test
    fun `fast straightening up is not reported`() {
        // trunk returns from 8 to 0 deg in one frame - negative velocity.
        val leans = doubleArrayOf(0.0, 2.0, 4.0, 6.0, 8.0, 8.0, 8.0, 0.0, 0.0)
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        val states = analyzer.feedWithLean(repKneeAngles, leans)

        assertTrue(states.last().lastCompletedRep!!.isCorrect)
    }

    @Test
    fun `front view never reports back rounding`() {
        val leans = doubleArrayOf(0.0, 2.0, 4.0, 14.0, 14.0, 10.0, 6.0, 2.0, 0.0)
        val analyzer = SquatAnalyzer(SquatViewMode.FRONT, config)
        val states = analyzer.feedWithLean(repKneeAngles, leans)

        assertFalse(SquatErrorType.EXCESSIVE_BACK_ROUNDING in states.last().lastCompletedRep!!.errors)
    }

    @Test
    fun `rep reports its highest trunk lean velocity`() {
        val leans = doubleArrayOf(0.0, 2.0, 4.0, 14.0, 14.0, 10.0, 6.0, 2.0, 0.0)
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        val states = analyzer.feedWithLean(repKneeAngles, leans)

        assertEquals(10.0 / 33.0 * 1000.0, states.last().lastCompletedRep!!.maxTrunkLeanVelocityDegPerSec, 1e-6)
    }

    @Test
    fun `sudden trunk drop above the knee angle limit is not reported`() {
        // 2 -> 12 deg in one frame while the knee is at 110 deg (above the 90 deg limit).
        val leans = doubleArrayOf(0.0, 2.0, 12.0, 12.0, 12.0, 10.0, 6.0, 2.0, 0.0)
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        val states = analyzer.feedWithLean(repKneeAngles, leans)

        assertTrue(states.last().lastCompletedRep!!.isCorrect)
    }

    // ---- Valgus reference and leg choice ----

    /** Inward knee shift that gives the requested hip-knee-ankle angle in [frontPoseLegs] */
    private fun shiftFor(angleDeg: Double): Double = 0.2 * kotlin.math.tan(Math.toRadians((180.0 - angleDeg) / 2.0))

    /** Front pose with separate inward shift and visibility for each leg (negative shift = knee outwards) */
    private fun frontPoseLegs(
        leftShift: Double, rightShift: Double,
        leftVisibility: Double = 0.9, rightVisibility: Double = 0.9
    ): List<PoseLandmarkPoint> {
        val points = MutableList(33) { PoseLandmarkPoint(0.5, 0.5, visibility = 0.9) }
        points[PoseLandmarkIndex.LEFT_SHOULDER] = PoseLandmarkPoint(0.55, 0.2, visibility = leftVisibility)
        points[PoseLandmarkIndex.RIGHT_SHOULDER] = PoseLandmarkPoint(0.45, 0.2, visibility = rightVisibility)
        points[PoseLandmarkIndex.LEFT_HIP] = PoseLandmarkPoint(0.55, 0.5, visibility = leftVisibility)
        points[PoseLandmarkIndex.RIGHT_HIP] = PoseLandmarkPoint(0.45, 0.5, visibility = rightVisibility)
        points[PoseLandmarkIndex.LEFT_KNEE] = PoseLandmarkPoint(0.55 - leftShift, 0.7, visibility = leftVisibility)
        points[PoseLandmarkIndex.RIGHT_KNEE] = PoseLandmarkPoint(0.45 + rightShift, 0.7, visibility = rightVisibility)
        points[PoseLandmarkIndex.LEFT_ANKLE] = PoseLandmarkPoint(0.55, 0.9, visibility = leftVisibility)
        points[PoseLandmarkIndex.RIGHT_ANKLE] = PoseLandmarkPoint(0.45, 0.9, visibility = rightVisibility)
        return points
    }

    @Test
    fun `valgus reference comes from the upright frame, not from the last frame before descent`() {
        val analyzer = SquatAnalyzer(SquatViewMode.FRONT, config)
        // 175 deg (upright) -> 151 deg (still standing, below 160) -> 145 deg (descent starts)
        // -> 140 deg (measured in the descent) -> back up
        val states = listOf(175.0, 151.0, 145.0, 140.0, 180.0).map {
            analyzer.step(frontPoseLegs(shiftFor(it), shiftFor(it)))
        }

        val rep = states.last().lastCompletedRep!!
        // Old behaviour: reference 151 deg -> deviation 11 deg. Now: reference 175 deg -> 35 deg.
        assertEquals(35.0, rep.maxKneeValgusDeviationDeg, 0.1)
        assertTrue(SquatErrorType.KNEE_VALGUS in rep.errors)
    }

    @Test
    fun `leg chosen at the start of the descent is kept until the rep ends`() {
        val analyzer = SquatAnalyzer(SquatViewMode.FRONT, config)
        val states = listOf(
            // standing, left leg better visible
            frontPoseLegs(0.0, 0.0, leftVisibility = 0.9, rightVisibility = 0.8),
            // descent starts on the left leg (140 deg)
            frontPoseLegs(shiftFor(140.0), 0.0, leftVisibility = 0.9, rightVisibility = 0.8),
            // right leg becomes better visible: left 130 deg inwards, right 120 deg outwards
            frontPoseLegs(shiftFor(130.0), -shiftFor(120.0), leftVisibility = 0.9, rightVisibility = 0.95),
            frontPoseLegs(shiftFor(130.0), -shiftFor(120.0), leftVisibility = 0.9, rightVisibility = 0.95),
            frontPoseLegs(0.0, 0.0, leftVisibility = 0.9, rightVisibility = 0.95)
        ).map { analyzer.step(it) }

        val rep = states.last().lastCompletedRep!!
        // Without the lock the right leg (120 deg, knee outwards) would be measured in frames 3-4.
        assertEquals(130.0, rep.minKneeAngleDeg, 0.1)
        assertTrue(SquatErrorType.KNEE_VALGUS in rep.errors)
    }

    @Test
    fun `frame is skipped when the chosen leg is not visible during the rep`() {
        val analyzer = SquatAnalyzer(SquatViewMode.FRONT, config)
        analyzer.step(frontPoseLegs(0.0, 0.0, leftVisibility = 0.9, rightVisibility = 0.8))
        analyzer.step(frontPoseLegs(shiftFor(140.0), 0.0, leftVisibility = 0.9, rightVisibility = 0.8))
        val state = analyzer.step(frontPoseLegs(shiftFor(130.0), 0.0, leftVisibility = 0.1, rightVisibility = 0.95))

        assertFalse(state.personDetected)
        assertEquals(SquatPhase.DESCENDING, state.phase)
    }

    // ---- Minimum interval between reps ----

    @Test
    fun `rep ending too soon after the previous one is not counted`() {
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config.copy(minRepIntervalMs = 1000L))
        analyzer.feed(170.0, 140.0, 85.0, 80.0, 90.0, 165.0)
        // second "rep" ends about 165 ms later
        val states = analyzer.feed(140.0, 85.0, 80.0, 90.0, 165.0)

        assertEquals(1, states.last().repCount)
        assertNull(states.last().lastCompletedRep)
        assertNotNull(states.last().suppressedRep)
        assertEquals(SquatPhase.STANDING, states.last().phase)
    }

    @Test
    fun `rep after the minimum interval is counted`() {
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config.copy(minRepIntervalMs = 1000L))
        analyzer.feed(170.0, 140.0, 85.0, 80.0, 90.0, 165.0)
        analyzer.feed(*DoubleArray(30) { 170.0 })   // about 1 s of standing
        val states = analyzer.feed(140.0, 85.0, 80.0, 90.0, 165.0)

        assertEquals(2, states.last().repCount)
        assertEquals(2, states.last().lastCompletedRep!!.repNumber)
    }

    // ---- Diagnostics ----

    @Test
    fun `full-rep trunk velocity is reported without the knee angle limit`() {
        // 2 -> 12 deg in one frame while the knee is at 110 deg (above the 90 deg limit).
        val leans = doubleArrayOf(0.0, 2.0, 12.0, 12.0, 12.0, 10.0, 6.0, 2.0, 0.0)
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        val rep = analyzer.feedWithLean(repKneeAngles, leans).last().lastCompletedRep!!

        assertEquals(10.0 / 33.0 * 1000.0, rep.maxTrunkLeanVelocityFullRepDegPerSec, 1e-6)
        assertTrue(rep.maxTrunkLeanVelocityDegPerSec < 100.0)
        assertTrue(rep.isCorrect)
    }
    @Test
    fun `back rounding in one rep is not carried over to the next`() {
        val jerk = doubleArrayOf(0.0, 2.0, 4.0, 14.0, 14.0, 10.0, 6.0, 2.0, 0.0)
        val clean = doubleArrayOf(0.0, 2.0, 4.0, 6.0, 8.0, 6.0, 4.0, 2.0, 0.0)
        val analyzer = SquatAnalyzer(SquatViewMode.SIDE, config)
        analyzer.feedWithLean(repKneeAngles, jerk)
        val second = analyzer.feedWithLean(repKneeAngles, clean).last().lastCompletedRep!!

        assertTrue(second.isCorrect)
    }
}