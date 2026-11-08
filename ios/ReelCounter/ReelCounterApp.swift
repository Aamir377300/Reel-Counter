import SwiftUI
import SwiftData

@main
struct ReelCounterApp: App {
    @State private var settings = UserSettings()
    private let container: ModelContainer

    init() {
        do {
            container = try ModelContainer(for: DailyStats.self)
        } catch {
            fatalError("Failed to open SwiftData store: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            RootView(settings: settings, container: container)
        }
        .modelContainer(container)
    }
}

private struct RootView: View {
    @Bindable var settings: UserSettings
    let container: ModelContainer
    @State private var store: StatsStore?

    var body: some View {
        Group {
            if let store {
                ContentView()
                    .environment(settings)
                    .environment(store)
            } else {
                ProgressView()
                    .tint(AppTheme.teal)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(AppTheme.background)
            }
        }
        .preferredColorScheme(.dark)
        .onAppear {
            if store == nil {
                store = StatsStore(modelContext: container.mainContext)
            }
        }
    }
}
