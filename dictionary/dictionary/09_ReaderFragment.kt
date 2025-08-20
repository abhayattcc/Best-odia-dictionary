package com.abhayattcc.dictionaryreader.fragments

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.abhayattcc.dictionaryreader.App
import com.abhayattcc.dictionaryreader.R
import com.abhayattcc.dictionaryreader.databinding.PopupDictionaryBinding
import com.abhayattcc.dictionaryreader.utils.FileUtils
import com.abhayattcc.dictionaryreader.utils.TtsUtils
import com.abhayattcc.dictionaryreader.viewmodels.DictionaryViewModel
import com.abhayattcc.dictionaryreader.viewmodels.DictionaryViewModelFactory
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReaderFragment : Fragment() {
    private lateinit var binding: FragmentReaderBinding
    private val viewModel: DictionaryViewModel by viewModels {
        DictionaryViewModelFactory(requireActivity().application, (requireActivity().application as App).database.dictionaryDao())
    }
    private var textPages = mutableListOf<String>()
    private var currentPageIndex = 0
    private var isEditing = false
    private var isReadingAloud = false
    private var currentSentenceIndex = -1

    companion object {
        fun newInstance(text: String) = ReaderFragment().apply {
            arguments = Bundle().apply { putString("text", text) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReaderBinding.inflate(inflater, container, false)
        textPages = mutableListOf(arguments?.getString("text") ?: "")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply animations
        val bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce_animation)
        val wiggleAnimation = AnimationUtils.loadAnimation(context, R.anim.wiggle_animation)
        val glowAnimation = AnimationUtils.loadAnimation(context, R.anim.glow_animation)
        binding.fileUploadReader.startAnimation(bounceAnimation)
        binding.textEditBtn.startAnimation(wiggleAnimation)
        binding.playBtn.startAnimation(glowAnimation)
        binding.stopBtn.startAnimation(glowAnimation)
        binding.exportBtn.startAnimation(bounceAnimation)
        binding.closeReader.startAnimation(wiggleAnimation)

        renderTextPage()

        binding.fileUploadReader.setOnClickListener {
            filePicker.launch(arrayOf("application/pdf", "text/plain", "text/html"))
        }

        binding.textEditBtn.setOnClickListener {
            toggleEditText()
        }

        binding.closeReader.setOnClickListener {
            TtsUtils.stop()
            parentFragmentManager.popBackStack()
        }

        binding.playBtn.setOnClickListener {
            playSpeech()
        }

        binding.stopBtn.setOnClickListener {
            TtsUtils.stop()
            isReadingAloud = false
            binding.playBtn.text = "🗣️"
            binding.stopBtn.isEnabled = false
            binding.textContent.removeAllViews()
            renderTextPage()
        }

        binding.exportBtn.setOnClickListener {
            FileUtils.exportText(requireContext(), textPages.joinToString("\n\n"))
        }

        TtsUtils.initialize(requireContext()) { voices ->
            val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, voices.map { "${it.name} (${it.locale.language})" })
            binding.readerVoiceSelect1.adapter = spinnerAdapter
            binding.readerVoiceSelect2.adapter = spinnerAdapter
            binding.readerVoiceSelect3.adapter = spinnerAdapter
        }

        setupDoubleTapListener()
    }

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            val file = FileUtils.uriToFile(requireContext(), uri)
            if (file != null) {
                if (file.extension == "pdf") {
                    binding.pdfView.visibility = View.VISIBLE
                    binding.textContent.visibility = View.GONE
                    binding.pdfView.fromFile(file).load()
                } else {
                    binding.pdfView.visibility = View.GONE
                    binding.textContent.visibility = View.VISIBLE
                    textPages = mutableListOf(file.readText())
                    renderTextPage()
                }
            }
        }
    }

    private fun renderTextPage() {
        if (textPages.isEmpty()) {
            binding.textContent.text = "No text to display."
            return
        }
        val sentences = textPages[currentPageIndex].split("\n").filter { it.trim().isNotEmpty() }
        binding.textContent.text = sentences.joinToString("\n")
        currentSentenceIndex = -1
    }

    private fun toggleEditText() {
        if (isEditing) {
            textPages[currentPageIndex] = binding.textEditArea.text.toString()
            renderTextPage()
            binding.textContent.visibility = View.VISIBLE
            binding.textEditArea.visibility = View.GONE
            binding.textEditBtn.text = "✏️"
        } else {
            binding.textEditArea.setText(textPages[currentPageIndex])
            binding.textContent.visibility = View.GONE
            binding.textEditArea.visibility = View.VISIBLE
            binding.textEditBtn.text = "💾"
        }
        isEditing = !isEditing
    }

    private fun playSpeech() {
        if (!textPages.isNotEmpty()) return
        if (isReadingAloud) {
            TtsUtils.stop()
            isReadingAloud = false
            binding.playBtn.text = "🗣️"
            binding.stopBtn.isEnabled = false
            return
        }
        isReadingAloud = true
        currentSentenceIndex = if (currentSentenceIndex >= textPages[currentPageIndex].split("\n").size - 1) 0 else currentSentenceIndex + 1
        binding.playBtn.text = "⏸️"
        readNextSentence()
    }

    private fun readNextSentence() {
        val sentences = textPages[currentPageIndex].split("\n").filter { it.trim().isNotEmpty() }
        if (currentSentenceIndex >= sentences.size || !isReadingAloud) {
            TtsUtils.stop()
            isReadingAloud = false
            binding.playBtn.text = "🗣️"
            binding.stopBtn.isEnabled = false
            return
        }
        val text = sentences[currentSentenceIndex]
        binding.textContent.text = textPages[currentPageIndex].replace(text, "**$text**")
        TtsUtils.speak(text, binding.readerVoiceSelect1.selectedItem.toString().substringAfter("(").substringBefore(")")) {
            currentSentenceIndex++
            readNextSentence()
        }
    }

    private fun setupDoubleTapListener() {
        val detector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val textView = binding.textContent
                val offset = textView.getOffsetForPosition(e.x, e.y)
                val text = textView.text.toString()
                val word = text.substring(0, offset).split(" ").lastOrNull() ?: return false
                showPopup(word)
                return true
            }
        })
        binding.textContent.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
    }

    private fun showPopup(word: String) {
        val windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = android.view.Gravity.CENTER
        val binding = PopupDictionaryBinding.inflate(LayoutInflater.from(context))
        TtsUtils.initialize(requireContext()) { voices ->
            val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, voices.map { "${it.name} (${it.locale.language})" })
            binding.popupVoiceSelect1.adapter = spinnerAdapter
            binding.popupVoiceSelect2.adapter = spinnerAdapter
            binding.popupVoiceSelect3.adapter = spinnerAdapter
        }
        binding.popupClose.setOnClickListener {
            windowManager.removeView(binding.root)
        }
        binding.popupSpeak.setOnClickListener {
            TtsUtils.speak(binding.popupContent.text.toString(), binding.popupVoiceSelect1.selectedItem.toString().substringAfter("(").substringBefore(")")) {
                binding.popupSpeak.isEnabled = true
                binding.popupStop.isEnabled = false
            }
            binding.popupSpeak.isEnabled = false
            binding.popupStop.isEnabled = true
        }
        binding.popupStop.setOnClickListener {
            TtsUtils.stop()
            binding.popupSpeak.isEnabled = true
            binding.popupStop.isEnabled = false
        }
        windowManager.addView(binding.root, layoutParams)
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.searchWord(word, true)
            viewModel.results.observeForever {
                binding.popupContent.text = it
            }
        }
    }
}