package vtsen.hashnode.dev.newemptycomposeapp.ui.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.AppDatabase
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelRepository

/* =========================================================
   VIEWMODEL FACTORY · ACTIVITY
   ========================================================= */

class ActivityViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(ActivityViewModel::class.java)) {

            // 1️⃣ Base de datos
            val database = AppDatabase.getInstance(context)

            // 2️⃣ DAO
            val travelDao = database.travelDao()

            // 3️⃣ Repository
            val repository = TravelRepository(travelDao)

            // 4️⃣ ViewModel
            return ActivityViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}