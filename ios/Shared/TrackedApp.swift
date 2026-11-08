import Foundation

enum TrackedApp: String, Codable, CaseIterable, Identifiable, Sendable {
    case instagram = "INSTAGRAM"
    case youtube = "YOUTUBE"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .instagram: return "Instagram Reels"
        case .youtube: return "YouTube Shorts"
        }
    }
}
