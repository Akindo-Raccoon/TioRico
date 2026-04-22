package com.ud.tiorico.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ud.tiorico.model.ChatMessage
import com.ud.tiorico.model.GameUiState
import com.ud.tiorico.model.Player
import com.ud.tiorico.repositories.GameRepository
import com.ud.toolloop.viewmodel.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameRepository()
    private val session = UserSession(app)
    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    val myUid: String get() = session.getUserId() ?: ""
    val myEmail: String get() = session.getEmail() ?: ""

    fun joinRoom(roomId: String) {
        val player = Player(uid = myUid, email = myEmail, money = 1000)
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true)
            repo.joinRoom(roomId, player)
                .onSuccess {
                    observeRoom(roomId)
                    observeChat(roomId)
                }
                .onFailure { e -> _ui.value = _ui.value.copy(errMessage = e.message, isLoading = false) }
        }
    }

    private fun observeRoom(roomId: String) {
        viewModelScope.launch {
            repo.observeRoom(roomId).collect { state ->
                _ui.value = _ui.value.copy(gameState = state, isLoading = false)
            }
        }
    }

    private fun observeChat(roomId: String) {
        viewModelScope.launch {
            repo.observeChat(roomId).collect { msgs ->
                _ui.value = _ui.value.copy(messages = msgs)
            }
        }
    }

    fun doAction(action: String) {
        val roomId = _ui.value.gameState.roomId.ifBlank { return }
        viewModelScope.launch {
            repo.applyAction(roomId, myUid, action)
                .onSuccess { _ui.value = _ui.value.copy(actionResult = "Acción: $action aplicada") }
                .onFailure { e -> _ui.value = _ui.value.copy(errMessage = e.message) }
        }
    }

    fun nextTurn() {
        val state = _ui.value.gameState
        viewModelScope.launch {
            repo.advanceTurn(state.roomId, state.currentTurn, state.maxTurns)
        }
    }

    fun resetGame() {
        val state = _ui.value.gameState
        viewModelScope.launch {
            repo.resetGame(state.roomId, state.players)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val roomId = _ui.value.gameState.roomId.ifBlank { return }
        viewModelScope.launch {
            val msg = ChatMessage(
                uid = myUid,
                email = myEmail,
                text = text.trim(),
                timestamp = System.currentTimeMillis()
            )
            repo.sendMessage(roomId, msg)
        }
    }

    fun clearError() { _ui.value = _ui.value.copy(errMessage = null) }
}