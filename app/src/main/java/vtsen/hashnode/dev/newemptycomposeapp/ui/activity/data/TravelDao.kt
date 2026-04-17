package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelDao {

    // ---------- OBSERVABLES ----------
    @Query("SELECT * FROM travels ORDER BY startTimestamp DESC")
    fun observeAllTravels(): Flow<List<TravelEntity>>

    /**
     * TravelStatus suele guardarse como String (con TypeConverter) o como ordinal.
     * En tu app lo estás tratando como enum name (IN_PROGRESS/CLOSED).
     */
    @Query("SELECT * FROM travels WHERE status = :status ORDER BY startTimestamp DESC LIMIT 1")
    fun observeCurrentTravel(status: String = "IN_PROGRESS"): Flow<TravelEntity?>

    @Query("SELECT * FROM travels WHERE status = :status ORDER BY startTimestamp DESC")
    fun observeTravelsByStatus(status: String = "CLOSED"): Flow<List<TravelEntity>>

    // ---------- CRUD ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravel(travel: TravelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTravels(travels: List<TravelEntity>)

    @Query("DELETE FROM travels WHERE id = :id")
    suspend fun deleteTravel(id: String)

    @Query("DELETE FROM travels")
    suspend fun deleteAllTravels()

    // ---------- UPDATES SIMPLES ----------
    @Query("UPDATE travels SET hoursDraft = :hoursDraft WHERE id = :id")
    suspend fun updateHoursDraft(id: String, hoursDraft: Double?)

    @Query("UPDATE travels SET kmStart = :kmStart WHERE id = :id")
    suspend fun updateKmStart(id: String, kmStart: Int)

    @Query("UPDATE travels SET isInvoiced = :isInvoiced WHERE id = :id")
    suspend fun setInvoiced(id: String, isInvoiced: Boolean)

    @Query("UPDATE travels SET endAddress = :endAddress WHERE id = :id")
    suspend fun updateEndAddress(id: String, endAddress: String)

    // ---------- CIERRE DE VIAJE ----------
    @Query(
        """
        UPDATE travels SET
            kmEnd = :kmEnd,
            endTimestamp = :endTimestamp,
            status = :status,
            hoursCalculatedSnapshot = :hoursCalculatedSnapshot,
            hoursImputed = :hoursImputed,
            hoursModified = :hoursModified,
            deltaHours = :deltaHours,
            impactEuroAlejandro = :impactEuroAlejandro,
            snapCosteKmOperativo = :snapCosteKmOperativo,
            snapCosteDietaFija = :snapCosteDietaFija,
            snapPorcBenefExigidoA = :snapPorcBenefExigidoA,
            snapCosteHoraAlejandro = :snapCosteHoraAlejandro,
            snapCosteHoraEmpresaX = :snapCosteHoraEmpresaX,
            snapTarifaObjetivoY = :snapTarifaObjetivoY
        WHERE id = :id
        """
    )
    suspend fun closeTravel(
        id: String,
        kmEnd: Int,
        endTimestamp: Long,
        hoursCalculatedSnapshot: Double,
        hoursImputed: Double,
        hoursModified: Boolean,
        deltaHours: Double,
        impactEuroAlejandro: Double,
        snapCosteKmOperativo: Double,
        snapCosteDietaFija: Double,
        snapPorcBenefExigidoA: Double,
        snapCosteHoraAlejandro: Double,
        snapCosteHoraEmpresaX: Double,
        snapTarifaObjetivoY: Double,
        status: String = "CLOSED"
    )
}
