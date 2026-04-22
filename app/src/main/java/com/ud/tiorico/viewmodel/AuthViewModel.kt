package com.ud.tiorico.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ud.tiorico.repositories.AuthRepository
import com.ud.toolloop.viewmodel.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(app: Application): AndroidViewModel(app) {
    private val authRepository = AuthRepository(app)
    private val _uiState = MutableStateFlow(LogState())
    private val session = UserSession(app.applicationContext)
    val uiState: StateFlow<LogState> = _uiState.asStateFlow()

    private fun handleAuthResult(result: Result<String>, onSuccess: () -> Unit) {
        result
            .onSuccess { raw ->
                val parts  = raw.split("|")
                val uid    = if (parts.size == 2) parts[0] else ""
                val email  = if (parts.size == 2) parts[1] else parts[0]

                session.saveUser(uid, email)
                _uiState.value = LogState(isSuccess = true)
                onSuccess()
            }
            .onFailure { e ->
                _uiState.value = LogState(errMessage = e.message)
            }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = LogState(errMessage = "Campos requeridos")
            return
        }
        viewModelScope.launch {
            _uiState.value = LogState(isLoading = true)
            val result = authRepository.loginWithEmailAndPassword(
                email = email.trim(), pass = pass.trim())

            handleAuthResult(result, onSuccess)
        }
    }

    fun isLogged(): Boolean = session.isLogged()

    fun register(
        name: String,
        idType: String,
        idNumber: String,
        email: String,
        pass: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        val errorResId = when {
            name.isBlank() || email.isBlank() || pass.isBlank() || idNumber.isBlank() ->
                "Campos requerido"
            pass != confirmPassword ->
                "Contrasenas no coinciden"
            pass.length < 6 ->
                "Contrasena muy corta"
            else -> null
        }

        if (errorResId != null) {
            _uiState.value = LogState(errMessage = errorResId)
            return
        }

        viewModelScope.launch {
            _uiState.value = LogState(isLoading = true)
            val result = authRepository.registerWithEmailAndPassword(email.trim(), pass.trim())
            handleAuthResult(result, onSuccess)
            _uiState.value = LogState()
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = LogState(errMessage = "Email no registrado")
            return
        }
        viewModelScope.launch {
            _uiState.value = LogState(isLoading = true)
            val result = authRepository.sendPasswordReset(email)
            result
                .onSuccess { _uiState.value = LogState(isPassResetSent = true) }
                .onFailure { e -> _uiState.value = LogState(errMessage = e.message) }
        }
    }


}