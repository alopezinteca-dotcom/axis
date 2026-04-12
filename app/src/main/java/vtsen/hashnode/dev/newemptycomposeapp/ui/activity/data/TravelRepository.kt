package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import kotlinx.coroutines.flow.Flow

class TravelRepository(private val travelDao: TravelDao) {

    val allTravels: Flow<List<TravelEntity>> = travelDao.getAllTravels()
    val currentTravel: Flow<TravelEntity?> = travelDao.getCurrentTravel()
    val closedTravels: Flow<List<TravelEntity>> = travelDao.getClosedTravels()

    suspend fun getTravelById(id: String): TravelEntity? {
        return travelDao.getTravelById(id)
    }

    suspend fun insertTravel(travel: TravelEntity) {
        travelDao.insertTravel(travel)
    }

    suspend fun updateTravel(travel: TravelEntity) {
        travelDao.updateTravel(travel)
    }

    suspend fun deleteTravel(travel: TravelEntity) {
        travelDao.deleteTravel(travel)
    }
}
