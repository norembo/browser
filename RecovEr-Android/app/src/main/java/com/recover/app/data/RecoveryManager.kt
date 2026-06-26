package com.recover.app.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

class RecoveryManager(private val context: Context) {

    private val _snapshot      = MutableStateFlow(HealthSnapshot())
    private val _score         = MutableStateFlow(RecoveryScore.placeholder)
    private val _isLoading     = MutableStateFlow(false)
    private val _faceScanState = MutableStateFlow<FaceScanState>(FaceScanState.Idle)
    private val _devices       = MutableStateFlow(defaultDevices())
    private val _history       = MutableStateFlow<List<DailyEntry>>(emptyList())
    private val _healthAvailable = MutableStateFlow(false)

    val snapshot:       StateFlow<HealthSnapshot>  = _snapshot
    val score:          StateFlow<RecoveryScore>   = _score
    val isLoading:      StateFlow<Boolean>         = _isLoading
    val faceScanState:  StateFlow<FaceScanState>   = _faceScanState
    val devices:        StateFlow<List<WearableDevice>> = _devices
    val history:        StateFlow<List<DailyEntry>>    = _history
    val healthAvailable: StateFlow<Boolean>         = _healthAvailable

    // MARK: - Health Connect

    suspend fun checkAndConnect() {
        val status = HealthConnectClient.getSdkStatus(context)
        _healthAvailable.value = status == HealthConnectClient.SDK_AVAILABLE
        refresh()
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val snap = if (_healthAvailable.value) {
                readFromHealthConnect()
            } else {
                demoSnapshot()
            }
            _snapshot.value = snap
            _score.value = RecoveryScore.from(snap.hrv, snap.restingHR, snap.sleepHours, snap.spo2)
            if (_history.value.isEmpty()) buildHistory()
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun readFromHealthConnect(): HealthSnapshot {
        val client = HealthConnectClient.getOrCreate(context)
        val now    = Instant.now()
        val dayAgo = now.minusSeconds(86400)
        val range  = TimeRangeFilter.between(dayAgo, now)

        val hrv = client.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, range))
            .records.lastOrNull()?.heartRateVariabilityMillis ?: 0.0

        val hr = client.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, range))
            .records.lastOrNull()?.beatsPerMinute?.toDouble() ?: 0.0

        val spo2 = client.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, range))
            .records.lastOrNull()?.percentage?.value ?: 0.0

        val steps = client.readRecords(ReadRecordsRequest(StepsRecord::class, range))
            .records.sumOf { it.count }.toInt()

        val cals = client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, range))
            .records.sumOf { it.energy.inKilocalories }.toInt()

        val sleepSeconds = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, range))
            .records.sumOf {
                it.stages.filter { s ->
                    s.stage in listOf(
                        SleepSessionRecord.STAGE_TYPE_SLEEPING,
                        SleepSessionRecord.STAGE_TYPE_DEEP,
                        SleepSessionRecord.STAGE_TYPE_REM,
                        SleepSessionRecord.STAGE_TYPE_LIGHT
                    )
                }.sumOf { s -> s.endTime.epochSecond - s.startTime.epochSecond }
            }

        return HealthSnapshot(
            hrv            = if (hrv > 0) hrv else demoValue(42.0, 20.0),
            restingHR      = if (hr > 0) hr  else demoValue(62.0, 12.0),
            sleepHours     = if (sleepSeconds > 0) sleepSeconds / 3600.0 else demoValue(7.2, 1.5),
            spo2           = if (spo2 > 0) spo2 else demoValue(97.5, 1.5),
            steps          = if (steps > 0) steps else Random.nextInt(3000, 11000),
            activeCalories = if (cals > 0) cals else Random.nextInt(200, 600),
            stressScore    = Random.nextInt(15, 55),
            bodyBattery    = Random.nextInt(30, 95)
        )
    }

    private fun demoSnapshot() = HealthSnapshot(
        hrv            = demoValue(42.0, 20.0),
        restingHR      = demoValue(62.0, 12.0),
        sleepHours     = demoValue(7.2, 1.5),
        spo2           = demoValue(97.5, 1.5),
        steps          = Random.nextInt(3000, 11000),
        activeCalories = Random.nextInt(200, 600),
        stressScore    = Random.nextInt(15, 55),
        bodyBattery    = Random.nextInt(30, 95)
    )

    // MARK: - Face Scan (ML Kit)

    suspend fun startFaceScan(image: InputImage? = null) {
        _faceScanState.value = FaceScanState.Scanning
        delay(1800)
        _faceScanState.value = FaceScanState.Analyzing

        if (image != null) {
            runMlKitScan(image)
        } else {
            delay(1500)
            val hint = (_score.value.value + Random.nextInt(-15, 11)).coerceIn(25, 100)
            _faceScanState.value = FaceScanState.Complete(
                FaceScanResult(
                    eyeRedness    = Random.nextDouble(0.05, 0.4),
                    swellingScore = Random.nextDouble(0.0, 0.35),
                    skinTone      = Random.nextDouble(0.6, 0.95),
                    recoveryHint  = hint
                )
            )
        }
    }

    private fun runMlKitScan(image: InputImage) {
        val opts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        FaceDetection.getClient(opts).process(image)
            .addOnSuccessListener { faces ->
                val face = faces.firstOrNull()
                val eyeOpen = face?.let {
                    ((it.leftEyeOpenProbability ?: 0.5f) + (it.rightEyeOpenProbability ?: 0.5f)) / 2
                } ?: 0.7f
                val smiling = face?.smilingProbability ?: 0.5f

                val eyeRedness    = (1.0 - eyeOpen).coerceIn(0.0, 1.0).toDouble()
                val swelling      = (1.0 - smiling).coerceIn(0.0, 1.0).toDouble() * 0.5
                val recoveryHint  = ((eyeOpen * 60 + smiling * 40)).toInt().coerceIn(20, 100)

                _faceScanState.value = FaceScanState.Complete(
                    FaceScanResult(eyeRedness, swelling, 0.8, recoveryHint)
                )
            }
            .addOnFailureListener { e ->
                _faceScanState.value = FaceScanState.Error(e.message ?: "Klaida skenuojant.")
            }
    }

    fun resetFaceScan() { _faceScanState.value = FaceScanState.Idle }

    // MARK: - Wearables

    fun toggleDevice(source: WearableSource) {
        _devices.value = _devices.value.map {
            if (it.source == source) it.copy(isConnected = !it.isConnected) else it
        }
    }

    // MARK: - History

    private fun buildHistory() {
        val today = LocalDate.now()
        _history.value = (13 downTo 0).map { offset ->
            val date  = today.minusDays(offset.toLong())
            val label = if (offset == 0) "Šiandien"
                        else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("lt"))
            val value = if (offset == 0) _score.value.value else Random.nextInt(45, 93)
            DailyEntry(label, value)
        }
    }

    // MARK: - Helpers

    private fun demoValue(base: Double, spread: Double) =
        base + Random.nextDouble(-spread, spread)

    private fun defaultDevices() = listOf(
        WearableDevice(WearableSource.GALAXY_WATCH, isConnected = true,  lastSync = Instant.now().epochSecond),
        WearableDevice(WearableSource.GARMIN),
        WearableDevice(WearableSource.HUAWEI_BAND),
        WearableDevice(WearableSource.FITBIT),
        WearableDevice(WearableSource.POLAR),
        WearableDevice(WearableSource.WEAR_OS),
        WearableDevice(WearableSource.WHOOP)
    )
}
