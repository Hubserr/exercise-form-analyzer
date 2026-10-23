package com.formanalyzer.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.formanalyzer.app.squat.BodyProfile
import com.formanalyzer.app.squat.Sex

/** User settings and body profile, stored as simple key-value pairs. */
class UserSettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var voiceFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE, true)
        set(value) = prefs.edit { putBoolean(KEY_VOICE, value) }

    fun loadProfile() = BodyProfile(
        weightKg = prefs.floatOrNull(KEY_WEIGHT)?.toDouble(),
        heightCm = prefs.floatOrNull(KEY_HEIGHT)?.toDouble(),
        ageYears = if (prefs.contains(KEY_AGE)) prefs.getInt(KEY_AGE, 0) else null,
        sex = prefs.getString(KEY_SEX, null)?.let { name -> Sex.values().find { it.name == name } }
    )

    fun saveProfile(profile: BodyProfile) {
        prefs.edit {
            putOrRemove(KEY_WEIGHT, profile.weightKg)
            putOrRemove(KEY_HEIGHT, profile.heightCm)
            if (profile.ageYears != null) putInt(KEY_AGE, profile.ageYears) else remove(KEY_AGE)
            if (profile.sex != null) putString(KEY_SEX, profile.sex.name) else remove(KEY_SEX)
        }
    }

    private fun SharedPreferences.floatOrNull(key: String): Float? =
        if (contains(key)) getFloat(key, 0f) else null

    private fun SharedPreferences.Editor.putOrRemove(key: String, value: Double?) {
        if (value != null) putFloat(key, value.toFloat()) else remove(key)
    }

    private companion object {
        const val PREFS_NAME = "user_settings"
        const val KEY_VOICE = "voice_feedback_enabled"
        const val KEY_WEIGHT = "weight_kg"
        const val KEY_HEIGHT = "height_cm"
        const val KEY_AGE = "age_years"
        const val KEY_SEX = "sex"
    }
}