package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import kotlinx.coroutines.flow.Flow

class TravelStopRepository(
    private val dao: TravelStopDao
) {
    fun stopsForTravel(travelId: String): Flow<List<TravelStopEntity>> =
        dao.getStopsForTravel(travelId)

    fun stopsInRange(fromMillis: Long, toMillis: Long): Flow<List<TravelStopEntity>> =
        dao.getStopsInRange(fromMillis, toMillis)

    suspend fun addStop(travelId: String, place: String, km: Int?) {
        dao.insertStop(
            TravelStopEntity(
                travelId = travelId,
                place = place.trim(),
                km = km
            )
        )
    }

    suspend fun deleteStop(id: String) {
        dao.deleteStop(id)
    }
}
