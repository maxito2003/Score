package com.example.clubhome

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.clubhome.ui.auth.*
import com.example.clubhome.ui.game.GameScreen
import com.example.clubhome.ui.home.HomeScreen
import com.example.clubhome.ui.matches.LiveMatchesScreen
import com.example.clubhome.ui.matches.LiveMatchesViewModel
import com.example.clubhome.ui.matches.MatchStatsScreen
import com.example.clubhome.ui.players.PlayersScreen
import com.example.clubhome.ui.splash.SplashScreen
import com.example.clubhome.ui.teams.TeamsScreen
import com.example.clubhome.ui.theme.CLUBHOMETheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CLUBHOMETheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    ClubHomeApp()
                }
            }
        }
    }
}

@Composable
fun ClubHomeApp(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onSplashFinished = { destination ->
                    navController.navigate(destination) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("welcome") {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("login") {
            LoginScreen(
                uiState = uiState,
                onEmailChange = authViewModel::onEmailChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onLoginClick = {
                    authViewModel.signIn {
                        navController.navigate("home") { popUpTo("welcome") { inclusive = true } }
                    }
                },
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateBack = { navController.popBackStack() }
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
                        navController.navigate("home") { popUpTo("welcome") { inclusive = true } }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login") { popUpTo("register") { inclusive = true } }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("home") {
            HomeScreen(
                userName = uiState.currentUser?.userMetadata?.get("name")?.toString(),
                onLeagueClick = { navController.navigate("match/LIGA") },
                onPersonalClick = { navController.navigate("match/PERSONAL") },
                onViewMatchesClick = { navController.navigate("matches_list") },
                onSignOutClick = {
                    authViewModel.signOut {
                        navController.navigate("welcome") { popUpTo("home") { inclusive = true } }
                    }
                }
            )
        }

        // Ruta de Lista de Partidos en Tiempo Real
        composable("matches_list") {
            val liveViewModel: LiveMatchesViewModel = viewModel()
            val matchesList by liveViewModel.matches.collectAsState()
            val isLoading by liveViewModel.isLoading.collectAsState()

            LiveMatchesScreen(
                matches = matchesList,
                isLoading = isLoading,
                onMatchClick = { matchId ->
                    if (matchId.isNotBlank()) {
                        navController.navigate("match_stats/$matchId")
                    } else {
                        Toast.makeText(context, "ID de partido no válido", Toast.LENGTH_SHORT).show()
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Ruta para ver las Estadísticas / Detalles del Partido
        composable(
            route = "match_stats/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""

            MatchStatsScreen(
                matchId = matchId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Ruta de Partido con la Grid del Rombo
        composable(
            route = "match/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val modeTitle = backStackEntry.arguments?.getString("mode") ?: "LIGA"

            GameScreen(
                modeTitle = modeTitle,
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateTeams = {
                    navController.navigate("teams/$modeTitle")
                },
                onNavigatePlayers = {
                    navController.navigate("players/general/General")
                },
                onSignOut = {
                    authViewModel.signOut {
                        navController.navigate("welcome") { popUpTo(0) }
                    }
                }
            )
        }

        // Ruta de Equipos
        composable(
            route = "teams/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val modeTitle = backStackEntry.arguments?.getString("mode") ?: "LIGA"
            TeamsScreen(
                modeTitle = modeTitle,
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateMatch = {
                    navController.navigate("match/$modeTitle") {
                        popUpTo("match/$modeTitle") { inclusive = true }
                    }
                },
                onSignOut = {
                    authViewModel.signOut {
                        navController.navigate("welcome") { popUpTo(0) }
                    }
                }
            )
        }

        // Ruta de Jugadores
        composable(
            route = "players/{teamId}/{teamName}",
            arguments = listOf(
                navArgument("teamId") { type = NavType.StringType },
                navArgument("teamName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
            val teamName = backStackEntry.arguments?.getString("teamName") ?: "Equipo"

            PlayersScreen(
                teamId = teamId,
                teamName = teamName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}