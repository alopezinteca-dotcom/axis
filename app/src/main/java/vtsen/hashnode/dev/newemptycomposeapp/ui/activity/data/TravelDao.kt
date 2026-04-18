package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelDao {

    // ===== Flows que tu TravelRepository YA está usando =====
    @Query("SELECT * FROM travels ORDER BY startTimestamp DESC")
    fun getAllTravels(): Flow<List<TravelEntity>>

    @Query("SELECT * FROM travels WHERE status = 'IN_PROGRESS' ORDER BY startTimestamp DESC LIMIT 1")
    fun getCurrentTravel(): Flow<TravelEntity?>

    @Query("SELECT * FROM travels WHERE status = 'CLOSED' ORDER BY startTimestamp DESC")
    fun getClosedTravels(): Flow<List<TravelEntity>>

    // ===== Insert / Update base =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravel(travel: TravelEntity)

    @Query("DELETE FROM travels WHERE id = :id")
    suspend fun deleteTravel(id: String)

    @Query("UPDATE travels SET kmStart = :kmStart WHERE id = :id")
    suspend fun updateKmStart(id: String, kmStart: Int)

    @Query("UPDATE travels SET hoursDraft = :hoursDraft WHERE id = :id")
    suspend fun updateHoursDraft(id: String, hoursDraft: Double?)

    @Query("UPDATE travels SET isInvoiced = :isInvoiced WHERE id = :id")
    suspend fun setInvoiced(id: String, isInvoiced: Boolean)

    // ===== Cierre de viaje =====
    @Query(
        """
        UPDATE travels SET
            kmEnd = :kmEnd,
            endTimestamp = :endTimestamp,
            status = 'CLOSED',
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
        snapTarifaObjetivoY: Double
    )

    // ===== Restore (wipe + upsert) =====
    @Query("DELETE FROM travels")
    suspend fun deleteAllTravels()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTravels(travels: List<TravelEntity>)
}
