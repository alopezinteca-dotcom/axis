package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "travels")
data class TravelEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    val startTimestamp: Long = System.currentTimeMillis(),
    val endTimestamp: Long? = null,

    val origin: String = "",
    val destination: String = "",
    val description: String = "",

    val kmStart: Int = 0,
    val kmEnd: Int? = null,

    val hasDiet: Boolean = false,
    val billingExpected: Double = 0.0,

    val status: TravelStatus = TravelStatus.IN_PROGRESS,

    // Horas
    val hoursDraft: Double? = null,
    val hoursCalculatedSnapshot: Double? = null,
    val hoursImputed: Double? = null,

    // Flags / impacto
    val hoursModified: Boolean = false,
    val deltaHours: Double? = null,
    val impactEuroAlejandro: Double? = null,

    // Snapshots (si los usas en cierre/edición)
    val snapCosteKmOperativo: Double? = null,
    val snapCosteDietaFija: Double? = null,
    val snapPorcBenefExigidoA: Double? = null,
    val snapCosteHoraAlejandro: Double? = null,
    val snapCosteHoraEmpresaX: Double? = null,
    val snapTarifaObjetivoY: Double? = null,

    // Facturado
    val isInvoiced: Boolean = false,

    // ✅ NUEVO: Dirección llegada (calle/ciudad) sin guardar coordenadas
    val endAddress: String = ""
)
