package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NewTravelScreen(
    viewModel: ActivityViewModel,
    onStartTravel: () -> Unit
) {
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var kmStart by remember { mutableStateOf("") }
    var billing by remember { mutableStateOf("") }
    var hasDiet by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = origin,
            onValueChange = { origin = it },
            label = { Text("Origen") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("Destino") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = kmStart,
            onValueChange = { kmStart = it },
            label = { Text("Kilómetros inicio") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = billing,
            onValueChange = { billing = it },
            label = { Text("Facturación prevista (€)") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = hasDiet,
                onCheckedChange = { hasDiet = it }
            )
            Text("Incluir dieta en este viaje")
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = {
                val success = viewModel.startTravel(
                    origin = origin,
                    destination = destination,
                    description = description,
                    kmStart = kmStart.toIntOrNull() ?: -1,
                    billingExpected = billing.toDoubleOrNull() ?: -1.0,
                    hasDiet = hasDiet
                )

                if (success) {
                    errorMessage = null
                    onStartTravel()
                } else {
                    errorMessage = "Revisa los campos obligatorios. Los números deben ser válidos."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Iniciar viaje")
        }
    }
}
