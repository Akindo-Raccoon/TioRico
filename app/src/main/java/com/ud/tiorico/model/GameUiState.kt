package com.ud.tiorico.model

data class GameUiState(
    val isLoading: Boolean = false,
    val gameState: GameState = GameState(),
    val messages: List<ChatMessage> = emptyList(),
    val errMessage: String? = null,
    val actionResult: String = ""
)
