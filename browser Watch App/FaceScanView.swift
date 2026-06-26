import SwiftUI

struct FaceScanView: View {
    let manager: RecoveryManager
    let subscription: SubscriptionManager

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                if !subscription.canUseFaceScan {
                    premiumLockedView
                } else {
                    scanContent
                }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
        }
        .background(appBackground)
        .navigationTitle("Veido Skan.")
    }

    // MARK: - Premium Lock

    private var premiumLockedView: some View {
        VStack(spacing: 10) {
            Image(systemName: "faceid")
                .font(.system(size: 40))
                .foregroundStyle(.blue.gradient)

            Text("Veido Biometrija")
                .font(.headline)

            Text("Ši funkcija reikalauja Premium plano. Naudok promo kodą NOREMBO arba užsiprenumeruok.")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(.thinMaterial)
    }

    // MARK: - Scan Content

    @ViewBuilder
    private var scanContent: some View {
        switch manager.faceScanState {
        case .idle:
            idleView
        case .scanning:
            scanningView
        case .analyzing:
            analyzingView
        case .complete(let result):
            resultView(result)
        case .error(let msg):
            errorView(msg)
        }
    }

    // MARK: - Idle

    private var idleView: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .stroke(.blue.opacity(0.3), lineWidth: 2)
                    .frame(width: 80, height: 80)
                Image(systemName: "faceid")
                    .font(.system(size: 38))
                    .foregroundStyle(.blue.gradient)
            }

            VStack(spacing: 4) {
                Text("Veido Skenavimas")
                    .font(.headline)
                Text("Kamera analizuoja odos spalvą, akių paraudimą ir patinimą, kad įvertintų atsigavimą.")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }

            infoRow(icon: "eye.fill",     text: "Akių paraudimas",     color: .red)
            infoRow(icon: "allergens",    text: "Odos tonas",           color: .orange)
            infoRow(icon: "face.smiling", text: "Veido patinimas",      color: .blue)

            Button {
                Task { await manager.startFaceScan() }
            } label: {
                Label("Pradėti skenavimą", systemImage: "camera.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(.blue)

            Text("Reikalinga iPhone kamera. Apple Watch rodo rezultatus per WatchConnectivity.")
                .font(.system(size: 9))
                .foregroundStyle(.tertiary)
                .multilineTextAlignment(.center)
        }
        .padding(10)
        .background(.thinMaterial)
    }

    // MARK: - Scanning

    private var scanningView: some View {
        VStack(spacing: 10) {
            PulsingCircleView(color: .blue)
            Text("Skenuojamas veidas…")
                .font(.footnote.weight(.semibold))
            Text("Prašome laikyti telefoną priešais veidą.")
                .font(.caption2)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(.thinMaterial)
    }

    // MARK: - Analyzing

    private var analyzingView: some View {
        VStack(spacing: 10) {
            PulsingCircleView(color: .purple)
            Text("AI analizuoja…")
                .font(.footnote.weight(.semibold))
            Text("Skaičiuojamas atsigavimo indeksas pagal biometrinius duomenis.")
                .font(.caption2)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(.thinMaterial)
    }

    // MARK: - Result

    private func resultView(_ result: FaceScanResult) -> some View {
        VStack(spacing: 10) {
            let score = RecoveryScore.make(value: result.recoveryHint)

            ZStack {
                Circle()
                    .trim(from: 0, to: CGFloat(result.recoveryHint) / 100)
                    .stroke(score.color.gradient, style: StrokeStyle(lineWidth: 7, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .frame(width: 80, height: 80)
                    .animation(.easeOut(duration: 0.8), value: result.recoveryHint)

                VStack(spacing: 0) {
                    Text("\(result.recoveryHint)")
                        .font(.system(size: 24, weight: .bold, design: .rounded))
                        .foregroundStyle(score.color)
                    Text(score.label)
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(.secondary)
                }
            }

            Text(result.interpretation)
                .font(.footnote)
                .multilineTextAlignment(.center)
                .foregroundStyle(.primary)

            VStack(spacing: 6) {
                biometricRow(label: "Akių paraudimas", value: result.eyeRedness,    low: "Mažas", high: "Didelis", color: .red)
                biometricRow(label: "Patinimas",       value: result.swellingScore, low: "Norma", high: "Pastebimas", color: .orange)
                biometricRow(label: "Odos tonas",      value: 1 - result.skinTone,  low: "Šviežias", high: "Blyškas", color: .teal)
            }

            Button("Skenuoti dar kartą") {
                manager.resetFaceScan()
            }
            .buttonStyle(.bordered)
            .font(.footnote)
        }
        .padding(10)
        .frame(maxWidth: .infinity)
        .background(.thinMaterial)
    }

    // MARK: - Error

    private func errorView(_ msg: String) -> some View {
        VStack(spacing: 8) {
            Image(systemName: "xmark.circle.fill")
                .font(.title2)
                .foregroundStyle(.red)
            Text(msg)
                .font(.footnote)
                .multilineTextAlignment(.center)
            Button("Bandyti iš naujo") { manager.resetFaceScan() }
                .buttonStyle(.bordered)
        }
        .padding()
        .background(.thinMaterial)
    }

    // MARK: - Reusable

    private func infoRow(icon: String, text: String, color: Color) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon).foregroundStyle(color).font(.caption)
            Text(text).font(.caption2).foregroundStyle(.primary)
            Spacer()
        }
    }

    private func biometricRow(label: String, value: Double, low: String, high: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack {
                Text(label).font(.caption2).foregroundStyle(.secondary)
                Spacer()
                Text(value < 0.4 ? low : high)
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(value < 0.4 ? .green : color)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 3).fill(Color.white.opacity(0.1)).frame(height: 5)
                    RoundedRectangle(cornerRadius: 3).fill(color.gradient).frame(width: geo.size.width * value, height: 5)
                }
            }
            .frame(height: 5)
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

// MARK: - Pulsing Animation

struct PulsingCircleView: View {
    let color: Color
    @State private var scale = 1.0

    var body: some View {
        ZStack {
            Circle().fill(color.opacity(0.2))
                .frame(width: 80, height: 80)
                .scaleEffect(scale)
            Circle().fill(color.opacity(0.4))
                .frame(width: 55, height: 55)
            Image(systemName: "faceid").font(.system(size: 28)).foregroundStyle(color)
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 1.0).repeatForever(autoreverses: true)) {
                scale = 1.25
            }
        }
    }
}
