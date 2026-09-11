# Open-Source-Recherche

Stand: 11. September 2026

Die konfigurierbare Tiefstandwarnung ist von den Alarmfunktionen des GPL-3.0-
Projekts [Battery Monitor](https://github.com/tswistak/Battery-Monitor) inspiriert.
Es wurde kein Code übernommen: Die Implementierung in `BatteryDischargeAlarm`
ist eine eigene, kleine Regelklasse mit validierten Grenzwerten von 5–50 %,
Einmal-Auslösung und 3-%-Hysterese. Damit bleibt das Verhalten lokal, testbar
und kompatibel mit der bestehenden Lade- und Temperaturwarnung, ohne GPL-Code
in die App zu kopieren.

Der optionale Ladebildschirm orientiert sich am Apache-2.0-Projekt
[Dock](https://github.com/Mobinshahidi/Dock). Dock zeigt, wie ein Android-
`DreamService` als systemweiter, während des Ladens aktivierbarer
Screensaver registriert wird. Ampere übernimmt weder dessen UI-Code noch
Assets: `BatteryDreamService` zeichnet eine eigene, minimalistische Canvas-
Ansicht und liest ausschließlich die bereits vorhandenen Android-
`BatteryManager`-Signale. Der Dienst startet nicht selbst, fordert keine neue
Berechtigung an und wird vom Nutzer in den Android-Bildschirmschoner-
Einstellungen aktiviert.

Beim Vergleich mit ABattery wurde außerdem ein konkreter OEM-Randfall
geschärft: ABattery wertet `EXTRA_PLUGGED` mit Bitmasken aus (`and`), während
Ampere die Quelle bisher per Gleichheitsvergleich auswählte. Ampere verwendet
jetzt `BatteryPlugType` als gemeinsame Bitfeld-Regel für Live-Anzeige und
Sitzungsexport; kombinierte oder OEM-erweiterte Werte fallen nicht mehr fälschlich
auf „Externe Stromquelle“ zurück.

Für die Tiefstandwarnung wurde zusätzlich der Alarmpfad aus Battery Monitor
gegen Ampere geprüft: Battery Monitor löst Grenzwert-Alarme beim Übergang über
den Schwellenwert aus. Ampere speichert deshalb nun `dischargeAlarmLastLevel`
und löst nur beim Eintritt in den konfigurierten Bereich aus; die bestehende
3-%-Hysterese verhindert nach dem Reset ein Flattern.

PlusPlusBattery wurde für die Zyklus-Historie zusätzlich bis zur Datenablage
verglichen: Die App führt pro Datum einen Tageswert und aktualisiert einen
bestehenden Tag nur mit einem stärkeren beziehungsweise höheren Messwert. Ampere
übernimmt dieses Verhalten als eigene `BatteryCycleHistory`: 90 Tage werden in
`SharedPreferences` gehalten, ein gemeldeter Android-/BMS-Zähler ersetzt eine
frühere EFC-Schätzung desselben Tages, und rückwärts laufende Werte werden nicht
übernommen. Die Historie erscheint in der Gesundheitsansicht sowie in CSV und
Research-JSON; fremder Code und eine zusätzliche Datenbank wurden nicht übernommen.

ABattery dokumentiert außerdem einen konkreten Full-Charge-Fallback: Wenn kein
`charge_full`-Treiberwert existiert, wird der verbleibende Charge-Counter durch
den aktuellen Ladezustandsanteil geteilt. Ampere prüft diese Implementierung
gegen die eigene Einheiten- und Plausibilitätslogik und verwendet sie nun erst
nach Treiber-/OPlus-Quellen als `Android-Charge-Counter (geschätzt)`. Unter 20 %
Ladezustand wird wegen der starken Fehlerverstärkung nicht hochgerechnet; eine
Schätzung überschreibt nie einen besseren gemeldeten Wert.

Die Vergleichsimplementierung verwendet dafür den Rohanteil `level / scale`,
nicht den bereits auf ganze Prozent gerundeten Wert. Ampere folgt diesem Detail
jetzt ebenfalls in `BatteryFullChargeEstimate`; die bestehende öffentliche
Prozentanzeige darf weiterhin runden, die interne Kapazitätsrechnung verliert
dadurch aber keine zusätzliche Präzision.

Für die „seit Ladung“-Statistik wurde die reine Zustandslogik aus dem GPL-3.0-
Projekt [Device Watch](https://github.com/jrs8205/Device-Watch) separat geprüft:
Device Watch unterscheidet zwischen einem einmaligen Voll-Ladeanker pro
Steckersitzung und einem Absteck-Anker, wenn vor dem Vollwerden getrennt wird.
Ampere bildet dieses Verhalten in der eigenen `BatteryChargeAnchor` nach und
persistiert den Ankertyp, den Zeitpunkt und den Akkustand. Die vorhandenen
Messwerte werden beim Ankerwechsel sauber auf null gesetzt; GPL-Code, Compose-
UI oder dessen Speicherarchitektur wurden nicht übernommen.

Beim App-Verbrauch wurde [OpenMonitor](https://github.com/1orz/OpenMonitor)
gegen Ampere abgegrenzt: OpenMonitor weist den Verbrauch pro App als
geschätzten Drain aus und kombiniert Nutzungsdaten mit Akkuverlauf, nicht als
vom Android-System gemessenen Pro-App-Akkuwert. Device Watch beschreibt die
gleiche Plattformgrenze ausdrücklich. Ampere berechnet deshalb zuerst direkte
Zuordnungen aus lokalen Telemetrieintervallen; wenn deren Summe größer als die
beobachtete Entladeenergie ist, werden alle direkten Werte proportional skaliert.
Für Apps ohne direkte Telemetrie wird die beobachtete Entladung nur nach
Vordergrundzeit verteilt. Beide Pfade bleiben in der Oberfläche mit `~` bzw.
„geschätzt“ gekennzeichnet. Es wurde kein Code aus OpenMonitor oder Device Watch
übernommen.

Battery Monitor 1.4 dokumentiert außerdem einen separaten
`BatteryCurrentMultiplierDetector`: Manche OEMs liefern Stromwerte in einer
falsch skalierten Größenordnung. Die dortige Regel prüft typische
Mindestbereiche getrennt für Laden und Entladen und ignoriert den niedrigen
Strom kurz vor Ladeende. Ampere implementiert dieselbe fachliche Idee als
eigene Java-Regel in `BatteryCurrentMultiplierDetector`: nur die Faktoren
1/10/100/1000 sind möglich, ungültige Werte bleiben unverändert, und die
Richtung wird weiterhin ausschließlich aus Androids Ladezustand abgeleitet.
Es wurde kein GPL-Code kopiert.

Der gleiche Battery-Monitor-Vergleich zeigt beim Ladeziel einen persistenten
`TargetAlarmEvaluator`: Er merkt sich den letzten Prozentwert, feuert nur beim
Überschreiten und hält den Alarmzustand getrennt vom aktuellen UI-Schalter.
Ampere bildet diese Zustandsmaschine als `BatteryChargeAlarm` nach, speichert
`chargeAlarmLastLevel` im Backup und verwendet eine eigene 3-%-Hysterese. Die
erste gültige Messung oberhalb des Ziels bleibt bewusst sofort benachrichtigbar;
danach ist ein echter Aufwärtssprung erforderlich. Der GPL-Code wurde nicht
übernommen.

Für die Hintergrundzuverlässigkeit wurde Battery Monitors
`BackgroundServiceWatchdog` separat verglichen: Ein periodischer
`AlarmManager.setAndAllowWhileIdle`-Termin prüft einen monotonen
`elapsedRealtime`-Heartbeat und fordert nur bei einem veralteten Heartbeat den
Foreground-Service erneut an. Ampere übernimmt dieses Architekturprinzip als
eigene `BatteryMonitorWatchdog`: Der Dienst schreibt alle zehn Minuten, die
Prüfung läuft alle zwanzig Minuten, und erst nach dreißig Minuten ohne
Lebenszeichen wird der Dienst einmalig neu gestartet. Wall-Clock-Sprünge können
die Prüfung nicht täuschen; es wird kein fremder Code übernommen.

Die Spannungsauflösung wurde mit Battery Monitors separatem
[`BatteryVoltageResolver`](https://github.com/tswistak/Battery-Monitor/blob/master/app/src/main/kotlin/codes/swistak/batterymonitor/monitoring/batteryvoltage/BatteryVoltageResolver.kt)
und [`BatteryVoltageValidator`](https://github.com/tswistak/Battery-Monitor/blob/master/app/src/main/kotlin/codes/swistak/batterymonitor/monitoring/batteryvoltage/BatteryVoltageValidator.kt)
verglichen. Der Android-Broadcast bleibt die bevorzugte Quelle; wenn er fehlt
oder außerhalb des plausiblen Bereichs liegt, prüft Ampere nun priorisierte
read-only `voltage_now`-Dateien von Batterie, BMS und Fuel-Gauge-Knoten. Werte
werden sowohl in mV als auch in µV erkannt, der erfolgreiche Pfad wird bis zu
seinem nächsten ungültigen Wert zwischengespeichert, und USB-Eingangsknoten
bleiben ausgeschlossen. Die Java-Implementierung ist eigenständig und kopiert
keinen GPL-Code.

Beam bündelt die Live-Messung in [`Battery.kt`](https://github.com/montafra/beam/blob/master/app/src/main/java/montafra/beam/Battery.kt)
und leitet die Einheiten, Vorzeichen und Leistung in einem unveränderlichen
[`BatterySnapshot`](https://github.com/montafra/beam/blob/master/app/src/main/java/montafra/beam/BatterySnapshot.kt)
ab. Ampere übernimmt dieses Architekturprinzip als eigenen `BatteryReading`:
Widget, Quick-Settings-Kachel, Overlay und Dream lesen jetzt denselben
validierten Snapshot statt Status, Strom, Temperatur und Spannung separat zu
parsen. Wie Beam unterscheidet Ampere dabei einen fehlenden Plug-Wert von einem
expliziten Wert `0`: Der Status `CHARGING`/`FULL` darf bei einem unvollständigen
OEM-Broadcast weiterleben, ein tatsächlich gemeldetes Abstecken bleibt
maßgeblich. Der Foreground-Monitor und das Dashboard behalten ihre zusätzliche
Ladezustands-Stabilisierung für Kabel-Events; sie verwenden danach weiterhin
dieselben zentralen Einheiten- und OEM-Validierer. Es wurde kein Beam-Code
übernommen.

Beams [`BatterySnapshot.secondsUntilCharged`](https://github.com/montafra/beam/blob/master/app/src/main/java/montafra/beam/BatterySnapshot.kt)
verwirft ebenfalls ungültige oder nicht berechenbare Ladezeitwerte und fällt
bei fehlender Systemprognose nur mit ausreichender Energie-, Leistungs- und
Ladezustandsbasis zurück. Ampere nutzt dafür jetzt die eigene, getestete
`BatteryTimeEstimate`: Eine unbekannte Kapazität oder eine nicht endliche Rate
bleibt „Nicht verfügbar“, und Zeit bis zum Vollstand sowie bis zum konfigurierten
Ladeziel teilen dieselbe Begrenzungsregel. Zusätzlich wird ein plausibler
historischer Ladesatz gegenüber einem einzelnen Stromausreißer bevorzugt. Es
wurde kein Beam-Code übernommen.

Für die verbleibende Lade- und Nutzungszeit wurde außerdem [BatteryLog](https://github.com/The412Banner/BatteryLog)
exakt gegen Ampere geprüft. BatteryLog liest im Fuel-Gauge-Fallback
`time_to_full_now`, ersatzweise `time_to_full_avg`, sowie
`time_to_empty_avg` aus dem read-only Power-Supply-Sysfs und verwirft
Sentinelwerte bzw. unrealistische Zeiträume. Ampere übernimmt davon nicht den
Code und benötigt keinen Root-Zugriff: `BatteryFuelGaugeTime` priorisiert
dieselben semantischen Felder über die zentrale Batterie-/BMS-Rangfolge,
normalisiert Sekunden in Minuten, begrenzt auf 48 Stunden und verwendet den
Fallback auch dann, wenn die Android-API für Systemprognosen noch nicht
vorhanden ist. Androids eigene Prognose bleibt die erste Quelle, der Fuel-Gauge
Wert ist klar als solche Quelle gekennzeichnet. Verglichen wurden insbesondere
[`Estimates.kt`](https://github.com/The412Banner/BatteryLog/blob/main/app/src/main/java/com/the412banner/batterylog/Estimates.kt)
und [`BatteryInfo.kt`](https://github.com/The412Banner/BatteryLog/blob/main/app/src/main/java/com/the412banner/batterylog/BatteryInfo.kt).

HeyBattery wurde für die Laufzeitprognose ebenfalls auf Implementierungsebene
geprüft. Die README nennt ein 40/60-Hybridmodell, der aktuelle Code in
[`BatteryDataManager.java`](https://github.com/ghostyapps/HeyBattery/blob/main/app/src/main/java/com/ghostyapps/heybattery/BatteryDataManager.java)
bildet jedoch den Mittelwert der letzten bis zu zehn abgeschlossenen Zyklen;
[`ChargeCycle.java`](https://github.com/ghostyapps/HeyBattery/blob/main/app/src/main/java/com/ghostyapps/heybattery/ChargeCycle.java)
berechnet dafür nur Start-/End-Prozent geteilt durch die Dauer. Ampere kopiert
diesen Code nicht. Stattdessen nutzt `BatteryRuntimeEstimate` die sinnvolle
Idee einer aktuellen Sitzungsgewichtung, ramped sie aber erst nach zehn
Minuten belastbarer Daten hoch und begrenzt sie nach 30 Minuten auf 60 %.
Ein Charge-Counter-Energieabfall kann dabei einen flachen Prozentwert ergänzen;
ungültige oder zu kurze Phasen bleiben unberücksichtigt.

Für Thermik wurde [OpenMonitor](https://github.com/1orz/OpenMonitor) geprüft.
OpenMonitor sammelt mit seinem privilegierten Daemon detaillierte Linux-
Thermal-Zonen; das ist für eine lokale App ohne Root, Shizuku oder ADB nicht
gleichwertig verfügbar. Ampere ergänzt daher bewusst nur den öffentlichen
Android-Status `PowerManager.getCurrentThermalStatus()` ab Android 10 und hält
ihn getrennt von der Akku-Sensortemperatur: „Thermik Hoch“ ist kein behaupteter
Akkuwert. Unbekannte API-/OEM-Werte werden nicht ersetzt und es wurde kein
privilegierter OpenMonitor-Code übernommen.

Das Ladeziel wurde anschließend gegen BatteryLogs [Charge-Control-Beschreibung](https://github.com/The412Banner/BatteryLog)
und das Linux-[power-supply-ABI](https://github.com/torvalds/linux/blob/master/Documentation/ABI/testing/sysfs-class-power)
abgegrenzt. `charge_control_limit` ist dort ein Stromlimit in µA; es darf
nicht als Prozentziel angezeigt werden. Das Prozent-Limit kommt aus
`charge_control_end_threshold`, optional ergänzt durch
`charge_control_start_threshold`. Ampere liest beide Werte über die
priorisierte Batterie-/BMS-Quelle, cached sie kurz und zeigt ein gültiges
Start–Ende-Paar als „OEM-Ladefenster“ neben dem eigenen Alarmziel. Es gibt
keinen Schreibpfad, keinen Root-Fallback und keine automatische Übernahme in
die Alarmregel.

BatteryLogs [`ReportGenerator.kt`](https://github.com/The412Banner/BatteryLog/blob/main/app/src/main/java/com/the412banner/batterylog/ReportGenerator.kt)
führt außerdem eine kompakte Anomalie-Sektion: abgeschlossenes Logging über
dem Nullpunkt wird als „Early Cutoff“ markiert, ein AYANEO-2S-Wert bis etwa
6,2 V oberhalb 5 % als möglicher Spannungseinbruch, und mindestens 48 °C als
hohe Temperatur. Ampere übernimmt die Idee einer reproduzierbaren Diagnose,
aber nicht diese gerätespezifischen Schlussfolgerungen: Unsere Telemetrie wird
vorher validiert und chronologisch sortiert; `BatteryTelemetryDiagnostics`
meldet nur beobachtbare Minimalspannung, Spitzen-Entladestrom,
Maximaltemperatur und echte Sampling-Lücken. Ein laufender Entladevorgang ist
kein „Early Cutoff“, und es gibt keine feste 2S-Spannungsgrenze für
1S-Smartphones. Dieselbe Zusammenfassung erscheint lokal im Verlauf und wird
im Research-JSON mit exportiert.

Zusätzlich bietet Ampere daraus einen eigenen lesbaren TXT-Bericht über den
bestehenden Exportdialog an. Das folgt BatteryLogs Report-Idee, verwendet aber
keine gerätespezifischen Designkapazitäten oder festen Packgrenzen und erzeugt
keine neue Messung beim Export. Bericht, CSV und Research-JSON greifen auf
dieselbe validierte Datenbasis zu.

ABattery liest im [`BatteryDataSource`](https://github.com/abanana84/abattery/blob/main/app/src/main/java/com/abanana/abattery/data/battery/BatteryDataSource.kt)
die öffentliche `EXTRA_TECHNOLOGY`-Angabe aus `ACTION_BATTERY_CHANGED` und
führt sie im Akku-Modell. Ampere übernimmt nur dieses standardisierte
Eingangssignal als eigenen `BatteryTechnology`-Adapter, validiert Länge und
Steuerzeichen und zeigt den Wert in der Gesundheitsansicht sowie im
Research-JSON. Herstellungsdatum und Erstnutzung werden dagegen nicht aus
den geschützten AOSP-BatteryManager-Properties behauptet: Diese verlangen
`BATTERY_STATS` und bleiben ohne passende Berechtigung korrekt nicht verfügbar.

Bei der Vordergrund-App-Zeit wurde [Device Watchs
`UsageEventAggregator`](https://github.com/jrs8205/Device-Watch/blob/main/app/src/main/java/org/jarsi/devicewatch/data/UsageEventAggregator.kt)
exakt gegen Ampere geprüft. Device Watch führt pro Paket ein Set aktiver
Activity-Klassen, weil manche Geräte `PAUSED` auslassen und andere sowohl
`PAUSED` als auch `STOPPED` für dieselbe Activity liefern. Ampere verwendet
jetzt dieselbe fachliche Zustandsgrenze in `UsageEventAccumulator`: Ein Wechsel
von Activity A zu B bleibt eine zusammenhängende Vordergrundsitzung, doppelte
Schließereignisse sind idempotent, und `SCREEN_NON_INTERACTIVE` schließt alle
offenen Pakete. Die Implementierung ist eigener Java-Code; Device-Watch-Code
wurde nicht kopiert.

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
- Die gleiche [PlusPlusBattery-OPlus-Unterstützung](https://github.com/dijia1124/PlusPlusBattery)
  dokumentiert `battery_fcc` und `battery_soh` unter dem OPlus-Ladecontroller.
  Ampere übernimmt nur die Pfad-Idee und eigene read-only Leser mit
  500–30.000-mAh- beziehungsweise 1–100-%-Grenzen, keinen Quellcode.
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
- [BatteryLog](https://github.com/TheDeathDragon/BatteryLog), MIT: trennt die
  Aufzeichnung von Akkuereignissen von der späteren Anzeige und verhindert,
  dass Status-/Level-Rauschen die Historie unnötig vergrößert. Als Muster wurde
  hier die zentrale Validierung vor jeder Auswertung genutzt; der Quellcode
  wurde nicht übernommen.
- [Battery Monitor](https://github.com/tswistak/Battery-Monitor), GPL-3.0:
  die aktuelle Version priorisiert Power-Supply-Knoten anhand von `type` und
  Namen. Ampere verwendet davon nur das allgemeine Auswahlprinzip und eine
  eigene Implementierung; GPL-Code wurde nicht übernommen.

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

Wenn Standard-Power-Supply-Knoten auf einem OPlus-Gerät fehlen, prüft
`BatteryCapacity` danach die read-only Pfade
`/sys/class/oplus_chg/battery/battery_fcc` beziehungsweise
`battery_soh`. Die Daten werden nur bei plausibler Einheit und innerhalb der
fachlichen Prozentgrenze verwendet; Androids Dateirechte können den Pfad
verbergen, ohne dass die App dafür Root anfordert.

Die Android-15-Quelle führt `BATTERY_PROPERTY_STATE_OF_HEALTH` als
feature-flagged BatteryManager-Eigenschaft. Deshalb setzt Ampere keine starre
API-36-Grenze: Auf Android 14–17 und bei OEM-Backports wird die Eigenschaft
versuchsweise gelesen, aber nur bei einem Wert von 1–100 akzeptiert. Fehlt das
Feld oder liefert der Dienst einen ungültigen Wert wie 110, wird die Quelle
verworfen und die lokale Kapazitätshierarchie verwendet.

Referenzen: [Android BatteryManager](https://developer.android.com/reference/android/os/BatteryManager),
[Android-15-BatteryManager-Quelle](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/core/java/android/os/BatteryManager.java),
[ABattery](https://github.com/abanana84/abattery) und
[BatteryLog](https://github.com/TheDeathDragon/BatteryLog).

Zusätzlich nutzt Ampere den standardisierten Linux-
[power_supply-`state_of_health`-Knoten](https://github.com/torvalds/linux/blob/master/Documentation/ABI/testing/sysfs-class-power)
als read-only OEM-Fallback. Die qualitative Datei `health` wird bewusst nicht
als Prozent interpretiert; nur ein expliziter Integer von 1 bis 100 ist gültig.

Für Widgets folgt Ampere dem offiziellen Android-12-Muster für responsive
`RemoteViews`: Das MIT-lizenzierte [LYRIQ Battery Widget](https://github.com/omarzanji/lyriq-battery-widget)
verwendet ebenfalls mehrere Größenpunkte mit `RemoteViews(Map<SizeF, RemoteViews>)`.
Ampere übernimmt nur das allgemeine Android-API-Muster, nicht dessen Fahrzeug-,
Smartcar- oder Home-Assistant-Code.

Die Canvas-Bedienung folgt für Screenreader dem offiziellen Muster für eine
virtuelle View-Hierarchie: Android beschreibt dafür einen
[`AccessibilityNodeProvider`](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeProvider)
als passende Schnittstelle für komplexe Custom Views. Das
[Android-TV-Accessibility-Beispiel](https://github.com/android/tv-samples/tree/main/AccessibilityDemo)
zeigt denselben Ansatz; Ampere verwendet nur die API-Idee für die eigenen
Header- und Tab-Flächen.

Die gemeinsame `BatterySupplyRules`-Rangfolge prüft vor dem Dateinamen den
deklarierten Power-Supply-Typ. Dadurch werden USB-/Netzeingänge auch dann
ausgeschlossen, wenn ein OEM ihnen einen irreführenden Namen gibt; Batterie,
BMS und Fuel-Gauge werden in einer stabilen Reihenfolge gelesen.

Wie bei historischen Batterie-Loggern wird die lokale Zeitreihe vor
aufeinanderfolgenden Raten- und Diagramm-Berechnungen chronologisch sortiert.
Wenn die Geräteuhr rückwärts korrigiert wurde, verwirft Ampere außerdem die
offene Sitzungsgrenze statt eine künstliche Ein-Minuten-Sitzung zu speichern.

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

Für Samsung-Kompatibilität übernimmt Ampere zusätzlich nur den expliziten,
lesbaren `fg_asoc`-Treiberwert als ASOC-SoH-Fallback. Dieser Pfad ist in
offenen Samsung-Diagnoseprojekten als sekundäre Quelle dokumentiert; er wird
gegen denselben Bereich 1–100 geprüft und als `Samsung-Batterietreiber (ASOC)`
gekennzeichnet. Das qualitative Linux-Attribut `health` bleibt absichtlich
ausgeschlossen, weil `Good`/`Dead` kein Prozentwert ist. Es wurde kein
proprietärer oder GPL-Code kopiert.

Für den Live-Strom liest Ampere zuerst `BatteryManager.CURRENT_NOW` und
`CURRENT_AVERAGE`. Wenn ein OEM dort keinen verwertbaren Wert liefert, folgt
ein strikt lesender Fallback auf `current_now` beziehungsweise `current_avg`
der Batterie-/BMS-Knoten unter `/sys/class/power_supply`. Die Linux-ABI
definiert diese Werte in Mikroampere und mit einem Vorzeichen für Laden bzw.
Entladen; Ampere verwendet hier nur den Betrag, weil die Sitzungsrichtung
bereits aus dem bestätigten Android-Ladezustand stammt. USB-Knoten werden nicht
als Akku-Stromquelle verwendet. Das folgt dem standardisierten
[power_supply-ABI](https://github.com/torvalds/linux/blob/master/Documentation/ABI/testing/sysfs-class-power)
und dem robusten „CURRENT_NOW, dann Durchschnitt“-Muster aus offenen
Batteriemonitoren wie [Beam](https://github.com/montafra/beam).

Sitzungen werden zusätzlich gegen das eigene gespeicherte Dauerformat und die
Reihenfolge von Start-/Endzeitpunkt geprüft. Das folgt dem Session-Modell
offener Akku-Tracker: unvollständige oder zeitlich rückwärts laufende Abschnitte
werden nicht als reale Nutzung ausgegeben.

Auch Telemetrie-Exporte prüfen die physikalische Richtung des gespeicherten
Stroms: Laden muss positiv, Entladen negativ sein. Widersprüchliche Altzeilen
werden einzeln ausgelassen, damit ein CSV-/JSON-Export keine umgekehrte
Messung als reale Akkuaufnahme ausgibt.

Diese Prüfung wird jetzt vor beiden Exportformaten über dieselbe Filterfunktion
ausgeführt. Dadurch können beschädigte CSV-Altzeilen nicht mehr an der
JSON-Prüfung vorbei in den normalen Nutzerexport gelangen.

Lokale Kapazitätsproben werden wie bei einer vorsichtigen Health-Schätzung nicht
mehr blind gemittelt: Ampere sortiert die letzten fünf gültigen Proben und nimmt
den Median. Dadurch kann eine einzelne fehlerhafte oder besonders unruhige
Ladesitzung den Gesundheitswert nicht unverhältnismäßig verschieben.

Automatische Proben werden zusätzlich nur nach einer Ladung bis mindestens 95 %
und dem zuletzt gültigen Ladestrom bis 25 mA
gespeichert. Teil- oder noch aktive Ladevorgänge werden nicht als
Full-Charge-Kapazität umetikettiert; das folgt der vorsichtigen FCC-Aufzeichnung
von [PlusPlusBattery](https://github.com/dijia1124/PlusPlusBattery). Der manuelle
Benchmark bleibt der ausdrücklich gestartete Weg für eine vollständige Messung.

Für die Sitzungsrichtung bleibt ein `POWER_CONNECTED`-/`POWER_DISCONNECTED`-
Event nur als kurzer Synchronisationshinweis aktiv. Nach fünf Sekunden fällt
Ampere auf den aktuellen `ACTION_BATTERY_CHANGED`-Status zurück; so wird ein
verlorenes Kabel-Event nicht zu einem dauerhaft falschen Ladezustand.

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
Strom-mal-Spannung-Prinzip in Milliwatt umgerechnet. ABattery liest dieselben
Felder mit den literalen Schlüsseln, weil SDK-Stubs die Konstanten nicht immer
bereitstellen; Ampere folgt diesem Kompatibilitätsdetail in einem eigenen
Reader. Die Rohwerte für maximalen Strom und maximale Spannung werden nun
getrennt vom abgeleiteten Leistungswert validiert und im Ladeprofil angezeigt.
Bei einem fehlenden Teilpaar bleibt nur der einzeln plausible Wert sichtbar;
die Leistung wird ausschließlich aus einem vollständigen plausiblen Paar
berechnet. Der aktuelle Batteriefluss bleibt davon unabhängig.

Als separaten Fallback wurde außerdem Capacity Info auf Implementierungsebene
verglichen: Die App liest `constant_charge_current_max` aus dem Batterie-Kernel
und bezeichnet ihn als „Charging Current Limit“. BatteryLog und das Linux-
[power-supply-ABI](https://github.com/torvalds/linux/blob/master/Documentation/ABI/testing/sysfs-class-power)
unterscheiden dieses Hardware-/Reglerlimit jedoch von der externen
Adapterleistung und vom momentanen Strom. Ampere liest deshalb nur den
read-only Knoten aus priorisierten Batterie-/BMS-Versorgungen, cached den
erfolgreichen Pfad kurz und zeigt ihn als „Ladehardware max.“. Das ähnliche
`charge_control_limit_max` wird bewusst nicht als Stromwert verwendet, weil
OEMs dort auch Stufen-/Indexwerte veröffentlichen können. Es gibt keinen
Schreibpfad und keinen Root-Zwang. Verglichen wurden [Capacity Info](https://github.com/Ph03niX-X/CapacityInfo)
und [BatteryLog](https://github.com/The412Banner/BatteryLog).

Ein weiterer ABI-Vergleich betrifft `internal_resistance`: Die aktuelle Linux-
[power-supply-Definition](https://github.com/torvalds/linux/blob/master/Documentation/ABI/testing/sysfs-class-power)
führt diesen Wert als dynamischen Equivalent Series Resistance in µΩ und weist
ausdrücklich auf seine Abhängigkeit von Ladezustand, Temperatur und Lade-
 beziehungsweise Entladezustand hin. Die untersuchten Android-Apps lösen das
meist nur über Root-, Shizuku- oder Herstellerpfade. Ampere liest den
standardisierten Knoten, wenn er als read-only Batterie-/BMS-Attribut vorhanden
ist, validiert ihn streng und zeigt ihn nur als ESR-Diagnose. Er wird weder als
Akkugesundheits-Prozentwert noch für eine künstliche Kapazitätsrechnung benutzt.

Die ABI definiert außerdem `capacity_error_margin` als maximale erwartete
Messunsicherheit des Fuel-Gauges in Prozent: Werte nahe null gelten nach einer
Kalibrierung als präziser, 100 % als praktisch unbrauchbar. Ampere liest diesen
read-only Wert nur aus Batterie-/BMS-Knoten, behandelt 0 ausdrücklich als
gültig und hält ihn getrennt von Akkualterung, SoH und Kapazitätsberechnung.
Damit wird keine Genauigkeit erfunden, wenn ein Gerät das Attribut nicht
bereitstellt.

Capacity Info nennt außerdem Minimum, Durchschnitt und Maximum der Lade-/Entlade-
ströme als eigenständige Messwerte. Ampere übernimmt dieses überprüfbare
Produktmuster, berechnet die drei Kennzahlen aber aus der bereits validierten,
richtungsgetrennten lokalen Zeitreihe und kapselt es in `BatteryCurrentStats`.
Null-, Negativ- und fehlende Werte werden ausgelassen; es wird kein
Momentanwert als Verlauf ausgegeben.

Für die aktive Ladeart wurde ebenfalls die aktuelle Linux-ABI-Definition mit
Implementierungen aus [BatteryLog](https://github.com/The412Banner/BatteryLog)
und dem [AYANEO-Plattformtreiber](https://github.com/ShadowBlip/ayaneo-platform)
verglichen. `charge_type` liefert dort einen einzelnen aktiven Algorithmus;
`charge_types` liefert eine Liste mit dem aktiven Wert in eckigen Klammern.
Ampere liest beide Attribute nur, akzeptiert ausschließlich bekannte Werte wie
Fast, Standard, Trickle, Adaptive, Long Life und Bypass und hält das Ergebnis
getrennt von Androids `EXTRA_CHARGING_STATUS`. Es werden keine Schreibpfade,
Root- oder Herstellerbefehle übernommen.

Das tatsächliche Ladeverhalten wurde separat gegen die Linux-ABI und den
[AYANEO-Plattformtreiber](https://github.com/ShadowBlip/ayaneo-platform)
verglichen. `charge_behaviour` ist nicht dasselbe wie `charge_type`: Es
beschreibt, ob normal geladen, das Laden bei angeschlossenem Netzteil gesperrt
oder die Entladung erzwungen wird. Die ABI nennt `auto`, `inhibit-charge`,
`inhibit-charge-awake` und `force-discharge`; einige Treiber liefern den
aktiven Wert als `[auto]`. Ampere akzeptiert ausschließlich diese vier Werte,
zeigt sie read-only in Dashboard, Benachrichtigung und Research JSON und
führt keine der im Linux-/Treiberbeispiel dokumentierten Schreiboperationen
aus. Dadurch bleibt ein Bypass-/Inhibit-Zustand sichtbar, ohne eine
Steuerfunktion zu versprechen.

Als weiterer universeller Diagnosewert wurden die Linux-ABI-Felder
`manufacture_year`, `manufacture_month` und `manufacture_day` gegen die
Herstellungsdaten-Implementierungen in [MyBattery](https://github.com/Alyaqdhans/MyBattery)
und [Samsung Battery Life Checker](https://github.com/tausifzaman/Samsung-Battery-Life-Checker)
abgegrenzt. Die Samsung-Projekte benötigen dafür proprietäre `LLB MAN`-Logs;
Ampere übernimmt diesen privilegierten bzw. herstellerspezifischen Pfad nicht,
sondern liest nur die standardisierten Batterie-/BMS-Dateien. Alle drei
Komponenten müssen einen gültigen Gregorianischen Tag ergeben, bevor Datum,
Quelle oder Export sichtbar werden.

Temperaturdaten folgen demselben Validierungsprinzip: Android liefert sie als
Zehntelgrad Celsius, aber einzelne Geräte können fehlende oder unplausible
Werte melden. `BatteryTemperature` verwirft deshalb Werte außerhalb von
0,1–100,0 °C zentral, bevor sie in Dashboard, Widget, Overlay, Kachel,
Telemetrie oder Alarm gelangen. BatteryLog bildet aus gültigen Verlaufspunkten
zusätzlich Min/Max/Ø; Ampere übernimmt dieses Auswertungsmuster in den
gemeinsamen `BatteryTelemetryDiagnostics`-Pfad und nutzt es dadurch in UI und
Research-Export, ohne einen einzelnen Live-Moment als Tagesstatistik auszugeben.

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

Die neue Leistungsstatistik wurde auf Implementierungsebene mit ABatterys
[`BatteryDataSource`](https://github.com/abanana84/abattery/blob/main/app/src/main/java/com/abanana/abattery/data/battery/BatteryDataSource.kt)
und der dokumentierten Watt-Anzeige von
[CapacityInfo](https://github.com/Ph03niX-X/CapacityInfo) verglichen: Beide
leiten die Akku-Seitenleistung aus Strom und Spannung ab, statt sie als
Steckdosenleistung auszugeben. Ampere verwendet dafür weiterhin den eigenen
validierten `BatteryPower`-Adapter und berechnet daraus jetzt zusätzlich
getrennte Laden-/Entladen-Min/Ø/Max-Werte. Diese Werte werden erst aus den
bereits gespeicherten 11-Spalten-Telemetrierows abgeleitet; dadurch bleibt die
Legacy-Telemetrie kompatibel und es entsteht keine zweite persistierte
Wahrheit. Der CSV-Export ergänzt nur die abgeleitete `battery_power_mw`-Spalte.

Die Button-Oberfläche wurde anschließend auf Androids 320-dp-Emulator geprüft.
Navigation, Header-Aktionen, Toggle-Zeilen, Benchmark-CTA und CSV-Aktion
verwenden nun einen gemeinsamen `smoothButton`-Adapter mit konsistentem
Radius, Outline, aktivem Ton und Press-State. Die sichtbaren Press-Flächen und
die Accessibility-Bounds teilen dieselben Maße; die Compact-Gesundheitszeile
trennt Temperaturstatistik und Vollzyklen in zwei Label-/Wertzeilen, damit
keine Textkollision entsteht. OLED-, Dark- und Light-Varianten bleiben tonal
getrennt, während aktive Zustände zusätzlich textlich erkennbar sind.

Für die nächste Button-Runde wurden die aktuellen Open-Source-Referenzen von
[Material Components Android](https://github.com/material-components/material-components-android/blob/master/docs/components/Button.md),
den [Material-Web-Button-Tokens](https://github.com/material-components/material-web/blob/main/docs/components/button.md)
und dem [Lucide-Iconprojekt](https://github.com/lucide-icons/lucide) als
Verhaltens- und Lizenzreferenz geprüft. Übernommen wurden nur die
übertragbaren Prinzipien: tonal getrennte Button-Rollen, sichtbarer Press-State,
große Touch-Ziele und konsistente Outline-/Icon-Geometrie. Die Canvas-Controls
bleiben eine eigene, lokale Implementierung ohne zusätzliche Bibliotheks- oder
Netzwerkabhängigkeit. Die neue visuelle Sprache heißt intern „Energy Rail“:
aktive Controls erhalten einen schmalen Energie-Fuß, ruhige Innenkanten und
eine zurückhaltende Materialstaffelung statt Neon-Glow, Schattenwolken oder
flächendeckender Pillen.
