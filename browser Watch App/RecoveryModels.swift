import Foundation
import SwiftUI

// MARK: - Recovery Score

struct RecoveryScore {
    let value: Int          // 0-100
    let label: String
    let color: Color
    let advice: String

    static func from(hrv: Double, restingHR: Double, sleepHours: Double, spo2: Double) -> RecoveryScore {
        var score = 50.0

        // HRV contribution (higher = better)
        if hrv > 60 { score += 20 } else if hrv > 40 { score += 10 } else if hrv < 20 { score -= 15 }

        // Resting HR (lower = better)
        if restingHR < 55 { score += 15 } else if restingHR < 65 { score += 5 } else if restingHR > 80 { score -= 15 }

        // Sleep (7-9h optimal)
        if sleepHours >= 7 && sleepHours <= 9 { score += 15 }
        else if sleepHours >= 6 { score += 5 }
        else if sleepHours < 5 { score -= 20 }

        // SpO2
        if spo2 >= 98 { score += 5 } else if spo2 < 95 { score -= 10 }

        let clamped = max(0, min(100, Int(score)))
        return RecoveryScore.make(value: clamped)
    }

    static func make(value: Int) -> RecoveryScore {
        switch value {
        case 80...100:
            return RecoveryScore(value: value, label: "Puikus",   color: .green,  advice: "Galite intensyviai treniruotis.")
        case 60...79:
            return RecoveryScore(value: value, label: "Geras",    color: .teal,   advice: "Vidutinio intensyvumo treniruotė tinka.")
        case 40...59:
            return RecoveryScore(value: value, label: "Vidutinis",color: .yellow, advice: "Atsigavimas dar vyksta. Pailsėkite.")
        case 20...39:
            return RecoveryScore(value: value, label: "Žemas",    color: .orange, advice: "Rekomenduojama lengva veikla arba poilsis.")
        default:
            return RecoveryScore(value: value, label: "Kritinis", color: .red,    advice: "Privalote ilsėtis. Venkite didelių krūvių.")
        }
    }

    static let placeholder = RecoveryScore(value: 0, label: "Kraunama…", color: .gray, advice: "Laukiama duomenų.")
}

// MARK: - Wearable Device

enum WearableSource: String, CaseIterable, Identifiable {
    case appleWatch   = "Apple Watch"
    case garmin       = "Garmin"
    case huaweiBand   = "Huawei Band"
    case fitbit       = "Fitbit"
    case polarH10     = "Polar H10"
    case samsung      = "Samsung Galaxy Watch"
    case whoop        = "WHOOP"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .appleWatch:  return "applewatch"
        case .garmin:      return "figure.run.circle.fill"
        case .huaweiBand:  return "waveform.path.ecg"
        case .fitbit:      return "heart.fill"
        case .polarH10:    return "heart.circle.fill"
        case .samsung:     return "applewatch.watchface"
        case .whoop:       return "bandage.fill"
        }
    }

    var color: Color {
        switch self {
        case .appleWatch:  return .blue
        case .garmin:      return .green
        case .huaweiBand:  return .red
        case .fitbit:      return .teal
        case .polarH10:    return .orange
        case .samsung:     return .indigo
        case .whoop:       return .black
        }
    }

    var supportsHealthKit: Bool {
        self == .appleWatch
    }
}

struct WearableDevice: Identifiable {
    let id = UUID()
    let source: WearableSource
    var isConnected: Bool
    var lastSync: Date?
    var batteryLevel: Int?
}

// MARK: - Health Metrics

struct HealthSnapshot {
    var hrv: Double          = 0
    var restingHR: Double    = 0
    var sleepHours: Double   = 0
    var spo2: Double         = 0
    var steps: Int           = 0
    var activeCalories: Int  = 0
    var bodyBattery: Int     = 0   // Garmin-style
    var stressScore: Int     = 0
    var readinessScore: Int  = 0   // WHOOP-style

    var isPopulated: Bool {
        hrv > 0 || restingHR > 0 || sleepHours > 0
    }
}

// MARK: - Subscription Plans

enum SubscriptionPlan: String, CaseIterable, Identifiable {
    case free    = "Nemokamas"
    case monthly = "Mėnesinis"
    case yearly  = "Metinis"

    var id: String { rawValue }

    var price: String {
        switch self {
        case .free:    return "0 €"
        case .monthly: return "9.99 €/mėn."
        case .yearly:  return "59.99 €/m."
        }
    }

    var features: [String] {
        switch self {
        case .free:
            return ["Bazinis atsigavimo rodiklis", "Apple Watch sinchronizacija", "7 dienų istorija"]
        case .monthly:
            return ["Visi nemokamo plano privalumai", "Veido skenavimas", "Visos apyrankės", "Stripe apmokėjimas", "Neribota istorija", "AI rekomendacijos"]
        case .yearly:
            return ["Visi mėnesinio plano privalumai", "2 mėnesiai nemokami", "Prioritetinis palaikymas", "Eksportas PDF"]
        }
    }

    var stripeProductId: String {
        switch self {
        case .free:    return ""
        case .monthly: return "prod_recovery_monthly"
        case .yearly:  return "prod_recovery_yearly"
        }
    }
}

// MARK: - Face Scan Result

enum FaceScanState {
    case idle
    case scanning
    case analyzing
    case complete(FaceScanResult)
    case error(String)
}

struct FaceScanResult {
    let skinTone: Double        // 0–1 (palyginamas su baziniu)
    let eyeRedness: Double      // 0–1
    let swellingScore: Double   // 0–1
    let recoveryHint: Int       // 0–100

    var interpretation: String {
        if recoveryHint > 75 { return "Veido analizė rodo gerą atsigavimą." }
        if recoveryHint > 50 { return "Pastebimi vidutinio nuovargio ženklai." }
        if recoveryHint > 25 { return "Akivaizdi nuovarga. Rekomenduojamas poilsis." }
        return "Stiprus nuovargis. Miegas yra prioritetas."
    }
}
