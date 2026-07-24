package com.example.audify.ui.upload

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.audify.LoginActivity
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import com.example.audify.data.DraftsManager
import com.example.audify.databinding.FragmentUploadBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private var audioUri: Uri? = null
    private var coverUri: Uri? = null
    private var audioFileName: String? = null
    private var selectedCategory: String? = null
    private var selectedCategoryId: Long = 0
    private var categoriesLoaded: List<SupabaseService.Category> = emptyList()

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

    private val imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            coverUri = uri
            binding.imgCover.setImageURI(uri)
            binding.imgCover.visibility = View.VISIBLE
            binding.imgCoverPlaceholder.visibility = View.GONE
            binding.txtCoverLabel.text = getString(R.string.upload_cover_label)
        }
    }

    private val audioPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            stopPlayback()
            audioUri = uri
            audioFileName = getFileNameFromUri(uri)
            recordedFile = null
            showAudioPreview(uri)
        }
    }

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
            requireActivity().finish()
            return
        }

        DraftsManager.init(requireContext())
        setupListeners()
        loadDraftIfPresent()
    }

    private fun setupListeners() {
        binding.cardCover.setOnClickListener { pickImage() }
        binding.btnSelectFile.setOnClickListener { pickAudio() }
        binding.btnRecord.setOnClickListener { requestRecordPermission() }
        binding.cardCategory.setOnClickListener { loadAndShowCategories() }
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
        try {
            audioPicker.launch("audio/*")
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "audio/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            audioIntentLauncher.launch(intent)
        }
    }

    private fun loadAndShowCategories() {
        if (categoriesLoaded.isNotEmpty()) {
            showCategoryDialog()
            return
        }
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Cargando...")
            .setMessage("Estamos buscando las categorías, un segundito")
            .setCancelable(false)
            .show()
        lifecycleScope.launch {
            val sessionOk = withContext(Dispatchers.IO) {
                SupabaseService.ensureValidSession()
            }
            if (!sessionOk) {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Tu sesión expiró, por favor inicia sesión de nuevo", Toast.LENGTH_LONG).show()
                SessionManager.clearSession()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                SupabaseService.getCategories()
            }
            progressDialog.dismiss()
            if (result.isSuccess) {
                categoriesLoaded = result.getOrNull() ?: emptyList()
                if (categoriesLoaded.isNotEmpty()) {
                    showCategoryDialog()
                } else {
                    Toast.makeText(requireContext(), "No se encontraron categorías", Toast.LENGTH_LONG).show()
                }
            } else {
                val err = result.exceptionOrNull()
                if (err is SupabaseService.SessionExpiredException) {
                    Toast.makeText(requireContext(), err.message, Toast.LENGTH_LONG).show()
                    SessionManager.clearSession()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                } else {
                    Toast.makeText(requireContext(), "No pudimos cargar las categorías. Intenta de nuevo", Toast.LENGTH_LONG).show()
                }
            }
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
        val categoryNames = categoriesLoaded.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.upload_category_dialog_title)
            .setItems(categoryNames) { _, which ->
                selectedCategory = categoryNames[which]
                selectedCategoryId = categoriesLoaded[which].id
                binding.txtSelectedCategory.text = categoryNames[which]
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

        if (selectedCategoryId == 0L) {
            Toast.makeText(requireContext(), "Selecciona una categoría", Toast.LENGTH_SHORT).show()
            return
        }

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
        val userId = SessionManager.getUserId()
        if (userId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No tienes sesión activa. Inicia sesión para subir podcasts", Toast.LENGTH_LONG).show()
            SessionManager.clearSession()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }
        val title = binding.edtTitle.text.toString().trim()
        val desc = binding.edtDescription.text.toString().trim()

        setLoading(true)

        lifecycleScope.launch {
            val sessionOk = withContext(Dispatchers.IO) {
                SupabaseService.ensureValidSession()
            }
            if (!sessionOk) {
                setLoading(false)
                Toast.makeText(requireContext(), "Tu sesión expiró. Inicia sesión de nuevo", Toast.LENGTH_LONG).show()
                SessionManager.clearSession()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
                return@launch
            }

            try {
                val audioBytes = getAudioBytes() ?: run {
                    setLoading(false)
                    showError("No se pudo leer el archivo de audio")
                    return@launch
                }
                val audioFileNameClean = audioFileName?.replace(Regex("[^a-zA-Z0-9._-]"), "_") ?: "audio.m4a"
                val audioPath = "${userId}/${UUID.randomUUID()}_$audioFileNameClean"
                Log.d("UploadFragment", "Subiendo audio a priv/$audioPath (${audioBytes.size} bytes)")
                val audioResult = withContext(Dispatchers.IO) {
                    SupabaseService.uploadAudio(bucketName = "priv", path = audioPath, audioBytes = audioBytes)
                }
                if (audioResult.isFailure) {
                    setLoading(false)
                    val ex = audioResult.exceptionOrNull()
                    val rawError = ex?.message ?: "error desconocido"
                    Log.e("UploadFragment", "Error subiendo audio: $rawError", ex)
                    if (ex is SupabaseService.SessionExpiredException) {
                        Toast.makeText(requireContext(), ex.message, Toast.LENGTH_LONG).show()
                        SessionManager.clearSession()
                        startActivity(Intent(requireContext(), LoginActivity::class.java))
                        requireActivity().finish()
                        return@launch
                    }
                    showError("No pudimos subir tu audio. Detalle: $rawError")
                    return@launch
                }
                val audioUrl = withContext(Dispatchers.IO) {
                    SupabaseService.getPublicAudioUrl("priv", audioPath)
                }

                var coverUrl: String? = null
                coverUri?.let { uri ->
                    val coverBytes = withContext(Dispatchers.IO) {
                        SupabaseService.readUriToBytes(requireContext(), uri)
                    }
                    if (coverBytes != null) {
                        val coverPath = "${userId}/${UUID.randomUUID()}_cover.jpg"
                        val coverResult = withContext(Dispatchers.IO) {
                            SupabaseService.uploadCoverImage(coverPath, coverBytes)
                        }
                        if (coverResult.isSuccess) {
                            coverUrl = withContext(Dispatchers.IO) {
                                SupabaseService.getPublicCoverUrl(coverPath)
                            }
                        }
                    }
                }

                val insertResult = withContext(Dispatchers.IO) {
                    SupabaseService.insertPodcast(
                        userId = userId,
                        title = title,
                        description = desc,
                        categoryId = selectedCategoryId,
                        audioUrl = audioUrl,
                        coverUrl = coverUrl
                    )
                }

                setLoading(false)

                if (insertResult.isSuccess) {
                    editingDraftId?.let { DraftsManager.deleteDraft(requireContext(), it) }
                    showSuccessAndNavigate()
                } else {
                    val ex = insertResult.exceptionOrNull()
                    val msg = when {
                        ex?.message?.contains("RLS", true) == true || ex?.message?.contains("row-level security", true) == true ->
                            "No tienes permiso para publicar podcasts. Revisa tu cuenta"
                        ex?.message?.contains("duplicate", true) == true ->
                            "Ya publicaste algo similar. ¿Querías subir otro?"
                        ex?.message?.contains("foreign key", true) == true || ex?.message?.contains("violates", true) == true ->
                            "Algo falló con la categoría seleccionada. Prueba elegir otra"
                        else -> "No pudimos guardar tu podcast. Intenta de nuevo"
                    }
                    showError(msg)
                }
            } catch (e: Exception) {
                setLoading(false)
                showError("Algo salió mal inesperadamente. No te preocupes, no se guardó nada raro")
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
            Navigation.findNavController(requireView()).navigate(R.id.profileFragment)
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
            .setTitle("Ups, algo falló")
            .setIcon(R.drawable.ic_error)
            .setMessage(message)
            .setPositiveButton("Intentar otra vez") { _, _ -> publishToSupabase() }
            .setNegativeButton("Cancelar") { _, _ -> resetForm() }
            .show()
    }

    private fun showSuccessAndNavigate() {
        AlertDialog.Builder(requireContext())
            .setTitle("¡Listo!")
            .setIcon(R.drawable.ic_check_circle)
            .setMessage("Tu podcast ya está en revisión. Pronto podrás verlo en tu perfil")
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
        selectedCategoryId = 0
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
