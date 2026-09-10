# Sicherheit der kostenlosen In-App-Updates

Das Update-Repository ist öffentlich lesbar, aber nicht öffentlich beschreibbar.
Ein beliebiger GitHub-Nutzer kann die Dateien herunterladen, hat dadurch aber
keine Berechtigung, sie zu ändern. Die App behandelt den Kanal trotzdem als
untrusted input und verlässt sich nicht allein auf GitHub oder die JSON-Datei.

## Prüfkette in der App

Vor dem Öffnen des Android-Installers werden alle folgenden Bedingungen geprüft:

1. Manifest und APK kommen ausschließlich per HTTPS von den fest verdrahteten
   `raw.githubusercontent.com`-Pfade dieses Update-Repositories.
2. Die APK-Größe bleibt begrenzt und der SHA-256-Hash muss exakt dem Wert aus
   dem Manifest entsprechen.
3. Paketname und `versionCode` der heruntergeladenen APK müssen exakt zum
   angekündigten Update passen.
4. Das APK-Zertifikat muss dem fest eingebauten Ampere-Release-Zertifikat
   entsprechen: `301bed44b5cc342485b485b24baeea404dbdb216e6e3e134ad2ebd6b28d1dce3`.
5. Erst danach wird Androids eigener Paket-Installer geöffnet. Android prüft
   zusätzlich die Signaturkompatibilität mit der bereits installierten App und
   verlangt die finale Installationsbestätigung.

Damit kann ein Angreifer nicht einfach eine fremde APK in das öffentliche
Repository legen. Selbst wenn jemand Manifest und Hash gemeinsam manipulieren
würde, scheitert die fest eingebaute Release-Signaturprüfung; ohne den privaten
Release-Schlüssel wird die APK nicht zur Installation angeboten.

## Repository-Schutz

- Der Quellcode bleibt im privaten Repository; das öffentliche Repository
  enthält nur APK, Manifest und Release-Dokumentation.
- Der öffentliche `main`-Branch ist gegen Force-Pushes und Löschung geschützt;
  Secret-Scanning und Push-Protection sind aktiviert.
- Private Release-Schlüssel liegen nur in GitHub Actions Secrets und niemals in
  einem Repository oder in der App.

## Verbleibendes Risiko

Die stärkste verbleibende Annahme ist der Schutz des GitHub-Kontos und des
privaten Release-Schlüssels. Bei einem kompromittierten Release-Schlüssel wäre
eine fremd signierte APK technisch nicht mehr von einer echten zu unterscheiden;
deshalb muss der Schlüssel außerhalb von Git gespeichert, mit MFA geschützt und
bei Verdacht sofort rotiert werden. Die Signatur-Lineage der App unterstützt
danach den kontrollierten Schlüsselwechsel, ohne lokale Nutzerdaten zu löschen.
