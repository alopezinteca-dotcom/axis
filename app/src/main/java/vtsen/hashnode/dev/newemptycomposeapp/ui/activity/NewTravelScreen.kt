package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = origin,
            onValueChange = { origin = it },
            label = { Text("Origen") }
        )

        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("Destino") }
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción") }
        )

        OutlinedTextField(
            value = kmStart,
            onValueChange = { kmStart = it },
            label = { Text("Kilómetros inicio") }
        )

        OutlinedTextField(
            value = billing,
            onValueChange = { billing = it },
            label = { Text("Facturación prevista") }
        )

        Button(
            onClick = {
                val success = viewModel.startTravel(
                    origin = origin,
                    destination = destination,
                    description = description,
                    kmStart = kmStart.toIntOrNull() ?: -1,
                    billingExpected = billing.toDoubleOrNull() ?: -1.0,
                    hasDiet = false
                )

                if (success) {
                    onStartTravel()
                }
            }
        ) {
            Text("Iniciar viaje")
        }
    }
}
