import Foundation
import UIKit
import UserNotifications

/// Central place that increments counts, fires milestones, and refreshes the Live Activity.
@MainActor
final class TrackingService {
    static let shared = TrackingService()

    private init() {}

    private let milestoneStart = 100
    private let milestoneStep = 50

    func countReel(
        store: StatsStore,
        settings: UserSettings,
        app: TrackedApp? = nil,
        scrollPixels: Int64 = 1800
    ) {
        let tracked = app ?? settings.activeApp
        let total = store.addReel(app: tracked, count: 1, scrollPixels: scrollPixels)

        maybeMilestone(total: total, settings: settings)
        maybeDailyLimit(total: total, settings: settings)
        refreshOverlay(store: store, settings: settings)
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }

    func refreshOverlay(store: StatsStore, settings: UserSettings, milestone: String? = nil) {
        guard settings.sessionActive || settings.overlayEnabled else {
            LiveOverlayController.startOrUpdate(
                reelCount: store.totalReelsToday,
                scrollPixels: store.totalPixelsToday,
                timeSpentMs: store.totalTimeToday,
                enabled: false
            )
            return
        }
        LiveOverlayController.startOrUpdate(
            reelCount: store.totalReelsToday,
            scrollPixels: store.totalPixelsToday,
            timeSpentMs: store.totalTimeToday,
            milestoneMessage: milestone,
            enabled: settings.overlayEnabled
        )
    }

    func startSession(store: StatsStore, settings: UserSettings, app: TrackedApp) {
        settings.activeApp = app
        settings.sessionActive = true
        refreshOverlay(store: store, settings: settings)
    }

    func stopSession(settings: UserSettings) {
        settings.sessionActive = false
        Task { await LiveOverlayController.endAll() }
    }

    private func maybeMilestone(total: Int, settings: UserSettings) {
        guard total >= milestoneStart else { return }
        let milestone = (total / milestoneStep) * milestoneStep
        guard milestone >= milestoneStart, total == milestone else { return }

        let today = DateHelpers.today()
        let last = settings.milestoneDate == today ? settings.lastMilestoneShown : 0
        guard milestone > last else { return }

        settings.milestoneDate = today
        settings.lastMilestoneShown = milestone
        UINotificationFeedbackGenerator().notificationOccurred(.warning)
        notify(
            title: "You have reached \(milestone) reels",
            body: "Take a quick break?"
        )
    }

    private func maybeDailyLimit(total: Int, settings: UserSettings) {
        guard total >= settings.dailyLimit else { return }
        let today = DateHelpers.today()
        guard settings.limitAlertDate != today else { return }
        settings.limitAlertDate = today
        UINotificationFeedbackGenerator().notificationOccurred(.warning)
        notify(
            title: "Daily reel limit reached",
            body: "You've hit your daily limit of \(settings.dailyLimit) reels."
        )
    }

    private func notify(title: String, body: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let req = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(req)
    }
}
