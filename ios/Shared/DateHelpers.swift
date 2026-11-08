import Foundation

enum DateHelpers {
    private static let iso: DateFormatter = {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = .current
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    static func today() -> String {
        iso.string(from: Date())
    }

    static func daysAgo(_ days: Int) -> String {
        let date = Calendar.current.date(byAdding: .day, value: -days, to: Date()) ?? Date()
        return iso.string(from: date)
    }

    static func formatDisplay(_ isoDate: String) -> String {
        guard let date = iso.date(from: isoDate) else { return isoDate }
        let f = DateFormatter()
        f.dateStyle = .medium
        return f.string(from: date)
    }

    static func formatShort(_ isoDate: String) -> String {
        guard let date = iso.date(from: isoDate) else { return isoDate }
        let f = DateFormatter()
        f.dateFormat = "MMM d"
        return f.string(from: date)
    }
}
