package com.formanalyzer.app.squat

enum class CalorieMethod { BODY_PROFILE, PER_REP_DEFAULT }

data class CalorieEstimate(val kcal: Double, val method: CalorieMethod)

/**
 * Estimates the energy expended during a set (gross, including resting
 * expenditure), choosing the most precise formula the profile allows:
 *
 *  - full profile: MET x (BMR / 24) x hours, with BMR from the Mifflin-St Jeor
 *    equation
 *  - weight only: MET x weight x hours, the standard MET formula,
 *  - no data: a flat value per repetition for a 70 kg reference person.
 *
 * Duration is measured from the start of the analysis to finishing the set,
 * so rests within a set are included. This is an estimate
 */
object CalorieEstimator {
    const val SQUAT_MET = 5.0

    /** About 3 s per rep for a 70 kg person at SQUAT_MET: 5 x 70 x 3 / 3600 = 0.29 kcal. */
    const val DEFAULT_KCAL_PER_REP = 0.3

    private const val MS_PER_HOUR = 3_600_000.0
    private const val HOURS_PER_DAY = 24.0

    /** Which formula was used: a personal one when the profile is complete. */

    fun estimate(summary: SessionSummary, profile: BodyProfile): CalorieEstimate {
        val bmr = basalMetabolicRate(profile)
            ?: return CalorieEstimate(
                summary.totalReps * DEFAULT_KCAL_PER_REP, CalorieMethod.PER_REP_DEFAULT
            )
        val hours = summary.durationMs / MS_PER_HOUR
        return CalorieEstimate(SQUAT_MET * bmr / HOURS_PER_DAY * hours, CalorieMethod.BODY_PROFILE)
    }

    /**
     * Resting energy expenditure in kcal per day (Mifflin-St Jeor equation),
     * or null if any of the required profile fields is missing.
     */
    fun basalMetabolicRate(profile: BodyProfile): Double? {
        val weight = profile.weightKg ?: return null
        val height = profile.heightCm ?: return null
        val age = profile.ageYears ?: return null
        val sexConstant = when (profile.sex ?: return null) {
            Sex.MALE -> 5.0
            Sex.FEMALE -> -161.0
        }
        return 10.0 * weight + 6.25 * height - 5.0 * age + sexConstant
    }
}