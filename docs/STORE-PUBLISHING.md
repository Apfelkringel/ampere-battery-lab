# Store-Veröffentlichung

## Aktueller Stand (25. September 2026)

Der neueste öffentliche Play-AAB ist weiterhin 0.493 / Code 493, SHA-256
f226f6990668a5187c5e083dc1f719364bbda2586b315351e0901bb49b65d383.
Submission 65 wurde veröffentlicht. Die geschlossene Alpha bietet 0.493
und den Kompatibilitäts-Fallback 0.459 in 177 Regionen für 20.367 unterstützte
Geräte. Das Dashboard zeigte zuletzt 12 Tester für 2 zusammenhängende Tage; die
14-Tage-Anforderung ist offen und Production inaktiv. Managed Publishing ist
aktiviert.

### Neuester Console-Stand (25. September 2026, 14:29 UTC / 16:29 Berlin)

Submission 66 wurde nach ausdrücklicher Nutzerfreigabe am 25. September um
16:08 Uhr Berliner Console-Zeit veröffentlicht. Sie enthielt exakt 85
Store-Eintragsänderungen: den neuen en-US-Standardeintrag sowie Titel, Kurz-
und Vollbeschreibung für 28 bestehende Sprachen. Keine Binärdatei, kein Track
und keine Testerzuordnung waren enthalten. Die Publishing-Übersicht ist jetzt
leer; Managed Publishing bleibt aktiv. Die öffentliche deutsche Play-Seite
zeigt `AkkuTakt: Akku-Monitor` und `Akku-Monitor: Ladestatus, Verlauf und
Batterienutzung — lokal, ohne Konto.`. Die 28 maschinell übersetzten
Lokalisierungen wurden nicht nativsprachlich gegengelesen. Der
Entwicklerkonto-Byline-Name `Ampere Battery Lab` ist weiterhin getrennt und
wurde nicht geändert.

Die geschlossene Alpha enthält weiterhin `0.493`/Code 493 plus den bestätigten
API-23-Fallback `459` (177 Regionen, 20.367 Geräte). Dashboard: 12 Tester für
2 zusammenhängende Tage; 14-Tage-Kriterium unvollständig, Produktionszugriff
deaktiviert. Das aktuelle Play-AAB ist weiterhin 0.493/493 mit SHA-256
`f226f6990668a5187c5e083dc1f719364bbda2586b315351e0901bb49b65d383`.
Der lokale Android-Quellstand ist für die AkkuTakt-Appanzeige auf 0.494/Code
494 angehoben und hat beide Debug-Flavors (je 444 Unit-Tests), Lint und
Debug-Build unter Java 17 bestanden. Er ist noch nicht signiert oder
veröffentlicht. Vor dem Alpha-Upload erneut vollständige Console/API-
Vorprüfung, öffentlichen Bundle-Hash, `minSdk 24`, steigenden Code und
Kontinuität von Fallback 459 bestätigen. Das Validation-only-Ergebnis
[36146448627](https://github.com/Apfelkringel/ampere-battery-lab/actions/runs/36146448627)
bestätigte zuletzt API-Codes `[459,493]` passend zur Console und lud nichts
hoch, weil 493 bereits aktiv war.

Die letzte Play-Wachstumsprüfung ergab 17 Geräte-Impressionen, 2 Akquisitionen
und 2 Erstöffnungen in 28 Tagen; monatlich aktive Geräte und 7-Tage-Bindung
sind nicht verfügbar. Das getrennte 90-Tage-Store-Eintragsfeld zeigt 66,67 %
Conversion. Keine Experimente bei dieser Datenbasis. Vitals liefert aktuell
keine numerischen Crash-/ANR-Raten; der Pre-Launch-Bereich hat noch keinen
Bericht und verlangt einen Artefakt-Upload. Nach dem nächsten Alpha-Upload
Bericht prüfen. Keine neue Richtlinien-/Sicherheitsblockade beobachtet.

Die lokalen Store-Metadaten enthalten AkkuTakt mit einem übersetzten
Akku-/Batterie-Monitor-Begriff in allen sieben unterstützten Sprachen:
Deutsch, Englisch, Spanisch, Französisch, Italienisch, Brasilianisches
Portugiesisch und Niederländisch. Titel, Kurz- und Vollbeschreibungen liegen
für Google Play lokalisiert vor; Apple-Quellen enthalten Name, Untertitel,
Suchbegriffe, Werbetext und Beschreibung. Das sind Quelltexte, keine Console-
Änderung: die Play-Console-Entwürfe wurden dadurch zunächst nicht geändert.
Der spätere Console-Stand ist oben dokumentiert: Submission 66 enthält die
autorisierte Umbenennung und Beschreibung für alle Console-Sprachen, ist aber
noch nicht live. Diese Änderungen nicht mit einem Alpha-Upload bündeln. Eine
Store-Veröffentlichung braucht eine separate, aktuelle Prüfung und Freigabe;
die ausdrückliche Freigabe für Submission 66 liegt vor.

Die neue Namenswahl ist eine vorläufige Risikoreduktion, keine rechtliche
Freigabe. Eine exakte Textsuche im DPMAregister ergab keinen Treffer für
AkkuTakt; eine vollständige Ähnlichkeitsrecherche in EUIPO/TMview und WIPO
sowie eine Prüfung nicht eingetragener Kennzeichen ist damit nicht belegt.
Vor einer breiten kommerziellen Nutzung oder Markenanmeldung eine fachkundige
Recherche durchführen.

## Android

Das Projekt erzeugt zwei bewusst getrennte Varianten:

- `directRelease` ist die signierte GitHub-APK für Nutzer außerhalb von Google Play. Sie darf den ausdrücklich bestätigten APK-Update-Dialog verwenden.
- `playRelease` ist das Google-Play-App-Bundle. Es enthält weder `REQUEST_INSTALL_PACKAGES` noch den APK-Update-Receiver; Aktualisierungen kommen ausschließlich über Google Play.

### Autorisierter Play-Release-Weg

Der manuelle GitHub-Workflow `.github/workflows/publish-google-play.yml` ist
dispatch-only, liest Version, Code und SHA-256 dynamisch aus `latest.json` und
ist fest auf den vorhandenen Track `alpha` mit Status `completed` eingestellt.
Er führt keinen Upload aus, wenn `console_preflight_confirmed` nicht bestätigt
ist oder `validate_only` gewählt wurde. Die Play-Variante muss weiterhin
`minSdk 24` haben. Der Workflow prüft den öffentlichen AAB-Hash, die Play-
Publisher-Berechtigung, den aktuellen API-Track, nicht steigende Versionscodes,
Draft-/Halted-Releases und den Code-459-Fallback. Er ersetzt keine manuelle
Console-Kontinuitätsprüfung.

Vor jedem Dispatch in der authentifizierten Console frisch verifizieren:

1. Es gibt keine Submission in Review und keine ausstehenden, nicht
   eingereichten oder fremden Console-Änderungen. `edits.commit` kann sonst
   mehr als den gewünschten Track-Release betreffen.
2. Der Alpha-Track ist weiterhin der beabsichtigte aktive Track; Console und
   Play-API stimmen bei aktiven Versioncodes überein.
3. `play`-Flavor `minSdk` ist exakt 24; Code `459` ist weiterhin der bestätigte
   API-23-Fallback. Andere alte Codes nur behalten, wenn Play bestätigt, dass
   sie Geräte bedienen, die das neue Bundle nicht unterstützt.
4. Das neueste öffentliche Play-AAB, sein Paket-/Versionscode und SHA-256 passen
   exakt zu `latest.json`. Bei jeder Abweichung, einem fehlenden Fallback,
   einem nicht monoton steigenden Code, einem Draft/Halted-Release oder
   unklarer Tester-Kontinuität stoppen.

Nach erfolgreichem Upload zuerst die von Google erstellte Submission öffnen.
Managed Publishing bedeutet, dass Review-Freigabe und Live-Schaltung getrennte
Schritte sind. Unter der bestehenden Autorisierung darf nach Freigabe nur die
eine binäre Alpha-Änderung mit verifiziertem Fallback veröffentlicht werden;
vor dem Klick Submission und gesamte Warteschlange erneut prüfen. Niemals
dadurch Listings, Internal, Open Testing oder Production veröffentlichen.

### Store-Metadaten und Datenschutz

Die primäre deutsche Quelle und die sechs weiteren unterstützten
Google-Play-Lokalisierungen liegen unter store/google-play/listing/. Jede
Sprache hat einen lokalisierten App-Titel, eine Kurzbeschreibung und eine
Vollbeschreibung. app-name-es-ES.txt spiegelt den spanischen Titel für
bestehende Automationen. tooling/validate-google-play-metadata.sh prüft alle
sieben Titel und Beschreibungen gegen die Play-Grenzen und die einheitliche
AkkuTakt-Marke. Die Apple-Metadaten liegen für dieselben sieben Sprachen unter
store/apple-app-store/metadata/ und enthalten Titel, Untertitel, Suchbegriffe,
Werbetext und Beschreibung. Diese Dateien sind lokale Quellen, kein Nachweis,
dass Console oder öffentliche Store-Seiten synchron sind; den öffentlichen
Eintrag nach jeder separat autorisierten Listing-Korrektur verifizieren.

Der Play-Datensicherheitsstatus wurde am 25. September 2026 read-only
kontrolliert: keine Erklärungen warten auf Prüfung; elf sind abgeschlossen.
Die Datensicherheitsübersicht nennt drei erhobene/geteilte Datentypen,
Verschlüsselung bei Übertragung und automatische Löschung. Vor Änderungen
immer `AnalyticsTracker.java`, `android/app/google-services.json`, aktuelle
Firebase-/Console-Freigaben und `PRIVACY.md` gemeinsam prüfen. Die App erfasst
Analytics nur nach Einwilligung und übermittelt keine Akku-Messwerte oder
Vordergrund-App-Namen. Diese Zusammenfassung ersetzt keine aktuelle
Feld-für-Feld-Prüfung oder Datenschutzfreigabe.

Vier aktuelle Smartphone-Screenshots sind für Play-Empfehlungsformate zu
klein (668×1188; Google verlangt dort vier Bilder mindestens 1080×1920).
Nur authentische, repräsentative App-Screens auf einem passenden Gerät
verwenden; niemals Emulator-Batteriewerte fingieren oder bestehende Bilder
hochskalieren. Store-Listing-Tests bleiben bei der derzeitigen sehr kleinen
Trafficbasis ausgesetzt.

## iOS

Das Repository enthält jetzt einen nativen SwiftUI-Port unter `ios/` mit eigenem Xcode-Projekt. Er zeigt die von `UIDevice` tatsächlich verfügbaren Werte (Akkustand und Ladezustand), speichert lokale Messpunkte und kennzeichnet Android-exklusive Strom-/Kapazitätsdaten als nicht verfügbar. Der Android-Batterie- und Hintergrunddienst wird nicht vorgetäuscht.

Der iOS-Workflow erstellt auf einem macOS-Runner ein unsigniertes Release-Build zur Prüfung. Für die App-Store-Veröffentlichung müssen anschließend ein Apple-Developer-Team, eine Bundle-ID, Zertifikate/Provisioning, App-Store-Connect-Metadaten und ein signierter Archive-Upload hinterlegt werden.

Für iOS ist die Bundle-ID `com.ampere.batterylab` im Projekt festgelegt. Benötigt
werden ein Apple-Developer-Team, eine passende Bundle-ID, ein Distribution-
Zertifikat mit Provisioning Profile sowie ein App-Store-Connect-API-Schlüssel
für den signierten Archive-Upload. Die Texte für App Store Connect liegen unter
`store/apple-app-store/metadata/`; die öffentliche Datenschutz-URL ist dieselbe
wie oben.

Der manuelle Workflow `Publish iOS build to App Store Connect` erwartet dafür
die geschützten Secrets `AMPERE_IOS_CERTIFICATE_P12_BASE64`,
`AMPERE_IOS_CERTIFICATE_PASSWORD`, `AMPERE_IOS_PROVISIONING_PROFILE_BASE64`,
`AMPERE_IOS_PROVISIONING_PROFILE_NAME`, `AMPERE_IOS_KEYCHAIN_PASSWORD`,
`AMPERE_IOS_TEAM_ID`, `AMPERE_ASC_ISSUER_ID`, `AMPERE_ASC_KEY_ID` und
`AMPERE_ASC_API_PRIVATE_KEY`. Ohne diese Signaturdaten wird bewusst kein
unsignierter Build als Store-Upload ausgegeben.


## Produktname: lokale Quellen

Die Anzeige- und Store-Quellen heißen jetzt AkkuTakt. Die lokalen
Google-Play-Quelldateien sind noch keine Console-Änderung und keine
Veröffentlichung. Den bestehenden Paketnamen, die Bundle-ID und den Direct-
APK-Dateinamen wegen Update-Kompatibilität beibehalten. Die Namensrecherche ist
vorläufig und ersetzt keine Ähnlichkeitsprüfung in den offiziellen Marken-
Datenbanken oder eine Rechtsberatung.
