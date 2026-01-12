package dev.mcd.chess.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.mcd.chess.ui.puzzle.PuzzleScreen
import dev.mcd.chess.ui.screen.botgame.BotGameScreen
import dev.mcd.chess.ui.screen.botselection.BotSelectionScreen
import dev.mcd.chess.ui.screen.choosemode.ChooseModeScreen
import dev.mcd.chess.ui.screen.onlinegame.OnlineGameScreen
import dev.mcd.chess.ui.screen.settings.SettingsScreen
import dev.mcd.chess.ui.screen.admin.AdminScreen
import dev.mcd.chess.ui.screen.login.LoginScreen

@Composable
fun Routing() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("choosemode") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("choosemode") {
            ChooseModeScreen(
                onPlayOnline = { navController.navigate("game/online") },
                onPlayBot = { navController.navigate("selectbot") },
                onSolvePuzzle = { navController.navigate("puzzle") },
                onNavigateSettings = { navController.navigate("settings") },
                onNavigateExistingGame = { gameId ->
                    navController.navigate("game/online?gameId=$gameId")
                },
                onNavigateAdmin = { navController.navigate("admin") },
            )
        }

        composable("selectbot") {
            BotSelectionScreen(
                onBotSelected = { bot, side -> navController.navigate("game/bot/$bot/$side") },
                onDismiss = { navController.popBackStack() },
            )
        }

        composable(
            route = "game/bot/{bot}/{side}",
            arguments = listOf(
                navArgument("bot") { type = NavType.StringType },
                navArgument("side") { type = NavType.StringType },
            ),
        ) {
            BotGameScreen {
                navController.popBackStack()
            }
        }

        composable(
            route = "game/online?gameId={gameId}",
            arguments = listOf(
                navArgument("gameId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            OnlineGameScreen {
                navController.popBackStack()
            }
        }

        composable("puzzle") { PuzzleScreen() }

        composable("settings") {
            SettingsScreen { navController.popBackStack() }
        }

        composable("admin") {
            AdminScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
