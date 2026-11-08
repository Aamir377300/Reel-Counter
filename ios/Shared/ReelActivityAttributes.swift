import ActivityKit
import Foundation

/// Live Activity payload — the iOS stand-in for Android's accessibility overlay pill.
struct ReelActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable, Sendable {
        var reelCount: Int
        var scrollPixels: Int64
        var timeSpentMs: Int64
        var milestoneMessage: String?

        init(
            reelCount: Int,
            scrollPixels: Int64 = 0,
            timeSpentMs: Int64 = 0,
            milestoneMessage: String? = nil
        ) {
            self.reelCount = reelCount
            self.scrollPixels = scrollPixels
            self.timeSpentMs = timeSpentMs
            self.milestoneMessage = milestoneMessage
        }
    }

    var appName: String

    init(appName: String = "ReelCounter") {
        self.appName = appName
    }
}
