# Grafikpaket 0.2.0

Vier eigens für MeiSternenreise generierte Bilddateien. Erstellung mit der eingebauten OpenAI-Bildgenerierung; anschließend ausschließlich technische WebP-Konvertierung für die Android-Assets. Keine Stockbilder oder extern nachgeladenen Spielgrafiken.

## Dateien und Bildbeschreibungen

| Datei im Repository | Bildauftrag |
| --- | --- |
| `app/src/main/assets/art/workshop.webp` | Eine nahtlos gedachte 360°-Rundumansicht einer gemütlichen märchenhaften Werkstatt in einem Wohnhaus, 2:1 equirektangular, Sitzhöhe, warme Holzoberflächen und Laternen, Bücherregale und Werkbank, runde Fenster und ein offener Ausgang rechts in den blauen Mondgarten. Weiche, detaillierte Bilderbuchmalerei, ohne Figuren, Rätselgegenstände, Text oder Benutzeroberfläche. |
| `app/src/main/assets/art/garden.webp` | Eine 360°-Rundumansicht eines echten offenen Gartens zur blauen Stunde: freier Sternenhimmel, Gras und Blumenbeete, Obstbäume, Wege und warmes Laternenlicht; links das Wohnhaus mit Gartentür, rechts eine separate runde Sternwarte mit Kupferkuppel. Freier Vordergrund für Pflanze und Gießkanne. Gleicher gemalter Bilderbuchstil, ohne Figuren oder Text. |
| `app/src/main/assets/art/observatory.webp` | Innenansicht einer eigenständigen runden Sternwarte aus Naturstein mit Kupferkuppel und geöffnetem Dach zum Sternenhimmel. Großes Messingteleskop links, Symbolpult in der Mitte, Steinsockel rechts und ein offener Ausgang zum Garten im hinteren linken Bereich. Warme Laternen und kühles Sternenlicht, derselbe gemalte Stil. Keine Hauptmaschine, Figuren, Rätselsymbole oder Schrift im Hintergrund. |
| `app/src/main/assets/art/sprites.webp` | Ein quadratischer 4×4-Atlas auf echtem transparentem Hintergrund, alle Objekte voneinander getrennt und vollständig in ihrer gleich großen Zelle: freundlicher mintfarbener Messingroboter, Sternenschlüssel, geschlossene und offene Holztruhe; leerer Blumentopf, Sprössling, blaue Mondblume und Gießkanne; ruhende und leuchtende Messing-Sternenmaschine mit runder Glaskugel, goldener Stern und violette Mondsichel; lächelnde Sonne, Gartentor, Sternwartentür und runder Hausknopf. Detaillierte, runde Bilderbuchformen, keine Polyeder oder Texte. |

## Atlas

Zellen sind zeilenweise von links nach rechts nummeriert, beginnend mit 0.

| Zeile | Spalte 1 | Spalte 2 | Spalte 3 | Spalte 4 |
| --- | --- | --- | --- | --- |
| 1 | 0: Lumi | 1: Schlüssel | 2: Truhe | 3: offene Truhe |
| 2 | 4: Topf | 5: Sprössling | 6: Mondblume | 7: Gießkanne |
| 3 | 8: Maschine | 9: leuchtende Maschine | 10: Stern | 11: Mond |
| 4 | 12: Sonne | 13: Gartentor (Reserve) | 14: Sternwartentür (Reserve) | 15: Haus / Menü |

## Darstellung und Grenzen

Die Panoramen werden anhand der Blickrichtung sphärisch abgetastet, nicht als flache Wand vor die Kamera gestellt. Der Horizont liegt auf halber Bildhöhe; Bildmitte ist die Blickrichtung beim Betreten. Die Texturen werden nur beim Raumwechsel geladen und an die maximal unterstützte Texturgröße angepasst. Die Sprites liegen in virtuellen Entfernungen vor dem Panorama und werden pro Auge mit Augenabstand gezeichnet. Transparente PNG-Ausgangsbilder wurden als WebP mit Alpha erhalten; Androids vormultipliziertes Alpha verwendet das passende OpenGL-Blending.

Die Umgebung ist gemalt und daher selbst ohne stereoskopische Tiefenparallaxe. Ein Panorama enthält keine frei begehbare Geometrie. Bei starker Vergrößerung kann die begrenzte Bildauflösung sichtbar werden; die generierten Rundumansichten können insbesondere hinter dem Spieler eine sichtbare Naht enthalten. Dieses Verfahren ist für die festen Aussichtspunkte vorgesehen. Die Bilddateien bilden kein vollständiges fotogrammetrisches oder individuell kalibriertes Cardboard-Szenenmodell.

Blickziele für die Übergänge sind auf die sichtbaren Türen und Gebäude abgestimmt. Gegenstandsbeschriftungen erscheinen beim Untersuchen; Ziele für Raumwechsel und Menü bleiben sichtbar. Alle Hinweise und der Spielstand stammen aus derselben unveränderten Story-Logik wie in 0.1.0.

## Auflagepunkte ab 0.2.1

Gegenstände auf gemalten Flächen werden an der sichtbaren Unterkante ihrer Atlaszelle verankert. `FOOT` im Renderer berücksichtigt den transparenten Rand pro Zelle. Der angegebene Winkel beschreibt die Auflagefläche im Panorama; die Bildmitte und das Blickziel werden um den sichtbaren Fußabstand angehoben. Dadurch bleiben Truhenboden und Topfboden beim Wechsel des Bildes fest. Topf und Sprössling verwenden dieselbe Größe; die Blume wächst mit größerer quadratischer Darstellung nach oben. Werkstatt und Sternwarte haben eigene Ablagehöhen für Lumi. Der freie Bildschirmbereich unterhalb der Bildmitte bleibt für die Gegenstände reserviert, Hinweise sitzen im oberen Bereich unter dem kompakten Titel.

## Ergänzungen 0.3.0

Erneut mit der eingebauten Bildgenerierung erstellt und technisch als WebP für die Offline-APK konvertiert:

- `app/src/main/assets/art/chapter-sprites.webp`: transparenter 4×4-Bilderbuchatlas mit Federschlüssel, Postbeutel, Sonnen-/Mond-/Sternenbrief, kleiner und großer Keramikkanne, Handpumpe, Zahnrad, Ölflasche, Sternenuhr, mechanischer Eule, freundlichem Lichtfuchs, Glühkäfer, Samenbeutel und Laternenfähre. Bildauftrag: detaillierte runde Formen in Messing, Türkis und warmem Gold, jedes Motiv vollständig in einer gleich großen Zelle, keine Schrift oder eingebrannten Zahlen.
- `app/src/main/assets/art/ferry.webp`: gemalte 2:1-Rundumansicht eines ruhigen Bachs im Mondgarten mit gegenüberliegenden steinernen Anlegern, Sternreflexen, Laternen, Haus links und separater Sternwarte rechts. Bildauftrag: derselbe weiche Bilderbuchstil, offener Himmel, freie Vordergrundflächen für die separaten Figuren und das Boot, keine Figuren oder Schrift im Hintergrund.

Die zusätzlichen Atlaszellen verwenden in `WorldRenderer` die Nummern 16–31 in derselben Lesereihenfolge. Der Fährhintergrund wird im Garten des fünften Kapitels geladen.
