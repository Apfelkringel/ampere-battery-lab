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
- vollständige Ladesitzungsdetails mit Prozentänderung, Startzeit, Dauer sowie Bildschirm-an/aus-Aufteilung
- jede neue Lade-/Entladephase speichert zusätzlich Bildschirmwerte, Bildschirmdauer, Ladequelle und Start-/Endzeit für detaillierte lokale Vergleiche
- lokale 30-Tage-Verlaufspunkte und bis zu 150 Lade-/Entladesitzungen mit Details
- analysefähiger lokaler CSV-Export mit Zeitreihe für Akkustand, Strom, Temperatur, Spannung, Ladequelle, Zykluszähler und Bildschirmstatus
- CSV-Export als auswählbare Datei über den Android-Dateidialog
- strukturierter Research-Export als JSON mit Messreihen, Sitzungen und Geräte-/Android-Kontext nach ausdrücklicher Nutzeraktion
- einstellbare lokale Messfrequenz (5/15/30/60 Minuten) für Hintergrundmonitor und Verlauf
- Kapazitätsschätzung und manueller Health-Benchmark
- Health-Auswertung mit letztem Ladeverschleiß und äquivalenten Vollzyklen (EFC)
- Bildschirmzeit, Deep Sleep, Ladezyklen und optionale Vordergrund-App-Nutzung
- Entladestatistik seit der letzten erkannten Volladung
- optionale lokale Zuordnung der Vordergrund-App zu Telemetriepunkten und geschätztem App-Verbrauch
- Live-Overlay mit Akkustrom, CPU-Kernauslastung, Top-App und best-effort Prozessauslastung der Top-App
- kein Konto und kein automatischer Upload von Messdaten; Netzwerk wird nur für den optionalen Update-Check verwendet
- Downloads werden auch nach einem App-Prozess-Neustart per Android-DownloadManager fortgesetzt, vor der Installation gehasht und von Android bestätigt

## Entwicklung

```sh
cd android
gradle lintDebug assembleDebug
```

Ein Release-Build verwendet niemals einen Fallback- oder Debug-Schlüssel. Dafür müssen
`AMPERE_KEYSTORE_FILE`, `AMPERE_KEYSTORE_PASSWORD`, `AMPERE_KEY_ALIAS` und
`AMPERE_KEY_PASSWORD` gesetzt sein. Die öffentliche Update-APK nutzt eine private
Release-Signatur; die Datei `android/ampere-release.lineage` enthält nur die öffentliche
Android-Signatur-Lineage, niemals einen privaten Schlüssel.

Die Ausgaben liegen danach unter `android/app/build/outputs/apk/debug/app-debug.apk` und `android/app/build/outputs/apk/release/app-release.apk`.

## Kostenlose In-App-Updates

Der Update-Checker prüft optional eine öffentliche HTTPS-Datei im JSON-Format. Die URL wird in `android/app/build.gradle` bei `UPDATE_MANIFEST_URL` eingetragen; ein Beispiel liegt in `latest.json.example`.

Für jede neue Version muss `versionCode` erhöht, die APK unter `apkUrl` veröffentlicht
und `sha256` als SHA-256-Hash ergänzt werden. Der private Release-Schlüssel bleibt im
GitHub-Secret; die Signatur-Lineage erlaubt den Übergang zur neuen Signatur, ohne die
App-Daten zu löschen. Android zeigt aus Sicherheitsgründen weiterhin eine einmalige
Installationsbestätigung an.

GitHub Actions kann die Release-APK bei einem `v*`-Tag reproduzierbar bauen. Die privaten
Schlüssel liegen ausschließlich in `AMPERE_ROTATED_KEYSTORE_BASE64` und
`AMPERE_KEYSTORE_BASE64` (nur für die Signaturrotation); Passwörter und Alias liegen in
den zugehörigen `AMPERE_ROTATED_*`-Secrets.
