package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.AppDatabase
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelRepository

class ActivityViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getDatabase(context)
        val repository = TravelRepository(db.travelDao())
        return ActivityViewModel(repository) as T
    }
}
