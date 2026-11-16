package com.formanalyzer.app.squat


class SquatAnalyzer(
    private val viewMode: SquatViewMode,
    private val config: SquatAnalyzerConfig = SquatAnalyzerConfig()
){

    private data class BodySide(val shoulder: Int, val hip: Int, val knee: Int, val ankle: Int)
    private data class KneeBaseline(val kneeAngleDeg: Double, val kneeOffsetRatio: Double)
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

    private var detectedViewMode = SquatViewMode.SIDE
    private var pendingViewMode: SquatViewMode ?= null
    private var pendingViewFrames =0

    /** Standing reference for each leg, updated only in upright frames (knee >= standingKneeAngleDeg) */
    private val standingBaselines = mutableMapOf<BodySide, KneeBaseline>()

    /** Leg chosen when the descent starts, kept until the rep ends */
    private var repSide: BodySide? = null

    /** Timestamp of the last counted rep, used for the minimum interval between reps */
    private var lastCountedRepTimestampMs: Long? = null

    private var previousTrunkLean: Double? = null
    private var previousTrunkLeanTimestampMs: Long? = null

    private var minKneeAngleInRep = Double.MAX_VALUE
    private var maxKneeValgusDeviationDegInRep = 0.0

    private var maxTrunkLeanVelocityInRep = 0.0
    private var maxTrunkLeanVelocityFullRepInRep = 0.0
    private var errorsInRep = mutableSetOf<SquatErrorType>()


    /** resets state when starting a new set*/
    fun reset(){
        smoother.reset()
        phase = SquatPhase.STANDING
        repCount =0
        standingBaselines.clear()
        repSide = null
        lastCountedRepTimestampMs = null
        previousTrunkLean = null
        previousTrunkLeanTimestampMs = null
        detectedViewMode = SquatViewMode.SIDE
        pendingViewMode = null
        pendingViewFrames =0
        clearRepAccumulators()


    }
    fun clearRepAccumulators(){
        minKneeAngleInRep = Double.MAX_VALUE
        maxKneeValgusDeviationDegInRep =0.0
        maxTrunkLeanVelocityInRep =0.0
        maxTrunkLeanVelocityFullRepInRep =0.0
        errorsInRep.clear()
    }

    fun processFrame(
        rawLandmarks: List<PoseLandmarkPoint>,
        timestampMs: Long,
        aspectRatio: Double =1.0
    ): SquatAnalysisState {
        val side = sideForFrame(rawLandmarks)
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
        val hip2 = landmarks[otherSide(side).hip]

        val kneeAngle = JointAngleCalculator.angleAt(
            landmarks[side.hip], landmarks[side.knee], landmarks[side.ankle], aspectRatio
        ) ?: return noDetectedState()

        val hipWidth = JointAngleCalculator.segmentLength(hip1,hip2,aspectRatio).coerceAtLeast(MIN_HIP_WIDTH)
        val kneeOffsetRatio = JointAngleCalculator.kneeCenterlineOffset(knee,hip1,hip2,aspectRatio)

        val trunkLean = JointAngleCalculator.angleFromVertical(upper = shoulder, lower = hip1,aspectRatio =aspectRatio)
        val trunkLeanVelocity = updateTrunkVelocity(trunkLean,timestampMs)

        if(viewMode== SquatViewMode.AUTO&&phase== SquatPhase.STANDING){
            val legLength = JointAngleCalculator.segmentLength(hip1,ankle,aspectRatio)
            if(legLength>0.0){
                updateDetectedView(hipWidth/legLength)
            }

        }

        val mode = effectiveViewModel()
        val activeErrors = mutableSetOf<SquatErrorType>()



        if(phase != SquatPhase.STANDING){
            maxTrunkLeanVelocityFullRepInRep = maxOf(maxTrunkLeanVelocityFullRepInRep, trunkLeanVelocity)
            if(kneeAngle < config.backRoundingMaxKneeAngleDeg){
                maxTrunkLeanVelocityInRep = maxOf(maxTrunkLeanVelocityInRep, trunkLeanVelocity)
            }
            if(mode == SquatViewMode.SIDE && maxTrunkLeanVelocityInRep > config.backRoundingVelocityDegPerSec){
                activeErrors += SquatErrorType.EXCESSIVE_BACK_ROUNDING
            }
        }

        when(phase ){
            SquatPhase.STANDING -> {
                updateStandingBaselines(landmarks, aspectRatio)
                if( kneeAngle < config.descendingEntryKneeAngleDeg){
                    phase = SquatPhase.DESCENDING
                    repSide = side
                    minKneeAngleInRep = kneeAngle
                }
            }

            SquatPhase.DESCENDING->{
            minKneeAngleInRep = minOf(minKneeAngleInRep,kneeAngle)
                if(mode== SquatViewMode.FRONT){
                    trackKneeValgus(side, kneeAngle, kneeOffsetRatio)
                }
                if(kneeAngle>minKneeAngleInRep+config.bottomHysteresisDeg){
                    phase = SquatPhase.ASCENDING
                    activeErrors+=evaluateBottom(mode)
                }
            }

            SquatPhase.ASCENDING -> {

            }

        }
        errorsInRep +=activeErrors

        var completedRep: RepAnalysis ?=null
        var suppressedRep: RepAnalysis ?=null

        if(phase== SquatPhase.ASCENDING&&kneeAngle>=config.standingKneeAngleDeg) {
            val rep = RepAnalysis(
                repNumber = repCount + 1,
                minKneeAngleDeg = minKneeAngleInRep,
                maxTrunkLeanVelocityDegPerSec = maxTrunkLeanVelocityInRep,
                errors = errorsInRep.toSet(),
                maxKneeValgusDeviationDeg = maxKneeValgusDeviationDegInRep,
                maxTrunkLeanVelocityFullRepDegPerSec = maxTrunkLeanVelocityFullRepInRep
            )
            val lastRepMs = lastCountedRepTimestampMs
            if(lastRepMs != null && timestampMs - lastRepMs < config.minRepIntervalMs){
                // Too soon after the previous rep - not counted and not saved
                suppressedRep = rep
            } else {
                repCount++
                lastCountedRepTimestampMs = timestampMs
                completedRep = rep
            }
            phase = SquatPhase.STANDING
            repSide = null
            clearRepAccumulators()
        }
            return SquatAnalysisState(
                personDetected = true,
                phase = phase,
                repCount = repCount,
                currentKneeAngleDeg = kneeAngle,
                activeErrors = activeErrors,
                viewMode = mode,
                lastCompletedRep = completedRep,
                suppressedRep = suppressedRep
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

    /** Stores the standing reference of every visible leg, but only when the leg is straight (knee >= standingKneeAngleDeg) */
    private fun updateStandingBaselines(landmarks: List<PoseLandmarkPoint>, aspectRatio: Double){
        for(side in listOf(leftSide, rightSide)){
            if(visibilityOf(landmarks, side) < config.minVisibility) continue
            val angle = JointAngleCalculator.angleAt(
                landmarks[side.hip], landmarks[side.knee], landmarks[side.ankle], aspectRatio
            ) ?: continue
            if(angle >= config.standingKneeAngleDeg){
                val offset = JointAngleCalculator.kneeCenterlineOffset(
                    landmarks[side.knee], landmarks[side.hip], landmarks[otherSide(side).hip], aspectRatio
                )
                standingBaselines[side] = KneeBaseline(angle, offset)
            }
        }
    }

    private fun trackKneeValgus(side: BodySide, kneeAngle: Double,kneeOffsetRatio: Double){
        val baseline = standingBaselines[side] ?: DEFAULT_BASELINE
        val movedMedially  = kneeOffsetRatio<baseline.kneeOffsetRatio
        if(movedMedially){
            val deviation = baseline.kneeAngleDeg - kneeAngle
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

    private  fun updateDetectedView(hipToLegRatio: Double){
        val observed = if(hipToLegRatio>config.orientationFrontRatio){
            SquatViewMode.FRONT
        }else{
            SquatViewMode.SIDE
        }
        if(observed==detectedViewMode){
            pendingViewMode=null
            pendingViewFrames=0
            return
        }
        if(observed==pendingViewMode){
            pendingViewFrames++
        }else{
            pendingViewMode=observed
            pendingViewFrames =1
        }
        if(pendingViewFrames>=config.orientationStableFrames){
            detectedViewMode=observed
            pendingViewMode=null
            pendingViewFrames =0
        }

    }
    private fun effectiveViewModel(): SquatViewMode =
        if(viewMode == SquatViewMode.AUTO) detectedViewMode else viewMode


    private fun otherSide(side: BodySide): BodySide = if(side == leftSide) rightSide else leftSide

    private fun visibilityOf(landmarks: List<PoseLandmarkPoint>, side: BodySide) = minOf(
        landmarks[side.shoulder].visibility,
        landmarks[side.hip].visibility,
        landmarks[side.knee].visibility,
        landmarks[side.ankle].visibility
    )

    /** While standing the better visible leg is chosen in every frame. During a rep the leg chosen
     * at the start of the descent is kept; if it is not visible enough, the frame is skipped. */
    private fun sideForFrame(landmarks: List<PoseLandmarkPoint>): BodySide?{
        val locked = repSide
        if(phase == SquatPhase.STANDING || locked == null) return selectReliableSide(landmarks)
        if(landmarks.size <= PoseLandmarkIndex.RIGHT_ANKLE) return null
        return if(visibilityOf(landmarks, locked) >= config.minVisibility) locked else null
    }

    private fun selectReliableSide(landmarks: List<PoseLandmarkPoint>): BodySide?{

        if(landmarks.size <= PoseLandmarkIndex.RIGHT_ANKLE) return null

        val left = visibilityOf(landmarks, leftSide)
        val right = visibilityOf(landmarks, rightSide)
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

        /** Used when no upright frame of the leg was seen yet */
        val DEFAULT_BASELINE = KneeBaseline(kneeAngleDeg = 180.0, kneeOffsetRatio = 0.0)
    }
}


