package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "travels")
data class TravelEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    val origin: String,
    val destination: String,
    val description: String,

    val kmStart: Int,
    val kmEnd: Int? = null,

    val billingExpected: Double,
    val hasDiet: Boolean,

    val status: TravelStatus = TravelStatus.IN_PROGRESS,
    val startTimestamp: Long = System.currentTimeMillis(),
    val endTimestamp: Long? = null,

    // Motor de horas y snapshots financieros
    val hoursDraft: Double? = null,
    val hoursCalculatedSnapshot: Double? = null,
    val hoursImputed: Double? = null,
    val hoursModified: Boolean = false,
    val deltaHours: Double? = null,
    val impactEuroAlejandro: Double? = null,

    // Snapshots de parámetros usados (Excel NO recalcula)
    val snapCosteKmOperativo: Double? = null,
    val snapCosteDietaFija: Double? = null,
    val snapPorcBenefExigidoA: Double? = null,
    val snapCosteHoraAlejandro: Double? = null,
    val snapCosteHoraEmpresaX: Double? = null,
    val snapTarifaObjetivoY: Double? = null,

    // ✅ NUEVO (V2): Facturación manual
    val isInvoiced: Boolean = false
)
