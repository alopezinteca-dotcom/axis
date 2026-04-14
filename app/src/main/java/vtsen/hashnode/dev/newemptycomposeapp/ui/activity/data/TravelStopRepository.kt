package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import kotlinx.coroutines.flow.Flow

class TravelStopRepository(
    private val dao: TravelStopDao
) {
    fun stopsForTravel(travelId: String): Flow<List<TravelStopEntity>> =
        dao.getStopsForTravel(travelId)

    fun stopsInRange(fromMillis: Long, toMillis: Long): Flow<List<TravelStopEntity>> =
        dao.getStopsInRange(fromMillis, toMillis)

    suspend fun upsert(stop: TravelStopEntity) {
        dao.upsertStop(stop)
    }

    suspend fun delete(stopId: String) {
        dao.deleteStop(stopId)
    }
}
