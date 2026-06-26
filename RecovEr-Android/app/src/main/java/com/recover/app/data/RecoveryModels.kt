package com.recover.app.data

import androidx.compose.ui.graphics.Color

// MARK: - Recovery Score

data class RecoveryScore(
    val value: Int,
    val label: String,
    val color: Color,
    val advice: String
) {
    companion object {
        fun from(hrv: Double, restingHR: Double, sleepHours: Double, spo2: Double): RecoveryScore {
            var score = 50.0
            if (hrv > 60) score += 20 else if (hrv > 40) score += 10 else if (hrv < 20) score -= 15
            if (restingHR < 55) score += 15 else if (restingHR < 65) score += 5 else if (restingHR > 80) score -= 15
            if (sleepHours in 7.0..9.0) score += 15 else if (sleepHours >= 6) score += 5 else if (sleepHours < 5) score -= 20
            if (spo2 >= 98) score += 5 else if (spo2 < 95) score -= 10
            return make(score.coerceIn(0.0, 100.0).toInt())
        }

        fun make(value: Int): RecoveryScore = when (value) {
            in 80..100 -> RecoveryScore(value, "Puikus",    Color(0xFF34C759), "Galite intensyviai treniruotis.")
            in 60..79  -> RecoveryScore(value, "Geras",     Color(0xFF30B0C7), "Vidutinio intensyvumo treniruotė tinka.")
            in 40..59  -> RecoveryScore(value, "Vidutinis", Color(0xFFFFD60A), "Atsigavimas dar vyksta. Pailsėkite.")
            in 20..39  -> RecoveryScore(value, "Žemas",     Color(0xFFFF9F0A), "Rekomenduojama lengva veikla arba poilsis.")
            else       -> RecoveryScore(value, "Kritinis",  Color(0xFFFF3B30), "Privalote ilsėtis. Venkite didelių krūvių.")
        }

        val placeholder = RecoveryScore(0, "Kraunama…", Color(0xFF8E8E93), "Laukiama duomenų.")
    }
}

// MARK: - Health Snapshot

data class HealthSnapshot(
    val hrv: Double            = 0.0,
    val restingHR: Double      = 0.0,
    val sleepHours: Double     = 0.0,
    val spo2: Double           = 0.0,
    val steps: Int             = 0,
    val activeCalories: Int    = 0,
    val stressScore: Int       = 0,
    val bodyBattery: Int       = 0
) {
    val isPopulated get() = hrv > 0 || restingHR > 0 || sleepHours > 0
}

// MARK: - Wearable Sources

enum class WearableSource(val displayName: String, val icon: String, val colorHex: Long) {
    GALAXY_WATCH ("Samsung Galaxy Watch", "watch",            0xFF1428A0),
    GARMIN       ("Garmin",               "directions_run",   0xFF009A44),
    HUAWEI_BAND  ("Huawei Band",          "monitor_heart",    0xFFCF0A2C),
    FITBIT       ("Fitbit",               "fitness_center",   0xFF00B0B9),
    POLAR        ("Polar H10",            "favorite",         0xFFD50000),
    WEAR_OS      ("Wear OS",              "watch",            0xFF4285F4),
    WHOOP        ("WHOOP",                "sports",           0xFF000000);

    val color get() = Color(colorHex)
}

data class WearableDevice(
    val source: WearableSource,
    val isConnected: Boolean = false,
    val lastSync: Long? = null,
    val batteryLevel: Int? = null
)

// MARK: - Subscription Plans

enum class SubscriptionPlan(
    val displayName: String,
    val price: String,
    val decimalPrice: Double,
    val stripePriceId: String
) {
    FREE    ("Nemokamas", "0 €",          0.0,  ""),
    MONTHLY ("Mėnesinis", "9.99 €/mėn.", 9.99, "price_recover_monthly"),
    YEARLY  ("Metinis",   "59.99 €/m.", 59.99, "price_recover_yearly");

    val features: List<String> get() = when (this) {
        FREE    -> listOf("Bazinis atsigavimo rodiklis", "Health Connect", "7 dienų istorija")
        MONTHLY -> listOf("Visos FREE funkcijos", "Veido skenavimas", "Visos apyrankės", "Stripe mokėjimai", "Neribota istorija", "AI rekomendacijos")
        YEARLY  -> listOf("Visos MONTHLY funkcijos", "2 mėnesiai nemokami", "Prioritetinis palaikymas", "PDF eksportas")
    }
}

// MARK: - Face Scan

sealed class FaceScanState {
    object Idle      : FaceScanState()
    object Scanning  : FaceScanState()
    object Analyzing : FaceScanState()
    data class Complete(val result: FaceScanResult) : FaceScanState()
    data class Error(val message: String)           : FaceScanState()
}

data class FaceScanResult(
    val eyeRedness: Double,
    val swellingScore: Double,
    val skinTone: Double,
    val recoveryHint: Int
) {
    val interpretation: String get() = when {
        recoveryHint > 75 -> "Veido analizė rodo gerą atsigavimą."
        recoveryHint > 50 -> "Pastebimi vidutinio nuovargio ženklai."
        recoveryHint > 25 -> "Akivaizdi nuovarga. Rekomenduojamas poilsis."
        else              -> "Stiprus nuovargis. Miegas yra prioritetas."
    }
}

// MARK: - History

data class DailyEntry(
    val dayLabel: String,
    val score: Int
) {
    val color: Color get() = RecoveryScore.make(score).color
}
