package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "travel_stops",
    indices = [
        Index(value = ["travelId"]),
        Index(value = ["timestamp"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = TravelEntity::class,
            parentColumns = ["id"],
            childColumns = ["travelId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TravelStopEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val travelId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val place: String,
    val km: Int? = null
)
