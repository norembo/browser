package com.nutrisnap.app.data.model

import kotlinx.serialization.Serializable

enum class Sex { MALE, FEMALE }

/** Activity multipliers applied on top of BMR (Mifflin-St Jeor). */
enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    ATHLETE(1.9),
}

enum class Goal(val calorieDelta: Int) {
    LOSE(-500),      // ~0.45 kg/week deficit
    MAINTAIN(0),
    GAIN(+350),
}

@Serializable
data class UserProfile(
    val heightCm: Double = 175.0,
    val weightKg: Double = 75.0,
    val ageYears: Int = 30,
    val sex: Sex = Sex.MALE,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goal: Goal = Goal.MAINTAIN,
)

/** Computed targets the rest of the app reads from. */
@Serializable
data class NutritionTargets(
    val tdee: Int,
    val dailyCalories: Int,
    val macros: Nutrition,
    /** Per-weekday overrides for calorie cycling. Key = 0(Mon)..6(Sun). */
    val calorieCyclingByWeekday: Map<Int, Int> = emptyMap(),
)
