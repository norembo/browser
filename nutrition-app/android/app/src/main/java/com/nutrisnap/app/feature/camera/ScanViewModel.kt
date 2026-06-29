package com.nutrisnap.app.feature.camera

import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrisnap.app.data.model.FoodLog
import com.nutrisnap.app.data.model.FoodSource
import com.nutrisnap.app.data.model.Meal
import com.nutrisnap.app.data.model.Nutrition
import com.nutrisnap.app.data.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

enum class ScanMode { BARCODE, SNAP }

/** A food item resolved by either scanner, awaiting user confirmation. */
data class PendingFood(
    val name: String,
    val servingUnit: String,
    val nutrition: Nutrition,
    val source: FoodSource,
    val confidence: Double? = null,
    val barcode: String? = null,
)

data class ScanUiState(
    val mode: ScanMode = ScanMode.BARCODE,
    val loading: Boolean = false,
    val pending: PendingFood? = null,
    val error: String? = null,
    val selectedMeal: Meal = Meal.LUNCH,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val snapItService: SnapItService,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    fun setMode(mode: ScanMode) = _state.update { it.copy(mode = mode, pending = null, error = null) }
    fun setMeal(meal: Meal) = _state.update { it.copy(selectedMeal = meal) }
    fun dismiss() = _state.update { it.copy(pending = null, error = null) }

    /** Called by [BarcodeScannerService] when a code is decoded. */
    fun onBarcodeScanned(barcode: String) {
        if (_state.value.loading || _state.value.pending != null) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val product = runCatching { foodRepository.lookupBarcode(barcode) }.getOrNull()
            _state.update {
                if (product == null) {
                    it.copy(loading = false, error = "Product not found ($barcode). Try Snap It or add manually.")
                } else {
                    it.copy(
                        loading = false,
                        pending = PendingFood(
                            name = product.name,
                            servingUnit = product.servingUnit,
                            nutrition = product.nutritionPerServing,
                            source = FoodSource.BARCODE,
                            barcode = product.barcode,
                        ),
                    )
                }
            }
        }
    }

    /** Called by the UI after the user taps the shutter in SNAP mode. */
    fun onSnap(imageCapture: ImageCapture) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                snapItService.snap(imageCapture, hintMeal = _state.value.selectedMeal.name)
            }.getOrNull()
            val top = result?.items?.firstOrNull()
            _state.update {
                if (top == null) {
                    it.copy(loading = false, error = "Couldn't recognize the food. Try again or add manually.")
                } else {
                    it.copy(
                        loading = false,
                        pending = PendingFood(
                            name = top.name,
                            servingUnit = top.servingUnit,
                            nutrition = top.nutrition,
                            source = FoodSource.SNAP,
                            confidence = result.confidence,
                        ),
                    )
                }
            }
        }
    }

    /** Persists the confirmed item as a FoodLog for today. */
    fun confirm(onLogged: () -> Unit) {
        val p = _state.value.pending ?: return
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            foodRepository.addLog(
                FoodLog(
                    id = UUID.randomUUID().toString(),
                    loggedAt = System.currentTimeMillis(),
                    date = today,
                    meal = _state.value.selectedMeal,
                    source = p.source,
                    name = p.name,
                    servingUnit = p.servingUnit,
                    nutrition = p.nutrition,
                    aiConfidence = p.confidence,
                    barcode = p.barcode,
                )
            )
            _state.update { it.copy(pending = null) }
            onLogged()
        }
    }
}
