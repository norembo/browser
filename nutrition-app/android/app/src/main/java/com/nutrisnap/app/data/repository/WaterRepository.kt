package com.nutrisnap.app.data.repository

import com.nutrisnap.app.data.local.WaterDao
import com.nutrisnap.app.data.local.WaterDayEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterRepository @Inject constructor(
    private val dao: WaterDao,
) {
    fun observe(date: String): Flow<WaterDayEntity?> = dao.observe(date)

    suspend fun addWater(date: String, ml: Int, goalMl: Int) {
        val current = dao.observe(date).firstOrNull()
        val total = (current?.totalMl ?: 0) + ml
        dao.upsert(WaterDayEntity(date = date, goalMl = goalMl, totalMl = total.coerceAtLeast(0)))
    }
}
