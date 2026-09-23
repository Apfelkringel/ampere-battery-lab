# Store-Veröffentlichung

## Aktueller Stand (15. September 2026)

Der aktuelle Android-Release wird über den signierten GitHub-Workflow und das
öffentliche Updates-Repository veröffentlicht. Die jeweils gültige Version,
Download-Adressen und Prüfsummen stehen in `latest.json` im Updates-Repository.

In der Play Console ist `Verwaltete Veröffentlichung` seit dem 23. September
2026 aktiviert. Nach einer Prüfung werden die meisten Änderungen – darunter
Store-Einträge und vollständige bzw. gestaffelte Releases – dadurch erst nach
einer separaten manuellen Veröffentlichung live. Vor dieser manuellen Aktion
immer die gesamte Warteschlange prüfen. Die Funktion hält Änderungen an
Testerkonfigurationen und Testerlisten-Mitgliedschaften ausdrücklich nicht
zurück; sie ersetzt daher keine Kontinuitätsprüfung am aktiven Closed-Test.
Aktuell sind 31 maschinell übersetzte bzw. Listing-Änderungen nicht eingereicht
und bleiben bis zur Qualitätsprüfung zurückgestellt. Die Aktivierung hat sie
nicht veröffentlicht.

## Android

Das Projekt erzeugt zwei bewusst getrennte Varianten:

- `directRelease` ist die signierte GitHub-APK für Nutzer außerhalb von Google Play. Sie darf den ausdrücklich bestätigten APK-Update-Dialog verwenden.
- `playRelease` ist das Google-Play-App-Bundle. Es enthält weder `REQUEST_INSTALL_PACKAGES` noch den APK-Update-Receiver; Aktualisierungen kommen ausschließlich über Google Play.

Für einen Upload wird das Artefakt `app-play-release.aab` aus dem Release-Workflow in die Play Console hochgeladen. Vor dem Upload müssen dort App-ID `com.ampere.batterylab`, Play App Signing, Store-Eintrag, Datenschutz-URL, Data-Safety-Angaben und die Erklärung für die beiden `specialUse`-Foreground-Services eingerichtet werden.

Für den ersten Play-Upload:

1. In der Play Console eine App mit Paketname `com.ampere.batterylab` anlegen und Play App Signing aktivieren.
2. Datenschutz-URL auf `https://github.com/Apfelkringel/ampere-battery-lab-updates/blob/main/PRIVACY.md` setzen.
3. Data-Safety-Formular passend zur optionalen, erst nach Zustimmung aktiven Firebase-Analytics ausfüllen. Mindestens App-Interaktionen, Geräte- oder andere IDs (Firebase-App-Instanzkennung) und ungefähre Standortinformationen (aus der IP-Adresse abgeleitet; die IP-Adresse wird danach verworfen) als erhoben und für Analytics verwendet berücksichtigen. Firebase überträgt diese Daten verschlüsselt. Werbe-ID, präziser Standort, Konten und Akku-Messwerte werden von dieser Integration nicht erfasst. Ereignis- und Nutzerdaten sind im Analytics-Projekt jeweils auf zwei Monate begrenzt; die Frist wird bei neuer Nutzeraktivität nicht zurückgesetzt. Für Version 0.372 ist die Analytics-Freigabe „Google-Produkte und -Dienste“ ausgeschaltet; Google Analytics bleibt damit Auftragsverarbeiter. Anonymisierte, aggregierte Beiträge für Benchmarks sind aktiviert. Vor jeder Änderung des Play-Formulars müssen die Konto-Freigaben erneut geprüft werden; Daten an einen Analytics-Auftragsverarbeiter zählen gemäß Play-Definition nicht als „geteilt“.
4. Für beide `specialUse`-Foreground-Services den sichtbaren Anwendungsfall „lokale Akku-Telemetrie und vom Nutzer aktivierte Überwachung“ erklären.
5. Das öffentliche `Ampere-Battery-Lab-play-release.aab` im internen Testtrack hochladen und die Release-Prüfung abwarten.

Die einsetzbaren Texte liegen unter `store/google-play/listing/`.
Alternativ kann der manuelle Workflow `Publish Google Play bundle` nach dem
Hinterlegen des geschützten Repository-Secrets
`AMPERE_PLAY_SERVICE_ACCOUNT_JSON` gestartet werden. Er prüft Version und
SHA-256 aus `latest.json` vor jedem Upload; standardmäßig wird ein Entwurf im
internen Track erstellt. Vor dem Upload liest er die vorhandenen Versionen des
Ziel-Tracks: Bereits enthaltene Zielversionen werden übersprungen. Wenn nur
Entwürfe vorhanden sind und auch der neue Status `draft` ist, übergibt der
Workflow deren Versionscodes als `versionCodesToRetain`, damit die App-Bundles
im neuen Entwurf erhalten bleiben. Ein vorhandener aktiver/nicht-Entwurfs-
Release blockiert standardmäßig den Upload; die manuelle Option zum Ersetzen
darf nur nach Prüfung der Track- und Tester-Kontinuität verwendet werden.
Alpha- und Produktions-Uploads benötigen immer eine bewusste manuelle Auswahl;
`validate_only` prüft Zugang und Artefakt, ohne das Bundle hochzuladen.

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
