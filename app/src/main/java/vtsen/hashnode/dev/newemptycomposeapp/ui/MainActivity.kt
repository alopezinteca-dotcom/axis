package vtsen.hashnode.dev.newemptycomposeapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.ActivityHomeScreen
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.ActivityViewModel
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.ActivityViewModelFactory
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.NewTravelScreen
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.TravelDetailScreen
import vtsen.hashnode.dev.newemptycomposeapp.ui.theme.AxisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            AxisTheme {
                AxisApp()
            }
        }
    }
}

private sealed class Screen {
    object Menu : Screen()
    object ActivityHome : Screen()
    object NewTravel : Screen()
    object TravelDetail : Screen()
    object Location : Screen()
    object Settings : Screen()
}

@Composable
private fun AxisApp() {
    val context = LocalContext.current
    val viewModel: ActivityViewModel = viewModel(factory = ActivityViewModelFactory(context))

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Menu) }

    BackHandler(enabled = currentScreen != Screen.Menu) {
        currentScreen = when (currentScreen) {
            Screen.TravelDetail -> Screen.ActivityHome
            Screen.NewTravel -> Screen.ActivityHome
            Screen.ActivityHome -> Screen.Menu
            Screen.Location -> Screen.Menu
            Screen.Settings -> Screen.Menu
            else -> Screen.Menu
        }
    }

    when (currentScreen) {
        Screen.Menu -> AxisMenuScreen(
            onActivityClick = { currentScreen = Screen.ActivityHome },
            onLocationClick = { currentScreen = Screen.Location },
            onSettingsClick = { currentScreen = Screen.Settings }
        )

        Screen.ActivityHome -> ActivityHomeScreen(
            viewModel = viewModel,
            onNewTravelClick = { currentScreen = Screen.NewTravel },
            onCurrentTravelClick = { currentScreen = Screen.TravelDetail }
        )

        Screen.NewTravel -> NewTravelScreen(
            viewModel = viewModel,
            onStartTravel = { currentScreen = Screen.TravelDetail }
        )

        Screen.TravelDetail -> TravelDetailScreen(
            viewModel = viewModel,
            onCloseTravel = { currentScreen = Screen.ActivityHome }
        )

        Screen.Location -> LocationComingSoonScreen()
        Screen.Settings -> SettingsComingSoonScreen()
    }
}

/* Menú 40/60 + Settings (placeholder por ahora) */

@Composable
private fun AxisMenuScreen(
    onActivityClick: () -> Unit,
    onLocationClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // (Mantén tu menú premium actual; solo asegúrate de incluir Settings.)
    // Si ya tienes AxisMenuScreen en otro archivo, no dupliques; usa el tuyo.
}

@Composable
private fun LocationComingSoonScreen() { /* placeholder */ }

@Composable
private fun SettingsComingSoonScreen() { /* placeholder */ }
