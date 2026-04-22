package com.ud.tiorico.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ud.tiorico.ui.theme.TioRicoTheme
import com.ud.tiorico.viewmodel.AuthViewModel
import com.ud.tiorico.ui.theme.*
import com.ud.toolloop.viewmodel.util.UserSession

class AuthActivity(): ComponentActivity() {
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TioRicoTheme {
                AuthScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { goToGame() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (UserSession(applicationContext).isLogged()) goToGame()
    }

    fun goToGame(){
        startActivity(Intent(this, LobbyActivity::class.java))
        finish()
    }
}

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
){
    val logState by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(AuthState.LOGIN) }

    LaunchedEffect(logState.isSuccess) {
        if (logState.isSuccess) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Cream, CreamDark, Color(0xFFEADFC8)))
            )
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = (-80).dp)
                .background(SageLight.copy(alpha = 0.25f), shape = RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .background(Sage.copy(alpha = 0.12f), shape = RoundedCornerShape(50))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.linearGradient(listOf(Sage, SageDark)),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🎩", fontSize = 32.sp)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Tío Rico",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Modo Supervivencia",
                fontSize = 14.sp,
                color = TextSub,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(36.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(visible = mode != AuthState.FORGOT) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CreamDark),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            AuthTab("Iniciar sesión", mode == AuthState.LOGIN) {
                                mode = AuthState.LOGIN
                            }
                            AuthTab("Registrarse", mode == AuthState.REGISTER) {
                                mode = AuthState.REGISTER
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    AnimatedContent(
                        targetState = mode,
                        transitionSpec = {
                            fadeIn(tween(300)) + slideInHorizontally(
                                tween(300),
                                initialOffsetX = { if (targetState > initialState) it else -it }
                            ) togetherWith fadeOut(tween(200)) + slideOutHorizontally(
                                tween(200),
                                targetOffsetX = { if (targetState > initialState) -it else it }
                            )
                        },
                        label = "auth_anim"
                    ) { currentMode ->
                        when (currentMode) {
                            AuthState.LOGIN -> LoginPanel(
                                viewModel,
                                logState.isLoading,
                                logState.errMessage
                            ) { mode = AuthState.FORGOT }

                            AuthState.REGISTER -> RegisterPanel(
                                viewModel,
                                logState.isLoading,
                                logState.errMessage
                            )

                            AuthState.FORGOT -> ForgotPanel(
                                viewModel,
                                logState.isLoading,
                                logState.errMessage,
                                logState.isPassResetSent
                            ) { mode = AuthState.LOGIN }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.AuthTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Sage else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else TextSub
        )
    }
}

@Composable
private fun LoginPanel(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    errMessage: String?,
    onForgot: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var pass  by rememberSaveable { mutableStateOf("") }
    var showPass by rememberSaveable { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AuthField(email, { email = it }, "Correo electrónico", Icons.Default.Email, KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        AuthField(
            value = pass, onValue = { pass = it },
            label = "Contraseña", icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPass = !showPass }) {
                    Icon(
                        imageVector = if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null, tint = TextSub
                    )
                }
            }
        )

        errMessage?.let { ErrorBox(it) }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "¿Olvidaste tu contraseña?",
            fontSize = 13.sp, color = Sage,
            modifier = Modifier
                .align(Alignment.End)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onForgot
                )
                .padding(vertical = 4.dp)
        )

        Spacer(Modifier.height(20.dp))
        AuthButton("Iniciar sesión", isLoading) {
            viewModel.login(email, pass) {}
        }
    }
}

@Composable
private fun RegisterPanel(viewModel: AuthViewModel, isLoading: Boolean, errMessage: String?) {
    var name    by rememberSaveable { mutableStateOf("") }
    var idType  by rememberSaveable { mutableStateOf("CC") }
    var idNum   by rememberSaveable { mutableStateOf("") }
    var email   by rememberSaveable { mutableStateOf("") }
    var pass    by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPass    by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val idTypes = listOf("CC", "TI", "CE", "Pasaporte")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AuthField(name, { name = it }, "Nombre completo", Icons.Default.Person)
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(0.45f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMain),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SageLight)
                ) {
                    Text(idType, fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("▾", fontSize = 12.sp, color = TextSub)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    idTypes.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t, color = TextMain, fontSize = 14.sp) },
                            onClick = { idType = t; expanded = false }
                        )
                    }
                }
            }
            Box(Modifier.weight(0.55f)) {
                AuthField(idNum, { idNum = it }, "Número", Icons.Default.Person, KeyboardType.Number)
            }
        }

        Spacer(Modifier.height(12.dp))
        AuthField(email, { email = it }, "Correo electrónico", Icons.Default.Email, KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        AuthField(
            value = pass, onValue = { pass = it }, label = "Contraseña",
            icon = Icons.Default.Lock, keyboardType = KeyboardType.Password,
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPass = !showPass }) {
                    Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextSub)
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        AuthField(
            value = confirm, onValue = { confirm = it }, label = "Confirmar contraseña",
            icon = Icons.Default.Lock, keyboardType = KeyboardType.Password,
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showConfirm = !showConfirm }) {
                    Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextSub)
                }
            }
        )

        errMessage?.let { ErrorBox(it) }

        Spacer(Modifier.height(20.dp))
        AuthButton("Crear cuenta", isLoading) {
            viewModel.register(name, idType, idNum, email, pass, confirm) {}
        }
    }
}

@Composable
private fun ForgotPanel(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    errMessage: String?,
    isEmailSent: Boolean,
    onBack: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Recuperar contraseña",
            fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextMain
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Te enviaremos un enlace para restablecer tu contraseña.",
            fontSize = 13.sp, color = TextSub, textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(20.dp))

        if (isEmailSent) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SageLight.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✅", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("Revisa tu correo, te enviamos el enlace.", fontSize = 13.sp, color = SageDark)
                }
            }
            Spacer(Modifier.height(20.dp))
        } else {
            AuthField(email, { email = it }, "Correo electrónico", Icons.Default.Email, KeyboardType.Email)
            errMessage?.let { ErrorBox(it) }
            Spacer(Modifier.height(20.dp))
            AuthButton("Enviar enlace", isLoading) { viewModel.sendPasswordReset(email) }
            Spacer(Modifier.height(4.dp))
        }

        TextButton(onClick = onBack) {
            Text("← Volver al inicio de sesión", color = Sage, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Sage, modifier = Modifier.size(20.dp)) },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Sage,
            unfocusedBorderColor = SageLight,
            focusedLabelColor = Sage,
            unfocusedLabelColor = TextSub,
            focusedTextColor = TextMain,
            unfocusedTextColor = TextMain,
            cursorColor = Sage
        )
    )
}

@Composable
private fun AuthButton(label: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Sage,
            disabledContainerColor = SageLight
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
        }
    }
}

@Composable
private fun ErrorBox(message: String) {
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ErrorRed.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚠️", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(message, color = ErrorRed, fontSize = 13.sp, lineHeight = 16.sp)
    }
}

