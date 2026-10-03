package com.example.clubhome.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubhome.data.model.Match
import com.example.clubhome.data.remote.SupabaseClientManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel : ViewModel() {
    var currentMatchId by mutableStateOf<String?>(null)
    var matchCode by mutableStateOf<String?>(null)

    // Conservar los nombres de los equipos
    var team1Name by mutableStateOf("Equipo 1")
    var team2Name by mutableStateOf("Equipo 2")

    var selectedTeam by mutableStateOf(1)

    // Control dinámico de Entradas (Innings)
    var totalInnings by mutableStateOf(10)

    // Control del temporizador
    var isTimerRunning by mutableStateOf(false)
        private set
    var elapsedSeconds by mutableStateOf(0L)

    private var timerJob: Job? = null
    private var activeChannelCode: String? = null

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

    init {
        // Inicializa las alineaciones por defecto únicamente cuando el ViewModel se crea por primera vez
        initializeLineups(forceReset = false)
    }

    // --- MÉTODOS DEL CÓDIGO DE PARTIDO Y CREADOR ---

    fun generateMatchCode(): String {
        if (matchCode.isNullOrEmpty()) {
            val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            matchCode = (1..6)
                .map { allowedChars.random() }
                .joinToString("")
        }
        return matchCode!!
    }

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

    // --- INICIALIZACIÓN Y SINCRONIZACIÓN CON SUPABASE ---

    /**
     * Inicializa las alineaciones solo si están vacías, a menos que forceReset sea true.
     * Esto evita borrar los cambios al regresar de otra pantalla.
     */
    fun initializeLineups(forceReset: Boolean = false) {
        if (forceReset || team1Lineup.isEmpty()) {
            team1Lineup.clear()
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

        if (forceReset || team2Lineup.isEmpty()) {
            team2Lineup.clear()
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

    /**
     * Guarda o actualiza el estado del partido en vivo en Supabase.
     */
    fun saveOrUpdateMatchToSupabase(mode: String = "LIGA", onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = SupabaseClientManager.client.auth.currentUserOrNull()
                val codeToUse = generateMatchCode()

                val totalRuns = (team1HomeRuns * 1) + (team2HomeRuns * 1)

                val matchData = Match(
                    id = currentMatchId,
                    code = codeToUse,
                    userId = user?.id,
                    mode = mode,
                    homeTeam = team1Name,
                    awayTeam = team2Name,
                    status = "LIVE",
                    runs = totalRuns,
                    hits = team1Hits + team2Hits,
                    errors = team1Errors + team2Errors,
                    outs = team1Outs + team2Outs,
                    homeRuns = team1HomeRuns + team2HomeRuns,
                    durationSeconds = elapsedSeconds.toInt()
                )

                if (currentMatchId == null) {
                    val insertedMatch = SupabaseClientManager.client.postgrest["matches"]
                        .insert(matchData) {
                            select()
                        }
                        .decodeSingle<Match>()

                    currentMatchId = insertedMatch.id
                    matchCode = insertedMatch.code ?: codeToUse

                    listenToMatchRealtime(matchCode!!)
                } else {
                    SupabaseClientManager.client.postgrest["matches"]
                        .update(matchData) {
                            filter { eq("id", currentMatchId!!) }
                        }
                }

                withContext(Dispatchers.Main) {
                    currentMatchId?.let { onComplete?.invoke(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Suscripción Realtime para actualizar la vista cuando ocurran cambios en el partido.
     */
    fun listenToMatchRealtime(code: String) {
        if (activeChannelCode == code) return // Evita re-suscribirse si ya está escuchando
        activeChannelCode = code

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val channel = SupabaseClientManager.client.channel("game_channel_$code")

                val changeFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "matches"
                    filter = "code=eq.$code"
                }

                channel.subscribe()

                changeFlow.onEach { change ->
                    val updatedMatch = change.decodeRecord<Match>()
                    withContext(Dispatchers.Main) {
                        team1Name = updatedMatch.homeTeam ?: team1Name
                        team2Name = updatedMatch.awayTeam ?: team2Name
                        // No sobrescribir elapsedSeconds si el cronómetro local está corriendo
                        if (!isTimerRunning) {
                            elapsedSeconds = updatedMatch.durationSeconds.toLong()
                        }
                    }
                }.launchIn(viewModelScope)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Marca el partido como TERMINADO (FINISHED) y limpia los estados temporales.
     */
    fun finishMatch(onFinished: () -> Unit) {
        pauseTimer()
        val matchId = currentMatchId
        if (matchId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    SupabaseClientManager.client.postgrest["matches"]
                        .update(mapOf("status" to "FINISHED")) {
                            filter { eq("id", matchId) }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    withContext(Dispatchers.Main) {
                        resetGameSession()
                        onFinished()
                    }
                }
            }
        } else {
            resetGameSession()
            onFinished()
        }
    }

    /**
     * Resetea el código y las variables de sesión para permitir un nuevo partido.
     */
    fun resetGameSession() {
        currentMatchId = null
        matchCode = null
        activeChannelCode = null

        // Resetear nombres por defecto
        team1Name = "Equipo 1"
        team2Name = "Equipo 2"

        resetTimer()
        totalInnings = 10

        team1Hits = 0
        team1Errors = 0
        team1Outs = 0
        team1HomeRuns = 0

        team2Hits = 0
        team2Errors = 0
        team2Outs = 0
        team2HomeRuns = 0

        // Forzar reconstrucción explícita de las alineaciones desde cero
        initializeLineups(forceReset = true)
    }
}