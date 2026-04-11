package vtsen.hashnode.dev.newemptycomposeapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.*

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

/* =========================================================
   ESTADOS DE NAVEGACIÓN
   ========================================================= */

private sealed class Screen {
    object Menu : Screen()
    object ActivityHome : Screen()
    object NewTravel : Screen()
    object TravelDetail : Screen()
}

/* =========================================================
   ROOT APP + VIEWMODEL + BACKHANDLER
   ========================================================= */

@Composable
private fun AxisApp() {

    val context = LocalContext.current

    val activityViewModel: ActivityViewModel = viewModel(
        factory = ActivityViewModelFactory(context)
    )

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Menu) }

    /* ---------- BACK HANDLER ---------- */
    BackHandler(currentScreen != Screen.Menu) {
        currentScreen = when (currentScreen) {
            Screen.TravelDetail -> Screen.ActivityHome
            Screen.NewTravel -> Screen.ActivityHome
            Screen.ActivityHome -> Screen.Menu
            else -> Screen.Menu
        }
    }

    when (currentScreen) {

        Screen.Menu -> {
            AxisMenuScreen(
                onActivityClick = {
                    currentScreen = Screen.ActivityHome
                }
            )
        }

        Screen.ActivityHome -> {
            ActivityHomeScreen(
                viewModel = activityViewModel,
                onNewTravelClick = {
                    currentScreen = Screen.NewTravel
                }
            )
        }

        Screen.NewTravel -> {
            NewTravelScreen(
                viewModel = activityViewModel,
                onStartTravel = {
                    currentScreen = Screen.TravelDetail
                }
            )
        }

        Screen.TravelDetail -> {
            TravelDetailScreen(
                viewModel = activityViewModel,
                onCloseTravel = {
                    currentScreen = Screen.ActivityHome
                }
            )
        }
    }
}

/* =========================================================
   PANTALLA 0 · MENÚ
   ========================================================= */

@Composable
fun AxisMenuScreen(
    onActivityClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(0.4f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "AXIS",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Suite Profesional",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                MenuCard(
                    title = "Activity",
                    subtitle = "Horas, viajes, rentabilidad",
                    onClick = onActivityClick
                )

                Spacer(modifier = Modifier.height(24.dp))

                MenuCard(
                    title = "Location",
                    subtitle = "GPS, ubicación y referencias",
                    onClick = {}
                )
            }
        }
    }
}

@Composable
fun MenuCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge)
        }
    }
}