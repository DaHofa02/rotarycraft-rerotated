# PORTING_NOTES – RotaryCraft / ReactorCraft / ElectriCraft → NeoForge 1.21.1

**Notizen-Version: v12 (2026-11-10)** · Sitzung: `claude/stoic-volta-ysznnm` · Diese Datei ist die Übergabe zwischen Arbeitssitzungen und
KI-Modellen. Vor jeder neuen Aufgabe lesen, nach jedem größeren Schritt fortschreiben (neuen Versionsabschnitt oben in „Änderungsverlauf“
ergänzen, Abschnitte B–G aktualisieren). Ausführliche Fachdokumentation des bisherigen Ports: `docs/01`–`docs/08` (Deutsch);
`docs/08-implementation-status.md` beschreibt jede portierte Maschine mit Formeln und Tests.

## Änderungsverlauf
| Version | Datum | Branch / Commits | Inhalt |
|---|---|---|---|
| v11 | 2026-11-09 | `claude/stoic-volta-ysznnm`: `ebefaad` … dieser Commit | Druckhalter, Gasleitung, Wärmerohr portiert (C31) – **ReactorCraft 43 von 44** (nur die Original-Attrappe Mini Turbine fehlt, G-8); drei Tests platzierungsunabhängig (C32); „All 296 required tests passed“ |
| v12 | 2026-11-10 | `claude/stoic-volta-ysznnm`: `63af58e` … dieser Commit | Entscheidungen G-1/G-2/G-3 (F-8, F-11 entfallen), G-5/G-6/G-8 vertagt; F-9 begonnen: Ventilator, Bohrer, Mob-Ernter, Aggregator, Flüssigkeits-Verteiler, Block-Füller, Rauchmelder, Spieler-Detektor, Mob-Radar, Gesteinsschmelzer, Heizer, Komposter, Kristallisator, Lampe (C33) |
| v10 | 2026-11-09 | `claude/stoic-volta-ysznnm`: `d913a72` … `c5e917f` (drei Subagents parallel + Hauptstrang) | **Alle 199 Blöcke in Factory-Zonen**, Abdeckungstest wieder Pflicht (C29); Port-Fehler: RF-Netz speist Funkpads, Treibstoffleitung trägt LiFBe, Brennstoffablass als Fluid-Handler (C30); „All 284 required tests passed“ |
| v9 | 2026-11-09 | `claude/stoic-volta-ysznnm`: `6654a62`, `1c7bb57`, `de6719d`, `1a9a41a` (Subagent), dieser Commit | Alle übrigen RotaryCraft-Maschinen in Zonen (C28) – **160 von 199**, kein `rotarycraft:`-Block fehlt mehr; „All 275 required tests passed“ |
| v8 | 2026-11-09 | `claude/stoic-volta-ysznnm`: `cc508c6`, `f129612`, `f726310`, dieser Commit | Zonen Solar und Motoren/Raps (C26) – **146 von 199**; Fluorit-Neutronentest robust (C27) |
| v7 | 2026-11-09 | `claude/stoic-volta-ysznnm`: `eb62217`, `aa977b6`, `f077a59`, `4f10d5e`, `780c490`, `06917af`, dieser Commit | Zonen Fusion, Spaltung, Prozessor, Abfall (C23) – **139 von 199**; Port-Fehler Abfallfass per Hand nicht auffüllbar behoben (C24); Steuerstab-Test 6 σ (C25) |
| v6 | 2026-11-09 | `claude/stoic-volta-ysznnm`: `db9b844`, `70ac64d`, dieser Commit | Zonen kleine Maschinen und Sonderrohre (C22) – **100 von 199** Blöcken |
| v5 | 2026-11-09 | `claude/stoic-volta-ysznnm`: `d6b2812`, `bdf4076`, `1d6df27`, `3ef30ec`, dieser Commit | Weitere Factory-Zonen (C20: ElectriCraft, Fluide/Motoren, Werkstatt) – **92 von 199** Blöcken; Grinder-Schmiermitteltest robust (C21) |
| v4 | 2026-11-09 | `claude/stoic-volta-ysznnm`: `62169cf`, `4d05ca9`, `b4f0019`, `fd677f2`, `bdf863c`, dieser Commit | G-4 umgesetzt: Abdeckungstest optional (C16), erste Factory-Zonen (C18: Antriebsstrang, Erze, Lagerblöcke – 48 von 199 Blöcken), Lagerblöcke ohne Drops behoben (C19), Magnetrohr-Test robust (C17) |
| v3 | 2026-11-09 | `claude/stoic-volta-ysznnm`: `d6269ff`, `6b6d5c1`, `c619751`, `e92bf6a`, dieser Commit | **Erster Build und Testlauf mit Netz** (F-1: v1/v2-Code kompiliert, alle Tests grün außer dem bekannten Factory-Test), Plasma-Test robust (C12), Turbinen-Dynamometer (C13), Dampfdiffusor (C14), Dampfleitung ↔ Hochdruckturbine (C15), Werkzeug: Dekompilieren der Original-JARs mit Vineflower |
| v2 | 2026-11-04 | `claude/stoic-volta-ysznnm`: `66f1bb2`, `af5ac36`, `28dedad`, `4531006`, `6feb4d0`, dieser Commit | Explosionsfestigkeit/Härte aller Blöcke nach Original (C6), ElectriCraft-Spitzhacken-Tag (C7), Brennstoffe (C8), Worktable-Bau in der Welt (C9), Prüfer: Java-Texturen und Data Maps (C10), Neutronenreflektor (C11) |
| v1 | 2026-11-03 | `claude/stoic-volta-ysznnm`: `aec563f`, `b735d43`, `d98b9b4` | Bestandsaufnahme, statischer Ressourcen-Check, Item-Texturen zurück zur Original-Form, Deko-Blöcke (Stahlblock u. a.), Explosionsfestigkeit der Maschinen, Spitzhacken-Tags |

---

## A. Projektübersicht

| Punkt | Wert | Quelle |
|---|---|---|
| Zielversion | Minecraft **1.21.1** | `port/gradle.properties` |
| Mod-Loader | NeoForge **21.1.262** (Bereich `[21.1.0,)`) | `port/gradle.properties` |
| Build-Plugin | ModDevGradle **2.0.148** (`net.neoforged.moddev`) | `port/build.gradle` |
| Gradle | **9.2.1** (Wrapper) | `port/gradle/wrapper/gradle-wrapper.properties` |
| Mappings | Parchment **2024.11.17** für MC 1.21.1 (auf Mojang-Mappings) | `port/gradle.properties` |
| Java | **21** (Toolchain) | `port/build.gradle` |
| Projektaufbau | Gradle-Multiprojekt in `port/`: `rotarycraft`, `reactorcraft`, `electricraft` (drei JARs) | `port/settings.gradle` |
| Abhängigkeiten | `reactorcraft` → `rotarycraft` (Pflicht), → `electricraft` (optional); `electricraft` → `rotarycraft` (Pflicht); DragonAPI ist **nicht** eigenständig, die benötigten Teile stecken in `reika.rotarycraft.lib` | `*/src/main/templates/META-INF/neoforge.mods.toml` |
| Optionale Integrationen (nur Kompilierung/Testinstanz) | JEI 19.21.0.247, Mekanism 1.21.1-10.7.19.85, GregTech CEu Modern 7.0.2 | `port/gradle.properties`, `port/reactorcraft/build.gradle` |
| Git-Branch | `claude/stoic-volta-ysznnm` (vorgegebener Arbeitsbranch) | – |
| Ausgangs-Commit | `0e71d62` („init“, Stand von `main` bei Sitzungsbeginn, Arbeitsbaum sauber) | `git log` |
| Originale | Nur die 1.7.10-JARs im Root (`RotaryCraft 1.7.10 V33a.jar`, `ReactorCraft … V33a`, `ElectriCraft … V33a`, `DragonAPI … V33b`). Die Ordner `RotaryCraft/`, `ReactorCraft/`, `ElectriCraft/`, `DragonAPI/` sind **leere Submodul-Einträge ohne `.gitmodules`** – die Original-Quelltexte fehlen im Klon. | `git ls-tree HEAD` |

**Original-Code nachschlagen ohne Quelltexte:** JARs in einen eigenen Ordner entpacken und mit `javap` disassemblieren, z. B.
`javap -c -p -cp <entpackt>/rc:<entpackt>/dapi Reika.RotaryCraft.Blocks.BlockDeco`. So wurden in v1 alle Werte (Härte, Widerstand,
Rezepte, Texturen, Namen) verifiziert. Obfuskierte 1.7.10-Namen: `func_149711_c` = setHardness, `func_149752_b` = setResistance,
`func_149672_a` = setStepSound, `field_149777_j` = soundTypeMetal, `Material.field_151573_f` = Material.iron.

**Original-Quelltext dekompilieren (v3):** Vineflower liegt im Gradle-Cache (NeoForge nutzt es selbst). JARs mit `jar xf` in einen eigenen Ordner
entpacken, dann z. B. `java -jar ~/.gradle/caches/modules-2/files-2.1/org.vineflower/vineflower/1.10.1/*/vineflower-1.10.1.jar -e=<rc> -e=<dapi>
<entpackt>/reactorcraft/Reika <ausgabe>` – lesbarer Java-Code mit obfuskierten Vanilla-Namen (`func_…`/`field_…`). Für Modelle die Namen wie in C14
zurückübersetzen, dann `tools/convert_models.py`.

**Build/Tests (in einer Umgebung mit Netz):**
```bash
cd port && ./gradlew build
```
```bash
cd port && ./gradlew :reactorcraft:runGameTestServer
```
Die zweite Zeile startet die GameTests aller drei Mods (Ergebnis im Log: „All N required tests passed“, Fehler: „failed at“).

---

## B. Portierungsstatus

Legende: ✅ fertig portiert · 🟡 teilweise portiert · 🔴 alter Stand / nicht implementiert · ❔ unklar

| Mod / Bereich | Status | Probleme | Nächster Schritt |
|---|---|---|---|
| **RotaryCraft** | 🟡 Kern fertig, Umfang teilweise | **41 von 150** `MachineRegistry`-Einträgen portiert (Kraftübertragung, alle 9 Motoren, Verarbeitung, Rohre, Hochofen/Worktable, Solar …). Fehlend u. a. Bohrer (BORER), Advanced Gears (CVT/Schnecke/Spule), Riemen/Kette, Mehrfachkupplung, Waffen, Weltwerkzeuge, Generator/E-Motor (Fremdmod-Energie). Werkzeuge/Rüstung aus Stahl/Bedrock, Handbuch, Achievements fehlen. Seit v1/v2 ergänzt: Deko-Blöcke (C2), Brennstoffe (C8), Worktable-Bau in der Welt (C9). | F-9, F-10 |
| **ReactorCraft** | 🟡 weit fortgeschritten | **40 von 44** `ReactorTiles` portiert (Spaltung, Brüter, HTGR/Kugelhaufen, Thorium, Fusion/Tokamak, Turbinen, Generator, Abfall; v2: Neutronenreflektor, C11; v3: Turbinen-Dynamometer C13, Dampfdiffusor C14). Fehlend: Pressurizer (`PUMP`), Gas Duct (`GASPIPE`), Heat Pipe (`HEATPIPE`); Mini Turbine (`MINITURBINE`) ist im Original ein Stub ohne Rezept und Leistung (G-8). Bis v1 waren 7 Rezepte nicht herstellbar und die Stahl-/Bedrock-Abschirmung existierte nicht (behoben, C2). | F-5, F-8 |
| **ElectriCraft** | ✅ weitgehend | **13 von 16** `ElectriTiles` portiert; die 3 fehlenden (EU-Kabel/-Batterie/-Splitter) hängen an IC2 (auf 1.21.1 nicht vorhanden). RF → FE umgesetzt. Spitzhacken-Tag und Härte der Maschinen seit v2 wie im Original (C6, C7). | – |
| **DragonAPI** | 🟡 nur benötigte Teile | Kein eigenständiger Mod; benötigte Bibliotheksteile in `reika.rotarycraft.lib` (Mathe, Umgebungstemperatur/-druck, ParallelTicker). | bei Bedarf je Maschine |
| Registrierung | ✅ | `DeferredRegister` (Blocks/Items/BlockEntityTypes/Menus/Fluids/Entities/RecipeTypes/DataComponents), korrekte `ResourceLocation`s, Lifecycle über Mod-Event-Bus. Keine veralteten Forge-Aufrufe gefunden. | – |
| Blöcke, Items, Inventare | ✅ | ItemHandler/FluidHandler-Capabilities, `Clearable`, Drops im Code (`MachineBlock.getDrops`, kein Loot Table nötig – Absicht). Bis v1/v2: Maschinen ohne `mineable/pickaxe`, Explosionsfestigkeit als 1.7.10-Rohwert (behoben C3/C4/C6/C7). | – |
| BlockEntities | ✅ | Speichern/Laden, Sync alle 5 Ticks bei Änderung, Ausrichtung im BE (bewusst, `docs/08` §3). Laut `docs/07` über GameTests und Neustarts geprüft. | – |
| Energie- und Kraftsystem | ✅ | Leistungsfluss (`IOMachineBlockEntity`, `PowerSourceList`), Drehzahl/Drehmoment mit Original-Formeln; JUnit `FormulaTest` (Motortabelle, Wellenbruch …) + > 100 GameTests mit Original-Sollwerten. In v1 statisch nicht neu verifiziert. | F-1 |
| Netzwerk | ✅ | 8 C→S-Payloads (`RotaryNetwork`, `ReactorPayloads`, `ElectriPayloads`), alle serverseitig validiert (Chunk geladen, BE-Typ, `isPlayerAccessible`, Wertebereich), Verarbeitung auf dem Main-Thread (NeoForge-Standard). S→C über BE-Update-Pakete. | – |
| Rendering | ✅ | BERs mit Original-Transformationskette (Wellen, Zahnräder, Turbinen, Motoren, Tokamak usw. animiert), Item-Renderer für Maschinen, Client-Code nur in `client/`, `compat/jei/`, `dev/` und hinter Dist-Prüfung (`ClientKeys`). Dedicated Server startete laut `docs/07` T4 fehlerfrei. | – |
| Ressourcen | ✅ | Statischer Check (C5, C10; Stand v2): 200 Blockstates (58 RotaryCraft, 100 ReactorCraft, 42 ElectriCraft), 1514 Modelle, 3022 Texturen (inkl. Varianten für verbundene Texturen), 916 Rezepte, Data Maps, 103 Textur-Verweise im Java-Code → **0 Fehler**. Ordner im 1.21-Format (Singular). Laufzeit-Prüfung zuletzt: „475 items checked, 0 without texture“ (`docs/08` §12). | F-1 |
| Mod-Integrationen | ✅ (innerhalb der drei Mods) | ReactorCraft nutzt RotaryCraft-API (Wellenleistung, Fuel Enhancer, Kompaktor, Kühlschrank-Anbauteile, Solar-Natrium); Generator speist ElectriCraft optional. JEI/Mekanism/GregTech nur optional. | – |
| Advancements (74 Original-Achievements) | 🔴 | nicht portiert | F-10 |
| Handbuch (HandbookRegistry) | 🔴 | nicht portiert; GUI-Tab „?“ fehlt | F-10 |
| Weltgenerierung | ✅ | Erze (ReactorCraft, ElectriCraft) über `placed_feature` + `neoforge:add_features`. Die `Unprimed heightmap: OCEAN_FLOOR_WG`-Fehler in den Server-Logs kommen aus `electricraft/gametest/OreTests` (Feature wird zur Laufzeit platziert, vanilla `OreFeature` fragt die WG-Heightmap ab) – kein Worldgen-Fehler. | – |
| Build und Tests | ✅ (v3) | Seit v3 in einer Umgebung mit Netz (eigene VM, Java 21 = OpenJDK 21.0.12, Linux). `./gradlew build` grün, JUnit 11/11; `:reactorcraft:runGameTestServer`: **248 von 249** grün, einziger Fehlschlag `everyblockisinthefactory` (D-2, erwartet); `check_resources_v1.py` 0 Fehler. Damit ist der in v1/v2 ungeprüft geschriebene Code belegt. | F-2 |

### Visuelle Ressourcen (Auftrag 4.1 / visuelle Leitlinie)
| Punkt | Stand |
|---|---|
| Gültige Modelle für alle Blöcke/Items | statisch ✅ (0 fehlende Modelle/Texturen/Parents; jede `item.*`-Übersetzung hat ein Item-Modell) |
| Texturen geladen / keine Missing-Texture | statisch ✅; Laufzeit zuletzt 2026-10 ✅ (`TextureAudit`), in v1 nicht erneut gestartet |
| Bewegliche Teile | BER-basiert (Original-Modelle mechanisch nach `ModelPart` konvertiert, `tools/convert_models.py`), animiert nach Original-Winkeln; Sichtprüfung per Screenshots in früheren Sitzungen (`docs/07` T5*) |
| Stil | **Originale** im eingebauten Ressourcenpaket „Classic Textures“ je Mod; Standard = gefilterte Originale (`tools/modernize_textures.py`). Seit v1 behalten **alle Items ihre Original-Form** (C1). Blöcke/Gehäuse: Tonkurve + Rahmen/Abschrägung + verbundene Texturen (Kalibrierung an GregTech – offene Entscheidung G-2) |
| GUI | unverändert Original (Vanilla 1.21 hat denselben GUI-Stil) |

---

## C. Durchgeführte Änderungen (v1: C1–C5, v2: C6–C11)

### C1 – Item-Texturen: Original-Form statt GregTech-Vorlagen
- **Dateien:** `port/tools/modernize_textures.py`, `port/tools/modernized_textures.sha1`, 172 PNGs unter `port/*/src/main/resources/assets/*/textures/item/`, `docs/08-implementation-status.md` (§12 Hinweis), `port/.gitignore` (`__pycache__/`).
- **Was:** Der Modernisierer zeichnete 189 Material-Items aus generischen GregTech-CEu-Vorlagen neu. Standard ist jetzt: jedes Item behält seine Original-Silhouette und bekommt nur den milden Item-Filter (Licht oben links, Kontur in Eigenfarbe, etwas Sättigung). GregTech-Vorlagen nur noch per `--gt-items`. Ausnahme `wood_shaft`, `shield`: deren Original ist der Platzhalter (violette Ecken), sie behalten den Ersatz.
- **Warum:** Leitlinie „Originaldesign vor Neugestaltung“, „keine generischen Designs“, „einheitlicher Stil der Mod-Familie“. Die Vorlagen ersetzten markante Formen: Getriebeeinheiten (zwei Zahnräder → ein Zahnrad), Wellenkerne, Spulen, Schneckenrad (→ Zahnrad!), Turbine/Laufrad, ReactorCraft-Kanister (→ GT-Fluidzelle), Erzlösungen/-flocken, Linse, Spiegel, Hefe.
- **Auswirkung:** Rein optisch; Originale im Classic-Paket unverändert. 113 weitere Item-PNGs wurden neu berechnet, aber pixelgleich → alte Bytes behalten (minimaler Diff). Sieben Items (drill, bedrockdrill, saw, spring, springtungsten, silumin, magnetite) waren schon von Hand zurückgestellt und bleiben pixelgleich.
- **Test:** Pipeline vorher in einer Kopie reproduziert (2866 von 2873 PNGs pixelgleich, Rest = diese 7 Handkorrekturen). Vergleichsbögen Classic | alt | neu für alle 172 geänderten Items angesehen. Manifest `modernized_textures.sha1` stimmt mit allen 1131 Dateien überein. Nicht im Spiel geprüft (kein Client in dieser Umgebung).

### C2 – Deko-Blöcke von RotaryCraft (BlockDeco) portiert
- **Dateien (neu):** `port/rotarycraft/src/main/java/reika/rotarycraft/content/block/FlammableBlock.java`; je Block Blockstate, Block-/Item-Modell, Loot Table; 12 Rezepte `data/rotarycraft/recipe/{steel,anthracite,lonsdaleite,bedrock_ingot,coke,shield}_block*.json`, `*_from_*_block.json`; Tags `data/c/tags/{block,item}/storage_blocks{,/steel,/bedrock}.json`.
- **Dateien (geändert):** `RotaryBlocks.java` (Registrierung), `RotaryCreativeTabs.java`, sechs `lang/*.json` (Originaltexte aus den 1.7.10-`.lang`; fehlende Übersetzungen fallen wie im Generator auf Englisch zurück), `data/minecraft/tags/block/{mineable/pickaxe,wither_immune,dragon_immune}.json`, 7 ReactorCraft-Rezepte (`neutron_absorber*`, `flywheel_core`, `turbine_flywheel*`, `neutron_absorber_pellets*`: Tag → `rotarycraft:steel_block`), Generatoren `tools/gen_rotarycraft_assets.py`, `tools/gen_rotarycraft_recipes.py`, `tools/gen_reactorcraft_assets.py`, JUnit `ResourceCompletenessTest` (kennt die neuen IDs).
- **Was:** `steel_block`, `anthracite_block`, `lonsdaleite_block`, `shield_block` (Bohrer-Schild), `bedrock_ingot_block`, `coke_block` mit Originalwerten: Material.iron (jede Spitzhacke, Metallklang), Härte 4, `setResistance(10)` → Explosionsfestigkeit 6; Bedrock-Barren-Block 6000 + Wither/Drache können ihn nicht zerstören; Anthrazit 30/4 und Koks 20/3 Brennbarkeit/Ausbreitung. Rezepte aus `RotaryRecipes`: 9 → Block (Werkbank, Stahlblock zusätzlich Worktable), Block → 9, Schildblock ×4 (` S `/`SOS`/` S `, S = HSLA-Stahl, O = Obsidian).
- **Warum:** ReactorCraft brauchte den Stahlblock in 7 Rezepten (Neutronenabsorber, Schwungradkern, Turbinen-Schwungrad, …) und als Strahlungsschild `STEEL` (90 % Neutronen/95 % Strahlung) bzw. `BEDINGOT` (97,5 %/100 %) über `c:storage_blocks/steel` bzw. `/bedrock`. Niemand stellte diese Tags bereit → **Rezepte nicht herstellbar, stärkste Abschirmungen nicht vorhanden**.
- **Auswirkung:** Neue Inhalte (6 Blöcke). ReactorCraft-Rezepte verlangen wie im Original genau den RotaryCraft-Stahlblock. `RadiationShield.STEEL` prüft weiter den Tag (auch fremde Stahlblöcke schirmen ab – G-3).
- **Abweichung:** `BlockDeco.onBlockExploded` setzte den Bedrock-Barren-Block nach einer Explosion wieder ein; nicht portiert, Widerstand 6000 hält jede Explosion bis Stärke ~1800 ab.
- **Test:** `tools/check_resources_v1.py` 0 Fehler; Rezeptgenerator erzeugt exakt die bisherigen 554 + 12 neue Dateien (sonst kein Diff). Java-Code **nicht kompiliert** (kein Build möglich) – Signaturen 1:1 nach bereits kompilierendem Projektcode (`registerSimpleBlock`, `registerSimpleBlockItem`, `simpleCodec`, `getFlammability` wie `PipingBlock`).

### C3 – Explosionsfestigkeit der RotaryCraft-Maschinen
- **Datei:** `RotaryBlocks.machineProperties()`.
- **Was:** 15 → 9 (`15·3/5`).
- **Warum:** 1.7.10 speicherte `setResistance(x)` als `3x`, Explosionen nutzten `/5` → effektiv `0,6·x`; 1.21 verwendet den Wert direkt (Eisenblock: 1.7.10 `setResistance(10)`, effektiv 6, in 1.21 ebenfalls 6). Der Port hatte den Rohwert übernommen → Maschinen ~66 % explosionsfester als im Original. Betrifft auch `NeutronEntity.onEnterBlock` (Schwellen `res >= 12`, `24 - res`), die mit dem Original-Wert rechnet.
- **Auswirkung:** Maschinen verhalten sich bei Explosionen wie im Original. ReactorCraft-Blöcke mit eigenem `.strength(…)` sind **nicht** betroffen (F-3).
- **Test:** keine GameTests mit Explosions-Erwartung an Maschinen gefunden; nicht kompiliert/ausgeführt.

### C4 – Spitzhacken-Tags
- **Dateien:** `port/rotarycraft/src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json`, `port/reactorcraft/…/mineable/pickaxe.json`, `tools/gen_reactorcraft_assets.py`.
- **Was:** 39 RotaryCraft-Maschinen + 6 Deko-Blöcke ergänzt (nicht: Welle/Getriebe/Rohre – eigene `getDestroyProgress`; Raps; Bedrock-Scheibe). ReactorCraft: `processor`, `synthesizer`, `isocentrifuge`, `fluid_extractor` (alle anderen `machine(...)`-Blöcke hatten den Tag schon).
- **Warum:** Material.iron-Maschinen wurden in 1.7.10 mit Spitzhacken im Werkzeugtempo abgebaut; ohne Tag braucht z. B. eine Eisenspitzhacke ~6 s statt ~1 s. Ernte-Regeln (`canHarvest`) unverändert.
- **Test:** statisch.

### C5 – Statischer Ressourcen-Prüfer `port/tools/check_resources_v1.py` (neu)
- Läuft ohne Gradle/Netz: JSON-Syntax, gültige Pfade, Blockstate→Modell→Parent→Textur, Item-Modelle ↔ Übersetzungen, Loot/Rezepte/Tags → existierende IDs/Fluide/Tags, `sounds.json`→`.ogg`, Animations-Streifen, Sprachdateien, Classic-Paket-Vollständigkeit, 1.21-Ordnernamen.
- Aufruf: `python3 tools/check_resources_v1.py [--verbose]` (in `port/`), Exit-Code 1 bei Fehlern. Stand v1: **0 ERROR, 1 WARN** (`models/item/gear_upgrade_gears.json` ohne eigenen Namen – ungenutztes Modell, E-4), 213 INFO (Blöcke mit Drops im Code statt Loot Table, `c:`-Tags aus NeoForge).

### C6 – Explosionsfestigkeit und Härte aller Blöcke nach Original (v2, `66f1bb2`)
- **Dateien:** neu `port/rotarycraft/src/main/java/reika/rotarycraft/lib/LegacyResistance.java`; geändert `RotaryBlocks.java`, `ReactorBlocks.java`, `ReactorFluids.java`, `ElectriBlocks.java`.
- **Was:** `LegacyResistance.of(x) = x·3/5` für jeden Block mit 1.7.10-`setResistance(x)` (Werte je Originalklasse per `javap` belegt: ReactorCraft-Erze 5, Fluorit 4, Materialblöcke/Maschinen/Kerne/Multiblöcke 10, Corium/Thoriumbrennstoff 500, Dampfleitung 1; ElectriCraft-Erze 5, Batterien/Pads/Maschinen 10, RF-Kabel 15, Kabel 2; RotaryCraft-Rohre 1). Werte aus `getExplosionResistance`-Overrides bleiben (Explosionsglas, Bedrock-Barren-Block: 6000); Dampf und Bedrock-Scheibe bleiben unzerstörbar. **Härte korrigiert:** Abfall- und Magnetrohr sind `BlockDuct` (1/1, Port hatte 2/10), ElectriCraft-Maschinen `NetworkBlock` (Härte 2, Port hatte über `RotaryBlocks.machineProperties()` 4).
- **Warum/Auswirkung:** wie C3; außerdem rechnet `NeutronEntity.onEnterBlock` (Schwellen 12/24) für Port-Blöcke wieder mit Originalwerten.
- **Test:** statisch; kein GameTest prüft feste Widerstände. Nicht kompiliert.

### C7 – ElectriCraft-Maschinen im Spitzhacken-Tag (v2, `66f1bb2`)
- `mineable/pickaxe` + `tools/gen_electricraft_assets.py`: generator, motor, transformer, meter, relay, resistor, precise_resistor, fuse. Beleg: `ElectriBlocks.getBlockMaterial` liefert Material.iron für alles außer Erzen. **Achtung Generator:** dessen `tag()` überschreibt die Datei je Aufruf – alle Einträge müssen in einem Aufruf stehen (so umgesetzt).

### C8 – Brennstoffe (v2, `af5ac36`)
- **Datei:** neu `port/rotarycraft/src/main/resources/data/neoforge/data_maps/item/furnace_fuels.json`.
- **Was/Warum:** Der Port hatte keine Brennstoffe. Original `FurnaceFuelRegistry.registerItemSimple(stack, f)` = `f·200` Ticks, Blöcke ×10 (Kohleblock/Kohle): Ethanol-Kristalle 1000, Koks 2400, Anthrazit 4800, Koksblock 24000, Anthrazitblock 48000, Sägemehl 50.
- **Test:** Prüfer kennt Data Maps (Negativtest mit falschem Schlüssel erkannt).

### C9 – Worktable in der Welt bauen (v2, `28dedad`)
- **Dateien:** neu `port/rotarycraft/src/main/java/reika/rotarycraft/content/WorktableBuilder.java` (Game-Bus-`@EventBusSubscriber`), neu `…/rotarycraft/gametest/WorktableBuildTests.java` (3 Tests mit `SimPlayer`).
- **Was:** Original `RotaryEventManager.buildWorktables`/`checkAndBuildWorktable`: Werkbank auf Stahlblock, ringsum 8 Ziegel; darunter Redstone-Block, ringsum 8 doppelte (glatte) Steinstufen. Setzen der Werkbank oder Rechtsklick darauf → Struktur und Werkbank werden Luft, Worktable unten in der Mitte, Ton `CRAFT`; der Rechtsklick wird verbraucht. Nur serverseitig. Werkbank = vanilla oder `c:player_workstations/crafting_tables` (statt Original-`BlockWorkbench`/Tinkers).
- **Test:** GameTests geschrieben, nicht ausgeführt; nicht kompiliert.

### C10 – Prüfer erweitert (v2, `af5ac36`, `4531006`)
- `check_resources_v1.py` prüft zusätzlich Data-Map-Schlüssel und alle im Java-Code genannten Texturen (`tex("…")`-Helfer der Renderer, literale `textures/…`-Pfade; Präfixe zusammengesetzter Namen). 103 Verweise, alle vorhanden; Negativtest (entfernte Textur) erkannt.

### C11 – Neutronenreflektor (ReactorCraft, v2, `6feb4d0`)
- **Dateien:** neu `…/reactorcraft/reactor/NeutronReflectorBlockEntity.java`, `…/reactorcraft/gametest/ReflectorTests.java`; geändert `ReactorBlocks.java`, `ReactorBlockEntities.java`, `ReactorCraft.java` (Kreativ-Tab); Blockstate, Modelle, Textur `block/reflector` (Original, über Classic-Paket + Modernisierer), 5 Sprachdateien, 2 Rezepte, `mineable/pickaxe`, `tools/gen_reactorcraft_assets.py`, `docs/08` (Zeile in §6).
- **Was:** Original `TileEntityNeutronReflector.onNeutron`: moderieren; 1/4 zurückwerfen (X/Z-Bewegung umgekehrt, `hurtMarked`), sonst 50 % Absorption; kein Ticken. Block wie `BlockReactorTile` (Härte 2, Widerstand 6), ohne Ausrichtung. Rezept `GGG/GSG/GGG` (Graphit um Stahlblock; Worktable, Werkbank nur mit `table_machines`).
- **Test:** `ReflectorTests.reflectorSendsBackAQuarter` (4000 Neutronen, 6-σ-Grenzen für 25 %/37,5 %); nicht ausgeführt.

### C12 – Plasma-Test zählt nur die eigenen Plasmakugeln (v3, `d6269ff`)
- **Datei:** `port/reactorcraft/src/main/java/reika/reactorcraft/gametest/FusionTests.java` (`injectorMakesPlasma`).
- **Was/Warum:** Der Test zählte alle Plasmakugeln in einem 12-Block-Würfel; Plasma eines benachbarten Fusionstests konnte hineinfliegen, das Ergebnis hing von der Platzierung der Tests ab (nach zwei neuen Tests einmal 12 statt höchstens 11). Jetzt nur Kugeln auf der Schusslinie des Injektors (gleiches x und gleiche Höhe); die Gesamtzahl steht in der Meldung. Kein Mod-Code geändert.

### C13 – Turbinen-Dynamometer (ReactorCraft, v3, `6b6d5c1`)
- **Dateien:** neu `…/reactorcraft/powergen/TurbineMeterBlockEntity.java`, `…/gametest/TurbineMeterTests.java`; geändert `ReactorBlocks`, `ReactorBlockEntities`, `ReactorCraft` (Kreativ-Tab); Blockstate, Modelle (`cube_bottom_top`), Texturen `turbinemeter_0..2` (Original `turbinemeter_#0..2`, über Classic-Paket + Modernisierer), 5 Sprachdateien, 2 Rezepte, `mineable/pickaxe`, `tools/gen_reactorcraft_assets.py`, `docs/08`.
- **Was:** Original `TileEntityTurbineMeter`: beim ersten Tick und alle 32 Welt-Ticks Suche senkrecht nach oben nach einem Turbinenkern (auch Hochdruckturbine); Komparator-Ausgang `15·ω/ω_max`, Nachbarn bei Änderung benachrichtigt. **Original-Eigenheit übernommen:** die Suche läuft bis zur Bauhöhe, jeder lichtundurchlässige Block über der obersten Turbine verwirft sie wieder (Turbinen hatten Opazität 0) – ein Dach über der Turbine macht das Messgerät blind, Glas nicht. Block wie `BlockReactorTile` (Härte 2, Widerstand 6), keine GUI, keine Ausrichtung. Rezept SrS/PGP/PCP. Deutsch fehlt im Original → Englisch.
- **Test:** `meterReadsTurbineSpeed` (fünfstufige Dampfturbine, Messgerät unter der letzten Stufe, Komparator gibt den Pegel weiter), `meterNeedsClearSkyAboveTurbine`; grün. Hinweis für Tests: die Plattform hat unter y = 1 keinen Boden – Komparatoren brauchen einen Block darunter.

### C14 – Dampfdiffusor (ReactorCraft, v3, `c619751`)
- **Dateien:** neu `…/powergen/SteamDiffuserBlockEntity.java`, `…/client/model/ModelDiffuser.java`, `…/gametest/DiffuserTests.java`; geändert `ReactorBlocks`, `ReactorBlockEntities`, `ReactorCraft` (Kreativ-Tab, Fluid-Capability), `client/ReactorClient` (Renderer, Layer, Item-Renderer), `SteamLineBlockEntity.connectsTo`; Assets wie üblich (Textur `machine/diffuser.png`), `tools/gen_reactorcraft_assets.py`, `docs/08`.
- **Was:** Original `TileEntitySteamDiffuser`: zieht aus der Dampfleitung, auf die er zeigt (Metadaten 0 W, 1 O, 2 N, 3 S), ¼ der Differenz +1 je Tick × Wirkungsgrad (Wasser 1, Ammoniak 2); wandelt je Tick `min(1 + Dampf/4, frei/RATIO)` Einheiten in je `RATIO·1000` mB Fluid „steam“ um (`RATIO = ⌈1,696/0,01272·0,6⌉ = 80`), Tank 2 500 000 mB, Entnahme nur von hinten (Standard-/Bedrockrohr, NeoForge-Fluid-Capability). **Abweichung:** Das Original suchte `FluidRegistry.getFluid("steam")` (nur Fremdmods wie Railcraft registrierten es); der Port nimmt das erste Fluid des Tags `c:steam` (z. B. Mekanism). Ohne solches Fluid tut der Diffusor nichts (wie im Original ohne Dampf-Mod). Produkt in `long` statt `int`-Überlauf. **Eigenheit übernommen:** Einheiten werden auch verbraucht, wenn ihr Dampf nicht mehr ganz in den Tank passt.
- **Modell:** Die Original-Quelltexte fehlen im Klon. `ModelDiffuser` wurde aus dem Bytecode dekompiliert (Vineflower 1.10.1 aus dem Gradle-Cache, siehe Abschnitt A), obfuskierte Namen zurückübersetzt (`func_78789_a` → `addBox`, `func_78793_a` → `setRotationPoint`, `func_78787_b` → `setTextureSize`, `field_78809_i` → `mirror`, `field_78090_t/u` → `textureWidth/Height`, `this.`-Präfixe entfernt, `render(te, 0.0625F)` → `render(te, f5)`) und dann mit `tools/convert_models.py` konvertiert (Kopfzeile/Import/Tabs wie bei den übrigen generierten Modellen nachgezogen).
- **Test:** `diffuserTurnsSteamIntoFluid` (8 Einheiten → 640 000 mB), `diffuserCountsAmmoniaDouble` (→ 1 280 000 mB), `diffuserFillsTankAndFeedsPipeBehind`; grün. Im Client angesehen (Modell, Textur, Verbindung von Dampfleitung und Rohr).

### C15 – Dampfleitung verbindet sich mit dem Hochdruckturbinen-Kern (v3, `e92bf6a`)
- **Datei:** `SteamLineBlockEntity.connectsTo`, Test `SteamTests.lineConnectsToHiPTurbineAndDiffuser`.
- **Was/Warum:** Original `canConnectToMachine` akzeptierte `BIGTURBINE`; der Port ließ ihn aus, die Leitung wurde neben dem Kern unverbunden gezeichnet. Die Dampfaufnahme der Turbine funktionierte schon – nur die Form ändert sich.

### C16 – Factory-Abdeckungstest optional (v4, `62169cf`, G-4)
- `FactoryCoverage.everyBlockIsInTheFactory` ist `required = false`, bis alle Zonen stehen (dann wieder `true`). Die Fehlermeldung nennt nur noch die Anzahl (Liste in der Logzeile `FACTORY zones`): die lange Meldung sprengte das GameTest-Berichtsbuch (Lesepult-Exception beim Speichern).

### C17 – `magneticPipeNeedsCharge` wartet auf den Zustand (v4, `4d05ca9`)
- Prüfte genau bei Tick 3; einmal in ~15 Läufen hatte das Rohr bis dahin nicht getickt (vermutlich Chunk, der erst später tickte; nicht reproduziert). Jetzt `succeedWhen` innerhalb der 20 Ticks. Kein Mod-Code geändert.

### C18 – Erste Factory-Zonen (v4, `b4f0019`, `bdf863c`)
- **Paket** `reactorcraft/…/gametest/factory/`, Regeln aus `team/BRIEF.md` (nur Endlos-Kisten/-Fässer, Überlebensspieler `SimPlayer`, keine `setBlock`-Abkürzungen außer Vorräten und Boden), Batch `factory`.
- `ZonePowerTrain.powerTrain` (8 Blöcke): zwei DC-Motoren → Wellenverzweigung (zusammenführen) → Holzgetriebe 4:1 → Kupplung am Hebel → Kegelrad → Welle → Holz-Schwungrad → Dynamometer; Hebel aus: nichts kommt an; Hebel an: 64 rad/s @ 16 Nm (Holz-Schwungrad gibt höchstens seine 16 Nm Nennlast weiter, Original `Flywheels.WOOD`).
- `ZoneOres.oresAreMinedForTheirDrops` (30) und `ZoneStorage.storageBlocksDropThemselves` (10): Blöcke ohne Maschinenfunktion werden aufgestellt und mit Diamantspitzhacke abgebaut (BRIEF-Regel 8); Erze müssen etwas droppen (ElectriCraft-Erze sich selbst), Lagerblöcke genau sich selbst.
- Neue Helfer in `Factory`: `placeAndMine`, `takeExactly` (genau n Stück per Maus aus der Kiste – Shift-Klick nimmt ganze Stapel und füllt das Inventar).
- Stand: **48 von 199** Blöcken in Zonen. Fehlend u. a. alle ElectriCraft-Maschinen/-Kabel/-Batterien/-Pads, alle übrigen RotaryCraft-Maschinen (Motoren außer DC, Rohre, Verarbeitung, Hochofen …), Raps (Farm-Zone), ReactorCraft-Maschinen und Multiblöcke.

### C19 – Kalzitblock, Beton und Graphitblock droppen sich selbst (v4, `fd677f2`)
- **Port-Fehler, gefunden durch `ZoneStorage`:** Die ReactorCraft-Materialblöcke (Original `BlockReactorMat`, `damageDropped(meta) = meta` → droppt sich selbst) waren einfache Blöcke ohne Loot Table und droppten nichts. Loot Tables (`survives_explosion`) ergänzt, ebenso in `tools/gen_reactorcraft_assets.py`.

### C20 – Factory-Zonen ElectriCraft, Fluide, Werkstatt (v5, `d6b2812`, `1d6df27`, `3ef30ec`)
- `ZoneElectric` (30): alle 16 Kabel + Messgerät in Reihe (286 Ω → Motor 28 rad/s @ 16 Nm, 2 A); Relais am Hebel, Widerstand und Präzisionswiderstand per brauner Farbe auf das richtige Band geklickt (1 A), 128-A-Sicherung → 40 rad/s @ 8 Nm; Transformator 1:1 → 48 rad/s @ 16 Nm; alle sechs Batteriearten laden (das Netz teilt den Strom in ganzen Ampere, daher Holzgetriebe 16:1 → 8 A). **Offen:** RF-Batterie, RF-Kabel, Funkpads (brauchen eine vom Spieler gebaute FE-Quelle, z. B. ReactorCraft-Generator).
- `ZoneFuel` (10): Ethanol → Treibstoffleitungen → ECU → Gasmotor 512 @ 128; Kerosin → Mikroturbine; Dampfmaschine über selbst entzündetem Netherrack mit Wasser per Rohr (512 @ 32); Schlauch schmiert ein Stahlgetriebe; selbst gesetztes Reservoir per Eimer gefüllt, Ventil am Hebel, Bedrockrohr, Rohr.
- `ZoneWorkshop` (4): Worktable-Struktur Block für Block gebaut, Werkbank darauf → Worktable; zwei Gasmotoren an einer Treibstoffleitung: Reibungsheizer → Hochofen (per GUI beladen) macht 3 HSLA-Stahl, Grinder macht Kies.
- Erfahrungen für weitere Zonen: Maschinen, die an einen anderen Maschinenblock grenzen, mit `placeSneaking` setzen (sonst nimmt die Maschine den Klick); Vorräte genau abzählen mit `Factory.takeExactly`; die Plattform hat unter y = 1 keinen Boden (`SimPlayer.floor` legt ihn auf y = 1).

### C21 – `grinderMakesLubricant` zählt das Schmiermittel im Schlauch mit (v5, `bdf4076`)
- Zwei niedrige Zufallswürfe (64 + 64) ergaben 126 mB in Grinder + Getriebe; der Schlauch dazwischen hält einen Rest. Kein Mod-Code geändert.

### C22 – Factory-Zonen kleine Maschinen und Sonderrohre (v6, `db9b844`, `70ac64d`)
- `ZoneSmallMachines` (4): DC-Motor → Holzgetriebe 2:1 mit Kühlrippe obendrauf → Wickler (Feder per GUI, auf 8 kJ gewickelt; ein trockenes Stahlgetriebe verschleißt und verliert Drehmoment – daher Holz); Gasmotor an Treibstoffleitung → Fermenter (Wassereimer vom Fass, Zucker + Erde per GUI) macht Hefe; Trockenbett mit Wassereimer → 4 Salz.
- `ZonePipeworks` (4): Wasser → Separator → Rohr darunter → Rohrpumpe (von einer Mikroturbine auf einer im Boden verlegten Kerosinleitung angetrieben) → Rohr; zwei Bypässe (der zweite gegen den ersten gesetzt) verbinden sich; Saugrohr verbindet sich mit einem Rohr, nimmt aber nichts.
- **Noch ohne Zone (99):** RotaryCraft: ac_engine, bedrock_breaker, canola, centrifuge, compactor, extractor, filling_station, fractionator, fuel_enhancer, heat_ray, hydro_engine, jet_engine, pulse_jet_furnace, pump, refrigerator, solar_mirror, solar_tower, sport_engine, van_de_graff, wind_engine; ElectriCraft: rf_battery, rf_cable, fünf Funkpads (FE-Quelle nötig); alle übrigen ReactorCraft-Blöcke (Spaltung, Brüter, HTGR, Thorium, Dampf/Turbinen/Generator/Schwungrad, Abfall, Verarbeitung, Solar, Fusion mit allen Multiblockteilen, auch Dynamometer und Dampfdiffusor). Vorschlag: zuerst eine Dampf-Zone (Kessel → Dampfleitung → Turbine → ReactorCraft-Generator, dabei FE für RF-Batterie/-Kabel/Funkpads und Turbinen-Dynamometer/Dampfdiffusor), dann Spaltung, dann Fusion.

### C23 – Factory-Zonen Fusion, Spaltung, Prozessor, Abfall (v7)
- `ZoneFusion` (28): Solenoid, Wasserstoff-Vorheizer und Injektorrahmen Block für Block aus Endlos-Kisten gebaut (`Factory.buildFromChests` – je Blockart ein Kistenplatz, genaue Stückzahl per Maus), jedes bildet sich; Toroidmagnet per Schraubendreher nach Westen; Magnetrohre verbinden sich; Gaskollektor an einem befeuerten Ofen sammelt CO2; Fusionsmarkierer, Neutronenabsorber.
- `ZoneFission` (7): 3×3 Brennstoffkerne per GUI befüllt, Kessel (Dampfleitung darauf) und Kühlzelle per Wassereimer, Reflektor, Steuerstab + CPU; der mittlere Kern wird aktiv und heiß, die CPU ohne Wellenleistung löst SCRAM aus (Komparator 15).
- `ZoneProcessing` (1): Uranprozessor mit Wasserrohr von einem Fass, per GUI beladen → 1000 mB UF6.
- `ZoneWaste` (4): Abfallbehälter (Zerfallswärme), Lagerfass (hält 16 Cs-137), Zerfallsbeschleuniger (beladen), zwei Abfallrohre. Neuer Helfer `Factory.putExactly` (exakter Stapel mit Komponenten).

### C24 – Abfall-Lagerfass per Hand auffüllbar (v7, `780c490`, Port-Fehler)
- `WasteStorageBlockEntity.isValidSlot` (Original `isAppropriateWasteSlot`) verlangte, dass der ganze Stapel noch passt. Das Original nutzte die Regel nur für Automatisierung (1 Item je Vorgang), seine GUI-Slots nahmen frei an; im Port prüft `SlotItemHandler` dieselbe Regel → ein Spieler mit mehr Abfall in der Hand konnte einen Slot nicht auffüllen (Rechtsklick legte 1 hinein, der nächste nahm es wieder heraus). Jetzt wird Platz für 1 Item geprüft; das Slot-Limit 16 begrenzt weiter.

### C25 – `rodAbsorbsOnlyWhenInserted` mit 6-σ-Grenzen (v7, `eb62217`)
- Binomial(1000; 0,6), alte Grenzen 540..660 (3,9 σ) schlugen einmal mit 662 fehl; jetzt 508..692. Kein Mod-Code geändert.

### C26 – Factory-Zonen Solar und Motoren/Raps (v8, `cc508c6`, `f726310`)
- `ZoneSolar` (3): Mittag (Tageszeit gesetzt wie in den Solartests), Solarturm 3 hoch, Natrium-Empfänger obendrauf, 8 Spiegel **zusammenhängend** angeschlossen (eine Anlage nimmt nur Spiegel, die den Turm oder einander berühren), Wasser per Rohr, Kegelrad + Dynamometer in den Boden gegraben → 512 rad/s; der Empfänger heizt sich auf.
- `ZoneEngines` (4): AC-Motor (magnetisierter Stahl-Wellenkern aus der Kiste – der Magnetisierer ist noch nicht portiert – plus Redstone-Upgrade) 256 @ 512; Hochleistungsmotor (Ethanol per Leitung, Wasser hinten, Redstone als Additiv) 1024 @ 256; Wasserkraftmotor neben einem selbst gegossenen Wasserfall (Eimer zielen: Eimer wirken über die Blickrichtung, `useItem`; Block über/unter dem Motor blockiert die Schaufeln) läuft; Raps auf Ackerboden, der per kurz gegossenem und wieder aufgenommenem Wasser feucht wird (Zufallstick ausgelöst statt ~1 min Warten); Knochenmehl wirkt nicht (Original lehnte BonemealEvent ab).
- **Noch offen (53):** Dampf-/Turbinen-/Generator-/Schwungradfamilie samt Turbinen-Dynamometer und Dampfdiffusor (braucht eine echte Dampfquelle: Spaltung heizt zu langsam; Lava am Wärmetauscher ist einmalig; sinnvoll wäre Solarempfänger → Natrium → Solartauscher → Wärmetauscher → Kessel), RF-Teile/Funkpads (brauchen FE vom ReactorCraft-Generator), Brüter/Thorium/Kugelhaufen/CO2-/Natrium-Heizer/Tritizer/Brennstoffablass/Thoriumbrennstoff, Elektrolyseur, Fluidextraktor, Isotopenzentrifuge, Synthesizer, Solartauscher, sowie die übrigen RotaryCraft-Maschinen (in Arbeit, siehe nächste Version).

### C27 – `neutronActivatesFluorite` mit drei Neutronen (v8, `f129612`)
- Ein einzelnes Neutron wurde gelegentlich von der Luft geschluckt (1:1000 je Block) oder übersprang den Block zwischen zwei Ticks (wie im Original). Drei Neutronen; Prüfung unverändert. Kein Mod-Code geändert.

### C28 – Factory-Zonen für die übrigen RotaryCraft-Maschinen (v9, Subagent in eigenem Worktree, vom Hauptstrang geprüft und übernommen)
- `ZoneDistillery` (Zentrifuge 4096 rad/s: Rapshülsen → Schmiermittel; Fraktionierer 8192 rad/s: Zutaten + Ghast-Träne → Kerosin; Gasmotoren an Ethanol-Leitungen, Bedrock-Getriebe per Schraubendreher auf Übersetzung ins Schnelle, Kegelräder unter den Maschinen).
- `ZoneTurbines` (vier Mikroturbinen auf in den Boden gegrabenen Kerosinleitungen: Hitzestrahl schmilzt Sand zu Glas, Kompaktor presst Eis zu Packeis, Pulsstrahlofen macht aus Obsidian Explosionsglas bei 850–1000 °C, Brennstoffverbesserer macht LiFBe-Brennstoff aus LiFBe + Thoriumstaub per Trichter).
- `ZoneUtilities` (Tankstelle füllt ein 2×-Getriebe-Upgrade mit Schmiermittel, das in einen Gasmotor gesetzt wird; Kühlschrank macht Flüssigstickstoff; Pumpe über einem selbst gebauten, per Eimer gefüllten Becken; Windmotor 1024 rad/s @ 8 Nm treibt den Van-de-Graaff-Generator, der sich in einen Eisenblock entlädt).
- `ZoneJetWorks` (zwei Gasturbinen, durch eine Bruchsteinwand im Ansaugtrakt auf 16384 rad/s gedrosselt: Bedrockbrecher mahlt Bedrock zu Staub in eine Kiste; Extraktor macht aus Eisenerz in vier Stufen Flocken, die im Ofen zu Eisen schmelzen).
- Kein Mod-Code geändert. **Hinweis für Subagents:** Das Worktree-Werkzeug startet auf einem alten Commit (`main`/„init“) – im Auftrag ausdrücklich verlangen, den Worktree-Branch zuerst auf `claude/stoic-volta-ysznnm` zu setzen; für Sichtprüfungen den Client auf dem GPU-Display (`~/xgpu/start-gpu-display.sh`, `DISPLAY=:1`) oder Xvfb nutzen.
- **Noch offen (39):** Dampf-/Turbinen-/Generator-/Schwungradfamilie samt Turbinen-Dynamometer und Dampfdiffusor, RF-Batterie/-Kabel und fünf Funkpads, Brüter, Thorium (Kern + Brennstoff), Kugelhaufen, CO2- und Natrium-Heizer, Tritizer, Brennstoffablass, Elektrolyseur, Fluidextraktor, Isotopenzentrifuge, Synthesizer, Solartauscher.

### C29 – Letzte Factory-Zonen, Abdeckung vollständig (v10)
- `ZoneChemistry` (Elektrolyseur am Van-de-Graaff-Generator: schweres Wasser → Deuterium, Salz über 801 °C per Reibungsheizer → Natrium/Chlor; Synthesizer macht Ammoniak per Reibungswärme; Isotopenzentrifuge bekommt UF6 per Rohr vom Prozessor; Fluidextraktor sammelt Lithium aus selbst gegossener Lava – schweres Wasser bräuchte ein Ozeanbiom).
- `ZoneSteamPower`: **echter Dampf** über Solarempfänger (73 Spiegel) → Natrium → Solartauscher → Wärmetauscher → Kessel → Dampfleitungen → Gitter → fünfstufige Turbine → Generator-Multiblock (RF) → RF-Kabel, zwei RF-Batterien, fünf Funkpads; Kondensator über dem Dampf-Bypass; Turbinen-Dynamometer findet die Turbine; zweiter Test: Schwungradring mit Dynamometer, Dampfdiffusor (nur Aufbau geprüft: der Kessel macht ~1 Dampfeinheit je Tick, wer zuerst zieht, hängt von der Tick-Reihenfolge ab), zweistufige Hochdruckturbine bildet sich und wird geschmiert (dreht nicht: zählt Solardampf nur mit 0,15).
- `ZoneHTGR` (29 Kugelhaufen-Betten heizen CO2; Thoriumkern auf Brennstoffablass, mit LiFBe per Treibstoffleitung gefüllt, von Uran-Kernen gezündet: aktiv, > 1100 °C, Ablass gibt Thoriumbrennstoff ab), `ZoneBreeder` (Brüterkerne, Natrium-Heizersäule, Tritizer macht schweres Wasser; der Natrium-Heizer erreicht die 301 °C für heißes Natrium nicht, ein größerer Brüter wäre schmelzgefährdet).
- `FactoryCoverage` wieder `required` (G-4 erfüllt): **199 von 199**.

### C30 – Port-Fehler, gefunden durch die Zonen (v10)
- `electricraft/network/RFNetwork.java`: das RF-Netz verwarf Endpunkte ohne Block-Entity; die Funkpads haben im Port keine (Original: `TileEntityWirelessCharger`, ein `IEnergyHandler`) → Pads bekamen nie Energie. Endpunkte ohne Block-Entity bleiben gültig, solange eine Seite Energie anbietet.
- `rotarycraft/content/piping/FuelLineBlockEntity.java`: die Original-Treibstoffleitung nahm auch „rc lifbe fuel“ und „rc lifbe fuel preheat“ (per `javap` belegt); im Port fehlte das → kein Thoriumkern war befüllbar.
- `reactorcraft/reactor/FuelDumpBlockEntity.java` + Capability in `ReactorCraft.java`: der Original-Brennstoffablass ist ein `TileEntityTankedReactorMachine` (Fluid-Handler, gibt Entnahmen an den Kern darüber weiter); im Port verbanden sich Rohre nie.
- Testseitig robuster gemacht: `ZoneSteamPower` (Diffusor), dazu früher C25/C27.

### C31 – Druckhalter, Gasleitung, Wärmerohr (v11, Subagents, geprüft und übernommen)
- **Druckhalter** (`powergen/ReactorPumpBlockEntity`, Modell `ModelReactorPump`): siehe docs/08; Tests `PressurizerTests`, Zone `ZonePressurizer`.
- **Gemeinsame Rohrbasis** `content/piping/ReactorPipingBlockEntity` + `content/block/ReactorPipeBlock` aus Abfall- und Magnetrohr herausgelöst (Verhalten unverändert, alle Alt-Tests grün); die Basis ist wie im Original `PumpablePipe`.
- **Gasleitung** auf dieser Basis; **Wärmerohr** entgegen F-5-Hinweis keine Reaktorleitung, sondern TileEntityLine; dafür neu `rotarycraft/content/blockentity/HeatConduction` (Original-Schnittstelle), umgesetzt von `TemperatureMachine`, `Temperatured` und einzelnen Maschinen mit Original-Werten. Zone `ZoneReactorPipes`.
- Abweichungen: Reaktortypen vom Wärmerohr nur an Reaktorkessel; Wärmekapazität aus dem 1.21-Blockvolumen; ChromatiCraft-Risse nicht portiert.

### C32 – Platzierungsabhängige Tests (v11)
- Fermenter-Rig hält 25 °C (Umgebungstemperatur folgt dem Biom unter dem Test); Wärmerohr-Starttest mit 1 Grad Toleranz; `ZoneReactorPipes` erlaubt 25–27 Abfallitems (Zufallszerfall). Kein Mod-Code geändert.

---

### C33 – F-9 begonnen: Ventilator, Bohrer, Mob-Ernter, Aggregator, Flüssigkeits-Verteiler, Block-Füller, Rauchmelder, Spieler-Detektor, Mob-Radar, Gesteinsschmelzer, Heizer, Komposter, Kristallisator, Lampe (v12)

- **Ventilator (FAN)** portiert (`FanBlockEntity`, `FanBlock`, `ModelFan`, `CropHelper`), Konstanten gegen das Original geprüft (`javap`); Details in `docs/08`. Zone `ZoneFarming`, 4 GameTests; Stand: 301 Pflichttests grün.
- **Testfalle:** In `platform_9x4x9` liegt der Steinboden für den GameTestHelper auf y = 1. Wer Maschinen auf y = 1 setzt, ersetzt nur einen Bodenblock – ringsum bleibt Stein. Tests mit Strahlen/Reichweite müssen die Ebene y = 1 erst leeren (siehe `FanTests.floor`).
- **Bohrer (BORER)** portiert aus dem Bergbau-WIP `48875fc` (`BorerBlockEntity`, `BorerBlock`, `MiningPipeBlock`, `BorerMenu`, `BorerScreen`); Details und nicht portierte Teile in `docs/08`. `BorerTests` (3) und Zone `ZoneMining` (Spieler wählt das Feld im GUI); Stand: 305 Pflichttests grün. Die erste Scheibe wird immer ganz geschnitten (7 × 5) – Tests brauchen `platform_9x8x9`. `ModelPileDriver` und `ModelSonicBorer` sind schon umgesetzt, die Maschinen dazu **nicht**.
- **Mob-Ernter (MOBHARVESTER)** portiert (`MobHarvesterBlockEntity`, `MobHarvesterRenderer`, `ModelHarvester`); Details und nicht portierte Teile in `docs/08`. `MobHarvesterTests` (4) und Zone `ZoneMobFarm`; 310 Pflichttests grün. Neu `SimPlayer.join()`: trägt den Testspieler in die Spielerliste ein, weil der Ernter im Namen seines Aufstellers trifft und ihn nur online findet (`leave()` nimmt ihn wieder heraus).
- **Aggregator (AGGREGATOR)** portiert (`AggregatorBlockEntity`, `AggregatorRenderer`, `ModelAggregator`, neu `lib/world/BiomeHumidity`); Details in `docs/08`. `AggregatorTests` (5) und Zone `ZoneCondenser`; 316 Pflichttests grün. Werkzeug-Hinweis: `mineable/pickaxe.json` von RotaryCraft wird von keinem Generator geschrieben, neue Maschinen dort von Hand eintragen.
- **Flüssigkeits-Verteiler (SPILLER)** portiert (`FlooderBlockEntity` auf der neuen Basis `AreaFillerBlockEntity`, die auch der Block Filler braucht; Config `spillerRange`); Details in `docs/08`. `FlooderTests` (4) und Zone `ZoneFlooding`; 321 Pflichttests grün. **Bekannter Wackler:** `heatPipeDrainsReactorPart` (ReactorPipeTests) fiel in einem von drei Läufen aus (Rohr hielt 188526 J statt 2,176e7 J); der Test vergleicht gegen eine einmal gemerkte Umgebungstemperatur, `AmbientTemperature` hängt aber von Sonnenstand und Regen ab – Ursache vermutet, nicht belegt; noch nicht behoben.
- **Block-Füller (FILLER)** portiert (`BlockFillerBlockEntity` auf `AreaFillerBlockEntity`; GUI über das vorhandene `MachineMenu`-Layout `ONE_SLOT`, das jetzt außer Motoren auch den Füller bedient); Details und Material-Ersatz (Klang/Tag statt 1.7.10-`Material`) in `docs/08`. Beim Testen bestätigt: Flüssigkeiten werden wie im Original nur überschrieben, wenn sie an den Block direkt unter der Maschine anschließen. `BlockFillerTests` (4) und Zone `ZoneFilling`; 326 Pflichttests grün.
- **Rauchmelder (SMOKEDETECTOR)** portiert (2026-11-11), dazu die Basis `SpringPoweredBlockEntity` (Original TileEntitySpringPowered) für alle federgetriebenen Maschinen; `MachineMenu`-Layout `ONE_SLOT` bedient jetzt auch diese. Abweichung: Feuer über den Tag `minecraft:fire` statt nur `Blocks.fire` (Seelenfeuer). Fabrikzone `ZoneAlarm`. 330 Pflichttests grün.
- **Spieler-Detektor (PLAYERDETECTOR)** portiert (2026-11-11) mit Reichweiten-GUI (`rangegui`; Eingabe als Menü-Button-Klick, Muster wie beim Bohrer). Abweichungen: keine Zuschauer, Nachbar-Updates nur bei Signalwechsel. Der Modell-Konverter stolpert über `(float) (Math.PI / 12)` in `setRotation` – vor dem Konvertieren im vorbereiteten Modell durch Zahlen ersetzen. Fabrikzone `ZoneSecurity`. 333 Pflichttests grün.
- **Mob-Radar (MOBRADAR)** portiert (2026-11-11) mit Karten-GUI (`mobradargui`, Symbole über eine Tabelle der 1.7.10-Entity-IDs) und der API-Schnittstelle `RadarJammer`. Offen: Motion-Tracker-Linien (Werkzeug fehlt). Der Konverter-Stolperstein `(float) (…)` wird jetzt von `fixrot.py` im Scratchpad vor dem Konvertieren behoben (gehört bei Gelegenheit in `tools/convert_models.py`). Fabrikzone `ZoneRadar`. 337 Pflichttests grün. **Zweiter Wackler:** `ductsCarryGasAndHeat` („container holds 24 waste items“) fiel einmal aus, im Wiederholungslauf grün.
- **Gesteinsschmelzer (LAVAMAKER)** portiert (2026-11-11): Temperatur-/Energie-Logik und Rezepte wie im Original (`LavaMakerRecipes`; Erzwörterbuch „stone"/„cobblestone" → Tags `c:stones`/`c:cobblestones`). Im Original schmilzt der Block `Blocks.snow` (voller Schneeblock), nicht die Schneeschicht. Offen: Trockeneis → CO2 (es gibt keine CO2-Flüssigkeit), Fremd-Mod-Rezepte. Praktisch erreichbar sind bei 65536 W etwa 1050 °C (Gleichgewicht 16 = ΔT/64), Netherrack (600 °C) nach rund 53 s. Fabrikzone `ZoneMelting` (Netherrack per GUI, Lava ins Rohr), 341 Pflichttests grün.
- **Heizer (HEATER)** portiert (2026-11-11): Logik wie im Original; die Solltemperatur kommt als Menü-Button-Klick (0–9999) statt als eigenes Paket. Das Original lässt beim Lavaeimer, dessen leerer Eimer nicht mehr ins Inventar passt, einen *Lava*eimer fallen (offensichtliches Versehen) – der Port lässt den leeren Eimer fallen. Beim Überhitzen fällt der Brennstoff wie im Original mit dem Block in die Explosion (geht meist verloren). Wärmeübergabe nur an `FrictionHeatable` (Original ThermalMachine), bisher also Hochofen & Co. Fabrikzone `ZoneHeating`, 347 Pflichttests grün.
- **Komposter (COMPOSTER)** portiert (2026-11-11): `ComposterBlockEntity` nach `TileEntityComposter` (MINTEMP 40, KILLTEMP 70, MAXTEMP 100, Timer 100). CompostMatter: Unterarten der 1.7.10-Blöcke sind 1.21-Tags (`saplings`, `small_flowers`, `leaves`, `tall_flowers` + hohes Gras/großer Farn; „tallgrass“ = kurzes Gras, Farn, toter Busch), Fisch = alle vier rohen und beide gebratenen Fische. Temperatur und Garzeit gehen per `DataSlot` an die GUI. Nicht portiert: Errungenschaft MAKEYEAST, Wärmequellen anderer Mods (`EnvironmentalHeatSource`), Zinn-Variante des Rezepts (Stahl-Rezept vorhanden). Hinweis: Die Umgebungstemperatur folgt dem Sonnenstand, der Test lässt deshalb 1 °C Abweichung zu.
- **Kristallisator (CRYSTALLIZER)** portiert (2026-11-12): `CrystallizerBlockEntity` nach `TileEntityCrystallizer` und `RecipesCrystallizer` (Rezept mit der größten Menge, die der Tank hält, wie `FluidHashMap.getForValue`). Der Gefrierpunkt nutzt die Temperatur des `FluidType` (Wasser 300 K, Lava 1300 K, Ethanol 340 K wie im Original). Nimmt Flüssigkeiten von allen Seiten aus Rohren/Capabilities. Nicht portiert: Ventilatorgeräusch, Cryotheum (Thermal Foundation), Flüssigkeiten anderer Mods (Ender, Redstone, Glowstone, Kohle, Tinkers-Metalle). Die Fabrikzone nutzt die große Plattform, weil das Windrad etwa 32 freie Blöcke in Windrichtung für volle 1024 rad/s braucht.
- **Lampe (LAMP)** portiert (2026-11-12): `LampBlockEntity` nach `TileEntityLamp` auf `SpringPoweredBlockEntity` (GUI über das vorhandene Ein-Slot-Layout). Der Lichtblock des Originals (`BlockRegistry.LIGHT`) ist der Vanilla-Block `minecraft:light` (Stufe 15). Abweichungen: vorhandene Lichtblöcke zählen beim Sammeln als frei (sonst blieben Lichter nach einem Neuladen stehen), und beim Abbauen entfernt die Lampe nur ihre Lichtblöcke (das Original setzte jeden gesammelten Punkt auf Luft, auch wenn dort inzwischen gebaut wurde).
- **Angefangen, nicht fertig (lokaler WIP-Commit im Worktree-Zweig, ungebaut/ungetestet, nicht gepusht):** Getriebe-Gruppe `3b07b33` (`worktree-agent-a930a79064c41f1f3`). Geht verloren, wenn der VM-Zweig gelöscht wird.
- Nutzerwunsch 2026-11-10: sparsam mit Tokens, keine parallelen Subagents mehr; eine Maschine nach der anderen.

## D. Bekannte Probleme und Risiken

1. ~~Kein Build, keine Tests in v1 und v2~~ – erledigt in v3: alles kompiliert, `WorktableBuildTests`, `ReflectorTests`, `PickBlockTests` (mit den Deko-Blöcken) und `ResourceCompletenessTest` grün.
2. ~~`FactoryCoverage.everyBlockIsInTheFactory` schlägt fehl~~ – erledigt in v10 (alle Blöcke in Zonen, Test wieder Pflicht). Historie: Der Test verlangt, dass jeder Block in einer „Zone“-Klasse (`reactorcraft/…/gametest/factory/Zone*.java`) gebaut wird, es gibt aber noch keine Zone (Projekt aus `team/BRIEF.md`, unfertig). War schon vor v1 so; die 6 neuen Blöcke kommen zur Liste hinzu. Der Batch läuft im normalen `runGameTestServer` mit (F-2).
3. ~~Explosionsfestigkeit anderer Blöcke~~ – erledigt in v2 (C6).
4. ~~ElectriCraft-Maschinen ohne Spitzhacken-Tag~~ – erledigt in v2 (C7).
5. ~~In-World-Bau der Worktable fehlt~~ – umgesetzt in v2 (C9), ungetestet.
6. ~~Brennwerte~~ – erledigt in v2 (C8).
7. **RadiationShield STEEL/BEDINGOT per Tag**: akzeptiert auch fremde Blöcke in `c:storage_blocks/steel` (Original: nur HSLA-Block) – G-3.
8. **Generatoren nicht lauffähig im Klon:** `gen_*_assets.py` brauchen die Original-Quelltexte (`../RotaryCraft` usw., leere Submodule) und eine entpackte JAR. v1 hat Generator-Code und erzeugte Dateien parallel gepflegt; nach einem echten Generatorlauf ist zu prüfen, dass die Ausgabe identisch bleibt.
9. **Test-/Dev-Code im Haupt-Source-Set:** `*/gametest/`, `reactorcraft/dev/`, Showcases und `SimPlayer` landen in den Mod-JARs (F-12).
10. **Visuell nicht im Spiel geprüft in v1:** C1 nur über Bildvergleich der PNGs; Deko-Blöcke nicht im Client gesehen. (v3: Client läuft in der VM auf der GT 720, Dampfdiffusor und Dynamometer angesehen; Deko-Blöcke und Item-Texturen weiterhin nicht.)
11. **Jeder GameTest-Lauf verändert die eingecheckte Testwelt** `port/reactorcraft/run-server/` (Regionen, `level.dat`, neue `.mca`). Vor jedem Commit zurücksetzen: `git checkout -- port/reactorcraft/run-server && git clean -fdq port/reactorcraft/run-server` (siehe E-1, G-6).
12. ~~Repository ist öffentlich~~ – vom Nutzer geklärt (G-9).

---

## E. Überflüssige oder veraltete Dateien (nicht gelöscht – Kandidaten für spätere Bereinigung)

1. `port/reactorcraft/run-server/` (~170 MB: Welt, Logs, Configs) ist eingecheckt; `port/.gitignore` deckt nur `*/run/` ab. Kandidat: aus Git entfernen und `*/run-server/` ignorieren. Die Logs sind derzeit der einzige Beleg des letzten Testlaufs (vorher sichern).
2. `team/{electric,fission,fusion,power,processing}/` – fünf identische Kopien von `port/` (je 43 MB, `diff -rq` ohne Unterschied), Arbeitsbereiche des Team-Experiments aus `team/BRIEF.md`. Kandidat: entfernen, `team/BRIEF.md` ggf. nach `docs/` verschieben.
3. Leere Submodul-Einträge `DragonAPI/`, `ElectriCraft/`, `ReactorCraft/`, `RotaryCraft/` ohne `.gitmodules`. Kandidat: `.gitmodules` mit den Original-Repositories ergänzen oder die Einträge entfernen.
4. `port/rotarycraft/src/main/resources/assets/rotarycraft/models/item/gear_upgrade_gears.json` – von keinem Modell referenziert (die Getriebe-Upgrade-Modelle nutzen nur die gleichnamige Textur).
5. Texturen nicht portierter Original-Maschinen (z. B. `textures/block/borer_*`, `bridge_*`, `gpr`, `sorter_*`; viele Atlanten unter `textures/machine/`) – werden gebraucht, sobald die Maschinen portiert sind; nicht entfernen.
6. `port/build-log.txt` (leer), `port/junit-log.txt`, `port/gametest-log.txt` (Stand Oktober) – veraltete Logs.
7. `.agents/skills/`, `skills-lock.json` – allgemeine Java/Spring-Agenten-Skills ohne Bezug zum Mod-Code.
8. `start.md` – ursprünglicher Auftrag (Phase A/B), inhaltlich durch `docs/` und diese Datei überholt; als Historie behalten.
9. `port/tools/gt_templates/` – nach C1 nur noch für `--gt-items` und die zwei Platzhalter-Ersätze nötig.

---

## F. Nächste Schritte (priorisiert)

**F-1 · ✅ erledigt in v3** – ursprüngliche Aufgabe zur Nachvollziehbarkeit: **Build und Tests in einer Umgebung mit Netz**
- Dateien: keine Änderung nötig; ggf. Fehlerkorrektur in `RotaryBlocks.java`, `FlammableBlock.java`, `RotaryCreativeTabs.java`, `ResourceCompletenessTest.java`.
- Aufgabe: In `port/` `./gradlew build` und `./gradlew :reactorcraft:runGameTestServer` ausführen; Compilerfehler aus v1 beheben.
- Hinweise: In der Claude-Cloud-Umgebung die Domains `maven.neoforged.net`, `libraries.minecraft.net`, `piston-meta.mojang.com`, `piston-data.mojang.com`, `resources.download.minecraft.net` freigeben (Environment → Network access). Lokal: Java 21.
- Abhängigkeiten: keine.
- Akzeptanz: `BUILD SUCCESSFUL`; JUnit 11/11 grün; GameTest-Log „All N required tests passed“ oder ausschließlich `everyblockisinthefactory` fehlgeschlagen (D-2); `PickBlockTests` schließt die 6 neuen Blöcke ein; `python3 tools/check_resources_v1.py` Exit-Code 0.

**F-2 · ✅ erledigt in v10** – Factory-Zonen: alle 199 Blöcke abgedeckt, Abdeckungstest wieder Pflicht (C29)
- Nächste Zonen (Vorschlag): ElectriCraft (Generator, Motor, Kabel, Transformator, Messgerät, Relais, Widerstände, Sicherung, Batterien, RF, Funkpads – Muster: `electricraft/…/gametest/LiveElectricTests`), RotaryCraft-Motoren und Rohre/Fluide, Verarbeitung (Grinder, Extraktor, Fermenter …), dann ReactorCraft (Spaltung, Turbinen, Fusion). Liste der fehlenden Blöcke: Logzeile `FACTORY zones` jedes GameTest-Laufs.
- Datei: `port/reactorcraft/src/main/java/reika/reactorcraft/gametest/factory/FactoryCoverage.java`.
- Aufgabe: Entweder Zonen gemäß `team/BRIEF.md` umsetzen oder (nach Freigabe, G-4) den Test als `required = false` markieren, bis Zonen existieren.
- Akzeptanz: Standardlauf von `runGameTestServer` ohne bekannte Fehlschläge.

**F-3 · ✅ erledigt in v2 (C6)** – ursprüngliche Aufgabe zur Nachvollziehbarkeit: **Explosionsfestigkeit aller übrigen Blöcke auf Originalwerte umrechnen**
- Dateien: `ReactorBlocks.java` (alle `.strength(a, b)`), `ElectriBlocks.java`, `RotaryBlocks.java` (Rohre `strength(0F, 1F)`, Blast Glass `6000F`, Bedrock Slice), `ReactorFluids.java` (`explosionResistance(500)`).
- Aufgabe: Je Block die Original-Klasse per `javap` prüfen: `func_149752_b(x)` (setResistance) → 1.21-Wert `x·3/5`. Überschreibt die Originalklasse `getExplosionResistance(...)` und gibt einen festen Wert zurück (z. B. BlockDeco: 6000), diesen Wert direkt übernehmen. Im Code als Konstante mit Kommentar wie `ORIGINAL_RESISTANCE_15` hinterlegen.
- Abhängigkeiten: F-1 (zum Testen).
- Akzeptanz: Jede `strength`-Zeile hat einen Kommentar mit Original-Wert und Umrechnung; alle GameTests grün; `NeutronEntity`-Verhalten unverändert gegenüber dem Original-Wert.

**F-4 · ✅ erledigt in v2 (C7)** – **ElectriCraft-Maschinen abbaubar wie im Original**
- Dateien: `port/electricraft/src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json`, `tools/gen_electricraft_assets.py`.
- Aufgabe: Original-Material von `BlockElectricMachine`, `BlockElectricBattery`, `BlockWire` per `javap` ermitteln; für Material.iron/rock die Blöcke generator, motor, transformer, meter, relay, resistor, precise_resistor, fuse eintragen (Kabel nur, wenn das Original es so hatte).
- Akzeptanz: Tag-Inhalt durch Original belegt (Kommentar im Generator); Check v1 ohne Fehler.

**F-5 · ✅ erledigt in v11 (C31; Mini Turbine = Original-Stub, G-8)** – **Fehlende ReactorCraft-Maschinen (v2: Reflektor C11; v3: Dynamometer C13, Diffusor C14; Mini Turbine = Original-Stub, G-8; offen: 3 – Pressurizer, Gas Duct, Heat Pipe)**
- Original-Klassen (aus der JAR verifiziert): `Reika.ReactorCraft.TileEntities.PowerGen.TileEntityReactorPump` (Pressurizer, `PUMP`), `Reika.ReactorCraft.TileEntities.TileEntityGasDuct` (`GASPIPE`), `…TileEntities.TileEntityNeutronReflector` (`REFLECTOR`), `…TileEntities.TileEntityTurbineMeter` (`TURBINEMETER`), `…TileEntities.TileEntitySteamDiffuser` (`DIFFUSER`), `…TileEntities.PowerGen.TileEntityCentrifugalTurbine` (`MINITURBINE`), `…TileEntities.TileEntityHeatPipe` (`HEATPIPE`).
- Aufgabe: je Maschine wie die bestehenden ReactorCraft-Ports: BE mit Original-Logik, Block in `ReactorBlocks`, Assets über `gen_reactorcraft_assets.py`, GameTest mit Original-Sollwerten, Abschnitt in `docs/08`.
- Akzeptanz: je Maschine ≥ 1 GameTest mit aus dem Original abgeleitetem Sollwert; `PickBlockTests` grün.
- Hinweis v3: Nächster Kandidat ist der Pressurizer (`TileEntityReactorPump`, Modell `ModelReactorPump`, Textur `pump.png`, Rezept PpP/gCg/PsP: Grundplatte, Rohr, Glasscheibe, Kompressor, Welle). Original-Code per Vineflower dekompilieren (Abschnitt A), Modell wie in C14 konvertieren.
- Hinweis v2: Gas Duct (`GASPIPE`) und Heat Pipe (`HEATPIPE`) erben im Original von `TileEntityReactorPiping` (Ansaugen, Abgeben, Verbindungen, `PumpablePipe`, Darstellung). Der Port hat dafür **keine gemeinsame Basis** – `WastePipeBlockEntity` und `MagneticPipeBlockEntity` implementieren alles einzeln. Erst eine Basisklasse `ReactorPipingBlockEntity` aus diesen beiden herauslösen (Verhalten unverändert, GameTests grün), dann Gas Duct (`isValidFluid` = gasförmig, Textur Terrakotta + Glas) und Heat Pipe darauf aufsetzen. Erst mit funktionierendem Build (F-1) angehen.

**F-6 · ✅ umgesetzt in v2 (C9), Tests offen (F-1)** – **In-World-Bau der Worktable**
- Original: `Reika.RotaryCraft.RotaryEventManager.buildWorktables(PlayerPlaceBlockEvent / PlayerInteractEvent)` und `checkAndBuildWorktable(World,int,int,int)` (prüft u. a. `ItemStacks.steelblock` und vanilla Blöcke, ersetzt die Struktur durch die Worktable, spielt `SoundRegistry.CRAFT`).
- Aufgabe: Struktur vollständig aus dem Bytecode ablesen (Koordinaten-Offsets, Blöcke), mit `BlockEvent.EntityPlaceEvent` / `PlayerInteractEvent.RightClickBlock` nachbauen.
- Akzeptanz: GameTest baut die Struktur mit `SimPlayer` und erhält eine Worktable.

**F-7 · ✅ erledigt in v2 (C8)** – **Brennwerte Koks/Anthrazit**
- Aufgabe: Original-Brennzeiten (Items `ItemStacks.coke`/`anthracite`, Blöcke ×Faktor `FurnaceFuelRegistry.getBlockOverItemFactor()`; Fundstelle der Registrierung: `Reika.RotaryCraft.RotaryCraft`, Aufrufe `FurnaceFuelRegistry.registerItemSimple`) per `javap` ermitteln und als NeoForge-Data-Map `port/rotarycraft/src/main/resources/data/neoforge/data_maps/item/furnace_fuels.json` eintragen: `{"values": {"rotarycraft:coke": {"burn_time": N}, …}}`. Vorher prüfen, ob die Brennzeit des Original-Items aus `ItemRotaryCraft`/`getBurnTime` oder einem `IFuelHandler` kam.
- Akzeptanz: Werte belegt; Ofen-GameTest brennt die erwartete Tickzahl.

~~**F-8 · P2 · RadiationShield exakt wie im Original (nach G-3)**~~ – **entfällt** (G-3: Tag-Verhalten bleibt).
- Datei: `reactorcraft/…/radiation/RadiationShield.java`.
- Aufgabe: falls G-3 „exakt“: `STEEL` → `s.is(RotaryBlocks.STEEL_BLOCK.get())`, `BEDINGOT` → `s.is(RotaryBlocks.BEDROCK_INGOT_BLOCK.get())`.

**F-9 · P2 · RotaryCraft-Maschinen-Backlog (109 Einträge)**
- Reihenfolge nach Spielrelevanz und Abhängigkeiten: (1) Advanced Gears (CVT, Schneckengetriebe, Hochdrehmoment-/Spulengetriebe), Mehrfachkupplung, Riemen/Kette, Split-Belt, Distrib-Clutch, Portal-Welle; (2) Bohrer (BORER) und Rammbock (PILEDRIVER), Sonic Borer; (3) Kompressor/Pneumatik-Motor, Lüfter, Bewässerung, Holzfäller, Mob-Harvester, Autobreeder, Item-Kanone, Sortierer, Crafter, Composter; (4) Verteidigung/Waffen (Railgun, Lasergun, TNT-Kanone …) nach Freigabe. Fremdmod-Energie (GENERATOR/ELECTRICMOTOR/DYNAMO → FE?) ist eine offene Entscheidung (G-5).
- Akzeptanz je Maschine wie F-5.

**F-10 · P2 · Advancements und Handbuch**
- Hinweis v2: Die 47 RotaryCraft-Achievements (`Reika.RotaryCraft.Registry.RotaryAchievements`) werden überwiegend im Maschinencode ausgelöst (`…triggerAchievement(player)`, z. B. FAILSTEEL im Hochofen, SUCKEDINTOJET/JETCHICKEN/JETFAIL in der Gasturbine, GIGAWATT, MRADS32) oder gehören zu nicht portierten Maschinen (GPR, BORER, RAILGUN, LIGHTBRIDGE …). Umsetzung: ein eigener Kriteriums-Trigger `rotarycraft:achievement` (ID als Parameter) + Aufrufe an den Original-Stellen (Fundstellen per `javap -c` nach `RotaryAchievements.<X>` suchen); vanilla-Trigger (`inventory_changed`) nur für reine Herstellungs-Erfolge.
- Aufgabe: 74 Original-Achievements (`RotaryAchievements`, ReactorCraft/ElectriCraft analog) als `data/<mod>/advancement/*.json` mit passenden Triggern; Handbuch als eigenes Buch-GUI oder JEI-Info.
- Akzeptanz: Datapack lädt fehlerfrei, jeder Fortschritt ist im Survival erreichbar.

~~**F-11 · P3 · Tonkurve/Rahmen der Block-Texturen an Vanilla 1.21 statt GregTech kalibrieren (nach G-2)**~~ – **entfällt** (G-2: GregTech-Stil bestätigt).
- Datei: `port/tools/modernize_textures.py` (`CURVE`, `frame`, `face_edges`).
- Aufgabe: Perzentile an Vanilla-1.21-Texturen (Eisen-/Kupferblock, Ofen, Hochofen) messen statt an GregTech-Gehäusen; Vergleichsbögen erzeugen (wie in v1 für Items).
- Akzeptanz: Vergleichsbögen freigegeben; Manifest konsistent; Check v1 ohne Fehler.

**F-12 · P3 · Repository- und Code-Hygiene**
- Aufgabe: (a) `run-server` aus Git entfernen und ignorieren, (b) `team/`-Kopien entfernen, (c) `.gitmodules` oder Submodul-Einträge bereinigen, (d) GameTest-/Dev-Code in ein eigenes Source-Set verschieben, sodass er nicht in den Release-JARs landet. Jeweils erst nach Freigabe (G-6).

---

## G. Offene Entscheidungen (nicht ohne Rücksprache treffen)

1. ~~**G-1 Item-Stil**~~ – **bestätigt 2026-11-10 (Nutzer):** bleibt wie umgesetzt (Original-Form + milder 1.21-Filter). Ursprünglicher Text: **(umgesetzt in C1)** Standard ist jetzt Original-Form + milder 1.21-Filter; GregTech-Vorlagen nur noch per `--gt-items`. Laut `docs/08` §12 war der GregTech-Stil früher „auf Wunsch“ eingeführt worden – die neue Leitlinie widerspricht dem, daher umgestellt. Rückweg: `python3 tools/modernize_textures.py --only item/ --gt-items`.
2. ~~**G-2 Block-/Gehäusetexturen**~~ – **bestätigt 2026-11-10 (Nutzer):** GregTech-Kalibrierung bleibt; F-11 entfällt. Ursprünglicher Text: Tonkurve „gemessen an GregTech“, dunkle Rahmen und Abschrägungen sowie verbundene Texturen. Erkennbar bleiben die Originale, die Rahmen sind aber ein GregTech-Merkmal (z. B. beim Stahlblock deutlich). Beibehalten oder an Vanilla 1.21 neu kalibrieren (F-11)?
3. ~~**G-3 Abschirmung per Tag oder exakt**~~ – **entschieden 2026-11-10 (Nutzer):** jeder Stahl (`c:storage_blocks/steel`) schirmt ab (heutiges Verhalten); F-8 entfällt. Ursprüngliche Frage: Soll jeder Block in `c:storage_blocks/steel` (z. B. Mekanism-Stahl) wie HSLA-Stahl abschirmen (heutiges Verhalten), oder nur der RotaryCraft-Block wie im Original (F-8)?
4. ~~**G-4 Factory-Batch**~~ – **entschieden 2026-11-09 (Nutzer):** Abdeckungstest sofort optional (`required = false`), Zonen danach nach und nach bauen; wenn alle Blöcke abgedeckt sind, wieder `required = true`.
5. **G-5 Fremdmod-Energie-Maschinen** (2026-11-10: Nutzer entscheidet später) (RotaryCraft GENERATOR/ELECTRICMOTOR/DYNAMO, ElectriCraft EU-Teile): auf FE umdeuten oder weglassen?
6. **G-6 Bereinigung** (2026-11-10: Nutzer entscheidet später): Dürfen `port/reactorcraft/run-server/`, `team/` und die leeren Submodul-Einträge aus dem Repository entfernt werden (E-1 … E-3)?
7. **G-7 Netzwerkfreigaben** für die Cloud-Umgebung (F-1) – Einstellung der Umgebung, nur durch den Nutzer änderbar.
8. **G-8 Mini Turbine (`MINITURBINE`)** (2026-11-10: Nutzer entscheidet später): Im Original ein Stub (`TileEntityCentrifugalTurbine`: kein Dampf, Drehzahl/Drehmoment 0, kein Rezept, nur im Kreativ-Inventar). Weglassen (Empfehlung) oder als funktionslosen Kreativ-Block portieren?
9. ~~**G-9 Öffentliches Repository**~~ – **geklärt 2026-11-09 (Nutzer):** Der Nutzer ist Contributor des Repositorys (GitHub `robohelo`); das öffentliche Repository ist für ihn kein Problem. Commits/Pushes wie sonst nur auf Anweisung.
