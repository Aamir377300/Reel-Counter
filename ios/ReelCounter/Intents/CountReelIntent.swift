import AppIntents
import SwiftData

enum TrackedAppAppEnum: String, AppEnum {
    case instagram
    case youtube

    static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "App")
    static var caseDisplayRepresentations: [TrackedAppAppEnum: DisplayRepresentation] = [
        .instagram: "Instagram Reels",
        .youtube: "YouTube Shorts"
    ]

    var tracked: TrackedApp {
        switch self {
        case .instagram: return .instagram
        case .youtube: return .youtube
        }
    }
}

/// Shortcut / Action Button / Back Tap entry point — increments today's reel count.
struct CountReelIntent: AppIntent {
    static var title: LocalizedStringResource = "Count one reel"
    static var description = IntentDescription(
        "Adds 1 to today's ReelCounter total while you scroll Reels or Shorts."
    )
    static var openAppWhenRun: Bool = false

    @Parameter(title: "App", default: .instagram)
    var app: TrackedAppAppEnum

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        let container = try ModelContainer(for: DailyStats.self)
        let store = StatsStore(modelContext: container.mainContext)
        let settings = UserSettings()
        TrackingService.shared.countReel(store: store, settings: settings, app: app.tracked)
        return .result(dialog: "Reels today: \(store.totalReelsToday)")
    }
}

struct ReelCounterShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: CountReelIntent(),
            phrases: [
                "Count a reel in \(.applicationName)",
                "Add reel with \(.applicationName)",
                "Log a Short with \(.applicationName)"
            ],
            shortTitle: "Count reel",
            systemImageName: "plus.circle.fill"
        )
    }
}
