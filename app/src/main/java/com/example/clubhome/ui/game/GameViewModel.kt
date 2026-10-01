package com.example.clubhome.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    var currentMatchId by mutableStateOf<String?>(null)

    // Conservar los nombres de los equipos
    var team1Name by mutableStateOf("Equipo 1")
    var team2Name by mutableStateOf("Equipo 2")

    var selectedTeam by mutableStateOf(1)

    // Control del temporizador
    var isTimerRunning by mutableStateOf(false)
        private set
    var elapsedSeconds by mutableStateOf(0L)
        private set

    private var timerJob: Job? = null

    // Alineaciones
    val team1Lineup = mutableStateListOf<PlayerLineup>()
    val team2Lineup = mutableStateListOf<PlayerLineup>()

    // Estadísticas Equipo 1
    var team1Hits by mutableStateOf(0)
    var team1Errors by mutableStateOf(0)
    var team1Outs by mutableStateOf(0)
    var team1HomeRuns by mutableStateOf(0)

    // Estadísticas Equipo 2
    var team2Hits by mutableStateOf(0)
    var team2Errors by mutableStateOf(0)
    var team2Outs by mutableStateOf(0)
    var team2HomeRuns by mutableStateOf(0)

    // --- MÉTODOS DEL CRONÓMETRO ---

    fun startTimer() {
        if (timerJob?.isActive == true) return
        isTimerRunning = true
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    fun pauseTimer() {
        isTimerRunning = false
        timerJob?.cancel()
        timerJob = null
    }

    fun toggleTimer() {
        if (isTimerRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun resetTimer() {
        pauseTimer()
        elapsedSeconds = 0L
    }

    override fun onCleared() {
        super.onCleared()
        pauseTimer()
    }

    // --- INICIALIZACIÓN ---

    fun initializeLineups() {
        if (team1Lineup.isEmpty()) {
            val initialTeam1 = List(10) { i ->
                PlayerLineup(
                    number = i + 1,
                    name = "Seleccionar",
                    position = BASEBALL_POSITIONS.getOrElse(i) { "${i + 1}" },
                    innings = List(10) { BaseState() }
                )
            }
            team1Lineup.addAll(initialTeam1)
        }

        if (team2Lineup.isEmpty()) {
            val initialTeam2 = List(10) { i ->
                PlayerLineup(
                    number = i + 1,
                    name = "Seleccionar",
                    position = BASEBALL_POSITIONS.getOrElse(i) { "${i + 1}" },
                    innings = List(10) { BaseState() }
                )
            }
            team2Lineup.addAll(initialTeam2)
        }
    }
}