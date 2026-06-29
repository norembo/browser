package com.nutrisnap.app.feature.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun WaterScreen(vm: WaterViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Water", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { s.progress },
                modifier = Modifier.size(200.dp),
                strokeWidth = 14.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${s.totalMl} ml", style = MaterialTheme.typography.headlineMedium)
                Text("of ${s.goalMl} ml", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.add(250) }) { Text("+250 ml") }
            OutlinedButton(onClick = { vm.add(500) }) { Text("+500 ml") }
            Button(onClick = { vm.add(750) }) { Text("+750 ml") }
        }
    }
}
