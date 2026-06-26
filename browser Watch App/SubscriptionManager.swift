import Foundation
import PassKit

@Observable
final class SubscriptionManager {

    private(set) var currentPlan: SubscriptionPlan = .free
    private(set) var isPromoActive: Bool = false
    private(set) var promoMessage: String = ""
    private(set) var isPurchasing: Bool = false

    private let promoCode = "Norembo"
    private let defaults  = UserDefaults.standard
    private var activePayDelegate: ApplePayDelegate?

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

    var canUseFaceScan: Bool      { hasPremium }
    var canUseAllWearables: Bool  { hasPremium }
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

    // MARK: - Apple Pay (iOS / watchOS)

    var canMakeApplePayPayments: Bool {
        PKPaymentAuthorizationController.canMakePayments()
    }

    func initiateApplePay(plan: SubscriptionPlan) async {
        guard plan != .free else { return }
        isPurchasing = true
        defer { isPurchasing = false }

        let item = PKPaymentSummaryItem(
            label: "RecovEr \(plan.rawValue)",
            amount: NSDecimalNumber(string: plan.decimalPrice),
            type: .final
        )

        let request = PKPaymentRequest()
        request.merchantIdentifier       = "merchant.com.recover.app"
        request.supportedNetworks        = [.visa, .masterCard, .amex]
        request.merchantCapabilities     = .capability3DS
        request.countryCode              = "LT"
        request.currencyCode             = "EUR"
        request.paymentSummaryItems      = [item]

        let controller = PKPaymentAuthorizationController(paymentRequest: request)
        let delegate   = ApplePayDelegate { [weak self] success in
            if success {
                self?.currentPlan = plan
                self?.defaults.set(plan.rawValue, forKey: "subscriptionPlan")
                self?.promoMessage = "\(plan.rawValue) planas aktyvuotas per Apple Pay!"
            } else {
                self?.promoMessage = "Apple Pay mokėjimas atšauktas."
            }
            self?.activePayDelegate = nil
        }
        activePayDelegate  = delegate   // retain strongly until callback fires
        controller.delegate = delegate
        await controller.present()
    }

    // MARK: - Stripe (Android only – backend flow)
    // Android app calls POST /api/create-checkout-session and opens Stripe WebView.
    // On payment success, Stripe webhook updates subscription server-side.

    func restore() async {
        isPurchasing = true
        defer { isPurchasing = false }
        try? await Task.sleep(nanoseconds: 800_000_000)
        promoMessage = isPromoActive ? "Promo versija atkurta." : "Nieko neaptikta."
    }
}

// MARK: - Apple Pay Delegate

private final class ApplePayDelegate: NSObject, PKPaymentAuthorizationControllerDelegate {
    private let completion: (Bool) -> Void
    init(completion: @escaping (Bool) -> Void) { self.completion = completion }

    func paymentAuthorizationController(
        _ controller: PKPaymentAuthorizationController,
        didAuthorizePayment payment: PKPayment,
        handler: @escaping (PKPaymentAuthorizationResult) -> Void
    ) {
        // Forward payment.token to your backend for server-side confirmation
        handler(PKPaymentAuthorizationResult(status: .success, errors: nil))
        completion(true)
    }

    func paymentAuthorizationControllerDidFinish(_ controller: PKPaymentAuthorizationController) {
        controller.dismiss()
    }
}

// MARK: - Plan Price Helpers

extension SubscriptionPlan {
    var decimalPrice: String {
        switch self {
        case .free:    return "0.00"
        case .monthly: return "9.99"
        case .yearly:  return "59.99"
        }
    }
}
