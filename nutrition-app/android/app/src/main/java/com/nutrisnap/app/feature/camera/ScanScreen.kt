package com.nutrisnap.app.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

/**
 * Live camera screen. Toggles between on-device barcode scanning and AI Snap It.
 * Shows a confirmation card when a food is resolved before it's logged.
 */
@Composable
fun ScanScreen(
    onLogged: () -> Unit,
    vm: ScanViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamera = granted }

    if (!hasCamera) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Camera access is needed to scan food.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Grant camera permission")
            }
        }
        return
    }

    // Remember the ImageCapture use case so the shutter can trigger Snap It.
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }

    Box(Modifier.fillMaxSize()) {
        CameraPreview(
            mode = state.mode,
            imageCapture = imageCapture,
            onBarcode = vm::onBarcodeScanned,
        )

        // Top: mode toggle
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.mode == ScanMode.BARCODE,
                onClick = { vm.setMode(ScanMode.BARCODE) },
                label = { Text("Barcode") },
            )
            FilterChip(
                selected = state.mode == ScanMode.SNAP,
                onClick = { vm.setMode(ScanMode.SNAP) },
                label = { Text("Snap It (AI)") },
            )
        }

        // Bottom controls / results
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state.error?.let { msg ->
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text(msg, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
                }
            }

            state.pending?.let { food ->
                ConfirmFoodCard(
                    food = food,
                    onConfirm = { vm.confirm(onLogged) },
                    onDismiss = vm::dismiss,
                )
            }

            if (state.loading) {
                CircularProgressIndicator()
            } else if (state.pending == null && state.mode == ScanMode.SNAP) {
                Button(onClick = { vm.onSnap(imageCapture) }) { Text("Capture & Analyze") }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    mode: ScanMode,
    imageCapture: ImageCapture,
    onBarcode: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeAnalyzer = remember(onBarcode) { BarcodeScannerService(onBarcode) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, barcodeAnalyzer) }

                provider.unbindAll()
                // Bind barcode analysis only in BARCODE mode; always bind capture for SNAP.
                val useCases = buildList {
                    add(preview)
                    add(imageCapture)
                    if (mode == ScanMode.BARCODE) add(analysis)
                }.toTypedArray()
                provider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, *useCases,
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        update = { /* rebind handled by factory key changes in production */ },
    )
}

@Composable
private fun ConfirmFoodCard(
    food: PendingFood,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(food.name, style = MaterialTheme.typography.titleMedium)
            food.confidence?.let {
                Text("AI confidence: ${(it * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            val n = food.nutrition
            Text("${n.calories} kcal · P ${n.proteinG.toInt()}g · C ${n.carbsG.toInt()}g · F ${n.fatG.toInt()}g")
            Text("per ${food.servingUnit}", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(0.4f)) { Text("Cancel") }
                Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("Add to diary") }
            }
        }
    }
}
