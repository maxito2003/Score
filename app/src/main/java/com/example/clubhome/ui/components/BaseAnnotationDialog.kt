package com.example.clubhome.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.clubhome.ui.game.BaseState

@Composable
fun BaseAnnotationDialog(
    initialState: BaseState,
    onDismiss: () -> Unit,
    onSave: (BaseState) -> Unit
) {
    var state by remember { mutableStateOf(initialState) }
    var showDiamondHelpDialog by remember { mutableStateOf(false) }

    fun applyQuickPlay(play: String) {
        val isAlreadySelected = state.playType == play
        if (isAlreadySelected) {
            state = state.copy(playType = null, outNumber = 0)
        } else {
            state = when (play) {
                "H1", "BB" -> state.copy(playType = play, firstBase = true, secondBase = false, thirdBase = false, homeBase = false, outNumber = 0)
                "H2"       -> state.copy(playType = play, firstBase = true, secondBase = true, thirdBase = false, homeBase = false, outNumber = 0)
                "H3"       -> state.copy(playType = play, firstBase = true, secondBase = true, thirdBase = true, homeBase = false, outNumber = 0)
                "HR"       -> state.copy(playType = play, firstBase = true, secondBase = true, thirdBase = true, homeBase = true, outNumber = 0)
                "K"        -> state.copy(playType = play, outNumber = 1)
                "2P"       -> state.copy(playType = play, outNumber = 2)
                else       -> state.copy(playType = play)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF000E4A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Encabezado
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Anotación de Bases",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showDiamondHelpDialog = true },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Glosario de Siglas",
                            tint = Color(0xFF64B5F6)
                        )
                    }
                }

                // Rombo interactivo con posiciones de béisbol
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BaseballDiamond(
                        state = state,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(35.dp)
                    )

                    BaseButton(
                        text = "2ª B",
                        isActive = state.secondBase,
                        modifier = Modifier.align(Alignment.TopCenter),
                        onClick = {
                            val newSecond = !state.secondBase
                            state = state.copy(
                                secondBase = newSecond,
                                firstBase = if (newSecond) true else state.firstBase
                            )
                        }
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
                        onClick = {
                            val newThird = !state.thirdBase
                            state = state.copy(
                                thirdBase = newThird,
                                firstBase = if (newThird) true else state.firstBase,
                                secondBase = if (newThird) true else state.secondBase
                            )
                        }
                    )

                    BaseButton(
                        text = "Home",
                        isActive = state.homeBase,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onClick = {
                            val newHome = !state.homeBase
                            state = state.copy(
                                homeBase = newHome,
                                firstBase = if (newHome) true else state.firstBase,
                                secondBase = if (newHome) true else state.secondBase,
                                thirdBase = if (newHome) true else state.thirdBase
                            )
                        }
                    )
                }

                // Botones rápidos por sigla
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionButton("H1", state.playType == "H1", Modifier.weight(1f)) { applyQuickPlay("H1") }
                        QuickActionButton("H2", state.playType == "H2", Modifier.weight(1f)) { applyQuickPlay("H2") }
                        QuickActionButton("H3", state.playType == "H3", Modifier.weight(1f)) { applyQuickPlay("H3") }
                        QuickActionButton("HR", state.playType == "HR", Modifier.weight(1f)) { applyQuickPlay("HR") }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionButton("BB", state.playType == "BB", Modifier.weight(1f)) { applyQuickPlay("BB") }
                        QuickActionButton("K", state.playType == "K", Modifier.weight(1f)) { applyQuickPlay("K") }
                        QuickActionButton("2P", state.playType == "2P", Modifier.weight(1f)) { applyQuickPlay("2P") }
                    }
                }

                // Guardar de forma segura
                Button(
                    onClick = {
                        onSave(state)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Listo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDiamondHelpDialog) {
        AlertDialog(
            onDismissRequest = { showDiamondHelpDialog = false },
            confirmButton = {
                TextButton(onClick = { showDiamondHelpDialog = false }) {
                    Text("Entendido", color = Color(0xFF64B5F6))
                }
            },
            title = { Text("Glosario de Siglas", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PosDescription("H1", "Sencillo (Hit de 1 Base)")
                    PosDescription("H2", "Doble (Hit de 2 Bases)")
                    PosDescription("H3", "Triple (Hit de 3 Bases)")
                    PosDescription("HR", "Home Run (Jonrón / 4 Bases)")
                    PosDescription("BB", "Base por Bolas (Walk)")
                    PosDescription("K", "Ponche (Strikeout)")
                    PosDescription("2P", "Doble Matanza (Double Play)")
                }
            },
            containerColor = Color(0xFF000B3B)
        )
    }
}

@Composable
fun QuickActionButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF1976D2) else Color(0xFF001666)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF64B5F6)) else null,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.LightGray,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
        )
    }
}

@Composable
fun BaseButton(text: String, isActive: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) Color(0xFF4CAF50) else Color(0xFF001A7A),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) Color.White else Color.Gray)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PosDescription(sigla: String, nombre: String) {
    Row {
        Text("$sigla: ", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(nombre, color = Color.LightGray, fontSize = 13.sp)
    }
}