package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelStopDao {

    @Query("SELECT * FROM travel_stops WHERE travelId = :travelId ORDER BY timestamp ASC")
    fun getStopsForTravel(travelId: String): Flow<List<TravelStopEntity>>

    @Query("SELECT * FROM travel_stops WHERE timestamp BETWEEN :fromMillis AND :toMillis ORDER BY timestamp ASC")
    fun getStopsInRange(fromMillis: Long, toMillis: Long): Flow<List<TravelStopEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStop(stop: TravelStopEntity)

    @Query("DELETE FROM travel_stops WHERE id = :stopId")
    suspend fun deleteStop(stopId: String)
}
