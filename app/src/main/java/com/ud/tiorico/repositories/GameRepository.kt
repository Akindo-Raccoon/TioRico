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

    suspend fun joinRoom(roomId: String, player: Player): Result<String> {
        return try {
            val roomRef = db.child("rooms").child(roomId)
            val snapshot = roomRef.child("currentTurn").get().await()

            if (!snapshot.exists()) {
                val initial = GameState(
                    roomId = roomId,
                    currentTurn = 1,
                    maxTurns = 10,
                    status = "waiting"
                )
                roomRef.child("currentTurn").setValue(initial.currentTurn).await()
                roomRef.child("maxTurns").setValue(initial.maxTurns).await()
                roomRef.child("status").setValue(initial.status).await()
            }

            roomRef.child("players").child(player.uid).setValue(player).await()
            Result.success(roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeRoom(roomId: String): Flow<GameState> = callbackFlow {
        val ref = db.child("rooms").child(roomId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val state = snap.getValue(GameState::class.java) ?: return
                trySend(state)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun applyAction(roomId: String, uid: String, action: String): Result<Unit> {
        return try {
            val playerRef = db.child("rooms").child(roomId).child("players").child(uid)
            val snap = playerRef.get().await()
            val player = snap.getValue(Player::class.java) ?: return Result.failure(Exception("Jugador no encontrado"))

            val delta = when (action) {
                "Ahorrar"  -> 100
                "Invertir" -> if ((0..1).random() == 1) (50..300).random() else -(50..200).random()
                "Gastar"   -> -(50..150).random()
                else       -> 0
            }
            val newMoney = player.money + delta
            playerRef.child("money").setValue(newMoney).await()
            playerRef.child("isAlive").setValue(newMoney > 0).await()
            playerRef.child("lastAction").setValue(action).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun advanceTurn(roomId: String, currentTurn: Int, maxTurns: Int): Result<Unit> {
        return try {
            val roomRef = db.child("rooms").child(roomId)
            val events = listOf("¡Bonificación! +200 a todos", "¡Crisis! -150 a todos", "Sin eventos", "Sin eventos")
            val event = events.random()

            if (event.contains("+")) {
                applyEventToAll(roomId, 200)
            } else if (event.contains("-")) {
                applyEventToAll(roomId, -150)
            }

            val next = currentTurn + 1
            roomRef.child("randomEvent").setValue(event).await()

            if (next > maxTurns) {
                roomRef.child("status").setValue("finished").await()
            } else {
                roomRef.child("currentTurn").setValue(next).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun applyEventToAll(roomId: String, delta: Int) {
        val playersRef = db.child("rooms").child(roomId).child("players")
        val snap = playersRef.get().await()
        snap.children.forEach { child ->
            val player = child.getValue(Player::class.java) ?: return@forEach
            if (player.isAlive) {
                val newMoney = (player.money + delta).coerceAtLeast(0)
                child.ref.child("money").setValue(newMoney).await()
                if (newMoney == 0) child.ref.child("isAlive").setValue(false).await()
            }
        }
    }

    suspend fun resetGame(roomId: String, players: Map<String, Player>): Result<Unit> {
        return try {
            val roomRef = db.child("rooms").child(roomId)
            roomRef.child("currentTurn").setValue(1).await()
            roomRef.child("status").setValue("playing").await()
            roomRef.child("randomEvent").setValue("").await()
            players.values.forEach { p ->
                roomRef.child("players").child(p.uid)
                    .setValue(p.copy(money = 1000, isAlive = true, lastAction = "")).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(roomId: String, msg: ChatMessage): Result<Unit> {
        return try {
            db.child("rooms").child(roomId).child("chat").push().setValue(msg).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeChat(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = db.child("rooms").child(roomId).child("chat")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val msgs = snap.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                trySend(msgs)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}