package com.nutrisnap.app.feature.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX [ImageAnalysis.Analyzer] that decodes product barcodes on-device with
 * ML Kit. No network, no key — runs at frame rate. Emits each unique raw value
 * once via [onBarcode] and then ignores duplicates so we don't fire the lookup
 * repeatedly while the user holds the code in frame.
 *
 * Usage (in the Compose camera setup):
 *   val analyzer = BarcodeScannerService { value -> viewModel.onBarcodeScanned(value) }
 *   imageAnalysis.setAnalyzer(executor, analyzer)
 */
class BarcodeScannerService(
    private val onBarcode: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build()
    )

    private var lastEmitted: String? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue }?.let { value ->
                    if (value != lastEmitted) {
                        lastEmitted = value
                        onBarcode(value)
                    }
                }
            }
            .addOnCompleteListener {
                // Always close the proxy so the pipeline can deliver the next frame.
                imageProxy.close()
            }
    }

    /** Allow re-scanning the same code (e.g. after the user dismisses a result). */
    fun reset() { lastEmitted = null }
}
