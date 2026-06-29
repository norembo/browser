package com.nutrisnap.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FoodLogEntity::class, FastingEntity::class, WaterDayEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
    abstract fun fastingDao(): FastingDao
    abstract fun waterDao(): WaterDao
}
