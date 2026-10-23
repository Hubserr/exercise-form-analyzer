package com.formanalyzer.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.formanalyzer.app.R
import com.formanalyzer.app.data.AppDatabase
import com.formanalyzer.app.data.TrainingSessionRepository
import com.formanalyzer.app.data.UserSettingsRepository
import com.formanalyzer.app.databinding.FragmentSettingsBinding
import com.formanalyzer.app.squat.BodyProfile
import com.formanalyzer.app.squat.Sex
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/** Body profile (for calorie estimates), voice feedback switch and history reset. */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val settings by lazy { UserSettingsRepository(requireContext()) }
    private val trainingRepository by lazy {
        TrainingSessionRepository(AppDatabase.getInstance(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showProfile(settings.loadProfile())

        binding.voiceSwitch.isChecked = settings.voiceFeedbackEnabled
        binding.voiceSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.voiceFeedbackEnabled = isChecked
        }

        binding.saveProfileButton.setOnClickListener { saveProfile() }
        binding.clearHistoryButton.setOnClickListener { confirmClearHistory() }
    }

    private fun showProfile(profile: BodyProfile) {
        binding.weightInput.setText(profile.weightKg?.let { formatNumber(it) }.orEmpty())
        binding.heightInput.setText(profile.heightCm?.let { formatNumber(it) }.orEmpty())
        binding.ageInput.setText(profile.ageYears?.toString().orEmpty())
        binding.sexGroup.check(
            when (profile.sex) {
                Sex.MALE -> R.id.sex_male
                Sex.FEMALE -> R.id.sex_female
                null -> R.id.sex_unset
            }
        )
    }

    private fun saveProfile() {
        val weight = readDecimal(binding.weightLayout)
        val height = readDecimal(binding.heightLayout)
        val age = readText(binding.ageLayout).toIntOrNull()

        val weightOk = validate(binding.weightLayout, weight, BodyProfile.isValidWeight(weight))
        val heightOk = validate(binding.heightLayout, height, BodyProfile.isValidHeight(height))
        val ageOk = validate(binding.ageLayout, age, BodyProfile.isValidAge(age))
        if (!weightOk || !heightOk || !ageOk) return

        val sex = when (binding.sexGroup.checkedRadioButtonId) {
            R.id.sex_male -> Sex.MALE
            R.id.sex_female -> Sex.FEMALE
            else -> null
        }

        val profile = BodyProfile(weight, height, age, sex)

        if (!profile.isComplete && !profile.isEmpty) {
            Toast.makeText(requireContext(), R.string.settings_profile_incomplete, Toast.LENGTH_LONG)
                .show()
            return
        }
        settings.saveProfile(profile)
        Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show()
    }

    private fun readText(layout: TextInputLayout): String =
        layout.editText?.text?.toString()?.trim().orEmpty()

    /** Accepts both a dot and a comma as the decimal separator. */
    private fun readDecimal(layout: TextInputLayout): Double? =
        readText(layout).replace(',', '.').toDoubleOrNull()

    /**
     * Shows an error under the field if the text is not a number or the number is
     * out of range. An empty field is valid and means "not provided".
     */
    private fun validate(layout: TextInputLayout, parsed: Number?, inRange: Boolean): Boolean {
        val text = readText(layout)
        val ok = text.isEmpty() || (parsed != null && inRange)
        layout.error = if (ok) null else getString(R.string.settings_invalid_value)
        return ok
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    private fun confirmClearHistory() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_clear_history)
            .setMessage(R.string.settings_clear_history_confirm)
            .setPositiveButton(R.string.settings_clear) { _, _ -> clearHistory() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun clearHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            trainingRepository.clearHistory()
            Toast.makeText(requireContext(), R.string.settings_history_cleared, Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}