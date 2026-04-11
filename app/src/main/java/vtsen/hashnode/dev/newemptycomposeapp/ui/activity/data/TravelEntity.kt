package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/* =========================================================
   ENTITY · VIAJE (MODELO DEFINITIVO)
   ========================================================= */

@Entity(tableName = "travels")
data class TravelEntity(

    /* ---------- IDENTIDAD ---------- */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /* ---------- CONTEXTO ---------- */
    val origin: String,
    val destination: String,
    val description: String,

    /* ---------- KILOMETRAJE ---------- */
    val kmStart: Int,
    val kmEnd: Int?,                 // null mientras esté EN CURSO

    /* ---------- ECONOMÍA ---------- */
    val billingExpected: Double,
    val hasDiet: Boolean,

    /* ---------- DECISIÓN HUMANA (PANTALLA 2) ---------- */
    val hoursImputed: Double?,        // null hasta cerrar el viaje

    /* ---------- ESTADO ---------- */
    val status: TravelStatus,

    /* ---------- TIEMPOS ---------- */
    val startTimestamp: Long,
    val endTimestamp: Long?           // null mientras esté EN CURSO
)
