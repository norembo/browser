package com.nutrisnap.app.data.remote

import com.nutrisnap.app.data.remote.dto.InsightsResponse
import com.nutrisnap.app.data.remote.dto.OffResponse
import com.nutrisnap.app.data.remote.dto.SnapRequest
import com.nutrisnap.app.data.remote.dto.SnapResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** OpenFoodFacts — public, no key. Called directly from device for barcode lookup. */
interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun product(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String =
            "product_name,brands,serving_size,serving_quantity,nutriments",
    ): OffResponse
}

/**
 * Our Firebase Cloud Functions. Holds the OpenAI key server-side, so the device
 * only ever talks to us for AI features.
 */
interface BackendApi {
    @POST("snapIt")
    suspend fun snapIt(@Body body: SnapRequest): SnapResponse

    @GET("insights")
    suspend fun insights(@Query("days") days: Int = 14): InsightsResponse
}
