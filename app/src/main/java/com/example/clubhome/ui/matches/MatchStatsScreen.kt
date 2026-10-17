package com.example.clubhome.ui.matches

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.serialization.json.decodeFromJsonElement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchStatsScreen(
    matchId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var matchInfo by remember { mutableStateOf<Match?>(null) }
    var payload by remember { mutableStateOf<LineupDataPayload?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedTeam by remember { mutableStateOf(1) } // 1 for Home/Team1, 2 for Away/Team2
    var showPosHelpDialog by remember { mutableStateOf(false) }
    var activeDialogCell by remember { mutableStateOf<BaseState?>(null) }

    var liveSeconds by remember { mutableLongStateOf(0L) }

    // Consulta en vivo constante (Polling cada 1 segundo) para actualizaciones instantáneas en tiempo real
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
                    fetchedMatch.lineupData?.let { jsonElement ->
                        try {
                            payload = Json.decodeFromJsonElement<LineupDataPayload>(jsonElement)
                        } catch (e: Exception) {
                            payload = Json.decodeFromString<LineupDataPayload>(jsonElement.toString())
                        }
                    }
                } else {
                    Log.e("MatchStatsScreen", "No se encontró el partido en vivo con ID: $matchId")
                }
            } catch (e: Exception) {
                Log.e("MatchStatsScreen", "Error al consultar Supabase en vivo", e)
            } finally {
                isLoading = false
            }
            delay(1000L) // 1 segundo constante
        }
    }

    // Sincronizar el reloj en vivo con los segundos recibidos del servidor
    LaunchedEffect(payload?.durationSeconds, matchInfo?.durationSeconds) {
        val serverSeconds = payload?.durationSeconds ?: matchInfo?.durationSeconds?.toLong() ?: 0L
        if (serverSeconds > liveSeconds) {
            liveSeconds = serverSeconds
        }
    }

    // Cronómetro en vivo constante en pantalla de espectador (Avanza cada segundo en tiempo real)
    LaunchedEffect(matchInfo?.status) {
        if (matchInfo?.status != "FINISHED") {
            while (true) {
                delay(1000L)
                liveSeconds++
            }
        }
    }

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
                            text = "Partido en Vivo",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = DarkNavy
                        )

                        val isFinished = matchInfo?.status == "FINISHED"
                        val badgeBg = if (isFinished) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                        val badgeText = if (isFinished) "● TERMINADO" else "● EN VIVO"

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(badgeBg, shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            matchInfo?.mode?.let { mode ->
                                Box(
                                    modifier = Modifier
                                        .background(Color.White, shape = RoundedCornerShape(12.dp))
                                        .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = mode.uppercase(),
                                        color = DarkNavy,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = DarkNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SkyBlueBackground)
            )
        },
        containerColor = SkyBlueBackground
    ) { padding ->
        if (isLoading && matchInfo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DarkNavy)
            }
        } else if (matchInfo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No se encontró el partido en vivo.", color = Color(0xFF334E68), fontSize = 16.sp)
            }
        } else {
            val currentMatch = matchInfo!!
            val team1Lineup = payload?.team1Lineup ?: emptyList()
            val team2Lineup = payload?.team2Lineup ?: emptyList()
            val activeLineup = if (selectedTeam == 1) team1Lineup else team2Lineup

            val totalInnings = remember(activeLineup) {
                activeLineup.maxOfOrNull { it.innings.size }?.coerceAtLeast(10) ?: 10
            }

            val hours = liveSeconds / 3600
            val minutes = (liveSeconds % 3600) / 60
            val seconds = liveSeconds % 60
            val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

            val openRunsTeam1 = team1Lineup.sumOf { p -> p.innings.count { it.isOpenRun } }
            val openRunsTeam2 = team2Lineup.sumOf { p -> p.innings.count { it.isOpenRun } }
            val closedRunsTeam1 = team1Lineup.sumOf { p -> p.innings.count { it.isClosedRun } }
            val closedRunsTeam2 = team2Lineup.sumOf { p -> p.innings.count { it.isClosedRun } }

            val currentOpenRuns = if (selectedTeam == 1) openRunsTeam1 else openRunsTeam2
            val currentClosedRuns = if (selectedTeam == 1) closedRunsTeam1 else closedRunsTeam2

            val homeTeamName = currentMatch.homeTeam?.takeIf { it.isNotBlank() } ?: "Local"
            val awayTeamName = currentMatch.awayTeam?.takeIf { it.isNotBlank() } ?: "Visitante"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fila con Código (Copiar al portapapeles) y Tiempo Transcurrido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!currentMatch.code.isNullOrBlank()) {
                        Surface(
                            color = LightCardBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier.clickable {
                                val code = currentMatch.code ?: ""
                                if (code.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(code))
                                    Toast.makeText(context, "Código $code copiado al portapapeles 📋", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("CÓDIGO: ", color = DarkNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = currentMatch.code ?: "",
                                    color = DarkNavy,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar código",
                                    tint = DarkNavy,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CardBorderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("⏱️ ", fontSize = 12.sp)
                            Text(
                                text = timeFormatted,
                                color = DarkNavy,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Tarjeta de Contadores de Estadísticas
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
                        ReadOnlyCounter("Hits", currentMatch.hits, DarkNavy)
                        ReadOnlyCounter("Errores", currentMatch.errors, DarkNavy)
                        ReadOnlyCounter("Outs", currentMatch.outs, DarkNavy)
                        ReadOnlyCounter("HR", currentMatch.homeRuns, DarkNavy)
                    }
                }

                // Selector de Equipos (Local vs Visitante)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedTeam = 1 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeam == 1) Color(0xFFBBE2FA) else LightCardBg
                        ),
                        border = BorderStroke(1.dp, CardBorderColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = homeTeamName,
                            color = DarkNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { selectedTeam = 2 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTeam == 2) Color(0xFFBBE2FA) else LightCardBg
                        ),
                        border = BorderStroke(1.dp, CardBorderColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = awayTeamName,
                            color = DarkNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Tabla de Anotación e Entradas (Modo Lectura en Tiempo Real)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    val tableHorizontalScrollState = rememberScrollState()

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
                                modifier = Modifier.width(60.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("POS", color = DarkNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Ayuda Posiciones",
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { showPosHelpDialog = true }
                                )
                            }

                            Row(modifier = Modifier.horizontalScroll(tableHorizontalScrollState)) {
                                for (i in 1..totalInnings) {
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
                            }
                        }
                        HorizontalDivider(color = CardBorderColor)

                        if (activeLineup.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cargando datos de alineación...", color = Color.Gray, fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                itemsIndexed(activeLineup) { playerIdx, player ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${player.number}", color = DarkNavy, modifier = Modifier.width(28.dp), fontSize = 12.sp, textAlign = TextAlign.Center)

                                        Text(
                                            text = player.name,
                                            color = if (player.name == "Seleccionar") Color.Gray else DarkNavy,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.width(130.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = player.position.ifBlank { "${playerIdx + 1}" },
                                            color = Color(0xFF1565C0),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(60.dp),
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
                                                        .background(Color(0xFFF0F7FF))
                                                        .border(1.dp, CardBorderColor, RoundedCornerShape(4.dp))
                                                        .clickable { activeDialogCell = baseState },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    BaseballDiamond(state = baseState)
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFFE8F3FC))
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
                                            color = if (totalRunsInInning > 0) Color(0xFF2E7D32) else Color.Gray,
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

    if (showPosHelpDialog) {
        AlertDialog(
            onDismissRequest = { showPosHelpDialog = false },
            confirmButton = {
                TextButton(onClick = { showPosHelpDialog = false }) {
                    Text("Entendido", color = DarkNavy, fontWeight = FontWeight.Bold)
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
                    PosDescription("7", "Jardinero Izquierdo (LF)")
                    PosDescription("8", "Jardinero Central (CF)")
                    PosDescription("9", "Jardinero Derecho (RF)")
                    PosDescription("10", "Bateador Designado / Extra (DH)")
                }
            },
            containerColor = LightCardBg
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
fun PosDescription(sigla: String, descripcion: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = sigla,
            color = Color(0xFF1565C0),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.width(30.dp)
        )
        Text(
            text = descripcion,
            color = DarkNavy,
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
            colors = CardDefaults.cardColors(containerColor = LightCardBg),
            border = BorderStroke(1.dp, CardBorderColor),
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
                    color = DarkNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color(0xFFF0F7FF), RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BaseballDiamond(state = state)
                }

                Text(
                    text = if (state.homeBase) "Carrera Anotada ⚾" else "En Juego / Sin Carrera",
                    color = if (state.homeBase) Color(0xFF2E7D32) else Color(0xFF334E68),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar", color = DarkNavy, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}