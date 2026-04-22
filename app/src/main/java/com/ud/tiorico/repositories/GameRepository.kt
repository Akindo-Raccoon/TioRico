package com.ud.tiorico.repositories

import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
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
        require(roomId.isNotBlank()) { "El código de sala no puede estar vacío" }

        val existing = db.child("rooms").child(roomId).child("roomId").get().await()
        check(!existing.exists()) { "Ya existe una sala con ese código" }

        val roomRef = db.child("rooms").child(roomId)
        val updates: Map<String, Any> = mapOf(
            "roomId"                              to roomId,
            "hostUid"                             to host.uid,
            "currentTurn"                         to 1,
            "maxTurns"                            to 10,
            "status"                              to "waiting",
            "randomEvent"                         to "",
            "players/${host.uid.toSafeKey()}/uid"             to host.uid,
            "players/${host.uid.toSafeKey()}/email"           to host.email,
            "players/${host.uid.toSafeKey()}/money"           to 1000,
            "players/${host.uid.toSafeKey()}/isAlive"         to true,
            "players/${host.uid.toSafeKey()}/lastAction"      to "",
            "players/${host.uid.toSafeKey()}/hasActed"        to false
        )
        roomRef.updateChildren(updates).await()
        roomId
    }

    suspend fun joinRoom(roomId: String, player: Player): Result<Unit> = runCatching {
        require(roomId.isNotBlank()) { "El código de sala no puede estar vacío" }

        val statusSnap = db.child("rooms").child(roomId).child("status").get().await()
        check(statusSnap.exists()) { "Sala \"$roomId\" no encontrada" }
        check(statusSnap.getValue(String::class.java) == "waiting") { "La partida ya inició" }

        val updates: Map<String, Any> = mapOf(
            "players/${player.uid.toSafeKey()}/uid"        to player.uid,
            "players/${player.uid.toSafeKey()}/email"      to player.email,
            "players/${player.uid.toSafeKey()}/money"      to 1000,
            "players/${player.uid.toSafeKey()}/isAlive"    to true,
            "players/${player.uid.toSafeKey()}/lastAction" to "",
            "players/${player.uid.toSafeKey()}/hasActed"   to false
        )
        db.child("rooms").child(roomId).updateChildren(updates).await()
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
        val playerRef = db.child("rooms").child(roomId).child("players").child(uid.toSafeKey())
        val snap = playerRef.get().await()
        val player = checkNotNull(snap.getValue(Player::class.java)) { "Jugador no encontrado" }

        val delta = when (action) {
            "Ahorrar"  -> 100
            "Invertir" -> if ((0..1).random() == 1) (50..300).random() else -(50..200).random()
            "Gastar"   -> -(50..150).random()
            else       -> 0
        }
        val newMoney = (player.money + delta).coerceAtLeast(0)

        val updates: Map<String, Any> = mapOf(
            "money"      to newMoney,
            "isAlive"    to (newMoney > 0),
            "lastAction" to action,
            "hasActed"   to true
        )
        playerRef.updateChildren(updates).await()
        delta
    }

    suspend fun advanceTurn(roomId: String): Result<String> = runCatching {
        val roomRef = db.child("rooms").child(roomId)
        val snap    = roomRef.get().await()
        val state   = checkNotNull(snap.getValue(GameState::class.java))

        val eventMsg = if ((0..2).random() == 0) {
            val events = listOf(
                "💸 Bono de mercado: +\$200 a todos"             to  200,
                "📉 Crisis económica: -\$150 a todos"            to -150,
                "🔥 Impuesto especial: -\$100 a todos"           to -100,
                "🎁 Subsidio gubernamental: +\$50 a todos"       to   50
            )
            val (msg, delta) = events.random()
            if (delta != 0) applyEventToAll(roomId, state.players, delta)
            msg
        } else ""

        val resetUpdates = mutableMapOf<String, Any>()
        state.players.keys.forEach { uid ->
            resetUpdates["players/${uid.toSafeKey()}/hasActed"] = false
        }
        resetUpdates["randomEvent"] = eventMsg

        val nextTurn = state.currentTurn + 1
        if (nextTurn > state.maxTurns) {
            resetUpdates["status"] = "finished"
        } else {
            resetUpdates["currentTurn"] = nextTurn
        }
        roomRef.updateChildren(resetUpdates).await()
        eventMsg
    }

    private suspend fun applyEventToAll(roomId: String, players: Map<String, Player>, delta: Int) {
        val updates = mutableMapOf<String, Any>()
        players.forEach { (uid, p) ->
            if (p.isAlive) {
                val newMoney = (p.money + delta).coerceAtLeast(0)
                updates["players/${uid.toSafeKey()}/money"]   = newMoney
                updates["players/${uid.toSafeKey()}/isAlive"] = newMoney > 0
            }
        }
        if (updates.isNotEmpty()) db.child("rooms").child(roomId).updateChildren(updates).await()
    }

    suspend fun resetGame(roomId: String, players: Map<String, Player>): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "currentTurn"  to 1,
            "status"       to "playing",
            "randomEvent"  to ""
        )
        players.forEach { (uid, _) ->
            updates["players/${uid.toSafeKey()}/money"]      = 1000
            updates["players/${uid.toSafeKey()}/isAlive"]    = true
            updates["players/${uid.toSafeKey()}/lastAction"] = ""
            updates["players/${uid.toSafeKey()}/hasActed"]   = false
        }
        db.child("rooms").child(roomId).updateChildren(updates).await()
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

    private fun String.toSafeKey(): String =
        this.replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
}