package com.example.clubhome

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.clubhome.ui.auth.AuthViewModel
import com.example.clubhome.ui.auth.LoginScreen
import com.example.clubhome.ui.auth.RegisterScreen
import com.example.clubhome.ui.auth.WelcomeScreen
import com.example.clubhome.ui.home.HomeScreen
import com.example.clubhome.ui.theme.CLUBHOMETheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CLUBHOMETheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    ClubHomeApp()
                }
            }
        }
    }
}

@Composable
fun ClubHomeApp(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val startDestination = if (uiState.isLoggedIn) "home" else "welcome"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("welcome") {
            WelcomeScreen(
                onNavigateToLogin = {
                    authViewModel.clearMessages()
                    navController.navigate("login")
                },
                onNavigateToRegister = {
                    authViewModel.clearMessages()
                    navController.navigate("register")
                }
            )
        }

        composable("login") {
            LoginScreen(
                uiState = uiState,
                onEmailChange = authViewModel::onEmailChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onLoginClick = {
                    authViewModel.signIn {
                        Toast.makeText(context, "¡Bienvenido a Score!", Toast.LENGTH_SHORT).show()
                        navController.navigate("home") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = {
                    authViewModel.clearMessages()
                    navController.navigate("register")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("register") {
            RegisterScreen(
                uiState = uiState,
                onNameChange = authViewModel::onNameChange,
                onEmailChange = authViewModel::onEmailChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onConfirmPasswordChange = authViewModel::onConfirmPasswordChange,
                onRegisterClick = {
                    authViewModel.signUp {
                        Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        navController.navigate("home") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                },
                onNavigateToLogin = {
                    authViewModel.clearMessages()
                    navController.navigate("register")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("home") {
            val userName = uiState.currentUser?.userMetadata?.get("name")?.toString()
                ?: uiState.name.ifEmpty { null }

            HomeScreen(
                userName = userName,
                onLeagueClick = {
                    Toast.makeText(context, "Modo Liga seleccionado", Toast.LENGTH_SHORT).show()
                },
                onPersonalClick = {
                    Toast.makeText(context, "Modo Personal seleccionado", Toast.LENGTH_SHORT).show()
                },
                onSignOutClick = {
                    authViewModel.signOut {
                        navController.navigate("welcome") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
