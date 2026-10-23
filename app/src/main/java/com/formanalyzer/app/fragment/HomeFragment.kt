package com.formanalyzer.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.formanalyzer.app.R
import com.formanalyzer.app.databinding.FragmentHomeBinding

/** Start screen: choose the exercise or open the training history. */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.startSquatButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_camera)
        }
        binding.openHistoryButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }
        binding.openSettingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}