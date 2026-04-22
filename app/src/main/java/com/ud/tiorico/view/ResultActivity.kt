package com.ud.tiorico.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ud.tiorico.model.Player
import com.ud.tiorico.ui.theme.TioRicoTheme
import com.ud.tiorico.viewmodel.GameViewModel
import com.ud.tiorico.ui.theme.*

class ResultActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TioRicoTheme {
                ResultScreen(
                    viewModel  = viewModel,
                    onRestart  = { viewModel.resetGame(); goToGame() },
                    onExit     = { goToLobby() }
                )
            }
        }
    }

    private fun goToGame() {
        startActivity(Intent(this, GameActivity::class.java))
        finish()
    }

    private fun goToLobby() {
        val intent = Intent(this, LobbyActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }
}

@Composable
fun ResultScreen(
    viewModel: GameViewModel,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val ui    by viewModel.ui.collectAsStateWithLifecycle()
    val state = ui.gameState
    val isHost = state.hostUid == viewModel.myUid

    // Clasificación: vivos primero (por dinero), luego eliminados
    val ranked = state.players.values
        .sortedWith(compareByDescending<Player> { it.isAlive }.thenByDescending { it.money })

    val winner = ranked.firstOrNull { it.isAlive }
    val iWon   = winner?.uid == viewModel.myUid

    // Animación de entrada
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeep, Color(0xFF1E2D26))))
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Trofeo / resultado personal ───────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(600)) + scaleIn(tween(600, easing = EaseOutBack))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (iWon) "🏆" else "💀", fontSize = 72.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (iWon) "¡Ganaste!" else "Eliminado",
                        fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (iWon) Gold else RedBad
                    )
                    Text(
                        text = if (iWon) "Sobreviviste todas las rondas" else "Tu capital llegó a \$0",
                        color = TextMuted, fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Clasificación ─────────────────────────────────────────────
            Text("Clasificación final", color = TextMuted, fontSize = 12.sp, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard)
            ) {
                Column(Modifier.padding(16.dp)) {
                    ranked.forEachIndexed { index, player ->
                        val isMe = player.uid == viewModel.myUid
                        val rankColor = when (index) {
                            0    -> Gold
                            1    -> Silver
                            2    -> Bronze
                            else -> TextMuted
                        }
                        val medal = when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${index + 1}." }

                        Row(
                            Modifier.fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isMe) Gold.copy(alpha = 0.08f) else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(medal, fontSize = 18.sp, modifier = Modifier.width(32.dp))
                                Column {
                                    Text(
                                        player.email.substringBefore("@") + if (isMe) " (tú)" else "",
                                        color = TextW, fontSize = 14.sp, fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        if (player.isAlive) "Sobrevivió ✅" else "Eliminado 💀",
                                        color = if (player.isAlive) GreenOk else RedBad,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Text(
                                "\$${player.money}",
                                color = rankColor, fontWeight = FontWeight.Bold, fontSize = 16.sp
                            )
                        }

                        if (index < ranked.lastIndex)
                            Divider(color = TextMuted.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Botones ───────────────────────────────────────────────────
            if (isHost) {
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text("🔄 Jugar de nuevo", color = BgDeep, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f))
            ) {
                Text("Salir al lobby", color = TextMuted, fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}