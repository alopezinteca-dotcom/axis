package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.content.Intent
import android.net.Uri
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

    var kmEnd by rememberSaveable { mutableStateOf("") }
    var hoursDraftText by rememberSaveable { mutableStateOf("") }
    var hoursCalculatedText by rememberSaveable { mutableStateOf("") }
    var hoursImputedText by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // ---- Paradas ----
    var showAddStop by remember { mutableStateOf(false) }
    var showEditStop by remember { mutableStateOf(false) }
    var editStopId by remember { mutableStateOf<String?>(null) }
    var stopPlace by remember { mutableStateOf("") }
    var stopKmText by remember { mutableStateOf("") }
    var stopWarning by remember { mutableStateOf<String?>(null) }

    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    if (travel == null) {
        LaunchedEffect(Unit) { onCloseTravel() }
        return
    }

    val startTimeStr = formatter.format(Date(travel!!.startTimestamp))

    // ===== MODELO A =====
    val COSTE_KM_OPERATIVO = settings.costeKmOperativo
    val COSTE_DIETA_FIJA = settings.costeDietaFija
    val PORC_BENEF_EXIGIDO_A = settings.porcBenefExigidoA
    val COSTE_HORA_ALEJANDRO = settings.costeHoraAlejandro

    // ===== MODELO EMPRESA =====
    val costeEmpresaAnual =
        (settings.salarioBrutoAnual * (1.0 + settings.cargasEmpresa)) +
            settings.overheadAnual
    val horasFacturables = settings.horasAnuales * settings.utilizacion
    val costeHoraEmpresaX = costeEmpresaAnual / horasFacturables
    val tarifaObjetivoY = costeHoraEmpresaX * (1.0 + settings.margenEmpresa)

    LaunchedEffect(travel!!.id) {
        hoursDraftText = travel!!.hoursDraft?.toString() ?: ""
        hoursImputedText =
            travel!!.hoursImputed?.toString()
                ?: travel!!.hoursDraft?.toString().orEmpty()
        hoursCalculatedText = travel!!.hoursCalculatedSnapshot?.toString() ?: ""
    }

    // ===== Cálculo horas sugeridas =====
    val kmEndInt = kmEnd.toIntOrNull()
    val kmDone =
        if (kmEndInt != null) max(0, kmEndInt - travel!!.kmStart) else null

    val suggestedHours: Double? =
        remember(kmDone, travel!!.billingExpected, travel!!.hasDiet, settings) {
            if (kmDone == null) return@remember null

            val facturacion = travel!!.billingExpected
            val costeMaxPermitido = facturacion / (1.0 + PORC_BENEF_EXIGIDO_A)

            val costeKm = kmDone * COSTE_KM_OPERATIVO
            val costeDieta = if (travel!!.hasDiet) COSTE_DIETA_FIJA else 0.0

            val presupuestoHoras = costeMaxPermitido - costeKm - costeDieta
            val horasRaw = presupuestoHoras / COSTE_HORA_ALEJANDRO
            val horasClamped = max(0.0, horasRaw)

            round(horasClamped * 10.0) / 10.0
        }

    LaunchedEffect(suggestedHours) {
        if (suggestedHours != null) {
            hoursCalculatedText =
                String.format(Locale.US, "%.1f", suggestedHours)
        }
    }

    fun openMaps(query: String) {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    fun parseStopKm(): Int? =
        stopKmText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

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

            // ================= CONTEXTO =================
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Contexto", fontWeight = FontWeight.Bold)
                    Text("📍 Origen: ${travel!!.origin}")
                    Text("🏁 Destino: ${travel!!.destination}")
                    Text("⏱️ Salida: $startTimeStr")
                    Text("🚗 KM inicio: ${travel!!.kmStart}")
                    Text("💶 Facturación: ${String.format(Locale.getDefault(), "%.2f", travel!!.billingExpected)} €")
                }
            }

            // ================= PARADAS =================
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paradas intermedias", fontWeight = FontWeight.Bold)

                    if (travel!!.status == TravelStatus.IN_PROGRESS) {
                        Button(
                            onClick = {
                                stopPlace = ""
                                stopKmText = ""
                                stopWarning = null
                                showAddStop = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📍 AÑADIR PARADA", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (stops.isEmpty()) {
                        Text("No hay paradas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        stops.forEach { s ->
                            val time =
                                formatter.format(Date(s.timestamp))

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
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

                                    IconButton(onClick = { openMaps(s.place) }) {
                                        Text("🗺️")
                                    }

                                    if (travel!!.status == TravelStatus.IN_PROGRESS) {
                                        IconButton(onClick = {
                                            editStopId = s.id
                                            stopPlace = s.place
                                            stopKmText = s.kmOdometer?.toString() ?: ""
                                            stopWarning = null
                                            showEditStop = true
                                        }) { Text("✏️") }

                                        IconButton(onClick = { viewModel.deleteStop(s.id) }) {
                                            Text("🗑️")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= CIERRE =================
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    OutlinedTextField(
                        value = kmEnd,
                        onValueChange = { kmEnd = it },
                        label = { Text("KM de llegada") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hoursCalculatedText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Horas sugeridas (Modelo 35%)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hoursImputedText,
                        onValueChange = { hoursImputedText = it },
                        label = { Text("Horas imputadas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = {
                            val ok = viewModel.closeCurrentTravel(
                                kmEnd = kmEnd.toIntOrNull() ?: -1,
                                hoursImputed = hoursImputedText.replace(',', '.').toDoubleOrNull() ?: -1.0,
                                hoursCalculated = hoursCalculatedText.replace(',', '.').toDoubleOrNull() ?: -1.0
                            )
                            if (ok) onCloseTravel()
                            else errorMessage = "Revisa KM fin y horas."
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

    // ================= DIALOGOS =================
    if (showAddStop) {
        AlertDialog(
            onDismissRequest = { showAddStop = false },
            confirmButton = {
                TextButton(onClick = {
                    val km = parseStopKm()
                    stopWarning = viewModel.validateStopKm(km)
                    val ok = viewModel.addStop(stopPlace, km)
                    if (ok) showAddStop = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddStop = false }) { Text("Cancelar") }
            },
            title = { Text("Nueva parada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stopPlace,
                        onValueChange = { stopPlace = it },
                        label = { Text("Lugar / destino") }
                    )
                    OutlinedTextField(
                        value = stopKmText,
                        onValueChange = { stopKmText = it.filter(Char::isDigit) },
                        label = { Text("KM odómetro (opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    stopWarning?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

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
            dismissButton = {
                TextButton(onClick = { showEditStop = false }) { Text("Cancelar") }
            },
            title = { Text("Editar parada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stopPlace,
                        onValueChange = { stopPlace = it },
                        label = { Text("Lugar / destino") }
                    )
                    OutlinedTextField(
                        value = stopKmText,
                        onValueChange = { stopKmText = it.filter(Char::isDigit) },
                        label = { Text("KM odómetro (opcional)") }
                    )
                    stopWarning?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }
}
