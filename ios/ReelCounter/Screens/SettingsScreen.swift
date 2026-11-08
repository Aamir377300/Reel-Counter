import SwiftUI

struct SettingsScreen: View {
    @Environment(StatsStore.self) private var store
    @Environment(UserSettings.self) private var settings
    @State private var confirmReset = false
    @State private var confirmClear = false

    var body: some View {
        @Bindable var settings = settings

        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("Settings")
                    .font(.largeTitle.bold())
                    .foregroundStyle(.white)

                Text("Daily limit")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.white)
                Text("Notify once when you hit \(settings.dailyLimit) reels.")
                    .font(.footnote)
                    .foregroundStyle(AppTheme.muted)
                Slider(value: Binding(
                    get: { Double(settings.dailyLimit) },
                    set: { settings.dailyLimit = Int($0) }
                ), in: 10...500, step: 10)
                Text("\(settings.dailyLimit) reels / day")
                    .foregroundStyle(.white)

                Toggle(isOn: $settings.overlayEnabled) {
                    VStack(alignment: .leading) {
                        Text("Show Live Activity overlay")
                        Text("Dynamic Island / Lock Screen pill while tracking")
                            .font(.caption)
                            .foregroundStyle(AppTheme.muted)
                    }
                }
                .tint(AppTheme.teal)

                Text("Overlay corner preference")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.white)
                Picker("Corner", selection: $settings.overlayCorner) {
                    ForEach(OverlayCorner.allCases) { c in
                        Text(c.label).tag(c)
                    }
                }
                .pickerStyle(.segmented)

                Toggle(isOn: $settings.debugMode) {
                    VStack(alignment: .leading) {
                        Text("Debug mode")
                        Text("Extra logging in Console (tag REEL_DEBUG)")
                            .font(.caption)
                            .foregroundStyle(AppTheme.muted)
                    }
                }
                .tint(AppTheme.teal)

                Text("Data")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.white)
                Button("Reset today") { confirmReset = true }
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)
                Button("Clear all data", role: .destructive) { confirmClear = true }
                    .buttonStyle(.borderedProminent)
                    .tint(AppTheme.coral)
                    .frame(maxWidth: .infinity)
            }
            .padding(20)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .alert("Reset today's stats?", isPresented: $confirmReset) {
            Button("Reset", role: .destructive) {
                store.resetToday()
                TrackingService.shared.refreshOverlay(store: store, settings: settings)
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Deletes today's reel counts, distance, and time.")
        }
        .alert("Clear all data?", isPresented: $confirmClear) {
            Button("Clear all", role: .destructive) {
                store.clearAll()
                TrackingService.shared.refreshOverlay(store: store, settings: settings)
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Permanently deletes every day of history on this device.")
        }
    }
}
