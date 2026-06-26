//
//  browser_Watch_AppTests.swift
//  browser Watch AppTests
//
//  Created by Norembo on 20.03.26.
//

import Foundation
import Testing
@testable import browser_Watch_App

struct RecoveryScoreTests {

    @Test func scoreIsExcellentWhenMetricsArePeak() {
        let score = RecoveryScore.from(hrv: 70, restingHR: 50, sleepHours: 8, spo2: 99)
        #expect(score.value >= 80)
        #expect(score.label == "Puikus")
    }

    @Test func scoreIsCriticalWithPoorSleep() {
        let score = RecoveryScore.from(hrv: 15, restingHR: 90, sleepHours: 3, spo2: 93)
        #expect(score.value < 40)
    }

    @Test func scoreClampedBetween0And100() {
        let low  = RecoveryScore.from(hrv: 0, restingHR: 110, sleepHours: 0, spo2: 80)
        let high = RecoveryScore.from(hrv: 200, restingHR: 30, sleepHours: 9, spo2: 100)
        #expect(low.value  >= 0)
        #expect(high.value <= 100)
    }

    @Test func makeLabelMatchesRange() {
        #expect(RecoveryScore.make(value: 85).label == "Puikus")
        #expect(RecoveryScore.make(value: 65).label == "Geras")
        #expect(RecoveryScore.make(value: 50).label == "Vidutinis")
        #expect(RecoveryScore.make(value: 30).label == "Žemas")
        #expect(RecoveryScore.make(value: 10).label == "Kritinis")
    }

    @Test func placeholderHasZeroValue() {
        #expect(RecoveryScore.placeholder.value == 0)
    }
}

struct SubscriptionManagerTests {

    @Test func correctPromoCodeUnlocksPremium() {
        let mgr = SubscriptionManager()
        #expect(!mgr.hasPremium)
        mgr.applyPromoCode("Norembo")
        #expect(mgr.hasPremium)
        #expect(mgr.isPromoActive)
        #expect(mgr.currentPlan == .yearly)
    }

    @Test func promoCodeCaseInsensitive() {
        let mgr = SubscriptionManager()
        mgr.applyPromoCode("NOREMBO")
        #expect(mgr.hasPremium)
    }

    @Test func wrongPromoCodeDoesNotUnlock() {
        let mgr = SubscriptionManager()
        mgr.applyPromoCode("WRONG")
        #expect(!mgr.hasPremium)
        #expect(mgr.promoMessage.contains("Neteisingas"))
    }

    @Test func freeUserCannotUseFaceScan() {
        let mgr = SubscriptionManager()
        #expect(!mgr.canUseFaceScan)
    }

    @Test func promoUserCanUseFaceScan() {
        let mgr = SubscriptionManager()
        mgr.applyPromoCode("norembo")
        #expect(mgr.canUseFaceScan)
        #expect(mgr.canUseAllWearables)
        #expect(mgr.hasAIRecommendations)
        #expect(mgr.hasUnlimitedHistory)
    }

    @Test func clearPromoMessageWorks() {
        let mgr = SubscriptionManager()
        mgr.applyPromoCode("BAD")
        #expect(!mgr.promoMessage.isEmpty)
        mgr.clearPromoMessage()
        #expect(mgr.promoMessage.isEmpty)
    }
}

struct FaceScanResultTests {

    @Test func highRecoveryHintGivesPositiveInterpretation() {
        let result = FaceScanResult(skinTone: 0.9, eyeRedness: 0.1, swellingScore: 0.05, recoveryHint: 80)
        #expect(result.interpretation.contains("gerą"))
    }

    @Test func lowRecoveryHintWarnsAboutFatigue() {
        let result = FaceScanResult(skinTone: 0.5, eyeRedness: 0.7, swellingScore: 0.6, recoveryHint: 20)
        #expect(result.interpretation.contains("Stiprus"))
    }
}

struct HealthSnapshotTests {

    @Test func defaultSnapshotIsNotPopulated() {
        let snap = HealthSnapshot()
        #expect(!snap.isPopulated)
    }

    @Test func snapshotWithHRVIsPopulated() {
        let snap = HealthSnapshot(hrv: 45)
        #expect(snap.isPopulated)
    }
}
