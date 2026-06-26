package com.recover.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recover.app.data.WearableDevice
import com.recover.app.data.WearableSource
import com.recover.app.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WearableScreen(
    devices: List<WearableDevice>,
    canUseAllWearables: Boolean,
    onToggle: (WearableSource) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Apyrankės", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)

        // Stats header
        val connected = devices.count { it.isConnected }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = CardBg),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Prijungti prietaisai", fontSize = 12.sp, color = TextSec)
                    Text("$connected / ${devices.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📡", fontSize = 20.sp)
                }
            }
        }

        // Device list
        devices.forEach { device ->
            DeviceCard(device = device, canUse = canUseAllWearables, onToggle = { onToggle(device.source) })
        }

        // Integration info
        IntegrationInfoCard()
    }
}

@Composable
private fun DeviceCard(
    device: WearableDevice,
    canUse: Boolean,
    onToggle: () -> Unit
) {
    val accessible = device.source == WearableSource.GALAXY_WATCH || canUse

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier  = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (accessible) device.source.color.copy(alpha = 0.2f) else Color.White.copy(0.05f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(deviceEmoji(device.source), fontSize = 22.sp)
            }

            // Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = device.source.displayName,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (accessible) Color.White else TextSec
                )
                when {
                    !accessible ->
                        Text("🔒 Reikia Premium", fontSize = 11.sp, color = Color(0xFFF59E0B))
                    device.isConnected && device.lastSync != null -> {
                        val time = Instant.ofEpochSecond(device.lastSync)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                        Text("Sinchronizuota $time", fontSize = 11.sp, color = AccentGreen)
                    }
                    device.isConnected ->
                        Text("Prijungta", fontSize = 11.sp, color = AccentGreen)
                    else ->
                        Text("Atjungta", fontSize = 11.sp, color = TextSec)
                }
            }

            // Toggle
            if (accessible) {
                Switch(
                    checked         = device.isConnected,
                    onCheckedChange = { onToggle() },
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = AccentGreen,
                        uncheckedTrackColor = Color.White.copy(0.15f)
                    )
                )
            } else {
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "PRO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IntegrationInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF0F2027)),
        shape    = RoundedCornerShape(16.dp),
        border   = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Integracijos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSec)

            listOf(
                Triple("🔷", "Health Connect",   "Oficiali Google API – ŠSD, HRV, miegas, SpO₂"),
                Triple("🟠", "Garmin Connect IQ","REST API + OAuth 2.0"),
                Triple("🔴", "Huawei Health Kit","HMS Core – HiHealth API"),
                Triple("🔵", "Fitbit Web API",   "OAuth 2.0 – real-time sinchronizacija"),
                Triple("⚫", "Polar Flow",        "BLE + Polar Open AccessLink API"),
                Triple("🟣", "WHOOP",            "WHOOP API v1 – Readiness Score"),
                Triple("🔵", "Wear OS",          "Health Services API")
            ).forEach { (emoji, name, note) ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(emoji, fontSize = 14.sp)
                    Column {
                        Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(note, fontSize = 10.sp, color = TextSec)
                    }
                }
            }
        }
    }
}

private fun deviceEmoji(source: WearableSource): String = when (source) {
    WearableSource.GALAXY_WATCH -> "⌚"
    WearableSource.GARMIN       -> "🏃"
    WearableSource.HUAWEI_BAND  -> "❤️"
    WearableSource.FITBIT       -> "💪"
    WearableSource.POLAR        -> "🫀"
    WearableSource.WEAR_OS      -> "⌚"
    WearableSource.WHOOP        -> "🖤"
}
