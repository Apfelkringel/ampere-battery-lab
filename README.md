# Ampere Battery Lab

Kostenlose, lokal arbeitende Android-Batterieanalyse als eigenständige Implementierung.

## APK installieren

1. `Ampere-Battery-Lab-release.apk` auf das Android-Handy kopieren.
2. Die Datei öffnen und – falls Android fragt – die Installation aus dieser Quelle erlauben.
3. Beim ersten Start Benachrichtigungen erlauben, damit der lokale Hintergrundmonitor und der Ladealarm funktionieren.
4. Für die App-Nutzungsanzeige im Tab „Drain“ den Android-Zugriff auf Nutzungsdaten freigeben.

## Enthalten

- Live-Ladezustand, Strom, Spannung und Temperatur
- Charging-, Discharging-, Health- und History-Ansichten
- lokaler Ladealarm und einstellbares Ladeziel
- lokale 30-Tage-Verlaufspunkte und bis zu 150 Lade-/Entladesitzungen mit Details
- analysefähiger lokaler CSV-Export mit Zeitreihe für Akkustand, Strom, Temperatur, Spannung und Bildschirmstatus
- Kapazitätsschätzung und manueller Health-Benchmark
- Bildschirmzeit, Deep Sleep, Ladezyklen und optionale Vordergrund-App-Nutzung
- Live-Overlay mit Akkustrom, CPU-Auslastung und Top-App
- kein Konto und kein automatischer Upload von Messdaten; Netzwerk wird nur für den optionalen Update-Check verwendet

## Entwicklung

```sh
cd android
gradle lintDebug assembleDebug
```

Die Ausgaben liegen danach unter `android/app/build/outputs/apk/debug/app-debug.apk` und `android/app/build/outputs/apk/release/app-release.apk`.

## Kostenlose In-App-Updates

Der Update-Checker prüft optional eine öffentliche HTTPS-Datei im JSON-Format. Die URL wird in `android/app/build.gradle` bei `UPDATE_MANIFEST_URL` eingetragen; ein Beispiel liegt in `latest.json.example`.

Für jede neue Version muss `versionCode` erhöht, die APK unter `apkUrl` veröffentlicht und derselbe Signaturschlüssel wie bei der vorherigen APK verwendet werden. Android zeigt aus Sicherheitsgründen weiterhin eine einmalige Installationsbestätigung an.
