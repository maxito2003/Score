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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
fun RegisterScreen(
    uiState: AuthUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

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

        // Capa de atenuación oscura sobre la imagen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

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
                        text = "CREAR CUENTA",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = BaseballNavy
                        )
                    )

                    Text(
                        text = "Regístrate para comenzar a usar Score",
                        fontSize = 15.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Campo Nombre
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = onNameChange,
                        label = { Text("Nombre Completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111111),
                            unfocusedTextColor = Color(0xFF111111),
                            focusedBorderColor = BaseballNavy,
                            unfocusedBorderColor = Color(0xFF888888),
                            focusedLabelColor = BaseballNavy,
                            unfocusedLabelColor = Color(0xFF555555),
                            focusedLeadingIconColor = BaseballNavy,
                            unfocusedLeadingIconColor = Color(0xFF555555),
                            focusedTrailingIconColor = BaseballNavy,
                            unfocusedTrailingIconColor = Color(0xFF555555)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Campo Correo Electrónico
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
                            focusedBorderColor = BaseballNavy,
                            unfocusedBorderColor = Color(0xFF888888),
                            focusedLabelColor = BaseballNavy,
                            unfocusedLabelColor = Color(0xFF555555),
                            focusedLeadingIconColor = BaseballNavy,
                            unfocusedLeadingIconColor = Color(0xFF555555),
                            focusedTrailingIconColor = BaseballNavy,
                            unfocusedTrailingIconColor = Color(0xFF555555)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Campo Contraseña
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
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111111),
                            unfocusedTextColor = Color(0xFF111111),
                            focusedBorderColor = BaseballNavy,
                            unfocusedBorderColor = Color(0xFF888888),
                            focusedLabelColor = BaseballNavy,
                            unfocusedLabelColor = Color(0xFF555555),
                            focusedLeadingIconColor = BaseballNavy,
                            unfocusedLeadingIconColor = Color(0xFF555555),
                            focusedTrailingIconColor = BaseballNavy,
                            unfocusedTrailingIconColor = Color(0xFF555555)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Campo Confirmar Contraseña
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = { Text("Confirmar Contraseña", fontSize = 13.sp, maxLines = 1) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Mostrar contraseña"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111111),
                            unfocusedTextColor = Color(0xFF111111),
                            focusedBorderColor = BaseballNavy,
                            unfocusedBorderColor = Color(0xFF888888),
                            focusedLabelColor = BaseballNavy,
                            unfocusedLabelColor = Color(0xFF555555),
                            focusedLeadingIconColor = BaseballNavy,
                            unfocusedLeadingIconColor = Color(0xFF555555),
                            focusedTrailingIconColor = BaseballNavy,
                            unfocusedTrailingIconColor = Color(0xFF555555)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Mensajes de Error o Éxito
                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }

                    if (uiState.successMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.successMessage,
                            color = Color(0xFF2E7D32),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón Registrarse (Azul Marino)
                    Button(
                        onClick = onRegisterClick,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BaseballNavy,
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
                                text = "Registrarse",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Enlace a Iniciar Sesión
                    TextButton(onClick = onNavigateToLogin) {
                        Text(
                            text = "¿Ya tienes una cuenta? Inicia sesión aquí",
                            color = BaseballRed,
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
