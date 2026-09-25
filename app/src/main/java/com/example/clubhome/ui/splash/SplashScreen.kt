package com.example.clubhome.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.clubhome.R
import com.example.clubhome.data.remote.SupabaseClientManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000L) // Muestra la imagen durante 2 segundos

        // Consulta si hay un token/usuario persistido en Supabase
        val session = SupabaseClientManager.client.auth.currentSessionOrNull()

        if (session != null) {
            onSplashFinished("home")
        } else {
            onSplashFinished("welcome")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.iconoappgrande),
            contentDescription = "Pantalla de inicio Score",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}