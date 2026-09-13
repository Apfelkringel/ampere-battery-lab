import SwiftUI

@main
struct AmpereApp: App {
    @StateObject private var battery = BatteryStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(battery)
                .preferredColorScheme(.dark)
        }
    }
}
