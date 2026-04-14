package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.round
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStatus
import vtsen.hashnode.dev.newemptycomposeapp.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelDetailScreen(
    viewModel: ActivityViewModel,
    settingsViewModel: SettingsViewModel,
    onCloseTravel: () -> Unit
) {
    val context = LocalContext.current
    val travel by viewModel.currentTravel.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val stops by viewModel.stopsForCurrentTravel.collectAsStateWithLifecycle()

    var kmStartText by rememberSaveable { mutableStateOf("") }
    var kmEndText by rememberSaveable { mutableStateOf("") }

    var hoursDraftText by rememberSaveable { mutableStateOf("") }
    var hoursCalculatedText by rememberSaveable { mutableStateOf("") }
    var hoursImputedText by rememberSaveable { mutableStateOf("") }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Stops dialog state
    var showAddStop by remember { mutableStateOf(false) }
    var showEditStop by remember { mutableStateOf(false) }
    var editStopId by remember { mutableStateOf<String?>(null) }
    var stopPlace by remember { mutableStateOf("") }
    var stopKmText by remember { mutableStateOf("") }
    var stopWarning by remember { mutableStateOf<String?>(null) }

    // Delete travel confirm
    var showDeleteTravelConfirm by remember { mutableStateOf(false) }

    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    if (travel == null) {
        LaunchedEffect(Unit) { onCloseTravel() }
        return
    }
    val t = travel!!

    LaunchedEffect(t.id) {
        kmStartText = t.kmStart.toString()
        kmEndText = t.kmEnd?.toString() ?: ""
        hoursDraftText = t.hoursDraft?.toString() ?: ""
        hoursImputedText = t.hoursImputed?.toString() ?: (t.hoursDraft?.toString() ?: "")
        hoursCalculatedText = t.hoursCalculatedSnapshot?.toString() ?: ""
    }

    fun openMaps(query: String) {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    fun parseStopKm(): Int? = stopKmText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

    // ===== Modelo A (como lo tienes ahora) =====
    val COSTE_KM_OPERATIVO = settings.costeKmOperativo
    val COSTE_DIETA_FIJA = settings.costeDietaFija
    val PORC_BENEF_EXIGIDO_A = settings.porcBenefExigidoA
    val COSTE_HORA_ALEJANDRO = settings.costeHoraAlejandro

    val kmEndInt = kmEndText.toIntOrNull()
    val kmStartInt = kmStartText.toIntOrNull() ?: t.kmStart
    val kmDone = if (kmEndInt != null) max(0, kmEndInt - kmStartInt) else null

    val suggestedHours: Double? = remember(kmDone, t.billingExpected, t.hasDiet, settings) {
        if (kmDone == null) return@remember null

        val facturacion = t.billingExpected
        val costeMaxPermitido = facturacion / (1.0 + PORC_BENEF_EXIGIDO_A)

        val costeKm = kmDone * COSTE_KM_OPERATIVO
        val costeDieta = if (t.hasDiet) COSTE_DIETA_FIJA else 0.0

        val presupuestoHoras = costeMaxPermitido - costeKm - costeDieta
        val horasRaw = presupuestoHoras / COSTE_HORA_ALEJANDRO
        val horasClamped = max(0.0, horasRaw)

        round(horasClamped * 10.0) / 10.0
    }

    LaunchedEffect(suggestedHours) {
        if (suggestedHours != null) {
            hoursCalculatedText = String.format(Locale.US, "%.1f", suggestedHours)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cierre de Expediente") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                actions = {
                    // Solo EN CURSO
                    if (t.status == TravelStatus.IN_PROGRESS) {
                        TextButton(onClick = { showDeleteTravelConfirm = true }) {
                            Text("🗑️", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                        }
                    }
                }
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

            // CONTEXTO
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Contexto", fontWeight = FontWeight.Bold)
                    Text("📍 Origen: ${t.origin}")
                    Text("🏁 Destino: ${t.destination}")
                    Text("📝 Ref: ${t.description}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("⏱️ Salida: ${formatter.format(Date(t.startTimestamp))}")
                    Text("💶 Facturación: ${String.format(Locale.getDefault(), "%.2f", t.billingExpected)} €")
                    Text("🍽️ Dieta: ${if (t.hasDiet) "SI" else "NO"}")
                }
            }

            // KM inicial editable (EN CURSO)
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kilometraje", fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = kmStartText,
                            onValueChange = { kmStartText = it.filter(Char::isDigit) },
                            label = { Text("KM inicial (editable)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            enabled = (t.status == TravelStatus.IN_PROGRESS)
                        )
                        Button(
                            onClick = {
                                val newKmStart = kmStartText.toIntOrNull() ?: -1
                                val ok = viewModel.updateKmStart(newKmStart)
                                errorMessage = if (ok) null else "KM inicial inválido."
                            },
                            enabled = (t.status == TravelStatus.IN_PROGRESS)
                        ) { Text("Guardar") }
                    }

                    OutlinedTextField(
                        value = kmEndText,
                        onValueChange = { kmEndText = it.filter(Char::isDigit) },
                        label = { Text("KM llegada") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // PARADAS (EN CURSO)
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paradas intermedias", fontWeight = FontWeight.Bold)

                    if (t.status == TravelStatus.IN_PROGRESS) {
                        Button(
                            onClick = {
                                stopPlace = ""
                                stopKmText = ""
                                stopWarning = null
                                showAddStop = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("📍 AÑADIR PARADA", fontWeight = FontWeight.Bold) }
                    }

                    if (stops.isEmpty()) {
                        Text("No hay paradas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        stops.forEach { s ->
                            val time = formatter.format(Date(s.timestamp))
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Row(
                                    Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("📍 $time · ${s.place}", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            if (s.kmOdometer != null) "KM: ${s.kmOdometer}" else "KM: —",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(onClick = { openMaps(s.place) }) { Text("🗺️") }

                                    if (t.status == TravelStatus.IN_PROGRESS) {
                                        IconButton(onClick = {
                                            editStopId = s.id
                                            stopPlace = s.place
                                            stopKmText = s.kmOdometer?.toString() ?: ""
                                            stopWarning = null
                                            showEditStop = true
                                        }) { Text("✏️") }

                                        IconButton(onClick = { viewModel.deleteStop(s.id) }) { Text("🗑️") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // HORAS + CIERRE
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = hoursDraftText,
                            onValueChange = { hoursDraftText = it },
                            label = { Text("Horas Draft") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Button(onClick = {
                            val draft = hoursDraftText.replace(',', '.').toDoubleOrNull()
                            val ok = viewModel.updateHoursDraft(draft)
                            errorMessage = if (ok) null else "Horas draft inválidas."
                        }) { Text("Guardar") }
                    }

                    OutlinedTextField(
                        value = hoursCalculatedText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Horas sugeridas (Modelo A)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hoursImputedText,
                        onValueChange = { hoursImputedText = it },
                        label = { Text("Horas imputadas") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    Button(
                        onClick = {
                            val endKm = kmEndText.toIntOrNull() ?: -1
                            val calculated = hoursCalculatedText.replace(',', '.').toDoubleOrNull() ?: -1.0
                            val imputed = hoursImputedText.replace(',', '.').toDoubleOrNull() ?: -1.0

                            if (calculated <= 0.0) {
                                errorMessage = "Introduce KM fin para calcular snapshot."
                                return@Button
                            }

                            val ok = viewModel.closeCurrentTravel(
                                kmEnd = endKm,
                                hoursImputed = imputed,
                                hoursCalculated = calculated
                            )

                            if (ok) onCloseTravel() else errorMessage = "Revisa KM fin y horas."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("CERRAR VIAJE DEFINITIVAMENTE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dialog añadir parada
    if (showAddStop) {
        AlertDialog(
            onDismissRequest = { showAddStop = false },
            confirmButton = {
                TextButton(onClick = {
                    val km = parseStopKm()
                    stopWarning = viewModel.validateStopKm(km) // avisa, no bloquea
                    val ok = viewModel.addStop(stopPlace, km)
                    if (ok) showAddStop = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddStop = false }) { Text("Cancelar") } },
            title = { Text("Nueva parada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = stopPlace, onValueChange = { stopPlace = it }, label = { Text("Lugar/destino") })
                    OutlinedTextField(
                        value = stopKmText,
                        onValueChange = { stopKmText = it.filter(Char::isDigit) },
                        label = { Text("KM odómetro (opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    stopWarning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }

    // Dialog editar parada
    if (showEditStop && editStopId != null) {
        AlertDialog(
            onDismissRequest = { showEditStop = false },
            confirmButton = {
                TextButton(onClick = {
                    val km = parseStopKm()
                    stopWarning = viewModel.validateStopKm(km)
                    val ok = viewModel.updateStop(editStopId!!, stopPlace, km)
                    if (ok) showEditStop = false
                }) { Text("Guardar cambios") }
            },
            dismissButton = { TextButton(onClick = { showEditStop = false }) { Text("Cancelar") } },
            title = { Text("Editar parada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = stopPlace, onValueChange = { stopPlace = it }, label = { Text("Lugar/destino") })
                    OutlinedTextField(
                        value = stopKmText,
                        onValueChange = { stopKmText = it.filter(Char::isDigit) },
                        label = { Text("KM odómetro (opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    stopWarning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }

    // Confirm borrar viaje
    if (showDeleteTravelConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteTravelConfirm = false },
            title = { Text("Eliminar viaje") },
            text = { Text("⚠️ Se eliminará el viaje y todas sus paradas. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTravel(t.id)
                    showDeleteTravelConfirm = false
                    onCloseTravel()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTravelConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}
