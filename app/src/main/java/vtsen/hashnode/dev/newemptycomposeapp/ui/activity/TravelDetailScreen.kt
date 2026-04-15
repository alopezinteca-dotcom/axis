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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStatus
import vtsen.hashnode.dev.newemptycomposeapp.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelDetailScreen(
    viewModel: ActivityViewModel,
    settingsViewModel: SettingsViewModel,
    onCloseTravel: () -> Unit
) {
    val travel by viewModel.currentTravel.collectAsStateWithLifecycle()
    val stops by viewModel.stopsForCurrentTravel.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    var kmEnd by rememberSaveable { mutableStateOf("") }
    var hoursDraftText by rememberSaveable { mutableStateOf("") }
    var hoursCalculatedText by rememberSaveable { mutableStateOf("") } // RAW con decimales
    var hoursImputedText by rememberSaveable { mutableStateOf("") }

    var endAddressText by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Stops dialog
    var showAddStop by remember { mutableStateOf(false) }
    var showEditStop by remember { mutableStateOf(false) }
    var editStopId by remember { mutableStateOf<String?>(null) }
    var stopPlace by remember { mutableStateOf("") }
    var stopKmText by remember { mutableStateOf("") }

    val dtFmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    if (travel == null) {
        LaunchedEffect(Unit) { onCloseTravel() }
        return
    }
    val t = travel!!

    fun parseStopKm(): Int? = stopKmText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

    LaunchedEffect(t.id) {
        hoursDraftText = t.hoursDraft?.toString() ?: ""
        hoursImputedText = t.hoursImputed?.toString() ?: (t.hoursDraft?.toString() ?: "")
        hoursCalculatedText = t.hoursCalculatedSnapshot?.toString() ?: ""
        endAddressText = t.endAddress
    }

    // ✅ MODELO A (35% facturación): horasRaw = (0.65F - costes) / costeHora
    val COSTE_KM_OPERATIVO = settings.costeKmOperativo
    val COSTE_DIETA_FIJA = settings.costeDietaFija
    val COSTE_HORA_ALEJANDRO = settings.costeHoraAlejandro

    val kmEndInt = kmEnd.toIntOrNull()
    val kmDone = if (kmEndInt != null) max(0, kmEndInt - t.kmStart) else null

    val suggestedHoursRaw: Double? = remember(kmDone, t.billingExpected, t.hasDiet, settings) {
        if (kmDone == null) return@remember null
        val facturacion = t.billingExpected
        val costeKm = kmDone * COSTE_KM_OPERATIVO
        val costeDieta = if (t.hasDiet) COSTE_DIETA_FIJA else 0.0
        val baseHorasEuros = (0.65 * facturacion) - costeKm - costeDieta
        val horasRaw = baseHorasEuros / COSTE_HORA_ALEJANDRO
        max(0.0, horasRaw)
    }

    LaunchedEffect(suggestedHoursRaw) {
        if (suggestedHoursRaw != null) {
            hoursCalculatedText = String.format(Locale.US, "%.2f", suggestedHoursRaw)
        }
    }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Salida", fontWeight = FontWeight.Bold)
                    Text(dtFmt.format(Date(t.startTimestamp)))
                    Text("Origen: ${t.origin}")
                    Text("Destino: ${t.destination}")
                    Text("KM inicio: ${t.kmStart}")
                    Text("Facturación: ${String.format(Locale.getDefault(), "%.2f", t.billingExpected)} €")
                    Text("Dieta: ${if (t.hasDiet) "SI" else "NO"}")

                    Spacer(Modifier.height(8.dp))
                    Text("Llegada", fontWeight = FontWeight.Bold)
                    Text(if (t.endTimestamp != null) dtFmt.format(Date(t.endTimestamp)) else "—")
                    if (t.endAddress.isNotBlank()) Text("📍 ${t.endAddress}")
                }
            }

            // Paradas con fecha/hora
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Paradas intermedias", fontWeight = FontWeight.Bold)

                    if (t.status == TravelStatus.IN_PROGRESS) {
                        Button(
                            onClick = {
                                stopPlace = ""
                                stopKmText = ""
                                showAddStop = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📍 AÑADIR PARADA", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (stops.isEmpty()) {
                        Text("No hay paradas.")
                    } else {
                        stops.forEach { s ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Row(
                                    Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("📍 ${dtFmt.format(Date(s.timestamp))}", fontWeight = FontWeight.SemiBold)
                                        Text(s.place)
                                        Text("KM: ${s.kmOdometer?.toString() ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (t.status == TravelStatus.IN_PROGRESS) {
                                        TextButton(onClick = {
                                            editStopId = s.id
                                            stopPlace = s.place
                                            stopKmText = s.kmOdometer?.toString() ?: ""
                                            showEditStop = true
                                        }) { Text("✏️") }
                                        TextButton(onClick = { viewModel.deleteStop(s.id) }) { Text("🗑️") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Cierre + GPS llegada
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Imputación y cierre", fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = kmEnd,
                        onValueChange = { kmEnd = it.filter(Char::isDigit) },
                        label = { Text("KM de llegada") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hoursCalculatedText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Horas sugeridas (Modelo A 35%) — raw") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hoursImputedText,
                        onValueChange = { hoursImputedText = it },
                        label = { Text("Horas imputadas") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = endAddressText,
                            onValueChange = { endAddressText = it },
                            label = { Text("📍 Dirección llegada (calle, ciudad)") },
                            modifier = Modifier.weight(1f)
                        )
                        GpsAddressButton(
                            onError = { errorMessage = it },
                            onAddress = { addr -> endAddressText = addr }
                        )
                    }

                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    Button(
                        onClick = {
                            val endKm = kmEnd.toIntOrNull() ?: -1
                            val calc = hoursCalculatedText.replace(',', '.').toDoubleOrNull() ?: -1.0
                            val imp = hoursImputedText.replace(',', '.').toDoubleOrNull() ?: -1.0

                            if (endKm < t.kmStart) {
                                errorMessage = "KM fin inválido."
                                return@Button
                            }
                            if (calc <= 0.0) {
                                errorMessage = "Introduce KM fin para calcular horas."
                                return@Button
                            }
                            if (imp <= 0.0) {
                                errorMessage = "Horas imputadas inválidas."
                                return@Button
                            }

                            viewModel.insertOrUpdateTravel(t.copy(endAddress = endAddressText))

                            val ok = viewModel.closeCurrentTravel(endKm, imp, calc)
                            if (ok) onCloseTravel() else errorMessage = "No se pudo cerrar el viaje."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("CERRAR VIAJE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dialog nueva parada con botón 📍
    if (showAddStop) {
        AlertDialog(
            onDismissRequest = { showAddStop = false },
            title = { Text("Nueva parada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stopPlace,
                        onValueChange = { stopPlace = it },
                        label = { Text("Lugar / Calle, Ciudad") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        GpsAddressButton(
                            onError = { errorMessage = it },
                            onAddress = { addr -> stopPlace = addr }
                        )
                        OutlinedTextField(
                            value = stopKmText,
                            onValueChange = { stopKmText = it.filter(Char::isDigit) },
                            label = { Text("KM (opcional)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val ok = viewModel.addStop(stopPlace, parseStopKm())
                    if (ok) showAddStop = false else errorMessage = "Parada inválida."
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddStop = false }) { Text("Cancelar") } }
        )
    }

    // Dialog editar parada con botón 📍
    if (showEditStop && editStopId != null) {
        AlertDialog(
            onDismissRequest = { showEditStop = false },
            title = { Text("Editar parada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stopPlace,
                        onValueChange = { stopPlace = it },
                        label = { Text("Lugar / Calle, Ciudad") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        GpsAddressButton(
                            onError = { errorMessage = it },
                            onAddress = { addr -> stopPlace = addr }
                        )
                        OutlinedTextField(
                            value = stopKmText,
                            onValueChange = { stopKmText = it.filter(Char::isDigit) },
                            label = { Text("KM (opcional)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val ok = viewModel.updateStop(editStopId!!, stopPlace, parseStopKm())
                    if (ok) showEditStop = false else errorMessage = "No se pudo actualizar."
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showEditStop = false }) { Text("Cancelar") } }
        )
    }
}
