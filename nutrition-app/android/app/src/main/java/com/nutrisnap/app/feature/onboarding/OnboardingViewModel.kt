package com.nutrisnap.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrisnap.app.data.model.ActivityLevel
import com.nutrisnap.app.data.model.Goal
import com.nutrisnap.app.data.model.NutritionTargets
import com.nutrisnap.app.data.model.Sex
import com.nutrisnap.app.data.model.UserProfile
import com.nutrisnap.app.data.repository.ProfileRepository
import com.nutrisnap.app.domain.usecase.CalorieTargetCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val heightCm: String = "175",
    val weightKg: String = "75",
    val ageYears: String = "30",
    val sex: Sex = Sex.MALE,
    val activity: ActivityLevel = ActivityLevel.MODERATE,
    val goal: Goal = Goal.MAINTAIN,
    val preview: NutritionTargets? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val calculator: CalorieTargetCalculator,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init { recompute() }

    fun update(transform: (OnboardingState) -> OnboardingState) {
        _state.update(transform); recompute()
    }

    private fun toProfile(s: OnboardingState) = UserProfile(
        heightCm = s.heightCm.toDoubleOrNull() ?: 175.0,
        weightKg = s.weightKg.toDoubleOrNull() ?: 75.0,
        ageYears = s.ageYears.toIntOrNull() ?: 30,
        sex = s.sex,
        activityLevel = s.activity,
        goal = s.goal,
    )

    private fun recompute() {
        val targets = calculator.calculate(toProfile(_state.value))
        _state.update { it.copy(preview = targets) }
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.save(toProfile(_state.value))
            onDone()
        }
    }
}
