import Foundation
import SwiftData
import Observation

@MainActor
@Observable
final class StatsStore {
    private let modelContext: ModelContext

    var todayRows: [DailyStats] = []
    var historyRows: [DailyStats] = []

    init(modelContext: ModelContext) {
        self.modelContext = modelContext
        refresh()
    }

    var totalReelsToday: Int { todayRows.reduce(0) { $0 + $1.reelCount } }
    var totalPixelsToday: Int64 { todayRows.reduce(0) { $0 + $1.scrollPixels } }
    var totalTimeToday: Int64 { todayRows.reduce(0) { $0 + $1.timeSpentMs } }

    func row(for app: TrackedApp) -> DailyStats? {
        todayRows.first { $0.app == app }
    }

    func refresh() {
        let today = DateHelpers.today()
        let from30 = DateHelpers.daysAgo(29)
        do {
            let all = try modelContext.fetch(FetchDescriptor<DailyStats>())
            todayRows = all.filter { $0.date == today }.sorted { $0.appRaw < $1.appRaw }
            historyRows = all.filter { $0.date >= from30 }.sorted {
                if $0.date == $1.date { return $0.appRaw < $1.appRaw }
                return $0.date > $1.date
            }
        } catch {
            todayRows = []
            historyRows = []
        }
    }

    @discardableResult
    func addReel(app: TrackedApp, count: Int = 1, scrollPixels: Int64 = 0, timeMs: Int64 = 0) -> Int {
        let today = DateHelpers.today()
        let row = todayRows.first { $0.app == app } ?? {
            let created = DailyStats(date: today, app: app)
            modelContext.insert(created)
            return created
        }()
        row.reelCount += count
        row.scrollPixels += scrollPixels
        row.timeSpentMs += timeMs
        try? modelContext.save()
        refresh()
        return totalReelsToday
    }

    func addTime(app: TrackedApp, ms: Int64) {
        _ = addReel(app: app, count: 0, timeMs: ms)
    }

    func resetToday() {
        let today = DateHelpers.today()
        todayRows.filter { $0.date == today }.forEach { modelContext.delete($0) }
        try? modelContext.save()
        refresh()
    }

    func clearAll() {
        do {
            try modelContext.delete(model: DailyStats.self)
            try modelContext.save()
        } catch {}
        refresh()
    }

    func dayAggregates(range: Int) -> [(date: String, reels: Int, pixels: Int64, time: Int64)] {
        let from = DateHelpers.daysAgo(range - 1)
        var map: [String: (Int, Int64, Int64)] = [:]
        for offset in 0..<range {
            map[DateHelpers.daysAgo(offset)] = (0, 0, 0)
        }
        for row in historyRows where row.date >= from {
            var cur = map[row.date] ?? (0, 0, 0)
            cur.0 += row.reelCount
            cur.1 += row.scrollPixels
            cur.2 += row.timeSpentMs
            map[row.date] = cur
        }
        return map.keys.sorted(by: >).map { key in
            let v = map[key]!
            return (key, v.0, v.1, v.2)
        }
    }
}
