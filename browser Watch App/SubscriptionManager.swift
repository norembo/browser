import Foundation
import SwiftUI

@Observable
final class SubscriptionManager {

    private(set) var currentPlan: SubscriptionPlan = .free
    private(set) var isPromoActive: Bool = false
    private(set) var promoMessage: String = ""
    private(set) var isPurchasing: Bool = false

    private let promoCode = "Norembo"
    private let defaults  = UserDefaults.standard

    init() {
        let savedPlan  = defaults.string(forKey: "subscriptionPlan") ?? SubscriptionPlan.free.rawValue
        let savedPromo = defaults.bool(forKey: "promoActive")
        currentPlan   = SubscriptionPlan(rawValue: savedPlan) ?? .free
        isPromoActive = savedPromo
        if isPromoActive { currentPlan = .yearly }
    }

    // MARK: - Feature Gates

    var hasPremium: Bool {
        isPromoActive || currentPlan == .monthly || currentPlan == .yearly
    }

    var canUseFaceScan: Bool     { hasPremium }
    var canUseAllWearables: Bool { hasPremium }
    var hasUnlimitedHistory: Bool { hasPremium }
    var hasAIRecommendations: Bool { hasPremium }

    // MARK: - Promo Code

    func applyPromoCode(_ code: String) {
        if code.trimmingCharacters(in: .whitespacesAndNewlines)
              .caseInsensitiveCompare(promoCode) == .orderedSame {
            isPromoActive = true
            currentPlan   = .yearly
            defaults.set(true, forKey: "promoActive")
            defaults.set(SubscriptionPlan.yearly.rawValue, forKey: "subscriptionPlan")
            promoMessage  = "Promo kodas priimtas! Visos funkcijos atrakintos."
        } else {
            promoMessage = "Neteisingas promo kodas."
        }
    }

    func clearPromoMessage() { promoMessage = "" }

    // MARK: - Stripe Checkout (requires backend)

    func initiateStripeCheckout(plan: SubscriptionPlan) async {
        guard plan != .free else { return }
        isPurchasing = true
        defer { isPurchasing = false }

        // Production flow:
        // 1. POST /api/create-checkout-session { productId: plan.stripeProductId, customerId: ... }
        // 2. Backend returns { sessionId, url }
        // 3. Open url in WKWebView / Safari
        // 4. On success Stripe webhook calls /api/webhook and confirms subscription
        // 5. App queries /api/subscription-status to refresh plan

        try? await Task.sleep(nanoseconds: 1_500_000_000)

        // Demo: activate plan (replace with real Stripe webhook confirmation)
        currentPlan = plan
        defaults.set(plan.rawValue, forKey: "subscriptionPlan")
        promoMessage = "\(plan.rawValue) planas aktyvuotas!"
    }

    func restore() async {
        isPurchasing = true
        defer { isPurchasing = false }
        try? await Task.sleep(nanoseconds: 800_000_000)
        // Production: query Stripe /v1/subscriptions or validate StoreKit receipt
        promoMessage = isPromoActive ? "Promo versija atkurta." : "Nieko neaptikta."
    }
}
