package com.example.clubhome.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubhome.data.model.Player
import com.example.clubhome.data.remote.SupabaseClientManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayersViewModel : ViewModel() {

    private val client = SupabaseClientManager.client

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadPlayersByTeam(teamId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = client.postgrest["players"]
                    .select {
                        if (teamId.isNotBlank() && teamId != "general") {
                            filter {
                                eq("team_id", teamId)
                            }
                        }
                    }
                    .decodeList<Player>()
                _players.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addPlayer(player: Player) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                client.postgrest["players"].insert(player)
                loadPlayersByTeam(player.teamId ?: "general")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}