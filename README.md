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
- Der Research-Export überspringt beschädigte Altzeilen einzeln, statt wegen eines einzigen ungültigen Wertes komplett abzubrechen
- CSV- und Research-Export verwenden dieselbe Telemetrievalidierung; beschädigte oder physikalisch widersprüchliche Altzeilen werden in beiden Formaten ausgelassen
- einstellbare lokale Messfrequenz (5/15/30/60 Minuten) für Hintergrundmonitor und Verlauf
- Kapazitätsschätzung und manueller Health-Benchmark
- best-effort lokale Designkapazitäts-Erkennung mit Batterie-/BMS-Treiber, Energie-/Spannungswerten und Android-PowerProfile sowie separate Full-Charge-Kapazität vom Batterie-Treiber; jede Quelle bleibt mit Einheitengrenzen und Herkunft transparent
- dynamische Full-Charge-Kapazität wird im laufenden Monitor regelmäßig neu gelesen, damit OEM-Lernwerte nach einer Ladung nicht veralten
- Health-Auswertung mit letztem Ladeverschleiß und äquivalenten Vollzyklen (EFC); Gesundheit wird fachlich auf maximal 100 % begrenzt, ungültige OEM-Werte über 100 % werden verworfen
- Jede Gesundheitsanzeige läuft zusätzlich durch einen zentralen Endfilter; Werte wie 110 % werden auf allen Seiten und in der Hintergrundbenachrichtigung als nicht gemessen behandelt
- Lokale Gesundheitsmessungen werden über die letzten fünf gültigen Ladevorgänge robust per Median ausgewertet, damit ein einzelner Ausreißer die Anzeige nicht verfälscht
- Ein gültiger Android-SoH-Wert ist die gemeinsame Quelle für Prozent- und Kapazitätsanzeige; ungültige oder nicht plausible Quellen bleiben ausdrücklich nicht verfügbar
- Android-SoH wird auf Android 14–17 opportunistisch über die vorhandene BatteryManager-Eigenschaft gelesen; Feature-Flag, OEM-Backport oder fehlende Berechtigung ändern nur die Fallback-Quelle, niemals die Validierungsgrenze
- Zykluszahlen werden zuerst aus der Android-Batterie-API und danach dynamisch aus Batterie-/BMS-/Fuel-Gauge-Treibern gelesen; ein Linux-`cycle_count` von 0 gilt dort korrekt als nicht verfügbar
- Wenn BatteryManager keinen SoH liefert, liest Ampere zusätzlich die standardisierte read-only `state_of_health`-Datei von Batterie-/BMS-Treibern; die konkrete Quelle bleibt sichtbar und Rohwerte wie 110 % werden verworfen
- Auf OnePlus/Oppo/Realme-Geräten werden zusätzlich die dokumentierten read-only `battery_fcc`-/`battery_soh`-Pfade des OPlus/ColorOS-Ladecontrollers geprüft; fehlende oder ungültige OEM-Werte bleiben „Nicht verfügbar"
- Wenn BatteryManager keinen verwertbaren Live-Strom liefert, liest Ampere zusätzlich `current_now`/`current_avg` der Batterie-/BMS-Treiber; USB-Eingangsknoten werden nicht als Akku-Strom ausgegeben
- Batterie-, BMS-, Fuel-Gauge- und USB-Power-Supply-Knoten werden zentral nach deklarierter Linux-Quelle priorisiert; ein irreführend benannter USB-Knoten kann dadurch weder Strom, Kapazität noch Zyklen liefern
- Stromdiagramme verwenden die echten Telemetrie-Zeitabstände; längere Überwachungslücken werden als Lücke dargestellt und nicht als erfundene Rampe verbunden
- Telemetrie-Zeitreihen werden vor Berechnung und Export chronologisch kanonisiert; eine manuelle Uhrkorrektur erzeugt dadurch keine rückwärts laufenden Raten oder Diagrammlinien
- Hintergrund- und Vordergrund-Sampling setzen nach einer rückwärts korrigierten Geräteuhr ihre Abtastbasis neu, statt für die falsche alte Zeitspanne auszufallen
- Automatische Gesundheitsproben entstehen nur nach einer nahezu vollständigen Ladung ab 95 % und dem zuletzt gültigen Ladestrom bis 25 mA; der manuelle Benchmark bleibt separat
- Kabel-Events überbrücken nur die kurze Android-Broadcast-Verzögerung; danach wird der tatsächliche Status erneut synchronisiert, damit Sitzungen nicht dauerhaft falsch offen bleiben
- Alle gespeicherten Phasen-Prozentwerte werden vor Berechnung und Anzeige auf endliche Werte zwischen 0 und 100 % geprüft; kumulativer Verbrauch seit voller Ladung darf fachlich über 100 % liegen
- Temperaturwerte werden in Dashboard, Dienst, Widget, Overlay, Kachel und Alarm zentral auf Androids Zehntelgrad-Einheit und einen plausiblen Bereich geprüft
- Batteriespannung wird in allen Oberflächen zentral als Millivolt validiert, bevor daraus Akku-Leistung oder Anzeige berechnet wird
- Der persistente Charge-Counter wird zentral in Androids Microampere-Stunden-Einheit validiert, bevor Sitzungen oder EFC daraus berechnet werden
- Der gespeicherte EFC-Restanteil bleibt endlich und kleiner als 1,0; beschädigte Werte wie `NaN`, Unendlich oder künstlich große Bruchteile erzeugen keine falschen Zyklen
- Erweiterte Sitzungszeilen prüfen auch den EFC-Wert strikt; ungültige Altzeilen können die Verschleißgrafik nicht mehr beschädigen
- Erweiterte Sitzungszeilen prüfen zusätzlich Start-/End-Akkustand, Energie, Bildschirmwerte und Zeitfelder, bevor sie in Details oder Export gelangen
- Sitzungsdauern werden im gespeicherten Format strikt gelesen; ungültige Dauertexte und rückwärts laufende Start-/Endzeitpunkte werden nicht als echte Sitzungen angezeigt
- Exporte verwerfen widersprüchliche Telemetrie, wenn Lade-/Entladerichtung und Vorzeichen des Stromwerts nicht zusammenpassen
- Sitzungsänderungen werden strikt auf maximal 100 % begrenzt; fehlerhafte Altzeilen und unrealistische Live-Änderungen werden verworfen bzw. aus gültiger Energie neu abgeleitet
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
- Live-Raten, App-Verbrauch, Stromdiagramme und Verlaufsgrafik verwenden dieselbe validierte Telemetriequelle wie die Exporte; beschädigte Altzeilen beeinflussen keine Anzeige mehr
- Ladezeit-Prognosen nutzen lokale 7-Tage-Laderaten, wenn Android keinen Systemwert liefert
- verbleibende Nutzungszeit wird als gemischt, Bildschirm-an und Bildschirm-aus ausgewiesen
- verbleibende Nutzungszeit nutzt zuerst lokale 7-Tage-Daten und kann auf Android 12+ zusätzlich die geprüfte Systemprognose verwenden; die Quelle wird sichtbar benannt
- separate Ladegeschwindigkeitswerte für Bildschirm-an und Bildschirm-aus in mA und %/h
- Health-Baseline kann nach einem Akkutausch zurückgesetzt werden, ohne History oder Telemetrie zu löschen
- Ungültige alte Gesundheitsmessungen werden beim Laden automatisch aus Graph und lokaler Messliste bereinigt
- Ladesitzungen nutzen bei verrauschten Akkustand-Snapshots die gemessene Energie als geprüfte Richtungsstütze, ohne 0-%-Sitzungen zu erzeugen
- kurze 1-%-Kabel-/Statusblips ohne Energiebeleg werden nicht als echte Sitzung gespeichert; langsame 1-%-Sitzungen mit Zeit- oder Energiebeleg bleiben erhalten
- Ladeziele aus Backups und alten Preferences werden zentral auf 50–100 % begrenzt, damit Regler, Fortschrittsbalken und Ladealarm keine unmöglichen Werte übernehmen
- Backup-Dialog zeigt den letzten automatischen Backup-Anstoß; Baseline-Änderungen melden Android sofort eine Datenänderung
- adaptive Darstellung ohne erzwungenes Hochformat für aktuelle Android-16/17-Geräte
- Android 16/17: optionales qualitatives Kapazitätsniveau wird getrennt von Akkustand und Akkugesundheit angezeigt
- Android 14+: optionales Ladeprofil (z. B. akkuschonend, adaptiv, zu heiß/zu kalt) wird getrennt vom Kabelstatus angezeigt
- optionale Ladegerät-Maximalleistung wird aus Androids Strom-/Spannungspaar berechnet und ausdrücklich vom aktuellen Akkustrom getrennt angezeigt
- momentane Akku-Leistung wird aus geprüftem Akkustrom und Akkuspannung berechnet und ausdrücklich von der Netzteil-Maximalleistung getrennt angezeigt
- schmale Ladeansichten verwenden für die Akku-Leistung eine eigene kurze Beschriftung, damit Nebenwerte nicht aus Karten herauslaufen oder unleserlich gekürzt werden
- konfigurierbare Temperaturwarnung mit Hysterese informiert einmalig ab 40/45/50/55 °C und setzt sich erst 3 °C darunter zurück
- Temperaturwarnungs-Dialog zeigt die auswählbaren Grenzwerte direkt als sichtbare Einzelauswahl
- Android-Stromwerte werden zentral in allen Anzeigen validiert; Sentinelwerte und unrealistische Rohstromspitzen werden als nicht verfügbar behandelt
- Android-Akkustand/Skalierung wird zentral validiert; unmögliche OEM-Paare werden nicht mehr stillschweigend auf 100 % gekappt, sondern als nicht verfügbar behandelt
- Auch gespeicherte Verlaufspunkte und Chart-Telemetrie akzeptieren keine ungültigen Prozentwerte aus alten App-Versionen
- Vor dem ersten gültigen Android-Akku-Broadcast zeigt das Dashboard „—“ statt eines erfundenen 0-%-Werts
- Vor dem ersten gültigen Android-Akku-Broadcast bleiben auch Status-Chip und Statuszeile konsistent bei „Nicht verfügbar“
- Die Entladen-Ansicht zeigt ohne abgeschlossene Entladung keinen aktuellen Akkustand als scheinbare Historie, sondern „Noch keine Entladung“
- responsive Kompaktansichten für schmale und breite Displays, zentrierter Inhaltsbereich auf Tablets/Foldables, 48-dp-Touch-Zonen und Screenreader-Zusammenfassung; Details in `docs/UI-UX.md`
- Das Startbildschirm-Widget wählt für kleine Breiten automatisch eine kompakte, nicht überlappende Variante und aktualisiert sich beim Resize anhand der echten Widget-Größe
- Gesundheitswert und zugehörige Kapazität werden als gemeinsamer validierter Snapshot aus derselben Quelle gelesen; ungültige Systemwerte wie 110 % lösen einen atomaren Fallback auf lokale Messungen aus
- Landscape-Übersicht reserviert Gauge, Überschrift, Kennzahlen und Statuszeile in getrennten Geometrie-Lanes, damit nichts übereinanderliegt
- Schmale Hochkant-Karten verwenden vollständige Kurzlabels wie „Kapazität“ statt abgeschnittener Bezeichnungen
- Live-Overlay mit Akkustrom, CPU-Kernauslastung, Top-App und best-effort Prozessauslastung der Top-App
- kein Konto und kein Upload an einen Ampere-Server; Android-Backup kann Verlauf, Einstellungen und lokale Telemetrie über den vom Gerät gewählten Backup-Transport sichern, wobei Cloud-Backups ohne Verschlüsselungsmöglichkeit ausgeschlossen werden; der sichtbare Export/Backup bleibt zusätzlich verfügbar
- keine ungenutzte Berechtigung zur Ausnahme von der Android-Akkuoptimierung; der Monitor bleibt bei den tatsächlich benötigten Rechten
- Downloads werden auch nach einem App-Prozess-Neustart per Android-DownloadManager fortgesetzt, vor der Installation gehasht und von Android bestätigt
- Ein bereits vollständig heruntergeladenes Update wird nach einem Prozess-Neustart beim nächsten Öffnen erneut verifiziert; laufende und doppelte Downloads werden nicht parallel gestartet
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
Beim Restore und bei der Migration werden Telemetriezeilen sofort mit derselben
Plausibilitätsprüfung wie Live-Auswertungen bereinigt; unmögliche Gesundheitswerte wie
`110 %` werden verworfen und nicht angezeigt.
Im breiten Querformat erhält die Live-Karte außerdem einen eigenen unteren Innenabstand,
damit Statuszeile und farbige Kontur nicht optisch ineinanderlaufen.
Gespeicherte Prozentverläufe und Sitzungen werden beim Laden kanonisiert, sodass ungültige
Altzeilen nicht nur ausgeblendet, sondern dauerhaft aus der lokalen Datenbasis entfernt werden.

GitHub Actions kann die Release-APK bei einem `v*`-Tag reproduzierbar bauen. Die privaten
Schlüssel liegen ausschließlich in `AMPERE_ROTATED_KEYSTORE_BASE64` und
`AMPERE_KEYSTORE_BASE64` (nur für die Signaturrotation); Passwörter und Alias liegen in
den zugehörigen `AMPERE_ROTATED_*`-Secrets.
