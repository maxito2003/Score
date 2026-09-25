package com.example.clubhome.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clubhome.R

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

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

        // Capa de atenuación oscura sobre la imagen para resaltar la interfaz
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tarjeta de formulario con fondo blanco semitransparente
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Título
                    Text(
                        text = "INICIAR SESIÓN",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = BaseballRed
                        )
                    )

                    Text(
                        text = "Ingresa a tu cuenta de Score",
                        fontSize = 15.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Campo de Correo Electrónico
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        label = { Text("Correo Electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111111),
                            unfocusedTextColor = Color(0xFF111111),
                            focusedBorderColor = BaseballRed,
                            unfocusedBorderColor = Color(0xFF888888),
                            focusedLabelColor = BaseballRed,
                            unfocusedLabelColor = Color(0xFF555555),
                            focusedLeadingIconColor = BaseballRed,
                            unfocusedLeadingIconColor = Color(0xFF555555),
                            focusedTrailingIconColor = BaseballRed,
                            unfocusedTrailingIconColor = Color(0xFF555555)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo de Contraseña
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Mostrar contraseña"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111111),
                            unfocusedTextColor = Color(0xFF111111),
                            focusedBorderColor = BaseballRed,
                            unfocusedBorderColor = Color(0xFF888888),
                            focusedLabelColor = BaseballRed,
                            unfocusedLabelColor = Color(0xFF555555),
                            focusedLeadingIconColor = BaseballRed,
                            unfocusedLeadingIconColor = Color(0xFF555555),
                            focusedTrailingIconColor = BaseballRed,
                            unfocusedTrailingIconColor = Color(0xFF555555)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Mensaje de Error
                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón de Iniciar Sesión
                    Button(
                        onClick = onLoginClick,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BaseballRed,
                            contentColor = Color.White
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.height(24.dp)
                            )
                        } else {
                            Text(
                                text = "Iniciar Sesión",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enlace a Registro
                    TextButton(onClick = onNavigateToRegister) {
                        Text(
                            text = "¿No tienes una cuenta? Regístrate aquí",
                            color = BaseballNavy,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(onClick = onNavigateBack) {
                        Text(
                            text = "Volver",
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
