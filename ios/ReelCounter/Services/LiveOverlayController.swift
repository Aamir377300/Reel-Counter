import ActivityKit
import Foundation

/// Starts / updates the Live Activity that mirrors Android's floating count pill.
@MainActor
enum LiveOverlayController {
    static func startOrUpdate(
        reelCount: Int,
        scrollPixels: Int64,
        timeSpentMs: Int64,
        milestoneMessage: String? = nil,
        enabled: Bool
    ) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }

        let state = ReelActivityAttributes.ContentState(
            reelCount: reelCount,
            scrollPixels: scrollPixels,
            timeSpentMs: timeSpentMs,
            milestoneMessage: milestoneMessage
        )

        if !enabled {
            Task { await endAll() }
            return
        }

        if let existing = Activity<ReelActivityAttributes>.activities.first {
            Task {
                await existing.update(ActivityContent(state: state, staleDate: nil))
            }
            return
        }

        do {
            _ = try Activity.request(
                attributes: ReelActivityAttributes(appName: "ReelCounter"),
                content: ActivityContent(state: state, staleDate: nil),
                pushType: nil
            )
        } catch {
            // Live Activities may be disabled in Focus / Low Power.
        }
    }

    static func endAll() async {
        for activity in Activity<ReelActivityAttributes>.activities {
            await activity.end(nil, dismissalPolicy: .immediate)
        }
    }
}
