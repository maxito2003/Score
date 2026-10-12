package com.example.clubhome.ui.matches

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clubhome.data.model.Match
import com.example.clubhome.data.remote.SupabaseClientManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val SkyBlueBackground = Color(0xFFCBE3F7)
val DarkNavy = Color(0xFF001254)
val LightCardBg = Color(0xFFE8F3FC)
val CardBorderColor = Color(0xFFBBE0FC)

fun formatMatchDateTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Fecha no disponible"
    return try {
        val datePart = isoString.take(10)
        val timePart = if (isoString.length >= 16) isoString.substring(11, 16) else ""
        val parts = datePart.split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}${if (timePart.isNotBlank()) " - $timePart h" else ""}"
        } else {
            isoString
        }
    } catch (e: Exception) {
        isoString
    }
}

fun formatSecondsToHHMMSS(secondsTotal: Int): String {
    val h = secondsTotal / 3600
    val m = (secondsTotal % 3600) / 60
    val s = secondsTotal % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedGamesListScreen(
    onMatchClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var finishedMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = SupabaseClientManager.client.auth.currentUserOrNull()?.id
                Log.d("FinishedGamesListScreen", "Obteniendo partidos terminados. UserId: $currentUserId")

                val allFinished = SupabaseClientManager.client.postgrest["matches"]
                    .select {
                        filter {
                            eq("status", "FINISHED")
                        }
                    }
                    .decodeList<Match>()

                Log.d("FinishedGamesListScreen", "Partidos totales con status FINISHED: ${allFinished.size}")

                finishedMatches = if (currentUserId != null) {
                    val filtered = allFinished.filter { it.userId == currentUserId }
                    if (filtered.isNotEmpty()) filtered else allFinished
                } else {
                    allFinished
                }
            } catch (e: Exception) {
                Log.e("FinishedGamesListScreen", "Error al consultar partidos terminados", e)
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partidos", color = DarkNavy, fontWeight = FontWeight.Black, fontSize = 22.sp) },
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
        } else if (finishedMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tienes partidos guardados aún.",
                    color = Color(0xFF334E68),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(finishedMatches) { match ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMatchClick(match.id.orEmpty()) },
                        colors = CardDefaults.cardColors(containerColor = LightCardBg),
                        border = BorderStroke(1.dp, CardBorderColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${match.homeTeam} vs ${match.awayTeam}",
                                    color = DarkNavy,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "${match.runs} Carreras",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📅 ${formatMatchDateTime(match.createdAt)}",
                                    color = Color(0xFF334E68),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = "⏱️ ${formatSecondsToHHMMSS(match.durationSeconds)}",
                                    color = DarkNavy,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Modo: ${match.mode}${if (!match.code.isNullOrBlank()) " | Código: ${match.code}" else ""}",
                                    color = Color(0xFF555555),
                                    fontSize = 12.sp
                                )

                                Text(
                                    text = "Hits: ${match.hits} | Err: ${match.errors} | Outs: ${match.outs} | HR: ${match.homeRuns}",
                                    color = Color(0xFF334E68),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}