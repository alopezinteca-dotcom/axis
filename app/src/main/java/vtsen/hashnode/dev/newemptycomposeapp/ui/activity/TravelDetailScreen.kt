package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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

    var kmEnd by remember { mutableStateOf("") }
    var hoursImputed by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        travel?.let {
            Text("${it.origin} → ${it.destination}")
            Text("Km inicio: ${it.kmStart}")
        }

        OutlinedTextField(
            value = kmEnd,
            onValueChange = { kmEnd = it },
            label = { Text("Kilómetros fin") }
        )

        OutlinedTextField(
            value = hoursImputed,
            onValueChange = { hoursImputed = it },
            label = { Text("Horas imputadas") }
        )

        Button(
            onClick = {
                val success = viewModel.closeCurrentTravel(
                    kmEnd = kmEnd.toIntOrNull() ?: -1,
                    hoursImputed = hoursImputed.toDoubleOrNull() ?: -1.0
                )

                if (success) {
                    onCloseTravel()
                }
            }
        ) {
            Text("Cerrar viaje")
        }
    }
}
