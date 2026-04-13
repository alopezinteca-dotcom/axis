package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.CalendarOverridesStore.Holiday
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.CalendarOverridesStore.Vacation

@Composable
fun CalendarManagementDialog(
    holidays: List<Holiday>,
    vacations: List<Vacation>,
    onAddHoliday: () -> Unit,
    onAddVacation: () -> Unit,
    onDeleteHoliday: (Holiday) -> Unit,
    onDeleteVacation: (Vacation) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        title = { Text("Gestión de Calendario", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ===== FESTIVOS =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Festivos", fontWeight = FontWeight.Bold)
                    Button(onClick = onAddHoliday) { Text("Añadir") }
                }

                if (holidays.isEmpty()) {
                    Text("No hay festivos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(holidays) { h ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("🔴 ${h.date}", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            h.description.ifBlank { "—" },
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    TextButton(onClick = { onDeleteHoliday(h) }) { Text("Eliminar") }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ===== VACACIONES =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vacaciones", fontWeight = FontWeight.Bold)
                    Button(onClick = onAddVacation) { Text("Añadir") }
                }

                if (vacations.isEmpty()) {
                    Text("No hay vacaciones.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vacations) { v ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("🟡 ${v.from} → ${v.to}", fontWeight = FontWeight.SemiBold)
                                        if (v.description.isNotBlank()) {
                                            Text(v.description, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        }
                                    }
                                    TextButton(onClick = { onDeleteVacation(v) }) { Text("Eliminar") }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
``
