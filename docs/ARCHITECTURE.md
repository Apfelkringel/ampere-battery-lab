# Architektur

Ampere trennt inzwischen zwischen Android-Adaptern, fachlichen Battery-Regeln
und lokaler Datenhaltung.

## Datenhaltung

`BatteryDataRepository` ist die Persistence-Seam. Der aktuelle Adapter nutzt
weiterhin `SharedPreferences`, damit bestehende Installationen und Backups
kompatibel bleiben. Telemetrie wird vor jedem Lesen normalisiert, atomar als
begrenzte Historie geschrieben und nur über diese Seam ergänzt. Ein späterer
SQLite-/Room-Adapter kann dieselbe fachliche Schnittstelle bedienen.

## Sampling

`BatterySamplingPolicy` enthält die reine Zeit- und Retentionslogik. Dadurch
sind Messintervall, Rollback-Verhalten und 30-Tage-Aufbewahrung unabhängig vom
Android-Service testbar.

## Backup und Zustandsübergänge

`BatteryBackupCodec` hält die Allowlist- und Typkodierung außerhalb der
Activity. `BatteryChargingTransition` beschreibt die reine Entscheidung, wann
ein widersprüchlicher Android-Ladezustand bestätigt werden darf. Beide Module
haben kleine Interfaces und direkte Unit-Tests.

## Android-UI und Monitor

`MainActivity` bleibt der Android-Lifecycle-Adapter; `BatteryDashboard` bleibt
für den Moment der Canvas-Renderer. Die Datenzugriffe und Sampling-Regeln sind
aus ihm herausgezogen, sodass die nächste Ausbaustufe Seitenmodule und einen
deterministischen Monitor-Koordinator ergänzen kann, ohne das Speicherformat
zu verändern.

## iOS

iOS ist ein bewusst eingeschränkter Battery-basics-Modus: öffentliche Werte
werden angezeigt, nicht verfügbare Werte bleiben explizit „Nicht verfügbar“.
Der Verlauf verwendet ausschließlich echte Vordergrund-Messpunkte und erfindet
keine Strom-, Spannungs-, Kapazitäts- oder Zykluswerte.
