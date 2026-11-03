# 01 – Die vier Original-Mods

Stand: Phase A (reine Analyse, nichts implementiert). Die Originalordner wurden nicht verändert. Die JARs wurden zur Analyse nur in ein temporäres Verzeichnis entpackt.

## 0. Ausgangsmaterial

| Mod | Quelltext | JAR | Git-Stand im Quellordner | JAR-Version |
|---|---|---|---|---|
| DragonAPI | `DragonAPI/` – 1 249 Java-Dateien, 164 380 Zeilen | `DragonAPI 1.7.10 V33b.jar` (2 958 Einträge, 2 763 Klassen) | `c29c4fcc`, 2023-08-24, „Update 1117 – v33a“ | V33**b** |
| RotaryCraft | `RotaryCraft/` – 980 Dateien, 178 124 Zeilen | `RotaryCraft 1.7.10 V33a.jar` (2 409 Einträge, 1 300 Klassen) | `b2288638`, 2023-08-24, „Update 1088 – v33a“ | V33a |
| ReactorCraft | `ReactorCraft/` – 272 Dateien, 45 135 Zeilen | `ReactorCraft 1.7.10 V33a.jar` (728 Einträge, 325 Klassen) | `d788f1b`, 2023-07-24, „Update 526“ | V33a |
| ElectriCraft | `ElectriCraft/` – 128 Dateien, 15 926 Zeilen | `ElectriCraft 1.7.10 V33a.jar` (307 Einträge, 151 Klassen) | `abd8bd0`, 2023-07-24, „Update 214“ | V33a |

Alle vier stammen von Reika Kalseki (GitHub `ReikaKalseki/*`). Insgesamt liegen etwa 403 000 Zeilen Java-Quelltext vor. **Vollständiger Quelltext ist vorhanden.** Dekompilieren ist daher nicht nötig.

**Unklar:** Für DragonAPI entspricht der Quelltext v33a, das JAR aber V33b. Die Unterschiede zwischen a und b sind nicht dokumentiert. Bei ReactorCraft und ElectriCraft nennt der Commit keine Versionsnummer. Dass Quelltext und JAR übereinstimmen, wird nur angenommen; geprüft wurde es nicht.

**Wichtig:** Die Quellordner enthalten **keine** `assets/`-Struktur. Block- und Item-Icons, Sprachdateien und `.mcmeta`-Animationen gibt es **nur in den JARs**. Modelltexturen, Sounds, Handbuch-XML und Shader liegen dagegen im Quellbaum (siehe `04-assets.md`).

### Gemeinsame Merkmale (alle vier)

- Minecraft **1.7.10**, Modloader **Forge (FML) für 1.7.10**, Java 6/8-Ära, Gradle-Build nicht im Ordner enthalten (kein `build.gradle`).
- Abhängigkeitskette laut `@Mod(dependencies=…)`:
  - DragonAPI: nur `after:` auf viele Fremdmods (soft)
  - RotaryCraft: `required-after:DragonAPI`
  - ReactorCraft: `required-after:DragonAPI;required-after:RotaryCraft`
  - ElectriCraft: `required-after:DragonAPI;required-after:RotaryCraft`
- Registrierung über Java-Enums (`MachineRegistry`, `ItemRegistry` …) mit **Metadaten-Subtypen** (vor dem „Flattening“ von 1.13).
- Rezepte, Loot, Erzeugung, Fortschritte und Tags sind **im Java-Code** definiert. Datapack-JSONs gibt es nicht.
- Modelle sind **Java-Modellklassen im Techne-Stil** (`ModelBase`/`ModelRenderer`) mit Tile-Entity-Spezialrenderern. Block- und JSON-Modelle gibt es nicht.
- Eigenes Paketsystem über DragonAPI (`PacketHandlerCore`, `PacketRegistry`).
- `mcmod.info` ist bei allen leer (modid/name/description fehlen). Die Metadaten stehen nur in der `@Mod`-Annotation.
- Lizenz: siehe Abschnitt 5 und `03-dependencies.md`.

---

## 1. DragonAPI (Bibliothek + Coremod)

**Mod-ID:** `DragonAPI` · **Paket:** `Reika.DragonAPI` · **Coremod:** ja (`FMLCorePlugin: Reika.DragonAPI.Auxiliary.DragonAPIASMHandler`, Access Transformer `DragonAPI_at.cfg` mit 211 Zeilen)

DragonAPI fügt **keine** Spielinhalte hinzu (keine eigenen Blöcke oder Items). Die einzige Registrierung in `DragonAPIInit` ist ein Furnace-Fuel-Handler. Es ist die Grundlage aller Reika-Mods:

| Bereich | Umfang | Inhalt |
|---|---|---|
| `Instantiable/` | 485 Dateien, 58 037 Z. | Datenstrukturen (BlockArray, StructuredBlockArray, Maps, HybridTank, StepTimer), GUI-Bausteine, Rendering-Helfer (StructureRenderer, TessellatorVertexList), IO (XMLInterface, SoundLoader, MIDIInterface, ControlledConfig), eigene Events |
| `Libraries/` | 71 Dateien, 31 950 Z. | Statische Helfer: Mathe/Physik (`ReikaEngLibrary`: Materialkonstanten E, G, Zugfestigkeit, Dichte), Inventar, NBT, Fluid, Welt, Rendering, Registry |
| `ModInteract/` | 142 Dateien, 25 472 Z. | Anbindung an rund 100 1.7.10-Mods (IC2, BuildCraft, Thaumcraft, Forestry, AE2, Thermal …), Lua-Bindings für ComputerCraft/OpenComputers, Ore-Dictionary-Listen (`ModOreList`) |
| `Auxiliary/` | 61 Dateien, 12 001 Z. | Tracker (Update-Checker, Donator/Patreon, Crash-Notifications, Integritäts- und Pack-Checks, TickScheduler, Keybinds, ChunkManager, Retrogen) |
| `ASM/` | 197 Dateien, 11 234 Z. | Coremod mit 181 Patcher-Klassen (Vanilla-Bugfixes, zusätzliche Event-Hooks), `APIStripper`/`DependentMethodStripper` (entfernt Interfaces und Methoden, wenn Fremdmods fehlen), `InterfaceInjector`, `ClassReparenter` |
| `Command/` | 49 Dateien | 48 Admin-/Debug-Befehle (Biome-Karte, Block-Ersetzen, Entity-Zählung, NBT-Ansicht, Chunk-Regen, Profiling …) |
| `Base/` | 25 Dateien | Basisklassen (`DragonAPIMod`, `TileEntityBase`, `BlockTEBase`, `CoreContainer`, `BlockMultiBlock`, `ISBRH` …) |
| `Interfaces/`, `Exception/`, `Extras/`, `IO/`, `ModRegistry/` | zusammen etwa 13 500 Z. | Schnittstellen, Hilfsklassen, `PowerTypes` (RF, EU, ROTARYCRAFT, PNEUMATIC, HYDRAULIC, STEAM, ELECTRICRAFT) |

Das DragonAPI-JAR enthält außerdem **gebündelte Fremd-APIs**: `appeng/api` (247 Klassen), `cofh/api` (12), `minechem/api`, `com/InfinityRaider` (AgriCraft), dazu API-Kopien weiterer Reika-Mods (RotaryCraft-API 90 Klassen, ChromatiCraft, Satisforestry, MeteorCraft, VoidMonster, LootTweaks, GeoStrata).

Tatsächliche Nutzung durch die drei Inhalts-Mods: **468 verschiedene DragonAPI-Klassen** werden direkt importiert (RotaryCraft 421, ReactorCraft 193, ElectriCraft 103). Hinzu kommen transitive Abhängigkeiten.

---

## 2. RotaryCraft

**Mod-ID:** `RotaryCraft` · **Paket:** `Reika.RotaryCraft` · **Coremod:** ja (`RotaryASMHandler`, ein Patch: `FURNACEINTERCEPT` lässt den Vanilla-Ofen-Tick ausfallen, wenn ein Friction Heater den Ofen „übernimmt“)

### Kernkonzept
Ein Energiesystem auf Basis **mechanischer Wellenleistung**: Leistung = Drehmoment × Winkelgeschwindigkeit (`torque`, `omega`). Motoren erzeugen Leistung. Wellen, Kegelräder, Getriebe (mit Material-Verschleiß und Schmierung), Kupplungen, Schwungräder (Trägheit), Riemen/Ketten, CVT/Advanced Gears, Splitter und Busse übertragen sie. Verbraucher (Maschinen) haben Mindestwerte für Drehmoment, Drehzahl und Leistung (`PowerReceivers`, 94 Einträge). Materialversagen (Wellen- und Schwungradbruch) folgt aus Materialkonstanten (`MaterialRegistry`: E-Modul, Schubmodul, Zugfestigkeit, Dichte).

### Inhalt (aus den Registries gezählt)

| Kategorie | Anzahl | Quelle |
|---|---|---|
| Maschinen (Tile Entities) | 139 | `MachineRegistry` |
| Motortypen | 9 (DC, Wind, Steam, Gas, AC, Sport, Hydro, Micro-Turbine, Jet) | `EngineType` |
| Getriebe-Materialien | 8 (Holz, Stein, Stahl, Wolfram, Diamant, Bedrock + 2 × Botania) | `GearboxTypes` |
| Schwungräder | 7 | `Flywheels` |
| Blöcke | 30 (20 Metadaten-Maschinenträger + 10 eigene: Deko, Canola, Licht, Strahl, Lichtbrücke, Bohrrohr, Panzerglas/-scheibe, Bedrock-Scheibe, Deko-Tank) | `BlockRegistry` |
| Items | 89 Registry-Einträge, davon viele Metadaten-Sammelitems; insgesamt 155 benannte Stacks in `ItemStacks` | `ItemRegistry` |
| Entitäten | 11 (Projektile, Eisblock, Gas-Lore, Sprenggranate, Schallwelle, Entladung, Custom-TNT, Flak, Gatling, Flammenschuss) | `RotaryEntities` |
| Fluide | 5 + „poison“ (Ethanol, Jet Fuel, Lubricant, Flüssigstickstoff, geschmolzenes HSLA) | Code |
| Trankeffekte | 2 (Freeze, Deafness) | `Auxiliary/` |
| Werkzeuge/Rüstung | Bedrock- und Stahl-(HSLA-)Werkzeuge, -Rüstung, Jetpacks, Sprungstiefel, Nachtsichtbrille, IO-Brille | `ItemRegistry` |
| Waffen und Handgeräte | Gravel Gun, Stun Gun, Fireball Launcher, Flammenwerfer, Railgun-Munition, Vakuum, Ultraschall, Bewegungsmelder, Rangefinder, Handpumpe, Handkolben | `ItemRegistry` |
| Maschinenrezepttypen | 13 Recipe-Manager (Grinder, Extractor, Blast Furnace, Compactor, Centrifuge, Crystallizer, Drying Bed, Friction Heater, Lava Maker, Magnetizer, Pulse Jet, Wetter, Worktable) + Fermenter, Fractionator, Fuel Converter, Filling Station, Composter, Drop Processor, Refrigerator (im TE-Code) | `Auxiliary/RecipeManagers` |
| Crafting-Rezepte | ca. 637 Aufrufe von `add*Recipe`/`addSmelting` | `RotaryRecipes`, Code |
| Benutzerrezepte | Dateiformat `*.recipes_<maschine>` (12 Beispiele) | `CustomRecipeExamples/` |
| Achievements | 46 | `RotaryAchievements` |
| Handbuch | 249 Einträge, Texte in 12 XML-Dateien (+ Übersetzungen es/fr/hu/pl/pt) | `HandbookRegistry`, `Resources/*.xml` |
| GUIs / Container | 69 / 53 | `GUIs/`, `Containers/` |
| Modelle / Renderer | 122 Modellklassen / 112 Renderer | `Models/`, `Renders/` |
| Sounds | 46 Registry-Einträge, 49 OGG | `SoundRegistry`, `Sounds/` |
| Pakete | 67 Pakettypen | `PacketRegistry` |
| Konfiguration | 91 Optionen + 3 ID-Optionen | `ConfigRegistry`, `ExtraConfigIDs` |
| Befehle | `/findmachines` | `FindMachinesCommand` |
| Weltgenerierung | Zusätzliches Eisenerz (`ExtraIronGenerator`, nur wenn „Iron Ore Density“ > 1). Sonst keine eigenen Erze, Biome, Strukturen oder Dimensionen. Der Borer kann Chunks im Radius erzeugen (`BORERGEN`). | |
| Kisten-Loot | `RotaryChests` (Config `CHESTGEN`) | |
| Sonstiges | Schwierigkeitsstufen (`DifficultyEffects`), Maschinenhaltbarkeit (`DurationRegistry`), Extraktor-Boni (`ExtractorBonus`), Mob-Köder (`MobBait`), Pflanzenmaterial (`PlantMaterials`), Shader (Hitzeflimmern/-glühen) | |
| Öffentliche API | `API/` (71 Dateien): Shaft-Power-Schnittstellen, IO-, Reservoir-, Worktable- und Extract-API, 20 API-Events | |
| Mod-Integrationen | `ModInterface/` (108 Dateien): NEI (24 Handler), Lua (34 Methoden), MineTweaker (3), Thaumcraft-Aspekte/Infusion, Forestry-Bienen, AgriCraft, BuildCraft-Statements, Bundled Bus (AE2/ProjectRed), Konverter RF/EU/Pneumatic/Steam | |

### Event-Handler (Auswahl, `RotaryEventManager` u. a.)
Lebewesen-Schaden (Bedrock-Rüstung, Rüstungsbonus), Fall (Sprungstiefel), Mob-Drops (Grinder „meat grinding“, Mob Harvester mit Looting), Despawn-Schutz (Spawner Controller), Ackerland-Zertrampeln, Enderman-Blick (Visier), Creeper-Explosion, Spawner-Abbau, Feuer an der Schmierschlauch-Leitung, Worktable-Aufbau, Bonemeal, Crafting-Aufräumen (Reservoir/Deko-Tank), Rezept-Override beim Serverstart, Sound-Dämpfer (Deafness), HUD-Overlays. Viele davon nutzen **per ASM eingeschleuste DragonAPI-Events**, zum Beispiel `SetBlockEvent`, `CreeperExplodeEvent`, `EnderLookAggroEvent`, `FarmlandTrampleEvent`, `EntityPushOutOfBlocksEvent`, `ItemSizeTextEvent`.

---

## 3. ReactorCraft

**Mod-ID:** `ReactorCraft` · **Paket:** `Reika.ReactorCraft` · Kein Coremod · benötigt DragonAPI + RotaryCraft

### Kernkonzept
Simulation von Kernreaktoren und Dampfkreisläufen. Neutronen sind **echte Entitäten** (`EntityNeutron`), die zwischen Brennstäben fliegen. Dazu kommen Temperatur, Moderation und Absorption sowie Steuerstäbe mit CPU-Steuerung. Wärme geht an Kessel oder Natrium/CO₂/LiFBe-Kreisläufe, die Dampf für Turbinen (auch Multiblock-Hochdruckturbine) erzeugen. Turbinen geben **RotaryCraft-Wellenleistung** ab. Der Reactor Generator setzt in RF/EU/ElectriCraft-Strom um. Kernschmelze erzeugt Corium. Strahlung wirkt als Entität und Trankeffekt, Kontamination ist persistent. Fusion umfasst einen Tokamak (Toroid-Magnete, Injektor, Heizer, Solenoid, Plasma-Entitäten) sowie Deuterium/Tritium-Gewinnung.

| Kategorie | Anzahl | Quelle |
|---|---|---|
| Maschinen (Tile Entities) | 46 | `ReactorTiles` |
| Reaktortypen | 6 (Fission, Breeder, HTGR/Pebble Bed, Thorium/LFTR, Fusion, Solar) | `ReactorType` |
| Blöcke | 22 Registry-Einträge (Metadaten: Materialien, Erze, Fluorit, Multiblock-Teile, Lampen, Corium, Dampf, Gase) | `ReactorBlocks` |
| Items | 25 Registry-Einträge + 19 Crafting-Zwischenprodukte (`CraftingItems`) | `ReactorItems` |
| Entitäten | 4 (Neutron, Radiation, Plasma, Fusion) | `ReactorEntities` |
| Fluide | 24 (u. a. schweres Wasser, Natrium, CO₂, Ammoniak, UF₆, Tritium, Deuterium, Plasma, Corium, Atommüll) | Code |
| Trankeffekte | 1 (Radiation) | `PotionRadiation` |
| Erze und Weltgenerierung | 10 Erze (Fluorit, Pechblende, Cadmium, Indium, Silber, Endblende im End, Ammonium im Nether, Calcit, Magnetit, Thorium) | `ReactorOres`, `World/ReactorOreGenerator` |
| Fluoritfarben | 8 | `FluoriteTypes` |
| Material-Blöcke | 6 (Beton, Schlacke, Calcit, Wäscher, Lodestone, Graphit) | `MatBlocks` |
| Multiblock-Strukturen | 6 (Preheater, Generator, Injector, Solenoid, HP-Turbine, Flywheel) | `ReactorStructures` |
| Strahlungsschilde | 7 Materialien | `RadiationShield` |
| Achievements | 28 | `ReactorAchievements` |
| Handbuch | 88 Einträge, 20 XML | `ReactorBook` |
| Sounds | 7 Einträge, 8 OGG | `ReactorSounds` |
| Pakete | 4 | `ReactorPackets` |
| Konfiguration | 17 Optionen | `ReactorOptions` |
| Befehle | `/clearsteam` | |
| Ausrüstung | Hazmat-Anzug (4 Teile), Strahlungsbrille, Geigerzähler, Strahlungsreiniger, Eisenfinder, CPU-Fernbedienung | |
| Integration | NEI (4 Handler), Lua, WAILA, IC2, Thaumcraft, Botania (Alfheim-Atommüll-Event), TFC, Gendustry, MFR | |

Querbezüge: `TileEntityReactorGenerator` implementiert `ElectriCraft.API.WrappableWireSource`. Das ist über `@Strippable` und `@ModDependent(ELECTRICRAFT)` **optional**.

---

## 4. ElectriCraft

**Mod-ID:** `ElectriCraft` · **Paket:** `Reika.ElectriCraft` · Kein Coremod · benötigt DragonAPI + RotaryCraft

### Kernkonzept
Ein elektrisches Netz mit Spannung und Strom. Generatoren wandeln Wellenleistung in Strom um, Motoren Strom zurück in Wellenleistung. Drähte haben pro Material Grenzen für Strom und Spannung (`WireType`, 9 Typen inklusive Supraleiter). Dazu kommen Widerstand, Präzisionswiderstand, Relais, Sicherung, Transformator, Messgerät und Batterien (`BatteryType`, 6 Stufen). Ein Netzwerk-Manager mit eigenen Tick- und Repath-Events (`ElectriNetworkManager`, `Network/`) steuert das Netz. Daneben gibt es reine RF-Kabel/-Batterien, EU-Kabel/-Batterien/-Splitter und ein drahtloses Lade-Pad.

| Kategorie | Anzahl | Quelle |
|---|---|---|
| Maschinen (Tile Entities) | 16 | `ElectriTiles` |
| Blöcke | 10 | `ElectriBlocks` |
| Items | 9 Registry-Einträge (Barren, Crafting, Energiekristall, Handbuch, Platzier-Items) | `ElectriItems` |
| Erze und Weltgenerierung | 6 (Kupfer, Zinn, Silber, Nickel, Aluminium, Platin) | `ElectriOres`, `World/ElectriOreGenerator` |
| Draht- und Batterietypen | 9 / 6 | `WireType`, `BatteryType` |
| Handbuch | 31 Einträge, 6 XML | `ElectriBook` |
| Pakete | 2 | `ElectriPackets` |
| Konfiguration | `ElectriOptions` (u. a. „Ore Discretization“) | |
| Modelle / Renderer | 7 / 11 | |
| Integration | WAILA (20 Bezüge), IC2 (17), Lua, Thermal Foundation, Condensed Ores | |

---

## 5. Lizenz (alle vier identisch)

`License.txt` ist bei DragonAPI, RotaryCraft und ElectriCraft byte-identisch. ReactorCraft hat keine eigene Datei, trägt aber denselben Copyright-Header („All rights reserved. Distribution … only allowed with explicit, prior permission“). Kernaussagen:

- **Mod-Code:** Nutzung zu Lernzwecken und **abgeleitete Werke nur für den privaten Gebrauch** erlaubt. Weitergabe von Quelltext oder abgeleiteten Werken **nur mit ausdrücklicher schriftlicher Erlaubnis**. „Derivative mods using any or all of the source code or assets are EXPRESSLY FORBIDDEN.“ Binärdateien dürfen nur der Autor (oder Modpacks) verbreiten.
- **API-Code** (Pakete `API`): Weitergabe zur Interop erlaubt, aber **keine Änderung ohne vorherige Rücksprache** mit Reika.
- Kennzeichnungs- und Attributionspflicht, keine kommerzielle Nutzung.

➡️ **Folge für dieses Projekt:** Ein Port ist allenfalls als **privates abgeleitetes Werk** zulässig. Die erzeugten JARs dürfen ohne Erlaubnis von Reika **nicht veröffentlicht oder weitergegeben** werden. Die Formulierung zu „derivative mods“ steht sogar in Spannung zur privaten Nutzung. Das ist eine **Freigabefrage** (siehe `06-target-architecture.md`, Abschnitt „Offene Entscheidungen“). Ich bin kein Jurist; das ist eine Wiedergabe des Lizenztexts, keine Rechtsberatung.
