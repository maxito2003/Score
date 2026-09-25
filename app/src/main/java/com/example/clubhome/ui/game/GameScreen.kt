package com.example.clubhome.ui.game

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// Modelo de estado de celdas
data class BaseState(
    var firstBase: Boolean = false,
    var secondBase: Boolean = false,
    var thirdBase: Boolean = false,
    var homeBase: Boolean = false
) {
    val isRun: Boolean get() = firstBase && secondBase && thirdBase && homeBase
}

data class PlayerLineup(
    val number: Int,
    var name: String,
    val position: String,
    val innings: MutableList<BaseState>
)

// Posiciones fijas
val BASEBALL_POSITIONS = listOf("P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF", "DH")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    registeredPlayers: List<String> = listOf("pepito", "ddd"),
    onMenuClick: () -> Unit = {} // Usa la acción del menú de hamburguesa existente de la app
) {
    var hits by remember { mutableStateOf(0) }
    var errors by remember { mutableStateOf(0) }
    var outs by remember { mutableStateOf(0) }
    var homeRuns by remember { mutableStateOf(0) }

    var selectedTeam by remember { mutableStateOf(1) }
    var showPosHelpDialog by remember { mutableStateOf(false) }

    // Inicialización de 10 posiciones de lineup sin asignación automática de nombres
    val lineup = remember {
        mutableStateListOf<PlayerLineup>().apply {
            for (i in 1..10) {
                add(
                    PlayerLineup(
                        number = i,
                        name = "Seleccionar",
                        position = BASEBALL_POSITIONS[i - 1],
                        innings = MutableList(10) { BaseState() }
                    )
                )
            }
        }
    }

    // Cálculo dinámico e inmutable de carreras
    val calculatedRuns = lineup.sumOf { player ->
        player.innings.count { it.isRun }
    }

    var activeDialogCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partido", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
                    }
                },
                actions = {
                    Surface(
                        color = Color(0xFFD32F2F),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            "LIGA",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF181818))
            )
        },
        containerColor = Color(0xFF101010)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Equipo 1 vs Equipo 2", color = Color.Gray, fontSize = 14.sp)

            // Marcador Superior
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
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
                    ScoreCounter("Hits", hits, Color(0xFF2196F3)) { hits = (hits + it).coerceAtLeast(0) }
                    ScoreCounter("Errores", errors, Color(0xFFF44336)) { errors = (errors + it).coerceAtLeast(0) }
                    ScoreCounter("Out", outs, Color(0xFFFFEB3B)) { outs = (outs + it).coerceIn(0, 3) }
                    ScoreCounter("HR", homeRuns, Color(0xFFFF9800)) { homeRuns = (homeRuns + it).coerceAtLeast(0) }
                }
            }

            // Selector de Equipos
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { selectedTeam = 1 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTeam == 1) Color(0xFF1565C0) else Color(0xFF2C2C2C)
                    )
                ) {
                    Text("Equipo 1", color = Color.White)
                }
                Button(
                    onClick = { selectedTeam = 2 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTeam == 2) Color(0xFF1565C0) else Color(0xFF2C2C2C)
                    )
                ) {
                    Text("Equipo 2", color = Color.White)
                }
            }

            // Tabla de Anotación
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
            ) {
                val tableHorizontalScrollState = rememberScrollState()

                Column {
                    // Encabezado
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF252525))
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", color = Color.Gray, modifier = Modifier.width(28.dp), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("LINE UP", color = Color.Gray, modifier = Modifier.width(130.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.width(60.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("POS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Ayuda Posiciones",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { showPosHelpDialog = true }
                            )
                        }

                        // Entradas alineadas numéricamente con el ancho de cada celda (48.dp + 4.dp de márgenes)
                        Row(modifier = Modifier.horizontalScroll(tableHorizontalScrollState)) {
                            for (i in 1..10) {
                                Box(
                                    modifier = Modifier
                                        .width(52.dp)
                                        .padding(horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$i",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color(0xFF333333))

                    // Filas de los 10 Jugadores
                    LazyColumn {
                        itemsIndexed(lineup) { playerIdx, player ->
                            var expandedMenu by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${player.number}", color = Color.White, modifier = Modifier.width(28.dp), fontSize = 12.sp, textAlign = TextAlign.Center)

                                // Selector Dropdown del Nombre
                                Box(modifier = Modifier.width(130.dp)) {
                                    Text(
                                        text = player.name,
                                        color = if (player.name == "Seleccionar") Color.Gray else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedMenu = true }
                                    )

                                    DropdownMenu(
                                        expanded = expandedMenu,
                                        onDismissRequest = { expandedMenu = false },
                                        modifier = Modifier.background(Color(0xFF2A2A2A))
                                    ) {
                                        if (registeredPlayers.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Sin jugadores registrados", color = Color.Gray) },
                                                enabled = false,
                                                onClick = {}
                                            )
                                        } else {
                                            registeredPlayers.forEach { name ->
                                                val isAlreadySelected = lineup.any { it.name == name }
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            name,
                                                            color = if (isAlreadySelected) Color.Gray else Color.White
                                                        )
                                                    },
                                                    enabled = !isAlreadySelected,
                                                    onClick = {
                                                        lineup[playerIdx] = player.copy(name = name)
                                                        expandedMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = player.position,
                                    color = Color(0xFF2196F3),
                                    modifier = Modifier.width(60.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Celdas de Entradas Scrollables (1 a 10)
                                Row(modifier = Modifier.horizontalScroll(tableHorizontalScrollState)) {
                                    for (inningIdx in 0 until 10) {
                                        val baseState = player.innings[inningIdx]
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF121212))
                                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp))
                                                .clickable { activeDialogCell = Pair(playerIdx, inningIdx) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            MiniDiamondCanvas(baseState)
                                        }
                                    }
                                }
                            }
                            Divider(color = Color(0xFF2A2A2A))
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
                    Text("Entendido", color = Color(0xFF2196F3))
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
            containerColor = Color(0xFF212121)
        )
    }

    activeDialogCell?.let { (pIdx, iIdx) ->
        val currentState = lineup[pIdx].innings[iIdx]
        BaseAnnotationDialog(
            initialState = currentState,
            onDismiss = { activeDialogCell = null },
            onSave = { updatedState ->
                lineup[pIdx].innings[iIdx] = updatedState
                activeDialogCell = null
            }
        )
    }
}

@Composable
fun ReadOnlyCounter(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text("$value", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ScoreCounter(label: String, value: Int, color: Color, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text("$value", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Row {
            Text("-", color = Color.Gray, modifier = Modifier.clickable { onChange(-1) }.padding(horizontal = 6.dp), fontSize = 16.sp)
            Text("+", color = Color.White, modifier = Modifier.clickable { onChange(1) }.padding(horizontal = 6.dp), fontSize = 16.sp)
        }
    }
}

@Composable
fun PosDescription(sigla: String, nombre: String) {
    Row {
        Text("$sigla: ", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(nombre, color = Color.LightGray, fontSize = 13.sp)
    }
}

@Composable
fun MiniDiamondCanvas(state: BaseState) {
    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        val w = size.width
        val h = size.height

        val top = Offset(w / 2, 0f)
        val right = Offset(w, h / 2)
        val bottom = Offset(w / 2, h)
        val left = Offset(0f, h / 2)

        val stroke = Stroke(width = 2f)
        drawPath(
            Path().apply {
                moveTo(top.x, top.y)
                lineTo(right.x, right.y)
                lineTo(bottom.x, bottom.y)
                lineTo(left.x, left.y)
                close()
            }, Color.DarkGray, style = stroke
        )

        val activeColor = Color(0xFF4CAF50)
        val redColor = Color(0xFFE53935)
        val yellowColor = Color(0xFFFFEB3B)

        if (state.firstBase) drawPath(Path().apply { moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(bottom.x, bottom.y); close() }, yellowColor)
        if (state.secondBase) drawPath(Path().apply { moveTo(top.x, top.y); lineTo(left.x, left.y); lineTo(bottom.x, bottom.y); close() }, activeColor)
        if (state.thirdBase) drawPath(Path().apply { moveTo(left.x, left.y); lineTo(top.x, top.y); lineTo(right.x, right.y); close() }, redColor)
        if (state.homeBase) drawPath(Path().apply { moveTo(left.x, left.y); lineTo(bottom.x, bottom.y); lineTo(right.x, right.y); close() }, Color(0xFF2196F3))
    }
}

@Composable
fun BaseAnnotationDialog(
    initialState: BaseState,
    onDismiss: () -> Unit,
    onSave: (BaseState) -> Unit
) {
    var state by remember { mutableStateOf(initialState.copy()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF212121)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Anotación de Bases", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(30.dp)) {
                        val w = size.width
                        val h = size.height
                        val top = Offset(w / 2, 0f)
                        val right = Offset(w, h / 2)
                        val bottom = Offset(w / 2, h)
                        val left = Offset(0f, h / 2)

                        val p = Path().apply {
                            moveTo(top.x, top.y)
                            lineTo(right.x, right.y)
                            lineTo(bottom.x, bottom.y)
                            lineTo(left.x, left.y)
                            close()
                        }
                        drawPath(p, Color.Gray, style = Stroke(width = 3f))
                        drawLine(Color.Gray, top, bottom, strokeWidth = 2f)
                        drawLine(Color.Gray, left, right, strokeWidth = 2f)
                    }

                    BaseButton(
                        text = "2ª B",
                        isActive = state.secondBase,
                        modifier = Modifier.align(Alignment.TopCenter),
                        onClick = { state = state.copy(secondBase = !state.secondBase) }
                    )

                    BaseButton(
                        text = "1ª B",
                        isActive = state.firstBase,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onClick = { state = state.copy(firstBase = !state.firstBase) }
                    )

                    BaseButton(
                        text = "3ª B",
                        isActive = state.thirdBase,
                        modifier = Modifier.align(Alignment.CenterStart),
                        onClick = { state = state.copy(thirdBase = !state.thirdBase) }
                    )

                    BaseButton(
                        text = "Home",
                        isActive = state.homeBase,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onClick = { state = state.copy(homeBase = !state.homeBase) }
                    )
                }

                Button(
                    onClick = { onSave(state) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Listo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BaseButton(text: String, isActive: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) Color(0xFF4CAF50) else Color(0xFF333333),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) Color.White else Color.Gray)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}