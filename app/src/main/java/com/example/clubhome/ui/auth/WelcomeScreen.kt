package com.example.clubhome.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clubhome.R

val BaseballRed = Color(0xFFE50914)
val BaseballNavy = Color(0xFF002266)

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Imagen de fondo: fondopelota
        Image(
            painter = painterResource(id = R.drawable.fondopelota),
            contentDescription = "Fondo de pelota de béisbol",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contenedor Superior: Texto VIVE, JUEGA, BÉISBOL + Imagen SULTAN + Imagen BINARY centrada abajo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "VIVE,",
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-1).sp
                            )
                        )
                        Text(
                            text = "JUEGA",
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-1).sp
                            )
                        )
                        Text(
                            text = "BÉISBOL",
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 50.sp,
                                fontWeight = FontWeight.Black,
                                color = BaseballRed,
                                letterSpacing = 0.sp
                            )
                        )
                    }

                    // Imagen Sultan al lado de VIVE, JUEGA BÉISBOL
                    Image(
                        painter = painterResource(id = R.drawable.sultan),
                        contentDescription = "Imagen Sultan",
                        modifier = Modifier.size(130.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Imagen Binary abajo de VIVE, JUEGA BÉISBOL y Sultan, pero CENTRADO
                Image(
                    painter = painterResource(id = R.drawable.binary),
                    contentDescription = "Imagen Binary",
                    modifier = Modifier
                        .height(70.dp)
                        .fillMaxWidth(0.85f),
                    contentScale = ContentScale.Fit
                )
            }

            // Botones inferiores (Iniciar Sesión / Crear Cuenta)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BaseballRed,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onNavigateToRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BaseballNavy,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Crear Cuenta",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}