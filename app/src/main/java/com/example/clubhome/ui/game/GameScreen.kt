package com.example.clubhome.ui.game

import android.util.Log
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubhome.data.model.Match
import com.example.clubhome.data.remote.SupabaseClientManager
import com.example.clubhome.ui.auth.BaseballNavy
import com.example.clubhome.ui.auth.BaseballRed
import com.example.clubhome.ui.components.BaseballDiamond
import com.example.clubhome.ui.players.PlayersViewModel
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    onSignOut: () -> Unit,
    playersViewModel: PlayersViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val playersList by playersViewModel.players.collectAsState()
    val isLoading by playersViewModel.isLoading.collectAsState()

    var currentMatchId by remember { mutableStateOf<String?>(null) }
    var selectedTeam by remember { mutableStateOf(1) } // 1 para Equipo 1, 2 para Equipo 2

    // --- CRONÓMETRO / RELOJ ---
    var isTimerRunning by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    // --- ALINEACIONES SEPARADAS POR EQUIPO ---
    val team1Lineup = remember {
        mutableStateListOf<PlayerLineup>().apply {
            for (i in 1..10) {
                add(PlayerLineup(i, "Seleccionar", BASEBALL_POSITIONS[i - 1], List(10) { BaseState() }))
            }
        }
    }

    val team2Lineup = remember {
        mutableStateListOf<PlayerLineup>().apply {
            for (i in 1..10) {
                add(PlayerLineup(i, "Seleccionar", BASEBALL_POSITIONS[i - 1], List(10) { BaseState() }))
            }
        }
    }

    val activeLineup = if (selectedTeam == 1) team1Lineup else team2Lineup

    // --- MARCADORES SEPARADOS POR EQUIPO ---
    var team1Hits by remember { mutableStateOf(0) }
    var team1Errors by remember { mutableStateOf(0) }
    var team1Outs by remember { mutableStateOf(0) }
    var team1HomeRuns by remember { mutableStateOf(0) }

    var team2Hits by remember { mutableStateOf(0) }
    var team2Errors by remember { mutableStateOf(0) }
    var team2Outs by remember { mutableStateOf(0) }
    var team2HomeRuns by remember { mutableStateOf(0) }

    val calculatedRunsTeam1 = team1Lineup.sumOf { player -> player.innings.count { it.isRun } }
    val calculatedRunsTeam2 = team2Lineup.sumOf { player -> player.innings.count { it.isRun } }

    val currentHits = if (selectedTeam == 1) team1Hits else team2Hits
    val currentErrors = if (selectedTeam == 1) team1Errors else team2Errors
    val currentOuts = if (selectedTeam == 1) team1Outs else team2Outs
    val currentHomeRuns = if (selectedTeam == 1) team1HomeRuns else team2HomeRuns
    val currentCalculatedRuns = if (selectedTeam == 1) calculatedRunsTeam1 else calculatedRunsTeam2

    fun syncToSupabase() {
        val matchId = currentMatchId ?: return
        val jsonLineup = Json.encodeToString(activeLineup.toList())

        scope.launch(Dispatchers.IO) {
            try {
                SupabaseClientManager.client.postgrest["matches"]
                    .update({
                        set("lineup_data", jsonLineup)
                        set("hits", currentHits)
                        set("errors", currentErrors)
                        set("outs", currentOuts)
                        set("home_runs", currentHomeRuns)
                        set("runs", currentCalculatedRuns)
                    }) {
                        filter { eq("id", matchId) }
                    }
            } catch (e: Exception) {
                Log.e("GameScreen", "Error al sincronizar datos con Supabase", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        playersViewModel.loadPlayersByTeam("general")

        try {
            val initialJson = Json.encodeToString(team1Lineup.toList())
            val newMatch = Match(
                mode = modeTitle,
                homeTeam = "Equipo 1",
                awayTeam = "Equipo 2",
                status = "LIVE",
                lineupData = initialJson,
                runs = 0,
                hits = 0,
                errors = 0,
                outs = 0,
                homeRuns = 0
            )
            val insertedMatch = SupabaseClientManager.client.postgrest["matches"]
                .insert(newMatch) {
                    select()
                }
                .decodeSingle<Match>()

            currentMatchId = insertedMatch.id
        } catch (e: Exception) {
            Log.e("GameScreen", "Error al crear el partido inicial", e)
        }
    }

    DisposableEffect(currentMatchId) {
        onDispose {
            currentMatchId?.let { matchId ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        SupabaseClientManager.client.postgrest["matches"]
                            .update({
                                set("status", "FINISHED")
                            }) {
                                filter { eq("id", matchId) }
                            }
                    } catch (e: Exception) {
                        Log.e("GameScreen", "Error al finalizar el partido", e)
                    }
                }
            }
        }
    }

    val registeredPlayerNames = remember(playersList) {
        playersList.map { it.name }
    }

    var showPosHelpDialog by remember { mutableStateOf(false) }
    var activeDialogCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = BaseballNavy) {
                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateHome()
                    }
                ) {
                    Text("Home", color = Color.White, fontSize = 20.sp)
                }

                TextButton(
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateTeams()
                    }
                ) {
                    Text("Equipos", color = Color.White, fontSize = 20.sp)
                }

                TextButton(
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigatePlayers()
                    }
                ) {
                    Text("Jugadores", color = Color.White, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSignOut()
                    }
                ) {
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
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .background(BaseballRed, shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = modeTitle.uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menú",
                                tint = Color.White
                            )
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
                // Fila superior: Equipos vs Equipos + Cronómetro con Play/Pausa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Equipo 1 vs Equipo 2", color = Color.LightGray, fontSize = 14.sp)

                    Surface(
                        color = Color(0xFF001254),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A2A70))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = { isTimerRunning = !isTimerRunning },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Cronómetro",
                                    tint = if (isTimerRunning) Color(0xFFFF4081) else Color(0xFF4CAF50),
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

                // Marcador Superior
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
                            if (selectedTeam == 1) team1Hits = (team1Hits + delta).coerceAtLeast(0)
                            else team2Hits = (team2Hits + delta).coerceAtLeast(0)
                            syncToSupabase()
                        }
                        ScoreCounter("Errores", currentErrors, Color(0xFFF44336)) { delta ->
                            if (selectedTeam == 1) team1Errors = (team1Errors + delta).coerceAtLeast(0)
                            else team2Errors = (team2Errors + delta).coerceAtLeast(0)
                            syncToSupabase()
                        }
                        ScoreCounter("Out", currentOuts, Color(0xFFFFEB3B)) { delta ->
                            if (selectedTeam == 1) team1Outs = (team1Outs + delta).coerceIn(0, 3)
                            else team2Outs = (team2Outs + delta).coerceIn(0, 3)
                            syncToSupabase()
                        }
                        ScoreCounter("HR", currentHomeRuns, Color(0xFFFF9800)) { delta ->
                            if (selectedTeam == 1) team1HomeRuns = (team1HomeRuns + delta).coerceAtLeast(0)
                            else team2HomeRuns = (team2HomeRuns + delta).coerceAtLeast(0)
                            syncToSupabase()
                        }
                    }
                }

                // Selector de Equipos (Tab Dividida)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            selectedTeam = 1
                            syncToSupabase()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeam == 1) Color(0xFF1565C0) else Color(0xFF001666)
                        )
                    ) {
                        Text("Equipo 1", color = Color.White)
                    }
                    Button(
                        onClick = {
                            selectedTeam = 2
                            syncToSupabase()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeam == 2) Color(0xFF1565C0) else Color(0xFF001666)
                        )
                    ) {
                        Text("Equipo 2", color = Color.White)
                    }
                }

                // Tabla de Anotación
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF000E4A),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A2A70))
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

                            Row(modifier = Modifier.horizontalScroll(tableHorizontalScrollState)) {
                                for (i in 1..10) {
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
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1A2A70))

                        if (isLoading && registeredPlayerNames.isEmpty()) {
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
                                            text = player.position,
                                            color = Color(0xFF64B5F6),
                                            modifier = Modifier.width(60.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Row(modifier = Modifier.horizontalScroll(tableHorizontalScrollState)) {
                                            for (inningIdx in 0 until 10) {
                                                val baseState = player.innings[inningIdx]
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

                        // --- FILA INFERIOR DE TOTALES POR INNING (CARRERAS ACUMULADAS DE CADA ENTRADA) ---
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
                                for (inningIdx in 0 until 10) {
                                    val totalRunsInInning = activeLineup.sumOf { player ->
                                        if (player.innings[inningIdx].isRun) 1 else 0
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

    // Modal Glosario Original de Posiciones
    if (showPosHelpDialog) {
        AlertDialog(
            onDismissRequest = { showPosHelpDialog = false },
            confirmButton = {
                TextButton(onClick = { showPosHelpDialog = false }) {
                    Text("Entendido", color = Color(0xFF64B5F6))
                }
            },
            title = { Text("Glosario de Posiciones", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PosDescription("P", "Lanzador (Pitcher)")
                    PosDescription("C", "Receptor (Catcher)")
                    PosDescription("1B", "Primera Base")
                    PosDescription("2B", "Segunda Base")
                    PosDescription("3B", "Tercera Base")
                    PosDescription("SS", "Campocorto (Shortstop)")
                    PosDescription("LF", "Jardinero Izquierdo (Left Fielder)")
                    PosDescription("CF", "Jardinero Central (Center Fielder)")
                    PosDescription("RF", "Jardinero Derecho (Right Fielder)")
                    PosDescription("DH", "Bateador Designado / Extra")
                }
            },
            containerColor = Color(0xFF000E4A)
        )
    }

    activeDialogCell?.let { (pIdx, iIdx) ->
        val currentState = activeLineup[pIdx].innings[iIdx]
        BaseAnnotationDialog(
            initialState = currentState,
            onDismiss = { activeDialogCell = null },
            onSave = { updatedState ->
                val newInnings = activeLineup[pIdx].innings.toMutableList().apply {
                    this[iIdx] = updatedState
                }
                activeLineup[pIdx] = activeLineup[pIdx].copy(innings = newInnings)
                activeDialogCell = null
                syncToSupabase()
            }
        )
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

// Diálogo Emergente del Rombo con Botón (?) de Glosario de Siglas en la Esquina Superior Derecha
@Composable
fun BaseAnnotationDialog(
    initialState: BaseState,
    onDismiss: () -> Unit,
    onSave: (BaseState) -> Unit
) {
    var state by remember { mutableStateOf(initialState) }
    var showDiamondHelpDialog by remember { mutableStateOf(false) }

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
                // Encabezado con título e ícono (?)
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

                // Rombo interactivo con botones
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
                        onClick = { state = state.copy(secondBase = !state.secondBase) }
                    )

                    BaseButton(
                        text = "3ª B",
                        isActive = state.thirdBase,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onClick = { state = state.copy(thirdBase = !state.thirdBase) }
                    )

                    BaseButton(
                        text = "1ª B",
                        isActive = state.firstBase,
                        modifier = Modifier.align(Alignment.CenterStart),
                        onClick = { state = state.copy(firstBase = !state.firstBase) }
                    )

                    BaseButton(
                        text = "Home",
                        isActive = state.homeBase,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onClick = { state = state.copy(homeBase = !state.homeBase) }
                    )
                }

                // Botones directos por sigla (Solo guardan el tipo de jugada, NO alteran las bases)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionButton(
                            text = "H1",
                            isSelected = state.playType == "H1",
                            modifier = Modifier.weight(1f)
                        ) {
                            state = state.copy(playType = if (state.playType == "H1") null else "H1")
                        }
                        QuickActionButton(
                            text = "H2",
                            isSelected = state.playType == "H2",
                            modifier = Modifier.weight(1f)
                        ) {
                            state = state.copy(playType = if (state.playType == "H2") null else "H2")
                        }
                        QuickActionButton(
                            text = "H3",
                            isSelected = state.playType == "H3",
                            modifier = Modifier.weight(1f)
                        ) {
                            state = state.copy(playType = if (state.playType == "H3") null else "H3")
                        }
                        QuickActionButton(
                            text = "HR",
                            isSelected = state.playType == "HR",
                            modifier = Modifier.weight(1f)
                        ) {
                            state = state.copy(playType = if (state.playType == "HR") null else "HR")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionButton(
                            text = "BB",
                            isSelected = state.playType == "BB",
                            modifier = Modifier.weight(1f)
                        ) {
                            state = state.copy(playType = if (state.playType == "BB") null else "BB")
                        }
                        QuickActionButton(
                            text = "K",
                            isSelected = state.playType == "K",
                            modifier = Modifier.weight(1f)
                        ) {
                            state = state.copy(
                                playType = if (state.playType == "K") null else "K",
                                outNumber = if (state.playType != "K") 1 else 0
                            )
                        }
                        QuickActionButton(
                            text = "2P",
                            isSelected = state.playType == "2P",
                            modifier = Modifier.weight(1f)
                        ) {
                            state = state.copy(
                                playType = if (state.playType == "2P") null else "2P",
                                outNumber = if (state.playType != "2P") 2 else 0
                            )
                        }
                    }
                }

                Button(
                    onClick = { onSave(state) },
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