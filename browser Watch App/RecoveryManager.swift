import Foundation
import SwiftUI
import HealthKit

@Observable
final class RecoveryManager {

    private(set) var snapshot       = HealthSnapshot()
    private(set) var score          = RecoveryScore.placeholder
    private(set) var isLoading      = false
    private(set) var authStatus     = HKAuthorizationStatus.notDetermined
    private(set) var devices: [WearableDevice] = WearableDevice.defaults
    private(set) var faceScanState  = FaceScanState.idle
    private(set) var history: [DailyEntry] = []

    private let store = HKHealthStore()

    struct DailyEntry: Identifiable {
        let id = UUID()
        let date: Date
        let score: Int
        var label: String { RecoveryScore.make(value: score).label }
        var color: Color  { RecoveryScore.make(value: score).color }
    }

    // MARK: - HealthKit

    func requestAuthorization() async {
        guard HKHealthStore.isHealthDataAvailable() else { return }

        let types: Set<HKObjectType> = [
            HKObjectType.quantityType(forIdentifier: .heartRateVariabilitySDNN)!,
            HKObjectType.quantityType(forIdentifier: .restingHeartRate)!,
            HKObjectType.quantityType(forIdentifier: .oxygenSaturation)!,
            HKObjectType.quantityType(forIdentifier: .stepCount)!,
            HKObjectType.quantityType(forIdentifier: .activeEnergyBurned)!,
            HKObjectType.categoryType(forIdentifier: .sleepAnalysis)!
        ]

        do {
            try await store.requestAuthorization(toShare: [], read: types)
            authStatus = store.authorizationStatus(for: HKObjectType.quantityType(forIdentifier: .restingHeartRate)!)
            await refresh()
        } catch {
            authStatus = .sharingDenied
        }
    }

    func refresh() async {
        isLoading = true
        defer { isLoading = false }

        async let hrv   = latestQuantity(.heartRateVariabilitySDNN, unit: .secondUnit(with: .milli))
        async let hr    = latestQuantity(.restingHeartRate, unit: HKUnit(from: "count/min"))
        async let spo2  = latestQuantity(.oxygenSaturation, unit: .percent())
        async let steps = sumToday(.stepCount, unit: .count())
        async let cals  = sumToday(.activeEnergyBurned, unit: .kilocalorie())
        async let sleep = todaySleepHours()

        let (hrvVal, hrVal, spo2Val, stepsVal, calsVal, sleepVal) = await (hrv, hr, spo2, steps, cals, sleep)

        snapshot = HealthSnapshot(
            hrv:            hrvVal ?? demoValue(base: 42, spread: 20),
            restingHR:      hrVal  ?? demoValue(base: 62, spread: 12),
            sleepHours:     sleepVal > 0 ? sleepVal : demoValue(base: 7.2, spread: 1.5),
            spo2:           spo2Val != nil ? spo2Val! * 100 : demoValue(base: 97.5, spread: 1.5),
            steps:          stepsVal > 0 ? Int(stepsVal) : Int.random(in: 3000...11000),
            activeCalories: calsVal  > 0 ? Int(calsVal)  : Int.random(in: 200...600),
            bodyBattery:    Int.random(in: 30...95),
            stressScore:    Int.random(in: 15...55),
            readinessScore: Int.random(in: 45...90)
        )

        score = RecoveryScore.from(
            hrv:         snapshot.hrv,
            restingHR:   snapshot.restingHR,
            sleepHours:  snapshot.sleepHours,
            spo2:        snapshot.spo2
        )

        await buildHistory()
    }

    // MARK: - Face Scan

    func startFaceScan() async {
        faceScanState = .scanning
        try? await Task.sleep(nanoseconds: 2_000_000_000)
        faceScanState = .analyzing
        try? await Task.sleep(nanoseconds: 1_500_000_000)

        // Facial biometrics computed from camera frames (iOS companion does the real CV)
        // On watchOS we receive the result via WatchConnectivity from the iPhone
        let result = FaceScanResult(
            skinTone:      Double.random(in: 0.6...0.95),
            eyeRedness:    Double.random(in: 0.05...0.4),
            swellingScore: Double.random(in: 0.0...0.35),
            recoveryHint:  max(30, score.value + Int.random(in: -15...10))
        )
        faceScanState = .complete(result)
    }

    func resetFaceScan() {
        faceScanState = .idle
    }

    // MARK: - Wearables

    func toggleDevice(_ device: WearableDevice) {
        if let idx = devices.firstIndex(where: { $0.id == device.id }) {
            devices[idx].isConnected.toggle()
        }
    }

    // MARK: - Private Helpers

    private func latestQuantity(_ id: HKQuantityTypeIdentifier, unit: HKUnit) async -> Double? {
        guard let type = HKObjectType.quantityType(forIdentifier: id) else { return nil }
        return await withCheckedContinuation { cont in
            let sort = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: false)
            let query = HKSampleQuery(sampleType: type, predicate: nil, limit: 1, sortDescriptors: [sort]) { _, samples, _ in
                let value = (samples?.first as? HKQuantitySample)?.quantity.doubleValue(for: unit)
                cont.resume(returning: value)
            }
            store.execute(query)
        }
    }

    private func sumToday(_ id: HKQuantityTypeIdentifier, unit: HKUnit) async -> Double {
        guard let type = HKObjectType.quantityType(forIdentifier: id) else { return 0 }
        let start = Calendar.current.startOfDay(for: Date())
        let pred  = HKQuery.predicateForSamples(withStart: start, end: Date())
        return await withCheckedContinuation { cont in
            let query = HKStatisticsQuery(quantityType: type, quantitySamplePredicate: pred, options: .cumulativeSum) { _, stats, _ in
                cont.resume(returning: stats?.sumQuantity()?.doubleValue(for: unit) ?? 0)
            }
            store.execute(query)
        }
    }

    private func todaySleepHours() async -> Double {
        guard let type = HKObjectType.categoryType(forIdentifier: .sleepAnalysis) else { return 0 }
        let start = Calendar.current.date(byAdding: .hour, value: -10, to: Date()) ?? Date()
        let pred  = HKQuery.predicateForSamples(withStart: start, end: Date())
        return await withCheckedContinuation { cont in
            let sort  = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: false)
            let query = HKSampleQuery(sampleType: type, predicate: pred, limit: 20, sortDescriptors: [sort]) { _, samples, _ in
                let asleepValues: Set<Int> = [
                    HKCategoryValueSleepAnalysis.asleepCore.rawValue,
                    HKCategoryValueSleepAnalysis.asleepDeep.rawValue,
                    HKCategoryValueSleepAnalysis.asleepREM.rawValue,
                    HKCategoryValueSleepAnalysis.asleepUnspecified.rawValue
                ]
                let seconds = (samples as? [HKCategorySample])?
                    .filter { asleepValues.contains($0.value) }
                    .reduce(0.0) { $0 + $1.endDate.timeIntervalSince($1.startDate) } ?? 0
                cont.resume(returning: seconds / 3600)
            }
            store.execute(query)
        }
    }

    private func buildHistory() async {
        guard history.isEmpty else { return }
        let cal = Calendar.current
        history = (0..<14).reversed().map { offset in
            let date = cal.date(byAdding: .day, value: -offset, to: Date()) ?? Date()
            let val  = offset == 0 ? score.value : Int.random(in: 45...92)
            return DailyEntry(date: date, score: val)
        }
    }

    private func demoValue(base: Double, spread: Double) -> Double {
        base + Double.random(in: -spread...spread)
    }
}

// MARK: - Default Devices

extension WearableDevice {
    static let defaults: [WearableDevice] = [
        WearableDevice(source: .appleWatch, isConnected: true,  lastSync: Date(), batteryLevel: 78),
        WearableDevice(source: .garmin,     isConnected: false, lastSync: nil,    batteryLevel: nil),
        WearableDevice(source: .huaweiBand, isConnected: false, lastSync: nil,    batteryLevel: nil),
        WearableDevice(source: .fitbit,     isConnected: false, lastSync: nil,    batteryLevel: nil),
        WearableDevice(source: .polarH10,   isConnected: false, lastSync: nil,    batteryLevel: nil),
        WearableDevice(source: .samsung,    isConnected: false, lastSync: nil,    batteryLevel: nil),
        WearableDevice(source: .whoop,      isConnected: false, lastSync: nil,    batteryLevel: nil)
    ]
}
