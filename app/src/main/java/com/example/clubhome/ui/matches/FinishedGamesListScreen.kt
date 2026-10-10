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
                title = { Text("Partidos Terminados", color = DarkNavy, fontWeight = FontWeight.Bold) },
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
                    text = "No tienes partidos terminados aún.",
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${match.homeTeam} vs ${match.awayTeam}",
                                    color = DarkNavy,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Modo: ${match.mode}${if (!match.code.isNullOrBlank()) " | Código: ${match.code}" else ""}",
                                    color = Color(0xFF334E68),
                                    fontSize = 13.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${match.runs} Carreras",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${match.hits} H / ${match.errors} E / ${match.outs} Out",
                                    color = Color(0xFF334E68),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}