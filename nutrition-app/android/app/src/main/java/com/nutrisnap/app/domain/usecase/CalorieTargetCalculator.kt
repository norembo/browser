package com.nutrisnap.app.domain.usecase

import com.nutrisnap.app.data.model.Goal
import com.nutrisnap.app.data.model.Nutrition
import com.nutrisnap.app.data.model.NutritionTargets
import com.nutrisnap.app.data.model.Sex
import com.nutrisnap.app.data.model.UserProfile
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Turns an onboarding profile into daily calorie + macro targets.
 *
 * BMR via Mifflin-St Jeor, then TDEE = BMR * activity multiplier, then apply the
 * goal delta. Macro split: protein anchored to bodyweight (1.8 g/kg — solid for
 * an active person on a deficit), fat at 25% of calories, carbs fill the rest.
 */
class CalorieTargetCalculator @Inject constructor() {

    fun calculate(profile: UserProfile): NutritionTargets {
        val s = if (profile.sex == Sex.MALE) 5 else -161
        val bmr = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.ageYears + s
        val tdee = (bmr * profile.activityLevel.multiplier).roundToInt()
        val daily = (tdee + profile.goal.calorieDelta).coerceAtLeast(1200)

        val proteinG = 1.8 * profile.weightKg
        val fatG = (daily * 0.25) / 9.0
        val proteinCals = proteinG * 4
        val fatCals = fatG * 9
        val carbsG = ((daily - proteinCals - fatCals) / 4.0).coerceAtLeast(0.0)

        val macros = Nutrition(
            calories = daily,
            proteinG = proteinG.roundToInt().toDouble(),
            carbsG = carbsG.roundToInt().toDouble(),
            fatG = fatG.roundToInt().toDouble(),
            // Guideline targets, not derived from calories:
            fiberG = 30.0,
            sugarG = (daily * 0.10 / 4.0).roundToInt().toDouble(), // <=10% cals
            sodiumMg = 2300.0,
        )
        return NutritionTargets(tdee = tdee, dailyCalories = daily, macros = macros)
    }

    /**
     * Resolves today's budget honoring calorie cycling. Falls back to the flat
     * daily target when no per-weekday override exists.
     */
    fun budgetFor(date: LocalDate, targets: NutritionTargets): Int {
        val idx = date.dayOfWeek.toIndex()
        return targets.calorieCyclingByWeekday[idx] ?: targets.dailyCalories
    }

    private fun DayOfWeek.toIndex() = ordinal // MONDAY=0 .. SUNDAY=6
}
