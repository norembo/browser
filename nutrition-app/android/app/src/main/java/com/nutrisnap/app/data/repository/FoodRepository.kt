package com.nutrisnap.app.data.repository

import com.nutrisnap.app.data.local.FoodLogDao
import com.nutrisnap.app.data.local.toEntity
import com.nutrisnap.app.data.local.toModel
import com.nutrisnap.app.data.model.FoodLog
import com.nutrisnap.app.data.remote.OpenFoodFactsApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Result of a barcode lookup, ready to be turned into a FoodLog. */
data class ScannedProduct(
    val barcode: String,
    val name: String,
    val servingUnit: String,
    val nutritionPerServing: com.nutrisnap.app.data.model.Nutrition,
)

@Singleton
class FoodRepository @Inject constructor(
    private val dao: FoodLogDao,
    private val off: OpenFoodFactsApi,
) {
    fun observeDay(date: String): Flow<List<FoodLog>> =
        dao.observeDay(date).map { list -> list.map { it.toModel() } }

    suspend fun addLog(log: FoodLog) = dao.upsert(log.toEntity())

    suspend fun deleteLog(id: String) = dao.delete(id)

    /**
     * Looks up a scanned barcode against OpenFoodFacts.
     * Returns null when the product is unknown (status != 1) or has no macros.
     */
    suspend fun lookupBarcode(barcode: String): ScannedProduct? {
        val resp = off.product(barcode)
        if (resp.status != 1 || resp.product == null) return null
        val p = resp.product
        val nutrition = p.nutriments?.toNutrition() ?: return null
        if (nutrition.calories == 0) return null
        return ScannedProduct(
            barcode = barcode,
            name = listOfNotNull(p.brands?.substringBefore(","), p.productName)
                .joinToString(" ").ifBlank { "Scanned product" },
            servingUnit = p.servingSize ?: "serving",
            nutritionPerServing = nutrition,
        )
    }
}
