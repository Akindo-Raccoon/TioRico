package com.ud.tiorico.repositories

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

class AuthRepository (private val context: Context){
    private val auth: FirebaseAuth = Firebase.auth

    suspend fun loginWithEmailAndPassword(email: String, pass: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val userEmail = result.user?.email ?: "Usuario desconocido"
            Result.success(userEmail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmailAndPassword(email: String, pass: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val uid = result.user?.uid ?: ""
            val userEmail = result.user?.email ?: "Usuario Desconocido"
            Result.success("$uid|$userEmail")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(){
        auth.signOut()
    }

}