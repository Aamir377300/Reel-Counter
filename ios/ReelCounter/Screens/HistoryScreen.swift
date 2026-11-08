import SwiftUI

struct HistoryScreen: View {
    @Environment(StatsStore.self) private var store
    @State private var range = 7

    var body: some View {
        let days = store.dayAggregates(range: range)

        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 12) {
                Text("History")
                    .font(.largeTitle.bold())
                    .foregroundStyle(.white)

                Picker("Range", selection: $range) {
                    Text("7 days").tag(7)
                    Text("30 days").tag(30)
                }
                .pickerStyle(.segmented)

                BarChartView(days: days.reversed())
                    .frame(height: 180)
                    .padding(12)
                    .background(AppTheme.card)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)

            List {
                ForEach(days, id: \.date) { day in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(DateHelpers.formatDisplay(day.date))
                                .foregroundStyle(.white)
                            Text(
                                "\(ScrollMetrics.formatMetres(ScrollMetrics.pixelsToMetres(day.pixels))) · \(ScrollMetrics.formatDuration(ms: day.time))"
                            )
                            .font(.caption)
                            .foregroundStyle(AppTheme.muted)
                        }
                        Spacer()
                        Text("\(day.reels) reels")
                            .foregroundStyle(AppTheme.teal)
                            .fontWeight(.medium)
                    }
                    .listRowBackground(AppTheme.card)
                }
            }
            .scrollContentBackground(.hidden)
        }
        .background(AppTheme.background.ignoresSafeArea())
    }
}

struct BarChartView: View {
    let days: [(date: String, reels: Int, pixels: Int64, time: Int64)]

    var body: some View {
        GeometryReader { geo in
            let maxVal = max(days.map(\.reels).max() ?? 1, 1)
            let gap: CGFloat = 4
            let barWidth = max(2, (geo.size.width - gap * CGFloat(max(days.count - 1, 0))) / CGFloat(max(days.count, 1)))

            HStack(alignment: .bottom, spacing: gap) {
                ForEach(Array(days.enumerated()), id: \.offset) { _, day in
                    let h = max(2, geo.size.height * CGFloat(day.reels) / CGFloat(maxVal))
                    RoundedRectangle(cornerRadius: 6)
                        .fill(AppTheme.teal)
                        .frame(width: barWidth, height: h)
                }
            }
            .frame(maxHeight: .infinity, alignment: .bottom)
        }
    }
}
