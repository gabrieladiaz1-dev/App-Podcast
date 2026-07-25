package com.example.audify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import com.example.audify.model.Playlist
import com.example.audify.model.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListsUiState(
    val isLoading: Boolean = true,
    val playlists: List<Playlist> = emptyList(),
    val allPodcasts: List<Podcast> = emptyList(),
    val error: String? = null
)

class ListsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    fun loadPlaylists() {
        if (!SessionManager.isLoggedIn()) {
            _uiState.update { it.copy(playlists = emptyList(), isLoading = false) }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                if (!ensureSession()) {
                    _uiState.update { it.copy(playlists = emptyList(), isLoading = false) }
                    return@launch
                }
                val result = SupabaseService.getUserPlaylists()
                if (result.isSuccess) {
                    val supabasePlaylists = result.getOrNull() ?: emptyList()
                    val models = supabasePlaylists.map { ps ->
                        val itemsResult = SupabaseService.getPlaylistItems(ps.id)
                        val count = itemsResult.getOrNull()?.size ?: 0
                        Playlist(id = ps.id.hashCode(), supabaseId = ps.id, name = ps.name, podcastCount = count)
                    }
                    _uiState.update { it.copy(playlists = models, isLoading = false) }
                } else {
                    _uiState.update { it.copy(playlists = emptyList(), isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadAllPodcasts() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val result = SupabaseService.getAllPodcasts()
                _uiState.update {
                    it.copy(allPodcasts = result.getOrNull() ?: emptyList(), isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun createPlaylist(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (!ensureSession()) { onError("Tu sesión expiró"); return@launch }
            val result = SupabaseService.createPlaylist(name)
            if (result.isSuccess) {
                loadPlaylists()
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "No pudimos crear la lista")
            }
        }
    }

    fun deletePlaylist(playlistId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = SupabaseService.deletePlaylist(playlistId)
            if (result.isSuccess) {
                loadPlaylists()
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun getPlaylistPodcasts(playlistId: String, onResult: (List<Podcast>) -> Unit) {
        viewModelScope.launch {
            val result = SupabaseService.getPlaylistPodcasts(playlistId)
            onResult(result.getOrNull() ?: emptyList())
        }
    }

    fun addToPlaylist(playlistId: String, podcastId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = SupabaseService.addPodcastToPlaylist(playlistId, podcastId)
            onResult(result.isSuccess)
        }
    }

    fun removeFromPlaylist(playlistId: String, podcastId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = SupabaseService.removePodcastFromPlaylist(playlistId, podcastId)
            onResult(result.isSuccess)
        }
    }

    private suspend fun ensureSession(): Boolean {
        return SupabaseService.ensureValidSession()
    }
}
