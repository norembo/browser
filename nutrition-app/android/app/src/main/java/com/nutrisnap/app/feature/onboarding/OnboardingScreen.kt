package com.nutrisnap.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrisnap.app.data.model.ActivityLevel
import com.nutrisnap.app.data.model.Goal
import com.nutrisnap.app.data.model.Sex

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val numeric = KeyboardOptions(keyboardType = KeyboardType.Number)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Let's set your goal", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = s.heightCm, onValueChange = { v -> vm.update { it.copy(heightCm = v) } },
            label = { Text("Height (cm)") }, keyboardOptions = numeric, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = s.weightKg, onValueChange = { v -> vm.update { it.copy(weightKg = v) } },
            label = { Text("Weight (kg)") }, keyboardOptions = numeric, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = s.ageYears, onValueChange = { v -> vm.update { it.copy(ageYears = v) } },
            label = { Text("Age") }, keyboardOptions = numeric, modifier = Modifier.fillMaxWidth(),
        )

        ChipGroup("Sex", Sex.entries, s.sex, { it.name }) { sel -> vm.update { it.copy(sex = sel) } }
        ChipGroup("Activity", ActivityLevel.entries, s.activity, { it.name.lowercase() }) { sel ->
            vm.update { it.copy(activity = sel) }
        }
        ChipGroup("Goal", Goal.entries, s.goal, { it.name.lowercase() }) { sel -> vm.update { it.copy(goal = sel) } }

        s.preview?.let { t ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Your daily target", style = MaterialTheme.typography.titleMedium)
                    Text("${t.dailyCalories} kcal (TDEE ${t.tdee})")
                    val m = t.macros
                    Text("Protein ${m.proteinG.toInt()}g · Carbs ${m.carbsG.toInt()}g · Fat ${m.fatG.toInt()}g")
                    Text("Fiber ${m.fiberG.toInt()}g · Sugar ≤${m.sugarG.toInt()}g · Sodium ≤${m.sodiumMg.toInt()}mg")
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(onClick = { vm.save(onDone) }, modifier = Modifier.fillMaxWidth()) {
            Text("Start tracking")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun <T> ChipGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { opt ->
                FilterChip(
                    selected = opt == selected,
                    onClick = { onSelect(opt) },
                    label = { Text(label(opt)) },
                )
            }
        }
    }
}
