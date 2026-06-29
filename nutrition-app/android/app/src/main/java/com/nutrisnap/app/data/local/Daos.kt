package com.nutrisnap.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY loggedAt ASC")
    fun observeDay(date: String): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE date BETWEEN :start AND :end ORDER BY loggedAt ASC")
    suspend fun rangeOnce(start: String, end: String): List<FoodLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: FoodLogEntity)

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface FastingDao {
    @Query("SELECT * FROM fasting_sessions WHERE endedAt IS NULL LIMIT 1")
    fun observeActive(): Flow<FastingEntity?>

    @Query("SELECT * FROM fasting_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<FastingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: FastingEntity)
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs WHERE date = :date")
    fun observe(date: String): Flow<WaterDayEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: WaterDayEntity)
}
