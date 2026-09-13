# Store-Veröffentlichung

## Aktueller Stand (13. September 2026)

Der signierte Android-Release `0.338` ist als APK und Play-App-Bundle gebaut. Das
öffentliche Bundle liegt hier: <https://github.com/Apfelkringel/ampere-battery-lab-updates/raw/refs/heads/main/Ampere-Battery-Lab-play-release.aab>.
Die zugehörige Prüfsumme steht in `latest.json` unter `aabSha256`.

Der Play-Eintrag und der App-Store-Eintrag sind noch nicht angelegt. Die
folgenden Zugangsschritte sind deshalb einmalig erforderlich; sie verändern
keine App-Daten und können nach Prüfung manuell ausgeführt werden.

## Android

Das Projekt erzeugt zwei bewusst getrennte Varianten:

- `directRelease` ist die signierte GitHub-APK für Nutzer außerhalb von Google Play. Sie darf den ausdrücklich bestätigten APK-Update-Dialog verwenden.
- `playRelease` ist das Google-Play-App-Bundle. Es enthält weder `REQUEST_INSTALL_PACKAGES` noch den APK-Update-Receiver; Aktualisierungen kommen ausschließlich über Google Play.

Für einen Upload wird das Artefakt `app-play-release.aab` aus dem Release-Workflow in die Play Console hochgeladen. Vor dem ersten Upload müssen dort App-ID `com.ampere.batterylab`, Play App Signing, Store-Eintrag, Datenschutz-URL, Data-Safety-Angaben und die Erklärung für die beiden `specialUse`-Foreground-Services eingerichtet werden.

Für den ersten Play-Upload:

1. In der Play Console eine App mit Paketname `com.ampere.batterylab` anlegen und Play App Signing aktivieren.
2. Datenschutz-URL auf `https://github.com/Apfelkringel/ampere-battery-lab-updates/blob/main/PRIVACY.md` setzen.
3. Data-Safety-Formular als lokale Verarbeitung ohne Konto, Werbung, Standort oder Weitergabe ausfüllen.
4. Für beide `specialUse`-Foreground-Services den sichtbaren Anwendungsfall „lokale Akku-Telemetrie und vom Nutzer aktivierte Überwachung“ erklären.
5. Das öffentliche `Ampere-Battery-Lab-play-release.aab` im internen Testtrack hochladen und die Release-Prüfung abwarten.

Die einsetzbaren Texte liegen unter `store/google-play/listing/`.
Alternativ kann der manuelle Workflow `Publish Google Play bundle` nach dem
Hinterlegen des geschützten Repository-Secrets
`AMPERE_PLAY_SERVICE_ACCOUNT_JSON` gestartet werden. Er prüft Version und
SHA-256 aus `latest.json` vor jedem Upload; standardmäßig wird ein Entwurf im
internen Track erstellt.

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
