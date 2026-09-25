package com.example.clubhome.data.repository

import com.example.clubhome.data.remote.SupabaseClientManager // Importante agregar este import
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    // CORREGIDO: Sin el ".kt"
    private val client = SupabaseClientManager.client

    suspend fun signUp(emailInput: String, passwordInput: String, nameInput: String): UserInfo? {
        client.auth.signUpWith(Email) {
            email = emailInput
            password = passwordInput
            data = buildJsonObject {
                put("name", nameInput)
            }
        }
        return client.auth.currentUserOrNull()
    }

    suspend fun signIn(emailInput: String, passwordInput: String): UserInfo? {
        client.auth.signInWith(Email) {
            email = emailInput
            password = passwordInput
        }
        return client.auth.currentUserOrNull()
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun getCurrentUser(): UserInfo? {
        return client.auth.currentUserOrNull()
    }
}