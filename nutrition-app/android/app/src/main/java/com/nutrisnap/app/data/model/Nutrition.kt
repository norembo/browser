package com.nutrisnap.app.data.model

import kotlinx.serialization.Serializable

/**
 * Full macro breakdown for a food item or a daily total.
 * All values are absolute amounts for the logged serving (not per-100g).
 */
@Serializable
data class Nutrition(
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
) {
    operator fun plus(other: Nutrition) = Nutrition(
        calories = calories + other.calories,
        proteinG = proteinG + other.proteinG,
        carbsG = carbsG + other.carbsG,
        fatG = fatG + other.fatG,
        fiberG = fiberG + other.fiberG,
        sugarG = sugarG + other.sugarG,
        sodiumMg = sodiumMg + other.sodiumMg,
    )

    /** Scale a per-serving nutrition value by a quantity (e.g. 1.5 servings). */
    fun scaledBy(factor: Double) = Nutrition(
        calories = (calories * factor).toInt(),
        proteinG = proteinG * factor,
        carbsG = carbsG * factor,
        fatG = fatG * factor,
        fiberG = fiberG * factor,
        sugarG = sugarG * factor,
        sodiumMg = sodiumMg * factor,
    )

    companion object {
        val EMPTY = Nutrition()
    }
}
