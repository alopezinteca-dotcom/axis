package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity

@Composable
fun ActivityHomeScreen(
    viewModel: ActivityViewModel,
    onNewTravelClick: () -> Unit
) {
    val currentTravel by viewModel.currentTravel.collectAsStateWithLifecycle()
    val closedTravels by viewModel.exportData.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.prepareExport()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTravelClick) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Resumen Activity",
                style = MaterialTheme.typography.headlineMedium
            )

            currentTravel?.let {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Viaje en curso")
                        Text("${it.origin} → ${it.destination}")
                    }
                }
            }

            Text(
                text = "Viajes cerrados: ${closedTravels.size}",
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn {
                items(closedTravels) { travel ->
                    ClosedTravelItem(travel)
                }
            }
        }
    }
}

@Composable
private fun ClosedTravelItem(travel: TravelEntity) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val dateText = travel.endTimestamp?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    } ?: ""

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("${travel.origin} → ${travel.destination}")
                Text(dateText, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${travel.hoursImputed ?: 0.0} h")
                Text("${travel.billingExpected} €")
            }
        }
    }
}
