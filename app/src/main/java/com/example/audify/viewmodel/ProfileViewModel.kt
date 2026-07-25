package com.example.audify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val email: String = "",
    val error: String? = null
)

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val email = SupabaseService.getCurrentUserEmail() ?: ""
                val profile = SupabaseService.getProfile()
                val name = profile.name.ifEmpty { email.substringBefore("@").ifEmpty { "Usuario" } }
                _uiState.update { it.copy(name = name, email = email, isLoading = false) }
            } catch (e: Exception) {
                val email = SupabaseService.getCurrentUserEmail() ?: ""
                val fallback = email.substringBefore("@").ifEmpty { "Usuario" }
                _uiState.update { it.copy(name = fallback, email = email, isLoading = false) }
            }
        }
    }

    fun updateProfile(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            SupabaseService.updateProfileName(name).onSuccess {
                _uiState.update { it.copy(name = name) }
                onSuccess()
            }.onFailure {
                onError("No pudimos guardar los cambios. Intenta de nuevo")
            }
        }
    }

    fun signOut() {
        SessionManager.clearSession()
        viewModelScope.launch {
            SupabaseService.signOut()
        }
    }
}
