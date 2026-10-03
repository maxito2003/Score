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
import com.example.clubhome.ui.game.GameViewModel
import com.example.clubhome.ui.home.HomeScreen
import com.example.clubhome.ui.matches.FinishedGameDetailScreen
import com.example.clubhome.ui.matches.FinishedGamesListScreen
import com.example.clubhome.ui.matches.LiveMatchesScreen
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

    // Instancia compartida del GameViewModel para conservar marcadores, lineups y cronómetro
    val sharedGameViewModel: GameViewModel = viewModel()

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

        // Ruta de Ingreso por Código de 6 Dígitos
        composable("matches_list") {
            LiveMatchesScreen(
                onMatchFound = { matchId ->
                    if (matchId.isNotBlank()) {
                        navController.navigate("match_stats/$matchId")
                    } else {
                        Toast.makeText(context, "ID de partido no válido", Toast.LENGTH_SHORT).show()
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Ruta de Lista de Partidos Terminados
        composable("finished_games_list") {
            FinishedGamesListScreen(
                onMatchClick = { matchId ->
                    navController.navigate("finished_game_detail/$matchId")
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Ruta de Detalle de Partido Terminado
        composable(
            route = "finished_game_detail/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""

            FinishedGameDetailScreen(
                matchId = matchId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Ruta para ver las Estadísticas / Detalles del Partido en Tiempo Real (Espectador)
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

        // Ruta de Partido Interactivo (Anotador)
        composable(
            route = "match/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val modeTitle = backStackEntry.arguments?.getString("mode") ?: "LIGA"

            GameScreen(
                modeTitle = modeTitle,
                gameViewModel = sharedGameViewModel,
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateTeams = {
                    navController.navigate("teams/$modeTitle") {
                        launchSingleTop = true
                    }
                },
                onNavigatePlayers = {
                    navController.navigate("players/general/General/$modeTitle") {
                        launchSingleTop = true
                    }
                },
                onNavigateFinishedGames = {
                    navController.navigate("finished_games_list")
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
                    // Regresa a la pantalla del Partido existente
                    navController.popBackStack()
                },
                onSignOut = {
                    authViewModel.signOut {
                        navController.navigate("welcome") { popUpTo(0) }
                    }
                }
            )
        }

        // Ruta de Jugadores (incluye parámetro de modo LIGA/PERSONAL)
        composable(
            route = "players/{teamId}/{teamName}/{mode}",
            arguments = listOf(
                navArgument("teamId") { type = NavType.StringType },
                navArgument("teamName") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType; defaultValue = "LIGA" }
            )
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
            val teamName = backStackEntry.arguments?.getString("teamName") ?: "Equipo"
            val mode = backStackEntry.arguments?.getString("mode") ?: "LIGA"

            PlayersScreen(
                teamId = teamId,
                teamName = teamName,
                mode = mode,
                onBack = { navController.popBackStack() }
            )
        }
    }
}