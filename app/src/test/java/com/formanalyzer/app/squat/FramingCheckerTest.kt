package com.formanalyzer.app.squat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FramingCheckerTest {

    private fun pose(overrides: Map<Int, PoseLandmarkPoint> = emptyMap()): List<PoseLandmarkPoint> =
        List(33) { index -> overrides[index] ?: PoseLandmarkPoint(0.5, 0.5, visibility = 0.9) }

    private fun hidden(vararg indices: Int): Map<Int, PoseLandmarkPoint> =
        indices.associateWith { PoseLandmarkPoint(0.5, 0.5, visibility = 0.1) }

    private val bothFeet = intArrayOf(
        PoseLandmarkIndex.LEFT_ANKLE, PoseLandmarkIndex.LEFT_HEEL, PoseLandmarkIndex.LEFT_FOOT_INDEX,
        PoseLandmarkIndex.RIGHT_ANKLE, PoseLandmarkIndex.RIGHT_HEEL, PoseLandmarkIndex.RIGHT_FOOT_INDEX
    )

    @Test
    fun `whole body in frame makes all parts visible`() {
        val status = FramingChecker().update(pose())

        assertTrue(status.allVisible)
    }

    @Test
    fun `ready only after the required number of stable frames`() {
        val checker = FramingChecker(requiredStableFrames = 15)
        repeat(14) { checker.update(pose()) }

        assertTrue(checker.update(pose()).isReady)
    }

    @Test
    fun `fourteen stable frames are not enough`() {
        val checker = FramingChecker(requiredStableFrames = 15)
        var status = checker.update(pose())
        repeat(13) { status = checker.update(pose()) }

        assertEquals(14, status.stableFrames)
        assertFalse(status.isReady)
    }

    @Test
    fun `a missing part resets the stable counter`() {
        val checker = FramingChecker(requiredStableFrames = 15)
        repeat(10) { checker.update(pose()) }
        val broken = checker.update(pose(hidden(*bothFeet)))
        assertEquals(0, broken.stableFrames)

        repeat(14) { checker.update(pose()) }
        assertFalse(checker.update(pose(hidden(*bothFeet))).isReady)
    }

    @Test
    fun `feet below the bottom edge are not visible`() {
        val outside = bothFeet.associateWith { PoseLandmarkPoint(0.5, 1.05, visibility = 0.9) }
        val status = FramingChecker().update(pose(outside))

        assertFalse(BodyPart.FEET in status.visibleParts)
    }

    @Test
    fun `one visible side is enough in a side view`() {
        val farSide = hidden(
            PoseLandmarkIndex.RIGHT_SHOULDER, PoseLandmarkIndex.RIGHT_HIP,
            PoseLandmarkIndex.RIGHT_KNEE, PoseLandmarkIndex.RIGHT_ANKLE,
            PoseLandmarkIndex.RIGHT_HEEL, PoseLandmarkIndex.RIGHT_FOOT_INDEX,
            PoseLandmarkIndex.RIGHT_EAR
        )
        val status = FramingChecker().update(pose(farSide))

        assertTrue(status.allVisible)
    }

    @Test
    fun `head is visible when only one ear is visible`() {
        val status = FramingChecker().update(
            pose(hidden(PoseLandmarkIndex.NOSE, PoseLandmarkIndex.LEFT_EAR))
        )

        assertTrue(BodyPart.HEAD in status.visibleParts)
    }

    @Test
    fun `readiness stays after the person leaves the frame`() {
        val checker = FramingChecker(requiredStableFrames = 15)
        repeat(15) { checker.update(pose()) }
        val status = checker.update(emptyList())

        assertTrue(status.isReady)
        assertTrue(status.visibleParts.isEmpty())
    }

    @Test
    fun `reset clears readiness`() {
        val checker = FramingChecker(requiredStableFrames = 15)
        repeat(15) { checker.update(pose()) }
        checker.reset()

        assertFalse(checker.update(pose()).isReady)
    }
}