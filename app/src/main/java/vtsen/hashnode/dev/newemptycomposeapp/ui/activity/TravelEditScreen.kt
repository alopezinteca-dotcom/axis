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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStatus
import vtsen.hashnode.dev.newemptycomposeapp.ui.settings.SettingsViewModel

/**
 * TravelEditScreen
 *
 * Pantalla de EDICIÓN TOTAL de un viaje cerrado.
 * Se divide en 3 bloques:
 *   - Bloque 1: RUTA (origen, destino, descripción, fechas, km, dieta, dirección llegada)
 *   - Bloque 2: ECONÓMICO (facturación esperada, facturado S/N)
 *   - Bloque 3: TELEMETRÍA (horas calculadas, horas imputadas, recálculo de KPIs al guardar)
 *
 * Al guardar recalcula: deltaHours e impactEuroAlejandro usando snapCosteHoraAlejandro
 * (o el valor actual de Settings si no hay snapshot).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelEditScreen(
    viewModel: ActivityViewModel,
    settingsViewModel: SettingsViewModel,
    onDone: () -> Unit
) {
    val selectedId by viewModel.selectedTravelId.collectAsStateWithLifecycle()
    val allTravels by viewModel.allTravels.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    // Busca el viaje en la lista; si no existe sale
    val travel: TravelEntity? = remember(selectedId, allTravels) {
        selectedId?.let { id -> allTravels.firstOrNull { it.id == id } }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val dtFmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    // --- BLOQUE 1: RUTA ---
    var origin by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var kmStartText by rememberSaveable { mutableStateOf("") }
    var kmEndText by rememberSaveable { mutableStateOf("") }
    var hasDiet by rememberSaveable { mutableStateOf(false) }
    var endAddress by rememberSaveable { mutableStateOf("") }

    // --- BLOQUE 2: ECONÓMICO ---
    var billingText by rememberSaveable { mutableStateOf("") }
    var isInvoiced by rememberSaveable { mutableStateOf(false) }

    // --- BLOQUE 3: TELEMETRÍA ---
    var hoursCalcText by rememberSaveable { mutableStateOf("") }
    var hoursImputedText by rememberSaveable { mutableStateOf("") }

    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    // Inicializa campos cuando el viaje carga
    LaunchedEffect(travel?.id) {
        val t = travel ?: return@LaunchedEffect
        origin          = t.origin
        destination     = t.destination
        description     = t.description
        kmStartText     = t.kmStart.toString()
        kmEndText       = t.kmEnd?.toString() ?: ""
        hasDiet         = t.hasDiet
        endAddress      = t.endAddress
        billingText     = String.format(Locale.US, "%.2f", t.billingExpected)
        isInvoiced      = t.isInvoiced
        hoursCalcText   = t.hoursCalculatedSnapshot?.let { String.format(Locale.US, "%.2f", it) } ?: ""
        hoursImputedText = t.hoursImputed?.let { String.format(Locale.US, "%.2f", it) } ?: ""
    }

    // Si no hay viaje seleccionado, vuelve
    if (travel == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val t = travel

    // Determina el coste/hora a usar para recalcular impacto
    val costeHoraAlejandro = t.snapCosteHoraAlejandro ?: settings.costeHoraAlejandro

    fun saveAndExit() {
        val kmStart = kmStartText.trim().toIntOrNull()
        val kmEnd   = kmEndText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        val billing = billingText.replace(',', '.').toDoubleOrNull()
        val hoursCalc    = hoursCalcText.replace(',', '.').toDoubleOrNull()
        val hoursImputed = hoursImputedText.replace(',', '.').toDoubleOrNull()

        if (origin.isBlank()) { errorMsg = "El origen no puede estar vacío."; return }
        if (destination.isBlank()) { errorMsg = "El destino no puede estar vacío."; return }
        if (kmStart == null || kmStart <= 0) { errorMsg = "KM de inicio inválido."; return }
        if (kmEnd != null && kmEnd < kmStart) { errorMsg = "KM de fin no puede ser menor que KM de inicio."; return }
        if (billing == null || billing < 0) { errorMsg = "Facturación inválida."; return }

        // ✅ Recalcula deltaHours e impactEuroAlejandro al guardar
        val delta: Double? = if (hoursImputed != null && hoursCalc != null) hoursImputed - hoursCalc else null
        val impact: Double? = delta?.let { it * costeHoraAlejandro }
        val hoursModified = delta?.let { abs(it) > 0.01 } ?: t.hoursModified

        val updated = t.copy(
            origin          = origin.trim(),
            destination     = destination.trim(),
            description     = description.trim(),
            kmStart         = kmStart,
            kmEnd           = kmEnd ?: t.kmEnd,
            hasDiet         = hasDiet,
            endAddress      = endAddress.trim(),
            billingExpected = billing,
            isInvoiced      = isInvoiced,
            hoursCalculatedSnapshot = hoursCalc ?: t.hoursCalculatedSnapshot,
            hoursImputed    = hoursImputed ?: t.hoursImputed,
            deltaHours      = delta ?: t.deltaHours,
            impactEuroAlejandro = impact ?: t.impactEuroAlejandro,
            hoursModified   = hoursModified
        )
        viewModel.updateTravel(updated)
        coroutineScope.launch {
            snackbarHostState.showSnackbar("✅ Viaje guardado")
        }
        onDone()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Editar Viaje") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                navigationIcon = {
                    TextButton(onClick = onDone) { Text("← Volver") }
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

            // ─── Info de lectura ───────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ID: ${t.id.take(8)}…", style = MaterialTheme.typography.labelSmall)
                    Text("Inicio: ${dtFmt.format(Date(t.startTimestamp))}", style = MaterialTheme.typography.bodySmall)
                    t.endTimestamp?.let { Text("Fin: ${dtFmt.format(Date(it))}", style = MaterialTheme.typography.bodySmall) }
                    Text(
                        "Estado: ${if (t.status == TravelStatus.CLOSED) "CERRADO ✅" else "EN CURSO 🟢"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ─── BLOQUE 1: RUTA ───────────────────────────────
            EditBlock(title = "🗺️ Bloque 1 · Ruta") {
                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Origen") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destino") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción / Referencia") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = kmStartText,
                        onValueChange = { kmStartText = it.filter(Char::isDigit) },
                        label = { Text("KM inicio") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = kmEndText,
                        onValueChange = { kmEndText = it.filter(Char::isDigit) },
                        label = { Text("KM fin") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                // Muestra km totales en tiempo real
                val kmTotal = run {
                    val s = kmStartText.toIntOrNull()
                    val e = kmEndText.toIntOrNull()
                    if (s != null && e != null && e >= s) e - s else null
                }
                if (kmTotal != null) {
                    Text("📏 KM totales: $kmTotal km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                OutlinedTextField(
                    value = endAddress,
                    onValueChange = { endAddress = it },
                    label = { Text("📍 Dirección llegada (calle, ciudad)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("¿Lleva dieta?")
                    Switch(checked = hasDiet, onCheckedChange = { hasDiet = it })
                    Text(if (hasDiet) "SÍ" else "NO", fontWeight = FontWeight.SemiBold)
                }
            }

            // ─── BLOQUE 2: ECONÓMICO ──────────────────────────
            EditBlock(title = "💶 Bloque 2 · Económico") {
                OutlinedTextField(
                    value = billingText,
                    onValueChange = { billingText = it },
                    label = { Text("Facturación esperada (€)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = isInvoiced,
                        onCheckedChange = { isInvoiced = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        if (isInvoiced) "✅ Facturado" else "⬜ Pendiente de facturar",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ─── BLOQUE 3: TELEMETRÍA ─────────────────────────
            EditBlock(title = "⏱️ Bloque 3 · Telemetría de horas") {
                OutlinedTextField(
                    value = hoursCalcText,
                    onValueChange = { hoursCalcText = it },
                    label = { Text("Horas calculadas (snapshot Modelo A)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = hoursImputedText,
                    onValueChange = { hoursImputedText = it },
                    label = { Text("Horas imputadas") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                // Recálculo en tiempo real de KPIs
                val calc    = hoursCalcText.replace(',', '.').toDoubleOrNull()
                val imputed = hoursImputedText.replace(',', '.').toDoubleOrNull()
                if (calc != null && imputed != null) {
                    val delta  = imputed - calc
                    val impact = delta * costeHoraAlejandro
                    HorizontalDivider()
                    Text("Recálculo en tiempo real", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    KpiPreviewRow("ΔHoras (imputadas − calculadas)", String.format(Locale.getDefault(), "%.2f h", delta))
                    KpiPreviewRow(
                        "Impacto € Alejandro (Δh × ${String.format(Locale.getDefault(), "%.2f", costeHoraAlejandro)} €/h)",
                        String.format(Locale.getDefault(), "%.2f €", impact)
                    )
                    KpiPreviewRow("¿Horas modificadas?", if (abs(delta) > 0.01) "SÍ ⚠️" else "NO ✅")

                    val snapLabel = if (t.snapCosteHoraAlejandro != null) "(snap guardado)" else "(Settings actual)"
                    Text(
                        "Usando coste/hora $snapLabel: ${String.format(Locale.getDefault(), "%.2f €/h", costeHoraAlejandro)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ─── Error ────────────────────────────────────────
            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }

            // ─── Guardar ──────────────────────────────────────
            Button(
                onClick = { saveAndExit() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("💾 GUARDAR CAMBIOS", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/* ─── Helpers de UI ───────────────────────────────────────────────── */

@Composable
private fun EditBlock(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun KpiPreviewRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}
