package com.example.clubhome.ui.players

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.clubhome.data.model.Player
import com.example.clubhome.ui.auth.BaseballNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    teamId: String,
    teamName: String,
    onBack: () -> Unit,
    viewModel: PlayersViewModel = viewModel()
) {
    val playersList by viewModel.players.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var isAddingPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(teamId) {
        viewModel.loadPlayersByTeam(teamId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jugadores: $teamName", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        floatingActionButton = {
            if (!isAddingPlayer) {
                FloatingActionButton(
                    onClick = { isAddingPlayer = true },
                    containerColor = BaseballNavy,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Jugador")
                }
            }
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = BaseballNavy)
            }

            if (isAddingPlayer) {
                PlayerForm(
                    onSave = { name, number, position, photoUri ->
                        val newPlayer = Player(
                            teamId = if (teamId == "general") null else teamId,
                            name = name,
                            number = number.toIntOrNull(),
                            position = position,
                            photoUrl = photoUri?.toString()
                        )
                        viewModel.addPlayer(newPlayer)
                        isAddingPlayer = false
                    },
                    onCancel = { isAddingPlayer = false }
                )
            } else {
                if (playersList.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay jugadores registrados.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(playersList, key = { it.id ?: it.hashCode().toString() }) { player ->
                            PlayerCardItem(player = player)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerCardItem(player: Player) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!player.photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = player.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Posición: ${player.position ?: "N/A"}",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }

            if (player.number != null) {
                Text(
                    text = "#${player.number}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun PlayerForm(
    onSave: (name: String, number: String, position: String, photoUri: Uri?) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> photoUri = uri }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = BaseballNavy,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.Gray
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A))
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre del Jugador") },
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            label = { Text("Número de Camiseta") },
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = position,
            onValueChange = { position = it },
            label = { Text("Posición (ej. Pitcher, Catcher)") },
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancelar", color = Color.White)
            }
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, number, position, photoUri) },
                colors = ButtonDefaults.buttonColors(containerColor = BaseballNavy),
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar", color = Color.White)
            }
        }
    }
}