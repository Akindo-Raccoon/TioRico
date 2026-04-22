package com.ud.tiorico.model

data class GameState(
    val roomId: String = "",
    val hostUid: String = "",
    val currentTurn: Int = 1,
    val maxTurns: Int = 10,
    val status: String = "waiting",
    val randomEvent: String = "",
    val players: Map<String, Player> = emptyMap()
)
