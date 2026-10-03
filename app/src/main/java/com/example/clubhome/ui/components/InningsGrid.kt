package com.example.clubhome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clubhome.data.model.Player
import com.example.clubhome.ui.game.BaseState

/**
 * Modelo de datos para representar el estado de un turno en una entrada específica.
 */
data class InningCellData(
    val inningNumber: Int,
    val baseState: BaseState = BaseState()
)

/**
 * Componente que muestra una tabla/grid desplazable horizontalmente con las entradas (Innings).
 * Permite agregar dinámicamente más entradas mediante el botón "+ Entrada".
 */
@Composable
fun InningsGrid(
    players: List<Player>,
    inningsCount: Int,
    cellsData: Map<Pair<String, Int>, BaseState>, // Clave: Pair(playerId, inningIndex)
    onAddInningClick: () -> Unit,
    onCellClick: (Player, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF000B3B))
            .padding(8.dp)
    ) {
        // Fila con el encabezado de la tabla y botón para añadir entradas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Entradas / Innings",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Button(
                onClick = onAddInningClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Entrada", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Grid desplazable horizontalmente
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            // Columna Fija: Nombres de los Jugadores
            Column(
                modifier = Modifier.width(130.dp)
            ) {
                // Encabezado "Jugador"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(Color(0xFF001666))
                        .border(0.5.dp, Color(0xFF1A2A70)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Jugador",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Lista de nombres
                players.forEach { player ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(Color(0xFF000E4A))
                            .border(0.5.dp, Color(0xFF1A2A70)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = player.name,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // Columnas Dinámicas: 1, 2, 3... N entradas
            for (inningIndex in 1..inningsCount) {
                Column(
                    modifier = Modifier.width(70.dp)
                ) {
                    // Encabezado del Inning (E1, E2, E3...)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(Color(0xFF001666))
                            .border(0.5.dp, Color(0xFF1A2A70)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "E$inningIndex",
                            color = Color(0xFF64B5F6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Celdas interactivas por jugador para la entrada actual
                    players.forEach { player ->
                        val playerId = player.id ?: ""
                        val state = cellsData[Pair(playerId, inningIndex)] ?: BaseState()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .background(Color(0xFF000726))
                                .border(0.5.dp, Color(0xFF1A2A70))
                                .clickable { onCellClick(player, inningIndex) },
                            contentAlignment = Alignment.Center
                        ) {
                            BaseballDiamond(
                                state = state,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}