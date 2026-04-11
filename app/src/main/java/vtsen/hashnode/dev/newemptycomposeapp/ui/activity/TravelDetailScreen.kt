package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/* =========================================================
   PANTALLA 2 · DETALLE DEL VIAJE
   ========================================================= */

@Composable
fun TravelDetailScreen(
    viewModel: ActivityViewModel,
    onCloseTravel: () -> Unit
) {

    val travel by viewModel.currentTravel.collectAsStateWithLifecycle()

    var kmEnd by remember { mutableStateOf("") }
    var hoursImputed by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    travel?.let { t ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Text(
                text = "Detalle del viaje",
                style = MaterialTheme.typography.displaySmall
            )

            Text("${t.origin} → ${t.destination}")
            Text("KM inicio: ${t.kmStart}")

            OutlinedTextField(
                value = kmEnd,
                onValueChange = { kmEnd = it },
                label = { Text("KM fin") },
                isError = errorMessage != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = hoursImputed,
                onValueChange = { hoursImputed = it },
                label = { Text("Horas imputadas") },
                isError = errorMessage != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))

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
                        errorMessage = "Datos inválidos (KM u horas)"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar viaje")
            }
        }
    }
}