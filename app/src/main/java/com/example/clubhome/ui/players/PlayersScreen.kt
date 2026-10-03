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
import com.example.clubhome.data.model.Team
import com.example.clubhome.ui.teams.TeamsViewModel

val AzulMarinoBeisbol = Color(0xFF000B3B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    teamId: String,
    teamName: String,
    mode: String = "LIGA",
    onBack: () -> Unit,
    viewModel: PlayersViewModel = viewModel(),
    teamsViewModel: TeamsViewModel = viewModel()
) {
    val context = LocalContext.current
    val playersList by viewModel.players.collectAsState()
    val teamsList by teamsViewModel.teams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTeam by remember { mutableStateOf<Team?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }

    var isAddingPlayer by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }
    LaunchedEffect(key1 = mode) {
        teamsViewModel.loadTeams(groupId = mode)
    }

    LaunchedEffect(teamsList) {
        if (selectedTeam == null && teamsList.isNotEmpty()) {
            val initial = teamsList.find { it.id == teamId } ?: teamsList.firstOrNull()
            selectedTeam = initial
            initial?.id?.let { viewModel.loadPlayersByTeam(it) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jugadores ($mode)", color = Color.White, fontWeight = FontWeight.Bold) },
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
                    onClick = { if (selectedTeam != null) isAddingPlayer = true },
                    containerColor = if (selectedTeam != null) Color(0xFF1565C0) else Color.Gray,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Jugador")
                }
            }
        },
        containerColor = AzulMarinoBeisbol
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Desplegable superior para seleccionar el equipo del modo actual
            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedTeam?.name ?: "Seleccione un equipo",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Equipo ($mode)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier.background(Color(0xFF001254))
                ) {
                    if (teamsList.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No hay equipos en $mode", color = Color.LightGray) },
                            onClick = { expandedDropdown = false }
                        )
                    } else {
                        teamsList.forEach { team ->
                            DropdownMenuItem(
                                text = { Text(team.name, color = Color.White) },
                                onClick = {
                                    selectedTeam = team
                                    expandedDropdown = false
                                    team.id?.let { viewModel.loadPlayersByTeam(it) }
                                }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF64B5F6))
            }

            if (selectedTeam == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Por favor, selecciona un equipo para ver y agregar jugadores.",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (isAddingPlayer) {
                PlayerForm(
                    onSave = { name, photoUri ->
                        selectedTeam?.id?.let { tId ->
                            viewModel.addPlayer(
                                context = context,
                                name = name,
                                photoUri = photoUri,
                                teamId = tId
                            )
                        }
                        isAddingPlayer = false
                    },
                    onCancel = { isAddingPlayer = false }
                )
            } else {
                if (playersList.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay jugadores registrados para este equipo.", color = Color.LightGray)
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
                        initialPhotoUrl = player.photoUrl,
                        onSave = { name, photoUri ->
                            val updatedPlayer = player.copy(
                                name = name,
                                teamId = selectedTeam?.id
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
                        player.id?.let { id ->
                            selectedTeam?.id?.let { tId -> viewModel.deletePlayer(id, tId) }
                        }
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

            Text(
                text = player.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

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
    initialPhotoUrl: String? = null,
    onSave: (name: String, photoUri: Uri?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
            ) {
                Text("Cancelar", color = Color.White)
            }
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, photoUri) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}