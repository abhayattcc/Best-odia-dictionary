package com.abhayattcc.dictionaryreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.abhayattcc.dictionaryreader.App
import com.abhayattcc.dictionaryreader.R
import com.abhayattcc.dictionaryreader.adapters.SuggestionsAdapter
import com.abhayattcc.dictionaryreader.databinding.FragmentDictionaryBinding
import com.abhayattcc.dictionaryreader.utils.TtsUtils
import com.abhayattcc.dictionaryreader.viewmodels.DictionaryViewModel
import com.abhayattcc.dictionaryreader.viewmodels.DictionaryViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DictionaryFragment : Fragment() {
    private lateinit var binding: FragmentDictionaryBinding
    private val viewModel: DictionaryViewModel by viewModels {
        DictionaryViewModelFactory(requireActivity().application, (requireActivity().application as App).database.dictionaryDao())
    }
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDictionaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply animations
        val bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce_animation)
        val wiggleAnimation = AnimationUtils.loadAnimation(context, R.anim.wiggle_animation)
        binding.searchBtn.startAnimation(bounceAnimation)
        binding.clearBtn.startAnimation(wiggleAnimation)
        binding.fileUpload.startAnimation(bounceAnimation)
        binding.odiaOcr.startAnimation(wiggleAnimation)

        binding.searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchJob?.cancel()
                searchJob = MainScope().launch {
                    delay(300)
                    viewModel.searchWord(s.toString(), false)
                }
            }
        })

        binding.searchBtn.setOnClickListener {
            viewModel.searchWord(binding.searchInput.text.toString(), true)
        }

        binding.clearBtn.setOnClickListener {
            binding.searchInput.text.clear()
            binding.resultText.text = ""
            binding.suggestionsRecyclerView.visibility = View.GONE
        }

        binding.fileUpload.setOnClickListener {
            // Implement file picker
        }

        binding.odiaOcr.setOnClickListener {
            // Implement Odia OCR
        }

        viewModel.results.observe(viewLifecycleOwner) { result ->
            binding.resultText.text = result
        }

        viewModel.suggestions.observe(viewLifecycleOwner) { suggestions ->
            binding.suggestionsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = SuggestionsAdapter(suggestions) { word ->
                    binding.searchInput.setText(word)
                    viewModel.searchWord(word, true)
                }
                visibility = if (suggestions.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }

        TtsUtils.initialize(requireContext()) { voices ->
            val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, voices.map { "${it.name} (${it.locale.language})" })
            binding.voiceSelect1.adapter = spinnerAdapter
            binding.voiceSelect2.adapter = spinnerAdapter
            binding.voiceSelect3.adapter = spinnerAdapter
        }

        binding.speakBtn.setOnClickListener {
            TtsUtils.speak(binding.resultText.text.toString(), binding.voiceSelect1.selectedItem.toString().substringAfter("(").substringBefore(")")) {
                binding.speakBtn.isEnabled = true
                binding.stopBtn.isEnabled = false
            }
            binding.speakBtn.isEnabled = false
            binding.stopBtn.isEnabled = true
        }

        binding.stopBtn.setOnClickListener {
            TtsUtils.stop()
            binding.speakBtn.isEnabled = true
            binding.stopBtn.isEnabled = false
        }
    }
}