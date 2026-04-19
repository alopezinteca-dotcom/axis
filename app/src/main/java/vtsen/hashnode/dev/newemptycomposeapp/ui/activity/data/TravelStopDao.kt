package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para las paradas de los viajes.
 * Incluye soporte para el timeline reactivo y operaciones de respaldo masivo.
 */
@Dao
interface TravelStopDao {

    /**
     * Obtiene las paradas vinculadas a un viaje específico (reactivo).
     */
    @Query("SELECT * FROM travel_stops WHERE travelId = :travelId ORDER BY timestamp ASC")
    fun getStopsForTravel(travelId: String): Flow<List<TravelStopEntity>>

    /**
     * Obtiene paradas dentro de un rango de tiempo para el Timeline (reactivo).
     */
    @Query("SELECT * FROM travel_stops WHERE timestamp BETWEEN :fromMillis AND :toMillis ORDER BY timestamp ASC")
    fun getStopsInRange(fromMillis: Long, toMillis: Long): Flow<List<TravelStopEntity>>

    /**
     * Obtiene todas las paradas registradas (reactivo).
     */
    @Query("SELECT * FROM travel_stops ORDER BY timestamp ASC")
    fun getAllStops(): Flow<List<TravelStopEntity>>

    /**
     * ✅ NUEVO (Snapshot): Obtiene todas las paradas una sola vez.
     * Imprescindible para generar el archivo maestro de exportación.
     */
    @Query("SELECT * FROM travel_stops ORDER BY timestamp ASC")
    suspend fun getAllStopsOnce(): List<TravelStopEntity>

    /**
     * Inserta o actualiza una parada individual.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stop: TravelStopEntity)

    /**
     * Inserta o actualiza una lista de paradas. 
     * Fundamental para el proceso de restauración desde CSV.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStops(stops: List<TravelStopEntity>)

    /**
     * Elimina una parada específica por su ID.
     */
    @Query("DELETE FROM travel_stops WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * ✅ RESTORE: Elimina todas las paradas de la base de datos.
     * Se usa en la estrategia REPLACE_ALL antes de importar un backup.
     */
    @Query("DELETE FROM travel_stops")
    suspend fun deleteAllStops()
}
