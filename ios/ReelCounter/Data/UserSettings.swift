import Foundation
import Observation

enum OverlayCorner: String, CaseIterable, Identifiable {
    case topLeft
    case topRight
    var id: String { rawValue }
    var label: String {
        switch self {
        case .topLeft: return "Top left"
        case .topRight: return "Top right"
        }
    }
}

@MainActor
@Observable
final class UserSettings {
    private let defaults = UserDefaults.standard

    var dailyLimit: Int {
        didSet { defaults.set(dailyLimit, forKey: Keys.dailyLimit) }
    }
    var overlayEnabled: Bool {
        didSet { defaults.set(overlayEnabled, forKey: Keys.overlayEnabled) }
    }
    var overlayCorner: OverlayCorner {
        didSet { defaults.set(overlayCorner.rawValue, forKey: Keys.overlayCorner) }
    }
    var debugMode: Bool {
        didSet { defaults.set(debugMode, forKey: Keys.debugMode) }
    }
    var setupCompleted: Bool {
        didSet { defaults.set(setupCompleted, forKey: Keys.setupCompleted) }
    }
    var lastMilestoneShown: Int {
        didSet { defaults.set(lastMilestoneShown, forKey: Keys.lastMilestone) }
    }
    var milestoneDate: String {
        didSet { defaults.set(milestoneDate, forKey: Keys.milestoneDate) }
    }
    var limitAlertDate: String {
        didSet { defaults.set(limitAlertDate, forKey: Keys.limitAlertDate) }
    }
    var activeApp: TrackedApp {
        didSet { defaults.set(activeApp.rawValue, forKey: Keys.activeApp) }
    }
    var sessionActive: Bool {
        didSet { defaults.set(sessionActive, forKey: Keys.sessionActive) }
    }

    init() {
        dailyLimit = defaults.object(forKey: Keys.dailyLimit) as? Int ?? 100
        overlayEnabled = defaults.object(forKey: Keys.overlayEnabled) as? Bool ?? true
        overlayCorner = OverlayCorner(rawValue: defaults.string(forKey: Keys.overlayCorner) ?? "") ?? .topRight
        debugMode = defaults.bool(forKey: Keys.debugMode)
        setupCompleted = defaults.bool(forKey: Keys.setupCompleted)
        lastMilestoneShown = defaults.integer(forKey: Keys.lastMilestone)
        milestoneDate = defaults.string(forKey: Keys.milestoneDate) ?? ""
        limitAlertDate = defaults.string(forKey: Keys.limitAlertDate) ?? ""
        activeApp = TrackedApp(rawValue: defaults.string(forKey: Keys.activeApp) ?? "") ?? .instagram
        sessionActive = defaults.bool(forKey: Keys.sessionActive)
    }

    private enum Keys {
        static let dailyLimit = "daily_limit"
        static let overlayEnabled = "overlay_enabled"
        static let overlayCorner = "overlay_corner"
        static let debugMode = "debug_mode"
        static let setupCompleted = "setup_completed"
        static let lastMilestone = "last_milestone"
        static let milestoneDate = "milestone_date"
        static let limitAlertDate = "limit_alert_date"
        static let activeApp = "active_app"
        static let sessionActive = "session_active"
    }
}
