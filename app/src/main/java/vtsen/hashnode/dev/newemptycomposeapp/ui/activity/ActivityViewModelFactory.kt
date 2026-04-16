package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.AppDatabase
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelRepository
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopRepository

class ActivityViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getDatabase(context.applicationContext)

        val travelRepository = TravelRepository(db.travelDao())
        val stopRepository = TravelStopRepository(db.travelStopDao())

        return ActivityViewModel(
            appContext = context.applicationContext,
            repository = travelRepository,
            stopRepository = stopRepository
        ) as T
    }
}
