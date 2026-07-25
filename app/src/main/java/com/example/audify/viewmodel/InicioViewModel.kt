package com.example.audify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import com.example.audify.model.Podcast
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InicioUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val allPodcasts: List<Podcast> = emptyList(),
    val filteredPodcasts: List<Podcast> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val favoriteIds: Set<String> = emptySet(),
    val error: String? = null
)

class InicioViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(InicioUiState())
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadPodcasts(fromSwipeRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { state ->
                if (fromSwipeRefresh) state.copy(isRefreshing = true)
                else state.copy(isLoading = true)
            }
            SupabaseService.getAllPodcasts()
                .onSuccess { podcasts ->
                    _uiState.update { state ->
                        val newState = state.copy(
                            allPodcasts = podcasts,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                        applyFilters(newState)
                    }
                    loadFavoriteIds()
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            allPodcasts = emptyList(),
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message ?: "No pudimos cargar los podcasts"
                        )
                    }
                }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state -> applyFilters(state.copy(searchQuery = query)) }
    }

    fun setSelectedCategory(category: String?) {
        _uiState.update { state -> applyFilters(state.copy(selectedCategory = category)) }
    }

    fun getCategories(): List<String> {
        return _uiState.value.allPodcasts.map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun toggleFavorite(supabaseId: String) {
        val userId = SessionManager.getUserId() ?: return
        val currentlyFav = _uiState.value.favoriteIds.contains(supabaseId)
        viewModelScope.launch {
            val result = if (currentlyFav) {
                SupabaseService.removeFavorite(userId, supabaseId)
            } else {
                SupabaseService.addFavorite(userId, supabaseId)
            }
            if (result.isSuccess) {
                _uiState.update { state ->
                    val newIds = if (currentlyFav) state.favoriteIds - supabaseId
                    else state.favoriteIds + supabaseId
                    state.copy(favoriteIds = newIds)
                }
            }
        }
    }

    private fun loadFavoriteIds() {
        val userId = SessionManager.getUserId() ?: return
        viewModelScope.launch {
            val ids = _uiState.value.allPodcasts.mapNotNull { podcast ->
                try {
                    if (SupabaseService.isFavorited(userId, podcast.supabaseId)) podcast.supabaseId else null
                } catch (_: Exception) { null }
            }.toSet()
            _uiState.update { it.copy(favoriteIds = ids) }
        }
    }

    private fun applyFilters(state: InicioUiState): InicioUiState {
        val q = state.searchQuery
        val filtered = state.allPodcasts.filter { podcast ->
            val matchesQuery = q.isBlank() ||
                podcast.title.contains(q, ignoreCase = true) ||
                podcast.author.contains(q, ignoreCase = true) ||
                podcast.description.contains(q, ignoreCase = true) ||
                podcast.category.contains(q, ignoreCase = true)
            val matchesCategory = state.selectedCategory.isNullOrBlank() ||
                podcast.category == state.selectedCategory
            matchesQuery && matchesCategory
        }
        return state.copy(filteredPodcasts = filtered)
    }
}
