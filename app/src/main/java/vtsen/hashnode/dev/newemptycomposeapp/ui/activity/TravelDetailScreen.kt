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
import kotlin.math.max
import kotlin.math.round
import vtsen.hashnode.dev.newemptycomposeapp.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelDetailScreen(
    viewModel: ActivityViewModel,
    settingsViewModel: SettingsViewModel,
    onCloseTravel: () -> Unit
) {
    val travel by viewModel.currentTravel.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    var kmEnd by rememberSaveable { mutableStateOf("") }
    var hoursDraftText by rememberSaveable { mutableStateOf("") }
    var hoursCalculatedText by rememberSaveable { mutableStateOf("") } // snapshot (readonly)
    var hoursImputedText by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    if (travel == null) {
        LaunchedEffect(Unit) { onCloseTravel() }
        return
    }

    val startTimeStr = formatter.format(Date(travel!!.startTimestamp))

    // --- Modelo A desde settings ---
    val COSTE_KM_OPERATIVO = settings.costeKmOperativo
    val COSTE_DIETA_FIJA = settings.costeDietaFija
    val PORC_BENEF_EXIGIDO_A = settings.porcBenefExigidoA
    val COSTE_HORA_ALEJANDRO = settings.costeHoraAlejandro

    // --- Modelo Empresa X/Y desde settings ---
    val costeEmpresaAnual = (settings.salarioBrutoAnual * (1.0 + settings.cargasEmpresa)) + settings.overheadAnual
    val horasFacturables = settings.horasAnuales * settings.utilizacion
    val costeHoraEmpresaX = costeEmpresaAnual / horasFacturables
    val tarifaObjetivoY = costeHoraEmpresaX * (1.0 + settings.margenEmpresa)

    LaunchedEffect(travel!!.id) {
        hoursDraftText = travel!!.hoursDraft?.toString() ?: ""
        hoursImputedText = travel!!.hoursImputed?.toString() ?: (travel!!.hoursDraft?.toString() ?: "")
        hoursCalculatedText = travel!!.hoursCalculatedSnapshot?.toString() ?: ""
    }

    // --- Auto-cálculo horas sugeridas (Modelo A) ---
    val kmEndInt = kmEnd.toIntOrNull()
    val kmDone = if (kmEndInt != null) max(0, kmEndInt - travel!!.kmStart) else null

    val suggestedHours: Double? = remember(kmDone, travel!!.billingExpected, travel!!.hasDiet, settings) {
        if (kmDone == null) return@remember null

        val facturacion = travel!!.billingExpected
        // % como MARKUP
        val costeMaxPermitidoA = facturacion / (1.0 + PORC_BENEF_EXIGIDO_A)

        val costeKm = kmDone * COSTE_KM_OPERATIVO
        val costeDieta = if (travel!!.hasDiet) COSTE_DIETA_FIJA else 0.0

        val presupuestoHorasEuros = costeMaxPermitidoA - costeKm - costeDieta
        val horasRaw = presupuestoHorasEuros / COSTE_HORA_ALEJANDRO
        val horasClamped = max(0.0, horasRaw)

        round(horasClamped * 10.0) / 10.0
    }

    LaunchedEffect(suggestedHours) {
        if (suggestedHours != null) {
            hoursCalculatedText = String.format(Locale.US, "%.1f", suggestedHours)
        }
    }

    val imputedValue = hoursImputedText.replace(',', '.').toDoubleOrNull()
    val calculatedValue = hoursCalculatedText.replace(',', '.').toDoubleOrNull()

    val tarifaEfectiva = remember(imputedValue, travel!!.billingExpected) {
        if (imputedValue == null || imputedValue <= 0.0) null else travel!!.billingExpected / imputedValue
    }

    val semaforoEmpresaX = tarifaEfectiva?.let { it >= costeHoraEmpresaX }
    val semaforoEmpresaY = tarifaEfectiva?.let { it >= tarifaObjetivoY }

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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                Card(modifier = Modifier.weight(0.4f), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Contexto Actual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Divider()
                        Text("📍 Origen: ${travel!!.origin}")
                        Text("🏁 Destino: ${travel!!.destination}")
                        Text("📝 Ref: ${travel!!.description}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Divider()
                        Text("⏱️ Hora Salida: $startTimeStr")
                        Text("🚗 KM Iniciales: ${travel!!.kmStart}")
                        Text("💶 Facturación: ${String.format(Locale.getDefault(), "%.2f", travel!!.billingExpected)} €")
                        Text("🍽️ Dieta: ${if (travel!!.hasDiet) "SI" else "NO"}")
                    }
                }

                Card(modifier = Modifier.weight(0.6f), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Imputación y Cierre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = hoursDraftText,
                                onValueChange = { hoursDraftText = it },
                                label = { Text("Horas Provisionales (Draft)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            Button(onClick = {
                                val draft = hoursDraftText.replace(',', '.').toDoubleOrNull()
                                val ok = viewModel.updateHoursDraft(draft)
                                errorMessage = if (ok) null else "Horas provisionales inválidas."
                            }) { Text("Guardar Draft") }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        OutlinedTextField(
                            value = kmEnd,
                            onValueChange = { kmEnd = it },
                            label = { Text("Kilómetros de Llegada") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = hoursCalculatedText,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Horas sugeridas (Modelo A) — snapshot") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = hoursImputedText,
                            onValueChange = { hoursImputedText = it },
                            label = { Text("Horas imputadas (definitivas)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text("Empresa (X/Y)", fontWeight = FontWeight.Bold)
                        Text("X: ${String.format(Locale.getDefault(), "%.2f", costeHoraEmpresaX)} €/h")
                        Text("Y: ${String.format(Locale.getDefault(), "%.2f", tarifaObjetivoY)} €/h")
                        Text("Tarifa efectiva: ${tarifaEfectiva?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "—"} €/h")

                        Text(
                            when (semaforoEmpresaX) {
                                true -> "🟢 Cumple X"
                                false -> "🔴 No cumple X"
                                null -> "—"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            when (semaforoEmpresaY) {
                                true -> "🟢 Cumple Y"
                                false -> "🟡 No llega a Y"
                                null -> "—"
                            }
                        )

                        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                        Button(
                            onClick = {
                                val endKm = kmEnd.toIntOrNull() ?: -1
                                val calculated = hoursCalculatedText.replace(',', '.').toDoubleOrNull() ?: -1.0
                                val imputed = hoursImputedText.replace(',', '.').toDoubleOrNull() ?: -1.0

                                if (calculated <= 0.0) {
                                    errorMessage = "Introduce KM fin para calcular el snapshot."
                                    return@Button
                                }

                                val snaps = ParamSnapshots(
                                    costeKmOperativo = COSTE_KM_OPERATIVO,
                                    costeDietaFija = COSTE_DIETA_FIJA,
                                    porcBenefExigidoA = PORC_BENEF_EXIGIDO_A,
                                    costeHoraAlejandro = COSTE_HORA_ALEJANDRO,
                                    costeHoraEmpresaX = costeHoraEmpresaX,
                                    tarifaObjetivoY = tarifaObjetivoY
                                )

                                val ok = viewModel.closeCurrentTravelWithSnapshots(
                                    kmEnd = endKm,
                                    hoursImputed = imputed,
                                    hoursCalculated = calculated,
                                    snaps = snaps
                                )

                                if (ok) {
                                    errorMessage = null
                                    onCloseTravel()
                                } else {
                                    errorMessage = "Revisa KM fin y horas (válidos y > 0)."
                                }
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
    }
}
