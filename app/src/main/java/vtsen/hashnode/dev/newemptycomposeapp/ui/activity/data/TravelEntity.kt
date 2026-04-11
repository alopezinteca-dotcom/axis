package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "travel")
data class TravelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val origin: String,
    val destination: String,
    val description: String,
    val kmStart: Int,
    val kmEnd: Int?,

    /**
     * Facturación prevista del viaje
     */
    val billingExpected: Double,

    val hasDiet: Boolean,
    val hoursImputed: Double?,

    val status: TravelStatus,
    val startTimestamp: Long,
    val endTimestamp: Long?
)
