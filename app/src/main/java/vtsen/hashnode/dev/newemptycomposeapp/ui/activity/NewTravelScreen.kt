package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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

        Button(onClick = {
            viewModel.startTravel(
                origin = origin,
                destination = destination,
                description = "",
                kmStart = 0,
                billingExpected = 0.0,
                hasDiet = false
            )
            onStartTravel()
        }) {
            Text("Iniciar viaje")
        }
    }
}
