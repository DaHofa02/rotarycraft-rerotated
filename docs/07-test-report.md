# 07 – Testbericht (Phase D, Schritte 1–4, ElectriCraft-Ausschnitt)

Stand: 2026-10-19. Alle Befehle im Ordner `port/`. Umgebung: macOS, Amazon Corretto 21.0.9, Gradle 9.2.1 (Wrapper aus der checksummenverifizierten Distribution erzeugt), ModDevGradle 2.0.148, NeoForge 21.1.262, Minecraft 1.21.1.

## 1. Ausgeführte Prüfungen und Ergebnisse

| # | Prüfung | Befehl | Ergebnis |
|---|---|---|---|
| T1 | Gradle-Build aller drei Mods | `./gradlew build` | **BUILD SUCCESSFUL**. JARs: `rotarycraft/build/libs/rotarycraft-1.21.1-0.1.0.jar` (4,2 MB), `reactorcraft/build/libs/reactorcraft-1.21.1-0.1.0.jar`, `electricraft/build/libs/electricraft-1.21.1-0.1.0.jar`. ElectriCraft enthält Kabelnetz, Generator, Motor, Leitungsbauteile, Batterien, Transformator und Erze (§T3b); ReactorCraft Erze und Materialien (§T3c). |
| T2 | JUnit (mit Minecraft auf dem Klassenpfad) | `./gradlew :rotarycraft:test` | **11/11 bestanden** (FormulaTest 7, ResourceCompletenessTest 4) |
| T3 | GameTests (echter Server, echte Weltblöcke) | `./gradlew :rotarycraft:runGameTestServer` | **„All 106 required tests passed“** (zuletzt) |
| T3b | GameTests RotaryCraft + ElectriCraft zusammen | `./gradlew :electricraft:runGameTestServer` | **„All 100 required tests passed“** (80 RotaryCraft + 10 `WireNetworkTests` + 3 `OreTests` + 4 `BatteryTests` + 3 `RFTests`) |
| T3c | GameTests aller drei Mods | `./gradlew :reactorcraft:runGameTestServer` | **„All 219 required tests passed“** (106 RotaryCraft + 20 ElectriCraft + 93 ReactorCraft; neu seit dem vorletzten Stand: 3 `RefrigeratorTests`, 3 `HeatRayTests`, 2 `CompactorTests`, 10 `FusionTests`, 1 `MagnetTests`, zuletzt 3 `FillingStationTests`. Der Fusionstest `tokamakFuses` baut den ganzen Tokamak mit echter Gasturbinen-Leistung am Solenoid und prüft Ring, Ladung, Plasmafluss, Fusionen und Fusionsneutronen) |
| T4 | Dedicated Server mit allen drei Mods | `./gradlew :reactorcraft:runServer` | Start ohne Fehler: `Done (3.533s)!`, alle drei Mods geladen, keine Registry-Konflikte. Belegt auch die Client/Server-Trennung (keine Client-Klassen auf dem Server geladen). |
| T5 | Client-Start in eine Testwelt | `./tools/make_showcase.sh` und `./gradlew :rotarycraft:runShowcase` | Client lädt die Welt. Renderer, Item-Darstellung und GUIs wurden per Screenshot geprüft (`rotarycraft/run/screenshots/showcase_*.png`). Client-seitig synchronisierte Werte stehen im Log (siehe §3). |
| T5b | Client-Start ElectriCraft-Testwelt | `sh tools/make_showcase.sh electricraft tools/showcase_electricraft` und `./gradlew :electricraft:runShowcase` | Generator, Motor, Dynamometer (16 Nm), die 5 Erze mit Barren, alle 16 Kabelsorten und eine Leitung mit Relais, Widerstand (Farbringe), Sicherung, Messgerät (zeigt 512 V / 2 A) und Präzisionswiderstand mit Verbindungen (gerade, Knick, Abzweig) sichtbar; `electricraft/run/screenshots/showcase_0..2.png` |
| T5c | Erzgenerierung in echter Welt | Dedicated Server (`:electricraft:runServer`, Seed 12345), Chunks per `forceload` erzeugt, Erze per `fill … replace` gezählt (RCON, nur lokal) | 16 Chunks: Zinn 53, Silber 128, Nickel 318, Platin 18, Aluminium 0 (Höhenband über dem Gelände); Vanilla-Eisen zum Vergleich vorhanden |
| T5d | Client-Start ReactorCraft-Testwelt mit laufendem Tokamak | `sh tools/make_showcase.sh reactorcraft tools/showcase_reactorcraft` und `./gradlew :reactorcraft:runShowcase` (dev-only `FusionShowcase` baut den Tokamak wie der GameTest und versorgt ihn je Tick) | `reactorcraft/run/screenshots/showcase_26..30.png`: Toroidmagnete, Solenoid-Spule, Injektor-Rahmen, Funken zwischen den Magneten, leuchtendes Plasma in den Magneten, Fusionsneutronen, geschmolzene Absorber; `showcase_24/25`: Kühlschrank mit Gaskollektor, Kompaktor, Hitzestrahler, Gaskollektor auf Ofen, Magnetrohr, Markierer-Linien, alle Fusions-Multiblockteile; GUIs `showcase_gui_13/14` (Kühlschrank mit Eis, Trockeneis und LN₂; Kompaktor) |
| T5e | JEI- und Kompatibilitäts-Prüfung (mit JEI 19.21.0.247 und Mekanism 10.7.19) | `./gradlew :reactorcraft:runJeiShowcase`, `./gradlew :reactorcraft:runCompatShowcase`, `./gradlew :reactorcraft:runGameTestServer` | alle 21 JEI-Kategorien und 7 Multiblock-Ansichten fotografiert (`jei_*.png`); Kompatibilitätswelt fotografiert; GameTests „All 224 required tests passed“ (darunter 5 `CompatTests`; die Zahl enthält auch Tests einer parallel laufenden Füllstation-Portierung) |
| T5f | Live-Szenarien mit simuliertem Spieler (Überleben und Kreativ, siehe 08 §10) und spielbare Basis | `./gradlew :reactorcraft:runGameTestServer`, `./gradlew :reactorcraft:runBaseShowcase` | **„All 236 required tests passed“** (darunter je 2 `LiveBaseTests.powerLine`/`workshop`, 2 `LiveElectricTests.electricLine`, 6 `LiveReactorTests`); Basis gebaut und fotografiert (`showcase_0..6.png`: Übersicht, Wellen-/Grinder-/Fermenter-Strang, ElectriCraft-Strang, Spaltkern und Vorheizer, geformtes Solenoid, Injektor, Mekanism-Kabel und -Tank mit Rohr) |
| T5g | GregTech CEu Modern 7.0.2: Energie FE → EU → FE (Überleben und Kreativ) und JEI-Vergleich (siehe 08 §11) | `./gradlew :reactorcraft:runGregtechTests`, `JEI_ONLY=… ./gradlew :reactorcraft:runJeiShowcase`, `./gradlew :reactorcraft:runBaseShowcase` | **„All 2 required tests passed“** (`GregTechTests`, im integrierten Server, da GregTech hier auf dem dedizierten Server nicht lädt); Server-GameTests weiter „All 236 required tests passed“ (jetzt in `reactorcraft/run-server`); JEI-Screenshots GregTech gegen Ports verglichen, neue Darstellung aller Kategorien und Multiblöcke fotografiert; GregTech-Strecke in der Basis fotografiert (`showcase_7.png`) |
| T5h | Moderne Texturen, verbundene Texturen und Classic-Paket (siehe 08 §12) | `python3 tools/modernize_textures.py`, `./gradlew :reactorcraft:runBaseShowcase` (einmal mit den Classic-Paketen in `options.txt`), `./gradlew build` | 1131 Texturen modernisiert (Items mit eigener Kurve, Licht und Kontur; Detailschutz), 1875 Varianten für verbundene Texturen, Originale im Classic-Paket; verbundene Wände und Böden fotografiert (`showcase_8.png`), Items in JEI fotografiert; GameTests weiter „All 236 required tests passed“; Vergleichs-Screenshots `textures_classic_vs_modern*.png`; Classic-Pakete laden; Build einschließlich der JUnit-Texturprüfungen grün |
| T5i | Alle Items modern, fehlende Texturen ergänzt (siehe 08 §12) | `./gradlew :reactorcraft:runItemGallery`, Texturprüfung im Log, `./gradlew build` | 189 Materialitems aus GregTech-Vorlagen, 5 Eimer als 1.21-Fluidbehälter, 2 Platzhalter-Texturen und 4 unsichtbare Fusions-Items ergänzt; Texturprüfung „475 items checked (64 drawn by renderers), 0 without texture“ (Gegenprobe erkennt ein abgemeldetes Item); alle 475 Items per Galerie-Screenshot angesehen; JUnit grün |
| T5j | Mittelklick bei jedem Block, Modellkanten (siehe 08 §13/§14) | `./gradlew :reactorcraft:runGameTestServer` (`PickBlockTests`), `./gradlew :reactorcraft:runBaseShowcase` | vorher 39 von 229 Kreativ-Stacks mit falscher Variante und 3 Blöcke ohne Pick-Item (davon 1 echt: `bedrock_slice`; Raps und Dampf siehe 08 §13); jetzt „229 tab stacks, 229 placed and picked, 0 wrong“ und „191 blocks, 0 without a pick item“; **„All 238 required tests passed“**; Modellkanten auf 72 Atlanten, fotografiert |
| T5k | Gesamtprüfung mit drei spezialisierten Subagents (Build/JUnit, Server-GameTests, Client-Läufe mit Bildprüfung) | `./gradlew build`, `:rotarycraft/:electricraft/:reactorcraft:runGameTestServer`, `runGregtechTests`, `runItemGallery`, `runJeiShowcase`, `runBaseShowcase` | Build grün, JUnit 11/11, Jars vollständig (mit Classic-Paketen), Textur-Manifest 1131/1131 stimmig; RotaryCraft 110/110, ElectriCraft 132/132; Gesamtlauf zunächst 237/238 – `BreederTests.breederConvertsFuel` war zufallsabhängig (Abfall vergiftet den Kern, wie im Original, bevor eine Umwandlung kam), Test entfernt jetzt den Abfall nach jedem Neutron; danach dreimal „All 238 required tests passed“; GregTech 2/2; 475 Items ohne Lücke; gefunden und behoben: Solenoid-Ring in der Welt mit Licht 0 gezeichnet (Kern im Multiblock eingeschlossen) und abgeschnittene Teile-Anzahlen im JEI-Multiblock-Viewer |
| T5d | ReactorCraft-Erze in echter Welt und Testwelt | Dedicated Server (Seed 12345, Oberwelt/Nether/End, RCON-Zählung) und `./gradlew :reactorcraft:runShowcase` | Alle Erze erscheinen in ihrer Dimension (Zahlen in 08 §6); Erze, Fluorit (aktiviert mit Partikeln), Blöcke, alle Items, Kanister und 15 Fluide in Reservoirs mit Originaltexturen |
| T6 | Scaffold-Test (vor dem Inhalt): Server mit drei leeren Mods | `./gradlew :reactorcraft:runServer` | `Done (0.492s)!` |

### T2 – JUnit im Einzelnen
| Test | Prüft |
|---|---|
| `engineTableMatchesOriginal` | Drehzahl und Drehmoment aller 9 Motortypen wie in `EngineType` des Originals |
| `shaftTorqueLimitMatchesTwistFailure` | `MaterialRegistry.getMaxShaftTorque` und `ReikaEngLibrary.mat_twistfailure` (Wellenbruch) stimmen für alle Materialien überein |
| `woodShaftLimit` | Holzwelle: max. ≈ 278 Nm (Formel ½πr³τ/16) |
| `flywheelSpeedLimits` | Schwungrad-Grenzdrehzahl und Mindestdrehmoment |
| `mathHelpers` | logbase2, logbase, intpow, getThousandBase, SI-Präfix |
| `powerFormat` | Ausgabeformat von Leistung/Drehzahl/Drehmoment wie im Original |
| `difficultyDefaultsToMedium` | Schwierigkeitsgrad-Vorgabe 2, abgeleitete Werte (PARTCRAFT = 3) |
| `plainItemsHaveTextureModelAndName` | jedes einfache Item hat Textur, Modell und englischen Namen |
| `machinesHaveBlockstateModelsAndName` | jede Maschine hat Blockstate, Block-/Item-Modell und Namen |
| `recipesReferenceExistingItems` | alle 173 Rezepte referenzieren nur existierende `rotarycraft:`-Items |
| `machineTexturesUsedByRenderersExist` | alle von den Renderern verwendeten Modelltexturen sind vorhanden |

### T3 – GameTests im Einzelnen (`PowerTransmissionTests`)
| Test | Aufbau | Erwartung (aus dem Original abgeleitet) |
|---|---|---|
| `dcEngineDrivesDynamometer` | DC-Motor + Redstone → Stahlwelle → Dynamometer | 256 rad/s @ 4 Nm |
| `dcEngineNeedsRedstone` | DC-Motor ohne Redstone | 0 W |
| `steelGearboxReduces` | DC → Stahlgetriebe 2:1 (geschmiert) → Dynamometer | 128 rad/s @ 8 Nm |
| `woodGearboxFourToOne` | DC → Holzgetriebe 4:1 | 64 rad/s @ 16 Nm |
| `clutchFollowsRedstone` | Kupplung ohne, dann mit Redstone | 0 W, danach 256 @ 4 |
| `junctionMergesTwoEngines` | 2 DC-Motoren → Wellenverbinder (Merge) | 256 rad/s @ 8 Nm |
| `junctionSplitsEvenly` | DC → Verbinder (Split 1:1) → zwei Dynamometer | je 256 @ 2 |
| `bevelGearsTurnPower` | DC → Kegelrad (Selbstfindung der Richtung) → Dynamometer quer | 256 @ 4 |
| `flywheelSpinsUp` | DC → Holzschwungrad | Drehzahl steigt langsam, 4 Nm |
| `gasEngineRunsOnEthanol` | Benzinmotor, Ethanol per Fluid-Capability von unten (oben abgewiesen) | 512 @ 128 |
| `dryGearboxWears` | Stahlgetriebe ohne Schmiermittel | Verschleiß > 0 |
| `hydroEngineRunsOnWaterfall` | Hydromotor, Wasserfall 4 Blöcke hoch, geschmiert | 4 rad/s @ 259 Nm |
| `hydroEngineNeedsLubricant` | dasselbe ohne Schmiermittel | 0 W |
| `jetEngineChokedByWall` | Gasturbine, Mauer im Einlass (Drossel 0,25) | 16384 rad/s @ 1024 Nm |
| `jetAfterburnerDoublesTorque` | dasselbe mit Nachbrenner | 16384 @ 2048 |
| `jetIngestsStone` | Stein-Item vor dem Einlass | Bruchstein kommt hinten raus, FOD 4, 64 Nm |
| `ecuThrottlesGasEngine` | Benzinmotor auf ECU (MEDIUM), Ethanol über den ECU-Tank | 256 @ 128 |
| `ecuShutdownStopsEngine` | ECU auf SHUTDOWN | 0 W |
| `dcRedstoneUpgrade` | DC-Motor mit Redstone-Upgrade, ohne Signal | 256 @ 4; zweites Upgrade abgelehnt |
| `blastFurnaceMakesSteel` | Hochofen 700 °C, Kohle/Schießpulver/Sand, 3 Eisenbarren | 3 HSLA-Stahl, Kohle −1 |
| `blastFurnaceTooCold` | dasselbe bei 300 °C | kein Fortschritt |
| `blastFurnaceCraftsCombustor` | 3×3-Muster bei 1200 °C | Hochtemperatur-Brennkammer |
| `worktableCraftsMachine` | ECU-Muster, Redstone-Impuls | ECU im Ausgang |
| `worktableRecycles` | Stahl-Getriebeeinheit 2:1, Redstone | 48 Schrott |
| `worktableUncrafts` | Benzinmotor in der Mitte, Redstone | Zylinder, Gold, Getriebeeinheit, Laufrad an den Rezeptpositionen |
| `frictionHeaterHeatsBlastFurnace` | Benzinmotor → Reibungsheizer → Hochofen | Heizer 750–790 °C (Formel: ~786), Hochofen > 700 °C |
| `frictionHeaterNeedsPower` | Heizer ohne Motor | < 100 °C |
| `frictionHeaterDrivesFurnace` | Benzinmotor → Heizer → Vanilla-Ofen mit Roheisen, ohne Brennstoff | Eisenbarren im Ausgang |
| `frictionHeaterReaches1100WithTwoEngines` | 2 Performance-Motoren → Wellenverbinder (1024 rad/s @ 512 Nm) → Heizer → Hochofen | Heizer 1100–1110 °C (Simulation der Originalformel: 1110), Hochofen ≥ 1100 °C, Federstahl |
| `frictionHeaterMakesHighCombustor` | derselbe Aufbau, Brennkammer-Muster | Hochtemperatur-Brennkammer (1100-°C-Rezept) |
| `turbineHeaterMakesBedrockAlloy` | Gasturbine gedrosselt (16384 @ 1024) → 3 Bedrock-Wellen → Heizer → Hochofen | Heizer ≥ 1450 °C (Gleichgewicht 1710), Bedrock-Barren, genau 4 Bedrockstaub verbraucht |
| `turbineHeaterMakesDiamondGears` | derselbe Aufbau, Diamant-Zahnrad-Muster | 6 Diamant-Zahnräder (1400 °C, mittlere Schwierigkeit) |
| `eightGasEnginesReach1100` | 2 Reihen à 4 Benzinmotoren (normale Verbinder), vereint durch einen Bedrock-Verbinder → Heizer → Hochofen | 512 rad/s @ 1024 Nm, Heizer 1100–1110 °C, Hochofen ≥ 1100 °C |
| `eightGasEnginesNeedBedrockJunction` | derselbe Aufbau mit normalem Verbinder | Verbinder verweigert (Engine-Spam-Regel), Heizer bleibt aus |
| `bedrockBreakerGrindsBedrock` | Gasturbine gedrosselt → 3 Bedrock-Wellen → Bedrock-Getriebe 16:1 → Bedrock Breaker (1024 rad/s @ 16384 Nm) | Bedrock wird Slice (von Süden geschnitten), Stufe ≥ 1 |
| `bedrockBreakerMakesDust` | derselbe Antrieb, Slice Stufe 15, Truhe daneben | Slice weg, 2 Bedrockstaub in der Truhe |
| `bedrockBreakerClearsBlocks` | Stein vor dem Kopf | Stein weg, keine Drops, Kopf auf Schritt 2 |
| `bedrockBreakerNeedsPower` | ein Benzinmotor | Bedrock bleibt |
| `extractorStageOne` | Benzinmotor → Stahlgetriebe 4:1 → Kegelrad → Extraktor (128 rad/s @ 512 Nm), Tiefenschiefer-Eisenerz | Erz verbraucht, 1–2 Eisenerz-Staub im Staub-Slot (Stufe 1 braucht 512 Nm) |
| `extractorStageTwo` | 4:1 ins Schnelle (2048 rad/s), Golderz-Staub | Golderz-Schlamm, genau 125 mB Wasser verbraucht |
| `extractorStageThree` | 16:1 ins Schnelle (8192 rad/s), Diamant-Schlamm | Diamant-Lösung |
| `extractorStageFour` | 4:1 Reduktion (256 Nm nötig), Eisen-Lösung | 1–2 Eisenflocken; Flocken schmelzen im Ofen zu Eisenbarren |
| `extractorNeedsTorque` | 2:1 ins Schnelle (32 Nm), Eisenerz | nach 650 Ticks unverändert, kein Fortschritt |
| `pipeFeedsSteamEngine` (PipeTests) | 3 Rohre mit 20000 mB Wasser → Rückseite eines Dampfmotors über Feuer | Rohr verbindet sich, Wasser im Motor, Motor läuft mit 512 rad/s @ 32 Nm |
| `pipeOnlyAtEngineBack` | Rohre vor, neben und hinter einem Motor | nur das Rohr an der Rückseite verbindet sich (Original `canConnectToPipeOnSide`) |
| `pipesEqualize` | 2 Rohre, 1000 mB in einem | Menge bleibt erhalten, Ausgleich bis auf ≤ 4 mB (je Schritt ein Viertel der Differenz) |
| `pipeTypesDoNotMix` | Rohr neben Treibstoffleitung, Schlauch und Bedrock-Rohr | nur Rohr ↔ Bedrock-Rohr verbunden |
| `pipeFluidRules` | – | Rohr nimmt kein Ethanol/Kerosin/Schmiermittel, aber Wasser/Lava; Treibstoffleitung kein Wasser; Schlauch nur Schmiermittel |
| `fuelLineFeedsEngine` | ECU mit 4000 mB Ethanol → 2 Treibstoffleitungen → Unterseite eines Benzinmotors | Leitung verbindet sich nach oben, Treibstoff im Motor |
| `hoseLubricatesGearbox` | Schlauch mit Schmiermittel seitlich am Getriebe, ein zweiter Schlauch oben | Getriebe erhält Schmiermittel, oberer Schlauch verbindet sich nicht |
| `pipeFillsExtractor` | Rohr mit Wasser seitlich am Extraktor (allgemeiner Fluid-Handler) | Extraktor erhält Wasser |
| `grinderMakesLubricant` (GrinderTests) | Benzinmotor → Grinder, 2 Rapssamen; Schlauch seitlich zu einem Getriebe | 512 rad/s @ 128 Nm, 2 Rapsschalen, Schmiermittel insgesamt 128–320 mB (2 × 64–160, mittlere Schwierigkeit), Getriebe erhält Schmiermittel über den Schlauch |
| `grinderGrindsCobblestone` | Bruchstein | Kies |
| `grinderNeedsTorque` | Gleichstrommotor (4 Nm) | kein Fortschritt nach 350 Ticks |
| `grinderFillsBucket` | 1500 mB im Tank, leerer Eimer | Schmiermitteleimer, 500 mB bleiben |
| `canolaGrows` | Raps auf feuchtem Ackerboden, Zufallsticks; Raps auf trockenem Ackerboden | wächst bis Stufe 9; auf trockenem Boden fällt er als Samen ab |
| `canolaDrops` | 500 × Ernte-Drop berechnet | reif 2–26 Samen (Originalformel), jung 1 Samen |
| `grassDropsCanolaSeeds` | 4800 × Loot von Gras (mit Loot-Modifier) | ≈ 100 Rapssamen und ≈ 500 Weizensamen (Original-Gewichte 2 : 10 bei 1/8 Samenchance) |
| `microturbinesSmeltTungsten` | 2 Mikroturbinen (Kerosin) → je Bedrock-Getriebe 16:1 → Wellenverbinder → 4:1 → 2048 rad/s @ 2048 Nm → Reibungsheizer → Vanilla-Ofen mit Wolframflocken | Heizer 1350–1482 °C (Gleichgewicht 1482), Wolframbarren im Ofen |
| `microturbinesRunBedrockBreaker` | dieselben Turbinen, 16:1, Verbinder, 16:1, 2:1 → 256 rad/s @ 16384 Nm = 4194304 W | Bedrock wird Slice (Mindestleistung genau erreicht; die Turbinen brauchen ~1930 Ticks bis Volllast, +68 rad/s je Tick wie im Original) |
| `fermenterMakesYeast` (EthanolChainTests) | Benzinmotor → Fermenter (25 °C, 4000 mB Wasser), Zucker + Erde | Hefe, je Hefe 50 mB Wasser verbraucht, Front nach Westen (Meta 0) |
| `fermenterStacksYeast` | wie oben, 3 Hefe schon im Ausgang | Stapel wächst |
| `fermenterMakesSludge` | Redstone-Signal, Hefe + Eichenlaub (Mulchwert 2) | Schlamm in Zweierschritten |
| `fermenterNeedsWater` | ohne Wasser | nichts nach 450 Ticks |
| `centrifugeCleansSludge` | Benzinmotor → Getriebe 8:1 ins Schnelle → Kegelrad → Zentrifuge (4096 rad/s), 6 Schlamm | 2 Schlamm je Vorgang (max_stack 2), nur sauberer Schlamm/Kompost im Ausgang |
| `centrifugeMakesLubricant` | 3 Rapsschalen | genau 90 mB Schmiermittel nach einem Vorgang (mittlere Schwierigkeit); sauberer Schlamm schmilzt zu Ethanol |
| `fractionatorMakesJetFuel` | Benzinmotor → Getriebe 16:1 ins Schnelle → Kegelrad → Fraktionierer (8192 rad/s, 65536 W), alle 6 Zutaten, Ghast-Träne, 4000 mB Ethanol | Kerosin > 0, genau 250 mB Ethanol und 1–2 Zutaten je Vorgang, Träne bleibt |
| `fractionatorNeedsGhastTear` | ohne Ghast-Träne | nichts nach 450 Ticks |
| `fractionatorYieldCurve` | Druck gesetzt | Ausbeute 0,01 / 0,1 / 1 / 2,5 bei 0 / 180 / 720 / 1000 kPa, linear dazwischen (600 kPa → 0,673) |
| `pumpFillsPipe` (PipeTests) | Pumpe über einem ummauerten 3×3-Wasserbecken, Benzinmotor (64-fache Mindestleistung), Rohr seitlich | Pumpe pumpt (4000 mB je Quellblock: Wasser ×4 bei 64× Leistung), Rohr füllt sich nur bis zum Gegendruck 5000 mB |
| `reservoirsLevelOut` | 2 Reservoirs, 8000 mB Wasser in einem | Wand dazwischen fällt weg, Pegel gleichen sich aus (±2 mB), Menge bleibt |
| `reservoirsKeepFluidsApart` | Wasser neben Schmiermittel | keine Vermischung, Wand bleibt |
| `reservoirDrainsDown` | Reservoir mit 8000 mB, Rohr darunter und daneben | Rohr darunter wird befüllt, Menge bleibt; das Item des gefüllten Reservoirs trägt den Inhalt |
| `pulseJetMakesBlastGlass` (EthanolChainTests) | Mikroturbine → Kegelrad → Pulse-Jet-Ofen mit Kerosin und Wasser, 2 Obsidian | Explosionsglas, Ofen zwischen 850 und 1000 °C (Turbine braucht ~1930 Ticks bis 131072 rad/s) |
| `pulseJetNeedsSpeed` | Benzinmotor (512 rad/s), Ofen auf 900 °C, Eisenbarren | kein Stahl (Mindestdrehzahl 131072 rad/s) |
| `valveNeedsRedstone` (PipeTests) | Ventilrohr unter einem gefüllten Reservoir, später Redstone-Block daneben | ohne Signal 0 mB, mit Signal fließt Wasser |
| `separatorGivesDown` | Rohr → Trennrohr seitlich, Rohre darüber und darunter | nur das untere Rohr erhält Wasser |
| `bypassesStayApart` | zwei Paare Bypass-Rohre nebeneinander, bei einem Paar „gegeneinander gesetzt“ | nur das gegeneinander gesetzte Paar verbindet sich |
| `suctionPipeIgnoresPipes` | Saugrohr zwischen gefülltem Rohr und Reservoir darüber | verbindet sich mit dem Rohr, nicht mit dem Reservoir, nimmt nichts aus dem Rohr |
| `gasPerformanceUpgrade` | Performance-Upgrade am Benzinmotor | wird Performance-Motor, Treibstoff und Ausrichtung bleiben |

Pulse-Jet-Ofen und Explosionsglas, Sichtprüfung (`showcase_gui_12.png`): GUI mit Temperatur-, Wasser- und Treibstoffanzeige, Eingang/Ausgang, Leistungsreiter; Modell und Glasblöcke in der Welt.

Pumpe und Reservoir, Sichtprüfung (`showcase_9.png`): Pumpe mit Motor über einem Becken, 2×2-Reservoir-Gruppe mit gemeinsamer, geglätteter Wasseroberfläche, abgedecktes Schmiermittel-Reservoir, Lava-Reservoir.

Fermenter, Zentrifuge, Fraktionierer, Sichtprüfung (`showcase_8.png`, `showcase_gui_9..11.png`): Fermenter-Würfel mit Vorder-/Rückseite, Zentrifuge mit drehender Trommel, Fraktionierer-Turm; alle drei GUIs mit Anzeigen, Ziel-Symbol, Tanks und Leistungsreiter. Server-Log: Fermenter bei 27 °C, Hefe-Stapel wächst.

Grinder und Raps, Sichtprüfung (`showcase_7.png`, `showcase_gui_8.png`): Rapsfeld mit allen Wachstumsstufen, Grinder mit Benzinmotor (läuft, Samen → Schalen), Grinder-GUI mit Schmiermittelanzeige und Leistungsreiter.

Rohre, Sichtprüfung (`showcase_6.png`): Rohr mit Wasser und mit Lava, Schlauch mit Schmiermittel, Treibstoffleitung mit Kerosin, Ecke und senkrechtes Stück, Bedrock-Rohr, Dampfmotor über Feuer mit Wasserrohr an der Rückseite (Client-Log: 512 rad/s, 32 Nm, 121 °C). Rahmen, Glasfenster und Anschlussstutzen wie im Original.

Zweite Runde, Sichtprüfung (`showcase_4.png`, `showcase_gui_4.png`): Gasturbine mit Glühen und Nachbrennerflamme (Gras im Abgas wird zu Glas, wie im Original ab 900 °C), Hydromotor, ECU (normal, gefärbt), rote E/A-Box. Dabei beobachtet: wilde Schleime wurden eingesaugt, verursachten FOD, die Turbine (mit 1500 °C gestartet) ging in den Fehlermodus und explodierte – Originalverhalten; die Testwelt schaltet Mob-Spawning jetzt ab.

## 2. Während der Tests gefundene und behobene Fehler

| Fehler | Ursache | Behebung |
|---|---|---|
| Zwei GameTests schlugen zuerst fehl | Fehler **im Test**: Dynamometer mit Meta 3 (liest Norden) statt Meta 2 (liest Süden) platziert | Test korrigiert; Diagnose über erweiterte Fehlermeldung |
| Getriebe lief in der Testwelt als Übersetzung ins Schnelle (1024 rad/s @ 1 Nm statt 64 @ 16) | `reduction` wurde beim Laden auf `false` gesetzt, wenn der NBT-Schlüssel fehlte | fehlender Schlüssel behält jetzt die Vorgabe „Reduktion“ (`GearboxBlockEntity.readSyncTag`) |
| Kegelrad in der Testwelt ohne Leistung | alle Blöcke im selben Tick gesetzt; die Richtungssuche läuft im ersten Tick, bevor die Nachbarn ihre E/A-Seiten kennen. **Gleiches Verhalten im Original.** | nicht geändert (originalgetreu); in der Testwelt Richtung explizit gesetzt. Im normalen Spiel, wo Maschinen nacheinander gesetzt werden, tritt das nicht auf |
| Modellkonverter nahm Teil-Texturgrößen (`setTextureSize` nach `addBox`) | In 1.7.10 wirkt nur die Modellgröße zum Zeitpunkt von `addBox` | Konverter nutzt die Modellgröße, auch geerbt (ModelGearboxBase 128×32); alle bisherigen Modelle unverändert, ModelJet korrekt 128×128 |
| Testwelt-Neuaufbau ließ Items fallen | `/fill` ersetzte Hochofen/Worktable, deren Inventar beim Entfernen herausfiel | Maschinen mit Inventar sind `Clearable` (Vanilla-Verhalten für Befehle) |
| `make_showcase.sh` erzeugte keine Welt (nur Datapack) | Server-Lauf fehlte im Skript | Skript startet den Server einmal und beendet ihn |
| Absturz „Accessing LegacyRandomSource from multiple threads“ in der Testwelt | Der gemeinsame Zufallsgenerator aller Maschinen (`RotaryCraftBlockEntity.rand`, statisch) wurde von Client- und Server-Thread gleichzeitig benutzt (im Original `java.util.Random`, threadsicher) | threadsicherer `RandomSource.createThreadSafe()` |
| Grinder-GUI-Screenshot zeigte die Welt | Maschinen öffnen nur für Spieler im Umkreis von 8 Blöcken (Original `isPlayerAccessible`), die Testwelt-Kamera stand weiter weg | Testwelt-Skript stellt den Spieler vor das Öffnen neben die Maschine |
| Raps wurde schwarz hinterlegt dargestellt | Pflanzenmodell ohne Cutout-Rendertyp | `render_type: cutout` |
| Fermenter verlor in der Testwelt seine Hefe | Umgebungstemperatur 95 °C: die Tiefenkorrektur nahm Meereshöhe + 1 (64) als Bodenhöhe, auch in Flachwelten (Boden y = −60) → „tief unter der Erde“ → Hefe stirbt bei 60 °C. Das Original nimmt `getAverageGroundLevel()` (64, in Flachwelten 4 = Oberkante der Schichten) | Bodenhöhe = Spawnhöhe des Chunk-Generators (64, in Flachwelten Oberkante der Schichten). Betrifft alle temperaturabhängigen Maschinen in Flachwelten (auch die Testwelt und die GameTest-Welt); frühere Testwelt-Werte (z. B. Dampfmotor 121 °C) waren dadurch zu hoch. Der Dampfmotor-Test `pipeFeedsSteamEngine` startet deshalb jetzt bei 95 °C (Feuer heizt nur ~1 °C/s) |
| Zentrifugen-Trommel unsichtbar | Animation `phi += log2(ω)` ergab bei Stillstand −∞ (NaN-Drehung); das Original nutzt den ganzzahligen `logbase2` (0 bei 0) | ganzzahliger `logbase2` |
| ElectriCraft-Testwelt hing im Startbildschirm | neues Run-Verzeichnis ohne `options.txt` → Barrierefreiheits-Abfrage beim ersten Start | `options.txt` aus `rotarycraft/run` übernommen (nur Dev-Umgebung) |
| Batterie-GameTest: Motor bekam keinen Strom | **Fehler im Test**: 32 A durch Stahlkabel (max. 16 A) – das Kabel schmolz originalgetreu zu Lava | Test nutzt Kupferkabel; das Schmelzen ist jetzt eigener Test |
| Transformator-GameTest: Motor bekam 2 statt 3 A | **Erwartung im Test falsch**: der Transformator ist eine zweite Quelle, der Motor ohne Verstärker ist dann originalgetreu auf Gesamtstrom/Quellen begrenzt | Erwartung korrigiert und dokumentiert |
| Prozessor-GameTest: kein UF₆ | **Fehler im Test**: 1000 mB Wasser sind nach 4 Zwischenschritten leer; danach läuft originalgetreu auch der Ausgabeschritt nicht mehr | Test füllt 3000 mB |
| Isotopen-Zentrifuge verbrauchte UF₆ ohne Staub | **Port-Fehler**: die Ausgabe ging über `insertItem`, das die (von außen gesperrten) Slots ablehnte | direktes Setzen wie `addOrSetStack` im Original; durch GameTest gefunden |
| Testwelt-Screenshots zeigten das Pausenmenü | Client pausiert ohne Fensterfokus | `pauseOnLostFocus:false` in der Dev-Run-Konfiguration |
| Spaltungs-GameTest: mittlerer Kern nur 25–57 °C | **Erwartung im Test falsch**: Spaltung ist zufällig, und eine kleine Reihe aus 3 Kernen verliert die Wärme (+20 °C je Spaltung) originalgetreu fast so schnell, wie sie entsteht | Test nutzt eine 3×3-Lage und prüft die Spitzentemperatur des mittleren Kerns (≥ 40 °C) |
| ReactorCraft-Erze, Fluorit, Beton usw. nicht als „mit Spitzhacke abbaubar“ markiert | **Port-Fehler im Generator**: `tag()` schrieb die Datei bei jedem Aufruf neu, nur der letzte Aufruf blieb (Brennstoffkern, Kühlzelle) | `tag()` sammelt alle Werte je Tag (jetzt 32 Einträge in `mineable/pickaxe`); neuer GameTest `pickaxeHarvests` |
| Turbinen-GameTest: Turbine stand still (blockiert) | **Testaufbau**: die Barrieredecke des 4 Blöcke hohen Testbereichs lag im Schaufelkreis der 5. Stufe (Radius 3,5) – originalgetreu blockiert | höhere Teststruktur `platform_9x8x9` |
| Neutronen-Fluorit-GameTest schlug etwa jedes 6. Mal fehl | **Testaufbau**: der Flugweg lief durch den Steinboden, der originalgetreu etwa 1 von 18 Neutronen je Block schluckt | Flugweg eine Ebene höher durch Luft; 3 Läufe hintereinander grün |
| Testwelt: Spieler starb an Strahlenkrankheit, danach brach das Öffnen einer GUI ab | **Testaufbau**: Atommüll in der Testwelt wirkt originalgetreu; der tote Spieler blieb im Spielstand | Testwelt-Spieler im Kreativmodus (Datapack-Tickfunktion), Welt wird vor dem Neuaufbau gelöscht |
| Testwelt: Abfallbehälter leer | **Testaufbau**: die Datapack-Befehle liefen, bevor die Chunks bei z ≥ 45 geladen waren, `fill`/`setblock` schlugen still fehl | Testwelt-Bereich per `forceload` gehalten, Aufbau 60 Ticks nach dem Laden wiederholt; GameTests `inventoryLoadsFromNbt`/`inventoryLoadsFromSetblock` bestätigen das Laden |
| Atommüllfass zerfiel beim ersten Tick | **Port-Randfall**: ohne gespeicherten Zeitstempel (Original 0) zählt die Echtzeit-Nachholung alle Ticks seit 1970 | ohne Zeitstempel wird nichts nachgeholt (Original-Verhalten nur für diesen Randfall geändert) |
| Strahlungs-GameTest mit Schwein schlug etwa jedes 4. Mal fehl | **Testaufbau**: die Explosion (Stärke 8) des Kernschmelze-Tests im selben Durchgang reichte bis in den Nachbar-Testbereich und tötete das Schwein | Kernschmelze-Test in eigenem Durchgang (`batch = "meltdown"`), 4 Läufe hintereinander grün |
| Generator-GameTest „Explosion beim Auflösen“: Absturz „Cannot get property formed … air“, danach hing der Testserver beim Speichern | **Port-Fehler**: nach der (originalgetreuen) Explosion las `breakMultiBlock` noch den Zustand inzwischen gesprengter Blöcke | Teile werden vor dem Lesen geprüft (`isPart`); GameTest grün, Server beendet sauber |
| Generator-GameTest RF: Leistung ≠ Turbinenleistung | **Fehler im Test**: der Generator rechnet wie im Original selbst ω·τ statt die Leistungszahl der Turbine zu übernehmen | Test prüft Leistung > 0 und RF/t = Leistung/520 |
| Schwungrad-GameTest: Werte ≠ Turbine | **Fehler im Test**: das Schwungrad übernimmt die Turbinenwerte in seinem eigenen Tick, also teils einen Turbinen-Tick zurück (originalgetreu) | Test erlaubt 5 % Abweichung, prüft die 12750-Nm-Grenze |
| Testwelt: Generator-Modell zeigte von der Turbine weg | **Testaufbau**: im Datapack Metadaten 0 gesetzt; beim Setzen mit Blick nach Westen (Ausrichtung des Generators) ergeben die Original-Metadaten 1 | Datapack nutzt 1; Draufsicht bestätigt die Lage |
| Testserver hing nach „All 171 required tests passed“ beim Beenden (Thread-Dump: `ChunkMap.processUnloads` plant Entladungen endlos neu) | **Port-Fehler (ElectriCraft-Kabelnetz)**: beim Entladen bauen sich Netze neu auf; `checkForWrappables` und die Knoten-Suche in `WireNetwork.addElement` fragten Nachbar-Block-Entities ohne `isLoaded` ab und luden dabei Chunks synchron nach | `isLoaded`-Prüfungen vor jedem Nachbarzugriff; danach 3 Läufe hintereinander sauber beendet (29–34 s) |
| Strahlungsreiniger-GameTest: kein Wasser aufgenommen | **Testaufbau**: der Blickstrahl der Testfigur traf vor der Wasserquelle einen Steinblock im Testbereich (Ursache nicht weiter untersucht) | Testfigur steht über der Quelle und blickt senkrecht nach unten; Trefferdiagnose bleibt in der Fehlermeldung |
| Solar-GameTests: kein Spiegel funktionierte | **Port-Fehler**: der Original-Startwert −1 für den höchsten Spiegel je Säule setzt y ≥ 0 voraus; in 1.21-Welten (Testwelt bei y = −59) blieb das Maximum −1 | Startwert `Integer.MIN_VALUE`; durch Diagnose im GameTest gefunden |
| Solar-GameTest zwei Türme: Turmfaktor 5 statt erwartet 4 | **Fehler im Test**: im Original-Zählschleifen-Ablauf verkürzt ein Spiegel neben dem zweiten Turmblock die wirksame Höhe nicht (Prüfung erst nach dem Hochzählen) | Erwartung 4 + 2·0,72 = 5,44 → 5 |
| Asset-Generator überschrieb das Spiegel-Bauteil-Item | **Port-Fehler (Namenskonflikt)**: Maschinen-ID `mirror` = Item-ID des Spiegel-Bauteils | Maschine heißt `solar_mirror` |
| GameTest-Server stürzte ab (NullPointerException in `GameTestInfo.tickInternal`) | **Fehler im Test**: `onEachTick` innerhalb eines `runAtTickTime`-Rückrufs registriert verändert die gerade durchlaufene Liste | Rückrufe vorab registriert, per Schalter aktiviert |
| VDG-GameTest: kein gezündetes TNT | **Testaufbau**: im Durchgang „meltdown“ warf die Kernschmelze nebenan Blöcke in den Testbereich | eigener Durchgang „vdg“; gezündetes TNT wird vor Testende entfernt |
| Testwelt: Elektrolyseur-GUI ohne Flüssigkeiten | **Testaufbau**: Datapack setzte den Block zweimal; `/setblock` mit gleichem Blockzustand übernimmt die NBT-Daten nicht (und Tanks brauchen das Format `{Fluid:{…}}`) | Datapack korrigiert; GameTest `loadsFromNbt` bestätigt das Laden |
| Hitzestrahl-GameTest: Sand verschwand ohne Leistung | **Testaufbau**: unter y = 1 hat die Test-Plattform keinen Boden (der Boden liegt eine Ebene tiefer), der Sand fiel herunter | Stützblöcke unter Sand und Kuh; durch Diagnoseausgabe je Tick gefunden |
| Hitzestrahl-GameTest: nur 218688 W | **Fehler im Test**: die Mikroturbine braucht wie im Original ~1930 Ticks bis zur vollen Drehzahl (+68 rad/s je Tick) | Zeitlimit 2400 Ticks |
| Kompaktor-GameTest: Eingang 0 rad/s | **Testaufbau**: das Stahlgetriebe nahm bei 4096 Nm Schaden, bevor der Druck reichte | Bedrock-Getriebe |
| Kompaktor-GameTest: 4 statt 2 Packeis | **Fehler im Test**: die Pressung lief schon während des Hochdrehens der Turbine; die Drehzahlprüfung schlug dabei fehl, danach lief ein zweiter Durchgang | je 1 Eis, nur das Drehmoment wird geprüft |
| Testwelt: Kühlschrank- und Kompaktor-GUI leer | **Testaufbau**: die Aufbaufunktion läuft zweimal; `/setblock` leert vor dem (scheiternden) Setzen des gleichen Blocks dessen Inventar (`Clearable`) | Blöcke mit Inventar werden vorher auf Luft gesetzt |
| Fusion: erster Lauf aller Tests sofort grün | **Prüfung der Prüfung**: Diagnoseausgabe alle 100 Ticks im Tokamak-Test | bestätigt: Solenoid erreicht die Magnete (Tick ~200), danach ~4 Fusionen je Tick bei ~430 Plasmabällen (gleich der Einspeisung von 4 Bällen je Tick) |
| Kompatibilitätswelt: Client-Absturz „Not building!“ im Spulenwickler-Renderer | **Port-Fehler**: der Renderer holte den zweiten Vertex-Puffer (ohne Flächen-Culling) schon vor dem Zeichnen; bei mehreren Maschinen im Bild beendete das den ersten Puffer | der zweite Puffer wird erst nach dem ersten Teil geholt; Testwelt danach ohne Fehler |
| Mekanism-Energiewürfel ohne FE-Capability | **Fremd-Mod/Testaufbau**: per Befehl gesetzt bietet der Würfel auf keiner Seite FE an (Mekanisms FE-Anbindung ist aktiv, Kabel und Maschinen-Seiten bieten sie an) | Energietest über Mekanism Universal Cable zwischen zwei RF-Batterien |

## 3. Client-Prüfung (T5) im Detail
- **Rendering:** Wellen (6 Materialien, vertikal, Kreuz), Getriebe 2/4/8/16 mit Lagerstufen-Textur, Schwungräder (7 Typen), Kegelrad, Kupplung, Dynamometer mit Anzeige, Wellenverbinder (normal/Bedrock), 7 Motoren. Alle mit Originaltexturen und den mechanisch übertragenen Originalmodellen; die Ausrichtung folgt den Metadaten.
- **Item-Darstellung:** In der Hand gehaltenes Getriebe erscheint als 3D-Modell (Hotbar und erste Person).
- **Synchronisation:** Client-Werte aus dem Log: DC → Holz 4:1 → Dynamometer `64 rad/s, 16 Nm, 1024 W`; Benzinmotor → Stahl 8:1 → Kegelrad → Dynamometer `64 rad/s, 1024 Nm, 65536 W`; Benzinmotor `512 rad/s, 128 Nm`, Treibstoff sinkt (100000 → 99780 mB); Rotationswinkel `phi` steigt (Animation läuft).
- **GUIs:** Benzinmotor (Slot, Treibstoffanzeige), Getriebe (Schmiermittel, Übersetzung, Modus, Leistung, Verschleiß), Kegelrad (farbige Seitenknöpfe, Nachbarblöcke), Wellenverbinder (9 Verhältnisse). Optisch mit den Original-Texturen.

## 4. Nicht durchgeführte oder nicht mögliche Prüfungen
- **Mehrspieler mit getrenntem Client und Server**: nicht getestet (nur Integrated Server). Die Pakete prüfen serverseitig Reichweite und Besitzerrechte.
- **Manuelles Spielen**: Bedienung per Maus (Schraubendreher, Transducer, GUI-Klicks) wurde nicht von Hand durchgespielt. GUIs wurden nur geöffnet und fotografiert; Knöpfe wurden nicht angeklickt.
- **Crafting im Spiel**: Rezepte sind nur per Datei-Test geprüft (T2). Dass sie in der Rezeptliste erscheinen, wurde nicht im Spiel geprüft. Maschinenrezepte sind standardmäßig gesperrt (siehe `08-implementation-status.md` §3).
- **Leistung bei vielen Maschinen**: nicht gemessen.
- **ReactorCraft**: Fusion: der Vorheizer erreicht im Test die 150 Mio. Grad nicht durch den Hitzestrahl (das dauert originalgetreu rund 19000 Ticks); geprüft sind die Heizrate je Tick und die Plasmaerzeugung ab der Zieltemperatur. Im Tokamak-Test und in der Testwelt hält der Test die VDG-Ladung (8192 C) und den Stickstoff nach, statt 36 VDGs mit Motoren zu betreiben; die VDG-Entladung in einen Magneten ist im Einzeltest mit echter Leistung geprüft. **ElectriCraft**: nur Kabelnetz, Generator und Motor (GameTests + Sichtprüfung); Generator-Explosion (> 400 MW) nicht getestet; Rezepte nicht im Spiel geprüft.
- **Vergleich mit einer laufenden 1.7.10-Instanz**: keine vorhanden; Sollwerte stammen aus dem Originalcode.
