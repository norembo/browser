package com.nutrisnap.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FastingSession(
    val id: String,
    val protocol: String = "16:8",
    val targetHours: Int = 16,
    val startedAt: Long,
    val endedAt: Long? = null,
    val completed: Boolean = false,
) {
    val isActive: Boolean get() = endedAt == null
}
