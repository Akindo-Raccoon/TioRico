package com.ud.tiorico.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ud.tiorico.ui.theme.TioRicoTheme
import com.ud.tiorico.viewmodel.AuthViewModel
import com.ud.toolloop.viewmodel.util.UserSession
import kotlin.getValue

class AuthActivity(): ComponentActivity() {
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TioRicoTheme {
                AuthScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { goToHome() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (UserSession(applicationContext).isLogged()) goToHome()
    }

    fun goToHome(){
//        startActivity(Intent(this, HomeActivity::class.java))
//        finish()
    }
}

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
){
    val logState by viewModel.uiState.collectAsStateWithLifecycle()



}

