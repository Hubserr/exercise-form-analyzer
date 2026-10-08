package com.formanalyzer.app.debug

import android.util.Log
import com.formanalyzer.app.squat.JointAngleCalculator
import com.formanalyzer.app.squat.LandmarkSmoothingFilter
import com.formanalyzer.app.squat.PoseLandmarkIndex
import com.formanalyzer.app.squat.PoseLandmarkPoint
import java.util.Locale

class JitterLogger (private val alphas: List<Double> = listOf(0.2,0.3,0.4,0.5,0.6,0.7,0.8)){

    private val filters = alphas.map { LandmarkSmoothingFilter(it) }
    private var headerLogged = false

    fun log(rawLandmarks: List<PoseLandmarkPoint>, aspectRatio: Double, timestampMs: Long){
        if(rawLandmarks.size<= PoseLandmarkIndex.RIGHT_FOOT_INDEX){
            filters.forEach { it.reset() }
            return
        }
        if(!headerLogged){
            Log.d(TAG,header())
            headerLogged =true
        }
        val series = listOf(rawLandmarks) + filters.map { it.smooth(rawLandmarks) }
        val values = series.flatMap {
            landmarks -> listOf(kneeAngle(landmarks,aspectRatio), trunkAngle(landmarks,aspectRatio))
        }
        val line = listOf(timestampMs.toString())+values.map{format(it)}
        Log.d(TAG,line.joinToString(","))

    }
    private fun header(): String{
        val labels = listOf("raw") + alphas.map { "a$it" }
        val columns = labels.flatMap { listOf("knee_$it", "trunk_$it") }
        return (listOf("t_ms") + columns).joinToString(",")
    }
    private fun useLeftSide(landmarks: List<PoseLandmarkPoint>): Boolean {
        fun visibility(vararg indices: Int) = indices.minOf { landmarks[it].visibility }
        val left = visibility(
            PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.LEFT_KNEE, PoseLandmarkIndex.LEFT_ANKLE
        )
        val right = visibility(
            PoseLandmarkIndex.RIGHT_SHOULDER, PoseLandmarkIndex.RIGHT_HIP,
            PoseLandmarkIndex.RIGHT_KNEE, PoseLandmarkIndex.RIGHT_ANKLE
        )
        return left >= right
    }
    private fun kneeAngle(landmarks: List<PoseLandmarkPoint>, aspectRatio: Double): Double? {
        val left = useLeftSide(landmarks)
        return JointAngleCalculator.angleAt(
            landmarks[if (left) PoseLandmarkIndex.LEFT_HIP else PoseLandmarkIndex.RIGHT_HIP],
            landmarks[if (left) PoseLandmarkIndex.LEFT_KNEE else PoseLandmarkIndex.RIGHT_KNEE],
            landmarks[if (left) PoseLandmarkIndex.LEFT_ANKLE else PoseLandmarkIndex.RIGHT_ANKLE],
            aspectRatio
        )
    }

    private fun trunkAngle(landmarks: List<PoseLandmarkPoint>, aspectRatio: Double): Double? {
        val left = useLeftSide(landmarks)
        return JointAngleCalculator.angleFromVertical(
            upper = landmarks[if (left) PoseLandmarkIndex.LEFT_SHOULDER else PoseLandmarkIndex.RIGHT_SHOULDER],
            lower = landmarks[if (left) PoseLandmarkIndex.LEFT_HIP else PoseLandmarkIndex.RIGHT_HIP],
            aspectRatio = aspectRatio
        )
    }

    private fun format(value: Double?): String =
        if (value == null) "" else String.format(Locale.US, "%.3f", value)

    private companion object {
        const val TAG = "Jitter"
}
}