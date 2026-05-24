import SwiftUI

struct SubscriptionView: View {
    let subscription: SubscriptionManager

    @State private var promoInput   = ""
    @State private var showingPromo = false
    @State private var selectedPlan = SubscriptionPlan.monthly

    var body: some View {
        ScrollView {
            VStack(spacing: 10) {
                if subscription.isPromoActive {
                    promoActiveCard
                } else {
                    statusCard
                    planPicker
                    applePayButton
                    promoSection
                    restoreButton
                }

                if !subscription.promoMessage.isEmpty {
                    messageCard
                }

                paymentInfoCard
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
        }
        .background(appBackground)
        .navigationTitle("Premium")
    }

    // MARK: - Promo Active

    private var promoActiveCard: some View {
        VStack(spacing: 8) {
            Image(systemName: "checkmark.seal.fill")
                .font(.system(size: 36))
                .foregroundStyle(.green.gradient)

            Text("Visos funkcijos atrakintos!")
                .font(.headline)

            Text("Promo kodas NOREMBO aktyvus. Naudojatės pilna RecovEr versija nemokamai.")
                .font(.caption2)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            VStack(alignment: .leading, spacing: 5) {
                featureRow("Veido skenavimas (biometrija)")
                featureRow("Visos apyrankės: Garmin, Huawei, Fitbit…")
                featureRow("Neribota istorija")
                featureRow("AI atsigavimo rekomendacijos")
                featureRow("Apple Pay mokėjimai")
                featureRow("Eksportas į PDF")
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(.thinMaterial)
    }

    // MARK: - Status

    private var statusCard: some View {
        HStack(spacing: 8) {
            Image(systemName: subscription.hasPremium ? "crown.fill" : "star.circle")
                .font(.title3)
                .foregroundStyle(subscription.hasPremium ? .yellow : .gray)
            VStack(alignment: .leading, spacing: 2) {
                Text(subscription.currentPlan.rawValue + " planas")
                    .font(.footnote.weight(.semibold))
                Text(subscription.hasPremium ? "Pilna prieiga" : "Atrakinkite Premium")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(10)
        .background(.thinMaterial)
    }

    // MARK: - Plan Picker

    private var planPicker: some View {
        VStack(spacing: 6) {
            ForEach([SubscriptionPlan.monthly, .yearly]) { plan in
                Button {
                    selectedPlan = plan
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            HStack {
                                Text(plan.rawValue)
                                    .font(.footnote.weight(.semibold))
                                if plan == .yearly {
                                    Text("IŠSAUGO 50%")
                                        .font(.system(size: 8, weight: .bold))
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 4)
                                        .padding(.vertical, 2)
                                        .background(Capsule().fill(.green))
                                }
                            }
                            Text(plan.price)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        if selectedPlan == plan {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(.blue)
                                .font(.footnote)
                        }
                    }
                    .padding(10)
                    .background(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(selectedPlan == plan ? Color.blue : Color.white.opacity(0.1), lineWidth: 1.5)
                            .background(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(selectedPlan == plan ? Color.blue.opacity(0.1) : Color.white.opacity(0.04))
                            )
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - Apple Pay Button

    @ViewBuilder
    private var applePayButton: some View {
        if subscription.canMakeApplePayPayments {
            Button {
                Task { await subscription.initiateApplePay(plan: selectedPlan) }
            } label: {
                if subscription.isPurchasing {
                    HStack(spacing: 6) {
                        ProgressView().tint(.white)
                        Text("Apdorojama…")
                    }
                    .frame(maxWidth: .infinity)
                } else {
                    HStack(spacing: 6) {
                        Image(systemName: "apple.logo")
                            .font(.footnote.weight(.semibold))
                        Text("Pay")
                            .font(.footnote.weight(.semibold))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 2)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(.black)
            .disabled(subscription.isPurchasing)
        } else {
            HStack(spacing: 6) {
                Image(systemName: "exclamationmark.circle")
                    .font(.caption2)
                    .foregroundStyle(.orange)
                Text("Apple Pay neprieinamas šiame įrenginyje.")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .padding(8)
            .background(.thinMaterial)
        }
    }

    // MARK: - Promo Code

    private var promoSection: some View {
        VStack(spacing: 6) {
            Button {
                showingPromo.toggle()
            } label: {
                HStack {
                    Image(systemName: "tag.fill").foregroundStyle(.purple)
                    Text("Promo kodas")
                        .font(.footnote)
                    Spacer()
                    Image(systemName: showingPromo ? "chevron.up" : "chevron.down")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                .padding(10)
                .background(.thinMaterial)
            }
            .buttonStyle(.plain)

            if showingPromo {
                VStack(spacing: 6) {
                    TextField("Įvesk promo kodą", text: $promoInput)
                        .textInputAutocapitalization(.characters)
                        .disableAutocorrection(true)
                        .font(.footnote)
                        .padding(8)
                        .background(.thinMaterial)

                    Button("Pritaikyti") {
                        subscription.applyPromoCode(promoInput)
                        promoInput = ""
                        showingPromo = false
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.purple)
                    .disabled(promoInput.isEmpty)
                }
            }
        }
    }

    // MARK: - Restore

    private var restoreButton: some View {
        Button {
            Task { await subscription.restore() }
        } label: {
            Text("Atkurti pirkimus")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Message

    private var messageCard: some View {
        let isSuccess = subscription.promoMessage.contains("priimtas") ||
                        subscription.promoMessage.contains("aktyvuotas") ||
                        subscription.promoMessage.contains("atkurta")
        return HStack(spacing: 6) {
            Image(systemName: isSuccess ? "checkmark.circle.fill" : "xmark.circle.fill")
                .foregroundStyle(isSuccess ? .green : .red)
            Text(subscription.promoMessage)
                .font(.caption2)
            Spacer()
        }
        .padding(8)
        .background(.thinMaterial)
        .onTapGesture { subscription.clearPromoMessage() }
    }

    // MARK: - Payment Info

    private var paymentInfoCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Label("Mokėjimo metodai", systemImage: "lock.shield.fill")
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.secondary)

            HStack(alignment: .top, spacing: 8) {
                VStack(alignment: .leading, spacing: 4) {
                    platformBadge(icon: "apple.logo", label: "iOS / watchOS", color: .primary)
                    HStack(spacing: 4) {
                        Image(systemName: "apple.logo").font(.system(size: 9))
                        Text("Apple Pay")
                            .font(.system(size: 9, weight: .semibold))
                    }
                    .foregroundStyle(.secondary)
                }

                Divider().frame(height: 36)

                VStack(alignment: .leading, spacing: 4) {
                    platformBadge(icon: "smartphone", label: "Android", color: .green)
                    Text("Stripe (Visa, MC,\nGoogle Pay)")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }
            }

            Text("Mokėjimo duomenys šifruojami Apple / Stripe saugumu. Atšaukti galima bet kada.")
                .font(.system(size: 9))
                .foregroundStyle(.tertiary)
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial)
    }

    private func platformBadge(icon: String, label: String, color: Color) -> some View {
        HStack(spacing: 3) {
            Image(systemName: icon).font(.system(size: 9))
            Text(label).font(.system(size: 9, weight: .semibold))
        }
        .foregroundStyle(color)
    }

    // MARK: - Helpers

    private func featureRow(_ text: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: "checkmark.circle.fill")
                .font(.caption2)
                .foregroundStyle(.green)
            Text(text)
                .font(.caption2)
        }
    }

    private var appBackground: some View {
        LinearGradient(
            colors: [Color(red: 0.04, green: 0.06, blue: 0.14), Color(red: 0.01, green: 0.02, blue: 0.06)],
            startPoint: .top, endPoint: .bottom
        )
        .ignoresSafeArea()
    }
}
