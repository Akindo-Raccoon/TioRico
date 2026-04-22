package com.ud.tiorico.viewmodel

data class LogState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isPassResetSent: Boolean = false,
    val errRespId: Int? = null,
    val errMessage: String? = null
)
