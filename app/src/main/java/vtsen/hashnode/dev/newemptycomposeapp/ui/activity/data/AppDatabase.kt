package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TravelEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun travelDao(): TravelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration() // 🟢 Destruye la v1 y crea la v2 limpia
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
