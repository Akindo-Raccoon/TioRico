package com.ud.tiorico.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ud.tiorico.ui.theme.*
import com.ud.tiorico.ui.theme.TioRicoTheme
import com.ud.tiorico.viewmodel.GameViewModel

class LobbyActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TioRicoTheme {
                LobbyScreen(
                    viewModel   = viewModel,
                    onGameStart = { goToGame() },
                    onLogout    = { goToLogin() }
                )
            }
        }
    }

    private fun goToGame() {
        startActivity(Intent(this, GameActivity::class.java))
        finish()
    }

    private fun goToLogin() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}

@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    onGameStart: () -> Unit,
    onLogout: () -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.gameState.status) {
        if (ui.gameState.status == "playing") onGameStart()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Cream, CreamDark, Color(0xFFEADFC8))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.align(Alignment.TopEnd),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = { viewModel.logout { onLogout() } },
                        modifier = Modifier.background(GoldLight.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = BlueSave,
                            modifier = Modifier.size(28.dp).padding(2.dp)
                        )
                    }
                    Text("Salir", color = ErrorRed, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(32.dp))

            Box(
                Modifier
                    .size(72.dp)
                    .background(Brush.linearGradient(listOf(Sage, SageDark)), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) { Text("🎩", fontSize = 32.sp) }

            Spacer(Modifier.height(16.dp))
            Text("Tío Rico", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("Modo Supervivencia", fontSize = 13.sp, color = TextSub, letterSpacing = 1.sp)

            Spacer(Modifier.height(36.dp))

            when {
                ui.gameState.status == "waiting" && ui.gameState.roomId.isNotBlank() -> {
                    WaitingRoom(
                        viewModel = viewModel,
                        isHost    = ui.gameState.hostUid == viewModel.myUid
                    )
                }
                else -> {
                    LobbyOptions(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun LobbyOptions(viewModel: GameViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var mode     by rememberSaveable { mutableStateOf<String?>(null) }
    var roomCode by rememberSaveable { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                "¿Qué deseas hacer?",
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMain
            )
            Spacer(Modifier.height(20.dp))

            LobbyOptionButton("Crear sala", Icons.Default.Add, Sage) {
                mode = if (mode == "create") null else "create"
                roomCode = ""
                viewModel.clearError()
            }
            Spacer(Modifier.height(12.dp))
            LobbyOptionButton("Unirse a sala", Icons.Default.MeetingRoom, SageDark) {
                mode = if (mode == "join") null else "join"
                roomCode = ""
                viewModel.clearError()
            }

            AnimatedVisibility(
                visible = mode != null,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = if (mode == "create") "Elige un código para tu sala"
                        else "Ingresa el código de la sala",
                        fontSize = 13.sp, color = TextSub
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value       = roomCode,
                        onValueChange = {
                            roomCode = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(10)
                            if (ui.errMessage != null) viewModel.clearError()
                        },
                        placeholder = { Text("Ej: SALA1", color = Color.Gray, fontSize = 14.sp) },
                        singleLine  = true,
                        isError     = ui.errMessage != null,
                        supportingText = {
                            ui.errMessage?.let {
                                Text(it, color = ErrorRed, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Sage,
                            unfocusedBorderColor = SageLight,
                            errorBorderColor     = ErrorRed,
                            focusedTextColor     = TextMain,
                            unfocusedTextColor   = TextMain,
                            cursorColor          = Sage
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (mode == "create") viewModel.createRoom(roomCode)
                            else viewModel.joinRoom(roomCode)
                        },
                        enabled  = roomCode.length >= 3 && !ui.isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = Sage,
                            disabledContainerColor = SageLight
                        )
                    ) {
                        if (ui.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color       = Color.White
                            )
                        } else {
                            Text(
                                text       = if (mode == "create") "Crear y esperar" else "Unirme",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingRoom(viewModel: GameViewModel, isHost: Boolean) {
    val ui      by viewModel.ui.collectAsStateWithLifecycle()
    val players = ui.gameState.players.values.toList()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            // Código de sala destacado
            Text("Sala", fontSize = 13.sp, color = TextSub, letterSpacing = 1.sp)
            Text(
                text         = ui.gameState.roomId,
                fontSize     = 26.sp,
                fontWeight   = FontWeight.Bold,
                color        = Sage,
                letterSpacing = 4.sp
            )
            Text("Comparte este código con tus amigos", fontSize = 12.sp, color = TextSub)

            HorizontalDivider(
                color    = SageLight.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                "Jugadores (${players.size})",
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextMain
            )
            Spacer(Modifier.height(10.dp))

            players.forEach { p ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(SageLight.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = p.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color      = SageDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(p.email.substringBefore("@"), fontSize = 14.sp, color = TextMain)
                        if (p.uid == ui.gameState.hostUid)
                            Text("Host", fontSize = 11.sp, color = Sage)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (isHost) {
                Button(
                    onClick  = { viewModel.startGame() },
                    enabled  = players.size >= 1,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Sage,
                        disabledContainerColor = SageLight
                    )
                ) {
                    Text("🎮 Iniciar partida", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                Row(
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = Sage
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Esperando que el host inicie...", color = TextSub, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun LobbyOptionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape    = RoundedCornerShape(14.dp),
        border   = androidx.compose.foundation.BorderStroke(1.5.dp, color),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}