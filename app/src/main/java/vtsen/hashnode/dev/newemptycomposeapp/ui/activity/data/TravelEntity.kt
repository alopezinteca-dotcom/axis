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

    // ✅ NUEVO (PASO 6): Snapshots de parámetros usados (Excel NO recalcula)
    val snapCosteKmOperativo: Double? = null,        // 0.19
    val snapCosteDietaFija: Double? = null,          // 12.00
    val snapPorcBenefExigidoA: Double? = null,       // 0.35
    val snapCosteHoraAlejandro: Double? = null,      // 26.00
    val snapCosteHoraEmpresaX: Double? = null,       // 36.65...
    val snapTarifaObjetivoY: Double? = null          // 42.14...
)
