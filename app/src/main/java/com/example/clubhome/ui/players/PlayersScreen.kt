package com.example.clubhome.ui.players

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.clubhome.data.model.Player

// Color azul marino principal del tema
val AzulMarinoBeisbol = Color(0xFF000B3B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    teamId: String,
    teamName: String,
    onBack: () -> Unit,
    viewModel: PlayersViewModel = viewModel()
) {
    val context = LocalContext.current
    val playersList by viewModel.players.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var isAddingPlayer by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }

    LaunchedEffect(teamId) {
        viewModel.loadPlayersByTeam(teamId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jugadores: $teamName", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF000726))
            )
        },
        floatingActionButton = {
            if (!isAddingPlayer && editingPlayer == null) {
                FloatingActionButton(
                    onClick = { isAddingPlayer = true },
                    containerColor = Color(0xFF1565C0),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Jugador")
                }
            }
        },
        containerColor = AzulMarinoBeisbol
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF64B5F6))
            }

            if (isAddingPlayer) {
                PlayerForm(
                    onSave = { name, number, position, photoUri ->
                        viewModel.addPlayer(
                            context = context,
                            name = name,
                            number = number,
                            position = position,
                            photoUri = photoUri,
                            teamId = teamId
                        )
                        isAddingPlayer = false
                    },
                    onCancel = { isAddingPlayer = false }
                )
            } else {
                if (playersList.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay jugadores registrados.", color = Color.LightGray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(playersList, key = { it.id ?: it.hashCode().toString() }) { player ->
                            PlayerCardItem(
                                player = player,
                                onEdit = { editingPlayer = player },
                                onDelete = { playerToDelete = player }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Edición de Jugador
    editingPlayer?.let { player ->
        Dialog(onDismissRequest = { editingPlayer = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF000E4A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Editar Jugador", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    PlayerForm(
                        initialName = player.name,
                        initialNumber = player.number?.toString() ?: "",
                        initialPosition = player.position ?: "",
                        initialPhotoUrl = player.photoUrl,
                        onSave = { name, number, position, photoUri ->
                            val updatedPlayer = player.copy(
                                name = name,
                                number = number.toIntOrNull(),
                                position = position
                            )
                            viewModel.updatePlayer(
                                context = context,
                                player = updatedPlayer,
                                newPhotoUri = photoUri
                            )
                            editingPlayer = null
                        },
                        onCancel = { editingPlayer = null }
                    )
                }
            }
        }
    }

    // Diálogo de Confirmación para Eliminar
    playerToDelete?.let { player ->
        AlertDialog(
            onDismissRequest = { playerToDelete = null },
            title = { Text("Eliminar Jugador", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar a \"${player.name}\"?",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        player.id?.let { id -> viewModel.deletePlayer(id) }
                        playerToDelete = null
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToDelete = null }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF000E4A)
        )
    }
}

@Composable
fun PlayerCardItem(
    player: Player,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001254)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!player.photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(player.photoUrl)
                        .crossfade(true)
                        .build(),
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
                        .background(Color(0xFF000833)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
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
                    color = Color(0xFF64B5F6),
                    fontSize = 14.sp
                )
            }

            if (player.number != null) {
                Text(
                    text = "#${player.number}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar jugador",
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar jugador",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PlayerForm(
    initialName: String = "",
    initialNumber: String = "",
    initialPosition: String = "",
    initialPhotoUrl: String? = null,
    onSave: (name: String, number: String, position: String, photoUri: Uri?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var number by remember { mutableStateOf(initialNumber) }
    var position by remember { mutableStateOf(initialPosition) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                // Otorgar permiso persistente para leer la URI seleccionada de la galería
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            photoUri = uri
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFF64B5F6),
        unfocusedBorderColor = Color(0xFF1A2A70),
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.LightGray,
        focusedContainerColor = Color(0xFF000833),
        unfocusedContainerColor = Color(0xFF000833)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF000833))
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            when {
                photoUri != null -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                !initialPhotoUrl.isNullOrEmpty() -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(initialPhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Icon(Icons.Default.Add, contentDescription = "Subir foto", tint = Color.LightGray)
                }
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
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
            ) {
                Text("Cancelar", color = Color.White)
            }
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, number, position, photoUri) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}