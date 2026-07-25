package com.example.audify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import com.example.audify.model.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val podcast: Podcast? = null,
    val isFavorited: Boolean = false,
    val resolvedAudioUrl: String? = null,
    val audioResolved: Boolean = false,
    val queuePodcastIds: IntArray? = null,
    val queueIndex: Int = -1,
    val error: String? = null
)

class DetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadPodcast(podcastId: Int) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val podcast = SupabaseService.getPodcastByIntId(podcastId)
                _uiState.update {
                    it.copy(podcast = podcast, isLoading = podcast == null, error = if (podcast == null) "No encontramos ese podcast" else null)
                }
                if (podcast != null) {
                    checkFavoriteStatus(podcast.supabaseId)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar el podcast") }
            }
        }
    }

    private suspend fun checkFavoriteStatus(supabaseId: String) {
        val userId = SessionManager.getUserId() ?: return
        try {
            val isFav = SupabaseService.isFavorited(userId, supabaseId)
            _uiState.update { it.copy(isFavorited = isFav) }
        } catch (_: Exception) {}
    }

    fun resolveAudio() {
        val p = _uiState.value.podcast ?: return
        if (_uiState.value.audioResolved) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val url = SupabaseService.resolveAudioUrl(p.audioUrl, p.approved)
                _uiState.update { it.copy(resolvedAudioUrl = url, audioResolved = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleFavorite() {
        val p = _uiState.value.podcast ?: return
        val userId = SessionManager.getUserId() ?: return
        val wasFav = _uiState.value.isFavorited
        viewModelScope.launch {
            val result = if (wasFav) {
                SupabaseService.removeFavorite(userId, p.supabaseId)
            } else {
                SupabaseService.addFavorite(userId, p.supabaseId)
            }
            if (result.isSuccess) {
                _uiState.update { it.copy(isFavorited = !wasFav) }
            }
        }
    }
}
