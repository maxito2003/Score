package com.example.clubhome.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clubhome.R
import com.example.clubhome.ui.auth.BaseballNavy
import com.example.clubhome.ui.auth.BaseballRed

@Composable
fun HomeScreen(
    userName: String?,
    onLeagueClick: () -> Unit,
    onPersonalClick: () -> Unit,
    onViewMatchesClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Imagen de fondo: fondosinpelota
        Image(
            painter = painterResource(id = R.drawable.fondosinpelota),
            contentDescription = "Fondo de campo de béisbol",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Botón de cerrar sesión en la parte superior derecha
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, end = 24.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onSignOutClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Insignia de BIENVENIDO (Caja Roja con texto inclinado en blanco)
            Box(
                modifier = Modifier
                    .background(BaseballRed, shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "BIENVENIDO",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtítulo
            Text(
                text = "A TU APP DE BÉISBOL",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )

            // Línea de acento azul inferior
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(160.dp)
                    .height(5.dp)
                    .background(BaseballNavy, shape = RoundedCornerShape(3.dp))
            )

            if (!userName.isNullOrEmpty()) {
                Text(
                    text = "¡Hola, $userName!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Instrucción "Elige el tipo de partido:"
            Text(
                text = "Elige el tipo de partido:",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Botón Liga (Azul Marino)
            Button(
                onClick = onLeagueClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(55.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BaseballNavy,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Liga",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Personal (Rojo)
            Button(
                onClick = onPersonalClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(55.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BaseballRed,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Personal",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pregunta "¿Quieres ver un partido?"
            Text(
                text = "¿Quieres ver un partido?",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Botón Ver partidos (Azul Marino)
            Button(
                onClick = onViewMatchesClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(55.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BaseballNavy,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Ver partidos",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}