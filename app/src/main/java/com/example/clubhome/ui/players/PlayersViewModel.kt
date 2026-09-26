package com.example.clubhome.ui.players

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubhome.data.model.Player
import com.example.clubhome.data.remote.SupabaseClientManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class PlayersViewModel : ViewModel() {

    private val client = SupabaseClientManager.client

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentTeamId: String = "general"

    /**
     * Obtiene el ID del usuario actualmente autenticado en Supabase Auth
     */
    private fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    /**
     * Comprueba si una cadena tiene el formato adecuado de UUID
     */
    private fun String?.isValidUuid(): Boolean {
        if (this.isNullOrBlank()) return false
        return try {
            UUID.fromString(this)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Sube un archivo de imagen al bucket "player_photos" en Supabase Storage
     */
    private suspend fun uploadPhotoToSupabase(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes()
                } ?: return@withContext null

                val fileName = "player_${UUID.randomUUID()}.jpg"

                val bucket = client.storage.from("player_photos")
                bucket.upload(path = fileName, data = bytes) {
                    upsert = false
                }

                bucket.publicUrl(fileName)
            } catch (e: Exception) {
                Log.e("PlayersViewModel", "Error al subir imagen a Supabase", e)
                null
            }
        }
    }

    /**
     * Carga los jugadores del usuario actual autenticado.
     * Si teamId es un UUID válido, filtra por ese equipo.
     */
    fun loadPlayersByTeam(teamId: String) {
        currentTeamId = teamId
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val userId = getCurrentUserId()
                if (userId == null) {
                    Log.e("PlayersViewModel", "No hay usuario autenticado al cargar jugadores")
                    _players.value = emptyList()
                    return@launch
                }

                val list = client.postgrest["players"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            if (teamId.isValidUuid()) {
                                eq("team_id", teamId)
                            }
                        }
                    }
                    .decodeList<Player>()

                _players.value = list
            } catch (e: Exception) {
                Log.e("PlayersViewModel", "Error al cargar jugadores", e)
                _players.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Agrega un nuevo jugador vinculándolo al user_id del usuario con sesión activa.
     */
    fun addPlayer(context: Context, name: String, number: String, position: String, photoUri: Uri?, teamId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val userId = getCurrentUserId()
                if (userId == null) {
                    Log.e("PlayersViewModel", "Error: No hay sesión activa de usuario para guardar el jugador")
                    return@launch
                }

                var photoUrl: String? = null
                if (photoUri != null) {
                    photoUrl = uploadPhotoToSupabase(context, photoUri)
                }

                // Asegurar que solo se pase un UUID válido o null
                val targetTeamId = teamId ?: currentTeamId
                val validTeamId = if (targetTeamId.isValidUuid()) targetTeamId else null

                val newPlayer = Player(
                    userId = userId,
                    teamId = validTeamId,
                    name = name,
                    number = number.toIntOrNull(),
                    position = position.ifBlank { null },
                    photoUrl = photoUrl
                )

                client.postgrest["players"].insert(newPlayer)

                // Recarga la lista con el identificador del contexto actual
                loadPlayersByTeam(targetTeamId)
            } catch (e: Exception) {
                Log.e("PlayersViewModel", "Error al insertar jugador en Supabase", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza un jugador existente respetando el user_id del propietario.
     */
    fun updatePlayer(context: Context, player: Player, newPhotoUri: Uri?) {
        val playerId = player.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val userId = getCurrentUserId() ?: return@launch

                var photoUrl = player.photoUrl
                if (newPhotoUri != null) {
                    val uploadedUrl = uploadPhotoToSupabase(context, newPhotoUri)
                    if (uploadedUrl != null) {
                        photoUrl = uploadedUrl
                    }
                }

                val updatedPlayer = player.copy(
                    userId = userId,
                    teamId = if (player.teamId?.isValidUuid() == true) player.teamId else null,
                    photoUrl = photoUrl
                )

                client.postgrest["players"].update(updatedPlayer) {
                    filter {
                        eq("id", playerId)
                        eq("user_id", userId)
                    }
                }
                loadPlayersByTeam(player.teamId ?: currentTeamId)
            } catch (e: Exception) {
                Log.e("PlayersViewModel", "Error al actualizar jugador", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePlayer(playerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val userId = getCurrentUserId() ?: return@launch

                client.postgrest["players"].delete {
                    filter {
                        eq("id", playerId)
                        eq("user_id", userId)
                    }
                }
                _players.value = _players.value.filter { it.id != playerId }
            } catch (e: Exception) {
                Log.e("PlayersViewModel", "Error al eliminar jugador", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}