package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import kotlinx.coroutines.flow.Flow

class TravelRepository(
    private val dao: TravelDao
) {
    val allTravels: Flow<List<TravelEntity>> = dao.getAllTravels()
    val currentTravel: Flow<TravelEntity?> = dao.getCurrentTravel()
    val closedTravels: Flow<List<TravelEntity>> = dao.getClosedTravels()

    suspend fun insertTravel(travel: TravelEntity) = dao.insertTravel(travel)
    suspend fun deleteTravel(id: String) = dao.deleteTravel(id)

    suspend fun updateKmStart(id: String, kmStart: Int) = dao.updateKmStart(id, kmStart)
    suspend fun updateHoursDraft(id: String, hoursDraft: Double?) = dao.updateHoursDraft(id, hoursDraft)
    suspend fun setInvoiced(id: String, isInvoiced: Boolean) = dao.setInvoiced(id, isInvoiced)

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
    ) = dao.closeTravel(
        id,
        kmEnd,
        endTimestamp,
        hoursCalculatedSnapshot,
        hoursImputed,
        hoursModified,
        deltaHours,
        impactEuroAlejandro,
        snapCosteKmOperativo,
        snapCosteDietaFija,
        snapPorcBenefExigidoA,
        snapCosteHoraAlejandro,
        snapCosteHoraEmpresaX,
        snapTarifaObjetivoY
    )

    // ✅ Restore
    suspend fun deleteAll() = dao.deleteAllTravels()
    suspend fun upsertAll(travels: List<TravelEntity>) = dao.upsertTravels(travels)
}
