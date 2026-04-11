package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/* =========================================================
   PANTALLA 3 · NUEVO VIAJE
   ========================================================= */

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
            .padding(48.dp)
    ) {

        Text(
            text = "Nuevo viaje",
            style = MaterialTheme.typography.displaySmall
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {

            /* ---------- IZQUIERDA ---------- */
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Origen") },
                    isError = errorMessage != null && origin.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destino") },
                    isError = errorMessage != null && destination.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción / Inspección") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
            }

            /* ---------- DERECHA ---------- */
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                OutlinedTextField(
                    value = kmStart,
                    onValueChange = { kmStart = it },
                    label = { Text("KM inicio") },
                    isError = errorMessage != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = billing,
                    onValueChange = { billing = it },
                    label = { Text("Facturación prevista (€)") },
                    isError = errorMessage != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasDiet,
                        onCheckedChange = { hasDiet = it }
                    )
                    Text("Incluir dieta")
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

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
                            errorMessage = "Revisa los campos obligatorios"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Text("Iniciar viaje")
                }
            }
        }
    }
}