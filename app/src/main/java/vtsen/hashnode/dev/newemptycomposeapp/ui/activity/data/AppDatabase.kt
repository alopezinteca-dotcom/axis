package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/* =========================================================
   ROOM DATABASE · AXIS
   ========================================================= */

@Database(
    entities = [TravelEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class) // <-- ENCHUFAMOS EL TRADUCTOR AQUÍ
abstract class AppDatabase : RoomDatabase() {

    abstract fun travelDao(): TravelDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "axis.db"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}