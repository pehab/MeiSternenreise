# MeiSternenreise

Ein kleines Android-Abenteuer für Papp-VR-Brillen und zum Ausprobieren am Handy. Hilf Lumi, das verschwundene Sternenlicht zurückzubringen. Drei Räume, Blicksteuerung und einfache Rätsel ohne Zeitdruck.

## Spielen

- **Am Handy:** Im Querformat mit dem Finger umsehen. Einen Gegenstand mit dem Fadenkreuz etwa 1,8 Sekunden anschauen. Der Kreis zeigt den Fortschritt. Wegschauen und erneut anschauen aktiviert ihn wieder.
- **Mit VR-Brille:** Bewegungssensor erforderlich. Nach dem Start fünf Sekunden Zeit, das Handy in die Brille zu legen. Kopf drehen, Gegenstände anschauen, Türen benutzen. Keine künstliche Fortbewegung oder Kamerafahrten; Raumwechsel werden abgeblendet.
- **Pause:** Hinter dir befindet sich eine mit „Pause“ beschriftete Fläche. Android-Zurück öffnet ebenfalls das Menü. Lautstärketasten richten im VR-Modus den Blick neu aus.
- **Hinweise:** Lumi anschauen. Alle Hinweise erscheinen als Text; eine installierte deutsche Android-TTS-Stimme liest sie auf Wunsch vor. Die App bevorzugt eine verfügbare Offline-Stimme.
- Fortschritt wird nach jeder Aktion lokal gespeichert. „Abenteuer neu starten“ setzt ihn nach Rückfrage zurück.

## Erstes Abenteuer

1. **Sternenwerkstatt:** Schlüssel im Regal entdecken, Truhe öffnen, Mondensamen mitnehmen.
2. **Mondgarten:** Samen einpflanzen und gießen. Die Mondblume schenkt einen Sternenkristall.
3. **Sternwarte:** Symbole in der Reihenfolge Stern → Mond → Sonne anschauen. Linse und Kristall bringen die Maschine zum Leuchten.

## VR-Profil und Grenzen dieser ersten Version

Seit 0.2.0 bestehen die Orte aus gemalten 360°-Panoramen. Freigestellte Gegenstände werden auf räumlich platzierten Flächen pro Auge getrennt gezeichnet. Der Hintergrund selbst ist monoskopisch (ohne Tiefenparallaxe), die Gegenstände und das Fadenkreuz verwenden die Stereo-Kameras. OpenGL ES 2.0 übernimmt Augenabstand und radiale Linsenkorrektur. Linsenkorrektur und Augenabstand lassen sich vor dem Start einstellen. Es gibt noch keinen Cardboard-QR-Profilscanner, keine individuelle asymmetrische Linsenkalibrierung und keine Positionsverfolgung. Die Eignung und Schärfe hängen von Handy und Brille ab. Kopfsteuerung, Bildausrichtung und Komfort müssen an einer echten Brille geprüft werden; Emulatorprüfungen ersetzen diesen Test nicht. Der Handy-Modus funktioniert auch ohne Bewegungssensor.

Kurze Spielrunden im Sitzen, bei Kindern mit Begleitung eines Erwachsenen. Bei Unwohlsein die Brille abnehmen. Hardware-Altersempfehlungen der jeweiligen Brille beachten.

## Entwicklung

JDK 17, Android SDK 35. Android Studio kann das Projekt direkt öffnen.

```sh
./gradlew :core:check
bash scripts/test-update-contract.sh
./gradlew :app:lintGithubDebug :app:lintPlayRelease :app:assembleGithubDebug
```

`core` enthält Story, Spielstandvalidierung und Blickauslösung ohne Android-Abhängigkeiten. JUnit und JaCoCo prüfen mindestens 85 % Zeilenabdeckung. CI prüft beide Vertriebskanäle, erzeugt APKs und startet einen instrumentierten Emulator-Smoke-Test. Berichte und Test-APKs liegen bei GitHub Actions.

## Updates und Signierung

Installationen aus Google Play nutzen Google Play (einschließlich In-App-Update bei manueller Suche). Direkt installierte APKs nutzen das gemeinsame MeiApps-Updateverfahren: tägliche optionale Prüfung, Installationsbestätigung, SHA-256-, Paket-, Versions- und Zertifikatsprüfung. Das Spiel selbst benötigt keine Verbindung, kein Konto und keine Kamera.

Für dauerhaft installierbare GitHub-Releases das Repository-Secret `MEI_SIGNING_JSON` hinterlegen. Inhalt: JSON mit `keystore` (Base64-PKCS12), `password`, `alias` und optional `keyPassword`. Den privaten Schlüssel sichern; nie ins Repository committen. Ohne Secret bleiben CI-APKs reine Testbuilds. Sobald die Prüfungen auf `main` grün sind, veröffentlicht CI mit konfiguriertem Schlüssel Version `0.2.0` und den Updatekanal. Bei Folgeversionen `versionCode` erhöhen; veröffentlichte APKs werden nicht überschrieben.

## Datenschutz

Spielstand und Einstellungen bleiben auf dem Gerät. Keine Werbung, Analyse, Mikrofon- oder Kameranutzung. Updateprüfungen kontaktieren GitHub oder Google Play. Die Sprachausgabe erfolgt über die konfigurierte Android-TTS-Engine; deren Datenschutz und gegebenenfalls Onlineverarbeitung hängen vom installierten Anbieter ab.

## Grafiken

Gemalte Hintergründe und Gegenstände sind vollständig in der APK enthalten. Es werden beim Spielen keine Bilder heruntergeladen. Dateipfade, Atlasbelegung, Bildbeschreibungen und technische Grenzen sind in [docs/ART.md](docs/ART.md) dokumentiert.

## VR-Ausrichtung ab 0.2.2

Bei Doppelbildern zuerst **VR-Bild ausrichten** im Menü wählen und das Handy während des Countdowns in die Brille legen. Mit **Lauter / Leiser** den Abstand der Bildmitten einstellen, bis die beiden grünen Kreuze durch die Brille zusammenpassen. Android-Zurück speichert den Wert und öffnet das Menü. Im eigentlichen Spiel richten die Lautstärketasten weiterhin den Blick neu aus.

Unter **Einstellungen & Ton** lassen sich der Abstand der Bildmitten, die tatsächliche Displaybreite im Querformat (sichtbare Leuchtfläche ohne Gehäuserand), ein vertikaler Versatz und die manuelle Linsenkorrektur einstellen. Die Displaybreite wird zunächst aus den gemeldeten Displaydaten geschätzt; falsche Dichteangaben eines Handys können eine manuelle Korrektur erfordern. Ein vollständiges QR-Brillenprofil wird weiterhin nicht verwendet. Die Einstellungen ersetzen daher keine individuelle Prüfung durch die echte Brille.

Die Sensorverarbeitung behandelt Androids Matrizen ausdrücklich als Zeilenformat und gibt eine spaltenorientierte OpenGL-Ansicht zurück. Bildschirmdrehung und Neuzentrierung werden vor der relativen Kopfbewegung berücksichtigt. Tests prüfen getrennte Gier-, Nick- und Rollbewegungen, alle vier Displaydrehungen und neue Referenzposen. Der Emulator prüft außerdem die Lage beider Ausrichtungskreuze auf dem gerenderten Bildschirm. Ob sich das Bild durch eine konkrete Brille angenehm vereinigen lässt, kann nur am Gerät geprüft werden.
