package com.formanalyzer.app.squat


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
    val repNumber :Int,
    val minKneeAngleDeg: Double,
    val maxTrunkLeanVelocityDegPerSec: Double,
    val errors:Set<SquatErrorType>,
    val maxKneeValgusDeviationDeg: Double = 0.0,
    /** Diagnostics only: trunk lean velocity over the whole rep, without the knee angle limit */
    val maxTrunkLeanVelocityFullRepDegPerSec: Double = 0.0
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
    val lastCompletedRep: RepAnalysis?=null,
    /** Rep that ended too soon after the previous one - not counted and not saved */
    val suppressedRep: RepAnalysis? = null
)

data class SquatAnalyzerConfig(
    val minVisibility: Double=0.5,
    val standingKneeAngleDeg: Double=160.0,
    val descendingEntryKneeAngleDeg: Double =150.0,
    val bottomHysteresisDeg: Double = 5.0,
    val insufficientDepthKneeAngleDeg: Double =90.0,
    val kneeValgusDeviationDeg: Double = 10.0,
    val backRoundingVelocityDegPerSec: Double = 100.0,
    val backRoundingMaxKneeAngleDeg: Double = 90.0,
    val orientationFrontRatio: Double = 0.20,
    val orientationStableFrames: Int =5,
    /** Minimum time between two counted reps */
    val minRepIntervalMs: Long = 1000L,

    val smoothingAlpha: Double =0.5


){
    init{
        require(descendingEntryKneeAngleDeg<standingKneeAngleDeg){
            "descendingEntryKneeAngleDeg must be lower than standingKneeAngleDeg"
        }
        require(bottomHysteresisDeg>=0.0){"bottomHysteresisDeg must not be negative" }
        require(minRepIntervalMs>=0L){"minRepIntervalMs must not be negative" }
    }
}