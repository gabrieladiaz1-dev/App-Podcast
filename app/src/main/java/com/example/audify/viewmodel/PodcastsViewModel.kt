package com.example.audify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audify.SupabaseService
import com.example.audify.model.Podcast
import com.example.audify.ui.adapter.CategoryUiItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StatusFilter { ALL, APPROVED, PENDING }

data class PodcastsUiState(
    val isLoading: Boolean = true,
    val profileName: String = "",
    val allPodcasts: List<Podcast> = emptyList(),
    val visiblePodcasts: List<Podcast> = emptyList(),
    val selectedStatus: StatusFilter = StatusFilter.ALL,
    val selectedCategory: String? = null,
    val approvedCount: Int = 0,
    val pendingCount: Int = 0,
    val categoryCards: List<CategoryUiItem> = emptyList(),
    val selectedKey: String = "status_all",
    val error: String? = null
)

class PodcastsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PodcastsUiState())
    val uiState: StateFlow<PodcastsUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            try {
                val profile = SupabaseService.getProfile()
                val name = profile.name.ifEmpty { "Usuario" }
                _uiState.update { it.copy(profileName = name) }
            } catch (_: Exception) {
                _uiState.update { it.copy(profileName = "Usuario") }
            }
        }
    }

    fun loadUserPodcasts() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val result = SupabaseService.getUserPodcasts()
                _uiState.update { state ->
                    val podcasts = result.getOrNull() ?: emptyList()
                    val approved = podcasts.count { it.approved }
                    val pending = podcasts.size - approved
                    val newState = state.copy(
                        allPodcasts = podcasts,
                        selectedStatus = StatusFilter.ALL,
                        selectedCategory = null,
                        isLoading = false,
                        approvedCount = approved,
                        pendingCount = pending
                    )
                    buildCategoryCards(newState)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(allPodcasts = emptyList(), isLoading = false) }
            }
        }
    }

    fun setStatusFilter(status: StatusFilter) {
        _uiState.update { state ->
            val newState = state.copy(selectedStatus = status, selectedCategory = null)
            buildCategoryCards(newState)
        }
    }

    fun toggleCategory(category: String) {
        _uiState.update { state ->
            val newCategory = if (state.selectedCategory == category) null else category
            val newState = state.copy(selectedCategory = newCategory)
            buildCategoryCards(newState)
        }
    }

    fun resetFilters() {
        _uiState.update { state ->
            val newState = state.copy(selectedStatus = StatusFilter.ALL, selectedCategory = null)
            buildCategoryCards(newState)
        }
    }

    private fun buildCategoryCards(state: PodcastsUiState): PodcastsUiState {
        val all = state.allPodcasts
        val approvedCount = all.count { it.approved }
        val pendingCount = all.size - approvedCount

        var effectiveCategory = state.selectedCategory
        if (effectiveCategory != null) {
            val exists = all.any { it.category.trim() == effectiveCategory }
            if (!exists) effectiveCategory = null
        }

        val statusFiltered = when (state.selectedStatus) {
            StatusFilter.ALL -> all
            StatusFilter.APPROVED -> all.filter { it.approved }
            StatusFilter.PENDING -> all.filter { !it.approved }
        }

        val categoryNames = statusFiltered.map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val cards = mutableListOf(
            CategoryUiItem("status_all", "Todos", all.size),
            CategoryUiItem("status_approved", "Aprobados", approvedCount),
            CategoryUiItem("status_pending", "En revision", pendingCount)
        )
        cards.addAll(
            categoryNames.map { name ->
                val count = statusFiltered.count { it.category.trim() == name }
                CategoryUiItem("cat_$name", name, count)
            }
        )

        val selectedKey = if (effectiveCategory == null) {
            when (state.selectedStatus) {
                StatusFilter.ALL -> "status_all"
                StatusFilter.APPROVED -> "status_approved"
                StatusFilter.PENDING -> "status_pending"
            }
        } else "cat_$effectiveCategory"

        val visible = statusFiltered.filter { podcast ->
            effectiveCategory.isNullOrBlank() || podcast.category.trim() == effectiveCategory
        }

        return state.copy(
            selectedCategory = effectiveCategory,
            categoryCards = cards,
            selectedKey = selectedKey,
            visiblePodcasts = visible
        )
    }
}
