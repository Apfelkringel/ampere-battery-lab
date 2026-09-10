# Open-Source-Recherche

Stand: 10. September 2026

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
