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
    suspend fun upsert(stop: TravelStopEntity)

    @Query("DELETE FROM travel_stops WHERE id = :id")
    suspend fun delete(id: String)

    // ✅ Restore
    @Query("DELETE FROM travel_stops")
    suspend fun deleteAllStops()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStops(stops: List<TravelStopEntity>)
}
