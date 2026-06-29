package com.nutrisnap.app.feature.fasting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrisnap.app.data.model.FastingSession
import com.nutrisnap.app.data.repository.FastingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FastingUiState(
    val active: FastingSession? = null,
    val elapsedMs: Long = 0,
    val targetHours: Int = 16,
) {
    val progress: Float
        get() = if (active == null) 0f
        else (elapsedMs / (targetHours * 3_600_000.0)).toFloat().coerceIn(0f, 1f)

    val elapsedLabel: String
        get() {
            val totalSec = elapsedMs / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return "%02d:%02d:%02d".format(h, m, s)
        }
}

@HiltViewModel
class FastingViewModel @Inject constructor(
    private val repo: FastingRepository,
) : ViewModel() {

    // Ticks once per second to refresh the elapsed display.
    private val ticker = flow {
        while (true) { emit(System.currentTimeMillis()); delay(1000) }
    }

    val state: StateFlow<FastingUiState> = combine(repo.observeActive(), ticker) { active, now ->
        FastingUiState(
            active = active,
            elapsedMs = active?.let { now - it.startedAt } ?: 0,
            targetHours = active?.targetHours ?: 16,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FastingUiState())

    fun toggle() = viewModelScope.launch {
        if (state.value.active == null) repo.start() else repo.stop()
    }
}
