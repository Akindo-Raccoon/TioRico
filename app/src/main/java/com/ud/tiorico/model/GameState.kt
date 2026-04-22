package com.ud.tiorico.model

data class GameState(
    val roomId: String = "",
    val currentTurn: Int = 1,
    val maxTurns: Int = 10,
    val status: String = "waiting",
    val players: Map<String, Player> = emptyMap(),
    val randomEvent: String = ""
)
