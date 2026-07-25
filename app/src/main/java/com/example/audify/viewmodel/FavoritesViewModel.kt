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

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val allFavorites: List<Podcast> = emptyList(),
    val filteredFavorites: List<Podcast> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null
)

class FavoritesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    fun loadFavorites() {
        val userId = SessionManager.getUserId() ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val result = SupabaseService.getFavoritePodcasts(userId)
                _uiState.update { state ->
                    val favs = result.getOrNull() ?: emptyList()
                    val filtered = if (state.searchQuery.isBlank()) favs
                    else favs.filter {
                        it.title.lowercase().contains(state.searchQuery) ||
                        it.author.lowercase().contains(state.searchQuery) ||
                        it.description.lowercase().contains(state.searchQuery)
                    }
                    state.copy(allFavorites = favs, filteredFavorites = filtered, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "No pudimos cargar tus favoritos") }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val q = query.lowercase()
            val filtered = state.allFavorites.filter {
                q.isEmpty() ||
                it.title.lowercase().contains(q) ||
                it.author.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
            state.copy(searchQuery = q, filteredFavorites = filtered)
        }
    }
}
