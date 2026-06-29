package com.nutrisnap.app.data.model

import kotlinx.serialization.Serializable

enum class Meal { BREAKFAST, LUNCH, DINNER, SNACK }

/** How the entry was created — drives UI badges and analytics. */
enum class FoodSource { SNAP, BARCODE, MANUAL, RECIPE }

/**
 * A single logged food item. `date` is denormalized ("yyyy-MM-dd") so we can
 * query a day cheaply both in Room and Firestore.
 */
@Serializable
data class FoodLog(
    val id: String,
    val loggedAt: Long,
    val date: String,
    val meal: Meal,
    val source: FoodSource,
    val name: String,
    val servingQty: Double = 1.0,
    val servingUnit: String = "serving",
    val nutrition: Nutrition,
    val aiConfidence: Double? = null,
    val barcode: String? = null,
    val photoUrl: String? = null,
)
