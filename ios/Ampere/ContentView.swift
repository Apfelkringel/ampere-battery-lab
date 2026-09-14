import SwiftUI

private enum AmperePalette {
    static let background = Color(red: 0.018, green: 0.155, blue: 0.17)
    static let surface = Color(red: 0.028, green: 0.235, blue: 0.245)
    static let surfaceRaised = Color(red: 0.035, green: 0.29, blue: 0.30)
    static let border = Color(red: 0.10, green: 0.52, blue: 0.51)
    static let primary = Color(red: 0.98, green: 0.97, blue: 0.90)
    static let muted = Color(red: 0.62, green: 0.82, blue: 0.79)
    static let accent = Color(red: 0.21, green: 0.83, blue: 0.78)
}

struct RootView: View {
    @EnvironmentObject private var battery: BatteryStore
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        TabView {
            OverviewView().tabItem { Label("Start", systemImage: "gauge.with.dots.needle.67percent") }
            ChargeView().tabItem { Label("Laden", systemImage: "bolt.fill") }
            DischargeView().tabItem { Label("Entladen", systemImage: "arrow.down") }
            HealthView().tabItem { Label("Akku", systemImage: "heart") }
            HistoryView().tabItem { Label("Verlauf", systemImage: "chart.xyaxis.line") }
        }
        .tint(AmperePalette.accent)
        .background(AmperePalette.background.ignoresSafeArea())
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { battery.startSampling() } else { battery.stopSampling() }
        }
    }
}

private struct AppHeader: View {
    @EnvironmentObject private var battery: BatteryStore

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(AmperePalette.accent)
                Image(systemName: "bolt.fill").foregroundStyle(AmperePalette.background)
            }
            .frame(width: 38, height: 38)
            VStack(alignment: .leading, spacing: 2) {
                Text("Ampere").font(.system(.headline, design: .rounded).weight(.bold))
                Text("BATTERY LAB · iOS").font(.system(size: 9, weight: .semibold, design: .rounded))
                    .tracking(1.1).foregroundStyle(AmperePalette.muted)
            }
            Spacer()
            HStack(spacing: 6) {
                Circle().fill(AmperePalette.accent).frame(width: 7, height: 7)
                Text(battery.statusTitle).font(.system(size: 11, weight: .semibold, design: .rounded))
            }
            .foregroundStyle(AmperePalette.accent)
            .padding(.horizontal, 10).padding(.vertical, 7)
            .background(AmperePalette.surface, in: Capsule())
            .overlay(Capsule().stroke(AmperePalette.border.opacity(0.8), lineWidth: 1))
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Ampere, \(battery.statusTitle), \(battery.levelText)")
    }
}

private struct PageShell<Content: View>: View {
    let title: String
    let content: Content

    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                AppHeader()
                Text(title).font(.system(size: 30, weight: .bold, design: .serif)).foregroundStyle(AmperePalette.primary)
                content
            }
            .padding(.horizontal, 18).padding(.top, 14).padding(.bottom, 28)
        }
        .scrollIndicators(.hidden)
        .background(AmperePalette.background.ignoresSafeArea())
    }
}

struct OverviewView: View {
    @EnvironmentObject private var battery: BatteryStore

    var body: some View {
        PageShell(title: "Übersicht") {
            VStack(alignment: .leading, spacing: 18) {
                Panel {
                    HStack(spacing: 20) {
                        BatteryGauge(level: battery.level, charging: battery.isCharging)
                        VStack(alignment: .leading, spacing: 7) {
                            Text(battery.statusTitle.uppercased()).font(.system(size: 11, weight: .bold, design: .rounded)).foregroundStyle(AmperePalette.accent)
                            Text(battery.levelText).font(.system(size: 42, weight: .bold, design: .rounded)).foregroundStyle(AmperePalette.primary)
                            Text("Direkt von iOS gelesen · aktualisiert \(battery.lastUpdatedText)")
                                .font(.system(size: 11, design: .rounded)).foregroundStyle(AmperePalette.muted).fixedSize(horizontal: false, vertical: true)
                        }
                        Spacer(minLength: 0)
                    }
                }
                Panel {
                    SectionHeading(eyebrow: "AKKUGESUNDHEIT", title: "Noch nicht verfügbar")
                    Text("iOS stellt keine gemessene Vollkapazität oder Ladezyklen für Drittanbieter-Apps bereit. Ampere zeigt hier deshalb keine erfundenen Werte.")
                        .font(.system(size: 13, design: .rounded)).foregroundStyle(AmperePalette.muted).fixedSize(horizontal: false, vertical: true)
                    MetricGrid(items: [("Kapazität", "Nicht verfügbar"), ("Verschleiß", "Nicht verfügbar"), ("Zyklen", "Nicht verfügbar"), ("Quelle", "Apple UIDevice")])
                }
                Panel {
                    SectionHeading(eyebrow: "IOS-GRENZEN", title: "Was lokal messbar ist")
                    Text("iOS liefert Akkustand, Ladezustand und Ereignisse. Strom, Spannung, Vollkapazität und Zyklen werden Drittanbieter-Apps nicht öffentlich bereitgestellt.")
                        .font(.system(size: 13, design: .rounded)).foregroundStyle(AmperePalette.muted).fixedSize(horizontal: false, vertical: true)
                    Text("Ampere zeichnet deshalb nur echte Vordergrund-Messpunkte auf und kennzeichnet fehlende Werte ausdrücklich.")
                        .font(.system(size: 12, design: .rounded)).foregroundStyle(AmperePalette.primary).fixedSize(horizontal: false, vertical: true)
                }
                MetricGrid(items: [("Akkustand", battery.levelText), ("Status", battery.statusTitle), ("Spannung", "Nicht verfügbar"), ("Strom", "Nicht verfügbar")])
                ShareLink(item: battery.statusSummary, subject: Text("Ampere-Akkustatus")) {
                    Label("Status teilen", systemImage: "square.and.arrow.up")
                        .font(.system(size: 13, weight: .semibold, design: .rounded))
                        .foregroundStyle(AmperePalette.background)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(AmperePalette.accent, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
            }
        }
    }
}

struct ChargeView: View {
    @EnvironmentObject private var battery: BatteryStore
    var body: some View {
        PageShell(title: "Laden") {
            Panel {
                SectionHeading(eyebrow: "LADEVORGANG", title: battery.isCharging ? "Laden erkannt" : "Nicht aktiv")
                HStack(alignment: .firstTextBaseline) {
                    Text(battery.levelText).font(.system(size: 40, weight: .bold, design: .rounded)).foregroundStyle(AmperePalette.primary)
                    Text("Akkustand").font(.system(size: 13, design: .rounded)).foregroundStyle(AmperePalette.muted)
                    Spacer()
                    Image(systemName: battery.isCharging ? "bolt.fill" : "bolt.slash").font(.title2).foregroundStyle(AmperePalette.accent)
                }
                Divider().overlay(AmperePalette.border)
                InfoRow(label: "Quelle", value: battery.isCharging ? "Netzteil · von iOS gemeldet" : "Nicht angeschlossen")
                InfoRow(label: "Ladestrom", value: "Nicht verfügbar auf iOS")
            }
        }
    }
}

struct DischargeView: View {
    @EnvironmentObject private var battery: BatteryStore
    var body: some View {
        PageShell(title: "Entladen") {
            Panel {
                SectionHeading(eyebrow: "AKKUBETRIEB", title: battery.isCharging ? "Nicht aktiv" : "Akkubetrieb")
                Text(battery.levelText).font(.system(size: 44, weight: .bold, design: .rounded)).foregroundStyle(AmperePalette.primary)
                Text("Der aktuelle Akkustand wird von iOS geliefert. Leistungsaufnahme und Laufzeit bleiben ohne öffentliche Strommessung offen.")
                    .font(.system(size: 13, design: .rounded)).foregroundStyle(AmperePalette.muted).fixedSize(horizontal: false, vertical: true)
                Divider().overlay(AmperePalette.border)
                InfoRow(label: "Leistungsaufnahme", value: "Nicht verfügbar auf iOS")
                InfoRow(label: "Restlaufzeit", value: "Nicht berechnet")
            }
        }
    }
}

struct HealthView: View {
    var body: some View {
        PageShell(title: "Akkugesundheit") {
            Panel {
                SectionHeading(eyebrow: "AKKUGESUNDHEIT", title: "Messung nicht möglich")
                Text("—").font(.system(size: 52, weight: .bold, design: .rounded)).foregroundStyle(AmperePalette.primary)
                Text("Apple veröffentlicht für Drittanbieter-Apps keinen Messwert für Designkapazität, Vollkapazität oder Verschleiß.")
                    .font(.system(size: 13, design: .rounded)).foregroundStyle(AmperePalette.muted).fixedSize(horizontal: false, vertical: true)
                MetricGrid(items: [("Designkapazität", "Nicht verfügbar"), ("Gemessene Kapazität", "Nicht verfügbar"), ("Akkuzustand", "—"), ("Datenbasis", "Apple UIDevice")])
            }
        }
    }
}

struct HistoryView: View {
    @EnvironmentObject private var battery: BatteryStore
    @State private var showingClearConfirmation = false
    var body: some View {
        PageShell(title: "Verlauf") {
            Panel {
                SectionHeading(eyebrow: "AKKUSTAND", title: battery.samples.isEmpty ? "Noch keine Messpunkte" : "Lokaler Verlauf")
                if battery.samples.isEmpty {
                    Text("Öffne Ampere regelmäßig, damit iOS-Akkustände lokal aufgezeichnet werden können.")
                        .font(.system(size: 13, design: .rounded)).foregroundStyle(AmperePalette.muted).fixedSize(horizontal: false, vertical: true)
                } else {
                    BatteryHistoryChart(samples: battery.samples)
                        .frame(height: 180)
                    Text("\(battery.samples.count) echte iOS-Messpunkte · keine künstliche Auffüllung")
                        .font(.system(size: 10, design: .rounded)).foregroundStyle(AmperePalette.muted)
                    Button("Lokalen Verlauf löschen", role: .destructive) {
                        showingClearConfirmation = true
                    }
                    .font(.system(size: 12, weight: .semibold, design: .rounded))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .confirmationDialog("Lokalen Verlauf löschen?", isPresented: $showingClearConfirmation, titleVisibility: .visible) {
                        Button("Verlauf löschen", role: .destructive) { battery.clearHistory() }
                        Button("Abbrechen", role: .cancel) { }
                    } message: {
                        Text("Alle lokal gespeicherten iOS-Messpunkte werden entfernt.")
                    }
                }
            }
        }
    }
}

private struct BatteryGauge: View {
    let level: Double?
    let charging: Bool
    var body: some View {
        ZStack {
            Circle().stroke(AmperePalette.border.opacity(0.55), lineWidth: 9)
            Circle().trim(from: 0, to: CGFloat(max(0, min(1, level ?? 0))))
                .stroke(AmperePalette.accent, style: StrokeStyle(lineWidth: 9, lineCap: .round))
                .rotationEffect(.degrees(-90))
            Image(systemName: charging ? "bolt.fill" : "battery.75percent")
                .font(.title3.weight(.bold)).foregroundStyle(AmperePalette.accent)
        }
        .frame(width: 92, height: 92)
        .accessibilityLabel("Akkustand \(level.map { "\(Int(($0 * 100).rounded())) Prozent" } ?? "nicht verfügbar")")
    }
}

private struct BatteryHistoryChart: View {
    let samples: [BatterySample]
    var body: some View {
        GeometryReader { proxy in
            let points = samples.suffix(120)
            let minDate = points.first?.date ?? .now
            let maxDate = points.last?.date ?? minDate.addingTimeInterval(1)
            let span = max(maxDate.timeIntervalSince(minDate), 1)
            let width = proxy.size.width
            let height = proxy.size.height
            ZStack {
                VStack(spacing: 0) {
                    ForEach(0..<4, id: \.self) { _ in Divider().overlay(AmperePalette.border.opacity(0.35)); Spacer() }
                }
                Path { path in
                    for (index, sample) in points.enumerated() {
                        let x = width * sample.date.timeIntervalSince(minDate) / span
                        let y = height * (1 - sample.level)
                        if index == 0 { path.move(to: CGPoint(x: x, y: y)) }
                        else { path.addLine(to: CGPoint(x: x, y: y)) }
                    }
                }
                .stroke(AmperePalette.accent, style: StrokeStyle(lineWidth: 2.5, lineJoin: .round))
            }
        }
    }
}

private struct Panel<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 14) { content }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AmperePalette.surface, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(AmperePalette.border.opacity(0.8), lineWidth: 1))
    }
}

private struct SectionHeading: View {
    let eyebrow: String
    let title: String
    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(eyebrow).font(.system(size: 10, weight: .bold, design: .rounded)).tracking(1.1).foregroundStyle(AmperePalette.accent)
            Text(title).font(.system(size: 21, weight: .bold, design: .rounded)).foregroundStyle(AmperePalette.primary)
        }
    }
}

private struct InfoRow: View {
    let label: String
    let value: String
    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text(label).font(.system(size: 12, design: .rounded)).foregroundStyle(AmperePalette.muted)
            Spacer(minLength: 8)
            Text(value).font(.system(size: 12, weight: .semibold, design: .rounded)).foregroundStyle(AmperePalette.primary).multilineTextAlignment(.trailing)
        }
    }
}

private struct MetricGrid: View {
    let items: [(String, String)]
    var body: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.0).font(.system(size: 10, design: .rounded)).foregroundStyle(AmperePalette.muted)
                    Text(item.1).font(.system(size: 13, weight: .semibold, design: .rounded)).foregroundStyle(AmperePalette.primary).lineLimit(2).minimumScaleFactor(0.8)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(11)
                .background(AmperePalette.surfaceRaised.opacity(0.55), in: RoundedRectangle(cornerRadius: 13, style: .continuous))
            }
        }
    }
}
