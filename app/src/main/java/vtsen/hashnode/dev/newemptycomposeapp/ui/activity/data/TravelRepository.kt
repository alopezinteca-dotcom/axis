package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import kotlinx.coroutines.flow.Flow

class TravelRepository(
    private val travelDao: TravelDao
) {
    val allTravels: Flow<List<TravelEntity>> = travelDao.getAllTravels()
    val currentTravel: Flow<TravelEntity?> = travelDao.getCurrentTravel()
    val closedTravels: Flow<List<TravelEntity>> = travelDao.getClosedTravels()

    suspend fun insertTravel(travel: TravelEntity) {
        travelDao.insertTravel(travel)
    }

    suspend fun updateHoursDraft(id: String, hoursDraft: Double?) {
        travelDao.updateHoursDraft(id, hoursDraft)
    }

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
    ) {
        travelDao.closeTravel(
            id = id,
            kmEnd = kmEnd,
            endTimestamp = endTimestamp,
            hoursCalculatedSnapshot = hoursCalculatedSnapshot,
            hoursImputed = hoursImputed,
            hoursModified = hoursModified,
            deltaHours = deltaHours,
            impactEuroAlejandro = impactEuroAlejandro,
            snapCosteKmOperativo = snapCosteKmOperativo,
            snapCosteDietaFija = snapCosteDietaFija,
            snapPorcBenefExigidoA = snapPorcBenefExigidoA,
            snapCosteHoraAlejandro = snapCosteHoraAlejandro,
            snapCosteHoraEmpresaX = snapCosteHoraEmpresaX,
            snapTarifaObjetivoY = snapTarifaObjetivoY
        )
    }
}
