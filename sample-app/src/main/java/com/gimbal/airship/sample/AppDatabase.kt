package com.gimbal.airship.sample

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gimbal.airship.sample.data.PlaceEventDao
import com.gimbal.airship.sample.data.PlaceEventDataModel

@Database(
    entities = [
        PlaceEventDataModel::class
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getPlaceEventDao(): PlaceEventDao
}
