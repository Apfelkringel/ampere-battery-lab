import Foundation
import Combine
import UIKit

struct BatterySample: Codable, Identifiable, Equatable {
    let id: UUID
    let date: Date
    let level: Double
    let state: UIDevice.BatteryState.RawValue

    init(date: Date = .now, level: Double, state: UIDevice.BatteryState) {
        self.id = UUID()
        self.date = date
        self.level = level
        self.state = state.rawValue
    }
}

@MainActor
final class BatteryStore: ObservableObject {
    @Published private(set) var level: Double?
    @Published private(set) var state: UIDevice.BatteryState = .unknown
    @Published private(set) var lastUpdated: Date?
    @Published private(set) var samples: [BatterySample] = []

    private let device = UIDevice.current
    private let storageKey = "ampere.ios.battery-samples.v1"
    private var observerTokens: [NSObjectProtocol] = []
    private var timer: Timer?

    init() {
        samples = loadSamples()
        device.isBatteryMonitoringEnabled = true
        observerTokens = [
            NotificationCenter.default.addObserver(
                forName: UIDevice.batteryLevelDidChangeNotification,
                object: device,
                queue: .main
            ) { [weak self] _ in Task { @MainActor in self?.refresh() } },
            NotificationCenter.default.addObserver(
                forName: UIDevice.batteryStateDidChangeNotification,
                object: device,
                queue: .main
            ) { [weak self] _ in Task { @MainActor in self?.refresh() } }
        ]
        refresh()
    }

    deinit {
        observerTokens.forEach(NotificationCenter.default.removeObserver)
        timer?.invalidate()
    }

    func startSampling() {
        refresh()
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 60, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.refresh() }
        }
    }

    func stopSampling() {
        timer?.invalidate()
        timer = nil
    }

    func refresh() {
        let rawLevel = device.batteryLevel
        let rawState = device.batteryState
        level = rawLevel >= 0 ? Double(rawLevel) : nil
        state = rawState
        lastUpdated = .now
        guard let level else { return }
        let sample = BatterySample(level: level, state: rawState)
        let periodicSampleDue = samples.last.map { sample.date.timeIntervalSince($0.date) >= 300 } ?? true
        if periodicSampleDue || samples.last?.level != sample.level || samples.last?.state != sample.state {
            samples.append(sample)
            samples = Array(samples.suffix(720))
            saveSamples()
        }
    }

    var statusSummary: String {
        "Ampere Battery Lab\nAkkustand: \(levelText)\nStatus: \(statusTitle)\nLetzte Messung: \(lastUpdatedText)\nNur lokale iOS-Werte; Strom, Spannung und Gesundheit sind öffentlich nicht verfügbar."
    }

    var isCharging: Bool {
        state == .charging || state == .full
    }

    var statusTitle: String {
        switch state {
        case .charging: return "Laden"
        case .full: return "Voll geladen"
        case .unplugged: return "Akkubetrieb"
        default: return "Status unbekannt"
        }
    }

    var levelText: String {
        guard let level else { return "—" }
        return "\(Int((level * 100).rounded()))%"
    }

    var lastUpdatedText: String {
        guard let lastUpdated else { return "Noch keine Messung" }
        return lastUpdated.formatted(date: .omitted, time: .shortened)
    }

    private func loadSamples() -> [BatterySample] {
        guard let data = UserDefaults.standard.data(forKey: storageKey),
              let decoded = try? JSONDecoder().decode([BatterySample].self, from: data) else {
            return []
        }
        return Array(decoded.sorted { $0.date < $1.date }.suffix(720))
    }

    private func saveSamples() {
        guard let data = try? JSONEncoder().encode(samples) else { return }
        UserDefaults.standard.set(data, forKey: storageKey)
    }
}
