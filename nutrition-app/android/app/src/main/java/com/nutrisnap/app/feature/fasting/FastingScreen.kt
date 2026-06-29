package com.nutrisnap.app.feature.fasting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FastingScreen(vm: FastingViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (s.active != null) "Fasting (${s.targetHours}:${24 - s.targetHours})" else "16:8 Intermittent Fasting",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(24.dp))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { s.progress },
                modifier = Modifier.size(220.dp),
                strokeWidth = 14.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.elapsedLabel, style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (s.active != null) "${(s.progress * 100).toInt()}% of ${s.targetHours}h" else "Not fasting",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = vm::toggle,
            colors = if (s.active != null)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            else ButtonDefaults.buttonColors(),
        ) {
            Text(if (s.active != null) "End fast" else "Start fast")
        }
    }
}
