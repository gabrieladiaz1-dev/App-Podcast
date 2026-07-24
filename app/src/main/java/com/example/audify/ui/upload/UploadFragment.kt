package com.example.audify.ui.upload

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
<<<<<<< HEAD
import com.example.audify.LoginActivity
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.databinding.FragmentUploadBinding
=======
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.audify.LoginActivity
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import com.example.audify.data.DraftsManager
>>>>>>> 1b10f94c7f0acd7d0da8896266b4e4f50e09e020
import com.example.audify.data.MockData
import com.example.audify.databinding.FragmentUploadBinding
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private var audioUri: Uri? = null
    private var coverUri: Uri? = null
    private var audioFileName: String? = null
    private var selectedCategory: String? = null
    private var selectedCategoryId: String = ""

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var recordedFile: File? = null

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private val handler = Handler(Looper.getMainLooper())
    private var isDraftMode = false
    private var editingDraftId: String? = null

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "audio_desconocido"
        val cursor: Cursor? = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            if (idx >= 0) name = it.getString(idx)
        }
        return name
    }

    // ── Image picker: Photo Picker (funciona en emuladores) ──
    private val imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            coverUri = uri
            binding.imgCover.setImageURI(uri)
            binding.imgCover.visibility = View.VISIBLE
            binding.imgCoverPlaceholder.visibility = View.GONE
            binding.txtCoverLabel.text = getString(R.string.upload_cover_label)
        }
    }

    // ── Audio picker: GetContent como fallback ──
    private val audioPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            stopPlayback()
            audioUri = uri
            audioFileName = getFileNameFromUri(uri)
            recordedFile = null
            showAudioPreview(uri)
        }
    }

    // ── Audio fallback: Intent directo ──
    private val audioIntentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                stopPlayback()
                audioUri = uri
                audioFileName = getFileNameFromUri(uri)
                recordedFile = null
                showAudioPreview(uri)
            }
        }
    }

    private val recorderPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) toggleRecording()
        else Toast.makeText(requireContext(), R.string.upload_permission_denied, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!SessionManager.isLoggedIn()) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            return
        }
<<<<<<< HEAD

        binding.cardCover.setOnClickListener { imagePicker.launch("image/*") }
=======
>>>>>>> 1b10f94c7f0acd7d0da8896266b4e4f50e09e020

        DraftsManager.init(requireContext())
        setupListeners()
        loadDraftIfPresent()
    }

    private fun setupListeners() {
        binding.cardCover.setOnClickListener { pickImage() }
        binding.btnSelectFile.setOnClickListener { pickAudio() }
        binding.btnRecord.setOnClickListener { requestRecordPermission() }
        binding.cardCategory.setOnClickListener { showCategoryDialog() }
        binding.btnPublish.setOnClickListener { attemptPublish() }
        binding.btnSaveDraft.setOnClickListener { saveDraft() }
        binding.btnPlayPauseAudio.setOnClickListener { togglePlayback() }

        binding.seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) {
                    val duration = mediaPlayer?.duration ?: 0
                    mediaPlayer?.seekTo((progress * duration) / 100)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun pickImage() {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun pickAudio() {
        // Try GetContent first (works on most emulators)
        try {
            audioPicker.launch("audio/*")
        } catch (e: Exception) {
            // Fallback: direct Intent
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "audio/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            audioIntentLauncher.launch(intent)
        }
    }

    private fun loadDraftIfPresent() {
        val draftId = arguments?.getString("draftId") ?: return
        val draft = DraftsManager.loadDraft(draftId) ?: return
        isDraftMode = true
        editingDraftId = draftId
        binding.edtTitle.setText(draft.title)
        binding.edtDescription.setText(draft.description)
        if (draft.category.isNotEmpty()) {
            selectedCategory = draft.category
            binding.txtSelectedCategory.text = draft.category
            binding.txtSelectedCategory.setTextColor(0xFF1E1B4B.toInt())
        }
        val audioFile = File(draft.audioFilePath)
        if (audioFile.exists()) {
            audioUri = Uri.fromFile(audioFile)
            audioFileName = draft.audioFileName
            recordedFile = audioFile
            showAudioPreview(audioUri!!)
        }
        draft.coverFilePath?.let { path ->
            val coverFile = File(path)
            if (coverFile.exists()) {
                coverUri = Uri.fromFile(coverFile)
                binding.imgCover.setImageURI(coverUri)
                binding.imgCover.visibility = View.VISIBLE
                binding.imgCoverPlaceholder.visibility = View.GONE
            }
        }
    }

    private fun requestRecordPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> toggleRecording()
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Permiso requerido")
                    .setMessage("Se necesita acceso al micrófono para grabar audio.")
                    .setPositiveButton("Conceder") { _, _ -> recorderPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            else -> recorderPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun toggleRecording() {
        if (isRecording) stopRecording() else startRecording()
    }

    private fun startRecording() {
        try {
            stopPlayback()
            val dir = requireContext().getExternalFilesDir(null) ?: requireContext().cacheDir
            recordedFile = File(dir, "grabacion_${System.currentTimeMillis()}.m4a")
            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(requireContext())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(recordedFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            binding.txtRecordLabel.text = getString(R.string.upload_stop)
            binding.imgMicIcon.setImageResource(R.drawable.ic_stop)
            binding.txtRecordLabel.setTextColor(0xFFD32F2F.toInt())
            audioUri = null
            audioFileName = null
            binding.cardAudioPreview.visibility = View.GONE
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.upload_recording_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (_: Exception) {}
        isRecording = false
        binding.txtRecordLabel.text = getString(R.string.upload_record)
        binding.imgMicIcon.setImageResource(R.drawable.ic_mic)
        binding.txtRecordLabel.setTextColor(0xFF1E1B4B.toInt())
        recordedFile?.let { file ->
            audioUri = Uri.fromFile(file)
            audioFileName = file.name
            showAudioPreview(audioUri!!)
        }
    }

    private fun showAudioPreview(uri: Uri) {
        binding.cardAudioPreview.visibility = View.VISIBLE
        binding.txtAudioFileName.text = audioFileName ?: "audio"
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), uri)
                prepare()
            }
            val duration = mediaPlayer?.duration ?: 0
            val durationStr = formatDuration(duration)
            binding.txtAudioDuration.text = durationStr
            binding.txtAudioTotalTime.text = durationStr
            binding.txtAudioCurrentTime.text = "0:00"
            binding.seekBarAudio.progress = 0
            binding.btnPlayPauseAudio.setImageResource(R.drawable.ic_play)
            isPlaying = false
            startSeekUpdate()
        } catch (e: Exception) {
            binding.txtAudioDuration.text = "??:??"
            binding.txtAudioTotalTime.text = "??:??"
        }
    }

    private fun togglePlayback() {
        if (isPlaying) stopPlayback() else startPlayback()
    }

    private fun startPlayback() {
        mediaPlayer?.let { mp ->
            mp.start()
            isPlaying = true
            binding.btnPlayPauseAudio.setImageResource(R.drawable.ic_pause)
            startSeekUpdate()
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) mp.pause()
            mp.seekTo(0)
        }
        isPlaying = false
        binding.btnPlayPauseAudio.setImageResource(R.drawable.ic_play)
        binding.seekBarAudio.progress = 0
        binding.txtAudioCurrentTime.text = "0:00"
        handler.removeCallbacksAndMessages(null)
    }

    private fun startSeekUpdate() {
        handler.removeCallbacksAndMessages(null)
        val update = object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val current = mp.currentPosition
                        val duration = mp.duration
                        if (duration > 0) {
                            binding.seekBarAudio.progress = (current * 100) / duration
                            binding.txtAudioCurrentTime.text = formatDuration(current)
                        }
                        handler.postDelayed(this, 300)
                    }
                }
            }
        }
        handler.postDelayed(update, 300)
    }

    private fun formatDuration(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${String.format("%02d", seconds)}"
    }

    private fun showCategoryDialog() {
        val categories = MockData.getCategories().toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.upload_category_dialog_title)
            .setItems(categories) { _, which ->
                selectedCategory = categories[which]
                selectedCategoryId = (which + 1).toString()
                binding.txtSelectedCategory.text = categories[which]
                binding.txtSelectedCategory.setTextColor(0xFF1E1B4B.toInt())
            }
            .show()
    }

    private fun attemptPublish() {
        val title = binding.edtTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.tilTitle.error = getString(R.string.upload_title_required)
            binding.tilTitle.requestFocus()
            return
        }
        binding.tilTitle.error = null

        val desc = binding.edtDescription.text.toString().trim()
        if (desc.isEmpty()) {
            binding.tilDescription.error = getString(R.string.upload_desc_required)
            binding.tilDescription.requestFocus()
            return
        }
        binding.tilDescription.error = null

        if (audioUri == null && recordedFile == null) {
            Toast.makeText(requireContext(), R.string.upload_audio_required, Toast.LENGTH_SHORT).show()
            return
        }

        showPublishConfirmation()
    }

    private fun showPublishConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.upload_dialog_title)
            .setMessage(R.string.upload_dialog_message)
            .setPositiveButton(R.string.upload_dialog_ok) { _, _ ->
                publishToSupabase()
            }
            .setNegativeButton(R.string.register_dialog_cancel, null)
            .setCancelable(false)
            .show()
    }

    private fun publishToSupabase() {
        val userId = SessionManager.getUserId() ?: return
        val title = binding.edtTitle.text.toString().trim()
        val desc = binding.edtDescription.text.toString().trim()

        setLoading(true)

        lifecycleScope.launch {
            try {
                // 1. Upload audio to "priv" (pending review)
                val audioBytes = getAudioBytes() ?: run {
                    setLoading(false)
                    showError("No se pudo leer el archivo de audio")
                    return@launch
                }
                val audioFileNameClean = audioFileName?.replace(Regex("[^a-zA-Z0-9._-]"), "_") ?: "audio.m4a"
                val audioPath = "${userId}/${UUID.randomUUID()}_$audioFileNameClean"
                val audioResult = SupabaseService.uploadAudio(bucketName = "priv", path = audioPath, audioBytes = audioBytes)
                if (audioResult.isFailure) {
                    setLoading(false)
                    showError("Error al subir el audio: ${audioResult.exceptionOrNull()?.message}")
                    return@launch
                }
                val audioUrl = SupabaseService.getPublicAudioUrl("priv", audioPath)

                // 2. Upload cover to "cover" bucket
                var coverUrl: String? = null
                coverUri?.let { uri ->
                    val coverBytes = SupabaseService.readUriToBytes(requireContext(), uri)
                    if (coverBytes != null) {
                        val coverPath = "${userId}/${UUID.randomUUID()}_cover.jpg"
                        val coverResult = SupabaseService.uploadCoverImage(coverPath, coverBytes)
                        if (coverResult.isSuccess) {
                            coverUrl = SupabaseService.getPublicCoverUrl(coverPath)
                        }
                    }
                }

                // 3. Insert podcast record
                val insertResult = SupabaseService.insertPodcast(
                    userId = userId,
                    title = title,
                    description = desc,
                    categoryId = selectedCategoryId,
                    audioUrl = audioUrl,
                    coverUrl = coverUrl
                )

                setLoading(false)

                if (insertResult.isSuccess) {
                    editingDraftId?.let { DraftsManager.deleteDraft(requireContext(), it) }
                    showSuccessAndNavigate()
                } else {
                    showError("Error al guardar el podcast: ${insertResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                setLoading(false)
                showError("Error inesperado: ${e.message}")
            }
        }
    }

    private fun getAudioBytes(): ByteArray? {
        val uri = audioUri ?: return null
        return SupabaseService.readUriToBytes(requireContext(), uri)
    }

    private fun saveDraft() {
        val title = binding.edtTitle.text.toString().trim()
        val desc = binding.edtDescription.text.toString().trim()

        if (audioUri == null && recordedFile == null) {
            Toast.makeText(requireContext(), R.string.upload_audio_required, Toast.LENGTH_SHORT).show()
            return
        }

        val audioFile = recordedFile ?: run {
            audioUri?.let { uri ->
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return@run null
                    val draftDir = File(requireContext().filesDir, "drafts")
                    draftDir.mkdirs()
                    val file = File(draftDir, "draft_audio_${System.currentTimeMillis()}.m4a")
                    file.outputStream().use { inputStream.copyTo(it) }
                    file
                } catch (e: Exception) { null }
            }
        }

        val audioPath = audioFile?.absolutePath ?: return
        val audioName = audioFileName ?: audioFile.name

        var coverPath: String? = null
        coverUri?.let { uri ->
            try {
                val bytes = SupabaseService.readUriToBytes(requireContext(), uri)
                if (bytes != null) {
                    val draftDir = File(requireContext().filesDir, "drafts")
                    draftDir.mkdirs()
                    val coverFile = File(draftDir, "draft_cover_${System.currentTimeMillis()}.jpg")
                    coverFile.outputStream().use { it.write(bytes) }
                    coverPath = coverFile.absolutePath
                }
            } catch (_: Exception) {}
        }

        val draft = DraftsManager.Draft(
            id = editingDraftId ?: UUID.randomUUID().toString(),
            title = title,
            description = desc,
            category = selectedCategory ?: "",
            audioFilePath = audioPath,
            audioFileName = audioName,
            coverFilePath = coverPath
        )

        DraftsManager.saveDraft(requireContext(), draft)
        Toast.makeText(requireContext(), getString(R.string.upload_draft_saved), Toast.LENGTH_SHORT).show()

        try {
            Navigation.findNavController(requireView()).navigate(R.id.podcastsFragment)
        } catch (_: Exception) {}
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.txtProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.scrollView.visibility = if (loading) View.INVISIBLE else View.VISIBLE
        binding.btnPublish.isEnabled = !loading
        binding.btnSaveDraft.isEnabled = !loading
    }

    private fun showError(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setIcon(R.drawable.ic_error)
            .setMessage(message)
            .setPositiveButton("Reintentar") { _, _ -> publishToSupabase() }
            .setNegativeButton("Cancelar") { _, _ -> resetForm() }
            .show()
    }

    private fun showSuccessAndNavigate() {
        AlertDialog.Builder(requireContext())
            .setTitle("Podcast enviado")
            .setIcon(R.drawable.ic_check_circle)
            .setMessage(getString(R.string.upload_success_message))
            .setPositiveButton("Ver mis podcasts") { _, _ ->
                resetForm()
                try {
                    Navigation.findNavController(requireView()).navigate(R.id.podcastsFragment)
                } catch (_: Exception) {}
            }
            .setCancelable(false)
            .show()
    }

    private fun resetForm() {
        binding.edtTitle.text?.clear()
        binding.tilTitle.error = null
        binding.edtDescription.text?.clear()
        binding.tilDescription.error = null
        selectedCategory = null
        selectedCategoryId = ""
        binding.txtSelectedCategory.text = getString(R.string.upload_select_category)
        binding.txtSelectedCategory.setTextColor(0xFF999999.toInt())
        stopPlayback()
        mediaPlayer?.release()
        mediaPlayer = null
        audioUri = null
        audioFileName = null
        recordedFile = null
        coverUri = null
        isDraftMode = false
        editingDraftId = null
        binding.imgCover.setImageDrawable(null)
        binding.imgCover.visibility = View.GONE
        binding.imgCoverPlaceholder.visibility = View.VISIBLE
        binding.txtCoverLabel.text = getString(R.string.upload_cover_label)
        binding.cardAudioPreview.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        try { mediaRecorder?.release() } catch (_: Exception) {}
        mediaRecorder = null
        try { mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
        _binding = null
    }
}
