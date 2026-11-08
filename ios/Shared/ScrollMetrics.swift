import Foundation

enum ScrollMetrics {
    /// Approximate metres from absolute scroll pixels (same idea as Android dpi conversion).
    static func pixelsToMetres(_ pixels: Int64, dpi: Double = 460) -> Double {
        guard dpi > 0 else { return 0 }
        return abs(Double(pixels)) / dpi * 0.0254
    }

    static func formatMetres(_ metres: Double) -> String {
        if metres < 10 {
            return String(format: "%.1f m", metres)
        }
        return String(format: "%.0f m", metres)
    }

    static func formatDuration(ms: Int64) -> String {
        let totalSec = max(0, ms / 1000)
        let hours = totalSec / 3600
        let minutes = (totalSec % 3600) / 60
        if hours > 0 { return "\(hours)h \(minutes)m" }
        if minutes > 0 { return "\(minutes) min" }
        return "\(totalSec)s"
    }
}
