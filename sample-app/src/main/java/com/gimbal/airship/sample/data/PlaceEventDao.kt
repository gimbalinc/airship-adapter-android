package com.gimbal.airship.sample.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceEventDao {
    @Insert(onConflict = REPLACE)
    fun insertPlaceEvent(item: PlaceEventDataModel)

    @Query("SELECT * FROM place_events ORDER BY timestamp DESC")
    fun getPlaceEvents(): Flow<List<PlaceEventDataModel>>

    @Query("DELETE FROM place_events")
    suspend fun deleteAllPlaceEvents()
}
