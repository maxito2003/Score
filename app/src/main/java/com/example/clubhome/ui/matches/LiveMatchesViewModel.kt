package com.example.clubhome.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubhome.data.remote.SupabaseClientManager
import com.example.clubhome.data.model.Match
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
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

    private fun loadMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Usamos SupabaseClientManager.client
                val result = SupabaseClientManager.client.postgrest["matches"]
                    .select()
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
        viewModelScope.launch {
            try {
                val channel = SupabaseClientManager.client.channel("matches_channel")
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "matches"
                }

                channel.subscribe()

                changeFlow.collect {
                    loadMatches()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}