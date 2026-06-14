# Cluedo - Software Engineering II Projekt

## Projektüberblick

Dieses Repository enthält den Android-Client für eine digitale Umsetzung des
Brettspielklassikers **Cluedo**.

Bei Cluedo schlüpfen die Spieler in die Rollen verschiedener Verdächtiger und
bewegen sich über ein Spielbrett mit sechs Räumen. Ziel ist es, durch
Vermutungen und logisches Ausschließen herauszufinden, **wer der Täter ist,
mit welcher Waffe** der Mord begangen wurde und **in welchem Raum** er
stattgefunden hat – und diese Lösung als erster Spieler korrekt anzuklagen.


## Features

- **Multiplayer über WebSockets**: Echtzeit-Kommunikation mit dem Server via
  STOMP-Protokoll (Krossbow).
- **Lobby-System**: Erstellen und Beitreten einer Lobby, Auswahl eines
  Charakters (Mrs. Lavender, Mrs. Pink, Dr. Red, Dr. Blue) und
  Bereit-Status; das Spiel kann startet werden, sobald alle Spieler bereit
  sind.
- **Spielbrett & Bewegung**: Würfeln per Button; Bewegung über das Spielfeld inklusive
  Räume (Küche, Salon, Arbeitszimmer, Ballsaal, Bibliothek, Billardzimmer).
- **Geheimgänge**: Bestimmte Räume sind über Geheimgänge direkt miteinander
  verbunden und können als Abkürzung genutzt werden.
- **Verdachtsäußerungen & Anklagen**: Spieler können in einem Raum eine
  Vermutung (Verdächtiger, Waffe, Raum) äußern oder am Ende eine finale
  Anklage stellen.
- **Schummel-Mechanik**: Beim Widerlegen einer Vermutung gibt es ein
  Zeitfenster, in dem geschummelt werden kann – inklusive
  Aufdeckungs-/Entlarvungsmechanismus für erkannte Schummelversuche.
- **Notiz-Checklisten**: Separate Checklisten für Verdächtige, Waffen und
  Räume, um bereits gesehene/ausgeschlossene Karten zu protokollieren.
- **Aktionsprotokoll**: Live-Verlauf der letzten Spielzüge und Ereignisse.
- **Reconnect-Handling**: Spieler können bei kurzfristigem
  Verbindungsabbruch (z. B. App im Hintergrund) wieder in eine laufende
  Lobby bzw. ein laufendes Spiel zurückkehren.
- **Sound & Musik**: Hintergrundmusik in Lobby und Spiel sowie
  Soundeffekte für Aktionen.
- **Spielanleitung**: Integrierter "Learn"-Bereich mit den Spielregeln.

## Tech-Stack

- **Kotlin** mit Jetpack Compose / Views (Android)
- **Krossbow** (WebSocket/STOMP-Client) für die Echtzeit-Kommunikation mit
  dem Server
- **Material Design 3** Komponenten
- Backend: **Spring Boot** mit WebSocket/STOMP und **MySQL** (siehe
  Server-Repository)

## Spielablauf

### Login & Verbindung

Beim Start der App wird eine eindeutige Spieler-ID erzeugt bzw.
wiederverwendet. Über den Start-Button verbindet sich die App mit dem
Server und gelangt in die Lobby.

### Lobby

In der Lobby wählt jeder Spieler einen der vier verfügbaren Charaktere und
markiert sich als bereit. Sobald alle Spieler bereit sind, kann das
Spiel für alle Teilnehmer gestartet werden.

### Spielzug

Der jeweils aktive Spieler würfelt (per Button) und bewegt seine Spielfigur entsprechend über das Spielbrett.
Betritt ein Spieler einen Raum, kann er:

- über einen **Geheimgang** in einen verbundenen Raum wechseln,
- eine **Vermutung** äußern (Verdächtiger + Waffe + aktueller Raum), woraufhin
  andere Spieler prüfen, ob sie diese widerlegen können,
- eine **Anklage** stellen, um die endgültige Lösung zu erraten.

### Schummeln & Aufdecken

Wenn ein Spieler eine Vermutung widerlegen müsste, aber das nicht möchte, kann er versuchen zu schummeln. Andere Spieler haben ein
Zeitfenster, um einen erkannten Schummelversuch aufzudecken. Man kann nur 1 Mal schummeln.

### Spielende

Das Spiel endet, wenn ein Spieler eine korrekte Anklage stellt (Sieg) oder
eine falsche Anklage stellt (Ausscheiden aus der aktiven Runde). Das
Ergebnis wird allen Spielern angezeigt. Wenn alle Spieler ausgeschieden sind, ist das Spiel **Game Over**.

## Team

- Anna Maxima Robin
- Lusine Babelyan
- Vanesa Markovic
- Baian Alsamman
- Teodora Kocmutt
- Lauro Schöndorfer
