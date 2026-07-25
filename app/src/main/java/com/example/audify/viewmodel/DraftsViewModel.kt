package com.example.audify.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audify.data.DraftsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DraftsUiState(
    val isLoading: Boolean = true,
    val drafts: List<DraftsManager.Draft> = emptyList()
)

class DraftsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DraftsUiState())
    val uiState: StateFlow<DraftsUiState> = _uiState.asStateFlow()

    fun loadDrafts(context: Context) {
        DraftsManager.init(context)
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val drafts = withContext(Dispatchers.IO) {
                DraftsManager.getAllDrafts()
            }
            _uiState.update { it.copy(drafts = drafts, isLoading = false) }
        }
    }

    fun deleteDraft(context: Context, draftId: String) {
        DraftsManager.deleteDraft(context, draftId)
        loadDrafts(context)
    }
}
