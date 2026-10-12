package com.example.clubhome.ui.game

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubhome.data.model.Match
import com.example.clubhome.data.model.Team
import com.example.clubhome.data.remote.SupabaseClientManager
import io.github.jan.supabase.auth.auth
import com.example.clubhome.ui.auth.BaseballRed
import com.example.clubhome.ui.components.BaseAnnotationDialog
import com.example.clubhome.ui.components.BaseballDiamond
import com.example.clubhome.ui.players.PlayersViewModel
import com.example.clubhome.ui.teams.TeamsViewModel
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

val SkyBlueBackground = Color(0xFFCBE3F7)
val DarkNavy = Color(0xFF001254)
val LightCardBg = Color(0xFFE8F3FC)
val CardBorderColor = Color(0xFFBBE0FC)

val CELL_SIZE = 48.dp

@Serializable
data class BaseState(
    val firstBase: Boolean = false,
    val secondBase: Boolean = false,
    val thirdBase: Boolean = false,
    val homeBase: Boolean = false,
    val playType: String? = null,
    val outNumber: Int = 0
) {
    val isOpenRun: Boolean get() = (firstBase || secondBase || thirdBase) && !homeBase
    val isClosedRun: Boolean get() = homeBase
    val isRun: Boolean get() = homeBase
}

@Serializable
data class LineupDataPayload(
    val team1Lineup: List<PlayerLineup>,
    val team2Lineup: List<PlayerLineup>,
    val durationSeconds: Long
)

@Serializable
data class PlayerLineup(
    val number: Int,
    val name: String,
    val position: String,
    val innings: List<BaseState>
)

val BASEBALL_POSITIONS = listOf("P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF", "DH")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    modeTitle: String,
    onNavigateHome: () -> Unit,
    onNavigateTeams: () -> Unit,
    onNavigatePlayers: () -> Unit,
    onNavigateFinishedGames: () -> Unit,
    onSignOut: () -> Unit,
    playersViewModel: PlayersViewModel = viewModel(),
    teamsViewModel: TeamsViewModel = viewModel(),
    gameViewModel: GameViewModel = viewModel()
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val tableHorizontalScrollState = rememberScrollState()

    val isPersonalMode = modeTitle.equals("PERSONAL", ignoreCase = true)

    val playersList by playersViewModel.players.collectAsState()
    val isLoadingPlayers by playersViewModel.isLoading.collectAsState()
    val teamsList by teamsViewModel.teams.collectAsState()

    val activeTeamName = if (gameViewModel.selectedTeam == 1) gameViewModel.team1Name else gameViewModel.team2Name
    val activeTeam = remember(teamsList, activeTeamName) {
        teamsList.find { it.name.equals(activeTeamName, ignoreCase = true) }
    }
    val registeredPlayerNames by remember(playersList, activeTeam) {
        derivedStateOf {
            if (activeTeam != null) {
                playersList
                    .filter { it.teamId == activeTeam.id }
                    .map { it.name }
            } else {
                emptyList()
            }
        }
    }

    var showPosHelpDialog by remember { mutableStateOf(false) }
    var showFinishMatchDialog by remember { mutableStateOf(false) }
    var activeDialogCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Inicialización del Partido: Mantiene los datos al navegar entre pantallas mientras el partido siga activo
    LaunchedEffect(modeTitle) {
        teamsViewModel.loadTeams(modeTitle)

        if (gameViewModel.currentMatchId == null || gameViewModel.currentMode != modeTitle) {
            Log.d("GameScreen", "Iniciando NUEVO partido para $modeTitle")

            gameViewModel.currentMode = modeTitle
            gameViewModel.currentMatchId = UUID.randomUUID().toString()
            gameViewModel.team1Name = "Seleccionar"
            gameViewModel.team2Name = "Seleccionar"
            gameViewModel.team1Hits = 0
            gameViewModel.team2Hits = 0
            gameViewModel.team1Errors = 0
            gameViewModel.team2Errors = 0
            gameViewModel.team1Outs = 0
            gameViewModel.team2Outs = 0
            gameViewModel.team1HomeRuns = 0
            gameViewModel.team2HomeRuns = 0
            gameViewModel.elapsedSeconds = 0
            if (gameViewModel.isTimerRunning) {
                gameViewModel.toggleTimer()
            }
            gameViewModel.totalInnings = 10
            gameViewModel.initializeLineups(forceReset = true)

            val matchCode = gameViewModel.generateMatchCode()

            scope.launch(Dispatchers.IO) {
                try {
                    val currentUserId = SupabaseClientManager.client.auth.currentUserOrNull()?.id
                    val initialPayload = LineupDataPayload(
                        team1Lineup = gameViewModel.team1Lineup.toList(),
                        team2Lineup = gameViewModel.team2Lineup.toList(),
                        durationSeconds = 0L
                    )
                    val initialJsonElement = Json.parseToJsonElement(Json.encodeToString(initialPayload))
                    val newMatch = Match(
                        code = matchCode,
                        userId = currentUserId,
                        mode = modeTitle,
                        homeTeam = gameViewModel.team1Name,
                        awayTeam = gameViewModel.team2Name,
                        status = "LIVE",
                        lineupData = initialJsonElement,
                        runs = 0,
                        hits = 0,
                        errors = 0,
                        outs = 0,
                        homeRuns = 0,
                        durationSeconds = 0
                    )
                    val insertedMatch = SupabaseClientManager.client.postgrest["matches"]
                        .insert(newMatch) {
                            select()
                        }
                        .decodeSingle<Match>()

                    withContext(Dispatchers.Main) {
                        gameViewModel.currentMatchId = insertedMatch.id
                        gameViewModel.matchCode = insertedMatch.code ?: matchCode
                    }
                } catch (e: Exception) {
                    Log.e("GameScreen", "Error al crear el partido inicial para el modo $modeTitle", e)
                }
            }
        } else {
            Log.d("GameScreen", "Conservando partido existente con ID: ${gameViewModel.currentMatchId} en modo $modeTitle")
        }
    }

    LaunchedEffect(gameViewModel.matchCode) {
        gameViewModel.matchCode?.let { code ->
            gameViewModel.listenToMatchRealtime(code)
        }
    }

    LaunchedEffect(activeTeam?.id) {
        val teamIdToLoad = activeTeam?.id
        if (!teamIdToLoad.isNullOrEmpty()) {
            playersViewModel.loadPlayersByTeam(teamIdToLoad)
        } else {
            playersViewModel.loadPlayersByTeam("general")
        }
    }

    val hours = gameViewModel.elapsedSeconds / 3600
    val minutes = (gameViewModel.elapsedSeconds % 3600) / 60
    val seconds = gameViewModel.elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    val activeLineup = if (gameViewModel.selectedTeam == 1) gameViewModel.team1Lineup else gameViewModel.team2Lineup

    val openRunsTeam1 = gameViewModel.team1Lineup.sumOf { player -> player.innings.count { it.isOpenRun } }
    val openRunsTeam2 = gameViewModel.team2Lineup.sumOf { player -> player.innings.count { it.isOpenRun } }
    val closedRunsTeam1 = gameViewModel.team1Lineup.sumOf { player -> player.innings.count { it.isClosedRun } }
    val closedRunsTeam2 = gameViewModel.team2Lineup.sumOf { player -> player.innings.count { it.isClosedRun } }

    val currentHits = if (gameViewModel.selectedTeam == 1) gameViewModel.team1Hits else gameViewModel.team2Hits
    val currentErrors = if (gameViewModel.selectedTeam == 1) gameViewModel.team1Errors else gameViewModel.team2Errors
    val currentOuts = if (gameViewModel.selectedTeam == 1) gameViewModel.team1Outs else gameViewModel.team2Outs
    val currentHomeRuns = if (gameViewModel.selectedTeam == 1) gameViewModel.team1HomeRuns else gameViewModel.team2HomeRuns
    val currentOpenRuns = if (gameViewModel.selectedTeam == 1) openRunsTeam1 else openRunsTeam2
    val currentClosedRuns = if (gameViewModel.selectedTeam == 1) closedRunsTeam1 else closedRunsTeam2

    fun syncToSupabase(status: String = "LIVE") {
        val matchId = gameViewModel.currentMatchId ?: return
        val currentUserId = SupabaseClientManager.client.auth.currentUserOrNull()?.id
        val payload = LineupDataPayload(
            team1Lineup = gameViewModel.team1Lineup.toList(),
            team2Lineup = gameViewModel.team2Lineup.toList(),
            durationSeconds = gameViewModel.elapsedSeconds
        )
        val jsonElement = Json.parseToJsonElement(Json.encodeToString(payload))
        scope.launch(Dispatchers.IO) {
            try {
                SupabaseClientManager.client.postgrest["matches"]
                    .update({
                        set("lineup_data", jsonElement)
                        set("home_team", gameViewModel.team1Name)
                        set("away_team", gameViewModel.team2Name)
                        set("hits", currentHits)
                        set("errors", currentErrors)
                        set("outs", currentOuts)
                        set("home_runs", currentHomeRuns)
                        set("runs", currentClosedRuns)
                        set("status", status)
                        set("mode", modeTitle)
                        set("duration_seconds", gameViewModel.elapsedSeconds.toInt())
                        if (currentUserId != null) {
                            set("user_id", currentUserId)
                        }
                    }) {
                        filter { eq("id", matchId) }
                    }
            } catch (e: Exception) {
                Log.e("GameScreen", "Error al sincronizar datos con Supabase", e)
            }
        }
    }

    fun addNewInningColumn() {
        gameViewModel.totalInnings++
        scope.launch {
            delay(100)
            tableHorizontalScrollState.animateScrollTo(tableHorizontalScrollState.maxValue + 1000)
        }
        syncToSupabase()
    }

    fun finishMatch() {
        if (gameViewModel.isTimerRunning) {
            gameViewModel.toggleTimer()
        }
        val matchCodeToUse = gameViewModel.matchCode ?: gameViewModel.generateMatchCode()
        val currentUserId = SupabaseClientManager.client.auth.currentUserOrNull()?.id
        val payload = LineupDataPayload(
            team1Lineup = gameViewModel.team1Lineup.toList(),
            team2Lineup = gameViewModel.team2Lineup.toList(),
            durationSeconds = gameViewModel.elapsedSeconds
        )
        val jsonLineupElement = Json.parseToJsonElement(Json.encodeToString(payload))
        val totalClosedRuns = (gameViewModel.team1Lineup.sumOf { p -> p.innings.count { it.isClosedRun } }) +
                              (gameViewModel.team2Lineup.sumOf { p -> p.innings.count { it.isClosedRun } })

        scope.launch(Dispatchers.IO) {
            try {
                Log.d("GameScreen", "Finalizando partido en Supabase. Code: $matchCodeToUse, Mode: $modeTitle, userId: $currentUserId")

                SupabaseClientManager.client.postgrest["matches"]
                    .update({
                        set("lineup_data", jsonLineupElement)
                        set("home_team", gameViewModel.team1Name)
                        set("away_team", gameViewModel.team2Name)
                        set("hits", gameViewModel.team1Hits + gameViewModel.team2Hits)
                        set("errors", gameViewModel.team1Errors + gameViewModel.team2Errors)
                        set("outs", gameViewModel.team1Outs + gameViewModel.team2Outs)
                        set("home_runs", gameViewModel.team1HomeRuns + gameViewModel.team2HomeRuns)
                        set("runs", totalClosedRuns)
                        set("status", "FINISHED")
                        set("mode", modeTitle)
                        set("duration_seconds", gameViewModel.elapsedSeconds.toInt())
                        if (currentUserId != null) {
                            set("user_id", currentUserId)
                        }
                    }) {
                        filter {
                            eq("code", matchCodeToUse)
                        }
                    }

                Log.d("GameScreen", "Partido con código $matchCodeToUse actualizado exitosamente a FINISHED")
            } catch (e: Exception) {
                Log.e("GameScreen", "Error al actualizar por código en Supabase, intentando insert directo", e)
                try {
                    val fallbackMatch = Match(
                        code = matchCodeToUse,
                        userId = currentUserId,
                        mode = modeTitle,
                        homeTeam = gameViewModel.team1Name,
                        awayTeam = gameViewModel.team2Name,
                        status = "FINISHED",
                        lineupData = jsonLineupElement,
                        runs = totalClosedRuns,
                        hits = gameViewModel.team1Hits + gameViewModel.team2Hits,
                        errors = gameViewModel.team1Errors + gameViewModel.team2Errors,
                        outs = gameViewModel.team1Outs + gameViewModel.team2Outs,
                        homeRuns = gameViewModel.team1HomeRuns + gameViewModel.team2HomeRuns,
                        durationSeconds = gameViewModel.elapsedSeconds.toInt()
                    )

                    SupabaseClientManager.client.postgrest["matches"].insert(fallbackMatch)
                    Log.d("GameScreen", "Partido guardado exitosamente mediante insert directo")
                } catch (e2: Exception) {
                    Log.e("GameScreen", "Error crítico al guardar partido finalizado", e2)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    gameViewModel.resetGameSession()
                    onNavigateFinishedGames()
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = SkyBlueBackground) {
                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = { scope.launch { drawerState.close() }; onNavigateHome() }) {
                    Text("Home", color = DarkNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { scope.launch { drawerState.close() }; onNavigateTeams() }) {
                    Text("Equipos", color = DarkNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { scope.launch { drawerState.close() }; onNavigatePlayers() }) {
                    Text("Jugadores", color = DarkNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { scope.launch { drawerState.close() }; onNavigateFinishedGames() }) {
                    Text("Partidos", color = DarkNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = { scope.launch { drawerState.close() }; onSignOut() }) {
                    Text("Cerrar Sesión", color = DarkNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Partido",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = DarkNavy
                            )

                            Box(
                                modifier = Modifier
                                    .background(Color.White, shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = modeTitle.uppercase(),
                                    color = DarkNavy,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú", tint = DarkNavy)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SkyBlueBackground)
                )
            },
            containerColor = SkyBlueBackground
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fila con Código (solo Liga), Terminar (solo Liga) y Cronómetro Alargado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isPersonalMode) {
                        Surface(
                            color = LightCardBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier.clickable {
                                val code = gameViewModel.matchCode ?: "------"
                                Toast.makeText(context, "Código del partido: $code", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("CÓDIGO: ", color = DarkNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = gameViewModel.matchCode ?: "------",
                                    color = DarkNavy,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isPersonalMode) {
                            Button(
                                onClick = { showFinishMatchDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BaseballRed),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Terminar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Reloj / Cronómetro Alargado
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier.defaultMinSize(minWidth = 110.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                IconButton(
                                    onClick = { gameViewModel.toggleTimer() },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = if (gameViewModel.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Cronómetro",
                                        tint = if (gameViewModel.isTimerRunning) Color(0xFFFF3B30) else Color(0xFF34C759),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = timeFormatted,
                                    color = DarkNavy,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Tarjeta de Contadores
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightCardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ReadOnlyCounter("C. Abiertas", currentOpenRuns, DarkNavy)
                        ReadOnlyCounter("C. Cerradas", currentClosedRuns, DarkNavy)
                        ScoreCounter("Hits", currentHits, DarkNavy) { delta ->
                            if (gameViewModel.selectedTeam == 1) {
                                gameViewModel.team1Hits = (gameViewModel.team1Hits + delta).coerceAtLeast(0)
                            } else {
                                gameViewModel.team2Hits = (gameViewModel.team2Hits + delta).coerceAtLeast(0)
                            }
                            syncToSupabase()
                        }
                        ScoreCounter("Errores", currentErrors, DarkNavy) { delta ->
                            if (gameViewModel.selectedTeam == 1) {
                                gameViewModel.team1Errors = (gameViewModel.team1Errors + delta).coerceAtLeast(0)
                            } else {
                                gameViewModel.team2Errors = (gameViewModel.team2Errors + delta).coerceAtLeast(0)
                            }
                            syncToSupabase()
                        }
                        ScoreCounter("Out", currentOuts, DarkNavy) { delta ->
                            if (gameViewModel.selectedTeam == 1) {
                                gameViewModel.team1Outs = (gameViewModel.team1Outs + delta).coerceIn(0, 3)
                            } else {
                                gameViewModel.team2Outs = (gameViewModel.team2Outs + delta).coerceIn(0, 3)
                            }
                            syncToSupabase()
                        }
                        ScoreCounter("HR", currentHomeRuns, DarkNavy) { delta ->
                            if (gameViewModel.selectedTeam == 1) {
                                gameViewModel.team1HomeRuns = (gameViewModel.team1HomeRuns + delta).coerceAtLeast(0)
                            } else {
                                gameViewModel.team2HomeRuns = (gameViewModel.team2HomeRuns + delta).coerceAtLeast(0)
                            }
                            syncToSupabase()
                        }
                    }
                }

                // Desplegables de Equipos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TeamDropdownButton(
                        teamName = gameViewModel.team1Name,
                        isSelected = gameViewModel.selectedTeam == 1,
                        teamsList = teamsList,
                        modifier = Modifier.weight(1f),
                        onButtonClick = {
                            gameViewModel.selectedTeam = 1
                            syncToSupabase()
                        },
                        onTeamSelected = { selected ->
                            gameViewModel.team1Name = selected.name
                            gameViewModel.selectedTeam = 1
                            syncToSupabase()
                        }
                    )

                    TeamDropdownButton(
                        teamName = gameViewModel.team2Name,
                        isSelected = gameViewModel.selectedTeam == 2,
                        teamsList = teamsList,
                        modifier = Modifier.weight(1f),
                        onButtonClick = {
                            gameViewModel.selectedTeam = 2
                            syncToSupabase()
                        },
                        onTeamSelected = { selected ->
                            gameViewModel.team2Name = selected.name
                            gameViewModel.selectedTeam = 2
                            syncToSupabase()
                        }
                    )
                }

                // Tabla de Alineación y Entradas
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFDCEEFB))
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#", color = DarkNavy, modifier = Modifier.width(28.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("LINE UP", color = DarkNavy, modifier = Modifier.width(130.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            Row(
                                modifier = Modifier
                                    .width(60.dp)
                                    .clickable { showPosHelpDialog = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("POS", color = DarkNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Ayuda Posiciones",
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.horizontalScroll(tableHorizontalScrollState),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 1..gameViewModel.totalInnings) {
                                    Box(
                                        modifier = Modifier
                                            .width(CELL_SIZE)
                                            .height(28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$i",
                                            color = DarkNavy,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // Botón "+" como última columna de la cabecera
                                Box(
                                    modifier = Modifier
                                        .width(CELL_SIZE)
                                        .height(28.dp)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFBBE2FA))
                                        .clickable { addNewInningColumn() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Agregar Entrada hacia la derecha",
                                        tint = DarkNavy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = CardBorderColor)
                        if (isLoadingPlayers && registeredPlayerNames.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = DarkNavy)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                itemsIndexed(activeLineup) { playerIdx, player ->
                                    var expandedMenu by remember { mutableStateOf(false) }
                                    var expandedPosMenu by remember { mutableStateOf(false) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${player.number}", color = DarkNavy, modifier = Modifier.width(28.dp), fontSize = 12.sp, textAlign = TextAlign.Center)

                                        Box(modifier = Modifier.width(130.dp)) {
                                            Text(
                                                text = player.name,
                                                color = if (player.name == "Seleccionar") Color.Gray else DarkNavy,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { expandedMenu = true }
                                            )

                                            DropdownMenu(
                                                expanded = expandedMenu,
                                                onDismissRequest = { expandedMenu = false },
                                                modifier = Modifier
                                                    .heightIn(max = 250.dp)
                                                    .background(LightCardBg)
                                            ) {
                                                if (registeredPlayerNames.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = { Text("Sin jugadores registrados", color = Color.Gray) },
                                                        enabled = false,
                                                        onClick = {}
                                                    )
                                                } else {
                                                    registeredPlayerNames.forEach { name ->
                                                        val isAlreadySelected = activeLineup.any { it.name == name }
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    name,
                                                                    color = if (isAlreadySelected) Color.Gray else DarkNavy
                                                                )
                                                            },
                                                            enabled = !isAlreadySelected,
                                                            onClick = {
                                                                activeLineup[playerIdx] = player.copy(name = name)
                                                                expandedMenu = false
                                                                syncToSupabase()
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Columna POS (Posición editable dinámicamente)
                                        Box(
                                            modifier = Modifier.width(60.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = player.position.ifBlank { "${playerIdx + 1}" },
                                                color = Color(0xFF1565C0),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { expandedPosMenu = true }
                                            )

                                            DropdownMenu(
                                                expanded = expandedPosMenu,
                                                onDismissRequest = { expandedPosMenu = false },
                                                modifier = Modifier
                                                    .heightIn(max = 250.dp)
                                                    .background(LightCardBg)
                                            ) {
                                                val posOptions = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
                                                posOptions.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                option,
                                                                color = if (player.position == option) Color(0xFF1565C0) else DarkNavy,
                                                                fontWeight = if (player.position == option) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        },
                                                        onClick = {
                                                            activeLineup[playerIdx] = player.copy(position = option)
                                                            expandedPosMenu = false
                                                            syncToSupabase()
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Row(modifier = Modifier.horizontalScroll(tableHorizontalScrollState)) {
                                            for (inningIdx in 0 until gameViewModel.totalInnings) {
                                                val baseState = player.innings.getOrElse(inningIdx) { BaseState() }
                                                Box(
                                                    modifier = Modifier
                                                        .size(CELL_SIZE)
                                                        .padding(2.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFF0F7FF))
                                                        .border(1.dp, CardBorderColor, RoundedCornerShape(4.dp))
                                                        .clickable { activeDialogCell = Pair(playerIdx, inningIdx) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    BaseballDiamond(state = baseState)
                                                }
                                            }

                                            // Celda "+" al final de la fila del jugador
                                            Box(
                                                modifier = Modifier
                                                    .size(CELL_SIZE)
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFE8F3FC))
                                                    .border(1.dp, CardBorderColor, RoundedCornerShape(4.dp))
                                                    .clickable { addNewInningColumn() },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Agregar Entrada",
                                                    tint = Color(0xFF1565C0),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFFE8F3FC))
                                }

                                // Botón para agregar más casillas / bateadores a la alineación
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val nextNumber = activeLineup.size + 1
                                                val newPlayer = PlayerLineup(
                                                    number = nextNumber,
                                                    name = "Seleccionar",
                                                    position = "$nextNumber",
                                                    innings = List(gameViewModel.totalInnings) { BaseState() }
                                                )
                                                activeLineup.add(newPlayer)
                                                syncToSupabase()
                                            }
                                            .padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Agregar Jugador a la Alineación",
                                            tint = Color(0xFF1565C0),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "+ Agregar Bateador / Casilla (${activeLineup.size + 1})",
                                            color = Color(0xFF1565C0),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = CardBorderColor, thickness = 2.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFDCEEFB))
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTALES",
                                color = DarkNavy,
                                modifier = Modifier.width(218.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Row(modifier = Modifier.horizontalScroll(tableHorizontalScrollState)) {
                                for (inningIdx in 0 until gameViewModel.totalInnings) {
                                    val totalRunsInInning = activeLineup.sumOf { player ->
                                        val inningState = player.innings.getOrNull(inningIdx)
                                        if (inningState != null && inningState.isRun) 1 else 0
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(CELL_SIZE)
                                            .height(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$totalRunsInInning",
                                            color = if (totalRunsInInning > 0) Color(0xFF2E7D32) else Color.Gray,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // Celda "+" al final de la fila de totales
                                Box(
                                    modifier = Modifier
                                        .width(CELL_SIZE)
                                        .height(24.dp)
                                        .clickable { addNewInningColumn() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Agregar Entrada",
                                        tint = Color(0xFF1565C0),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    activeDialogCell?.let { (playerIdx, inningIdx) ->
        if (playerIdx in activeLineup.indices) {
            val player = activeLineup[playerIdx]
            val currentInningState = player.innings.getOrElse(inningIdx) { BaseState() }

            BaseAnnotationDialog(
                initialState = currentInningState,
                onDismiss = { activeDialogCell = null },
                onSave = { updatedState ->
                    val updatedInnings = player.innings.toMutableList()

                    while (updatedInnings.size <= inningIdx) {
                        updatedInnings.add(BaseState())
                    }

                    updatedInnings[inningIdx] = updatedState
                    activeLineup[playerIdx] = player.copy(innings = updatedInnings)
                    syncToSupabase()
                    activeDialogCell = null
                }
            )
        }
    }

    if (showPosHelpDialog) {
        AlertDialog(
            onDismissRequest = { showPosHelpDialog = false },
            confirmButton = {
                TextButton(onClick = { showPosHelpDialog = false }) {
                    Text("Entendido", color = Color(0xFF1565C0))
                }
            },
            title = { Text("Glosario de Posiciones (1-10)", color = DarkNavy, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PosDescription("1", "Lanzador (Pitcher / P)")
                    PosDescription("2", "Receptor (Catcher / C)")
                    PosDescription("3", "Primera Base (1B)")
                    PosDescription("4", "Segunda Base (2B)")
                    PosDescription("5", "Tercera Base (3B)")
                    PosDescription("6", "Campocorto (Shortstop / SS)")
                    PosDescription("7", "Jardinero Izquierdo (Left Fielder / LF)")
                    PosDescription("8", "Jardinero Central (Center Fielder / CF)")
                    PosDescription("9", "Jardinero Derecho (Right Fielder / RF)")
                    PosDescription("10", "Bateador Designado / Bateador Extra (DH/EH)")
                }
            },
            containerColor = LightCardBg
        )
    }

    if (showFinishMatchDialog) {
        AlertDialog(
            onDismissRequest = { showFinishMatchDialog = false },
            containerColor = LightCardBg,
            title = {
                Text("Terminar Partido", color = DarkNavy, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas finalizar este partido? Se guardarán todas las estadísticas, carreras y alineaciones en el historial.",
                    color = Color(0xFF334E68)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishMatchDialog = false
                        finishMatch()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BaseballRed)
                ) {
                    Text("Terminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishMatchDialog = false }) {
                    Text("Cancelar", color = DarkNavy)
                }
            }
        )
    }
}

@Composable
fun TeamDropdownButton(
    teamName: String,
    isSelected: Boolean,
    teamsList: List<Team>,
    modifier: Modifier = Modifier,
    onButtonClick: () -> Unit,
    onTeamSelected: (Team) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(
            onClick = onButtonClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) Color(0xFFBBE2FA) else LightCardBg
            ),
            border = BorderStroke(1.dp, CardBorderColor),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = teamName,
                    color = DarkNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Desplegar equipos",
                        tint = DarkNavy
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .heightIn(max = 250.dp)
                .background(LightCardBg)
        ) {
            if (teamsList.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Sin equipos registrados", color = Color.Gray) },
                    enabled = false,
                    onClick = {}
                )
            } else {
                teamsList.forEach { team ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = team.name,
                                color = DarkNavy,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        onClick = {
                            onTeamSelected(team)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReadOnlyCounter(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF334E68), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text("$value", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ScoreCounter(label: String, value: Int, color: Color, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF334E68), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text("$value", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Row {
            Text("-", color = Color(0xFF334E68), modifier = Modifier.clickable { onChange(-1) }.padding(horizontal = 6.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("+", color = DarkNavy, modifier = Modifier.clickable { onChange(1) }.padding(horizontal = 6.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PosDescription(sigla: String, nombre: String) {
    Row {
        Text("$sigla: ", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(nombre, color = DarkNavy, fontSize = 13.sp)
    }
}