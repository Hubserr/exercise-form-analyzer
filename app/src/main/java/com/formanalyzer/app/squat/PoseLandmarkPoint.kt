package com.formanalyzer.app.squat


/**
 *  A single body landmark:
 *  x,y normalized coordinates in [0,...,1]
 *  z - depth estimated by the model not used for angle calculations
 *
 * visibility - model confidence [0,...,1] that landmark is visible
 *
 *
 */
data class PoseLandmarkPoint (
    val x: Double,
    val y: Double,
    val z: Double =0.0,
    val visibility: Double = 1.0
)

/** Blaze Pose landmarks indicies  **/
object PoseLandmarkIndex{
    const val NOSE = 0
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
    const val LEFT_HEEL = 29
    const val RIGHT_HEEL = 30
    const val LEFT_FOOT_INDEX = 31
    const val RIGHT_FOOT_INDEX = 32

}