package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

/* =========================================================
   REPOSITORY · VIAJES
   CAPA DE DOMINIO ENTRE UI Y ROOM
   ========================================================= */

class TravelRepository(
    private val travelDao: TravelDao
) {

    /* -----------------------------------------------------
       INICIAR VIAJE (PANTALLA 3)
       ----------------------------------------------------- */

    suspend fun startTravel(
        origin: String,
        destination: String,
        description: String,
        kmStart: Int,
        billingExpected: Double,
        hasDiet: Boolean,
        startTimestamp: Long
    ) {
        val travel = TravelEntity(
            origin = origin,
            destination = destination,
            description = description,
            kmStart = kmStart,
            kmEnd = null,
            billingExpected = billingExpected,
            hasDiet = hasDiet,
            hoursImputed = null,
            status = TravelStatus.IN_PROGRESS,
            startTimestamp = startTimestamp,
            endTimestamp = null
        )

        travelDao.insertTravel(travel)
    }

    /* -----------------------------------------------------
       OBTENER VIAJE EN CURSO
       ----------------------------------------------------- */

    suspend fun getTravelInProgress(): TravelEntity? {
        return travelDao.getTravelInProgress()
    }

    /* -----------------------------------------------------
       CERRAR VIAJE (PANTALLA 2)
       ----------------------------------------------------- */

    suspend fun closeTravel(
        travelId: Long,
        kmEnd: Int,
        hoursImputed: Double,
        endTimestamp: Long
    ) {
        travelDao.closeTravel(
            travelId = travelId,
            kmEnd = kmEnd,
            hoursImputed = hoursImputed,
            endTimestamp = endTimestamp
        )
    }

    /* -----------------------------------------------------
       OBTENER VIAJES CERRADOS (HISTÓRICO / EXCEL)
       ----------------------------------------------------- */

    suspend fun getClosedTravels(): List<TravelEntity> {
        return travelDao.getClosedTravels()
    }

    /* -----------------------------------------------------
       EXPORTACIÓN (PASO 1)
       ----------------------------------------------------- */

    suspend fun exportClosedTravels(): List<TravelEntity> {
        return travelDao.getClosedTravels()
    }
}
