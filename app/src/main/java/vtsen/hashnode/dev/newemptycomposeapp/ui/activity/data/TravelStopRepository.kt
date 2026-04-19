package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import kotlinx.coroutines.flow.Flow

class TravelStopRepository(
    private val dao: TravelStopDao
) {
    fun stopsForTravel(travelId: String): Flow<List<TravelStopEntity>> =
        dao.getStopsForTravel(travelId)

    fun stopsInRange(fromMillis: Long, toMillis: Long): Flow<List<TravelStopEntity>> =
        dao.getStopsInRange(fromMillis, toMillis)

    // ✅ NUEVO: todas las paradas (flow)
    fun allStops(): Flow<List<TravelStopEntity>> =
        dao.getAllStops()

    // ✅ NUEVO: todas las paradas (snapshot) — imprescindible para export maestro
    suspend fun allStopsOnce(): List<TravelStopEntity> =
        dao.getAllStopsOnce()

    suspend fun upsert(stop: TravelStopEntity) =
        dao.upsert(stop)

    suspend fun delete(id: String) =
        dao.delete(id)

    // ✅ Restore / REPLACE_ALL
    suspend fun deleteAll() =
        dao.deleteAllStops()

    suspend fun upsertAll(stops: List<TravelStopEntity>) =
        dao.upsertStops(stops)
}
