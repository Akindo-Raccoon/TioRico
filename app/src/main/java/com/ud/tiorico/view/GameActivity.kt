package com.ud.tiorico.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ud.tiorico.model.GameState
import com.ud.tiorico.ui.theme.TioRicoTheme
import com.ud.tiorico.viewmodel.GameViewModel
import kotlinx.coroutines.launch

class GameActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val roomId = intent.getStringExtra("ROOM_ID") ?: "sala1"
        viewModel.joinRoom(roomId)

        setContent {
            TioRicoTheme {
                GameScreen(
                    viewModel = viewModel,
                    onExit = { finish() }
                )
            }
        }
    }
}

@Composable
fun GameScreen(viewModel: GameViewModel, onExit: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val state = ui.gameState
    val myPlayer = state.players[viewModel.myUid]

    ui.errMessage?.let { err ->
        LaunchedEffect(err) {
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B2A3B))
            .padding(16.dp)
    ) {
        Text(
            text = "🎩 Tío Rico - Sala ${state.roomId}",
            color = Color(0xFFFFD700),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Turno ${state.currentTurn} / ${state.maxTurns}  •  ${state.status.uppercase()}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )

        if (state.randomEvent.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("⚡ ${state.randomEvent}", color = Color(0xFFFFA500), fontSize = 13.sp)
        }

        Divider(color = Color(0xFFFFD700).copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

        myPlayer?.let { p ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF243447)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("💰 Tu dinero: \$${p.money}", color = Color(0xFFFFD700), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Estado: ${if (p.isAlive) "✅ En juego" else "💀 Eliminado"}", color = Color.White, fontSize = 13.sp)
                    if (p.lastAction.isNotBlank()) Text("Última acción: ${p.lastAction}", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        Text("Jugadores", color = Color.White, fontWeight = FontWeight.SemiBold)
        state.players.values.forEach { p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(p.email.substringBefore("@"), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                Text(
                    text = if (p.isAlive) "\$${p.money}" else "ELIMINADO",
                    color = if (p.isAlive) Color(0xFF4CAF50) else Color.Red,
                    fontSize = 13.sp
                )
            }
        }

        Divider(color = Color(0xFFFFD700).copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))


        if (state.status == "playing" && myPlayer?.isAlive == true) {
            Text("Elige tu acción:", color = Color.White, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Ahorrar", "Invertir", "Gastar").forEach { action ->
                    Button(
                        onClick = { viewModel.doAction(action) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (action) {
                                "Ahorrar"  -> Color(0xFF2E7D32)
                                "Invertir" -> Color(0xFF1565C0)
                                else       -> Color(0xFFC62828)
                            }
                        )
                    ) { Text(action, fontSize = 12.sp) }
                }
            }
            Spacer(Modifier.height(4.dp))
            // Host avanza turno
            Button(
                onClick = { viewModel.nextTurn() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
            ) { Text("Siguiente turno ▶", color = Color.Black, fontWeight = FontWeight.Bold) }
        }

        if (state.status == "finished") {
            val winner = state.players.values.filter { it.isAlive }.maxByOrNull { it.money }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆 JUEGO TERMINADO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    winner?.let { Text("Ganador: ${it.email.substringBefore("@")} (\$${it.money})", fontSize = 14.sp) }
                        ?: Text("¡Todos eliminados!", fontSize = 14.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.resetGame() }, modifier = Modifier.weight(1f)) { Text("🔄 Reiniciar") }
                OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f)) { Text("Salir", color = Color.White) }
            }
        }

        Divider(color = Color(0xFFFFD700).copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

        Text("💬 Chat", color = Color.White, fontWeight = FontWeight.SemiBold)
        val listState = rememberLazyListState()
        val msgs = ui.messages
        LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.lastIndex) }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF162230), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            items(msgs) { msg ->
                val isMe = msg.uid == viewModel.myUid
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Text(
                        text = msg.email.substringBefore("@"),
                        color = if (isMe) Color(0xFFFFD700) else Color(0xFF90CAF9),
                        fontSize = 11.sp
                    )
                    Surface(
                        color = if (isMe) Color(0xFF1565C0) else Color(0xFF243447),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(msg.text, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        var chatText by remember { mutableStateOf("") }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = chatText,
                onValueChange = { chatText = it },
                placeholder = { Text("Mensaje...", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { viewModel.sendMessage(chatText); chatText = "" },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
            ) { Text("➤", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
    }
}