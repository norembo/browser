package com.nutrisnap.app.data.repository

import com.nutrisnap.app.data.local.FastingDao
import com.nutrisnap.app.data.local.toEntity
import com.nutrisnap.app.data.local.toModel
import com.nutrisnap.app.data.model.FastingSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FastingRepository @Inject constructor(
    private val dao: FastingDao,
) {
    fun observeActive(): Flow<FastingSession?> = dao.observeActive().map { it?.toModel() }
    fun observeRecent(): Flow<List<FastingSession>> = dao.observeRecent().map { l -> l.map { it.toModel() } }

    suspend fun start(protocol: String = "16:8", targetHours: Int = 16) {
        // Guard against two concurrent active fasts.
        if (dao.observeActive().firstOrNull() != null) return
        dao.upsert(
            FastingSession(
                id = UUID.randomUUID().toString(),
                protocol = protocol,
                targetHours = targetHours,
                startedAt = System.currentTimeMillis(),
            ).toEntity()
        )
    }

    suspend fun stop() {
        val active = dao.observeActive().firstOrNull() ?: return
        val ended = System.currentTimeMillis()
        val hours = (ended - active.startedAt) / 3_600_000.0
        dao.upsert(active.copy(endedAt = ended, completed = hours >= active.targetHours))
    }
}
