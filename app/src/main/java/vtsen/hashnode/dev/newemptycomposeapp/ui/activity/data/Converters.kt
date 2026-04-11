package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.TypeConverter

/* =========================================================
   ROOM CONVERTERS
   Enseña a SQLite a entender tipos de datos complejos (Enums)
   ========================================================= */

class Converters {

    @TypeConverter
    fun fromTravelStatus(value: TravelStatus): String {
        return value.name // Guarda "IN_PROGRESS" o "CLOSED" en disco
    }

    @TypeConverter
    fun toTravelStatus(value: String): TravelStatus {
        return enumValueOf<TravelStatus>(value) // Convierte de texto a Enum en la app
    }
}