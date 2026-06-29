package com.nutrisnap.app.data.remote.dto

import com.nutrisnap.app.data.model.Nutrition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Subset of the OpenFoodFacts product response we actually use. */
@Serializable
data class OffResponse(
    val status: Int = 0,
    val product: OffProduct? = null,
)

@Serializable
data class OffProduct(
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    @SerialName("serving_quantity") val servingQuantity: Double? = null,
    val nutriments: OffNutriments? = null,
)

/**
 * OFF gives nutriments per 100g/ml AND per serving. We prefer per-serving when
 * available, else fall back to per-100g.
 */
@Serializable
data class OffNutriments(
    @SerialName("energy-kcal_serving") val kcalServing: Double? = null,
    @SerialName("energy-kcal_100g") val kcal100g: Double? = null,
    @SerialName("proteins_serving") val proteinServing: Double? = null,
    @SerialName("proteins_100g") val protein100g: Double? = null,
    @SerialName("carbohydrates_serving") val carbsServing: Double? = null,
    @SerialName("carbohydrates_100g") val carbs100g: Double? = null,
    @SerialName("fat_serving") val fatServing: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("fiber_serving") val fiberServing: Double? = null,
    @SerialName("fiber_100g") val fiber100g: Double? = null,
    @SerialName("sugars_serving") val sugarServing: Double? = null,
    @SerialName("sugars_100g") val sugar100g: Double? = null,
    @SerialName("sodium_serving") val sodiumServing: Double? = null,
    @SerialName("sodium_100g") val sodium100g: Double? = null,
) {
    /** Returns nutrition for one serving, preferring per-serving fields. */
    fun toNutrition(): Nutrition {
        fun pick(serving: Double?, per100: Double?) = serving ?: per100 ?: 0.0
        return Nutrition(
            calories = pick(kcalServing, kcal100g).toInt(),
            proteinG = pick(proteinServing, protein100g),
            carbsG = pick(carbsServing, carbs100g),
            fatG = pick(fatServing, fat100g),
            fiberG = pick(fiberServing, fiber100g),
            sugarG = pick(sugarServing, sugar100g),
            // OFF reports sodium in grams; convert to mg.
            sodiumMg = pick(sodiumServing, sodium100g) * 1000.0,
        )
    }
}
