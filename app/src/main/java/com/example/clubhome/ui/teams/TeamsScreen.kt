package com.example.clubhome.ui.teams

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
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
import com.example.clubhome.data.model.Team
import com.example.clubhome.ui.auth.BaseballNavy
import com.example.clubhome.ui.auth.BaseballRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(
    modeTitle: String,
    onNavigateHome: () -> Unit,
    onNavigateMatch: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: TeamsViewModel = viewModel()
) {
    // Obtenemos los datos desde el ViewModel y Supabase
    val teamsList by viewModel.teams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var isAddingNewTeam by remember { mutableStateOf(false) }
    var teamToEdit by remember { mutableStateOf<Team?>(null) }
    var teamToDelete by remember { mutableStateOf<Team?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Cargar equipos automáticamente cuando la pantalla aparece
    LaunchedEffect(Unit) {
        viewModel.loadTeams()
    }

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
                        onNavigateMatch()
                    }
                ) {
                    Text("Partido", color = Color.White, fontSize = 20.sp)
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
                                text = when {
                                    isAddingNewTeam -> "Nuevo Equipo"
                                    teamToEdit != null -> "Editar Equipo"
                                    else -> "Equipos"
                                },
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
                )
            },
            floatingActionButton = {
                if (!isAddingNewTeam && teamToEdit == null) {
                    FloatingActionButton(
                        onClick = { isAddingNewTeam = true },
                        containerColor = BaseballNavy,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar Equipo")
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
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = BaseballNavy
                    )
                }

                when {
                    isAddingNewTeam -> {
                        TeamForm(
                            teamToEdit = null,
                            onSave = { name, coach, imageUri ->
                                if (name.isNotBlank()) {
                                    // Crear y enviar el objeto a Supabase a través del ViewModel
                                    val newTeam = Team(
                                        name = name,
                                        coach = coach,
                                        logoUrl = imageUri?.toString()
                                    )
                                    viewModel.addTeam(newTeam)
                                }
                                isAddingNewTeam = false
                            },
                            onCancel = { isAddingNewTeam = false }
                        )
                    }
                    teamToEdit != null -> {
                        TeamForm(
                            teamToEdit = teamToEdit,
                            onSave = { name, coach, imageUri ->
                                teamToEdit?.let { current ->
                                    val updatedTeam = current.copy(
                                        name = name,
                                        coach = coach,
                                        logoUrl = imageUri?.toString()
                                    )
                                    viewModel.addTeam(updatedTeam)
                                }
                                teamToEdit = null
                            },
                            onCancel = { teamToEdit = null }
                        )
                    }
                    else -> {
                        if (teamsList.isEmpty() && !isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay equipos registrados.\nToca el botón + para agregar uno.",
                                    color = Color.Gray,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(teamsList, key = { it.id ?: it.hashCode().toString() }) { team ->
                                    TeamCardItem(
                                        team = team,
                                        onEdit = { teamToEdit = team },
                                        onDelete = { teamToDelete = team }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar
    if (teamToDelete != null) {
        AlertDialog(
            onDismissRequest = { teamToDelete = null },
            title = { Text("Eliminar Equipo", color = Color.White) },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar el equipo \"${teamToDelete?.name}\"?",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Aquí puedes añadir la lógica de eliminación en el ViewModel cuando la implementes
                        teamToDelete = null
                    }
                ) {
                    Text("Eliminar", color = BaseballRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { teamToDelete = null }) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

@Composable
fun TeamCardItem(
    team: Team,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Imagen en formato Banner
            if (!team.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = team.logoUrl,
                    contentDescription = "Logo o banner del equipo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Datos del equipo y botones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Nombre: ${team.name ?: ""}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (!team.coach.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Coach: ${team.coach}",
                            color = Color.LightGray,
                            fontSize = 15.sp
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = BaseballRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeamForm(
    teamToEdit: Team?,
    onSave: (name: String, coach: String, imageUri: Uri?) -> Unit,
    onCancel: () -> Unit
) {
    var teamName by remember { mutableStateOf(teamToEdit?.name ?: "") }
    var coachName by remember { mutableStateOf(teamToEdit?.coach ?: "") }
    var selectedImageUri by remember {
        mutableStateOf<Uri?>(teamToEdit?.logoUrl?.let { Uri.parse(it) })
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A))
                .border(2.dp, BaseballNavy, CircleShape)
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Logo seleccionado",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Cargar foto",
                        tint = Color.Gray
                    )
                    Text("Foto", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        OutlinedTextField(
            value = teamName,
            onValueChange = { teamName = it },
            label = { Text("Nombre del Equipo", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BaseballNavy,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = coachName,
            onValueChange = { coachName = it },
            label = { Text("Coach", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BaseballNavy,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = { onSave(teamName, coachName, selectedImageUri) },
                colors = ButtonDefaults.buttonColors(containerColor = BaseballNavy),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (teamToEdit != null) "Guardar" else "Registrar", color = Color.White)
            }
        }
    }
}