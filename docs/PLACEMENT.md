# Gegenstandsauflagen 0.3.1

Winkel beziehen sich auf die Bildmitte der sphärischen Panoramen. Positive Azimute liegen rechts, negative Höhen unter dem Horizont. `Support` bezeichnet die sichtbare Unterkante eines Objekts, nicht seinen Mittelpunkt. Der Renderer misst für jede der 32 Atlaszellen den unteren Alpharand einschließlich UV-Inset; der Blickmittelpunkt erhält dieselbe Anhebung wie das Bild. Die neuen Zellen verwenden keine pauschale Fußhöhe mehr.

| Umgebung | Gegenstände | Gemalte Auflage |
| --- | --- | --- |
| Haus | Lumi / Truhe | Freier Holzboden vor der Einrichtung; Truhe seitlich der Tür, nicht auf der Türschwelle |
| Haus | Schlüssel / Eule / Nachricht | Seitliche Arbeitsflächen und Schrankplatte unter dem Fenster |
| Garten | Topf, Sprössling, Blume, Kannen, Pumpe, Postbeutel, Öl und Zahnrad | Gras bzw. befestigter Weg vor Beeten und Bank, unterhalb der Mauerkante |
| Garten | Drei Mondbriefe | Gemeinsame Gartenbank; getrennte Blickziele |
| Garten / Fähre | Regel-Nachricht | Am Haus angebrachte Nachricht |
| Sternwarte | Lumi | Steinboden vor dem Bücherschrank |
| Sternwarte | Eule / Sternenkarte | Seitliche Ablagen des Bücherschranks |
| Sternwarte | Zeichen, Postauswahl und Uhrhebel | Symbolpult statt frei im Raum verstreut |
| Sternwarte | Sternenmaschine bzw. Sternenuhr | Oberkante des rechten Steinsockels |
| Fähre | Drei Gäste | Linker bzw. rechter steinerner Anleger; identische Höhe auf beiden Seiten |
| Fähre | Lumi / Boot | Zugangsweg am aktuellen Ufer / Wasser neben dem Anleger |

Geschlossene und offene Truhe verwenden dieselbe Skalierung und Auflage. Alle Pflanzenstufen wachsen vom selben Punkt aus nach oben. Schlüssel und Gartenobjekte bleiben außerhalb der anfänglichen Blickrichtung. Der bestehende Schutz vor unbeabsichtigter Blickauswahl bleibt aktiv.

Der Android-Smoke-Test richtet die Kamera auf jedes Blickziel und prüft dessen Treffbarkeit. Er speichert `layout-c…png` für Haus, Pflanzenstufen, Gartenrätsel, Pulte, Sockel und beide Fähranleger. Diese Ansichten werden visuell zusammen mit den Panoramen kontrolliert; ein reiner Geradeaus-Screenshot reicht für Positionsänderungen nicht aus.
