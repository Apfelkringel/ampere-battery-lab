# Store-Veröffentlichung

## Android

Das Projekt erzeugt zwei bewusst getrennte Varianten:

- `directRelease` ist die signierte GitHub-APK für Nutzer außerhalb von Google Play. Sie darf den ausdrücklich bestätigten APK-Update-Dialog verwenden.
- `playRelease` ist das Google-Play-App-Bundle. Es enthält weder `REQUEST_INSTALL_PACKAGES` noch den APK-Update-Receiver; Aktualisierungen kommen ausschließlich über Google Play.

Für einen Upload wird das Artefakt `app-play-release.aab` aus dem Release-Workflow in die Play Console hochgeladen. Vor dem ersten Upload müssen dort App-ID `com.ampere.batterylab`, Play App Signing, Store-Eintrag, Datenschutz-URL, Data-Safety-Angaben und die Erklärung für die beiden `specialUse`-Foreground-Services eingerichtet werden.

## iOS

Das Repository enthält jetzt einen nativen SwiftUI-Port unter `ios/` mit eigenem Xcode-Projekt. Er zeigt die von `UIDevice` tatsächlich verfügbaren Werte (Akkustand und Ladezustand), speichert lokale Messpunkte und kennzeichnet Android-exklusive Strom-/Kapazitätsdaten als nicht verfügbar. Der Android-Batterie- und Hintergrunddienst wird nicht vorgetäuscht.

Der iOS-Workflow erstellt auf einem macOS-Runner ein unsigniertes Release-Build zur Prüfung. Für die App-Store-Veröffentlichung müssen anschließend ein Apple-Developer-Team, eine Bundle-ID, Zertifikate/Provisioning, App-Store-Connect-Metadaten und ein signierter Archive-Upload hinterlegt werden.
