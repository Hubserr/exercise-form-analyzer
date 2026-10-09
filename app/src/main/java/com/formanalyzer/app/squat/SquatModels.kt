package com.formanalyzer.app.squat

import androidx.transition.Visibility

enum class SquatPhase{
    STANDING,
    DESCENDING,
    ASCENDING
}
enum class SquatViewMode{
    SIDE,
    FRONT,
    AUTO
}
enum class SquatErrorType {
    INSUFFICIENT_DEPTH,
    KNEE_VALGUS,
    EXCESSIVE_BACK_ROUNDING
}
data class RepAnalysis(
    val repNumberval :Int,
    val minKneeAngleDeg: Double,
    val maxTrunkLeanVelocityDegPerSec: Double,
    val errors:Set<SquatErrorType>
){
    val isCorrect: Boolean
        get() = errors.isEmpty()
}
data class SquatAnalysisState(
    val personDetected: Boolean,
    val phase: SquatPhase,
    val repCount: Int,
    val currentKneeAngleDeg: Double?,
    val activeErrors: Set<SquatErrorType>,
    val viewMode: SquatViewMode,
    val lastCompletedRep: RepAnalysis?=null
)

data class SquatAnalyzerConfig(
    val minVisibility: Double=0.5,
    val standingKneeAngleDeg: Double=160.0,
    val descendingEntryKneeAngleDeg: Double =150.0,
    val bottomHysterisisDeg: Double = 5.0,
    val insufficientDepthKneeAngleDeg: Double =90.0,
    val kneeValgusDeviationDeg: Double = 10.0,
    val backRoundingVelocityDegPerSec: Double = 100.0,
    val orientationFrontRatio: Double = 0.20,
    val orientationStableFrames: Int =5,

    val smoothingAlpha: Double =0.5


){
    init{
        require(descendingEntryKneeAngleDeg<standingKneeAngleDeg){
            "descendingEntryKneeAngleDeg must be lower than standingKneeAngleDeg"
        }
        require(bottomHysterisisDeg>=0.0){"bottomHysteresisDeg must not be negative" }
    }
}