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

data class UserProfileUiState(
    val isLoading: Boolean = true,
    val displayName: String = "Usuario",
    val avatarUrl: String? = null,
    val allPodcasts: List<Podcast> = emptyList(),
    val visiblePodcasts: List<Podcast> = emptyList(),
    val selectedCategory: String? = null,
    val categoryCards: List<CategoryUiItem> = emptyList(),
    val selectedKey: String = "all",
    val error: String? = null
)

class UserProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(userId: String, fallbackName: String? = null) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val profile = SupabaseService.getProfileByUserId(userId)
                val displayName = profile?.name?.ifEmpty { null }
                    ?: profile?.username?.ifBlank { null }
                    ?: fallbackName?.ifBlank { null }
                    ?: "Usuario"
                val avatarUrl = profile?.avatar_url

                val podcastsResult = SupabaseService.getPodcastsByUser(userId)
                val podcasts = if (podcastsResult.isSuccess) podcastsResult.getOrNull() ?: emptyList() else emptyList()

                _uiState.update { state ->
                    val newState = state.copy(
                        displayName = displayName,
                        avatarUrl = avatarUrl,
                        allPodcasts = podcasts,
                        isLoading = false
                    )
                    buildCategoryCards(newState)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleCategory(category: String) {
        _uiState.update { state ->
            val newCategory = if (state.selectedCategory == category) null else category
            val newState = state.copy(selectedCategory = newCategory)
            buildCategoryCards(newState)
        }
    }

    private fun buildCategoryCards(state: UserProfileUiState): UserProfileUiState {
        val all = state.allPodcasts
        var effectiveCategory = state.selectedCategory
        if (effectiveCategory != null && !all.any { it.category.trim() == effectiveCategory }) {
            effectiveCategory = null
        }

        val categoryNames = all.map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val cards = mutableListOf(CategoryUiItem("all", "Todos", all.size))
        cards.addAll(
            categoryNames.map { name ->
                CategoryUiItem("cat_$name", name, all.count { it.category.trim() == name })
            }
        )

        val selectedKey = effectiveCategory?.let { "cat_$it" } ?: "all"
        val visible = all.filter {
            effectiveCategory.isNullOrBlank() || it.category.trim() == effectiveCategory
        }

        return state.copy(
            selectedCategory = effectiveCategory,
            categoryCards = cards,
            selectedKey = selectedKey,
            visiblePodcasts = visible
        )
    }
}
