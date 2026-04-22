package com.ud.tiorico.model

data class GameUiState(
    val isLoading: Boolean = false,
    val gameState: GameState = GameState(),
    val messages: List<ChatMessage> = emptyList(),
    val actionDelta: Int? = null,
    val actionFeedback: String = "",
    val errMessage: String? = null
)
