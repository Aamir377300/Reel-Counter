import SwiftUI
import UserNotifications

struct SetupScreen: View {
    @Environment(UserSettings.self) private var settings
    var onFinished: () -> Void

    @State private var notificationsGranted = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Set up ReelCounter")
                    .font(.largeTitle.bold())
                    .foregroundStyle(.white)
                Text("iPhone can't inject an overlay into Instagram like Android Accessibility. Use Live Activity + Back Tap / Shortcuts instead — same counting result.")
                    .foregroundStyle(AppTheme.muted)

                checklistCard(
                    title: "1. Allow notifications",
                    detail: "Needed for daily-limit and milestone alerts.",
                    done: notificationsGranted
                ) {
                    Task {
                        let center = UNUserNotificationCenter.current()
                        notificationsGranted = (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
                    }
                }

                checklistCard(
                    title: "2. Enable Live Activities",
                    detail: "Settings → Face ID & Passcode / Screen Time aren't needed. Go to Settings → ReelCounter → Live Activities → On (or Settings → Face ID… / Control Center related Live Activities).",
                    done: true
                ) {
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        UIApplication.shared.open(url)
                    }
                }

                infoCard(
                    title: "3. Assign Back Tap (best counter)",
                    body: "Settings → Accessibility → Touch → Back Tap → Double Tap → choose Shortcut \"Count a reel\" (or ReelCounter). Each double-tap on the back of the phone counts 1 reel while you scroll."
                )

                infoCard(
                    title: "4. Or Action Button / Shortcuts",
                    body: "Create Shortcut: Apps → ReelCounter → Count one reel. Bind it to Action Button or Control Center for one-tap counting over Instagram/YouTube."
                )

                infoCard(
                    title: "5. How a session works",
                    body: "Open ReelCounter → Start Instagram/YouTube session → switch to Reels/Shorts → count with Back Tap or the in-app + button. Live Activity shows today's number (the overlay)."
                )

                Button {
                    settings.setupCompleted = true
                    onFinished()
                } label: {
                    Text(notificationsGranted ? "Continue" : "Continue anyway")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppTheme.teal)
                .padding(.top, 8)
            }
            .padding(24)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .task {
            let settings = await UNUserNotificationCenter.current().notificationSettings()
            notificationsGranted = settings.authorizationStatus == .authorized
        }
    }

    private func checklistCard(title: String, detail: String, done: Bool, action: @escaping () -> Void) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                Image(systemName: done ? "checkmark.circle.fill" : "xmark.circle.fill")
                    .foregroundStyle(done ? AppTheme.teal : .red)
                VStack(alignment: .leading, spacing: 4) {
                    Text(title).font(.headline).foregroundStyle(.white)
                    Text(detail).font(.footnote).foregroundStyle(AppTheme.muted)
                }
            }
            Button(done ? "Open settings again" : "Enable", action: action)
                .buttonStyle(.bordered)
                .frame(maxWidth: .infinity)
        }
        .padding(16)
        .background(AppTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func infoCard(title: String, body: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.headline).foregroundStyle(.white)
            Text(body).font(.footnote).foregroundStyle(AppTheme.muted)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}
