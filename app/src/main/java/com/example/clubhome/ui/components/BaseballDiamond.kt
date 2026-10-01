package com.example.clubhome.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.clubhome.ui.game.BaseState

@Composable
fun BaseballDiamond(
    state: BaseState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val padding = 8f

            val top = Offset(w / 2f, padding)             // 2ª Base
            val right = Offset(w - padding, h / 2f)       // 1ª Base (Derecha)
            val bottom = Offset(w / 2f, h - padding)      // Home Base
            val left = Offset(padding, h / 2f)            // 3ª Base (Izquierda)

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

            // 1ª Base -> Derecha
            drawCircle(
                color = if (state.firstBase) yellowActive else inactiveColor,
                radius = dotRadius,
                center = right
            )

            // 2ª Base -> Arriba
            drawCircle(
                color = if (state.secondBase) yellowActive else inactiveColor,
                radius = dotRadius,
                center = top
            )

            // 3ª Base -> Izquierda
            drawCircle(
                color = if (state.thirdBase) yellowActive else inactiveColor,
                radius = dotRadius,
                center = left
            )

            // Home Base -> Abajo
            drawCircle(
                color = if (state.homeBase) greenActive else inactiveColor,
                radius = dotRadius,
                center = bottom
            )

            // Dibujar el texto de la jugada (H1, H2, HR, K, etc.) en el centro del rombo
            state.playType?.let { text ->
                if (text.isNotEmpty()) {
                    val paint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = w * 0.35f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    val textY = (h / 2f) - ((paint.descent() + paint.ascent()) / 2f)
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        w / 2f,
                        textY,
                        paint
                    )
                }
            }
        }
    }
}