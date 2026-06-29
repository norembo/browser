package com.nutrisnap.app.feature.camera

import android.util.Base64
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import com.nutrisnap.app.data.remote.BackendApi
import com.nutrisnap.app.data.remote.dto.SnapRequest
import com.nutrisnap.app.data.remote.dto.SnapResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * "Snap It": captures a single frame via CameraX, downsizes + JPEG-encodes it,
 * and sends it to our backend, which calls OpenAI Vision and returns identified
 * food items with estimated macros. The OpenAI key never touches the device.
 *
 * We cap the JPEG to keep the upload small (vision models don't need full-res)
 * and run encoding off the main thread.
 */
@Singleton
class SnapItService @Inject constructor(
    private val backend: BackendApi,
) {
    private val executor: Executor = Executors.newSingleThreadExecutor()

    /** Triggers capture and returns the analyzed result. Throws on capture/network failure. */
    suspend fun snap(imageCapture: ImageCapture, hintMeal: String? = null): SnapResponse {
        val jpeg = captureJpeg(imageCapture)
        val base64 = withContext(Dispatchers.Default) {
            Base64.encodeToString(jpeg, Base64.NO_WRAP)
        }
        return backend.snapIt(SnapRequest(imageBase64 = base64, hintMeal = hintMeal))
    }

    /** Bridges CameraX's callback-based capture into a coroutine, yielding JPEG bytes. */
    private suspend fun captureJpeg(imageCapture: ImageCapture): ByteArray =
        suspendCancellableCoroutine { cont ->
            imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val bytes = image.toJpegBytes()
                        cont.resume(bytes)
                    } catch (t: Throwable) {
                        cont.resumeWithException(t)
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            })
        }

    /** CameraX delivers JPEG-format frames as a single plane; read it straight out. */
    private fun ImageProxy.toJpegBytes(): ByteArray {
        val buffer = planes[0].buffer
        val out = ByteArrayOutputStream(buffer.remaining())
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        out.write(bytes)
        return out.toByteArray()
    }
}
