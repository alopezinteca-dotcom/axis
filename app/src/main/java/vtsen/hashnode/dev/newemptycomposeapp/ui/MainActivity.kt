package vtsen.hashnode.dev.newemptycomposeapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
            else -> Screen.Menu
        }
    }

    when (currentScreen) {
        Screen.Menu -> AxisMenuScreen(
            onActivityClick = { currentScreen = Screen.ActivityHome },
            onLocationClick = { currentScreen = Screen.Location }
        )

        Screen.ActivityHome -> ActivityHomeScreen(
            viewModel = viewModel,
            onNewTravelClick = { currentScreen = Screen.NewTravel }
        )

        Screen.NewTravel -> NewTravelScreen(
            viewModel = viewModel,
            onStartTravel = { currentScreen = Screen.TravelDetail }
        )

        Screen.TravelDetail -> TravelDetailScreen(
            viewModel = viewModel,
            onCloseTravel = { currentScreen = Screen.ActivityHome }
        )

        Screen.Location -> LocationPlaceholderScreen()
    }
}

@Composable
private fun AxisMenuScreen(
    onActivityClick: () -> Unit,
    onLocationClick: () -> Unit
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
            // Izquierda (40%) Branding
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
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
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Derecha (60%) Navegación
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AxisMenuCard(
                    title = "Activity",
                    subtitle = "Viajes, horas, exportación",
                    onClick = onActivityClick
                )
                Spacer(modifier = Modifier.height(24.dp))
                AxisMenuCard(
                    title = "Location",
                    subtitle = "GPS / ubicaciones (Módulo 2)",
                    onClick = onLocationClick
                )
            }
        }
    }
}

@Composable
private fun AxisMenuCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun LocationPlaceholderScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Location (Módulo 2)", style = MaterialTheme.typography.displaySmall)
            Text(
                "Placeholder: aquí irá el módulo GPS/ubicaciones.\n" +
                    "No se ha implementado todavía, pero ya está integrado en el menú.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
