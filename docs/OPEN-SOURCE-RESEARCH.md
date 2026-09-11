# Open-Source-Recherche

Stand: 11. September 2026

Die Akku-Datenlogik wurde gegen mehrere fertige Open-Source-Apps geprüft. In
Ampere Battery Lab wurden nur allgemeine, nachgebaut getestete Muster aus
Apache-2.0- und MIT-Projekten verwendet; GPL-Code wurde nicht übernommen.

## Übernommene Muster

- [ABattery](https://github.com/abanana84/abattery), Apache-2.0: klare Trennung
  zwischen Nennkapazität, gemessener Voll-Ladekapazität und geschätzten Werten;
  robuste Einheiten-/Grenzwertprüfung; Quellenhierarchie für Android-, Treiber-
  und OEM-Daten.
- [Beam](https://github.com/montafra/beam), MIT: kompakte Live-Anzeige,
  explizite Behandlung nicht verfügbarer Stromwerte und keine erfundenen
  Messdaten.
- [PlusPlusBattery](https://github.com/dijia1124/PlusPlusBattery), MIT:
  optionale OEM-/Sysfs-Pfade als Ergänzung, aber nur mit strengen Einheiten-
  und Plausibilitätsgrenzen.
- [RTMON](https://github.com/n1th1n-19/RTMON), MIT: klassisches Android-
  `RemoteViews`-Widget mit systemnahen Messwerten und sparsamer Aktualisierung;
  als Architektur- und Resize-Referenz geprüft.
- [BatteryTile](https://github.com/Solarphlare/BatteryTile), GPL-3.0: zeigt,
  wie eine reine Informationskachel Strom, Spannung, Temperatur und Ladestatus
  sinnvoll bündelt. Verwendet wurde nur die Produktidee; der Quellcode wurde
  nicht übernommen.
- [Android AOSP BatteryManager](https://android.googlesource.com/platform/frameworks/base/+/main/core/java/android/os/BatteryManager): offizielle
  API-Definition für Androids qualitative Kapazitätsstufe. Diese Stufe ist ein
  Power-Management-Signal und keine Akkugesundheit in Prozent; Ampere zeigt sie
  deshalb separat und nur bei tatsächlich vorhandenem Systemwert.

## Bewusst nicht übernommen

[Device-Watch](https://github.com/jrs8205/Device-Watch) und
[Battery Monitor](https://github.com/tswistak/Battery-Monitor) stehen unter
GPL-Lizenzen. Ihre Ideen zu Verlauf, Widgets und „seit letzter Ladung“ waren
als Produktreferenz nützlich, ihr Quellcode ist nicht Bestandteil dieses
Projekts.

## Anwendung im Projekt

`BatteryCapacity` prüft jetzt in fester Reihenfolge:

1. manuelle Nutzereingabe,
2. Batterie-/BMS-Treiberwerte,
3. Energie-/Spannungsumrechnung des Treibers,
4. Android-PowerProfile als nominale OEM-Angabe,
5. explizit „Nicht verfügbar“.

Für die Gesundheitsanzeige wird ein gültiger Android-SoH-Wert zuerst verwendet,
damit Prozent- und mAh-Anzeige dieselbe Quelle teilen. Fehlt dieser Wert,
folgen lokale Benchmark- und Lademessungen; die separat gelesene `Full Charge
Capacity` vom Batterie-/BMS-Treiber bleibt der letzte Fallback. Keine Quelle
wird als eine andere umetikettiert und jede Kapazität bleibt auf
500–30.000 mAh begrenzt.

Jeder automatische Treffer bleibt auf 500–30.000 mAh begrenzt und wird als
Quelle angezeigt. Ein nicht verfügbarer Wert wird weder als `0 mAh` noch als
Gesundheitsmessung dargestellt.

Die Zykluszählung folgt demselben Prinzip: Androids gemeldeter Wert hat Vorrang,
danach werden nur bekannte Batterie-/BMS-Treiberknoten mit einer festen
Plausibilitätsgrenze gelesen. Fehlt beides, nutzt Ampere als letzte Option eine
vorsichtige lokale EFC-Schätzung aus `BATTERY_PROPERTY_CHARGE_COUNTER`: Es werden
nur stabile Zunahmen während des Ladens addiert; Rücksprünge, Resets und
unplausible Sprünge werden verworfen. Die Anzeige markiert diesen Fallback mit
`~`.

Die gemeinsam verwendeten Regeln sind in `BatteryRulesTest` gegen Androids
Status-/Netzquellenregel und gegen ungültige Zyklusgrenzen abgesichert.

Der lokale EFC-Fallback speichert nach jedem Schritt ausschließlich den
Restanteil eines begonnenen Vollzyklus. Dieser Restanteil wird vor jeder
Berechnung strikt auf endliche Werte in `[0, 1)` geprüft; damit können
beschädigte Preferences keinen künstlichen Zykluszähler oder eine sichtbare
`NaN`-Anzeige erzeugen.

Auch erweiterte Sitzungszeilen validieren ihren EFC-Wert vor der Aufnahme in
die lokale Historie. Das verhindert, dass von `Float.parseFloat` akzeptierte
Sonderwerte wie `NaN` oder `Infinity` die Verschleißgrafik skalieren.

Die übrigen optionalen Felder derselben Zeilen werden ebenfalls typabhängig
geprüft: Start und Ende bleiben echte 0–100-%-Akkustände, Entladewerte sind
auf 0–1000 Zehntelprozent begrenzt und Zeit-/Energiefelder müssen endliche,
nichtnegative Ganzzahlen sein. So kann eine beschädigte Sicherung keine
ungültige Prozentzahl in der Detailansicht oder im Export wieder einschleusen.

Die Android-16/17-Kapazitätsstufe wird in `BatteryCapacityLevel` bewusst nicht
in `BatteryHealth.percent(...)` eingespeist. Ein Systemwert wie `Hoch` oder
`Voll` kann damit niemals versehentlich als `110 %` oder als andere
Gesundheitszahl dargestellt werden. Der separate SoH-Wert wird nur im
strikten Bereich 1–100 akzeptiert; ein OEM-Fehlwert wie `110` wird verworfen,
damit er nicht stillschweigend als `100` ausgegeben wird. Die Intent-Auswertung
und die Trennung zwischen beiden Bedeutungen sind in `BatteryRulesTest`
regressionsgesichert.

Sitzungen werden zusätzlich gegen das eigene gespeicherte Dauerformat und die
Reihenfolge von Start-/Endzeitpunkt geprüft. Das folgt dem Session-Modell
offener Akku-Tracker: unvollständige oder zeitlich rückwärts laufende Abschnitte
werden nicht als reale Nutzung ausgegeben.

Auch Telemetrie-Exporte prüfen die physikalische Richtung des gespeicherten
Stroms: Laden muss positiv, Entladen negativ sein. Widersprüchliche Altzeilen
werden einzeln ausgelassen, damit ein CSV-/JSON-Export keine umgekehrte
Messung als reale Akkuaufnahme ausgibt.

Lokale Kapazitätsproben werden wie bei einer vorsichtigen Health-Schätzung nicht
mehr blind gemittelt: Ampere sortiert die letzten fünf gültigen Proben und nimmt
den Median. Dadurch kann eine einzelne fehlerhafte oder besonders unruhige
Ladesitzung den Gesundheitswert nicht unverhältnismäßig verschieben.

Automatische Proben werden zusätzlich nur nach einer Ladung bis mindestens 95 %
und bei einem während der Sitzung beobachteten stabilen Ladestrom bis 25 mA
gespeichert. Teil- oder noch aktive Ladevorgänge werden nicht als
Full-Charge-Kapazität umetikettiert; das folgt der vorsichtigen FCC-Aufzeichnung
von [PlusPlusBattery](https://github.com/dijia1124/PlusPlusBattery). Der manuelle
Benchmark bleibt der ausdrücklich gestartete Weg für eine vollständige Messung.

Für die Laufzeitprognose bleibt der persönliche lokale 7-Tage-Verlauf die
erste Wahl. Wenn dafür noch keine ausreichenden Daten vorliegen, verwendet
Ampere auf Android 12+ als Fallback `PowerManager.getBatteryDischargePrediction()`
und verwirft `null`, unrealistische Werte sowie Laufzeiten außerhalb von sieben
Tagen. Die Oberfläche nennt dann ausdrücklich die verwendete Quelle.

Zusätzlich wird Androids `EXTRA_CHARGING_STATUS` ab API 34 als reines
Ladeprofil gelesen. Die AOSP-Definition unterscheidet Normalbetrieb, zu kalte/
zu heiße Ladebedingungen sowie Akku-schonende und adaptive Profile. Dieser
Wert beeinflusst weder die Kabelerkennung noch die Sitzungsrichtung; er wird
nur angezeigt, wenn er gültig vorhanden ist.

Die optionalen AOSP-Felder `max_charging_current` und `max_charging_voltage`
werden nach dem von Androids eigener `BatteryStatus`-Logik verwendeten
Strom-mal-Spannung-Prinzip in Milliwatt umgerechnet. Ampere zeigt das Ergebnis
nur mit engen Spannungs-, Strom- und Leistungsgrenzen als „Max. … W“; der
aktuelle Batteriefluss bleibt davon unabhängig.

Temperaturdaten folgen demselben Validierungsprinzip: Android liefert sie als
Zehntelgrad Celsius, aber einzelne Geräte können fehlende oder unplausible
Werte melden. `BatteryTemperature` verwirft deshalb Werte außerhalb von
0,1–100,0 °C zentral, bevor sie in Dashboard, Widget, Overlay, Kachel,
Telemetrie oder Alarm gelangen.

Auch Batteriespannung wird vor Anzeige und Leistungsberechnung zentral als
Millivolt validiert. Der Bereich 1.000–10.000 mV deckt die üblichen ein- und
mehrzelligen Smartphone-/Tablet-Akkus ab; fehlende oder darüberliegende Werte
werden als nicht verfügbar behandelt.

Der persistente Charge-Counter wird ebenfalls nur in der von Android
dokumentierten Microampere-Stunden-Einheit akzeptiert. Werte außerhalb von
500–30.000 mAh werden verworfen, statt einen Hersteller-Sentinel oder eine
unbekannte Einheit in Sitzungen und EFC-Schätzungen einzubauen.

Sitzungen werden wie bei den untersuchten Session-Trackern als begrenzte
Abschnitte behandelt: eine einzelne Zeile darf höchstens den vollständigen
1–100-%-Bereich abdecken. Größere oder richtungswidrige Werte werden verworfen;
wenn ein gültiger Energiezähler vorhanden ist, wird die Änderung daraus
vorsichtig neu abgeleitet.

Für die zusätzliche Live-Leistung orientiert sich Ampere an dem in offenen
Batteriemonitoren üblichen, transparenten Modell `P = I × U`: Androids
gemessener Akkustrom wird mit der Akkuspannung multipliziert. Die Implementierung
prüft Einheiten und Grenzen, zeigt die Größe als Akku-Seitenleistung mit `≈` an
und behauptet damit ausdrücklich keine Leistung an der Steckdose.

Die Temperaturwarnung folgt dem in mehreren offenen Batteriemonitoren üblichen
Muster aus Grenzwert und Hysterese: eine Warnung wird nur einmal ausgelöst und
erst nach deutlicher Abkühlung zurückgesetzt. Ampere speichert die Einstellung
lokal, nutzt den Android-Akkusensor und verändert weder Ladeleistung noch
Systemeinstellungen.

Die Stromquelle wird nach demselben defensiven Prinzip behandelt: `CURRENT_NOW`
wird zuerst gelesen, `CURRENT_AVERAGE` dient nur bei fehlendem/ungültigem
Momentanwert als Fallback. Sentinelwerte und Rohwerte außerhalb eines plausiblen
Bereichs werden vor Umrechnung und Anzeige verworfen. So bleiben Dashboard,
Service, Widget, Overlay und Quick-Settings-Kachel konsistent.

Für die optionale App-Nutzungsansicht verwendet Ampere bevorzugt Androids
`UsageStatsManager.queryEvents()`. Aggregierte `queryUsageStats()`-Tageswerte
können laut Android-Dokumentation über den angefragten Zeitraum hinausreichen;
deshalb bleibt diese API nur der Fallback, wenn keine auswertbaren Ereignisse
vorliegen. Die reine Intervalllogik liegt in `UsageEventAccumulator` und wird
ohne Android-Systemobjekte getestet.

Der laufende Monitor verwendet außerdem einen eigenen `HandlerThread` für
Broadcast-Verarbeitung, Messung und lokale Persistenz. Dadurch bleibt die
Canvas-Oberfläche vom Hintergrund-I/O getrennt; Foreground-Service und
Benachrichtigung werden weiterhin sofort im vorgesehenen Android-Startpfad
initialisiert.

Das Startbildschirm-Widget ist eigenständig als `AppWidgetProvider` umgesetzt.
Es liest den aktuellen Sticky-Akku-Broadcast und `BatteryManager` direkt, wird
bei jedem laufenden Monitor-Sample aktualisiert und hat zusätzlich den von
Android vorgegebenen 30-Minuten-Fallback. Es übernimmt keine Daten und keine
Lizenz aus den GPL-Referenzprojekten; nicht verfügbare Werte bleiben `—`.

Die Schnelleinstellung ist ebenfalls eine reine Informationskachel: Sie ändert
keine Systemeinstellung und öffnet beim Tippen nur Ampere. Für Android 14 und
höher verwendet sie den vorgeschriebenen `PendingIntent`-Startpfad.
