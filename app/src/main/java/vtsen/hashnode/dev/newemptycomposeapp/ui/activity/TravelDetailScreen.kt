package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelDetailScreen(
    viewModel: ActivityViewModel,
    onCloseTravel: () -> Unit
) {
    val travel by viewModel.currentTravel.collectAsStateWithLifecycle()

    var kmEnd by rememberSaveable { mutableStateOf("") }
    var hoursDraftText by rememberSaveable { mutableStateOf("") }
    var hoursCalculatedText by rememberSaveable { mutableStateOf("") }
    var hoursImputedText by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(travel?.id) {
        travel?.let {
            hoursDraftText = it.hoursDraft?.toString() ?: ""
            hoursCalculatedText = it.hoursCalculatedSnapshot?.toString()
                ?: (it.hoursDraft?.toString() ?: "")
            hoursImputedText = it.hoursImputed?.toString()
                ?: (it.hoursDraft?.toString() ?: "")
        }
    }

    if (travel == null) {
        LaunchedEffect(Unit) { onCloseTravel() }
        return
    }

    val startTimeStr = formatter.format(Date(travel!!.startTimestamp))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cierre de Expediente") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // IZQUIERDA (40%): Contexto
                Card(
                    modifier = Modifier.weight(0.4f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Contexto Actual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Divider()
                        Text("📍 Origen: ${travel!!.origin}", style = MaterialTheme.typography.bodyLarge)
                        Text("🏁 Destino: ${travel!!.destination}", style = MaterialTheme.typography.bodyLarge)
                        Text("📝 Ref: ${travel!!.description}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Divider()
                        Text("⏱️ Hora Salida: $startTimeStr", style = MaterialTheme.typography.bodyLarge)
                        Text("🚗 KM Iniciales: ${travel!!.kmStart}", style = MaterialTheme.typography.bodyLarge)
                        Text("💶 Facturación: ${travel!!.billingExpected} €", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                // DERECHA (60%): Draft + Cierre
                Card(
                    modifier = Modifier.weight(0.6f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Imputación y Cierre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        // FASE 1: Draft
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = hoursDraftText,
                                onValueChange = { hoursDraftText = it },
                                label = { Text("Horas Provisionales (Draft)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                supportingText = { Text("Anotación temporal en ruta") }
                            )
                            Button(
                                onClick = {
                                    val draft = hoursDraftText.replace(',', '.').toDoubleOrNull()
                                    val ok = viewModel.updateHoursDraft(draft)
                                    errorMessage = if (ok) null else "Horas provisionales inválidas."
                                }
                            ) {
                                Text("Guardar Draft")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // FASE 2: Cierre
                        Text("Datos Finales", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)

                        OutlinedTextField(
                            value = kmEnd,
                            onValueChange = { kmEnd = it },
                            label = { Text("Kilómetros de Llegada") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = hoursCalculatedText,
                            onValueChange = { hoursCalculatedText = it },
                            label = { Text("Horas calculadas (snapshot del modelo)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            supportingText = {
                                Text(
                                    "Temporal (DEV): más adelante será automático y solo lectura.\n" +
                                        "Se guarda en Room para que Excel NO recalculé nada."
                                )
                            }
                        )

                        OutlinedTextField(
                            value = hoursImputedText,
                            onValueChange = { hoursImputedText = it },
                            label = { Text("Horas imputadas (definitivas)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = {
                                val endKm = kmEnd.toIntOrNull() ?: -1
                                val calculated = hoursCalculatedText.replace(',', '.').toDoubleOrNull() ?: -1.0
                                val imputed = hoursImputedText.replace(',', '.').toDoubleOrNull() ?: -1.0

                                val ok = viewModel.closeCurrentTravel(
                                    kmEnd = endKm,
                                    hoursImputed = imputed,
                                    hoursCalculated = calculated
                                )

                                if (ok) {
                                    errorMessage = null
                                    onCloseTravel()
                                } else {
                                    errorMessage = "Revisa KM fin y horas (deben ser válidos y > 0)."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("CERRAR VIAJE DEFINITIVAMENTE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
