package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelDao {

    @Query("SELECT * FROM travels ORDER BY startTimestamp DESC")
    fun getAllTravels(): Flow<List<TravelEntity>>

    // ✅ Mejor que hardcodear strings
    @Query("SELECT * FROM travels WHERE status = :status LIMIT 1")
    fun getCurrentTravel(status: TravelStatus = TravelStatus.IN_PROGRESS): Flow<TravelEntity?>

    @Query("SELECT * FROM travels WHERE status = :status ORDER BY startTimestamp DESC")
    fun getClosedTravels(status: TravelStatus = TravelStatus.CLOSED): Flow<List<TravelEntity>>

    @Query("SELECT * FROM travels WHERE id = :id")
    suspend fun getTravelById(id: String): TravelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravel(travel: TravelEntity)

    @Update
    suspend fun updateTravel(travel: TravelEntity)

    // ✅ Motor “horas en curso”
    @Query("UPDATE travels SET hoursDraft = :hoursDraft WHERE id = :id")
    suspend fun updateHoursDraft(id: String, hoursDraft: Double?)

    @Delete
    suspend fun deleteTravel(travel: TravelEntity)

    @Query("DELETE FROM travels")
    suspend fun deleteAllTravels()
}
