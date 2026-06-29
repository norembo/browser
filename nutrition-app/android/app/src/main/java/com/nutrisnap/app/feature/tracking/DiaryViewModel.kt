package com.nutrisnap.app.feature.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrisnap.app.data.model.FoodLog
import com.nutrisnap.app.data.model.Nutrition
import com.nutrisnap.app.data.model.NutritionTargets
import com.nutrisnap.app.data.repository.FoodRepository
import com.nutrisnap.app.data.repository.ProfileRepository
import com.nutrisnap.app.domain.usecase.CalorieTargetCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DiaryUiState(
    val date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val logs: List<FoodLog> = emptyList(),
    val consumed: Nutrition = Nutrition.EMPTY,
    val targets: NutritionTargets? = null,
    val budgetCalories: Int = 0,
) {
    val remaining: Int get() = budgetCalories - consumed.calories
}

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    profileRepository: ProfileRepository,
    private val calculator: CalorieTargetCalculator,
) : ViewModel() {

    private val today = LocalDate.now()
    private val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    val state: StateFlow<DiaryUiState> = combine(
        foodRepository.observeDay(todayStr),
        profileRepository.targets,
    ) { logs, targets ->
        val consumed = logs.fold(Nutrition.EMPTY) { acc, l -> acc + l.nutrition }
        val budget = targets?.let { calculator.budgetFor(today, it) } ?: 0
        DiaryUiState(
            date = todayStr,
            logs = logs,
            consumed = consumed,
            targets = targets,
            budgetCalories = budget,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryUiState())

    fun delete(log: FoodLog) = viewModelScope.launch { foodRepository.deleteLog(log.id) }
}
