package com.example.audify.ui.upload

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.audify.R
import com.example.audify.databinding.FragmentUploadBinding
import com.example.audify.data.MockData
import java.io.File

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private var audioUri: Uri? = null
    private var coverUri: Uri? = null
    private var audioFileName: String? = null
    private var selectedCategory: String? = null

    // Recording
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var recordedFile: File? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            coverUri = uri
            binding.imgCoverPlaceholder.visibility = View.GONE
            binding.imgCover.setImageURI(uri)
            binding.txtCoverLabel.text = getString(R.string.upload_cover_label)
        }
    }

    private val audioPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            val name = cursor?.use {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                it.moveToFirst()
                if (idx >= 0) it.getString(idx) else "audio_desconocido"
            } ?: "audio_desconocido"
            audioFileName = name
            binding.txtFileInfo.text = getString(R.string.upload_file_selected, name)
            binding.txtFileInfo.visibility = View.VISIBLE
        }
    }

    private val recorderPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) toggleRecording() else Toast.makeText(requireContext(), R.string.upload_permission_denied, Toast.LENGTH_SHORT).show()
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

        binding.cardCover.setOnClickListener { imagePicker.launch("image/*") }

        binding.btnSelectFile.setOnClickListener { audioPicker.launch("audio/*") }

        binding.btnRecord.setOnClickListener { requestRecordPermission() }

        binding.cardCategory.setOnClickListener { showCategoryDialog() }

        binding.btnPublish.setOnClickListener { attemptPublish() }
    }

    private fun showCategoryDialog() {
        val categories = MockData.getCategories().toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.upload_category_dialog_title)
            .setItems(categories) { _, which ->
                selectedCategory = categories[which]
                binding.txtSelectedCategory.text = categories[which]
                binding.txtSelectedCategory.setTextColor(0xFF1E1B4B.toInt())
            }
            .show()
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
            val dir = requireContext().cacheDir
            recordedFile = File(dir, "grabacion_${System.currentTimeMillis()}.mp3")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordedFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            binding.txtRecordLabel.text = getString(R.string.upload_stop)
            binding.imgMicIcon.setImageResource(R.drawable.ic_music_note)
            binding.txtFileInfo.text = getString(R.string.upload_recording)
            binding.txtFileInfo.visibility = View.VISIBLE
            audioUri = null
            audioFileName = null
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.upload_recording_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply { stop(); release() }
            mediaRecorder = null
        } catch (_: Exception) {}
        isRecording = false
        binding.txtRecordLabel.text = getString(R.string.upload_record)
        binding.imgMicIcon.setImageResource(R.drawable.ic_mic)

        recordedFile?.let { file ->
            audioUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            audioFileName = file.name
            binding.txtFileInfo.text = getString(R.string.upload_file_selected, file.name)
        }
    }

    private fun attemptPublish() {
        val title = binding.edtTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), R.string.upload_title_required, Toast.LENGTH_SHORT).show()
            return
        }
        val desc = binding.edtDescription.text.toString().trim()
        if (desc.isEmpty()) {
            Toast.makeText(requireContext(), R.string.upload_desc_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (audioUri == null && recordedFile == null) {
            Toast.makeText(requireContext(), R.string.upload_audio_required, Toast.LENGTH_SHORT).show()
            return
        }

        showPublishDialog()
    }

    private fun showPublishDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.upload_dialog_title)
            .setMessage(R.string.upload_dialog_message)
            .setPositiveButton(R.string.upload_dialog_ok) { _, _ -> resetForm() }
            .setCancelable(false)
            .show()
    }

    private fun resetForm() {
        binding.edtTitle.text?.clear()
        binding.edtDescription.text?.clear()
        selectedCategory = null
        binding.txtSelectedCategory.text = getString(R.string.upload_select_category)
        binding.txtSelectedCategory.setTextColor(0xFF999999.toInt())
        audioUri = null
        audioFileName = null
        recordedFile = null
        coverUri = null
        binding.imgCover.setImageResource(R.drawable.bg_circle_violet)
        binding.imgCoverPlaceholder.visibility = View.VISIBLE
        binding.txtCoverLabel.text = getString(R.string.upload_cover_label)
        binding.txtFileInfo.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        _binding = null
    }
}
