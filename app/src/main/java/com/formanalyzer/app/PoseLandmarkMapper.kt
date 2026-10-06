package com.formanalyzer.app

import com.formanalyzer.app.squat.PoseLandmarkPoint
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

/**
 * Converts MediaPipe landmark to analyzer independent model
 *
 * Missing visibility is treated as 0 so unrealiable landmarks wont be mistaken as confident one
 *
 *
 * */
fun NormalizedLandmark.toPoseLandmarkPoint(): PoseLandmarkPoint = PoseLandmarkPoint(
    x = x().toDouble(),
    y = y().toDouble(),
    z = z().toDouble(),
    visibility = visibility().orElse(0f).toDouble()

)