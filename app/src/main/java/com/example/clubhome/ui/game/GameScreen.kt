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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubhome.data.model.Team
import com.example.clubhome.data.remote.SupabaseClientManager
import com.example.clubhome.ui.auth.BaseballNavy
import com.example.clubhome.ui.auth.BaseballRed
import com.example.clubhome.ui.components.BaseballDiamond
import com.example.clubhome.ui.matches.PosDescription
import com.example.clubhome.ui.matches.ReadOnlyCounter
import com.example.clubhome.ui.players.PlayersViewModel
import com.example.clubhome.ui.teams.TeamsViewModel
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.material.icons.filled.Add
import java.util.UUID
import com.example.clubhome.data.model.Match
val AzulMarinoBeisbol = Color(0xFF000B3B)
val CELL_SIZE = 48.dp

@Serializable
data class BaseState(
    val firstBase: Boolean = false,
    val secondBase: Boolean = false,
    val thirdBase: Boolean = false,
    val homeBase: Boolean = false,
    val playType: String? = null, // <--- Este es el nuevo campo
    val outNumber: Int = 0
) {
    val isRun: Boolean get() = homeBase || (firstBase && secondBase && thirdBase && homeBase)
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
    var totalInnings by remember { mutableStateOf(10) }

    // Usamos Unit como clave para que SOLO se ejecute UNA VEZ cuando la pantalla se crea por primera vez.
    LaunchedEffect(Unit) {
        teamsViewModel.loadTeams(modeTitle)

        // Verificación estricta: Si ya existe un matchId en el ViewModel compartido, NO reiniciamos nada.
        if (gameViewModel.currentMatchId == null) {
            Log.d("GameScreen", "Iniciando NUEVO partido para $modeTitle")

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
            totalInnings = 10
            gameViewModel.initializeLineups(forceReset = true)

            try {
                val matchCode = gameViewModel.generateMatchCode()

                val initialPayload = LineupDataPayload(
                    team1Lineup = gameViewModel.team1Lineup.toList(),
                    team2Lineup = gameViewModel.team2Lineup.toList(),
                    durationSeconds = 0L
                )
                val initialJson = Json.encodeToString(initialPayload)
                val newMatch = Match(
                    code = matchCode,
                    mode = modeTitle,
                    homeTeam = gameViewModel.team1Name,
                    awayTeam = gameViewModel.team2Name,
                    status = "LIVE",
                    lineupData = initialJson,
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

                gameViewModel.currentMatchId = insertedMatch.id
                gameViewModel.matchCode = insertedMatch.code ?: matchCode
            } catch (e: Exception) {
                Log.e("GameScreen", "Error al crear el partido inicial para el modo $modeTitle", e)
            }
        } else {
            Log.d("GameScreen", "Conservando partido existente con ID: ${gameViewModel.currentMatchId}")
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

    val minutes = gameViewModel.elapsedSeconds / 60
    val seconds = gameViewModel.elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val activeLineup = if (gameViewModel.selectedTeam == 1) gameViewModel.team1Lineup else gameViewModel.team2Lineup

    val calculatedRunsTeam1 = gameViewModel.team1Lineup.sumOf { player -> player.innings.count { it.isRun } }
    val calculatedRunsTeam2 = gameViewModel.team2Lineup.sumOf { player -> player.innings.count { it.isRun } }

    val currentHits = if (gameViewModel.selectedTeam == 1) gameViewModel.team1Hits else gameViewModel.team2Hits
    val currentErrors = if (gameViewModel.selectedTeam == 1) gameViewModel.team1Errors else gameViewModel.team2Errors
    val currentOuts = if (gameViewModel.selectedTeam == 1) gameViewModel.team1Outs else gameViewModel.team2Outs
    val currentHomeRuns = if (gameViewModel.selectedTeam == 1) gameViewModel.team1HomeRuns else gameViewModel.team2HomeRuns
    val currentCalculatedRuns = if (gameViewModel.selectedTeam == 1) calculatedRunsTeam1 else calculatedRunsTeam2

    fun syncToSupabase(status: String = "LIVE") {
        val matchId = gameViewModel.currentMatchId ?: return
        val payload = LineupDataPayload(
            team1Lineup = gameViewModel.team1Lineup.toList(),
            team2Lineup = gameViewModel.team2Lineup.toList(),
            durationSeconds = gameViewModel.elapsedSeconds
        )
        val jsonLineup = Json.encodeToString(payload)
        scope.launch(Dispatchers.IO) {
            try {
                SupabaseClientManager.client.postgrest["matches"]
                    .update({
                        set("lineup_data", jsonLineup)
                        set("home_team", gameViewModel.team1Name)
                        set("away_team", gameViewModel.team2Name)
                        set("hits", currentHits)
                        set("errors", currentErrors)
                        set("outs", currentOuts)
                        set("home_runs", currentHomeRuns)
                        set("runs", currentCalculatedRuns)
                        set("status", status)
                        set("duration_seconds", gameViewModel.elapsedSeconds.toInt())
                    }) {
                        filter { eq("id", matchId) }
                    }
            } catch (e: Exception) {
                Log.e("GameScreen", "Error al sincronizar datos con Supabase", e)
            }
        }
    }

    fun finishMatch() {
        if (gameViewModel.isTimerRunning) {
            gameViewModel.toggleTimer()
        }
        syncToSupabase(status = "FINISHED")
        onNavigateFinishedGames()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = BaseballNavy) {
                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = { scope.launch { drawerState.close() }; onNavigateHome() }) {
                    Text("Home", color = Color.White, fontSize = 20.sp)
                }
                TextButton(onClick = { scope.launch { drawerState.close() }; onNavigateTeams() }) {
                    Text("Equipos", color = Color.White, fontSize = 20.sp)
                }
                TextButton(onClick = { scope.launch { drawerState.close() }; onNavigatePlayers() }) {
                    Text("Jugadores", color = Color.White, fontSize = 20.sp)
                }
                TextButton(onClick = { scope.launch { drawerState.close() }; onNavigateFinishedGames() }) {
                    Text("Partidos Terminados", color = Color.White, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = { scope.launch { drawerState.close() }; onSignOut() }) {
                    Text("Cerrar Sesión", color = Color.White, fontSize = 18.sp)
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
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Surface(
                                color = Color(0xFF001254),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF1A2A70)),
                                modifier = Modifier.clickable {
                                    val code = gameViewModel.matchCode ?: "------"
                                    Toast.makeText(context, "Código del partido: $code", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("CÓDIGO: ", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = gameViewModel.matchCode ?: "------",
                                        color = Color(0xFFFFEB3B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(BaseballRed, shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = modeTitle.uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF000726))
                )
            },
            containerColor = AzulMarinoBeisbol
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${gameViewModel.team1Name} vs ${gameViewModel.team2Name}", color = Color.LightGray, fontSize = 14.sp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showFinishMatchDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Terminar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            color = Color(0xFF001254),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF1A2A70))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                IconButton(
                                    onClick = { gameViewModel.toggleTimer() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (gameViewModel.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Cronómetro",
                                        tint = if (gameViewModel.isTimerRunning) Color(0xFFFF4081) else Color(0xFF4CAF50),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = timeFormatted,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF001254)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ReadOnlyCounter("Carreras", currentCalculatedRuns, Color(0xFF4CAF50))
                        ScoreCounter("Hits", currentHits, Color(0xFF2196F3)) { delta ->
                            if (gameViewModel.selectedTeam == 1) {
                                gameViewModel.team1Hits = (gameViewModel.team1Hits + delta).coerceAtLeast(0)
                            } else {
                                gameViewModel.team2Hits = (gameViewModel.team2Hits + delta).coerceAtLeast(0)
                            }
                            syncToSupabase()
                        }
                        ScoreCounter("Errores", currentErrors, Color(0xFFF44336)) { delta ->
                            if (gameViewModel.selectedTeam == 1) {
                                gameViewModel.team1Errors = (gameViewModel.team1Errors + delta).coerceAtLeast(0)
                            } else {
                                gameViewModel.team2Errors = (gameViewModel.team2Errors + delta).coerceAtLeast(0)
                            }
                            syncToSupabase()
                        }
                        ScoreCounter("Out", currentOuts, Color(0xFFFFEB3B)) { delta ->
                            if (gameViewModel.selectedTeam == 1) {
                                gameViewModel.team1Outs = (gameViewModel.team1Outs + delta).coerceIn(0, 3)
                            } else {
                                gameViewModel.team2Outs = (gameViewModel.team2Outs + delta).coerceIn(0, 3)
                            }
                            syncToSupabase()
                        }
                        ScoreCounter("HR", currentHomeRuns, Color(0xFFFF9800)) { delta ->
                            if (gameViewModel.selectedTeam == 1) {
                                gameViewModel.team1HomeRuns = (gameViewModel.team1HomeRuns + delta).coerceAtLeast(0)
                            } else {
                                gameViewModel.team2HomeRuns = (gameViewModel.team2HomeRuns + delta).coerceAtLeast(0)
                            }
                            syncToSupabase()
                        }
                    }
                }

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

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF000E4A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1A2A70))
                ) {
                    val tableHorizontalScrollState = rememberScrollState()

                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF000833))
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#", color = Color.LightGray, modifier = Modifier.width(28.dp), fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("LINE UP", color = Color.LightGray, modifier = Modifier.width(130.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            Row(
                                modifier = Modifier
                                    .width(60.dp)
                                    .clickable { showPosHelpDialog = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("POS", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Ayuda Posiciones",
                                    tint = Color(0xFF64B5F6),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(tableHorizontalScrollState),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 1..totalInnings) {
                                    Box(
                                        modifier = Modifier
                                            .width(CELL_SIZE)
                                            .height(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$i",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(CELL_SIZE)
                                        .clickable { totalInnings++ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Agregar Entrada",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFF1A2A70))
                        if (isLoadingPlayers && registeredPlayerNames.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                itemsIndexed(activeLineup) { playerIdx, player ->
                                    var expandedMenu by remember { mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${player.number}", color = Color.White, modifier = Modifier.width(28.dp), fontSize = 12.sp, textAlign = TextAlign.Center)

                                        Box(modifier = Modifier.width(130.dp)) {
                                            Text(
                                                text = player.name,
                                                color = if (player.name == "Seleccionar") Color.Gray else Color.White,
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
                                                    .background(Color(0xFF001254))
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
                                                                    color = if (isAlreadySelected) Color.Gray else Color.White
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

                                        Text(
                                            text = "${playerIdx + 1}",
                                            color = Color(0xFF64B5F6),
                                            modifier = Modifier.width(60.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )

                                        Row(modifier = Modifier.horizontalScroll(tableHorizontalScrollState)) {
                                            for (inningIdx in 0 until totalInnings) {
                                                val baseState = player.innings.getOrElse(inningIdx) { BaseState() }
                                                Box(
                                                    modifier = Modifier
                                                        .size(CELL_SIZE)
                                                        .padding(2.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF00051C))
                                                        .border(1.dp, Color(0xFF1A2A70), RoundedCornerShape(4.dp))
                                                        .clickable { activeDialogCell = Pair(playerIdx, inningIdx) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    BaseballDiamond(state = baseState)
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFF001254))
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1A2A70), thickness = 2.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF000833))
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTALES",
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.width(218.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Row(modifier = Modifier.horizontalScroll(tableHorizontalScrollState)) {
                                for (inningIdx in 0 until totalInnings) {
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
                                            color = if (totalRunsInInning > 0) Color(0xFF4CAF50) else Color.Gray,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
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
                    Text("Entendido", color = Color(0xFF64B5F6))
                }
            },
            title = { Text("Glosario de Posiciones (1-10)", color = Color.White, fontWeight = FontWeight.Bold) },
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
            containerColor = Color(0xFF000E4A)
        )
    }

    if (showFinishMatchDialog) {
        AlertDialog(
            onDismissRequest = { showFinishMatchDialog = false },
            containerColor = Color(0xFF000E4A),
            title = {
                Text("Terminar Partido", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas finalizar este partido? Se guardarán todas las estadísticas, carreras y alineaciones en el historial.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishMatchDialog = false
                        finishMatch()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Terminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishMatchDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            }
        )
    }
}
// Componente para los botones desplegables con la flechita
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
                containerColor = if (isSelected) Color(0xFF1565C0) else Color(0xFF001666)
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = teamName,
                    color = Color.White,
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
                        tint = Color.White
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .heightIn(max = 250.dp)
                .background(Color(0xFF001254))
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
                                color = Color.White,
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
        Text(label, color = Color.LightGray, fontSize = 11.sp)
        Text("$value", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ScoreCounter(label: String, value: Int, color: Color, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.LightGray, fontSize = 11.sp)
        Text("$value", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Row {
            Text("-", color = Color.LightGray, modifier = Modifier.clickable { onChange(-1) }.padding(horizontal = 6.dp), fontSize = 16.sp)
            Text("+", color = Color.White, modifier = Modifier.clickable { onChange(1) }.padding(horizontal = 6.dp), fontSize = 16.sp)
        }
    }
}

@Composable
fun PosDescription(sigla: String, nombre: String) {
    Row {
        Text("$sigla: ", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(nombre, color = Color.LightGray, fontSize = 13.sp)
    }
}

// Diálogo Emergente del Rombo con Botón (?) de Glosario de Siglas
@Composable
fun BaseAnnotationDialog(
    initialState: BaseState,
    onDismiss: () -> Unit,
    onSave: (BaseState) -> Unit
) {
    var state by remember { mutableStateOf(initialState) }
    var showDiamondHelpDialog by remember { mutableStateOf(false) }

    fun applyQuickPlay(play: String) {
        val isAlreadySelected = state.playType == play
        if (isAlreadySelected) {
            state = state.copy(playType = null, outNumber = 0)
        } else {
            state = when (play) {
                "H1", "BB" -> state.copy(playType = play, firstBase = true, secondBase = false, thirdBase = false, homeBase = false, outNumber = 0)
                "H2"       -> state.copy(playType = play, firstBase = true, secondBase = true, thirdBase = false, homeBase = false, outNumber = 0)
                "H3"       -> state.copy(playType = play, firstBase = true, secondBase = true, thirdBase = true, homeBase = false, outNumber = 0)
                "HR"       -> state.copy(playType = play, firstBase = true, secondBase = true, thirdBase = true, homeBase = true, outNumber = 0)
                "K"        -> state.copy(playType = play, outNumber = 1)
                "2P"       -> state.copy(playType = play, outNumber = 2)
                else       -> state.copy(playType = play)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF000E4A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Anotación de Bases",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showDiamondHelpDialog = true },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Glosario de Siglas",
                            tint = Color(0xFF64B5F6)
                        )
                    }
                }

                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BaseballDiamond(
                        state = state,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(35.dp)
                    )

                    BaseButton(
                        text = "2ª B",
                        isActive = state.secondBase,
                        modifier = Modifier.align(Alignment.TopCenter),
                        onClick = {
                            val newSecond = !state.secondBase
                            state = state.copy(
                                secondBase = newSecond,
                                firstBase = if (newSecond) true else state.firstBase
                            )
                        }
                    )

                    BaseButton(
                        text = "1ª B",
                        isActive = state.firstBase,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onClick = { state = state.copy(firstBase = !state.firstBase) }
                    )

                    BaseButton(
                        text = "3ª B",
                        isActive = state.thirdBase,
                        modifier = Modifier.align(Alignment.CenterStart),
                        onClick = {
                            val newThird = !state.thirdBase
                            state = state.copy(
                                thirdBase = newThird,
                                firstBase = if (newThird) true else state.firstBase,
                                secondBase = if (newThird) true else state.secondBase
                            )
                        }
                    )

                    BaseButton(
                        text = "Home",
                        isActive = state.homeBase,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onClick = {
                            val newHome = !state.homeBase
                            state = state.copy(
                                homeBase = newHome,
                                firstBase = if (newHome) true else state.firstBase,
                                secondBase = if (newHome) true else state.secondBase,
                                thirdBase = if (newHome) true else state.thirdBase
                            )
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionButton(
                            text = "H1",
                            isSelected = state.playType == "H1",
                            modifier = Modifier.weight(1f)
                        ) { applyQuickPlay("H1") }

                        QuickActionButton(
                            text = "H2",
                            isSelected = state.playType == "H2",
                            modifier = Modifier.weight(1f)
                        ) { applyQuickPlay("H2") }

                        QuickActionButton(
                            text = "H3",
                            isSelected = state.playType == "H3",
                            modifier = Modifier.weight(1f)
                        ) { applyQuickPlay("H3") }

                        QuickActionButton(
                            text = "HR",
                            isSelected = state.playType == "HR",
                            modifier = Modifier.weight(1f)
                        ) { applyQuickPlay("HR") }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionButton(
                            text = "BB",
                            isSelected = state.playType == "BB",
                            modifier = Modifier.weight(1f)
                        ) { applyQuickPlay("BB") }

                        QuickActionButton(
                            text = "K",
                            isSelected = state.playType == "K",
                            modifier = Modifier.weight(1f)
                        ) { applyQuickPlay("K") }

                        QuickActionButton(
                            text = "2P",
                            isSelected = state.playType == "2P",
                            modifier = Modifier.weight(1f)
                        ) { applyQuickPlay("2P") }
                    }
                }

                // FIX: Guardar estado y descartar el diálogo explícitamente
                Button(
                    onClick = {
                        onSave(state)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Listo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDiamondHelpDialog) {
        AlertDialog(
            onDismissRequest = { showDiamondHelpDialog = false },
            confirmButton = {
                TextButton(onClick = { showDiamondHelpDialog = false }) {
                    Text("Entendido", color = Color(0xFF64B5F6))
                }
            },
            title = { Text("Glosario de Siglas", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PosDescription("H1", "Sencillo (Hit de 1 Base)")
                    PosDescription("H2", "Doble (Hit de 2 Bases)")
                    PosDescription("H3", "Triple (Hit de 3 Bases)")
                    PosDescription("HR", "Home Run (Jonrón / 4 Bases)")
                    PosDescription("BB", "Base por Bolas (Walk)")
                    PosDescription("K", "Ponche (Strikeout)")
                    PosDescription("2P", "Doble Matanza (Double Play)")
                }
            },
            containerColor = Color(0xFF000B3B)
        )
    }
}

@Composable
fun QuickActionButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF1976D2) else Color(0xFF001666)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF64B5F6)) else null,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.LightGray,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
        )
    }
}

@Composable
fun BaseButton(text: String, isActive: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) Color(0xFF4CAF50) else Color(0xFF001A7A),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) Color.White else Color.Gray)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}