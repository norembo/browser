package com.nutrisnap.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nutrisnap.app.data.model.FastingSession
import com.nutrisnap.app.data.model.FoodLog
import com.nutrisnap.app.data.model.FoodSource
import com.nutrisnap.app.data.model.Meal
import com.nutrisnap.app.data.model.Nutrition

/** Room embeds the macro columns directly into the food_logs table. */
data class NutritionEmbedded(
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
)

fun Nutrition.toEmbedded() = NutritionEmbedded(calories, proteinG, carbsG, fatG, fiberG, sugarG, sodiumMg)
fun NutritionEmbedded.toModel() = Nutrition(calories, proteinG, carbsG, fatG, fiberG, sugarG, sodiumMg)

@Entity(tableName = "food_logs")
data class FoodLogEntity(
    @PrimaryKey val id: String,
    val loggedAt: Long,
    val date: String,
    val meal: String,
    val source: String,
    val name: String,
    val servingQty: Double,
    val servingUnit: String,
    @Embedded val nutrition: NutritionEmbedded,
    val aiConfidence: Double?,
    val barcode: String?,
    val photoUrl: String?,
    val synced: Boolean = false,
)

fun FoodLog.toEntity() = FoodLogEntity(
    id, loggedAt, date, meal.name, source.name, name, servingQty, servingUnit,
    nutrition.toEmbedded(), aiConfidence, barcode, photoUrl,
)

fun FoodLogEntity.toModel() = FoodLog(
    id = id, loggedAt = loggedAt, date = date, meal = Meal.valueOf(meal),
    source = FoodSource.valueOf(source), name = name, servingQty = servingQty,
    servingUnit = servingUnit, nutrition = nutrition.toModel(),
    aiConfidence = aiConfidence, barcode = barcode, photoUrl = photoUrl,
)

@Entity(tableName = "fasting_sessions")
data class FastingEntity(
    @PrimaryKey val id: String,
    val protocol: String,
    val targetHours: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val completed: Boolean,
)

fun FastingSession.toEntity() = FastingEntity(id, protocol, targetHours, startedAt, endedAt, completed)
fun FastingEntity.toModel() = FastingSession(id, protocol, targetHours, startedAt, endedAt, completed)

@Entity(tableName = "water_logs")
data class WaterDayEntity(
    @PrimaryKey val date: String,
    val goalMl: Int,
    val totalMl: Int,
)
