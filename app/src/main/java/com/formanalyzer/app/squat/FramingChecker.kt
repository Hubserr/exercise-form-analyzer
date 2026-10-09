package com.formanalyzer.app.squat

/** Body parts that must be visible before the analysis starts. */
enum class BodyPart { HEAD, TORSO, LEGS, FEET }

data class FramingStatus(
    val visibleParts: Set<BodyPart>,

    val stableFrames: Int,

    val isReady: Boolean
) {
    val allVisible: Boolean
        get() = visibleParts.size == BodyPart.values().size
}


class FramingChecker(
    private val minVisibility: Double = 0.5,
    private val requiredStableFrames: Int = 15
) {

    private var stableFrames = 0
    private var ready = false


    fun reset() {
        stableFrames = 0
        ready = false
    }

    fun update(landmarks: List<PoseLandmarkPoint>): FramingStatus {
        val visible = visibleParts(landmarks)
        stableFrames = if (visible.size == BodyPart.values().size) stableFrames + 1 else 0
        if (stableFrames >= requiredStableFrames) {
            ready = true
        }
        return FramingStatus(visible, stableFrames, ready)
    }

    private fun visibleParts(landmarks: List<PoseLandmarkPoint>): Set<BodyPart> {
        if (landmarks.size <= PoseLandmarkIndex.RIGHT_FOOT_INDEX) return emptySet()

        fun isVisible(index: Int): Boolean {
            val point = landmarks[index]
            return point.visibility >= minVisibility &&
                    point.x in 0.0..1.0 &&
                    point.y in 0.0..1.0
        }

        fun allVisible(vararg indices: Int) = indices.all { isVisible(it) }

        val parts = mutableSetOf<BodyPart>()
        if (isVisible(PoseLandmarkIndex.NOSE) ||
            isVisible(PoseLandmarkIndex.LEFT_EAR) ||
            isVisible(PoseLandmarkIndex.RIGHT_EAR)
        ) {
            parts += BodyPart.HEAD
        }
        if (allVisible(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.LEFT_HIP) ||
            allVisible(PoseLandmarkIndex.RIGHT_SHOULDER, PoseLandmarkIndex.RIGHT_HIP)
        ) {
            parts += BodyPart.TORSO
        }
        if (isVisible(PoseLandmarkIndex.LEFT_KNEE) || isVisible(PoseLandmarkIndex.RIGHT_KNEE)) {
            parts += BodyPart.LEGS
        }
        if (allVisible(
                PoseLandmarkIndex.LEFT_ANKLE, PoseLandmarkIndex.LEFT_HEEL,
                PoseLandmarkIndex.LEFT_FOOT_INDEX
            ) ||
            allVisible(
                PoseLandmarkIndex.RIGHT_ANKLE, PoseLandmarkIndex.RIGHT_HEEL,
                PoseLandmarkIndex.RIGHT_FOOT_INDEX
            )
        ) {
            parts += BodyPart.FEET
        }
        return parts
    }
}