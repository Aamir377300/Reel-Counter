import ActivityKit
import WidgetKit
import SwiftUI

struct ReelLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: ReelActivityAttributes.self) { context in
            // Lock Screen / banner
            lockScreen(context: context)
                .activityBackgroundTint(Color.black.opacity(0.85))
                .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Text("Reels")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text("\(context.state.reelCount)")
                        .font(.title2.bold())
                        .foregroundStyle(.white)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    if let msg = context.state.milestoneMessage {
                        Text(msg)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color.red.opacity(0.9))
                            .clipShape(Capsule())
                    } else {
                        Text(ScrollMetrics.formatDuration(ms: context.state.timeSpentMs))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            } compactLeading: {
                Image(systemName: "play.rectangle.fill")
            } compactTrailing: {
                Text("\(context.state.reelCount)")
                    .monospacedDigit()
                    .fontWeight(.bold)
            } minimal: {
                Text("\(context.state.reelCount)")
                    .font(.caption2.bold())
            }
        }
    }

    @ViewBuilder
    private func lockScreen(context: ActivityViewContext<ReelActivityAttributes>) -> some View {
        HStack {
            if let msg = context.state.milestoneMessage {
                Text(msg)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(Color.red.opacity(0.92))
                    .clipShape(Capsule())
            } else {
                Text("\(context.state.reelCount)")
                    .font(.title2.bold())
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color(red: 0.12, green: 0.16, blue: 0.22).opacity(0.95))
                    .clipShape(Capsule())
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(ScrollMetrics.formatMetres(ScrollMetrics.pixelsToMetres(context.state.scrollPixels)))
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.8))
                Text(ScrollMetrics.formatDuration(ms: context.state.timeSpentMs))
                    .font(.caption2)
                    .foregroundStyle(.white.opacity(0.6))
            }
        }
        .padding(16)
    }
}
