package com.example.clubhome.ui.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.clubhome.ui.auth.BaseballNavy
import com.example.clubhome.ui.game.LineupDataPayload
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedGameDetailScreen(
    matchId: String,
    onBackClick: () -> Unit
) {
    var match by remember { mutableStateOf<Match?>(null) }
    var payload by remember { mutableStateOf<LineupDataPayload?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(matchId) {
        withContext(Dispatchers.IO) {
            try {
                val result = SupabaseClientManager.client.postgrest["matches"]
                    .select { filter { eq("id", matchId) } }
                    .decodeSingleOrNull<Match>()

                match = result
                result?.lineupData?.let { jsonStr ->
                    payload = Json.decodeFromString<LineupDataPayload>(jsonStr)
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
                title = { Text("Resumen del Partido", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BaseballNavy)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BaseballNavy)
            }
        } else if (match == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No se encontró la información del partido.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tarjeta de Marcador
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ESTADO: FINALIZADO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(match?.homeTeam ?: "Local", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("${match?.runs ?: 0}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Text("VS", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.Gray)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(match?.awayTeam ?: "Visitante", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("${match?.runs ?: 0}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }

                // Estadísticas Generales (Hits, Errores, Outs, Home Runs)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Hits: ${match?.hits ?: 0}")
                            Text("Errores: ${match?.errors ?: 0}")
                            Text("Outs: ${match?.outs ?: 0}")
                            Text("HR: ${match?.homeRuns ?: 0}")
                        }
                    }
                }
            }
        }
    }
}