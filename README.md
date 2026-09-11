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
- der obere LIVE-Schalter liest auf Wunsch sofort den aktuellen Android-Akku-Broadcast erneut ein
- Charging-, Discharging-, Health- und History-Ansichten
- lokaler Ladealarm und einstellbares Ladeziel
- Ladealarm löst nur beim Überschreiten des Ziels aus und nutzt beim Zurücksetzen 3 % Hysterese, damit Prozentflattern keine Alarmserie erzeugt
- ein lokaler Hintergrund-Watchdog prüft den Dienst-Heartbeat im Energiesparmodus und startet die Akkuüberwachung erst nach 30 Minuten ohne Lebenszeichen neu
- vollständige Ladesitzungsdetails mit Prozentänderung, Startzeit, Dauer sowie Bildschirm-an/aus-Aufteilung
- kompakte Kennzahlenkarten zeigen vollständige Zeitwerte wie `20 h 4 m`, ohne abgeschnittene Ellipsen
- jede neue Lade-/Entladephase speichert zusätzlich Bildschirmwerte, Bildschirmdauer, Ladequelle und Start-/Endzeit für detaillierte lokale Vergleiche
- lokale 30-Tage-Verlaufspunkte und bis zu 150 Lade-/Entladesitzungen mit Details
- analysefähiger lokaler CSV-Export mit Zeitreihe für Akkustand, signiertem Strom (Laden positiv/Entladen negativ), Temperatur, Spannung, Ladequelle, Zykluszähler, Bildschirmstatus und Bildschirm-Wakeups
- CSV-Export als auswählbare Datei über den Android-Dateidialog
- strukturierter Research-Export als JSON mit Messreihen, Sitzungen, Zyklus-Historie, „Seit Ladung“-Anker und Geräte-/Android-Kontext nach ausdrücklicher Nutzeraktion
- der Research-Export enthält zusätzlich eine reproduzierbare Telemetrie-Diagnose mit Messwertanzahl, Spitzenstrom, Minimalspannung, Maximaltemperatur und erkannten Sampling-Lücken
- Androids optionaler `BATTERY_PROPERTY_ENERGY_COUNTER` wird als verbleibende Restenergie in nWh/Wh angezeigt und getrennt vom Ladungszähler in den Research-Export geschrieben; nicht unterstützte oder unplausible Gerätewerte bleiben „Nicht verfügbar"
- Die lokale Telemetrie leitet zusätzlich reproduzierbare Akku-Leistung in mW aus validiertem Strom und Spannung ab, getrennt für Laden und Entladen; Min/Ø/Max erscheinen in Diagnose und Research-Export, CSV ergänzt `battery_power_mw`
- ein lokaler Diagnosebericht kann als lesbare TXT-Datei exportiert werden und erklärt Warnungen, Sampling-Lücken und die bewusst geräteagnostische Spannungsbewertung
- die Gesundheitsansicht und der Research-Export zeigen die vom Android-Broadcast gelieferte Akkuchemie nur nach strenger Textvalidierung, ohne geschützte Hidden-API-Daten zu erfinden
- die Verlaufseite zeigt dieselbe geräteagnostische Messdiagnose; für Spannung wird keine feste 2S-Pack-Grenze auf 1S-Smartphones übertragen und laufende Entladungen werden nicht fälschlich als „Early Cutoff“ markiert
- Der Research-Export überspringt beschädigte Altzeilen einzeln, statt wegen eines einzigen ungültigen Wertes komplett abzubrechen
- Die gezeichneten Header-Aktionen und fünf Navigationstabs werden zusätzlich als einzelne Android-Bedienelemente mit echten Bildschirmgrenzen für TalkBack und Touch-Exploration bereitgestellt
- Seiteninteraktionen wie 7/30-Tage-Auswahl, Ladealarm, Live-Anzeige, Benchmark, Nennkapazität, Nutzungsdetails und CSV-Export sind ebenfalls als echte TalkBack-Controls verfügbar; Schalter melden ihren aktuellen Zustand
- Das Ladeziel ist zusätzlich ein standardkonformer 50–100-%-Regler für TalkBack und andere Accessibility-Services; Änderungen verwenden dieselbe sichere Ladealarm-Logik wie Touch-Eingaben
- CSV- und Research-Export verwenden dieselbe Telemetrievalidierung; beschädigte oder physikalisch widersprüchliche Altzeilen werden in beiden Formaten ausgelassen
- einstellbare lokale Messfrequenz (5/15/30/60 Minuten) für Hintergrundmonitor und Verlauf
- Kapazitätsschätzung und manueller Health-Benchmark
- best-effort lokale Designkapazitäts-Erkennung mit Batterie-/BMS-Treiber, Energie-/Spannungswerten und Android-PowerProfile sowie separate Full-Charge-Kapazität vom Batterie-Treiber; jede Quelle bleibt mit Einheitengrenzen und Herkunft transparent
- wenn kein Full-Charge-Treiberwert vorhanden ist, wird Androids verbleibender Charge-Counter vorsichtig durch den aktuellen Ladezustandsanteil hochgerechnet und als Schätzung gekennzeichnet
- diese Charge-Counter-Hochrechnung verwendet den ungerundeten Android-Anteil `level / scale` statt einer vorzeitigen Ganzzahl-Prozent-Rundung
- dynamische Full-Charge-Kapazität wird im laufenden Monitor regelmäßig neu gelesen, damit OEM-Lernwerte nach einer Ladung nicht veralten
- Health-Auswertung mit letztem Ladeverschleiß und äquivalenten Vollzyklen (EFC); Gesundheit wird fachlich auf maximal 100 % begrenzt, ungültige OEM-Werte über 100 % werden verworfen
- tägliche Vollzyklus-Historie über bis zu 90 Tage; ein gemeldeter Android-/BMS-Zähler gewinnt gegenüber der lokalen EFC-Schätzung und bleibt pro Tag monoton
- tägliche Vollzyklen werden auch in CSV- und Research-JSON exportiert und gemeinsam mit der Gesundheitsbasis zurückgesetzt
- Jede Gesundheitsanzeige läuft zusätzlich durch einen zentralen Endfilter; Werte wie 110 % werden auf allen Seiten und in der Hintergrundbenachrichtigung als nicht gemessen behandelt
- Lokale Gesundheitsmessungen werden über die letzten fünf gültigen Ladevorgänge robust per Median ausgewertet, damit ein einzelner Ausreißer die Anzeige nicht verfälscht
- Ein gültiger Android-SoH-Wert ist die gemeinsame Quelle für Prozent- und Kapazitätsanzeige; ungültige oder nicht plausible Quellen bleiben ausdrücklich nicht verfügbar
- Android-SoH wird auf Android 14–17 opportunistisch über die vorhandene BatteryManager-Eigenschaft gelesen; Feature-Flag, OEM-Backport oder fehlende Berechtigung ändern nur die Fallback-Quelle, niemals die Validierungsgrenze
- Zykluszahlen werden zuerst aus der Android-Batterie-API und danach dynamisch aus Batterie-/BMS-/Fuel-Gauge-Treibern gelesen; ein Linux-`cycle_count` von 0 gilt dort korrekt als nicht verfügbar
- Wenn BatteryManager keinen SoH liefert, liest Ampere zusätzlich die standardisierte read-only `state_of_health`-Datei von Batterie-/BMS-Treibern; die konkrete Quelle bleibt sichtbar und Rohwerte wie 110 % werden verworfen
- Auf OnePlus/Oppo/Realme-Geräten werden zusätzlich die dokumentierten read-only `battery_fcc`-/`battery_soh`-Pfade des OPlus/ColorOS-Ladecontrollers geprüft; fehlende oder ungültige OEM-Werte bleiben „Nicht verfügbar"
- Wenn BatteryManager keinen verwertbaren Live-Strom liefert, liest Ampere zusätzlich `current_now`/`current_avg` der Batterie-/BMS-Treiber; USB-Eingangsknoten werden nicht als Akku-Strom ausgegeben
- OEM-Stromwerte mit plausibler, aber falsch verkleinerter Einheit werden wie bei Battery Monitor gegen typische Lade-/Entladebereiche geprüft und höchstens mit Faktor 10/100/1000 korrigiert; Lade-Taper ab 90 % bleibt unangetastet
- Samsung-Geräte mit lesbarem `fg_asoc` erhalten zusätzlich einen expliziten ASOC-SoH-Fallback; der qualitative Treiberwert `health` bleibt davon getrennt
- Das Startbildschirm-Widget verwendet ab Android 12 responsive `RemoteViews`-Breakpoints für kurze, kompakte und breite Größen; Android 14/15 ohne diese Auswahl-API nutzt denselben validierten Größen-Fallback
- Die Gesundheitsquelle wird in jeder Ansicht einheitlich benannt; ungültige Prozentwerte können keine veraltete Quellenbezeichnung zurücklassen
- Batterie-, BMS-, Fuel-Gauge- und USB-Power-Supply-Knoten werden zentral nach deklarierter Linux-Quelle priorisiert; ein irreführend benannter USB-Knoten kann dadurch weder Strom, Kapazität noch Zyklen liefern
- Stromdiagramme verwenden die echten Telemetrie-Zeitabstände; längere Überwachungslücken werden als Lücke dargestellt und nicht als erfundene Rampe verbunden
- Telemetrie-Zeitreihen werden vor Berechnung und Export chronologisch kanonisiert; eine manuelle Uhrkorrektur erzeugt dadurch keine rückwärts laufenden Raten oder Diagrammlinien
- Hintergrund- und Vordergrund-Sampling setzen nach einer rückwärts korrigierten Geräteuhr ihre Abtastbasis neu, statt für die falsche alte Zeitspanne auszufallen
- Automatische Gesundheitsproben entstehen nur nach einer nahezu vollständigen Ladung ab 95 % und dem zuletzt gültigen Ladestrom bis 25 mA; der manuelle Benchmark bleibt separat
- Kabel-Events überbrücken nur die kurze Android-Broadcast-Verzögerung; danach wird der tatsächliche Status erneut synchronisiert, damit Sitzungen nicht dauerhaft falsch offen bleiben
- Alle gespeicherten Phasen-Prozentwerte werden vor Berechnung und Anzeige auf endliche Werte zwischen 0 und 100 % geprüft; kumulativer Verbrauch seit voller Ladung darf fachlich über 100 % liegen
- Temperaturwerte werden in Dashboard, Dienst, Widget, Overlay, Kachel und Alarm zentral auf Androids Zehntelgrad-Einheit und einen plausiblen Bereich geprüft
- Temperatur-Telemetrie wird zusätzlich als Min/Ø/Max ausgewertet und dieselbe Statistik erscheint in Gesundheitsansicht, Verlaufdiagnose und Research-Export
- Batteriespannung wird in allen Oberflächen zentral als Millivolt validiert, bevor daraus Akku-Leistung oder Anzeige berechnet wird
- Der persistente Charge-Counter wird zentral in Androids Microampere-Stunden-Einheit validiert, bevor Sitzungen oder EFC daraus berechnet werden
- Der gespeicherte EFC-Restanteil bleibt endlich und kleiner als 1,0; beschädigte Werte wie `NaN`, Unendlich oder künstlich große Bruchteile erzeugen keine falschen Zyklen
- Erweiterte Sitzungszeilen prüfen auch den EFC-Wert strikt; ungültige Altzeilen können die Verschleißgrafik nicht mehr beschädigen
- Erweiterte Sitzungszeilen prüfen zusätzlich Start-/End-Akkustand, Energie, Bildschirmwerte und Zeitfelder, bevor sie in Details oder Export gelangen
- Sitzungsdauern werden im gespeicherten Format strikt gelesen; ungültige Dauertexte und rückwärts laufende Start-/Endzeitpunkte werden nicht als echte Sitzungen angezeigt
- Exporte verwerfen widersprüchliche Telemetrie, wenn Lade-/Entladerichtung und Vorzeichen des Stromwerts nicht zusammenpassen
- Sitzungsänderungen werden strikt auf maximal 100 % begrenzt; fehlerhafte Altzeilen und unrealistische Live-Änderungen werden verworfen bzw. aus gültiger Energie neu abgeleitet
- Erweiterte Alt-Sitzungen werden zusätzlich auf physikalische Richtung geprüft: Laden darf nicht mit weniger Prozent enden und Entladen nicht mit mehr; widersprüchliche Zeilen werden beim Laden der Daten entfernt
- Charge-Counter-Sprünge über drei Nennkapazitäten werden vor der Sitzungs-/EFC-Berechnung verworfen; die Prozentänderung bleibt bei vorhandener Messung erhalten, während die unrealistische mAh-Angabe als nicht verfügbar gilt
- Nach einer unbeobachteten Sampling-Lücke wird eine alte offene Sitzung nicht mehr über die gesamte Lücke fortgeschrieben; der aktuelle Messpunkt startet eine neue belastbare Basis
- System-Ladezyklen werden aus Android oder unterstützten Batterie-/BMS-Treibern gelesen; fehlt beides, nutzt Ampere eine vorsichtige lokale EFC-Schätzung aus Androids persistentem Charge Counter und kennzeichnet sie mit `~`
- transparente Anzeige, warum eine Ladesitzung noch nicht als Health-Kapazitätsprobe zählt
- Bildschirmzeit, echte Android-Suspendzeit (Deep Sleep), Ladezyklen und optionale Vordergrund-App-Nutzung
- Vordergrund-App-Zeit verfolgt aktive Activity-Klassen als Set, ignoriert doppelte PAUSED/STOPPED-Events und schließt beim echten Screen-Off alle offenen Sitzungen
- Bildschirm-Aufwachereignisse pro Entladephase als transparente Näherung für Deep-Sleep-Wakeups
- Entladestatistik seit der letzten erkannten Volladung
- hybride „Seit Ladung“-Statistik: neuer Voll-Ladeanker pro Ladestecker-Sitzung oder transparenter Absteck-Anker bei vorzeitigem Abstecken
- lokale 7-Tage-Laufzeitprognose aus Bildschirm-an/aus-Telemetrie
- Verlaufsgrafik nutzt für 7/30 Tage die tatsächlichen lokalen Telemetrie-Zeitpunkte und echte Datums-/Wochentagsachsen
- relative Verschleißwirkung des gewählten Ladeziels mit erhöhter Gewichtung hoher Ladezustände
- speichert auch lange Ladesitzungen bei unverändertem Prozentstand (z. B. OEM-Ladelimit) anhand geladener mAh
- optionale lokale Zuordnung der Vordergrund-App zu Telemetriepunkten und geschätztem App-Verbrauch; Vordergrundzeiten werden bevorzugt aus exakten Android-UsageEvents statt aus erweiterten Tages-Buckets berechnet
- App-Verbrauchsschätzung berücksichtigt die tatsächlichen Zeitabstände der lokalen Messpunkte
- App-mAh bleiben ausdrücklich Schätzungen; direkte Telemetrie wird proportional auf die beobachtete Entladeenergie begrenzt und fällt sonst auf eine zeitgewichtete Verteilung zurück
- strombasierte mAh-Fallbacks berücksichtigen auch gewählte 60-Minuten-Messintervalle
- 30-Tage-History passt die gespeicherte Punktzahl automatisch an das gewählte Messintervall an
- lokale Telemetrie behält ebenfalls ungefähr 30 Tage bei jeder Messfrequenz (5/15/30/60 Minuten)
- Live-Raten, App-Verbrauch, Stromdiagramme und Verlaufsgrafik verwenden dieselbe validierte Telemetriequelle wie die Exporte; beschädigte Altzeilen beeinflussen keine Anzeige mehr
- Ladezeit-Prognosen nutzen lokale 7-Tage-Laderaten, wenn Android keinen Systemwert liefert
- wenn Android keine Lade-/Entladeprognose liefert, prüft Ampere zusätzlich optionale read-only Fuel-Gauge-Werte (`time_to_full_now`, `time_to_full_avg`, `time_to_empty_avg`); 0/-1, nicht lesbare Werte und Prognosen über 48 Stunden bleiben „Nicht verfügbar"
- verbleibende Nutzungszeit wird als gemischt, Bildschirm-an und Bildschirm-aus ausgewiesen
- die gemischte Laufzeitprognose stabilisiert eine aktuelle Entladephase gegen die lokale 7-Tage-Historie; nach zehn Minuten wird der aktuelle Verlauf schrittweise höher gewichtet und erreicht erst nach 30 Minuten maximal 60 %, statt einen einzelnen Prozent-Sprung sofort als Wahrheit zu übernehmen
- Lade-/Entlade-Zeitstatistiken begrenzen unbeobachtete Monitorlücken auf das Messintervall, damit ein beendeter Dienst weder Sitzungsdauer noch Laufzeitprognose künstlich verlängert
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
- wenn ein Batterie-/BMS-Treiber `charge_control_start_threshold` und `charge_control_end_threshold` read-only bereitstellt, zeigt Ampere das tatsächliche OEM-Ladefenster getrennt vom eigenen Ladeziel; fehlende oder ungültige Werte bleiben unsichtbar
- optionale Ladegerät-Maximalleistung wird aus Androids Strom-/Spannungspaar berechnet und ausdrücklich vom aktuellen Akkustrom getrennt angezeigt
- optionale Ladegerät-Grenzwerte werden zusätzlich getrennt als maximaler Strom und maximale Spannung validiert angezeigt; die abgeleitete Maximalleistung bleibt davon getrennt
- falls Android kein Ladegerät-Paar liefert, wird zusätzlich `constant_charge_current_max` read-only als getrennte Ladehardware-Grenze geprüft; `charge_control_limit_max` wird wegen möglicher OEM-Stufenwerte nicht fehlinterpretiert
- wenn ein Gerät `internal_resistance` bereitstellt, zeigt Ampere den dynamischen Innenwiderstand/ESR read-only als Diagnose; der Wert wird nicht fälschlich in Akkugesundheit umgerechnet
- wenn ein Gerät `charge_type` oder `charge_types` bereitstellt, zeigt Ampere den aktiven Kernel-Ladealgorithmus getrennt vom Android-Ladeprofil; unbekannte OEM-Texte bleiben unsichtbar
- wenn ein Gerät `capacity_error_margin` bereitstellt, zeigt Ampere die vom Fuel-Gauge gemeldete Kapazitätsunsicherheit getrennt von Akkualterung und Gesundheit
- wenn ein Gerät `charge_behaviour` bereitstellt, zeigt Ampere den aktiven Kernel-Lademodus (`auto`, Ladesperre oder erzwungene Entladung) read-only an und erklärt dadurch auch Bypass-/Inhibit-Zustände
- wenn ein Batterie-/BMS-Treiber die drei standardisierten Manufacture-Date-Felder bereitstellt, zeigt Ampere ein vollständig validiertes Herstellungsdatum read-only; unvollständige oder unmögliche Kalenderdaten bleiben unsichtbar
- Lade- und Entladestrom-Diagramme zeigen aus validierten Messpunkten Minimum, Durchschnitt und Maximum; fehlende oder nicht-positive Werte werden nicht aggregiert
- Ladequellen werden wie in Android-/OEM-Broadcasts als Bitfeld ausgewertet, damit kombinierte `EXTRA_PLUGGED`-Werte in Anzeige und Sitzungsverlauf korrekt als Netzteil, USB, Dock oder kabellos erscheinen
- momentane Akku-Leistung wird aus geprüftem Akkustrom und Akkuspannung berechnet und ausdrücklich von der Netzteil-Maximalleistung getrennt angezeigt
- wenn der Android-Spannungs-Broadcast fehlt oder unplausibel ist, nutzt Ampere priorisierte read-only `voltage_now`-Knoten von Batterie/BMS und normalisiert mV sowie µV; USB-Power-Supplies bleiben ausgeschlossen
- Widget, Quick-Settings-Kachel, Overlay und Dream-Screensaver verwenden gemeinsam einen validierten Akku-Snapshot, damit Status, Strom, Spannung und Temperatur auf allen Ausgabekanälen dieselbe Messung darstellen
- fehlt einem OEM-Broadcast das Feld `EXTRA_PLUGGED`, bleibt ein expliziter Android-Status `CHARGING`/`FULL` trotzdem verwertbar; ein tatsächlich vorhandenes `EXTRA_PLUGGED=0` bleibt dagegen als abgesteckt maßgeblich
- Zeit-bis-voll-/Zeit-bis-Ladeziel-Schätzungen bleiben bei unbekannter Kapazität, ungültigen Raten oder zu kleinem Strom nicht verfügbar, statt eine künstliche 1-Minuten-Prognose zu erzeugen
- Ladezeit-Raten werden zusätzlich auf endliche, physikalisch plausible Größen begrenzt; ein Ausreißer im Momentanstrom überschreibt keine gültige historische Laderate
- schmale Ladeansichten verwenden für die Akku-Leistung eine eigene kurze Beschriftung, damit Nebenwerte nicht aus Karten herauslaufen oder unleserlich gekürzt werden
- konfigurierbare Temperaturwarnung mit Hysterese informiert einmalig ab 40/45/50/55 °C und setzt sich erst 3 °C darunter zurück
- Android 10+: der öffentliche `PowerManager`-Thermikstatus wird zusätzlich zur Akku-Temperatur in Ladeansicht und Hintergrundbenachrichtigung angezeigt; fehlende OEM-/API-Werte bleiben „Nicht verfügbar"
- konfigurierbare Tiefstandwarnung informiert einmalig bei 10/15/20/25/30 % und setzt sich erst mit 3 % Abstand oder nach dem Laden zurück
- optionaler systemweiter Ladebildschirm als Android-Dream/Screensaver mit OLED-schwarzem Hintergrund, Uhr, Akkustand, Strom, Akku-Leistung, Spannung und Temperatur; Aktivierung erfolgt ausschließlich über Androids Bildschirmschoner-Einstellungen
- Temperaturwarnungs-Dialog zeigt die auswählbaren Grenzwerte direkt als sichtbare Einzelauswahl
- Android-Stromwerte werden zentral in allen Anzeigen validiert; Sentinelwerte und unrealistische Rohstromspitzen werden als nicht verfügbar behandelt
- Android-Akkustand/Skalierung wird zentral validiert; unmögliche OEM-Paare werden nicht mehr stillschweigend auf 100 % gekappt, sondern als nicht verfügbar behandelt
- Auch gespeicherte Verlaufspunkte und Chart-Telemetrie akzeptieren keine ungültigen Prozentwerte aus alten App-Versionen
- Vor dem ersten gültigen Android-Akku-Broadcast zeigt das Dashboard „—“ statt eines erfundenen 0-%-Werts
- Vor dem ersten gültigen Android-Akku-Broadcast bleiben auch Status-Chip und Statuszeile konsistent bei „Nicht verfügbar“
- Die Entladen-Ansicht zeigt ohne abgeschlossene Entladung keinen aktuellen Akkustand als scheinbare Historie, sondern „Noch keine Entladung“
- responsive Kompaktansichten für schmale und breite Displays, zentrierter Inhaltsbereich auf Tablets/Foldables, 48-dp-Touch-Zonen und Screenreader-Zusammenfassung; Details in `docs/UI-UX.md`
- Das Startbildschirm-Widget wählt anhand von Breite und Höhe automatisch eine passende, nicht überlappende Variante; sehr niedrige Querformat-Größen verwenden eine eigene Einzeilenansicht
- Gesundheitswert und zugehörige Kapazität werden als gemeinsamer validierter Snapshot aus derselben Quelle gelesen; ungültige Systemwerte wie 110 % lösen einen atomaren Fallback auf lokale Messungen aus
- Landscape-Übersicht reserviert Gauge, Überschrift, Kennzahlen und Statuszeile in getrennten Geometrie-Lanes, damit nichts übereinanderliegt
- Schmale Hochkant-Karten verwenden vollständige Kurzlabels wie „Kapazität“ statt abgeschnittener Bezeichnungen
- Live-Overlay mit Akkustrom, CPU-Kernauslastung, Top-App und best-effort Prozessauslastung der Top-App
- Das Live-Overlay verwendet stabile Textbeschriftungen statt herstellerabhängiger Emoji-Glyphen
- kein Konto und kein Upload an einen Ampere-Server; Android-Backup kann Verlauf, Einstellungen und lokale Telemetrie über den vom Gerät gewählten Backup-Transport sichern, wobei Cloud-Backups ohne Verschlüsselungsmöglichkeit ausgeschlossen werden; der sichtbare Export/Backup bleibt zusätzlich verfügbar
- keine ungenutzte Berechtigung zur Ausnahme von der Android-Akkuoptimierung; der Monitor bleibt bei den tatsächlich benötigten Rechten
- Downloads werden auch nach einem App-Prozess-Neustart per Android-DownloadManager fortgesetzt, vor der Installation gehasht und von Android bestätigt
- Ein bereits vollständig heruntergeladenes Update wird nach einem Prozess-Neustart beim nächsten Öffnen erneut verifiziert; laufende und doppelte Downloads werden nicht parallel gestartet
- Zeitwerte in schmalen Kennzahlenkarten werden kompakt und vollständig angezeigt, statt am Kartenrand abgeschnitten zu werden
- Kennzahlenkarten stapeln sich unter 160 dp automatisch, damit Labels auf 320-dp-Geräten nicht neben dem Icon abgeschnitten werden
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
