package com.example.clubhome.ui.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubhome.data.model.Team
import com.example.clubhome.data.repository.BaseballRepository
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

    // Carga la lista de equipos desde Supabase
    fun loadTeams(groupId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _teams.value = repository.getTeams(groupId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Registra un nuevo equipo y recarga la lista automáticamente
    fun addTeam(team: Team, groupId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.createTeam(team)
                loadTeams(groupId) // Recarga para actualizar la UI con los datos de Supabase
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}