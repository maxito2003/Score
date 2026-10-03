package com.example.clubhome.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubhome.data.model.Match
import com.example.clubhome.data.remote.SupabaseClientManager
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveMatchesViewModel : ViewModel() {

    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMatches()
        listenToMatchesRealtime()
    }

    fun loadMatches() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Muestra los partidos que estén en vivo ("LIVE")
                val result = SupabaseClientManager.client.postgrest["matches"]
                    .select {
                        filter {
                            eq("status", "LIVE")
                        }
                    }
                    .decodeList<Match>()
                _matches.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun listenToMatchesRealtime() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val channel = SupabaseClientManager.client.channel("matches_live_list")

                // Escuchamos cualquier evento (INSERT, UPDATE, DELETE) en la tabla 'matches'
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "matches"
                }

                channel.subscribe()

                // Ante cualquier cambio en Supabase, re-consultamos los partidos activos
                changeFlow.collect {
                    loadMatches()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}