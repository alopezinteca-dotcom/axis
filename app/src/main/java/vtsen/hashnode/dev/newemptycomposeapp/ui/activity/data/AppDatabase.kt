package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TravelEntity::class,
        TravelStopEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun travelDao(): TravelDao
    abstract fun travelStopDao(): TravelStopDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "axis.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                instance = db
                db
            }
        }
    }
}
