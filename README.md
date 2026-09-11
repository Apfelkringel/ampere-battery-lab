# Ampere Battery Lab

Kostenlose, lokal arbeitende Android-Batterieanalyse als eigenständige Implementierung.

## APK installieren

1. `Ampere-Battery-Lab-release.apk` auf das Android-Handy kopieren.
2. Die Datei öffnen und – falls Android fragt – die Installation aus dieser Quelle erlauben.
3. Beim ersten Start Benachrichtigungen erlauben, damit der lokale Hintergrundmonitor und der Ladealarm funktionieren.
4. Für die App-Nutzungsanzeige im Tab „Drain“ den Android-Zugriff auf Nutzungsdaten freigeben.

Die eigentliche Android-App liegt unter `android/`. Lokale APK-Kopien liegen unter
`artifacts/`; der optionale Web-Prototyp liegt getrennt unter `prototypes/web/`.
Die kanonische Projektablage befindet sich auf der externen SSD unter
`/Volumes/MacSSD/02_PROJECTS/Active/AccuBattery`. Eine Übersicht gibt es in
`docs/PROJECT-STRUCTURE.md`.

## Enthalten

- Live-Ladezustand, Strom, Spannung und Temperatur
- Ladeerkennung erfolgt automatisch über Android; der Status ist keine manuelle Schaltfläche
- Charging-, Discharging-, Health- und History-Ansichten
- lokaler Ladealarm und einstellbares Ladeziel
- vollständige Ladesitzungsdetails mit Prozentänderung, Startzeit, Dauer sowie Bildschirm-an/aus-Aufteilung
- jede neue Lade-/Entladephase speichert zusätzlich Bildschirmwerte, Bildschirmdauer, Ladequelle und Start-/Endzeit für detaillierte lokale Vergleiche
- lokale 30-Tage-Verlaufspunkte und bis zu 150 Lade-/Entladesitzungen mit Details
- analysefähiger lokaler CSV-Export mit Zeitreihe für Akkustand, signiertem Strom (Laden positiv/Entladen negativ), Temperatur, Spannung, Ladequelle, Zykluszähler, Bildschirmstatus und Bildschirm-Wakeups
- CSV-Export als auswählbare Datei über den Android-Dateidialog
- strukturierter Research-Export als JSON mit Messreihen, Sitzungen und Geräte-/Android-Kontext nach ausdrücklicher Nutzeraktion
- einstellbare lokale Messfrequenz (5/15/30/60 Minuten) für Hintergrundmonitor und Verlauf
- Kapazitätsschätzung und manueller Health-Benchmark
- best-effort lokale Designkapazitäts-Erkennung mit Batterie-/BMS-Treiber, Energie-/Spannungswerten und Android-PowerProfile sowie separate Full-Charge-Kapazität vom Batterie-Treiber; jede Quelle bleibt mit Einheitengrenzen und Herkunft transparent
- Health-Auswertung mit letztem Ladeverschleiß und äquivalenten Vollzyklen (EFC); Gesundheit wird fachlich auf maximal 100 % begrenzt
- System-Ladezyklen werden aus Android oder unterstützten Batterie-/BMS-Treibern gelesen; fehlt beides, nutzt Ampere eine vorsichtige lokale EFC-Schätzung aus Androids persistentem Charge Counter und kennzeichnet sie mit `~`
- transparente Anzeige, warum eine Ladesitzung noch nicht als Health-Kapazitätsprobe zählt
- Bildschirmzeit, echte Android-Suspendzeit (Deep Sleep), Ladezyklen und optionale Vordergrund-App-Nutzung
- Bildschirm-Aufwachereignisse pro Entladephase als transparente Näherung für Deep-Sleep-Wakeups
- Entladestatistik seit der letzten erkannten Volladung
- lokale 7-Tage-Laufzeitprognose aus Bildschirm-an/aus-Telemetrie
- Verlaufsgrafik nutzt für 7/30 Tage die tatsächlichen lokalen Telemetrie-Zeitpunkte und echte Datums-/Wochentagsachsen
- relative Verschleißwirkung des gewählten Ladeziels mit erhöhter Gewichtung hoher Ladezustände
- speichert auch lange Ladesitzungen bei unverändertem Prozentstand (z. B. OEM-Ladelimit) anhand geladener mAh
- optionale lokale Zuordnung der Vordergrund-App zu Telemetriepunkten und geschätztem App-Verbrauch; Vordergrundzeiten werden bevorzugt aus exakten Android-UsageEvents statt aus erweiterten Tages-Buckets berechnet
- App-Verbrauchsschätzung berücksichtigt die tatsächlichen Zeitabstände der lokalen Messpunkte
- strombasierte mAh-Fallbacks berücksichtigen auch gewählte 60-Minuten-Messintervalle
- 30-Tage-History passt die gespeicherte Punktzahl automatisch an das gewählte Messintervall an
- lokale Telemetrie behält ebenfalls ungefähr 30 Tage bei jeder Messfrequenz (5/15/30/60 Minuten)
- Ladezeit-Prognosen nutzen lokale 7-Tage-Laderaten, wenn Android keinen Systemwert liefert
- verbleibende Nutzungszeit wird als gemischt, Bildschirm-an und Bildschirm-aus ausgewiesen
- separate Ladegeschwindigkeitswerte für Bildschirm-an und Bildschirm-aus in mA und %/h
- Health-Baseline kann nach einem Akkutausch zurückgesetzt werden, ohne History oder Telemetrie zu löschen
- Backup-Dialog zeigt den letzten automatischen Backup-Anstoß; Baseline-Änderungen melden Android sofort eine Datenänderung
- adaptive Darstellung ohne erzwungenes Hochformat für aktuelle Android-16/17-Geräte
- responsive Kompaktansichten für schmale und breite Displays, zentrierter Inhaltsbereich auf Tablets/Foldables, 48-dp-Touch-Zonen und Screenreader-Zusammenfassung; Details in `docs/UI-UX.md`
- Live-Overlay mit Akkustrom, CPU-Kernauslastung, Top-App und best-effort Prozessauslastung der Top-App
- kein Konto und kein Upload an einen Ampere-Server; Android-Backup kann Verlauf, Einstellungen und lokale Telemetrie über den vom Gerät gewählten Backup-Transport sichern, wobei Cloud-Backups ohne Verschlüsselungsmöglichkeit ausgeschlossen werden; der sichtbare Export/Backup bleibt zusätzlich verfügbar
- Downloads werden auch nach einem App-Prozess-Neustart per Android-DownloadManager fortgesetzt, vor der Installation gehasht und von Android bestätigt
- der Hintergrundmonitor prüft höchstens alle 12 Stunden die öffentliche Update-Datei und meldet neue Versionen per Android-Benachrichtigung
- In-App-Updates laden Manifest und APK über den festgelegten GitHub-Contents-Endpunkt und prüfen vor dem Installer zusätzlich Hash, Paketname, Version und das eingebettete Release-Zertifikat; Details in `docs/UPDATE-SECURITY.md`
- Lade-/Entladegrenzen werden zusätzlich direkt über Androids `POWER_CONNECTED`-/`POWER_DISCONNECTED`-Broadcasts verarbeitet, damit Kabelwechsel und Ladealarm nicht auf eine verzögerte Statusmessung warten
- Der lokale Foreground-Monitor verarbeitet Messungen und System-Broadcasts auf einem eigenen Hintergrundthread, damit die responsive Oberfläche nicht durch Akku-/Datei-/Telemetriearbeit blockiert wird

## Entwicklung

```sh
cd android
gradle lintDebug testDebugUnitTest assembleDebug
```

Ein Release-Build verwendet niemals einen Fallback- oder Debug-Schlüssel. Dafür müssen
`AMPERE_KEYSTORE_FILE`, `AMPERE_KEYSTORE_PASSWORD`, `AMPERE_KEY_ALIAS` und
`AMPERE_KEY_PASSWORD` gesetzt sein. Die öffentliche Update-APK nutzt als aktuellen
Signer eine private Release-Signatur (RSA 4096). Die Datei `android/ampere-release.lineage`
enthält zusätzlich den öffentlichen Android-Debug-Vorgänger, damit bereits installierte
Testversionen beim Übergang ihre Daten behalten; der Debug-Key ist nicht der aktuelle
Signer und wird in CI ausdrücklich nicht als aktueller Release-Signer akzeptiert.
Private Schlüssel werden niemals eingecheckt.

Die Ausgaben liegen danach unter `android/app/build/outputs/apk/debug/app-debug.apk` und `android/app/build/outputs/apk/release/app-release.apk`.

## Kostenlose In-App-Updates

Der Update-Checker prüft optional eine öffentliche HTTPS-Datei im JSON-Format. Die URL wird in `android/app/build.gradle` bei `UPDATE_MANIFEST_URL` eingetragen; ein Beispiel liegt in `latest.json.example`.

Für jede neue Version muss `versionCode` erhöht, die APK unter `apkUrl` veröffentlicht
und `sha256` als SHA-256-Hash ergänzt werden. Der private Release-Schlüssel bleibt im
GitHub-Secret; die Signatur-Lineage erlaubt den Übergang zur neuen Signatur, ohne die
App-Daten zu löschen. Android zeigt aus Sicherheitsgründen weiterhin eine einmalige
Installationsbestätigung an.

Der sichtbare Backup-/Restore-Dialog erzeugt ein vollständiges JSON-Backup inklusive
Telemetrie. Das automatische Android-Backup sichert ebenfalls Verlauf, Einstellungen und
lokale Telemetrie und wird für Cloud-Backups ohne Verschlüsselungsmöglichkeit nicht
freigegeben. Bei einer Deinstallation sollte vorab trotzdem ein sichtbares Backup erzeugt
werden, weil Verfügbarkeit und Aufbewahrung des Android-Backups vom Gerät und Konto abhängen.

GitHub Actions kann die Release-APK bei einem `v*`-Tag reproduzierbar bauen. Die privaten
Schlüssel liegen ausschließlich in `AMPERE_ROTATED_KEYSTORE_BASE64` und
`AMPERE_KEYSTORE_BASE64` (nur für die Signaturrotation); Passwörter und Alias liegen in
den zugehörigen `AMPERE_ROTATED_*`-Secrets.
