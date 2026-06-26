package com.recover.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recover.app.data.*
import com.recover.app.ui.theme.*

@Composable
fun DashboardScreen(
    score: RecoveryScore,
    snapshot: HealthSnapshot,
    history: List<DailyEntry>,
    isLoading: Boolean,
    hasPremium: Boolean,
    hasAIAdvice: Boolean,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RecovEr", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                    contentDescription = "Atnaujinti",
                    tint = AccentBlue
                )
            }
        }

        // Score Ring
        ScoreRingCard(score = score, isLoading = isLoading)

        // Metrics Grid
        MetricsGrid(snapshot = snapshot)

        // AI Advice
        if (hasAIAdvice) {
            AIAdviceCard(advice = score.advice)
        }

        // History Chart
        HistoryChart(history = history, hasPremium = hasPremium)
    }
}

@Composable
private fun ScoreRingCard(score: RecoveryScore, isLoading: Boolean) {
    val animatedProgress by animateFloatAsState(
        targetValue = score.value / 100f,
        animationSpec = tween(1000),
        label = "scoreAnim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke    = 14.dp.toPx()
                    val topLeft   = Offset(stroke / 2, stroke / 2)
                    val arcSize   = Size(size.width - stroke, size.height - stroke)

                    drawArc(
                        color     = score.color.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSize,
                        style      = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color      = score.color,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSize,
                        style      = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(32.dp))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = "${score.value}",
                            fontSize   = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = score.color
                        )
                        Text(text = score.label, fontSize = 14.sp, color = TextSec)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsGrid(snapshot: HealthSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("HRV",       "${snapshot.hrv.toInt()} ms",      Color(0xFF8B5CF6), Modifier.weight(1f))
            MetricTile("ŠSD",       "${snapshot.restingHR.toInt()} bpm",Color(0xFFEF4444), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Miegas",    "%.1f val.".format(snapshot.sleepHours), Color(0xFF6366F1), Modifier.weight(1f))
            MetricTile("SpO₂",     "%.0f%%".format(snapshot.spo2),           Color(0xFF14B8A6), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Žingsniai", "${snapshot.steps}",                Color(0xFF22C55E), Modifier.weight(1f))
            MetricTile("Kalorijos", "${snapshot.activeCalories} kcal",  Color(0xFFF97316), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Stresas",   "${snapshot.stressScore}%",         Color(0xFFF59E0B), Modifier.weight(1f))
            MetricTile("Energija",  "${snapshot.bodyBattery}%",         Color(0xFF3B82F6), Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = label, fontSize = 11.sp, color = TextSec)
        }
    }
}

@Composable
private fun AIAdviceCard(advice: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F)),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier  = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🧠", fontSize = 28.sp)
            Column {
                Text("AI Rekomendacija", fontSize = 11.sp, color = TextSec)
                Text(advice, fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun HistoryChart(history: List<DailyEntry>, hasPremium: Boolean) {
    val visible = if (hasPremium) history else history.takeLast(7)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("14 dienų istorija", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSec)

            Row(
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                visible.forEach { entry ->
                    val fraction = entry.score / 100f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(entry.color)
                    )
                }
            }

            if (!hasPremium) {
                Text(
                    "Premium: 14 dienų neribota istorija",
                    fontSize = 11.sp,
                    color    = TextSec
                )
            }
        }
    }
}
