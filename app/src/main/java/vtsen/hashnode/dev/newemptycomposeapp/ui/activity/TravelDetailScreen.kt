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

    // Paso 3: snapshot automático (solo lectura)
    var hoursCalculatedText by rememberSaveable { mutableStateOf("") }

    // Dato maestro
    var hoursImputedText by rememberSaveable { mutableStateOf("") }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    if (travel == null) {
        LaunchedEffect(Unit) { onCloseTravel() }
        return
    }

    // =========================================================
    // PASO 3 — MODELO A (Alejandro) — los 4 que “cuentan”
    // Dieta por viaje => travel!!.hasDiet
    // % como MARKUP (alineado con tu Excel)
    // =========================================================
    val COSTE_KM_OPERATIVO = 0.19
    val COSTE_DIETA_FIJA = 12.0
    val PORC_BENEF_EXIGIDO_A = 0.35
    val COSTE_HORA_ALEJANDRO = 26.0

    // =========================================================
    // PASO 4 — MODELO EMPRESA (X/Y) — parámetros de tu tabla
    // =========================================================
    val SALARIO_BRUTO_ANUAL = 33000.0
    val CARGAS_EMPRESA = 0.3065
    val OVERHEAD_ANUAL = 12000.0
    val HORAS_ANUALES = 1880.0
    val UTILIZACION = 0.80
    val MARGEN_OBJETIVO_EMPRESA = 0.15

    // X (rentable mínimo) y Y (con margen)
    val costeHoraEmpresaX = remember {
        val costeEmpresaAnual = (SALARIO_BRUTO_ANUAL * (1.0 + CARGAS_EMPRESA)) + OVERHEAD_ANUAL
        val horasFacturables = HORAS_ANUALES * UTILIZACION
        costeEmpresaAnual / horasFacturables
    }
    val tarifaObjetivoY = remember { costeHoraEmpresaX * (1.0 + MARGEN_OBJETIVO_EMPRESA) }

    val startTimeStr = formatter.format(Date(travel!!.startTimestamp))

    // Inicialización
    LaunchedEffect(travel!!.id) {
        hoursDraftText = travel!!.hoursDraft?.toString() ?: ""
        hoursImputedText = travel!!.hoursImputed?.toString()
            ?: (travel!!.hoursDraft?.toString() ?: "")
        hoursCalculatedText = travel!!.hoursCalculatedSnapshot?.toString() ?: ""
    }

    // =========================================================
    // PASO 3 — CÁLCULO AUTOMÁTICO HORAS SUGERIDAS (snapshot)
    // Coste max permitido = Facturación / (1 + 0,35)
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

        round(horasClamped * 10.0) / 10.0
    }

    LaunchedEffect(suggestedHours) {
        if (suggestedHours != null) {
            hoursCalculatedText = String.format(Locale.US, "%.1f", suggestedHours)
        }
    }

    // Semáforo modelo A (imputadas vs sugeridas)
    val imputedValue = hoursImputedText.replace(',', '.').toDoubleOrNull()
    val calculatedValue = hoursCalculatedText.replace(',', '.').toDoubleOrNull()

    val semaforoA: Boolean? = remember(imputedValue, calculatedValue) {
        if (imputedValue == null || calculatedValue == null) null
        else imputedValue <= calculatedValue + 1e-9
    }

    // =========================================================
    // PASO 4 — TARIFA EFECTIVA y SEMÁFORO EMPRESA (X/Y)
    // tarifa_efectiva = facturación / horas_imputadas
    // =========================================================
    val tarifaEfectiva = remember(imputedValue, travel!!.billingExpected) {
        if (imputedValue == null || imputedValue <= 0.0) null
        else travel!!.billingExpected / imputedValue
    }

    val semaforoEmpresaX: Boolean? = remember(tarifaEfectiva) {
        if (tarifaEfectiva == null) null else tarifaEfectiva >= costeHoraEmpresaX
    }

    val semaforoEmpresaY: Boolean? = remember(tarifaEfectiva) {
        if (tarifaEfectiva == null) null else tarifaEfectiva >= tarifaObjetivoY
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

                // IZQUIERDA 40% — Contexto
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
                            "🍽️ Dieta: ${if (travel!!.hasDiet) "SI (12,00 €)" else "NO"}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // DERECHA 60% — Draft + Modelos + Cierre
                Card(
                    modifier = Modifier.weight(0.6f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Imputación y Cierre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        // Draft
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
                            ) { Text("Guardar Draft") }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text("Datos Finales", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)

                        OutlinedTextField(
                            value = kmEnd,
                            onValueChange = { kmEnd = it },
                            label = { Text("Kilómetros de Llegada") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = { Text("Al escribir KM fin, se calculan los modelos automáticamente.") }
                        )

                        // Snapshot (solo lectura)
                        OutlinedTextField(
                            value = hoursCalculatedText,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Horas sugeridas (Modelo A) — snapshot") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            supportingText = { Text("Redondeo 0,1h. Se guarda en Room. Excel NO recalcula.") }
                        )

                        OutlinedTextField(
                            value = hoursImputedText,
                            onValueChange = { hoursImputedText = it },
                            label = { Text("Horas imputadas (definitivas)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        // Semáforo Modelo A
                        val txtA = when (semaforoA) {
                            true -> "🟢 Modelo A OK (imputadas ≤ sugeridas)"
                            false -> "🔴 Modelo A NO (imputadas > sugeridas)"
                            null -> "— Introduce KM fin y horas imputadas"
                        }
                        Text(txtA, fontWeight = FontWeight.SemiBold)

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // PASO 4 — MODELO EMPRESA
                        Text("Modelo Empresa (X/Y)", fontWeight = FontWeight.Bold)

                        Text("X (rentable mínimo): ${String.format(Locale.getDefault(), "%.2f", costeHoraEmpresaX)} €/h")
                        Text("Y (con margen 15%): ${String.format(Locale.getDefault(), "%.2f", tarifaObjetivoY)} €/h")

                        val tarifaTxt = tarifaEfectiva?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "—"
                        Text("Tarifa efectiva (F/h): $tarifaTxt €/h", fontWeight = FontWeight.SemiBold)

                        val txtX = when (semaforoEmpresaX) {
                            true -> "🟢 Cumple X (rentable mínimo)"
                            false -> "🔴 No cumple X"
                            null -> "— Introduce horas imputadas"
                        }
                        Text(txtX, fontWeight = FontWeight.SemiBold)

                        val txtY = when (semaforoEmpresaY) {
                            true -> "🟢 Cumple Y (objetivo con margen)"
                            false -> "🟡 No llega a Y (info secundaria)"
                            null -> "—"
                        }
                        Text(txtY)

                        Spacer(modifier = Modifier.height(8.dp))

                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = {
                                val endKm = kmEnd.toIntOrNull() ?: -1
                                val calculated = hoursCalculatedText.replace(',', '.').toDoubleOrNull() ?: -1.0
                                val imputed = hoursImputedText.replace(',', '.').toDoubleOrNull() ?: -1.0

                                if (calculated <= 0.0) {
                                    errorMessage = "Introduce KM fin para calcular el snapshot."
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
