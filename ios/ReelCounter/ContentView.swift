import SwiftUI

struct ContentView: View {
    @Environment(UserSettings.self) private var settings
    @State private var showSetup = false
    @State private var tab = 0

    var body: some View {
        Group {
            if showSetup {
                SetupScreen {
                    showSetup = false
                }
            } else {
                TabView(selection: $tab) {
                    HomeScreen()
                        .tabItem { Label("Home", systemImage: "house.fill") }
                        .tag(0)
                    HistoryScreen()
                        .tabItem { Label("History", systemImage: "chart.bar.fill") }
                        .tag(1)
                    SettingsScreen()
                        .tabItem { Label("Settings", systemImage: "gearshape.fill") }
                        .tag(2)
                }
                .tint(AppTheme.teal)
            }
        }
        .onAppear {
            showSetup = !settings.setupCompleted
        }
    }
}
