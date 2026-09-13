# Store-Veröffentlichung

## Android

Das Projekt erzeugt zwei bewusst getrennte Varianten:

- `directRelease` ist die signierte GitHub-APK für Nutzer außerhalb von Google Play. Sie darf den ausdrücklich bestätigten APK-Update-Dialog verwenden.
- `playRelease` ist das Google-Play-App-Bundle. Es enthält weder `REQUEST_INSTALL_PACKAGES` noch den APK-Update-Receiver; Aktualisierungen kommen ausschließlich über Google Play.

Für einen Upload wird das Artefakt `app-play-release.aab` aus dem Release-Workflow in die Play Console hochgeladen. Vor dem ersten Upload müssen dort App-ID `com.ampere.batterylab`, Play App Signing, Store-Eintrag, Datenschutz-URL, Data-Safety-Angaben und die Erklärung für die beiden `specialUse`-Foreground-Services eingerichtet werden.

## iOS

Dieses Repository enthält aktuell keine iOS-App, kein Xcode-Projekt und keine Swift-Implementierung. Eine Veröffentlichung im Apple App Store kann daher erst nach einem echten iOS-Port erfolgen. Dafür werden ein Apple-Developer-Team, eine Bundle-ID, Zertifikate/Provisioning, App-Store-Connect-Metadaten und ein vollständiger Xcode-Build benötigt. Der Android-Batterie- und Hintergrunddienst kann nicht unverändert auf iOS übernommen werden.
