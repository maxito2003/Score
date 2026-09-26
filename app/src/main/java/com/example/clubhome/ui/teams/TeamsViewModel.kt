package com.example.clubhome.ui.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubhome.data.model.Team
import com.example.clubhome.data.remote.SupabaseClientManager
import com.example.clubhome.data.repository.BaseballRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TeamsViewModel(
    private val repository: BaseballRepository = BaseballRepository()
) : ViewModel() {

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun getCurrentUserId(): String? {
        return SupabaseClientManager.client.auth.currentUserOrNull()?.id
    }

    // Carga la lista de equipos correspondientes al usuario logueado
    fun loadTeams(groupId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val userId = getCurrentUserId()
                if (userId != null) {
                    _teams.value = repository.getTeams(groupId, userId)
                } else {
                    _teams.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _teams.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Registra un nuevo equipo asignándole el user_id del usuario con sesión activa
    fun addTeam(team: Team, groupId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val userId = getCurrentUserId() ?: return@launch
                val teamWithUser = team.copy(userId = userId)

                repository.createTeam(teamWithUser)
                loadTeams(groupId) // Recarga para actualizar la UI
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}