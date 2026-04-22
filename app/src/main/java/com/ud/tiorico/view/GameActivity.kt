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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ud.tiorico.model.GameState
import com.ud.tiorico.ui.theme.TioRicoTheme
import com.ud.tiorico.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import com.ud.tiorico.ui.theme.*
import com.ud.tiorico.model.Player

class GameActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TioRicoTheme {
                GameScreen(
                    viewModel = viewModel,
                    onFinished = { goToResult() },
                    onExit     = { finish() }
                )
            }
        }
    }

    private fun goToResult() {
        startActivity(Intent(this, ResultActivity::class.java))
    }
}

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onFinished: () -> Unit,
    onExit: () -> Unit
) {
    val ui    by viewModel.ui.collectAsStateWithLifecycle()
    val state = ui.gameState
    val me    = state.players[viewModel.myUid]
    val isHost = state.hostUid == viewModel.myUid
    val allActed = state.players.values.filter { it.isAlive }.all { it.hasActed }

    LaunchedEffect(state.status) {
        if (state.status == "finished") onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeep, Color(0xFF1E2D26))))
    ) {
        GameHeader(state.roomId, state.currentTurn, state.maxTurns)

        AnimatedVisibility(visible = state.randomEvent.isNotBlank()) {
            Box(
                Modifier.fillMaxWidth()
                    .background(Gold.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(state.randomEvent, color = GoldLight, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        AnimatedVisibility(
            visible = ui.actionFeedback.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit  = fadeOut() + shrinkVertically()
        ) {
            val delta = ui.actionDelta ?: 0
            Box(
                Modifier.fillMaxWidth()
                    .background(if (delta >= 0) GreenOk.copy(alpha = 0.2f) else RedBad.copy(alpha = 0.2f))
                    .padding(10.dp)
            ) {
                Text(
                    ui.actionFeedback,
                    color   = if (delta >= 0) GreenOk else RedBad,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }
        }

        me?.let { MyMoneyCard(it) }

        PlayersTable(state.players, viewModel.myUid)

        if (me?.isAlive == true && !me.hasActed) {
            ActionButtons { action -> viewModel.doAction(action) }
        } else if (me?.isAlive == true && me.hasActed) {
            WaitingForOthers(allActed)
        }

        AnimatedVisibility(visible = isHost && allActed && state.status == "playing") {
            Button(
                onClick = { viewModel.nextTurn() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                Text("Siguiente turno ▶", color = BgDeep, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Divider(color = TextMuted.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        ChatSection(ui.messages, viewModel.myUid) { text -> viewModel.sendMessage(text) }

        TextButton(onClick = onExit, modifier = Modifier.align(Alignment.End).padding(end = 8.dp)) {
            Text("Salir", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GameHeader(roomId: String, current: Int, max: Int) {
    Row(
        Modifier.fillMaxWidth()
            .background(BgCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("🎩 Sala $roomId", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Tío Rico – Modo Supervivencia", color = TextMuted, fontSize = 11.sp)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$current/$max",
                color = GoldLight, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold
            )
            Text("Ronda", color = TextMuted, fontSize = 11.sp)
        }
    }

    LinearProgressIndicator(
        progress = { current.toFloat() / max.toFloat() },
        modifier = Modifier.fillMaxWidth().height(4.dp),
        color = Gold,
        trackColor = BgCard
    )
}


@Composable
private fun MyMoneyCard(me: Player) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Tu capital", color = TextMuted, fontSize = 12.sp)
                Text(
                    "\$${me.money}",
                    color = if (me.money > 300) Gold else RedBad,
                    fontSize = 28.sp, fontWeight = FontWeight.ExtraBold
                )
                if (me.lastAction.isNotBlank())
                    Text("Última: ${me.lastAction}", color = TextMuted, fontSize = 11.sp)
            }
            Box(
                Modifier.size(52.dp)
                    .background(
                        if (me.isAlive) GreenOk.copy(alpha = 0.2f) else RedBad.copy(alpha = 0.2f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (me.isAlive) "✅" else "💀", fontSize = 24.sp)
            }
        }
    }
}

// ─── Tabla de jugadores ───────────────────────────────────────────────────────
@Composable
private fun PlayersTable(players: Map<String, Player>, myUid: String) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("Jugadores", color = TextMuted, fontSize = 12.sp, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(6.dp))
        players.values.sortedByDescending { it.money }.forEach { p ->
            val isMe = p.uid == myUid
            Row(
                Modifier.fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isMe) Gold.copy(alpha = 0.08f) else BgCard.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = p.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = if (isMe) Gold else TextMuted,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier
                            .size(28.dp)
                            .background(BgCard, RoundedCornerShape(8.dp))
                            .wrapContentHeight(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            p.email.substringBefore("@") + if (isMe) " (tú)" else "",
                            color = TextW, fontSize = 13.sp
                        )
                        if (p.hasActed && p.isAlive)
                            Text("✓ Listo", color = GreenOk, fontSize = 11.sp)
                        else if (!p.isAlive)
                            Text("Eliminado", color = RedBad, fontSize = 11.sp)
                    }
                }
                Text(
                    text = if (p.isAlive) "\$${p.money}" else "—",
                    color = when {
                        !p.isAlive  -> RedBad
                        p.money > 500 -> GreenOk
                        p.money > 200 -> Gold
                        else          -> RedBad
                    },
                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(onAction: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Elige tu acción:", color = TextMuted, fontSize = 12.sp, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionChip("🏦", "Ahorrar", "+\$100 fijo", BlueSave, Modifier.weight(1f)) { onAction("Ahorrar") }
            ActionChip("📈", "Invertir", "±Aleatorio", Gold,    Modifier.weight(1f)) { onAction("Invertir") }
            ActionChip("💸", "Gastar",  "-\$50~150",  RedBad,  Modifier.weight(1f)) { onAction("Gastar") }
        }
    }
}

@Composable
private fun ActionChip(
    emoji: String, label: String, hint: String,
    color: Color, modifier: Modifier, onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(hint,  color = color.copy(alpha = 0.7f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun WaitingForOthers(allActed: Boolean) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .background(GreenOk.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!allActed) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = GreenOk)
            Spacer(Modifier.width(8.dp))
            Text(
                if (allActed) "✅ Todos listos — el host avanzará el turno"
                else "✓ Acción registrada — esperando a los demás...",
                color = GreenOk, fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ChatSection(
    messages: List<com.ud.tiorico.model.ChatMessage>,
    myUid: String,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("💬 Chat", color = TextMuted, fontSize = 12.sp, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
                .padding(8.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.uid == myUid
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Text(msg.senderName, color = if (isMe) Gold else Color(0xFF90CAF9), fontSize = 10.sp)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMe) BlueSave.copy(alpha = 0.5f) else BgDeep)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(msg.text, color = TextW, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text("Mensaje...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Gold,
                    unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                    focusedTextColor     = TextW,
                    unfocusedTextColor   = TextW,
                    cursorColor          = Gold
                )
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onSend(text); text = "" },
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) { Text("➤", color = BgDeep, fontWeight = FontWeight.Bold) }
        }
    }
}

//
//        Text(
//            text = "🎩 Tío Rico - Sala ${state.roomId}",
//            color = Color(0xFFFFD700),
//            fontSize = 20.sp,
//            fontWeight = FontWeight.Bold
//        )
//        Text(
//            text = "Turno ${state.currentTurn} / ${state.maxTurns}  •  ${state.status.uppercase()}",
//            color = Color.White.copy(alpha = 0.7f),
//            fontSize = 13.sp
//        )
//
//        if (state.randomEvent.isNotBlank()) {
//            Spacer(Modifier.height(4.dp))
//            Text("⚡ ${state.randomEvent}", color = Color(0xFFFFA500), fontSize = 13.sp)
//        }
//
//        Divider(color = Color(0xFFFFD700).copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
//
//        myPlayer?.let { p ->
//            Card(
//                colors = CardDefaults.cardColors(containerColor = Color(0xFF243447)),
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Column(Modifier.padding(12.dp)) {
//                    Text("💰 Tu dinero: \$${p.money}", color = Color(0xFFFFD700), fontSize = 18.sp, fontWeight = FontWeight.Bold)
//                    Text("Estado: ${if (p.isAlive) "✅ En juego" else "💀 Eliminado"}", color = Color.White, fontSize = 13.sp)
//                    if (p.lastAction.isNotBlank()) Text("Última acción: ${p.lastAction}", color = Color.Gray, fontSize = 12.sp)
//                }
//            }
//        }
//        Spacer(Modifier.height(8.dp))
//
//        Text("Jugadores", color = Color.White, fontWeight = FontWeight.SemiBold)
//        state.players.values.forEach { p ->
//            Row(
//                Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 2.dp),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(p.email.substringBefore("@"), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
//                Text(
//                    text = if (p.isAlive) "\$${p.money}" else "ELIMINADO",
//                    color = if (p.isAlive) Color(0xFF4CAF50) else Color.Red,
//                    fontSize = 13.sp
//                )
//            }
//        }
//
//        Divider(color = Color(0xFFFFD700).copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
//
//
//        if (state.status == "playing" && myPlayer?.isAlive == true) {
//            Text("Elige tu acción:", color = Color.White, fontWeight = FontWeight.SemiBold)
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                listOf("Ahorrar", "Invertir", "Gastar").forEach { action ->
//                    Button(
//                        onClick = { viewModel.doAction(action) },
//                        modifier = Modifier.weight(1f),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = when (action) {
//                                "Ahorrar"  -> Color(0xFF2E7D32)
//                                "Invertir" -> Color(0xFF1565C0)
//                                else       -> Color(0xFFC62828)
//                            }
//                        )
//                    ) { Text(action, fontSize = 12.sp) }
//                }
//            }
//            Spacer(Modifier.height(4.dp))
//            // Host avanza turno
//            Button(
//                onClick = { viewModel.nextTurn() },
//                modifier = Modifier.fillMaxWidth(),
//                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
//            ) { Text("Siguiente turno ▶", color = Color.Black, fontWeight = FontWeight.Bold) }
//        }
//
//        if (state.status == "finished") {
//            val winner = state.players.values.filter { it.isAlive }.maxByOrNull { it.money }
//            Card(
//                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)),
//                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
//            ) {
//                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
//                    Text("🏆 JUEGO TERMINADO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
//                    winner?.let { Text("Ganador: ${it.email.substringBefore("@")} (\$${it.money})", fontSize = 14.sp) }
//                        ?: Text("¡Todos eliminados!", fontSize = 14.sp)
//                }
//            }
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                Button(onClick = { viewModel.resetGame() }, modifier = Modifier.weight(1f)) { Text("🔄 Reiniciar") }
//                OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f)) { Text("Salir", color = Color.White) }
//            }
//        }
//
//        Divider(color = Color(0xFFFFD700).copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
//
//        Text("💬 Chat", color = Color.White, fontWeight = FontWeight.SemiBold)
//        val listState = rememberLazyListState()
//        val msgs = ui.messages
//        LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.lastIndex) }
//
//        LazyColumn(
//            state = listState,
//            modifier = Modifier
//                .weight(1f)
//                .fillMaxWidth()
//                .background(Color(0xFF162230), RoundedCornerShape(8.dp))
//                .padding(8.dp)
//        ) {
//            items(msgs) { msg ->
//                val isMe = msg.uid == viewModel.myUid
//                Column(
//                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
//                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
//                ) {
//                    Text(
//                        text = msg.senderName.substringBefore("@"),
//                        color = if (isMe) Color(0xFFFFD700) else Color(0xFF90CAF9),
//                        fontSize = 11.sp
//                    )
//                    Surface(
//                        color = if (isMe) Color(0xFF1565C0) else Color(0xFF243447),
//                        shape = RoundedCornerShape(8.dp)
//                    ) {
//                        Text(msg.text, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
//                    }
//                }
//            }
//        }
//
//        Spacer(Modifier.height(6.dp))
//        var chatText by remember { mutableStateOf("") }
//        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
//            OutlinedTextField(
//                value = chatText,
//                onValueChange = { chatText = it },
//                placeholder = { Text("Mensaje...", color = Color.Gray) },
//                modifier = Modifier.weight(1f),
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedTextColor = Color.White,
//                    unfocusedTextColor = Color.White,
//                    focusedBorderColor = Color(0xFFFFD700),
//                    unfocusedBorderColor = Color.Gray
//                ),
//                singleLine = true
//            )
//            Spacer(Modifier.width(8.dp))
//            Button(
//                onClick = { viewModel.sendMessage(chatText); chatText = "" },
//                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
//            ) { Text("➤", color = Color.Black, fontWeight = FontWeight.Bold) }
//        }
//    }
//}