package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TravelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravel(travel: TravelEntity)

    @Query(
        """
        SELECT * FROM travels
        WHERE status = :status
        LIMIT 1
        """
    )
    suspend fun getTravelInProgress(
        status: TravelStatus = TravelStatus.IN_PROGRESS
    ): TravelEntity?

    @Query(
        """
        UPDATE travels
        SET
            kmEnd = :kmEnd,
            hoursImputed = :hoursImputed,
            status = :newStatus,
            endTimestamp = :endTimestamp
        WHERE id = :travelId
        """
    )
    suspend fun closeTravel(
        travelId: Long,
        kmEnd: Int,
        hoursImputed: Double,
        newStatus: TravelStatus = TravelStatus.CLOSED,
        endTimestamp: Long
    )

    @Query(
        """
        SELECT * FROM travels
        WHERE status = :status
        ORDER BY startTimestamp ASC
        """
    )
    suspend fun getClosedTravels(
        status: TravelStatus = TravelStatus.CLOSED
    ): List<TravelEntity>
}
