# 06 – Zielarchitektur (Entwurf zur Freigabe)

Status: **Vorschlag**. Noch nichts implementiert. Grundlage sind die Befunde in 01–05.

## 1. Entscheidung: drei Zielmods, keine eigene Bibliothek

| Zielmod | Mod-ID | Paket (Vorschlag) | Inhalt | JAR |
|---|---|---|---|---|
| **RotaryCraft** | `rotarycraft` | `reika.rotarycraft` | gesamter RotaryCraft-Inhalt + **die tatsächlich benötigten Teile von DragonAPI** als internes Paket `reika.rotarycraft.lib` + öffentliche Wellenleistungs-API `reika.rotarycraft.api` | `rotarycraft-<ver>.jar` |
| **ReactorCraft** | `reactorcraft` | `reika.reactorcraft` | gesamter ReactorCraft-Inhalt | `reactorcraft-<ver>.jar` |
| **ElectriCraft** | `electricraft` | `reika.electricraft` | gesamter ElectriCraft-Inhalt | `electricraft-<ver>.jar` |

Die Mod-IDs müssen in 1.21 klein geschrieben werden (`[a-z][a-z0-9_]{1,63}`). Die Original-IDs `RotaryCraft` usw. sind daher nicht zulässig. Das Paket wird klein geschrieben, aber nach dem Original benannt.

### Begründung

1. **Die Aufteilung folgt der Originalstruktur.** Die drei Inhalts-Mods waren schon im Original eigenständige Mods mit eigener Mod-ID, eigener Konfiguration, eigenem Handbuch und eigenen Assets. ReactorCraft und ElectriCraft hängen nur von RotaryCraft (und DragonAPI) ab, nicht voneinander. Die einzige Querverbindung ReC → EC ist optional. Damit ist die Aufteilung nicht willkürlich (start.md §4), sondern übernimmt die vorhandenen Modulgrenzen.
2. **DragonAPI wird keine vierte Mod.** DragonAPI ist eine 1.7.10-Infrastrukturbibliothek. Der Großteil ist auf 1.21.1 überflüssig, weil NeoForge oder Vanilla es abdecken (03 §4): Registrierung, Pakete, Konfiguration, Sync, Ore-Dict, Retrogen, Chunkloading, Keybinds, Tick-Scheduling, Coremod-Hooks, Stripper. Es bleibt ein überschaubarer Rest:
   - Mathe- und Physik-Konstanten (`ReikaEngLibrary`, `ReikaMathLibrary` …)
   - Block-Struktur-Werkzeuge (BlockArray, StructuredBlockArray, FilledBlockArray), Multiblock-Prüfung
   - Datenstrukturen (StepTimer, ChancedOutputList, WeightedRandom, Collections)
   - Handbuch-GUI-Framework, GUI-Bausteine, PianoKeyboard, Tank-Anzeige
   - Strukturvorschau-Renderer, XML- und MIDI-Leser
   
   Alle drei Mods brauchen diesen Rest. Da RotaryCraft aber ohnehin eine **Pflichtabhängigkeit** von ReactorCraft und ElectriCraft ist, kann er dort liegen. Eine zusätzliche Bibliotheks-JAR hätte keinen Vorteil für unabhängige Installation, würde aber eine weitere Abhängigkeit und eine weitere Versionierung einführen (Regel 7, start.md §4 letzter Absatz).
3. **Kopplung bleibt gering.** ReC und EC greifen nur über eine **klar abgegrenzte, öffentliche API** auf RotaryCraft zu (`reika.rotarycraft.api`: Wellenleistung, Werkzeug-Interfaces, gemeinsame Materialien und Tags). Interne Klassen (`…lib`, `…content`) sind nicht für Fremdzugriff vorgesehen. Das ersetzt die bisherigen 338 Importe aus `Auxiliary`, `Registry`, `Base` und `TileEntities`.

**Alternative B (nicht empfohlen):** vier JARs mit einer schlanken `dragonapi`-Bibliothek. Das wäre näher an der Originalstruktur, schafft aber eine zusätzliche Abhängigkeit ohne fachlichen Mehrwert. Ich setze sie um, falls du die Originalstruktur ausdrücklich beibehalten möchtest.

## 2. Abhängigkeitsdiagramm

```
                 ┌──────────────────────────┐
                 │  NeoForge 21.1.x / MC 1.21.1 │
                 └─────────────▲────────────┘
                               │ required
                 ┌─────────────┴────────────┐
                 │        rotarycraft        │
                 │  api/  (öffentlich)       │
                 │  lib/  (intern, aus       │
                 │         DragonAPI)        │
                 │  content/ (Maschinen …)   │
                 └───▲──────────────────▲───┘
            required │                  │ required
       ┌─────────────┴───┐        ┌─────┴────────────┐
       │  reactorcraft   │·······►│   electricraft   │
       └─────────────────┘optional└──────────────────┘
        (Reactor Generator speist ElectriCraft-Netz, nur wenn installiert)

 rotarycraft ·····► reactorcraft   optional (Legierung als Rezeptzutat, Sprinkler vs. Strahlung,
                                             Advancements mit ReC-Icons): umgesetzt über Tags,
                                             bedingte Rezepte (neoforge:conditions mod_loaded)
                                             und ModList-Prüfung, ohne Klassenimport

 alle ·····► JEI, Jade, CC: Tweaked   optional (compileOnly; nur bei Freigabe)
```

`neoforge.mods.toml`:
- `rotarycraft`: `dependencies` → `neoforge` (required), `minecraft` `[1.21.1]` (required)
- `reactorcraft`: + `rotarycraft` (required, `ordering="AFTER"`), `electricraft` (optional)
- `electricraft`: + `rotarycraft` (required, `ordering="AFTER"`)
- JEI/Jade/CC: Tweaked jeweils `type="optional"`

## 3. Gemeinsam genutzte Komponenten

| Komponente | Ort | Nutzer |
|---|---|---|
| Wellenleistungs-API (Drehmoment, ω, Leistung, Ein- und Ausgänge, Interfaces `ShaftPowerEmitter`/`ShaftPowerReceiver` …) | `rotarycraft/api/power` | RC, ReC (Turbinen, Pumpen), EC (Generator, Motor) |
| Materialkonstanten, Physikformeln | `rotarycraft/lib/math` | alle |
| Multiblock-Strukturprüfung + Vorschau-Renderer | `rotarycraft/lib/structure` | RC (Worktable, Solar-Turm …), ReC (6 Strukturen) |
| Handbuch-Framework (XML → GUI-Seiten) | `rotarycraft/lib/handbook` | alle drei Handbücher |
| Basis-BlockEntity (Owner, Redstone-Modi, Sync-Hilfen) | `rotarycraft/lib/block` | alle; nur dünne Hilfen über `BlockEntity`, kein eigenes Framework |
| Gemeinsame Tags | Datapack `c:` + `rotarycraft:` | Stahl (HSLA) `c:ingots/steel`, Silber `c:ingots/silver`, Blei `c:storage_blocks/lead` … |
| Werkzeug-Interfaces (Schraubendreher, Meter) | `rotarycraft/api` | ReC/EC-Maschinen drehen und messen |

Nicht gemeinsam: Konfigurationen (je Mod eine `ModConfigSpec`), Pakete (je Mod eigene Payloads mit eigenem Namespace), Creative Tabs, Advancements.

## 4. NeoForge-Systeme je Feature (Phase C, Kurzfassung)

| Original-Feature | NeoForge/Vanilla-System |
|---|---|
| Maschinen | `Block` + `BlockEntity` + `BlockEntityTicker`; ein Block je Maschine; Ausrichtung als `DirectionProperty` |
| Inventare | `ItemStackHandler`, `Capabilities.ItemHandler.BLOCK` (seitenabhängig) |
| Tanks | `FluidTank`, `Capabilities.FluidHandler.BLOCK` |
| RF-Kompatibilität (Dynamo, Magnetic Engine, EC-RF-Kabel/-Batterie, ReC-Generator) | `Capabilities.EnergyStorage.BLOCK` |
| GUIs | `MenuType` (`IMenuTypeExtension.create` für Positionsdaten), `AbstractContainerScreen`, `ContainerData` |
| Synchronisation | `getUpdateTag`/`getUpdatePacket`; Payloads nur für Benutzeraktionen (Knöpfe, Schieberegler) |
| Item-Zustände | `DataComponentType` (Treibstoff, Ladung, Modus, Ziele …) |
| Strahlung, Kontamination | `AttachmentType` am Chunk (persistiert) bzw. Entity |
| Netze (ElectriCraft), Reaktorweite Daten | `SavedData` pro `ServerLevel` |
| Rezepte | Vanilla-Crafting als JSON per Datagen; Maschinenrezepte als eigene `RecipeType` + `RecipeSerializer` |
| Erze | `ConfiguredFeature`/`PlacedFeature` + `neoforge:add_features` Biome Modifier (Datagen) |
| Konfiguration | `ModConfigSpec`; Bereichs- und Leistungswerte SERVER, Lautstärke und Partikel CLIENT |
| Chunk Loader, Fission-Kerne | `TicketController` |
| Rendering | `BlockEntityRenderer` (bewegte Teile), JSON-Modelle (statisch), `IClientItemExtensions` (Item-Darstellung) |
| Sounds | `SoundEvent` (DeferredRegister), Loop-Sounds als `AbstractTickableSoundInstance` |
| Tests | NeoForge GameTest (`@GameTestHolder`, `runGameTestServer`) + JUnit für reine Logik |

## 5. Zuordnung Feature → Zielmod → Test

Die vollständige Zuordnung aller **723** Einzel-Features (Maschinen, Blöcke, Items, Entities, Fluide, Erze, Sounds, Advancements, Konfiguration, Integrationen, DragonAPI-Bestandteile) mit Status und Test steht in **`02-feature-matrix.md`**. Zusammengefasst nach Gruppen:

| Feature-Gruppe | Ursprung | Zielmod | Test |
|---|---|---|---|
| Wellenleistung: Motoren (9), Wellen, Getriebe (6+2), Schwungräder (7), Kupplungen, Riemen/Ketten, CVT, Splitter, Busse | RC | rotarycraft | JUnit: Leistungsformeln, Übersetzung, Bruchgrenzen · GameTest: Motor → Welle → Verbraucher erreicht Sollwert |
| 139 RC-Maschinen (Verarbeitung, Landwirtschaft, Verteidigung, Transport, Wetter, Überwachung …) | RC | rotarycraft | GameTest je Maschine: Platzieren, Mindestleistung, Kernfunktion |
| Werkzeuge, Rüstung, Handgeräte, Jetpacks | RC | rotarycraft | GameTest (wo serverseitig prüfbar), sonst manueller Client-Test |
| 13+ Maschinenrezepttypen, ca. 637 Crafting-Rezepte | RC | rotarycraft | Datagen-Abgleich: Anzahl alt vs. neu · JUnit: Rezeptauflösung |
| Canola, Zusatz-Eisen, Kisten-Loot | RC | rotarycraft | GameTest Wachstum · Datagen Feature/Loot |
| 46 Reaktormaschinen, 6 Reaktortypen, 6 Multiblocks | ReC | reactorcraft | GameTest: Strukturerkennung, Kritikalität, SCRAM, Kernschmelze in Testarena |
| Strahlung, Hazmat, Geiger, Kontamination | ReC | reactorcraft | GameTest: Effekt und Schutz · Speichern/Laden der Attachments |
| 10 ReC-Erze, 8 Fluoritfarben, Materialblöcke | ReC | reactorcraft | Datagen · `/place feature` in GameTest |
| 24 ReC-Fluide | ReC | reactorcraft | JUnit/GameTest: Registrierung und Tank-Transfer |
| Elektrisches Netz: Drähte (9), Batterien (6), Generator, Motor, Widerstände, Relais, Sicherung, Transformator, Messgerät | EC | electricraft | JUnit: Netzberechnung · GameTest: Generator → Draht → Motor; Sicherung brennt durch |
| RF-Kabel/-Batterie, Ladepad, Energiekristall | EC | electricraft | GameTest: FE-Fluss |
| 6 EC-Erze | EC | electricraft | Datagen |
| Handbücher (249/88/31 Einträge) | RC/ReC/EC | jeweiliger Mod | Ladetest aller XML-Seiten (JUnit) + manueller Client-Test |
| Benötigte DragonAPI-Teile | DAPI | rotarycraft (intern) | JUnit für Logik |
| JEI-, Jade-, CC-Integration | alle | jeweiliger Mod (optional) | Ladetest mit **und ohne** Fremdmod |

## 6. Bewusst nicht übernommene bzw. ersetzte Teile (Vorschlag, Freigabe nötig)

| Teil | Grund |
|---|---|
| EU-Maschinen (RC Generator/Electric Motor, EC EU-Kabel/-Batterie/-Splitter, ReC EU-Ausgang) | IC2 existiert für 1.21.1 nicht |
| BuildCraft-, Forestry-, Thaumcraft-, Multipart- und ChromatiCraft-Inhalte (Fuel Engine, Grafter, Bedrock Saw, Reveal-Brille, Aspekte …) | Zielmods fehlen |
| DragonAPI-Coremod und Bugfix-Patches, Stripper, Class-Reparenter | 1.7.10-spezifisch; NeoForge-Isolation ersetzt sie |
| 48 DragonAPI-Admin-Befehle | großteils durch Vanilla-Befehle abgedeckt (Regel 3) |
| Update-Checker, Donator/Patreon, Crash-Notifications, Remote-Assets, Integritäts-Tracker | kein Spielfeature |
| Benutzerrezeptdateien `*.recipes_*`, MineTweaker-Anbindung | ersetzt durch Datapacks |
| Konfigurierbare Potion-/Material-IDs | durch Registries obsolet |
| Gebündelte Fremd-APIs | nicht nötig |

## 7. Migrationsreihenfolge

Jeder Schritt endet mit einem erfolgreichen Build und den zugehörigen Tests. Erst danach folgt der nächste.

1. **Gerüst:** Multi-Projekt-Gradle (`settings.gradle` mit `rotarycraft`, `reactorcraft`, `electricraft`), ModDevGradle 2.0.148, NeoForge 21.1.262, drei leere Mods, die gemeinsam starten (Client + Dedicated Server). Asset-Konvertierungsskripte (Sprite-Sheets zerlegen, `.lang` → `.json`, Texturpfade).
2. **`rotarycraft/lib` + `api`:** Mathe, Strukturen, Handbuch-Framework, Basis-BlockEntity, Wellenleistungs-API. JUnit-Tests.
3. **RotaryCraft-Kern:** Motoren, Welle, Kegelrad, Getriebe, Kupplung, Schwungrad, Dynamometer, Splitter mit Renderern, Rezepten und GameTests. **Erster Meilenstein:** spielbares Mini-RotaryCraft. Hier wird auch geprüft, ob das Renderer-Konvertierungsverfahren trägt.
4. **RotaryCraft-Werkstoffe:** HSLA, Bedrock, Extraktor-Kette, Grinder, Blast Furnace, Worktable, Deko-Blöcke, Fluide, Rohre und Schläuche.
5. **Übrige RotaryCraft-Maschinen**, gruppiert (Landwirtschaft, Bergbau, Verteidigung, Transport, Umwelt, Überwachung), Werkzeuge und Rüstung, Entities, Advancements, Handbuch.
6. **ElectriCraft** (kleiner, hängt nur an der RC-API): Netzmanager, Drähte, Generator/Motor, Batterien, RF-Teile, Erze.
7. **ReactorCraft:** Erze und Materialien, Fluide, Kreisläufe und Turbinen, Fission, Breeder, HTGR, Thorium, Solar, Fusion, Strahlung.
8. **Optionale Integrationen** (JEI, Jade, CC: Tweaked), soweit freigegeben.
9. **QS (Phase E)** und `07-test-report.md`.

## 8. Bekannte Einschränkungen (absehbar)

- Fremdmod-Inhalte laut §6 fehlen.
- Keine Weltkompatibilität mit 1.7.10-Spielständen.
- Rendering wird technisch neu geschrieben. Geometrie und Texturen bleiben, aber Licht, Transparenz und Shader-Effekte (Hitzeflimmern, Plasma) können **sichtbar abweichen**.
- Kamera-, Bildschirm- und Projektor-Maschinen (CCTV, Screen, Spy Cam, Projector) brauchen Render-to-Texture in der 1.21-Pipeline. Hier ist das Risiko am höchsten, dass sie nur eingeschränkt funktionieren.
- Jetpack-Kick-Umgehung und das Herausdrücken von Items aus Blöcken sind evtl. nur per Mixin machbar.
- Realistischer Aufwand: Schritte 1–3 sind eine überschaubare erste Etappe. Der vollständige Port (Schritte 4–9) ist ein **großes, mehrmonatiges Vorhaben** und braucht viele Arbeitssitzungen.

## 9. Offene Entscheidungen, die deine Freigabe brauchen

1. **Lizenz:** Bestätigst du, dass der Port **nur privat** genutzt und nicht weitergegeben wird (bzw. dass eine Erlaubnis von Reika vorliegt)? Ohne diese Bestätigung würde ich nicht implementieren.
2. **Architektur:** drei JARs mit in RotaryCraft integrierten DragonAPI-Resten (empfohlen) oder vier JARs mit eigener Bibliothek?
3. **Mod-IDs:** `rotarycraft`, `reactorcraft`, `electricraft` – einverstanden?
4. **Nicht reproduzierbare Fremdmod-Features (§6)** entfallen lassen?
5. **Vanilla-Überschneidungen:** ElectriCraft-Kupfer durch Vanilla-Kupfer ersetzen? ReC-„Calcite“ und -„Lodestone“ unter eigenem Namen behalten (z. B. `reactorcraft:calcite_ore`)?
6. **Deepslate-Erzvarianten:** keine (empfohlen, ohne neue Assets) oder neue Overlay-Texturen?
7. **Admin- und Entwicklerwerkzeuge** (World Edit Tool, Debug-Item, DragonAPI-Befehle) übernehmen?
8. **Optionale Integrationen:** JEI, Jade, CC: Tweaked einplanen? Später AE2, PneumaticCraft, Railcraft-Dampf prüfen?
9. **Furnace-Übernahme (Friction Heater):** Accessor-Lösung ohne Mixin, wenn verhaltensgleich, sonst gezielter Mixin – einverstanden?
10. **Umfang der ersten Umsetzungsetappe:** Schritte 1–3 (Gerüst, Bibliothek/API, RotaryCraft-Kern) als erste prüfbare Lieferung?
