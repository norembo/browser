import SwiftUI

struct ContentView: View {
    @State private var recoveryManager     = RecoveryManager()
    @State private var subscriptionManager = SubscriptionManager()
    @State private var selectedTab         = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                DashboardView(manager: recoveryManager, subscription: subscriptionManager)
            }
            .tag(0)

            NavigationStack {
                FaceScanView(manager: recoveryManager, subscription: subscriptionManager)
            }
            .tag(1)

            NavigationStack {
                WearableView(manager: recoveryManager, subscription: subscriptionManager)
            }
            .tag(2)

            NavigationStack {
                SubscriptionView(subscription: subscriptionManager)
            }
            .tag(3)
        }
        .tabViewStyle(.page)
        .task {
            await recoveryManager.requestAuthorization()
        }
    }
}

#Preview {
    ContentView()
}
