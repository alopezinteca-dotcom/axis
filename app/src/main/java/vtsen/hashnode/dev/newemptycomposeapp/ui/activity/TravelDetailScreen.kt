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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelDetailScreen(
    viewModel: ActivityViewModel,
    onCloseTravel: () -> Unit
) {
    val travel by viewModel.currentTravel.collectAsStateWithLifecycle()

    var kmEnd by rememberSaveable { mutableStateOf("") }
    var hoursDraftText by rememberSaveable { mutableStateOf("") }

    // ✅ Snapshot automático (solo lectura)
    var hoursCalculatedText by rememberSaveable { mutableStateOf("") }

    // ✅ Horas imputadas (dato maestro editable)
    var hoursImputedText by rememberSaveable { mutableStateOf("") }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    if (travel == null) {
        LaunchedEffect(Unit) { onCloseTravel() }
        return
    }

    // =========================================================
    // PARÁMETROS DEL MODELO A (los 4 que “cuentan” en tu hoja)
    // Dieta es por viaje => travel!!.hasDiet
    // =========================================================
    val COSTE_KM_OPERATIVO = 0.19
    val COSTE_DIETA_FIJA = 12.0
    val PORC_BENEF_EXIGIDO_A = 0.35        // 35%
    val COSTE_HORA_ALEJANDRO = 26.0        // €/h

    val startTimeStr = formatter.format(Date(travel!!.startTimestamp))

    // Inicialización campos al entrar
    LaunchedEffect(travel!!.id) {
        hoursDraftText = travel!!.hoursDraft?.toString() ?: ""
        hoursImputedText = travel!!.hoursImputed?.toString()
            ?: (travel!!.hoursDraft?.toString() ?: "")
        hoursCalculatedText = travel!!.hoursCalculatedSnapshot?.toString() ?: ""
    }

    // =========================================================
    // CÁLCULO AUTOMÁTICO HORAS SUGERIDAS (alineado con Excel)
    // Interpretación % como MARKUP (como tu 36,65 -> 42,14 con 15%)
    // COSTE_MAX_PERMITIDO_A = Facturación / (1 + 0,35)
    // =========================================================
    val kmEndInt = kmEnd.toIntOrNull()
    val kmDone = if (kmEndInt != null) max(0, kmEndInt - travel!!.kmStart) else null

    val suggestedHours: Double? = remember(kmDone, travel!!.billingExpected, travel!!.hasDiet) {
        if (kmDone == null) return@remember null

        val facturacion = travel!!.billingExpected
        val costeMaxPermitidoA = facturacion / (1.0 + PORC_BENEF_EXIGIDO_A)

        val costeKm = kmDone * COSTE_KM_OPERATIVO
        val costeDieta = if (travel!!.hasDiet) COSTE_DIETA_FIJA else 0.0

        val presupuestoHorasEuros = costeMaxPermitidoA - costeKm - costeDieta
        val horasRaw = presupuestoHorasEuros / COSTE_HORA_ALEJANDRO

        val horasClamped = max(0.0, horasRaw)

        // ✅ Redondeo al decimal más cercano (0,1 h)
        round(horasClamped * 10.0) / 10.0
    }

    // Refrescar snapshot en UI
    LaunchedEffect(suggestedHours) {
        if (suggestedHours != null) {
            hoursCalculatedText = String.format(Locale.US, "%.1f", suggestedHours)
        }
    }

    // Semáforo (modelo vs imputadas)
    val imputedValue = hoursImputedText.replace(',', '.').toDoubleOrNull()
    val calculatedValue = hoursCalculatedText.replace(',', '.').toDoubleOrNull()

    val isCompliant: Boolean? = remember(imputedValue, calculatedValue) {
        if (imputedValue == null || calculatedValue == null) null
        else imputedValue <= calculatedValue + 1e-9
    }

    // Breakdown para mostrar (modelo hiperrealista A)
    val modelDetails = remember(kmDone, suggestedHours) {
        if (kmDone == null || suggestedHours == null) null else {
            val facturacion = travel!!.billingExpected
            val costeMaxPermitidoA = facturacion / (1.0 + PORC_BENEF_EXIGIDO_A)

            val costeKm = kmDone * COSTE_KM_OPERATIVO
            val costeDieta = if (travel!!.hasDiet) COSTE_DIETA_FIJA else 0.0
            val presupuestoHorasEuros = costeMaxPermitidoA - costeKm - costeDieta

            ModelBreakdown(
                kmDone = kmDone,
                costeKm = costeKm,
                costeDieta = costeDieta,
                costeMaxPermitidoA = costeMaxPermitidoA,
                presupuestoHorasEuros = presupuestoHorasEuros,
                horasSugeridas = suggestedHours
            )
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // IZQUIERDA 40%: Contexto
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
                        Text(
                            "📝 Ref: ${travel!!.description}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Divider()
                        Text("⏱️ Hora Salida: $startTimeStr", style = MaterialTheme.typography.bodyLarge)
                        Text("🚗 KM Iniciales: ${travel!!.kmStart}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "💶 Facturación: ${String.format(Locale.getDefault(), "%.2f", travel!!.billingExpected)} €",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "🍽️ Dieta: ${if (travel!!.hasDiet) "SI (${String.format(Locale.getDefault(), "%.2f", COSTE_DIETA_FIJA)} €)" else "NO"}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // DERECHA 60%: Draft + Modelo + Cierre
                Card(
                    modifier = Modifier.weight(0.6f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Imputación y Cierre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        // FASE 1: Draft (punto intermedio)
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
                                supportingText = { Text("Punto intermedio (en ruta).") }
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

                        Text("Datos Finales", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)

                        OutlinedTextField(
                            value = kmEnd,
                            onValueChange = { kmEnd = it },
                            label = { Text("Kilómetros de Llegada") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = { Text("Al escribir KM fin, se calcula el modelo automáticamente.") }
                        )

                        // ✅ Snapshot automático y solo lectura
                        OutlinedTextField(
                            value = hoursCalculatedText,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Horas sugeridas (modelo) — snapshot") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            supportingText = { Text("Redondeo 0,1 h. Se guarda en Room. Excel NO recalcula.") }
                        )

                        OutlinedTextField(
                            value = hoursImputedText,
                            onValueChange = { hoursImputedText = it },
                            label = { Text("Horas imputadas (definitivas)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            supportingText = { Text("Tú decides el valor final.") }
                        )

                        // Bloque de modelo A
                        modelDetails?.let { m ->
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Modelo A (Alejandro)", fontWeight = FontWeight.Bold)

                            Text("COSTE_KM_OPERATIVO: 0,19 €/km")
                            Text("COSTE_DIETA_FIJA: 12,00 €")
                            Text("PORC_BENEF_EXIGIDO_A: 35%")
                            Text("COSTE_HORA_ALEJANDRO: 26,00 €/h")

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("KM realizados: ${m.kmDone} km")
                            Text("Coste KM: ${String.format(Locale.getDefault(), "%.2f", m.costeKm)} €")
                            Text("Coste dieta: ${String.format(Locale.getDefault(), "%.2f", m.costeDieta)} €")
                            Text("Coste máx permitido (A): ${String.format(Locale.getDefault(), "%.2f", m.costeMaxPermitidoA)} €")
                            Text("Presupuesto horas (€): ${String.format(Locale.getDefault(), "%.2f", m.presupuestoHorasEuros)} €")
                            Text("Horas sugeridas (0,1h): ${String.format(Locale.getDefault(), "%.1f", m.horasSugeridas)} h")

                            Spacer(modifier = Modifier.height(6.dp))
                            val semaforoText = when (isCompliant) {
                                true -> "🟢 Cumple el modelo (imputadas ≤ sugeridas)"
                                false -> "🔴 NO cumple (imputadas > sugeridas)"
                                null -> "— Introduce horas imputadas para comparar"
                            }
                            Text(semaforoText, fontWeight = FontWeight.SemiBold)
                        }

                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = {
                                val endKm = kmEnd.toIntOrNull() ?: -1
                                val calculated = hoursCalculatedText.replace(',', '.').toDoubleOrNull() ?: -1.0
                                val imputed = hoursImputedText.replace(',', '.').toDoubleOrNull() ?: -1.0

                                if (calculated <= 0.0) {
                                    errorMessage = "Introduce KM fin para calcular horas sugeridas."
                                    return@Button
                                }

                                val ok = viewModel.closeCurrentTravel(
                                    kmEnd = endKm,
                                    hoursImputed = imputed,
                                    hoursCalculated = calculated
                                )

                                if (ok) {
                                    errorMessage = null
                                    onCloseTravel()
                                } else {
                                    errorMessage = "Revisa KM fin y horas (válidos y > 0)."
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

private data class ModelBreakdown(
    val kmDone: Int,
    val costeKm: Double,
    val costeDieta: Double,
    val costeMaxPermitidoA: Double,
    val presupuestoHorasEuros: Double,
    val horasSugeridas: Double
)
