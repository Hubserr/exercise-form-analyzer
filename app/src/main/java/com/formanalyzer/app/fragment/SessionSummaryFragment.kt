package com.formanalyzer.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.formanalyzer.app.R
import com.formanalyzer.app.data.AppDatabase
import com.formanalyzer.app.data.TrainingSessionRepository
import com.formanalyzer.app.data.UserSettingsRepository
import com.formanalyzer.app.databinding.FragmentSessionSummaryBinding
import com.formanalyzer.app.squat.CalorieEstimator
import com.formanalyzer.app.squat.CalorieMethod
import com.formanalyzer.app.squat.SessionSummary
import com.formanalyzer.app.squat.SquatErrorType
import kotlinx.coroutines.launch

/** Summary of a just finished set, loaded from the database by its id. */
class SessionSummaryFragment : Fragment() {

    private var _binding: FragmentSessionSummaryBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy {
        TrainingSessionRepository(AppDatabase.getInstance(requireContext()))
    }
    private val settings by lazy { UserSettingsRepository(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back to the camera screen, which starts a new set.
        binding.nextSetButton.setOnClickListener { findNavController().popBackStack() }
        // Back to the home screen, closing the camera too.
        binding.finishTrainingButton.setOnClickListener {
            findNavController().popBackStack(R.id.home_fragment, false)
        }

        val sessionId = requireArguments().getLong(ARG_SESSION_ID)
        viewLifecycleOwner.lifecycleScope.launch {
            val saved = repository.getSession(sessionId)
            if (saved == null) {
                findNavController().popBackStack()
            } else {
                showSummary(saved.summary)
            }
        }
    }

    private fun showSummary(summary: SessionSummary) {
        binding.summaryDateText.text = formatDateTime(summary.startedAtMs)
        binding.summaryRepsText.text = getString(R.string.summary_reps, summary.totalReps)
        binding.summaryCorrectText.text = getString(R.string.summary_correct, summary.correctReps)
        binding.summaryDepthText.text = getString(
            R.string.summary_too_shallow, summary.errorCount(SquatErrorType.INSUFFICIENT_DEPTH)
        )
        binding.summaryValgusText.text = getString(
            R.string.summary_knee_valgus, summary.errorCount(SquatErrorType.KNEE_VALGUS)
        )
        binding.summaryBackText.text = getString(
            R.string.summary_back_rounding, summary.errorCount(SquatErrorType.EXCESSIVE_BACK_ROUNDING)
        )
        binding.summaryDurationText.text =
            getString(R.string.summary_duration, formatDuration(summary.durationMs))

        val calories = CalorieEstimator.estimate(summary, settings.loadProfile())
        binding.summaryCaloriesText.text = getString(R.string.summary_calories, calories.kcal)
        binding.summaryCaloriesNote.text = getString(
            when (calories.method) {
                CalorieMethod.BODY_PROFILE -> R.string.summary_calories_personal
                CalorieMethod.PER_REP_DEFAULT -> R.string.summary_calories_general
            }
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        /** Id of the saved set to show, passed in the navigation arguments. */
        const val ARG_SESSION_ID = "sessionId"
    }
}