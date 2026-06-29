package com.nutrisnap.app.feature.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrisnap.app.data.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class WaterUiState(
    val totalMl: Int = 0,
    val goalMl: Int = 2500,
) {
    val progress: Float get() = (totalMl.toFloat() / goalMl).coerceIn(0f, 1f)
}

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val repo: WaterRepository,
) : ViewModel() {

    private val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    private val goalMl = 2500

    val state: StateFlow<WaterUiState> = repo.observe(today)
        .map { day -> WaterUiState(totalMl = day?.totalMl ?: 0, goalMl = day?.goalMl ?: goalMl) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WaterUiState(goalMl = goalMl))

    fun add(ml: Int) = viewModelScope.launch { repo.addWater(today, ml, goalMl) }
}
