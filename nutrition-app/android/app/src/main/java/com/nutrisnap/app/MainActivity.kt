package com.nutrisnap.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nutrisnap.app.feature.water.WaterReminderWorker
import com.nutrisnap.app.ui.navigation.NutriNavHost
import com.nutrisnap.app.ui.theme.NutriSnapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Ask for notification permission (Android 13+) and schedule water reminders.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        WaterReminderWorker.schedule(applicationContext)

        setContent {
            NutriSnapTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NutriNavHost()
                }
            }
        }
    }
}
