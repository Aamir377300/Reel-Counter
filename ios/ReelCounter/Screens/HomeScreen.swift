import SwiftUI

struct HomeScreen: View {
    @Environment(StatsStore.self) private var store
    @Environment(UserSettings.self) private var settings

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Today")
                    .font(.largeTitle.bold())
                    .foregroundStyle(.white)
                Text("Reels & Shorts you've scrolled today")
                    .foregroundStyle(AppTheme.muted)

                sessionCard

                bigStat(label: "TOTAL REELS", value: "\(store.totalReelsToday)")

                HStack(spacing: 12) {
                    smallStat(
                        label: "Distance",
                        value: ScrollMetrics.formatMetres(
                            ScrollMetrics.pixelsToMetres(store.totalPixelsToday)
                        )
                    )
                    smallStat(
                        label: "Time",
                        value: ScrollMetrics.formatDuration(ms: store.totalTimeToday)
                    )
                }

                Text("By app")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.white)
                    .padding(.top, 8)

                appRow(.instagram)
                appRow(.youtube)
            }
            .padding(20)
        }
        .background(AppTheme.background.ignoresSafeArea())
    }

    private var sessionCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(settings.sessionActive ? "Tracking session on" : "Start a tracking session")
                .font(.headline)
                .foregroundStyle(.white)
            Text("iOS can't read Instagram like Android Accessibility. Start a session, open Reels/Shorts, then use Back Tap / Action Button / the + button to count.")
                .font(.footnote)
                .foregroundStyle(AppTheme.muted)

            HStack(spacing: 8) {
                ForEach(TrackedApp.allCases) { app in
                    Button(app == .instagram ? "Instagram" : "YouTube") {
                        TrackingService.shared.startSession(store: store, settings: settings, app: app)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(settings.activeApp == app && settings.sessionActive ? AppTheme.teal : AppTheme.card)
                }

                if settings.sessionActive {
                    Button("Stop") {
                        TrackingService.shared.stopSession(settings: settings)
                    }
                    .buttonStyle(.bordered)
                }
            }

            Button {
                TrackingService.shared.countReel(store: store, settings: settings)
            } label: {
                Label("Count 1 reel", systemImage: "plus.circle.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppTheme.coral)
            .disabled(!settings.sessionActive)
        }
        .padding(16)
        .background(AppTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func bigStat(label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.white.opacity(0.8))
            Text(value)
                .font(.system(size: 48, weight: .bold))
                .foregroundStyle(.white)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(24)
        .background(AppTheme.teal)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func smallStat(label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.caption)
                .foregroundStyle(AppTheme.muted)
            Text(value)
                .font(.title2.weight(.semibold))
                .foregroundStyle(.white)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(AppTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func appRow(_ app: TrackedApp) -> some View {
        let row = store.row(for: app)
        return HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(app.displayName)
                    .font(.headline)
                    .foregroundStyle(.white)
                Text(
                    "\(ScrollMetrics.formatDuration(ms: row?.timeSpentMs ?? 0)) · \(ScrollMetrics.formatMetres(ScrollMetrics.pixelsToMetres(row?.scrollPixels ?? 0)))"
                )
                .font(.caption)
                .foregroundStyle(AppTheme.muted)
            }
            Spacer()
            Text("\(row?.reelCount ?? 0)")
                .font(.title.bold())
                .foregroundStyle(AppTheme.teal)
        }
        .padding(16)
        .background(AppTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}
