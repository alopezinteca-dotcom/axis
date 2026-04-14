package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelDao {

    @Query("SELECT * FROM travels ORDER BY startTimestamp DESC")
    fun getAllTravels(): Flow<List<TravelEntity>>

    @Query("SELECT * FROM travels WHERE status = :status LIMIT 1")
    fun getCurrentTravel(status: TravelStatus = TravelStatus.IN_PROGRESS): Flow<TravelEntity?>

    @Query("SELECT * FROM travels WHERE status = :status ORDER BY startTimestamp DESC")
    fun getClosedTravels(status: TravelStatus = TravelStatus.CLOSED): Flow<List<TravelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravel(travel: TravelEntity)

    @Query("UPDATE travels SET hoursDraft = :hoursDraft WHERE id = :id")
    suspend fun updateHoursDraft(id: String, hoursDraft: Double?)

    @Query("UPDATE travels SET isInvoiced = :isInvoiced WHERE id = :id")
    suspend fun setInvoiced(id: String, isInvoiced: Boolean)

    // ✅ NUEVO: borrar viaje (paradas se borran por CASCADE)
    @Query("DELETE FROM travels WHERE id = :id")
    suspend fun deleteTravel(id: String)

    // ✅ NUEVO: editar KM inicial
    @Query("UPDATE travels SET kmStart = :kmStart WHERE id = :id")
    suspend fun updateKmStart(id: String, kmStart: Int)

    @Query(
        """
        UPDATE travels
        SET
            kmEnd = :kmEnd,
            status = :newStatus,
            endTimestamp = :endTimestamp,
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
        newStatus: TravelStatus = TravelStatus.CLOSED,
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
}
