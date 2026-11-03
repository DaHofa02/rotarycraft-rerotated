# Auftrag: Vier Minecraft-Mods auf Minecraft 1.21.1 und NeoForge portieren

## 1. Ziel

Analysiere vier vorhandene Minecraft-Mods vollständig und portiere ihre bestehenden Funktionen auf:

- Minecraft Java Edition 1.21.1
- NeoForge für Minecraft 1.21.1
- Java 21
- Eine passende, offiziell verfügbare NeoForge-Version für Minecraft 1.21.1
- Gradle und eine zu dieser Zielversion passende NeoForge-Entwicklungsumgebung

Die fertige Lösung muss aus **mindestens drei eigenständig ladbaren NeoForge-Mod-JARs** bestehen.

Die vier ursprünglichen Mods sollen inhaltlich möglichst vollständig erhalten bleiben. Fasse verwandte Funktionen sinnvoll zusammen, ohne Features grundlos zu entfernen, zu vereinfachen oder neu zu erfinden.

## 2. Verbindliche Grundregeln

1. Analysiere alle vier Mods, bevor du die Architektur festlegst.
2. Erfinde keine zusätzlichen Gameplay-Features, Gegenstände, Blöcke, Kreaturen, Systeme oder Benutzeroberflächen, die in den Originalen nicht vorhanden oder für die technische Migration zwingend notwendig sind.
3. Implementiere keine Funktionen erneut, die Minecraft Vanilla oder NeoForge bereits bereitstellt.
4. Verwende die APIs und Systeme der tatsächlich gewählten NeoForge-Version. Überprüfe Signaturen und Implementierungsdetails anhand der offiziellen Dokumentation und der verfügbaren Quelltexte.
5. Verwende keine Forge-, Fabric- oder veralteten APIs, sofern sie nicht ausdrücklich durch eine nachweislich kompatible Abhängigkeit bereitgestellt werden.
6. Bewahre vorhandene Modelle, Texturen, Animationen, Sounds, Übersetzungen, Rezepte, Loot Tables, Tags, Weltgenerierung und sonstige Assets.
7. Vermeide unnötige Bibliotheken, Abhängigkeiten, Abstraktionsschichten und Frameworks.
8. Erhalte das bestehende Verhalten so weit wie technisch möglich. Dokumentiere notwendige Änderungen und nicht reproduzierbare Funktionen.
9. Behaupte niemals, dass etwas funktioniert, wenn es nicht gebaut oder angemessen getestet wurde.
10. Arbeite nachvollziehbar, in kleinen Schritten und mit überprüfbaren Ergebnissen.

## 3. Phase A: Bestandsaufnahme aller vier Mods

Untersuche die vier Projekte getrennt und vollständig.

Prüfe unter anderem:

- Quellcode, Paketstruktur und Build-System
- Ursprüngliche Minecraft-Version und Modloader
- Mod-ID, Metadaten, Lizenz und Abhängigkeiten
- Registrierte Blöcke, Items, Kreaturen, Block Entities und Menüs
- Rezepte, Tags, Loot Tables und Fortschritte
- Weltgenerierung, Biome, Strukturen und Dimensionen
- Entitäten, KI, Attribute, Schadenslogik und Interaktionen
- Inventare, Container, Netzwerkkommunikation und persistente Daten
- Client-Rendering, Renderer, Modelle, Animationen, Partikel und Sounds
- Konfigurationen und Serververhalten
- Mixins, Access Transformers, Reflection und direkte Zugriffe auf interne Minecraft-Klassen
- Datengenerierung und vorhandene Tests
- Lizenzen und Wiederverwendungsbedingungen

Wenn nur JAR-Dateien vorhanden sind, untersuche sie statisch. Extrahiere Ressourcen, Metadaten und Klasseninformationen, soweit möglich. Stelle fest, ob dekompilierter Code verfügbar ist und wo die ursprünglichen Quelltexte fehlen.

Erstelle folgende Dokumente:

- `docs/01-original-mods.md`: Eigenschaften und Funktionen aller vier Mods
- `docs/02-feature-matrix.md`: vollständige Feature-Matrix mit Ursprung, Status und Zielmod
- `docs/03-dependencies.md`: interne und externe Abhängigkeiten
- `docs/04-assets.md`: Modelle, Texturen, Sounds, Animationen und weitere Ressourcen
- `docs/05-migration-risks.md`: technische Risiken, fehlende Informationen und inkompatible APIs

Erfinde keine unbekannten Eigenschaften. Kennzeichne unklare Punkte ausdrücklich.

## 4. Phase B: Architektur und Aufteilung

Entwirf eine Aufteilung in mindestens drei eigenständige NeoForge-Mods.

Entscheide anhand der tatsächlichen Funktionen der vier Originale, welche Inhalte zusammengehören. Berücksichtige dabei:

- Funktionale Zusammengehörigkeit
- Laufzeitabhängigkeiten
- gemeinsame Assets und Codebestandteile
- optionale und erforderliche Abhängigkeiten
- unabhängige Installation und Aktualisierung
- Wartbarkeit und möglichst geringe Kopplung

Verwende keine willkürliche Aufteilung allein, um drei JAR-Dateien zu erzeugen.

Erstelle `docs/06-target-architecture.md` mit:

- Namen und Mod-IDs der Zielmods
- Zuordnung jeder ursprünglichen Funktion
- Begründung der Aufteilung
- Abhängigkeitsdiagramm
- gemeinsam genutzten Komponenten
- optionalen und zwingenden Abhängigkeiten
- Migrationsreihenfolge
- bekannten Einschränkungen

Erstelle eine Tabelle, die jedes Feature einem Zielmod und einem Test zuordnet.

Falls eine kleine gemeinsame Bibliothek tatsächlich erforderlich ist, begründe sie. Erzeuge keine zusätzliche Bibliothek, wenn reguläre Abhängigkeiten oder eine einfache, saubere Implementierung ausreichen.

## 5. Phase C: NeoForge richtig verwenden

Verwende die in der ausgewählten NeoForge-Version vorhandenen APIs und Vanilla-Mechanismen, soweit sie den Anforderungen entsprechen.

Prüfe insbesondere:

- Registrierungen mit den passenden Registry- und `DeferredRegister`-APIs
- Event-System und mod-spezifischen Event Bus
- Netzwerk- und Payload-Systeme
- Data Attachments, SavedData und Data Components entsprechend ihrem jeweiligen Anwendungsfall
- Datapack- und dynamische Registries
- Data Generation
- Tags, Rezepte, Loot Tables und JSON-Ressourcen
- Konfigurationssystem
- Capability- und Handler-APIs, soweit erforderlich
- Client-/Server-Trennung
- Rendering- und Model-APIs
- Lifecycle- und Ladeereignisse

Dies ist eine Prüfliste, keine Aufforderung, jedes System einzubauen.

Verwende jedes System nur, wenn es für ein tatsächlich vorhandenes Feature erforderlich ist. Prüfe vor jeder Eigenimplementierung, ob Vanilla oder NeoForge die benötigte Funktion bereits bereitstellt.

Vermeide insbesondere:

- selbst gebaute Registrierungsframeworks
- unnötige Event-Busse
- eigene Netzwerkprotokolle, wenn die vorhandenen APIs ausreichen
- selbst entwickelte Persistenz, wenn Vanilla oder NeoForge sie abdeckt
- redundante Inventar- und Capability-Systeme
- unnötige Reflection, Mixins oder Access Transformers
- doppelte Konfigurations- und Datengenerierungssysteme
- unnötige Coremods oder zusätzliche Modloader-Abstraktionen

Nutze die offizielle Dokumentation für Minecraft 1.21.1 und die exakt gewählte NeoForge-Version. Verlasse dich nicht auf Codebeispiele für andere Minecraft-Versionen.

## 6. Phase D: Umsetzung

Implementiere die Migration in kontrollierten Schritten:

1. Entwicklungsumgebung und Gradle-Build einrichten.
2. Zielmods und Metadaten anlegen.
3. Registrierungen und grundlegende Ressourcen migrieren.
4. Items, Blöcke, Rezepte, Tags und Loot Tables portieren.
5. Entitäten, Block Entities, Interaktionen und Gameplay-Logik portieren.
6. Persistenz und Netzwerkkommunikation migrieren.
7. Rendering, Modelle, Animationen, Partikel und Sounds integrieren.
8. Weltgenerierung und sonstige Datenressourcen migrieren.
9. Konfigurationen und optionale Abhängigkeiten einrichten.
10. Übersetzungen, Rezepte und Ressourcen auf Vollständigkeit prüfen.
11. Tests ausführen, Fehler beheben und alle Zielmods bauen.

Passe die Reihenfolge an die tatsächlichen Abhängigkeiten an.

Verwende bestehende Modelle und Texturen, sofern sie mit dem Zielsystem kompatibel sind. Ändere Geometrie, Animationen oder Gestaltung nicht ohne technischen Grund.

Erstelle neue Assets nur dann, wenn ein vorhandenes Asset nicht weiterverwendet werden kann und eine funktional gleichwertige Migration erforderlich ist. Dokumentiere diesen Fall.

## 7. Phase E: Qualitätssicherung

Führe mindestens folgende Prüfungen durch:

- Gradle-Build für alle Zielmods
- Prüfung der Mod-Metadaten und Abhängigkeiten
- Prüfung auf doppelte Registrierungen und Mod-IDs
- Prüfung auf fehlende Modelle, Texturen, Sounds und Übersetzungen
- Prüfung auf fehlende Rezepte, Tags, Loot Tables und Datenressourcen
- Prüfung von Netzwerkkommunikation und synchronisierten Daten
- Prüfung der Client-/Server-Trennung
- Start des Minecraft-Clients
- Start eines Dedicated Servers, sofern die Umgebung dies ermöglicht
- Prüfung, ob alle Mods zusammen funktionieren
- Prüfung, ob Mods ohne optionale Abhängigkeiten korrekt laden
- Regressionstests für die ursprünglichen Kernfunktionen

Erstelle automatisierte Tests, wo sinnvoll. Simuliere keine erfolgreichen Spieltests, wenn Minecraft nicht tatsächlich gestartet wurde.

Lege die Ergebnisse in `docs/07-test-report.md` ab. Dokumentiere ausgeführte Befehle, Ergebnisse, bekannte Fehler und nicht durchgeführte Tests.

## 8. Arbeitsmodus und Sicherheitsregeln

Beginne mit einer vollständigen Analyse und einem konkreten Migrationsplan. Ändere die Originalprojekte während der Analyse nicht.

Danach arbeite schrittweise. Erstelle bei Bedarf einen neuen Zielordner für die portierten Projekte. Bewahre die Originaldateien auf.

Vor destruktiven Änderungen, dem Löschen von Assets oder einer nicht rückgängig zu machenden Umstrukturierung musst du den Umfang prüfen und eine sichere Alternative bevorzugen.

Erfinde keine Versionsnummern, API-Methoden oder Abhängigkeiten. Verifiziere diese Informationen.

Wenn ein kritisches Detail fehlt, untersuche zuerst die vorhandenen Dateien und Dokumentationen. Stelle gezielte Rückfragen nur dann, wenn die Entscheidung andernfalls nicht zuverlässig getroffen werden kann.

## 9. Abschluss und Lieferumfang

Liefere:

- Mindestens drei eigenständig ladbare NeoForge-Mod-JARs
- Den vollständigen Quellcode aller Zielmods
- Gradle-Konfiguration und Build-Anleitung
- Alle benötigten Assets und Datenressourcen
- Eine vollständige Feature-Zuordnung von den vier Originalen zu den Zielmods
- Dokumentierte Abhängigkeiten und Lizenzinformationen
- Testberichte und bekannte Einschränkungen
- Eine Anleitung zur Installation und gemeinsamen Verwendung

Am Ende musst du konkret berichten:

1. Welche ursprünglichen Features übernommen wurden
2. Welche Features geändert werden mussten und warum
3. Welche Features noch fehlen
4. Welche Tests tatsächlich erfolgreich waren
5. Welche JAR-Dateien erstellt wurden und wo sie liegen
6. Welche externen Abhängigkeiten erforderlich sind
7. Welche Entscheidungen oder offenen Fragen noch meine Freigabe benötigen

**Starte jetzt ausschließlich mit Phase A und dem Architekturentwurf. Implementiere noch nichts, bevor du alle vier Mods analysiert und die vorgeschlagene Aufteilung dokumentiert hast.**