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

    @Query("SELECT * FROM travels WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun getCurrentTravel(): Flow<TravelEntity?>

    @Query("SELECT * FROM travels WHERE status = 'CLOSED' ORDER BY startTimestamp DESC")
    fun getClosedTravels(): Flow<List<TravelEntity>>

    @Query("SELECT * FROM travels WHERE id = :id")
    suspend fun getTravelById(id: String): TravelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravel(travel: TravelEntity)

    @Update
    suspend fun updateTravel(travel: TravelEntity)

    @Delete
    suspend fun deleteTravel(travel: TravelEntity)
    
    @Query("DELETE FROM travels")
    suspend fun deleteAllTravels()
}
