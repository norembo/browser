package com.recover.app.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.recover.app.data.FaceScanResult
import com.recover.app.data.FaceScanState
import com.recover.app.data.RecoveryScore
import com.recover.app.ui.theme.*
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FaceScanScreen(
    faceScanState: FaceScanState,
    canUseFaceScan: Boolean,
    onStartScan: (InputImage?) -> Unit,
    onReset: () -> Unit
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Veido Skenavimas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)

        when {
            !canUseFaceScan -> PremiumLockedCard()

            !cameraPermission.status.isGranted -> PermissionCard {
                cameraPermission.launchPermissionRequest()
            }

            else -> when (val state = faceScanState) {
                is FaceScanState.Idle      -> IdleCard { image -> onStartScan(image) }
                is FaceScanState.Scanning  -> ScanningCard()
                is FaceScanState.Analyzing -> AnalyzingCard()
                is FaceScanState.Complete  -> ResultCard(state.result, onReset)
                is FaceScanState.Error     -> ErrorCard(state.message, onReset)
            }
        }
    }
}

@Composable
private fun PremiumLockedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier              = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(12.dp)
        ) {
            Text("🔒", fontSize = 48.sp)
            Text("Veido Biometrija", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "Ši funkcija reikalauja Premium plano.\nNaudok promo kodą NOREMBO arba užsiprenumeruok.",
                fontSize  = 13.sp,
                color     = TextSec,
                modifier  = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier              = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(12.dp)
        ) {
            Text("📷", fontSize = 48.sp)
            Text("Reikalinga kamera", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text("Leidžia atlikti veido biometrinę analizę.", fontSize = 13.sp, color = TextSec)
            Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) {
                Text("Suteikti leidimą")
            }
        }
    }
}

@Composable
private fun IdleCard(onStart: (InputImage?) -> Unit) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var capturedImage by remember { mutableStateOf<InputImage?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live camera preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                val executor = remember { Executors.newSingleThreadExecutor() }

                AndroidView(
                    factory = { ctx ->
                        val preview = PreviewView(ctx)
                        val cameraFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraFuture.addListener({
                            val provider = cameraFuture.get()
                            val previewUseCase = Preview.Builder().build().also {
                                it.setSurfaceProvider(preview.surfaceProvider)
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            analysis.setAnalyzer(executor) { proxy ->
                                val mediaImage = proxy.image
                                if (mediaImage != null) {
                                    capturedImage = InputImage.fromMediaImage(
                                        mediaImage, proxy.imageInfo.rotationDegrees
                                    )
                                }
                                proxy.close()
                            }
                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                    previewUseCase, analysis
                                )
                            } catch (_: Exception) {}
                        }, executor)
                        preview
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Face overlay guide
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.Center)
                        .border(2.dp, AccentBlue, CircleShape)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Sulygiuokite veidą su apskritimu", fontSize = 13.sp, color = TextSec)
                Text("ML Kit analizuoja akių ir veido parametrus", fontSize = 11.sp, color = TextSec)
            }

            Button(
                onClick  = { onStart(capturedImage) },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Text("Pradėti skenavimą", fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FeatureChip("👁 Akių paraudimas")
                FeatureChip("🫀 Odos tonas")
                FeatureChip("💧 Patinimas")
            }
        }
    }
}

@Composable
private fun FeatureChip(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.07f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(text, fontSize = 10.sp, color = TextSec, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun ScanningCard() {
    PulsingCard(label = "Skenuojamas veidas…", sublabel = "ML Kit analizuoja kadrą", color = AccentBlue)
}

@Composable
private fun AnalyzingCard() {
    PulsingCard(label = "AI analizuoja…", sublabel = "Skaičiuojamas atsigavimo indeksas", color = Color(0xFF8B5CF6))
}

@Composable
private fun PulsingCard(label: String, sublabel: String, color: Color) {
    val inf = rememberInfiniteTransition(label = "pulse")
    val scale by inf.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.2f,
        animationSpec  = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label          = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier            = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🔍", fontSize = 32.sp)
            }
            Text(label,   fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(sublabel, fontSize = 12.sp, color = TextSec)
            LinearProgressIndicator(color = color, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ResultCard(result: FaceScanResult, onReset: () -> Unit) {
    val score = RecoveryScore.make(result.recoveryHint)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Veido analizė baigta", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${result.recoveryHint}",
                    fontSize   = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = score.color
                )
                Column {
                    Text(score.label, fontWeight = FontWeight.Bold, color = score.color)
                    Text("Atsigavimas", fontSize = 11.sp, color = TextSec)
                }
            }

            Text(result.interpretation, fontSize = 13.sp, color = TextSec,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BiometricBar("Akių paraudimas", result.eyeRedness,    Color(0xFFEF4444), "Mažas",  "Didelis")
                BiometricBar("Patinimas",        result.swellingScore, Color(0xFFF97316), "Norma",  "Pastebimas")
                BiometricBar("Odos gyvybingumas",1.0 - result.skinTone,Color(0xFF14B8A6), "Šviežias","Blyškas")
            }

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Skenuoti dar kartą")
            }
        }
    }
}

@Composable
private fun BiometricBar(label: String, value: Double, color: Color, low: String, high: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = TextSec)
            Text(
                if (value < 0.4) low else high,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (value < 0.4) Color(0xFF34C759) else color
            )
        }
        LinearProgressIndicator(
            progress    = { value.toFloat() },
            color       = color,
            trackColor  = Color.White.copy(alpha = 0.1f),
            modifier    = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun ErrorCard(message: String, onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        shape    = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier            = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("❌", fontSize = 36.sp)
            Text(message, fontSize = 13.sp, color = Color(0xFFEF4444),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Button(onClick = onReset, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) {
                Text("Bandyti iš naujo")
            }
        }
    }
}
