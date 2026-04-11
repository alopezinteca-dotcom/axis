package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "travels")
data class TravelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val origin: String,
    val destination: String,
    val description: String,

    val kmStart: Int,
    val kmEnd: Int?,

    val billingExpected: Double,
    val hasDiet: Boolean,

    // Dato maestro al cerrar
    val hoursImputed: Double?,

    val status: TravelStatus,

    val startTimestamp: Long,
    val endTimestamp: Long?
)
