package com.nutrisnap.app.data.remote.dto

import com.nutrisnap.app.data.model.Nutrition
import kotlinx.serialization.Serializable

/** Request body for the "Snap It" vision endpoint. Image sent as base64 JPEG. */
@Serializable
data class SnapRequest(
    val imageBase64: String,
    val hintMeal: String? = null,
)

@Serializable
data class SnapItem(
    val name: String,
    val servingQty: Double = 1.0,
    val servingUnit: String = "serving",
    val nutrition: Nutrition,
)

@Serializable
data class SnapResponse(
    val items: List<SnapItem> = emptyList(),
    val confidence: Double = 0.0,
)

@Serializable
data class InsightsResponse(
    val insights: List<String> = emptyList(),
)
