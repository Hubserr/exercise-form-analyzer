package com.formanalyzer.app.squat

import androidx.camera.core.AspectRatio
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

    private var minKneeAngleInRep = Double.MAX_VALUE
    private var errorsInRep = mutableSetOf<SquatErrorType>()


    /** resets state when starting a new set*/
    fun reset(){
        smoother.reset()
        phase = SquatPhase.STANDING
        repCount =0
        clearRepAccumulators()


    }
    fun clearRepAccumulators(){
        minKneeAngleInRep = Double.MAX_VALUE
        errorsInRep.clear()
    }

    fun processFrame(
        rawLandmarks: List<PoseLandmarkPoint>,
        aspectRatio: Double =1.0
    ): SquatAnalysisState {
        val side = selectReliableSide(rawLandmarks)
        if(side==null){
            smoother.reset()
            return noDetectedState()
        }
        val landmarks = smoother.smooth(rawLandmarks)
        val kneeAngle = JointAngleCalculator.angleAt(
            landmarks[side.hip], landmarks[side.knee], landmarks[side.ankle], aspectRatio
        ) ?: return noDetectedState()

        val mode = effectiveViewModel()
        val activeErrors = mutableSetOf<SquatErrorType>()

        when(phase ){
            SquatPhase.STANDING -> {
                if( kneeAngle <config.descendingEntryKneeAngleDeg){
                    phase = SquatPhase.DESCENDING
                    minKneeAngleInRep = kneeAngle
                }
            }

            SquatPhase.DESCENDING->{
            minKneeAngleInRep = minOf(minKneeAngleInRep,kneeAngle)
                if(kneeAngle>minKneeAngleInRep + config.bottomHysterisisDeg){

                    phase = SquatPhase.ASCENDING
                    if(mode== SquatViewMode.SIDE&&minKneeAngleInRep>config.insufficientDepthKneeAngleDeg){
                        activeErrors += SquatErrorType.INSUFFICIENT_DEPTH
                    }
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


}


