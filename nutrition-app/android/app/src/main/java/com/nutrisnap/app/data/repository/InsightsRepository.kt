package com.nutrisnap.app.data.repository

import com.nutrisnap.app.data.remote.BackendApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightsRepository @Inject constructor(
    private val backend: BackendApi,
) {
    /**
     * Asks the backend insights engine to analyze recent logs. The heavy
     * lifting (pattern detection + LLM phrasing) runs server-side so it can read
     * the user's full history from Firestore without shipping it to the device.
     */
    suspend fun fetch(days: Int = 14): List<String> =
        runCatching { backend.insights(days).insights }.getOrDefault(emptyList())
}
