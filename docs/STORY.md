# Kampagne 0.3.0

Fünf Kapitel mit wachsendem Denkanteil. Die Kapitel verwenden das zusammenhängende Haus–Garten–Sternwarte-Ensemble; das Finale ersetzt den Gartenblick durch den Bach mit zwei Anlegern. Gegenstände und aktive Bedienflächen stehen abseits der ersten Blickrichtung. Die Blickauswahl ist nach Szenenwechseln und Aktionen erst nach einem Blick ohne Ziel wieder scharf.

## Regeln und Lösungen (Entwicklungsdokument, enthält Spoiler)

| Kapitel | Zentrale Regel | Referenzlösung |
| --- | --- | --- |
| Sternenlicht | Schlüssel öffnet Truhe; Pflanze braucht Wasser; drei Symbole | Schlüssel, Truhe, Garten, Topf, Wasser, Blume, Sternwarte, Stern–Mond–Sonne, Maschine |
| Mondpost | Sonne vor Mond, Stern nach beiden; Empfänger weder rund noch Sichel | Hausnachricht + Schlüssel + Truhe; Gartenregel; Sonne–Mond–Stern sortieren; Sternwarten-Nachricht; Sternenbriefkasten; Bestätigung an Maschine |
| Tauquelle | Kleine Kanne 3 L, große 5 L; Umgießen endet bei leerer Quelle/voller Zielkanne | Große füllen, in kleine gießen (2/3), kleine leeren, große in kleine (0/2), große füllen (5/2), in kleine gießen (4/3); Blume nimmt vier Liter; Tauperle zur Maschine |
| Sternenuhr | Zeigerzyklus Stern–Mond–Sonne; A bewegt 1+2, B bewegt 2+3, C bewegt 1+3; Ziel Stern–Stern–Mond | Zahnrad im Haus holen; Gartenanleitung + Öl + Achse pflegen; Zielkarte lesen; A einmal, B zweimal, C zweimal; Maschine prüfen |
| Fünf Sterne | Fähre nimmt höchstens einen Gast; ohne Lumi sind Fuchs+Käfer und Käfer+Samen unsicher | Käfer rüber, leer zurück, Fuchs rüber, Käfer zurück, Samen rüber, leer zurück, Käfer rüber. Zeichenhinweise: Mond am Anfang/Ende, Stern direkt vor Sonne → Mond–Stern–Sonne–Mond. |

Falsche Eingaben sind rückgängig bzw. erneut versuchbar. Eine unsichere Fährfahrt wird mit Erklärung abgelehnt, ohne einen Gast zu verlieren. Kannen und Uhr haben einen Rücksetzknopf. Tipps bei Lumi werden stufenweise konkreter; die automatische Zielansage verrät nicht die Lösung. Die Eule wiederholt den Kapiteltext.

## Speicherung

`Adventure` speichert `SR3|aktivesKapitel|Freischaltungsgrenze|Kapitelstand0|…|Kapitelstand4`. Jeder Kapitelstand enthält Ort, Inventar, Fortschritt, gelesene Hinweise, Tipps, Füllstände, Zeiger, Uferbelegung und gewählten Fahrgast. Das alte neunteilige CSV-Format aus 0.2.x wird als erstes Kapitel übernommen. Ein abgeschlossener alter Spielstand schaltet Kapitel 2 frei. Kapitelwechsel erhalten alle Zwischenstände. Ein Neustart betrifft nur das aktive Kapitel und senkt die bisherige Freischaltungsgrenze nicht ab.

`PlayerProfiles` speichert bis zu acht stabile Profil-IDs mit Namen und vollständigem Kampagnenstand in einem gemeinsamen `PROFILES1`-Datensatz. Namen und Kampagnentexte sind UTF-8/Base64URL-kodiert; Trennzeichen im Namen sind erlaubt. Der frühere Preference-Schlüssel `state` dient nur noch der einmaligen Übernahme. Laufende Speicher-Callbacks halten die Profil-ID und werden über einen Kontextzähler beim Stoppen/Wechseln verworfen. Vor einem Profilwechsel wird der fertig abgearbeitete GL-Spielstand synchron ins richtige Profil übernommen. Gelöschte Profile werden durch verspätete Speichervorgänge nicht neu angelegt.

## Prüfung

Core-Tests spielen alle fünf Kapitel mit Speichern/Laden nach jeder Aktion durch, prüfen Sperren, Fehlversuche, Kannenbilanz, gekoppelte Zeiger, sichere Ufer, Kapitelwechsel und Profilisolation. Gaze-Tests halten einen bereits fokussierten Gegenstand lange im Blick und prüfen, dass er erst nach Weg- und erneutem Hinschauen auslöst. Der Emulator spielt dieselbe vollständige Kampagne einschließlich öffentlicher UI-/GL-Speicherstrecke und Ausrichtungsbild durch und erzeugt Kapitel-Screenshots. Die VR-Matrix- und Bildmittenprüfungen aus 0.2.2 bleiben bestehen.
