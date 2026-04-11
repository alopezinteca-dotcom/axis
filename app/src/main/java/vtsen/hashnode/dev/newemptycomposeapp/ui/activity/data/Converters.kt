package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromTravelStatus(value: TravelStatus): String = value.name

    @TypeConverter
    fun toTravelStatus(value: String): TravelStatus = enumValueOf(value)
}
