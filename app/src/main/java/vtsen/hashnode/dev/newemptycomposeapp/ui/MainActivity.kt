package vtsen.hashnode.dev.newemptycomposeapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.ActivityHomeScreen
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.ActivityViewModel
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.ActivityViewModelFactory
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.NewTravelScreen
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.TravelDetailScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AxisApp()
            }
        }
    }
}

private sealed class Screen {
    object Home : Screen()
    object NewTravel : Screen()
    object TravelDetail : Screen()
}

@Composable
private fun AxisApp() {
    val context = LocalContext.current
    val viewModel: ActivityViewModel = viewModel(
        factory = ActivityViewModelFactory(context)
    )

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    BackHandler(currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    when (currentScreen) {
        Screen.Home -> ActivityHomeScreen(
            viewModel = viewModel,
            onNewTravelClick = { currentScreen = Screen.NewTravel }
        )

        Screen.NewTravel -> NewTravelScreen(
            viewModel = viewModel,
            onStartTravel = { currentScreen = Screen.TravelDetail }
        )

        Screen.TravelDetail -> TravelDetailScreen(
            viewModel = viewModel,
            onCloseTravel = { currentScreen = Screen.Home }
        )
    }
}
