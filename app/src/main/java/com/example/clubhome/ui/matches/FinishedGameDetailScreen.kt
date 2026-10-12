package com.example.clubhome.ui.matches

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clubhome.data.model.Match
import com.example.clubhome.data.remote.SupabaseClientManager
import com.example.clubhome.ui.components.BaseballDiamond
import com.example.clubhome.ui.game.BaseState
import com.example.clubhome.ui.game.CELL_SIZE
import com.example.clubhome.ui.game.LineupDataPayload
import com.example.clubhome.ui.game.ReadOnlyCounter
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedGameDetailScreen(
    matchId: String,
    onBackClick: () -> Unit
) {
    var match by remember { mutableStateOf<Match?>(null) }
    var payload by remember { mutableStateOf<LineupDataPayload?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedTeam by remember { mutableStateOf(1) } // 1 for Home/Team1, 2 for Away/Team2

    LaunchedEffect(matchId) {
        withContext(Dispatchers.IO) {
            try {
                val result = SupabaseClientManager.client.postgrest["matches"]
                    .select { filter { eq("id", matchId) } }
                    .decodeSingleOrNull<Match>()

                match = result
                result?.lineupData?.let { jsonElement ->
                    try {
                        payload = Json.decodeFromJsonElement<LineupDataPayload>(jsonElement)
                    } catch (e: Exception) {
                        payload = Json.decodeFromString<LineupDataPayload>(jsonElement.toString())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
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
                            text = "Estadísticas del Partido",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = DarkNavy
                        )

                        match?.mode?.let { mode ->
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DarkNavy)
            }
        } else if (match == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encontró la información del partido.",
                    color = Color(0xFF334E68),
                    fontSize = 16.sp
                )
            }
        } else {
            val currentMatch = match!!
            val team1Lineup = payload?.team1Lineup ?: emptyList()
            val team2Lineup = payload?.team2Lineup ?: emptyList()
            val activeLineup = if (selectedTeam == 1) team1Lineup else team2Lineup

            val totalInnings = remember(activeLineup) {
                activeLineup.maxOfOrNull { it.innings.size }?.coerceAtLeast(10) ?: 10
            }

            val durationSeconds = payload?.durationSeconds ?: currentMatch.durationSeconds.toLong()
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

            val openRunsTeam1 = team1Lineup.sumOf { p -> p.innings.count { it.isOpenRun } }
            val openRunsTeam2 = team2Lineup.sumOf { p -> p.innings.count { it.isOpenRun } }
            val closedRunsTeam1 = team1Lineup.sumOf { p -> p.innings.count { it.isClosedRun } }
            val closedRunsTeam2 = team2Lineup.sumOf { p -> p.innings.count { it.isClosedRun } }

            val currentOpenRuns = if (selectedTeam == 1) openRunsTeam1 else openRunsTeam2
            val currentClosedRuns = if (selectedTeam == 1) closedRunsTeam1 else closedRunsTeam2

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fila con Código, Fecha y Tiempo Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${currentMatch.homeTeam} vs ${currentMatch.awayTeam}",
                            color = DarkNavy,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "📅 ${formatMatchDateTime(currentMatch.createdAt)}",
                            color = Color(0xFF334E68),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CardBorderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("⏱️ ", fontSize = 12.sp)
                            Text(
                                text = timeFormatted,
                                color = DarkNavy,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Tarjeta de Contadores (C. Abiertas, C. Cerradas, Hits, Errores, Outs, HR)
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

                // Botones para alternar entre Equipos
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
                            text = currentMatch.homeTeam ?: "Equipo 1",
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
                            text = currentMatch.awayTeam ?: "Equipo 2",
                            color = DarkNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Tabla de Alineación y Entradas (Modo Lectura Completo)
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
                            Text("POS", color = DarkNavy, modifier = Modifier.width(60.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

                            Row(
                                modifier = Modifier.horizontalScroll(tableHorizontalScrollState),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                Text("No hay datos de alineación guardados para este equipo.", color = Color.Gray, fontSize = 14.sp)
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
                                                        .border(1.dp, CardBorderColor, RoundedCornerShape(4.dp)),
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
}