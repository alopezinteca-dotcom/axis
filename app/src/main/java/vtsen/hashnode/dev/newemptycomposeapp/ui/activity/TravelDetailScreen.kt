package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Detalle del viaje", style = MaterialTheme.typography.headlineMedium)

        travel?.let {
            Text("${it.origin} → ${it.destination}", style = MaterialTheme.typography.titleMedium)
            Text("Kilómetros de inicio: ${it.kmStart}")
        }

        OutlinedTextField(
            value = kmEnd,
            onValueChange = { kmEnd = it },
            label = { Text("Kilómetros fin") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = hoursImputed,
            onValueChange = { hoursImputed = it },
            label = { Text("Horas imputadas") },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = {
                val success = viewModel.closeCurrentTravel(
                    kmEnd = kmEnd.toIntOrNull() ?: -1,
                    hoursImputed = hoursImputed.toDoubleOrNull() ?: -1.0
                )

                if (success) {
                    errorMessage = null
                    onCloseTravel()
                } else {
                    errorMessage = "Error: KM fin debe ser mayor al inicio y las horas válidas."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar viaje")
        }
    }
}
