package com.formanalyzer.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.formanalyzer.app.data.AppDatabase
import com.formanalyzer.app.data.TrainingSessionRepository
import com.formanalyzer.app.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch
import com.formanalyzer.app.data.UserSettingsRepository

/** List of saved sets, newest first. */
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy {
        TrainingSessionRepository(AppDatabase.getInstance(requireContext()))
    }
    private val adapter by lazy {
        SessionHistoryAdapter(UserSettingsRepository(requireContext()).loadProfile())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
        binding.historyList.adapter = adapter

        // Collect the history only while the screen is visible.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeHistory().collect { sessions ->
                    adapter.submitList(sessions)
                    binding.historyEmptyText.isVisible = sessions.isEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.historyList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}