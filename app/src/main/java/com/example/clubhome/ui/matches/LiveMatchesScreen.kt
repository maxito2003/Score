package com.example.clubhome.ui.matches

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clubhome.data.model.Match
import com.example.clubhome.data.remote.SupabaseClientManager
import com.example.clubhome.ui.auth.BaseballNavy
import com.example.clubhome.ui.auth.BaseballRed
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMatchesScreen(
    onMatchFound: (matchId: String) -> Unit,
    onBackClick: () -> Unit
) {
    var matchCodeInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun searchMatchByCode() {
        val cleanCode = matchCodeInput.trim().uppercase()
        if (cleanCode.length < 6) {
            errorMessage = "El código debe tener 6 caracteres"
            return
        }

        isSearching = true
        errorMessage = null

        scope.launch(Dispatchers.IO) {
            try {
                val matches = SupabaseClientManager.client.postgrest["matches"]
                    .select {
                        filter {
                            eq("code", cleanCode)
                            eq("status", "LIVE")
                        }
                    }
                    .decodeList<Match>()

                withContext(Dispatchers.Main) {
                    isSearching = false
                    if (matches.isNotEmpty()) {
                        val match = matches.first()
                        match.id?.let { matchId ->
                            onMatchFound(matchId)
                        } ?: run {
                            errorMessage = "Error al obtener el ID del partido"
                        }
                    } else {
                        errorMessage = "No se encontró ningún partido activo con ese código"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isSearching = false
                    errorMessage = "Error de conexión al buscar el partido"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ver Partido en Vivo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF000726))
            )
        },
        containerColor = BaseballNavy
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF001254)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Numbers,
                        contentDescription = null,
                        tint = BaseballRed,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "Ingresa el Código del Partido",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Pídele el código de 6 dígitos al creador del partido para verlo en tiempo real.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = matchCodeInput,
                        onValueChange = { input ->
                            if (input.length <= 6) {
                                matchCodeInput = input.uppercase()
                                errorMessage = null
                            }
                        },
                        label = { Text("Código (Ej. 8B31X9)", color = Color.Gray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BaseballRed,
                            unfocusedBorderColor = Color(0xFF1A2A70),
                            cursorColor = BaseballRed
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { searchMatchByCode() },
                        enabled = matchCodeInput.length == 6 && !isSearching,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BaseballRed,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "UNIRSE AL PARTIDO",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}