package com.example.clubhome.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        val session = SupabaseClientManager.client.auth.currentSessionOrNull()
        delay(300L) // Inicio ultrarrápido (300ms)

        if (session != null) {
            onSplashFinished("home")
        } else {
            onSplashFinished("welcome")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCBE3F7)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.iconoappgrande),
            contentDescription = "Pantalla de inicio Score",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}