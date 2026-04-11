package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TravelDetailScreen(
    viewModel: ActivityViewModel,
    onCloseTravel: () -> Unit
) {
    val travel by viewModel.currentTravel.collectAsStateWithLifecycle()

    var hours by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Detalle del viaje")

        travel?.let {
            Text("${it.origin} → ${it.destination}")
        }

        OutlinedTextField(
            value = hours,
            onValueChange = { hours = it },
            label = { Text("Horas imputadas") }
        )

        Button(onClick = {
            viewModel.closeCurrentTravel(
                kmEnd = 0,
                hoursImputed = hours.toDoubleOrNull() ?: 0.0
            )
            onCloseTravel()
        }) {
            Text("Cerrar viaje")
        }
    }
}
