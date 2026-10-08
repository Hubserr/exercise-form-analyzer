package com.formanalyzer.app.squat

import androidx.camera.core.AspectRatio
import com.google.errorprone.annotations.Var
import com.google.mediapipe.tasks.components.containers.Landmark


class SquatAnalyzer(
    private val viewMode: SquatViewMode,
    private val config: SquatAnalyzerConfig = SquatAnalyzerConfig()
){

    private data class BodySide(val shoulder: Int, val hip: Int, val knee: Int, val ankle: Int)

    private val leftSide = BodySide(
        PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.LEFT_HIP,
        PoseLandmarkIndex.LEFT_KNEE, PoseLandmarkIndex.LEFT_ANKLE
    )
    private val rightSide = BodySide(
        PoseLandmarkIndex.RIGHT_SHOULDER, PoseLandmarkIndex.RIGHT_HIP,
        PoseLandmarkIndex.RIGHT_KNEE, PoseLandmarkIndex.RIGHT_ANKLE
    )

    private val smoother = LandmarkSmoothingFilter(config.smoothingAlpha)
    private var phase = SquatPhase.STANDING
    private var repCount =0

    private var standingKneeAngleBaseline = 180.0
    private var standingKneeOffsetRatioBaseline = 0.0

    private var previousTrunkLean: Double? = null
    private var previousTrunkLeanTimestampMs: Long? = null

    private var minKneeAngleInRep = Double.MAX_VALUE
    private var maxKneeValgusDeviationDegInRep = 0.0

    private var maxTrunkLeanVelocityInRep = 0.0
    private var errorsInRep = mutableSetOf<SquatErrorType>()


    /** resets state when starting a new set*/
    fun reset(){
        smoother.reset()
        phase = SquatPhase.STANDING
        repCount =0
        standingKneeAngleBaseline =180.0
        standingKneeOffsetRatioBaseline =0.0
        previousTrunkLean = null
        previousTrunkLeanTimestampMs = null
        clearRepAccumulators()


    }
    fun clearRepAccumulators(){
        minKneeAngleInRep = Double.MAX_VALUE
        maxKneeValgusDeviationDegInRep =0.0
        maxTrunkLeanVelocityInRep =0.0
        errorsInRep.clear()
    }

    fun processFrame(
        rawLandmarks: List<PoseLandmarkPoint>,
        timestampMs: Long,
        aspectRatio: Double =1.0
    ): SquatAnalysisState {
        val side = selectReliableSide(rawLandmarks)
        if(side==null){
            smoother.reset()
            previousTrunkLean =null
            return noDetectedState()
        }
        val landmarks = smoother.smooth(rawLandmarks)
        val shoulder = landmarks[side.shoulder]
        val hip1 = landmarks[side.hip]
        val knee = landmarks[side.knee]
        val ankle = landmarks[side.ankle]
        val hip2 = landmarks[if(side==leftSide) rightSide.hip else leftSide.hip]

        val kneeAngle = JointAngleCalculator.angleAt(
            landmarks[side.hip], landmarks[side.knee], landmarks[side.ankle], aspectRatio
        ) ?: return noDetectedState()

        val hipWidth = JointAngleCalculator.segmentLength(hip1,hip2,aspectRatio).coerceAtLeast(MIN_HIP_WIDTH)
        val kneeOffsetRatio = JointAngleCalculator.kneeCenterlineOffset(knee,hip1,hip2,aspectRatio)

        val trunkLean = JointAngleCalculator.angleFromVertical(upper = shoulder, lower = hip1,aspectRatio =aspectRatio)
        val trunkLeanVelocity = updateTrunkVelocity(trunkLean,timestampMs)

        val mode = effectiveViewModel()
        val activeErrors = mutableSetOf<SquatErrorType>()

        if(phase!= SquatPhase.STANDING){
            maxTrunkLeanVelocityInRep =maxOf( maxTrunkLeanVelocityInRep,trunkLeanVelocity)
            if(mode== SquatViewMode.SIDE&&maxTrunkLeanVelocityInRep>config.backRoundingVelocityDegPerSec){
                activeErrors+= SquatErrorType.EXCESSIVE_BACK_ROUNDING
            }

        }

        when(phase ){
            SquatPhase.STANDING -> {
                if( kneeAngle >= config.descendingEntryKneeAngleDeg){
                    standingKneeAngleBaseline = kneeAngle
                    standingKneeOffsetRatioBaseline = kneeOffsetRatio
                } else{
                    phase = SquatPhase.DESCENDING
                    minKneeAngleInRep = kneeAngle
                }
            }

            SquatPhase.DESCENDING->{
            minKneeAngleInRep = minOf(minKneeAngleInRep,kneeAngle)
                if(mode== SquatViewMode.FRONT){
                    trackKneeValgus(kneeAngle, kneeOffsetRatio)
                }
                if(kneeAngle>minKneeAngleInRep+config.bottomHysterisisDeg){
                    phase = SquatPhase.ASCENDING
                    activeErrors+=evaluateBottom(mode)
                }
            }

            SquatPhase.ASCENDING -> {

            }

        }
        errorsInRep +=activeErrors

        var completedRep: RepAnalysis ?=null

        if(phase== SquatPhase.ASCENDING&&kneeAngle>=config.standingKneeAngleDeg) {
            repCount++
            completedRep = RepAnalysis(
                repNumberval = repCount,
                minKneeAngleDeg = minKneeAngleInRep,
                maxTrunkLeanVelocityDegPerSec = maxTrunkLeanVelocityInRep,
                errors = errorsInRep.toSet()
            )
            phase = SquatPhase.STANDING
            clearRepAccumulators()
        }
            return SquatAnalysisState(
                personDetected = true,
                phase = phase,
                repCount = repCount,
                currentKneeAngleDeg = kneeAngle,
                activeErrors = activeErrors,
                viewMode = mode,
                lastCompletedRep = completedRep
            )

    }

    private fun updateTrunkVelocity(trunkLean: Double?, timestampMs: Long): Double{
        val lastLean = previousTrunkLean
        val lastTimestampMs = previousTrunkLeanTimestampMs
        previousTrunkLean = trunkLean
        previousTrunkLeanTimestampMs = timestampMs

        if (trunkLean==null || lastLean == null || lastTimestampMs == null || timestampMs <= lastTimestampMs
        ) {
            return 0.0
        }
        return (trunkLean - lastLean) / (timestampMs - lastTimestampMs) * 1000.0

    }

    private fun trackKneeValgus(kneeAngle: Double,kneeOffsetRatio: Double){
        val movedMedially  = kneeOffsetRatio<standingKneeOffsetRatioBaseline
        if(movedMedially){
            val deviation = standingKneeAngleBaseline - kneeAngle
            maxKneeValgusDeviationDegInRep =maxOf(maxKneeValgusDeviationDegInRep,deviation)
        }

    }

    private fun evaluateBottom(mode: SquatViewMode): Set<SquatErrorType>{
        val errors = mutableSetOf<SquatErrorType>()
        if(mode== SquatViewMode.SIDE&&minKneeAngleInRep>config.insufficientDepthKneeAngleDeg){
            errors+= SquatErrorType.INSUFFICIENT_DEPTH
        }
        if(mode== SquatViewMode.FRONT&&maxKneeValgusDeviationDegInRep>=config.kneeValgusDeviationDeg){
            errors+= SquatErrorType.KNEE_VALGUS
        }

        return errors
    }

    private fun effectiveViewModel(): SquatViewMode =
        if(viewMode == SquatViewMode.AUTO) SquatViewMode.SIDE else viewMode


    private fun selectReliableSide(landmarks: List<PoseLandmarkPoint>): BodySide?{

        if(landmarks.size <= PoseLandmarkIndex.RIGHT_ANKLE) return null

        fun visibilityOf(side: BodySide)= minOf(
            landmarks[side.shoulder].visibility,
            landmarks[side.hip].visibility,
            landmarks[side.knee].visibility,
            landmarks[side.ankle].visibility
        )

        val left = visibilityOf(leftSide)
        val right = visibilityOf(rightSide)
        if(maxOf(left,right)<config.minVisibility) return null
        return if(left>=right) leftSide else rightSide
    }

    private fun noDetectedState() = SquatAnalysisState(
        personDetected = false,
        phase = phase,
        repCount = repCount,
        currentKneeAngleDeg = null,
        activeErrors = emptySet(),
        viewMode = effectiveViewModel()
    )

    private companion object {
        /** Lower bound for hip width - in a side view both hips nearly overlap. */
        const val MIN_HIP_WIDTH = 0.01
    }
}


