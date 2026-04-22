package com.ud.tiorico.model

data class Player(
    val uid: String = "",
    val email: String = "",
    var money: Int = 1000,
    var isAlive: Boolean = true,
    var lastAction: String = ""
)
