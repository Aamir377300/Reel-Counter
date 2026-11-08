import Foundation
import SwiftData

@Model
final class DailyStats {
    var date: String
    var appRaw: String
    var reelCount: Int
    var scrollPixels: Int64
    var timeSpentMs: Int64

    var app: TrackedApp {
        get { TrackedApp(rawValue: appRaw) ?? .instagram }
        set { appRaw = newValue.rawValue }
    }

    init(
        date: String,
        app: TrackedApp,
        reelCount: Int = 0,
        scrollPixels: Int64 = 0,
        timeSpentMs: Int64 = 0
    ) {
        self.date = date
        self.appRaw = app.rawValue
        self.reelCount = reelCount
        self.scrollPixels = scrollPixels
        self.timeSpentMs = timeSpentMs
    }
}
