package com.example.audify.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class UploadUiState(
    val isLoading: Boolean = false,
    val categories: List<SupabaseService.Category> = emptyList(),
    val publishSuccess: Boolean = false,
    val publishError: String? = null,
    val sessionValid: Boolean = true
)

class UploadViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun loadCategories(onRedirectNeeded: () -> Unit) {
        if (_uiState.value.categories.isNotEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val sessionOk = withContext(Dispatchers.IO) {
                SupabaseService.ensureValidSession()
            }
            if (!sessionOk) {
                _uiState.update { it.copy(isLoading = false, sessionValid = false) }
                SessionManager.clearSession()
                onRedirectNeeded()
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                SupabaseService.getCategories()
            }
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(categories = result.getOrNull() ?: emptyList(), isLoading = false)
                } else {
                    it.copy(isLoading = false, publishError = "No pudimos cargar las categorías")
                }
            }
        }
    }

    fun publish(
        context: Context,
        title: String,
        description: String,
        categoryId: Long,
        audioUri: Uri?,
        coverUri: Uri?,
        editingDraftId: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onSessionExpired: () -> Unit
    ) {
        val userId = SessionManager.getUserId()
        if (userId.isNullOrEmpty()) {
            onSessionExpired()
            return
        }

        _uiState.update { it.copy(isLoading = true, publishSuccess = false, publishError = null) }

        viewModelScope.launch {
            val sessionOk = withContext(Dispatchers.IO) {
                SupabaseService.ensureValidSession()
            }
            if (!sessionOk) {
                _uiState.update { it.copy(isLoading = false) }
                SessionManager.clearSession()
                onSessionExpired()
                return@launch
            }

            try {
                val audioBytes = withContext(Dispatchers.IO) {
                    audioUri?.let { SupabaseService.readUriToBytes(context, it) }
                }
                if (audioBytes == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    onError("No se pudo leer el archivo de audio")
                    return@launch
                }

                val audioFileNameClean = "audio_${UUID.randomUUID()}.m4a"
                val audioPath = "${userId}/${UUID.randomUUID()}_$audioFileNameClean"

                val audioResult = withContext(Dispatchers.IO) {
                    SupabaseService.uploadAudio(bucketName = "priv", path = audioPath, audioBytes = audioBytes)
                }
                if (audioResult.isFailure) {
                    _uiState.update { it.copy(isLoading = false) }
                    val ex = audioResult.exceptionOrNull()
                    if (ex is SupabaseService.SessionExpiredException) {
                        onSessionExpired()
                    } else {
                        onError("No pudimos subir tu audio. Detalle: ${ex?.message ?: "error desconocido"}")
                    }
                    return@launch
                }

                val audioUrl = withContext(Dispatchers.IO) {
                    SupabaseService.getPublicAudioUrl("priv", audioPath)
                }

                var coverUrl: String? = null
                coverUri?.let { uri ->
                    val coverBytes = withContext(Dispatchers.IO) {
                        SupabaseService.readUriToBytes(context, uri)
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
                        description = description,
                        categoryId = categoryId,
                        audioUrl = audioUrl,
                        coverUrl = coverUrl
                    )
                }

                _uiState.update { it.copy(isLoading = false) }

                if (insertResult.isSuccess) {
                    editingDraftId?.let { com.example.audify.data.DraftsManager.deleteDraft(context, it) }
                    _uiState.update { it.copy(publishSuccess = true) }
                    onSuccess()
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
                    _uiState.update { it.copy(publishError = msg) }
                    onError(msg)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                onError("Algo salió mal inesperadamente")
            }
        }
    }
}
