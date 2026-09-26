package com.example.clubhome.ui.matches

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
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
import com.example.clubhome.data.model.Match
import com.example.clubhome.data.remote.SupabaseClientManager
import com.example.clubhome.ui.components.BaseballDiamond
import com.example.clubhome.ui.game.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchStatsScreen(
    matchId: String,
    onBackClick: () -> Unit
) {
    var matchInfo by remember { mutableStateOf<Match?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedTeam by remember { mutableStateOf(1) }
    var showPosHelpDialog by remember { mutableStateOf(false) }
    var activeDialogCell by remember { mutableStateOf<BaseState?>(null) }

    val lineup = remember { mutableStateListOf<PlayerLineup>() }

    // Sincronización continua con Supabase usando decodeSingleOrNull
    LaunchedEffect(matchId) {
        while (true) {
            try {
                val fetchedMatch = SupabaseClientManager.client.postgrest["matches"]
                    .select {
                        filter { eq("id", matchId) }
                    }
                    .decodeSingleOrNull<Match>()

                if (fetchedMatch != null) {
                    matchInfo = fetchedMatch
                } else {
                    Log.e("MatchStatsScreen", "No se encontró el partido con ID: $matchId")
                }
            } catch (e: Exception) {
                Log.e("MatchStatsScreen", "Error al consultar Supabase", e)
            } finally {
                isLoading = false
            }
            delay(3000)
        }
    }

    // Decodificar lineupData cuando se reciba actualización de Supabase
    LaunchedEffect(matchInfo) {
        val rawJson = matchInfo?.lineupData
        if (!rawJson.isNullOrBlank()) {
            try {
                val parsedLineup = Json.decodeFromString<List<PlayerLineup>>(rawJson)
                lineup.clear()
                lineup.addAll(parsedLineup)
            } catch (e: Exception) {
                Log.e("MatchStatsScreen", "Error al parsear el JSON de lineupData", e)
            }
        } else if (lineup.isEmpty()) {
            // Cargar estado inicial por defecto si la base de datos está vacía
            lineup.clear()
            for (i in 1..10) {
                lineup.add(
                    PlayerLineup(
                        number = i,
                        name = "Jugador $i",
                        position = BASEBALL_POSITIONS.getOrElse(i - 1) { "DH" },
                        innings = List(10) { BaseState() }
                    )
                )
            }
        }
    }

    // Cálculos dinámicos de las estadísticas usando las propiedades correctas de BaseState
    val calculatedRuns = lineup.sumOf { player -> player.innings.count { it.homeBase } }
    val calculatedHits = lineup.sumOf { player -> player.innings.count { it.firstBase || it.secondBase || it.thirdBase || it.homeBase } }
    val calculatedErrors = lineup.sumOf { player -> player.innings.count { it.outNumber == 0 && !it.firstBase && !it.secondBase && !it.thirdBase && !it.homeBase } }
    val calculatedOuts = lineup.sumOf { player -> player.innings.count { it.outNumber > 0 } }
    val calculatedHomeRuns = lineup.sumOf { player -> player.innings.count { it.firstBase && it.secondBase && it.thirdBase && it.homeBase } }

    val homeTeamName = matchInfo?.homeTeam?.takeIf { it.isNotBlank() } ?: "Equipo 1"
    val awayTeamName = matchInfo?.awayTeam?.takeIf { it.isNotBlank() } ?: "Equipo 2"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Estadísticas del Partido",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "ID: ${matchId.take(8)}...",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF000726))
            )
        },
        containerColor = AzulMarinoBeisbol
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && matchInfo == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cargando partido...", color = Color.White)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "$homeTeamName vs $awayTeamName",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )

                    // Marcador Superior Dinámico
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
                            ReadOnlyCounter("Carreras", calculatedRuns, Color(0xFF4CAF50))
                            ReadOnlyCounter("Hits", calculatedHits, Color(0xFF2196F3))
                            ReadOnlyCounter("Errores", calculatedErrors, Color(0xFFF44336))
                            ReadOnlyCounter("Out", calculatedOuts, Color(0xFFFFEB3B))
                            ReadOnlyCounter("HR", calculatedHomeRuns, Color(0xFFFF9800))
                        }
                    }

                    // Selector de Equipos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedTeam = 1 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTeam == 1) Color(0xFF1565C0) else Color(0xFF001666)
                            )
                        ) {
                            Text(homeTeamName, color = Color.White)
                        }
                        Button(
                            onClick = { selectedTeam = 2 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTeam == 2) Color(0xFF1565C0) else Color(0xFF001666)
                            )
                        ) {
                            Text(awayTeamName, color = Color.White)
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
                                    modifier = Modifier.width(60.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("POS", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = "Ayuda Posiciones",
                                        tint = Color(0xFF64B5F6),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { showPosHelpDialog = true }
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

                            LazyColumn {
                                itemsIndexed(lineup) { _, player ->
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
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
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
                                                val baseState = player.innings.getOrNull(inningIdx) ?: BaseState()
                                                Box(
                                                    modifier = Modifier
                                                        .size(CELL_SIZE)
                                                        .padding(2.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF00051C))
                                                        .border(1.dp, Color(0xFF1A2A70), RoundedCornerShape(4.dp))
                                                        .clickable { activeDialogCell = baseState },
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
                    }
                }
            }
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

    val cellState = activeDialogCell
    if (cellState != null) {
        ReadOnlyBaseDialog(
            state = cellState,
            onDismiss = { activeDialogCell = null }
        )
    }
}

// -----------------------------------------------------------------------------
// Componentes Auxiliares
// -----------------------------------------------------------------------------

@Composable
fun ReadOnlyCounter(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = Color.LightGray)
        Text(text = "$count", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun PosDescription(sigla: String, descripcion: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = sigla,
            color = Color(0xFF64B5F6),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.width(30.dp)
        )
        Text(
            text = descripcion,
            color = Color.White,
            fontSize = 13.sp
        )
    }
}

@Composable
fun ReadOnlyBaseDialog(
    state: BaseState,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF000E4A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Detalle del Inning",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color(0xFF00051C), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BaseballDiamond(state = state)
                }

                Text(
                    text = if (state.homeBase) "Carrera Anotada ⚾" else "En Juego / Sin Carrera",
                    color = if (state.homeBase) Color(0xFF4CAF50) else Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar", color = Color(0xFF64B5F6))
                }
            }
        }
    }
}