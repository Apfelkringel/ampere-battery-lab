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

Jeder automatische Treffer bleibt auf 500–30.000 mAh begrenzt und wird als
Quelle angezeigt. Ein nicht verfügbarer Wert wird weder als `0 mAh` noch als
Gesundheitsmessung dargestellt.

Die Zykluszählung folgt demselben Prinzip: Androids gemeldeter Wert hat Vorrang,
danach werden nur bekannte Batterie-/BMS-Treiberknoten mit einer festen
Plausibilitätsgrenze gelesen. Fehlt beides, wird kein künstlicher Gesamtzähler
angezeigt.

Die gemeinsam verwendeten Regeln sind in `BatteryRulesTest` gegen Androids
Status-/Netzquellenregel und gegen ungültige Zyklusgrenzen abgesichert.

Das Startbildschirm-Widget ist eigenständig als `AppWidgetProvider` umgesetzt.
Es liest den aktuellen Sticky-Akku-Broadcast und `BatteryManager` direkt, wird
bei jedem laufenden Monitor-Sample aktualisiert und hat zusätzlich den von
Android vorgegebenen 30-Minuten-Fallback. Es übernimmt keine Daten und keine
Lizenz aus den GPL-Referenzprojekten; nicht verfügbare Werte bleiben `—`.

Die Schnelleinstellung ist ebenfalls eine reine Informationskachel: Sie ändert
keine Systemeinstellung und öffnet beim Tippen nur Ampere. Für Android 14 und
höher verwendet sie den vorgeschriebenen `PendingIntent`-Startpfad.
