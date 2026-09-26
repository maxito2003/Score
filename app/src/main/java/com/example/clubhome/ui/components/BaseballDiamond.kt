package com.example.clubhome.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.clubhome.ui.game.BaseState

@Composable
fun BaseballDiamond(
    state: BaseState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize().padding(6.dp)) {
        val w = size.width
        val h = size.height

        val padding = 8f

        val top = Offset(w / 2f, padding)             // 2ª Base
        val right = Offset(w - padding, h / 2f)       // Posición Derecha
        val bottom = Offset(w / 2f, h - padding)      // Home Base
        val left = Offset(padding, h / 2f)            // Posición Izquierda

        // Contorno gris del rombo
        val outlinePath = Path().apply {
            moveTo(top.x, top.y)
            lineTo(right.x, right.y)
            lineTo(bottom.x, bottom.y)
            lineTo(left.x, left.y)
            close()
        }

        drawPath(
            path = outlinePath,
            color = Color(0xFF757575),
            style = Stroke(width = 3f)
        )

        // Colores para encendido / apagado
        val inactiveColor = Color(0xFF424242)
        val yellowActive = Color(0xFFFFEB3B) // Amarillo brillante
        val greenActive = Color(0xFF00E676)  // Verde brillante

        val dotRadius = 9f

        // 1ª Base -> Izquierda (al presionar 1ª Base, enciende el punto izquierdo)
        drawCircle(
            color = if (state.firstBase) yellowActive else inactiveColor,
            radius = dotRadius,
            center = left
        )

        // 2ª Base -> Arriba
        drawCircle(
            color = if (state.secondBase) yellowActive else inactiveColor,
            radius = dotRadius,
            center = top
        )

        // 3ª Base -> Derecha (al presionar 3ª Base, enciende el punto derecho)
        drawCircle(
            color = if (state.thirdBase) yellowActive else inactiveColor,
            radius = dotRadius,
            center = right
        )

        // Home Base -> Abajo
        drawCircle(
            color = if (state.homeBase) greenActive else inactiveColor,
            radius = dotRadius,
            center = bottom
        )
    }
}