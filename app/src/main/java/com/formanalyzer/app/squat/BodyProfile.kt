package com.formanalyzer.app.squat

enum class Sex { MALE, FEMALE }

/**
 * Body data entered by the user, used to estimate calories.
 * Every field is optional - the estimate falls back to a simpler formula
 * when some data is missing.
 */
data class BodyProfile(
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val ageYears: Int? = null,
    val sex: Sex? = null
) {

    /** four fields  needed for a personal estimate. */
    val isComplete: Boolean
        get() = weightKg != null && heightCm != null && ageYears != null && sex != null

    val isEmpty: Boolean
        get() = weightKg == null && heightCm == null && ageYears == null && sex == null

    companion object {
        val WEIGHT_RANGE_KG = 30.0..250.0
        val HEIGHT_RANGE_CM = 100.0..230.0
        val AGE_RANGE_YEARS = 10..100

        /** An empty value is valid (not provided); a provided one must be in range. */
        fun isValidWeight(value: Double?) = value == null || value in WEIGHT_RANGE_KG
        fun isValidHeight(value: Double?) = value == null || value in HEIGHT_RANGE_CM
        fun isValidAge(value: Int?) = value == null || value in AGE_RANGE_YEARS
    }
}