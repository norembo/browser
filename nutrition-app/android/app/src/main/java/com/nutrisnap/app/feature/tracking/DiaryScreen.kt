package com.nutrisnap.app.feature.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrisnap.app.data.model.Nutrition
import com.nutrisnap.app.ui.theme.CarbAmber
import com.nutrisnap.app.ui.theme.FatPurple
import com.nutrisnap.app.ui.theme.ProteinBlue

@Composable
fun DiaryScreen(
    onAddFood: () -> Unit,
    vm: DiaryViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddFood,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Log food") },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Calorie summary
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Today", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat("Eaten", state.consumed.calories.toString())
                        Stat("Budget", state.budgetCalories.toString())
                        Stat("Left", state.remaining.toString())
                    }
                    Spacer(Modifier.height(12.dp))
                    val targets = state.targets?.macros ?: Nutrition.EMPTY
                    MacroBar("Protein", state.consumed.proteinG, targets.proteinG, ProteinBlue)
                    MacroBar("Carbs", state.consumed.carbsG, targets.carbsG, CarbAmber)
                    MacroBar("Fat", state.consumed.fatG, targets.fatG, FatPurple)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Diary", style = MaterialTheme.typography.titleMedium)

            if (state.logs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nothing logged yet. Tap “Log food”.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.logs, key = { it.id }) { log ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(log.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${log.meal.name.lowercase()} · ${log.nutrition.calories} kcal · " +
                                        "P${log.nutrition.proteinG.toInt()} C${log.nutrition.carbsG.toInt()} F${log.nutrition.fatG.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MacroBar(label: String, current: Double, target: Double, color: androidx.compose.ui.graphics.Color) {
    val pct = if (target > 0) (current / target).toFloat().coerceIn(0f, 1f) else 0f
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("${current.toInt()} / ${target.toInt()}g", style = MaterialTheme.typography.labelSmall)
        }
        LinearProgressIndicator(
            progress = { pct },
            color = color,
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
    }
}
