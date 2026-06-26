import SwiftUI

struct DashboardView: View {
    let manager: RecoveryManager
    let subscription: SubscriptionManager

    var body: some View {
        ScrollView {
            VStack(spacing: 10) {
                scoreRing
                metricsGrid
                if subscription.hasAIRecommendations {
                    adviceCard
                }
                historySection
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
        }
        .background(appBackground)
        .navigationTitle("RecovEr")
    }

    // MARK: - Score Ring

    private var scoreRing: some View {
        let score = manager.score
        return ZStack {
            Circle()
                .stroke(score.color.opacity(0.2), lineWidth: 8)
            Circle()
                .trim(from: 0, to: CGFloat(score.value) / 100)
                .stroke(
                    score.color.gradient,
                    style: StrokeStyle(lineWidth: 8, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .animation(.easeOut(duration: 1.0), value: score.value)

            VStack(spacing: 2) {
                if manager.isLoading {
                    ProgressView().tint(score.color)
                } else {
                    Text("\(score.value)")
                        .font(.system(size: 32, weight: .bold, design: .rounded))
                        .foregroundStyle(score.color)
                    Text(score.label)
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(.secondary)
                }
            }
        }
        .frame(width: 110, height: 110)
        .padding(.top, 4)
    }

    // MARK: - Metrics Grid

    private var metricsGrid: some View {
        let snap = manager.snapshot
        return VStack(spacing: 6) {
            HStack(spacing: 6) {
                metricTile(label: "HRV",   value: "\(Int(snap.hrv)) ms",  icon: "waveform.path.ecg", color: .purple)
                metricTile(label: "ŠSD",   value: "\(Int(snap.restingHR)) bpm", icon: "heart.fill", color: .red)
            }
            HStack(spacing: 6) {
                metricTile(label: "Miegas", value: String(format: "%.1f val.", snap.sleepHours), icon: "moon.fill",   color: .indigo)
                metricTile(label: "SpO₂",  value: String(format: "%.0f%%",   snap.spo2),        icon: "lungs.fill",  color: .teal)
            }
            HStack(spacing: 6) {
                metricTile(label: "Žingsniai", value: "\(snap.steps)",           icon: "figure.walk",    color: .green)
                metricTile(label: "Kalorijos", value: "\(snap.activeCalories) kcal", icon: "flame.fill", color: .orange)
            }
        }
    }

    private func metricTile(label: String, value: String, icon: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(color)
                Text(label)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Text(value)
                .font(.footnote.weight(.bold))
                .foregroundStyle(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(8)
        .background(cardBg)
    }

    // MARK: - AI Advice Card

    private var adviceCard: some View {
        HStack(spacing: 8) {
            Image(systemName: "brain.head.profile")
                .font(.title3)
                .foregroundStyle(.blue)

            VStack(alignment: .leading, spacing: 2) {
                Text("AI Rekomendacija")
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.secondary)
                Text(manager.score.advice)
                    .font(.footnote)
                    .foregroundStyle(.primary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(cardBg)
    }

    // MARK: - History

    private var historySection: some View {
        VStack(alignment: .leading, spacing: 6) {
            Label("14 dienų istorija", systemImage: "chart.bar.fill")
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.secondary)

            let entries = subscription.hasUnlimitedHistory ? manager.history : Array(manager.history.suffix(7))
            HStack(alignment: .bottom, spacing: 3) {
                ForEach(entries) { entry in
                    VStack(spacing: 2) {
                        RoundedRectangle(cornerRadius: 3)
                            .fill(entry.color.gradient)
                            .frame(width: 8, height: max(4, CGFloat(entry.score) / 100 * 40))
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .frame(height: 44)

            if !subscription.hasUnlimitedHistory {
                Text("Premium: neribota istorija")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(cardBg)
    }

    // MARK: - Shared Style

    private var cardBg: some ShapeStyle { .thinMaterial }

    private var appBackground: some View {
        LinearGradient(
            colors: [Color(red: 0.04, green: 0.06, blue: 0.14), Color(red: 0.01, green: 0.02, blue: 0.06)],
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }
}
