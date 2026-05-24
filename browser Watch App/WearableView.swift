import SwiftUI

struct WearableView: View {
    let manager: RecoveryManager
    let subscription: SubscriptionManager

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                headerCard
                deviceList
                if subscription.canUseAllWearables {
                    platformNote
                }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
        }
        .background(appBackground)
        .navigationTitle("Apyrankės")
    }

    // MARK: - Header

    private var headerCard: some View {
        HStack(spacing: 8) {
            Image(systemName: "antenna.radiowaves.left.and.right")
                .font(.title3)
                .foregroundStyle(.green)

            VStack(alignment: .leading, spacing: 2) {
                Text("Prijungti prietaisai")
                    .font(.footnote.weight(.semibold))
                let count = manager.devices.filter(\.isConnected).count
                Text("\(count) aktyvus")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(10)
        .background(.thinMaterial)
    }

    // MARK: - Device List

    private var deviceList: some View {
        VStack(spacing: 6) {
            ForEach(manager.devices) { device in
                DeviceRow(device: device, isPremium: subscription.canUseAllWearables) {
                    if device.source == .appleWatch || subscription.canUseAllWearables {
                        manager.toggleDevice(device)
                    }
                }
            }
        }
    }

    // MARK: - Platform Note

    private var platformNote: some View {
        VStack(alignment: .leading, spacing: 6) {
            Label("Platforma", systemImage: "info.circle")
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.secondary)

            VStack(alignment: .leading, spacing: 4) {
                platformRow(platform: "iOS",     icon: "iphone",         color: .blue,   note: "HealthKit, Garmin Connect IQ SDK")
                platformRow(platform: "Android", icon: "smartphone",     color: .green,  note: "Google Fit, Huawei Health Kit API")
                platformRow(platform: "Garmin",  icon: "figure.run",     color: .orange, note: "Connect IQ REST API")
                platformRow(platform: "Huawei",  icon: "waveform",       color: .red,    note: "Huawei Health Kit (HMS Core)")
                platformRow(platform: "Polar",   icon: "heart.circle",   color: .red,    note: "Polar Flow API / BLE")
                platformRow(platform: "Fitbit",  icon: "gauge",          color: .teal,   note: "Web API OAuth 2.0")
            }
        }
        .padding(10)
        .background(.thinMaterial)
    }

    private func platformRow(platform: String, icon: String, color: Color, note: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .font(.caption2)
                .foregroundStyle(color)
                .frame(width: 16)
            VStack(alignment: .leading, spacing: 1) {
                Text(platform)
                    .font(.caption2.weight(.semibold))
                Text(note)
                    .font(.system(size: 9))
                    .foregroundStyle(.secondary)
            }
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

// MARK: - Device Row

private struct DeviceRow: View {
    let device: WearableDevice
    let isPremium: Bool
    let onToggle: () -> Void

    private var isAccessible: Bool {
        device.source == .appleWatch || isPremium
    }

    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: 8) {
                ZStack {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(device.source.color.opacity(isAccessible ? 0.25 : 0.1))
                        .frame(width: 32, height: 32)
                    Image(systemName: device.source.icon)
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(isAccessible ? device.source.color : .gray)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(device.source.rawValue)
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(isAccessible ? .primary : .secondary)

                    if !isAccessible {
                        Text("Reikia Premium")
                            .font(.system(size: 9))
                            .foregroundStyle(.orange)
                    } else if device.isConnected, let sync = device.lastSync {
                        Text("Sinchronizuota \(sync.formatted(.relative(presentation: .named)))")
                            .font(.system(size: 9))
                            .foregroundStyle(.secondary)
                    } else {
                        Text(device.isConnected ? "Prijungta" : "Atjungta")
                            .font(.system(size: 9))
                            .foregroundStyle(device.isConnected ? .green : .secondary)
                    }
                }

                Spacer()

                if !isAccessible {
                    Image(systemName: "lock.fill")
                        .font(.caption2)
                        .foregroundStyle(.orange)
                } else {
                    ZStack {
                        RoundedRectangle(cornerRadius: 6, style: .continuous)
                            .fill(device.isConnected ? Color.green : Color.white.opacity(0.1))
                            .frame(width: 28, height: 16)
                        Circle()
                            .fill(.white)
                            .frame(width: 12, height: 12)
                            .offset(x: device.isConnected ? 6 : -6)
                            .animation(.spring(duration: 0.25), value: device.isConnected)
                    }
                }
            }
            .padding(8)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color.white.opacity(device.isConnected ? 0.08 : 0.04))
            )
        }
        .buttonStyle(.plain)
        .disabled(!isAccessible)
    }
}
