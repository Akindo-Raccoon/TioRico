package com.ud.tiorico.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ud.tiorico.model.ChatMessage
import com.ud.tiorico.model.GameUiState
import com.ud.tiorico.model.Player
import com.ud.tiorico.repositories.AuthRepository
import com.ud.tiorico.repositories.GameRepository
import com.ud.toolloop.viewmodel.util.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val repo     = GameRepository()
    private val repoAuth = AuthRepository(app)
    private val session  = UserSession(app)
    private val _ui      = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    val myUid: String   get() = session.getUserId() ?: ""
    val myEmail: String get() = session.getEmail()  ?: ""
    val myName: String  get() = myEmail.substringBefore("@")

    fun createRoom(roomId: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true, errMessage = null)
        val player = Player(uid = myUid, email = myEmail)

        repo.createRoom(roomId.trim().uppercase(), player)
            .onSuccess { id ->
                observe(id)
            }
            .onFailure { e ->
                _ui.value = _ui.value.copy(isLoading = false, errMessage = e.message)
            }
    }

    fun joinRoom(roomId: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true, errMessage = null)
        val player = Player(uid = myUid, email = myEmail)
        repo.joinRoom(roomId.trim().uppercase(), player)
            .onSuccess {
                observe(roomId.trim().uppercase())
            }
            .onFailure { e ->
                _ui.value = _ui.value.copy(isLoading = false, errMessage = e.message)
            }
    }

    private fun observe(roomId: String) {
        viewModelScope.launch {
            repo.observeRoom(roomId).collect { state ->
                _ui.value = _ui.value.copy(gameState = state, isLoading = false)
            }
        }
        viewModelScope.launch {
            repo.observeChat(roomId).collect { msgs ->
                _ui.value = _ui.value.copy(messages = msgs)
            }
        }
    }

    fun startGame() = viewModelScope.launch {
        repo.startGame(_ui.value.gameState.roomId)
    }

    fun doAction(action: String) = viewModelScope.launch {
        val roomId = _ui.value.gameState.roomId.ifBlank { return@launch }
        repo.applyAction(roomId, myUid, action)
            .onSuccess { delta ->
                val sign  = if (delta >= 0) "+" else ""
                val emoji = when {
                    action == "Ahorrar" -> "🏦"
                    delta > 0           -> "📈"
                    else                -> "📉"
                }
                _ui.value = _ui.value.copy(
                    actionDelta    = delta,
                    actionFeedback = "$emoji $action: $sign$$delta"
                )
                delay(2500)
                _ui.value = _ui.value.copy(actionDelta = null, actionFeedback = "")
            }
            .onFailure { e -> setError(e.message) }
    }

    fun nextTurn() = viewModelScope.launch {
        repo.advanceTurn(_ui.value.gameState.roomId)
    }

    fun resetGame() = viewModelScope.launch {
        repo.resetGame(_ui.value.gameState.roomId, _ui.value.gameState.players)
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val roomId = _ui.value.gameState.roomId.ifBlank { return }
        viewModelScope.launch {
            repo.sendMessage(
                roomId,
                ChatMessage(
                    uid        = myUid,
                    senderName = myName,
                    text       = text.trim(),
                    timestamp  = System.currentTimeMillis()
                )
            )
        }
    }

    fun logout(onLogout: () -> Unit) = viewModelScope.launch {
        repoAuth.logout()
        session.clearSession()
        _ui.value = GameUiState()
        onLogout()
    }

    fun clearError() { _ui.value = _ui.value.copy(errMessage = null) }

    private fun setError(msg: String?) {
        _ui.value = _ui.value.copy(isLoading = false, errMessage = msg ?: "Error desconocido")
    }
}