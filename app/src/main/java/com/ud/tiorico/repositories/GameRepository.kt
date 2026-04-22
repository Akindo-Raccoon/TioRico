package com.ud.tiorico.repositories

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.Firebase
import com.ud.tiorico.model.ChatMessage
import com.ud.tiorico.model.GameState
import com.ud.tiorico.model.Player
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GameRepository {
    private val db = Firebase.database.reference

    suspend fun createRoom(roomId: String, host: Player): Result<String> = runCatching {
        val state = GameState(
            roomId   = roomId,
            hostUid  = host.uid,
            status   = "waiting",
            currentTurn = 1,
            maxTurns = 10
        )
        db.child("rooms").child(roomId).setValue(state).await()
        db.child("rooms").child(roomId).child("players").child(host.uid).setValue(host).await()
        roomId
    }

    suspend fun joinRoom(roomId: String, player: Player): Result<Unit> = runCatching {
        val snap = db.child("rooms").child(roomId).child("status").get().await()
        check(snap.exists()) { "Sala no encontrada" }
        check(snap.getValue(String::class.java) == "waiting") { "La partida ya inició" }
        db.child("rooms").child(roomId).child("players").child(player.uid).setValue(player).await()
    }

    suspend fun startGame(roomId: String): Result<Unit> = runCatching {
        db.child("rooms").child(roomId).child("status").setValue("playing").await()
    }

    fun observeRoom(roomId: String): Flow<GameState> = callbackFlow {
        val ref = db.child("rooms").child(roomId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                snap.getValue(GameState::class.java)?.let { trySend(it) }
            }
            override fun onCancelled(err: DatabaseError) {
                close(err.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun applyAction(roomId: String, uid: String, action: String): Result<Int> = runCatching {
        val playerRef = db.child("rooms").child(roomId).child("players").child(uid)
        val snap = playerRef.get().await()
        val player = checkNotNull(snap.getValue(Player::class.java)) { "Jugador no encontrado" }

        val delta = when (action) {
            "Ahorrar"  -> 100
            "Invertir" -> if ((0..1).random() == 1) (50..300).random() else -(50..200).random()
            "Gastar"   -> -(50..150).random()
            else       -> 0
        }
        val newMoney = (player.money + delta).coerceAtLeast(0)
        playerRef.child("money").setValue(newMoney).await()
        playerRef.child("isAlive").setValue(newMoney > 0).await()
        playerRef.child("lastAction").setValue(action).await()
        playerRef.child("hasActed").setValue(true).await()
        delta
    }

    suspend fun advanceTurn(roomId: String): Result<String> = runCatching {
        val roomRef  = db.child("rooms").child(roomId)
        val snap     = roomRef.get().await()
        val state    = checkNotNull(snap.getValue(GameState::class.java))

        // Evento aleatorio (solo en algunos turnos)
        val eventMsg = if ((0..2).random() == 0) {
            val events = listOf(
                "💸 Bono de mercado: +\$200 a todos" to  200,
                "📉 Crisis económica: -\$150 a todos" to -150,
                "🎰 Lotería: +\$500 al jugador con menos dinero" to 0,
                "🔥 Impuesto: -\$100 a todos" to -100
            )
            val (msg, delta) = events.random()
            if (delta != 0) applyEventToAll(roomId, state.players, delta)
            msg
        } else ""

        // Resetear hasActed de todos para el siguiente turno
        state.players.keys.forEach { uid ->
            roomRef.child("players").child(uid).child("hasActed").setValue(false).await()
        }

        roomRef.child("randomEvent").setValue(eventMsg).await()

        val nextTurn = state.currentTurn + 1
        if (nextTurn > state.maxTurns) {
            roomRef.child("status").setValue("finished").await()
        } else {
            roomRef.child("currentTurn").setValue(nextTurn).await()
        }
        eventMsg
    }

    private suspend fun applyEventToAll(
        roomId: String,
        players: Map<String, Player>,
        delta: Int
    ) {
        players.forEach { (uid, p) ->
            if (p.isAlive) {
                val newMoney = (p.money + delta).coerceAtLeast(0)
                val ref = db.child("rooms").child(roomId).child("players").child(uid)
                ref.child("money").setValue(newMoney).await()
                if (newMoney == 0) ref.child("isAlive").setValue(false).await()
            }
        }
    }

    suspend fun resetGame(roomId: String, players: Map<String, Player>): Result<Unit> = runCatching {
        val roomRef = db.child("rooms").child(roomId)
        roomRef.child("currentTurn").setValue(1).await()
        roomRef.child("status").setValue("playing").await()
        roomRef.child("randomEvent").setValue("").await()
        players.forEach { (uid, p) ->
            roomRef.child("players").child(uid)
                .setValue(p.copy(money = 1000, isAlive = true, lastAction = "", hasActed = false))
                .await()
        }
    }

    suspend fun sendMessage(roomId: String, msg: ChatMessage): Result<Unit> = runCatching {
        db.child("rooms").child(roomId).child("chat").push().setValue(msg).await()
    }

    fun observeChat(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = db.child("rooms").child(roomId).child("chat")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val msgs = snap.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                trySend(msgs)
            }
            override fun onCancelled(err: DatabaseError) {
                close(err.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}