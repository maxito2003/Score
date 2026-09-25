package com.example.clubhome.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

enum class QuadrantColor(val color: Color) {
    DEFAULT(Color.White),
    GREEN(Color.Green),
    ORANGE(Color(0xFFFF9800)),
    BLUE(Color.Blue),
    RED(Color.Red)
}

@Composable
fun BaseballDiamond(
    modifier: Modifier = Modifier,
    onSaveSelection: (quadrant: Int, selectedColor: QuadrantColor) -> Unit = { _, _ -> }
) {
    // Colores de los 4 sectores del rombo (0: Arriba-Izq, 1: Arriba-Der, 2: Abajo-Der, 3: Abajo-Izq)
    var quadColors by remember {
        mutableStateOf(
            listOf(
                QuadrantColor.DEFAULT,
                QuadrantColor.DEFAULT,
                QuadrantColor.DEFAULT,
                QuadrantColor.DEFAULT
            )
        )
    }

    var selectedQuadrant by remember { mutableStateOf<Int?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        // Determinación del cuadrante según la posición del toque
                        val isTop = offset.y < centerY
                        val isLeft = offset.x < centerX

                        selectedQuadrant = when {
                            isTop && isLeft -> 0
                            isTop && !isLeft -> 1
                            !isTop && !isLeft -> 2
                            else -> 3
                        }
                        showMenu = true
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            val top = Offset(w / 2f, 0f)
            val right = Offset(w, h / 2f)
            val bottom = Offset(w / 2f, h)
            val left = Offset(0f, h / 2f)

            // Dibujo de los 4 triángulos que conforman el rombo
            val p0 = Path().apply { moveTo(center.x, center.y); lineTo(top.x, top.y); lineTo(left.x, left.y); close() }
            val p1 = Path().apply { moveTo(center.x, center.y); lineTo(top.x, top.y); lineTo(right.x, right.y); close() }
            val p2 = Path().apply { moveTo(center.x, center.y); lineTo(bottom.x, bottom.y); lineTo(right.x, right.y); close() }
            val p3 = Path().apply { moveTo(center.x, center.y); lineTo(bottom.x, bottom.y); lineTo(left.x, left.y); close() }

            drawPath(p0, quadColors[0].color)
            drawPath(p1, quadColors[1].color)
            drawPath(p2, quadColors[2].color)
            drawPath(p3, quadColors[3].color)

            // Vértices/Bases (Puntos circulares en las 4 esquinas)
            val dotRadius = 10f
            val baseColor = Color.Yellow
            drawCircle(baseColor, dotRadius, top)
            drawCircle(baseColor, dotRadius, right)
            drawCircle(baseColor, dotRadius, left)
            drawCircle(Color.Green, dotRadius, bottom) // Home Base
        }

        // Diálogo con las 4 opciones al seleccionar una zona del rombo
        if (showMenu && selectedQuadrant != null) {
            AlertDialog(
                onDismissRequest = { showMenu = false },
                title = { Text("Seleccionar Opción") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuadrantColor.entries.filter { it != QuadrantColor.DEFAULT }.forEach { quadColor ->
                            Button(
                                onClick = {
                                    val q = selectedQuadrant!!
                                    val newColors = quadColors.toMutableList()
                                    newColors[q] = quadColor
                                    quadColors = newColors
                                    onSaveSelection(q, quadColor)
                                    showMenu = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = quadColor.color),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(quadColor.name, color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}