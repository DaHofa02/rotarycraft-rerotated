# 02 – Feature-Matrix

Automatisch erzeugt aus den Registry-Enums der Originalquelltexte (`*/Registry/*.java`) und den `en_US.lang`-Dateien der Original-JARs. Spalte **Status** ist ein *Vorschlag* (Phase A/B); nichts davon ist implementiert.

Statuswerte: **Portieren** (1:1-Verhalten), **Portieren (angepasst)** (technische Anpassung nötig, Verhalten bleibt), **Ersetzt (Flattening)** (Metadaten-Konstrukt entfällt, Inhalt bleibt erhalten), **Ersetzt durch Vanilla/NeoForge**, **Optional (…)** (Soft-Dependency), **Nicht reproduzierbar** (Abhängigkeit existiert auf 1.21.1 nicht), **Unklar / Freigabe** (Entscheidung nötig).

Spalte **Test** benennt die geplante Prüfung (GameTest = `./gradlew runGameTestServer`, Datagen = Prüfung generierter Ressourcen, Unit = JUnit).

## Zusammenfassung

| Ursprung | Status | Anzahl |
|---|---|---|
| DragonAPI | Ersetzt durch BakedModel/BER/RenderType | 1 |
| DragonAPI | Ersetzt durch ModConfigSpec | 1 |
| DragonAPI | Ersetzt durch NeoForge | 1 |
| DragonAPI | Ersetzt durch NeoForge (Events, KeyMapping, TicketController) | 1 |
| DragonAPI | Ersetzt durch NeoForge/Vanilla | 1 |
| DragonAPI | Ersetzt durch Payload-API | 1 |
| DragonAPI | Nicht übernommen | 1 |
| DragonAPI | Nicht übernommen / Freigabe | 3 |
| DragonAPI | Nur benötigte Einträge, neu für 1.21.1 | 1 |
| DragonAPI | Nur für tatsächlich benötigte Ziele (JEI/Jade/CC) | 1 |
| DragonAPI | Portieren | 1 |
| DragonAPI | Portieren (angepasst) | 3 |
| DragonAPI | Teilweise durch NeoForge-Events ersetzt | 1 |
| DragonAPI | Teilweise ersetzt | 1 |
| ElectriCraft | Ersetzt (Flattening) | 1 |
| ElectriCraft | Freigabe: Vanilla | 1 |
| ElectriCraft | Nicht reproduzierbar | 7 |
| ElectriCraft | Portieren | 27 |
| ElectriCraft | Portieren (angepasst) | 29 |
| ElectriCraft | Teilweise / Freigabe | 1 |
| ReactorCraft | Ersetzt (Flattening) | 5 |
| ReactorCraft | Nicht reproduzierbar / Freigabe | 1 |
| ReactorCraft | Optional (JEI) | 1 |
| ReactorCraft | Portieren | 100 |
| ReactorCraft | Portieren (angepasst) | 119 |
| RotaryCraft | Ersetzt (Flattening) | 26 |
| RotaryCraft | Ersetzt durch Datapacks | 1 |
| RotaryCraft | Ersetzt durch Vanilla | 1 |
| RotaryCraft | Nicht reproduzierbar | 11 |
| RotaryCraft | Optional (CC: Tweaked) | 1 |
| RotaryCraft | Optional (JEI) | 1 |
| RotaryCraft | Optional (Jade) | 1 |
| RotaryCraft | Optional / Freigabe | 2 |
| RotaryCraft | Portieren | 282 |
| RotaryCraft | Portieren (angepasst) | 80 |
| RotaryCraft | Teilweise / Freigabe | 1 |
| RotaryCraft | Unklar / Freigabe | 6 |

**Gesamt: 723 Einträge.**

## RotaryCraft

| # | Kategorie | Original-ID | Name | Zielmod | Status | Test | Anmerkung |
|---|---|---|---|---|---|---|---|
| 1 | Maschine (BlockEntity) | `BEDROCKBREAKER` | Bedrock Breaker | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 2 | Maschine (BlockEntity) | `ENGINE` | Engine | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 3 | Maschine (BlockEntity) | `FLYWHEEL` | Flywheel | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 4 | Maschine (BlockEntity) | `SHAFT` | Shaft | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 5 | Maschine (BlockEntity) | `BEVELGEARS` | Bevel Gears | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 6 | Maschine (BlockEntity) | `GEARBOX` | Gearbox | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 7 | Maschine (BlockEntity) | `SPLITTER` | Shaft Junction | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 8 | Maschine (BlockEntity) | `FERMENTER` | Fermenter | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 9 | Maschine (BlockEntity) | `FLOODLIGHT` | Floodlight | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 10 | Maschine (BlockEntity) | `CLUTCH` | Clutch | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 11 | Maschine (BlockEntity) | `DYNAMOMETER` | Dynamometer | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 12 | Maschine (BlockEntity) | `GRINDER` | Grinder | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 13 | Maschine (BlockEntity) | `HEATRAY` | Heat Ray | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 14 | Maschine (BlockEntity) | `HOSE` | Lubricant Hose | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 15 | Maschine (BlockEntity) | `BORER` | Boring Machine | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 16 | Maschine (BlockEntity) | `LIGHTBRIDGE` | Light Bridge | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 17 | Maschine (BlockEntity) | `PUMP` | Pump | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 18 | Maschine (BlockEntity) | `PIPE` | Liquid Pipe | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 19 | Maschine (BlockEntity) | `RESERVOIR` | Reservoir | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 20 | Maschine (BlockEntity) | `AEROSOLIZER` | Aerosolizer | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 21 | Maschine (BlockEntity) | `EXTRACTOR` | Extractor | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 22 | Maschine (BlockEntity) | `PULSEJET` | Pulse Jet Furnace | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 23 | Maschine (BlockEntity) | `COMPACTOR` | Compactor | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 24 | Maschine (BlockEntity) | `FAN` | Fan | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 25 | Maschine (BlockEntity) | `FUELLINE` | Fuel Line | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 26 | Maschine (BlockEntity) | `FRACTIONATOR` | Fractionation Unit | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 27 | Maschine (BlockEntity) | `GPR` | Ground Penetrating Radar | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 28 | Maschine (BlockEntity) | `OBSIDIAN` | Obsidian Factory | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 29 | Maschine (BlockEntity) | `PILEDRIVER` | Pile Driver | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 30 | Maschine (BlockEntity) | `VACUUM` | Item Vacuum | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 31 | Maschine (BlockEntity) | `FIREWORK` | Fireworks Display | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 32 | Maschine (BlockEntity) | `SPRINKLER` | Sprinkler | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 33 | Maschine (BlockEntity) | `WOODCUTTER` | Woodcutter | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 34 | Maschine (BlockEntity) | `SPAWNERCONTROLLER` | Spawner Controller | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 35 | Maschine (BlockEntity) | `PLAYERDETECTOR` | Player Detector | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 36 | Maschine (BlockEntity) | `HEATER` | Heater | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 37 | Maschine (BlockEntity) | `BAITBOX` | Bait Box | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 38 | Maschine (BlockEntity) | `AUTOBREEDER` | Auto Breeder | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 39 | Maschine (BlockEntity) | `ECU` | Engine Control Unit | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 40 | Maschine (BlockEntity) | `SMOKEDETECTOR` | Smoke Detector | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 41 | Maschine (BlockEntity) | `MOBRADAR` | Mob Radar | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 42 | Maschine (BlockEntity) | `WINDER` | Coil Winder | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 43 | Maschine (BlockEntity) | `ADVANCEDGEARS` | Advanced Gears | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 44 | Maschine (BlockEntity) | `TNTCANNON` | TNT Cannon | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 45 | Maschine (BlockEntity) | `SONICWEAPON` | Sonic Weapon | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 46 | Maschine (BlockEntity) | `BLASTFURNACE` | Blast Furnace | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 47 | Maschine (BlockEntity) | `FORCEFIELD` | Force Field | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 48 | Maschine (BlockEntity) | `MUSICBOX` | Music Box | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 49 | Maschine (BlockEntity) | `SPILLER` | Liquid Spiller | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 50 | Maschine (BlockEntity) | `CHUNKLOADER` | Chunk Loader | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest | NeoForge TicketController statt Forge-Tickets |
| 51 | Maschine (BlockEntity) | `MOBHARVESTER` | Mob Harvester | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 52 | Maschine (BlockEntity) | `CCTV` | CCTV | rotarycraft | Portieren (angepasst) | GameTest: platzieren, Leistungs-/Funktionstest | Kamera-/Bildschirm-Rendering muss für 1.21-Renderpipeline neu umgesetzt werden |
| 53 | Maschine (BlockEntity) | `PROJECTOR` | Projector | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 54 | Maschine (BlockEntity) | `RAILGUN` | RailGun | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 55 | Maschine (BlockEntity) | `WEATHERCONTROLLER` | Silver Iodide Cannon | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 56 | Maschine (BlockEntity) | `REFRESHER` | Item Refresher | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 57 | Maschine (BlockEntity) | `FREEZEGUN` | Freeze Gun | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 58 | Maschine (BlockEntity) | `CAVESCANNER` | Cave Scanner | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 59 | Maschine (BlockEntity) | `SCALECHEST` | Scaleable Chest | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 60 | Maschine (BlockEntity) | `IGNITER` | Firestarter | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 61 | Maschine (BlockEntity) | `MAGNETIZER` | Magnetizing Unit | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 62 | Maschine (BlockEntity) | `CONTAINMENT` | Containment Field | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 63 | Maschine (BlockEntity) | `SCREEN` | CCTV Screen | rotarycraft | Portieren (angepasst) | GameTest: platzieren, Leistungs-/Funktionstest | Kamera-/Bildschirm-Rendering muss für 1.21-Renderpipeline neu umgesetzt werden |
| 64 | Maschine (BlockEntity) | `PURIFIER` | Steel Purifier | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 65 | Maschine (BlockEntity) | `LASERGUN` | Laser Gun | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 66 | Maschine (BlockEntity) | `ITEMCANNON` | Item Cannon | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 67 | Maschine (BlockEntity) | `LANDMINE` | Land Mine | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 68 | Maschine (BlockEntity) | `FRICTION` | Friction Heater | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 69 | Maschine (BlockEntity) | `BLOCKCANNON` | Block Cannon | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 70 | Maschine (BlockEntity) | `BUCKETFILLER` | Bucket Filler | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 71 | Maschine (BlockEntity) | `MIRROR` | Solar Mirror | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 72 | Maschine (BlockEntity) | `SOLARTOWER` | Solar Tower | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 73 | Maschine (BlockEntity) | `SPYCAM` | Aerial Camera | rotarycraft | Portieren (angepasst) | GameTest: platzieren, Leistungs-/Funktionstest | Kamera-/Bildschirm-Rendering muss für 1.21-Renderpipeline neu umgesetzt werden |
| 74 | Maschine (BlockEntity) | `SELFDESTRUCT` | Self Destruct Mechanism | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 75 | Maschine (BlockEntity) | `COOLINGFIN` | Cooling Fin | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 76 | Maschine (BlockEntity) | `WORKTABLE` | Worktable | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 77 | Maschine (BlockEntity) | `COMPRESSOR` | Air Compressor | rotarycraft | Optional / Freigabe | — | PneumaticCraft-Druck; PneumaticCraft: Repressurized 1.21.1 vorhanden, API ungeprüft |
| 78 | Maschine (BlockEntity) | `PNEUENGINE` | Pneumatic Engine | rotarycraft | Optional / Freigabe | — | PneumaticCraft-Druck; PneumaticCraft: Repressurized 1.21.1 vorhanden, API ungeprüft |
| 79 | Maschine (BlockEntity) | `DISPLAY` | Display Screen | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 80 | Maschine (BlockEntity) | `LAMP` | Bright Lamp | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 81 | Maschine (BlockEntity) | `EMP` | EMP Machine | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 82 | Maschine (BlockEntity) | `LINEBUILDER` | Block Ram | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 83 | Maschine (BlockEntity) | `BEAMMIRROR` | Beam Mirror | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 84 | Maschine (BlockEntity) | `MULTICLUTCH` | Multi-Directional Clutch | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 85 | Maschine (BlockEntity) | `TERRAFORMER` | Terraformer | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 86 | Maschine (BlockEntity) | `SORTING` | Sorting Machine | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 87 | Maschine (BlockEntity) | `FUELENHANCER` | Fuel Enhancer | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 88 | Maschine (BlockEntity) | `ARROWGUN` | Arrow Gun | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 89 | Maschine (BlockEntity) | `BOILER` | Friction Boiler | rotarycraft | Unklar / Freigabe | — | benötigt Fremd-Fluid 'steam' (Railcraft o. ä.) |
| 90 | Maschine (BlockEntity) | `STEAMTURBINE` | Steam Turbine | rotarycraft | Unklar / Freigabe | — | benötigt Fremd-Fluid 'steam' (Railcraft o. ä.) |
| 91 | Maschine (BlockEntity) | `FERTILIZER` | Fertilizer | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 92 | Maschine (BlockEntity) | `LAVAMAKER` | Rock Melter | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 93 | Maschine (BlockEntity) | `GENERATOR` | Electric Generator | rotarycraft | Nicht reproduzierbar | — | IC2-EU: kein IC2 für 1.21.1 NeoForge |
| 94 | Maschine (BlockEntity) | `ELECTRICMOTOR` | Electric Motor | rotarycraft | Nicht reproduzierbar | — | IC2-EU: kein IC2 für 1.21.1 NeoForge |
| 95 | Maschine (BlockEntity) | `VALVE` | Valve Pipe | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 96 | Maschine (BlockEntity) | `BYPASS` | Bypass Pipe | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 97 | Maschine (BlockEntity) | `SEPARATION` | Separation Pipe | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 98 | Maschine (BlockEntity) | `AGGREGATOR` | Dew Point Aggregator | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 99 | Maschine (BlockEntity) | `AIRGUN` | Air Pressure Gun | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 100 | Maschine (BlockEntity) | `SONICBORER` | Sonic Borer | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 101 | Maschine (BlockEntity) | `FUELENGINE` | Fuel Powered Engine | rotarycraft | Unklar / Freigabe | — | BuildCraft-Kraftstoffe; BuildCraft existiert nicht für 1.21.1 |
| 102 | Maschine (BlockEntity) | `FILLINGSTATION` | Filling Station | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 103 | Maschine (BlockEntity) | `BELT` | Belt Hub | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 104 | Maschine (BlockEntity) | `VANDEGRAFF` | Van De Graaff Generator | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 105 | Maschine (BlockEntity) | `DEFOLIATOR` | Defoliation Machine | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 106 | Maschine (BlockEntity) | `BIGFURNACE` | Lava Smeltery | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 107 | Maschine (BlockEntity) | `DISTILLER` | Liquid Distillery | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 108 | Maschine (BlockEntity) | `SUCTION` | Suction Pipe | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 109 | Maschine (BlockEntity) | `DYNAMO` | Rotational Dynamo | rotarycraft | Portieren (angepasst) | GameTest: platzieren, Leistungs-/Funktionstest | RF → NeoForge Energy (FE, Capabilities.EnergyStorage) |
| 110 | Maschine (BlockEntity) | `MAGNETIC` | Magnetostatic Engine | rotarycraft | Portieren (angepasst) | GameTest: platzieren, Leistungs-/Funktionstest | RF → NeoForge Energy (FE, Capabilities.EnergyStorage) |
| 111 | Maschine (BlockEntity) | `CRYSTALLIZER` | Fluid Crystallizer | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 112 | Maschine (BlockEntity) | `BUSCONTROLLER` | Shaft Power Bus Controller | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 113 | Maschine (BlockEntity) | `POWERBUS` | Shaft Power Bus | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 114 | Maschine (BlockEntity) | `PARTICLE` | Particle Display | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 115 | Maschine (BlockEntity) | `LAWNSPRINKLER` | Ground Sprinkler | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 116 | Maschine (BlockEntity) | `GRINDSTONE` | Grindstone | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 117 | Maschine (BlockEntity) | `BLOWER` | Pneumatic Item Pump | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 118 | Maschine (BlockEntity) | `PORTALSHAFT` | machine.portalshaft | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest | Dimensionsübergreifende Wellen; Verhalten bei Chunk-Entladung prüfen |
| 119 | Maschine (BlockEntity) | `REFRIGERATOR` | Refrigeration Unit | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 120 | Maschine (BlockEntity) | `GASTANK` | Fluid Compression Chamber | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 121 | Maschine (BlockEntity) | `CRAFTER` | AutoCrafting Unit | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 122 | Maschine (BlockEntity) | `COMPOSTER` | Composter | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 123 | Maschine (BlockEntity) | `ANTIAIR` | Anti-Air Gun | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 124 | Maschine (BlockEntity) | `PIPEPUMP` | Pipe Pump | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 125 | Maschine (BlockEntity) | `CHAIN` | Chain Drive | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 126 | Maschine (BlockEntity) | `CENTRIFUGE` | Centrifuge | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 127 | Maschine (BlockEntity) | `BEDPIPE` | Bedrock Pipe | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 128 | Maschine (BlockEntity) | `DRYING` | Drying Bed | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 129 | Maschine (BlockEntity) | `WETTER` | Liquefaction Machine | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 130 | Maschine (BlockEntity) | `DROPS` | Block Drop Processor | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 131 | Maschine (BlockEntity) | `ITEMFILTER` | Item Filter | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 132 | Maschine (BlockEntity) | `HYDRATOR` | Soil Hydrator | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 133 | Maschine (BlockEntity) | `FILLER` | Block Filler | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 134 | Maschine (BlockEntity) | `GATLING` | Gatling Gun | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 135 | Maschine (BlockEntity) | `SPILLWAY` | Spillway | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 136 | Maschine (BlockEntity) | `FLAMETURRET` | Flamethrower Turret | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 137 | Maschine (BlockEntity) | `BUNDLEDBUS` | Bundled-Signaled ME Extractor | rotarycraft | Nicht reproduzierbar | — | AE2 + ProjectRed Bundled Cable; Kombination auf 1.21.1 unbelegt |
| 138 | Maschine (BlockEntity) | `DISTRIBCLUTCH` | Shaft Distribution Clutch | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 139 | Maschine (BlockEntity) | `SPLITBELT` | Take-off Belt | rotarycraft | Portieren | GameTest: platzieren, Leistungs-/Funktionstest |  |
| 140 | Motor-Typ (Engine) | `DC` | Dc Engine | rotarycraft | Portieren | Unit: Drehmoment/ω-Werte; GameTest: Leistungsabgabe | Werte: 		256, 	4, 		 |
| 141 | Motor-Typ (Engine) | `WIND` | Wind Engine | rotarycraft | Portieren | Unit: Drehmoment/ω-Werte; GameTest: Leistungsabgabe | Werte: 	1024, 	8, 		 |
| 142 | Motor-Typ (Engine) | `STEAM` | Steam Engine | rotarycraft | Portieren | Unit: Drehmoment/ω-Werte; GameTest: Leistungsabgabe | Werte: 	512, 	32, 	 |
| 143 | Motor-Typ (Engine) | `GAS` | Gas Engine | rotarycraft | Portieren | Unit: Drehmoment/ω-Werte; GameTest: Leistungsabgabe | Werte: 	512, 	128, 	 |
| 144 | Motor-Typ (Engine) | `AC` | Ac Engine | rotarycraft | Portieren | Unit: Drehmoment/ω-Werte; GameTest: Leistungsabgabe | Werte: 		256, 	512, 	 |
| 145 | Motor-Typ (Engine) | `SPORT` | Sport Engine | rotarycraft | Portieren | Unit: Drehmoment/ω-Werte; GameTest: Leistungsabgabe | Werte: 	1024, 	256, 	 |
| 146 | Motor-Typ (Engine) | `HYDRO` | Hydro Engine | rotarycraft | Portieren | Unit: Drehmoment/ω-Werte; GameTest: Leistungsabgabe | Werte: 	32, 	16384, 	 |
| 147 | Motor-Typ (Engine) | `MICRO` | Micro Engine | rotarycraft | Portieren | Unit: Drehmoment/ω-Werte; GameTest: Leistungsabgabe | Werte: 	131072, 16, 	 |
| 148 | Motor-Typ (Engine) | `JET` | Jet Engine | rotarycraft | Portieren | Unit: Drehmoment/ω-Werte; GameTest: Leistungsabgabe | Werte: 	65536, 	1024, 	 |
| 149 | Getriebe-Material | `WOOD` | Wood Gearbox | rotarycraft | Portieren | Unit: Übersetzung/Verschleiß |  |
| 150 | Getriebe-Material | `STONE` | Stone Gearbox | rotarycraft | Portieren | Unit: Übersetzung/Verschleiß |  |
| 151 | Getriebe-Material | `STEEL` | Steel Gearbox | rotarycraft | Portieren | Unit: Übersetzung/Verschleiß |  |
| 152 | Getriebe-Material | `TUNGSTEN` | Tungsten Gearbox | rotarycraft | Portieren | Unit: Übersetzung/Verschleiß |  |
| 153 | Getriebe-Material | `DIAMOND` | Diamond Gearbox | rotarycraft | Portieren | Unit: Übersetzung/Verschleiß |  |
| 154 | Getriebe-Material | `BEDROCK` | Bedrock Gearbox | rotarycraft | Portieren | Unit: Übersetzung/Verschleiß |  |
| 155 | Getriebe-Material | `LIVINGWOOD` | Livingwood Gearbox | rotarycraft | Unklar / Freigabe | Unit: Übersetzung/Verschleiß | Botania (1.21.1-Verfügbarkeit nicht verifiziert) |
| 156 | Getriebe-Material | `LIVINGROCK` | Livingrock Gearbox | rotarycraft | Unklar / Freigabe | Unit: Übersetzung/Verschleiß | Botania (1.21.1-Verfügbarkeit nicht verifiziert) |
| 157 | Schwungrad-Material | `WOOD` | Wood Flywheel | rotarycraft | Portieren | Unit: Trägheit/Bruchgrenze |  |
| 158 | Schwungrad-Material | `STONE` | Stone Flywheel | rotarycraft | Portieren | Unit: Trägheit/Bruchgrenze |  |
| 159 | Schwungrad-Material | `IRON` | Iron Flywheel | rotarycraft | Portieren | Unit: Trägheit/Bruchgrenze |  |
| 160 | Schwungrad-Material | `GOLD` | Gold Flywheel | rotarycraft | Portieren | Unit: Trägheit/Bruchgrenze |  |
| 161 | Schwungrad-Material | `TUNGSTEN` | Tungsten Flywheel | rotarycraft | Portieren | Unit: Trägheit/Bruchgrenze |  |
| 162 | Schwungrad-Material | `DEPLETEDU` | Depletedu Flywheel | rotarycraft | Portieren | Unit: Trägheit/Bruchgrenze |  |
| 163 | Schwungrad-Material | `BEDROCK` | Bedrock Flywheel | rotarycraft | Portieren | Unit: Trägheit/Bruchgrenze |  |
| 164 | Block (Maschinen-Trägerblock) | `ADVANCEDGEAR` | intern: BlockAdvGear | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 165 | Block (Maschinen-Trägerblock) | `DIR` | intern: BlockDMachine | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 166 | Block (Maschinen-Trägerblock) | `DIRMODELINV` | intern: BlockDMIMachine | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 167 | Block (Maschinen-Trägerblock) | `DIRMODEL` | intern: BlockDMMachine | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 168 | Block (Maschinen-Trägerblock) | `ENGINE` | intern: BlockEngine | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 169 | Block (Maschinen-Trägerblock) | `GPR` | intern: BlockGPR | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 170 | Block (Maschinen-Trägerblock) | `FLYWHEEL` | intern: BlockFlywheel | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 171 | Block (Maschinen-Trägerblock) | `GEARBOX` | intern: BlockGearbox | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 172 | Block (Maschinen-Trägerblock) | `INV` | intern: BlockIMachine | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 173 | Block (Maschinen-Trägerblock) | `BASIC` | intern: BlockMachine | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 174 | Block (Maschinen-Trägerblock) | `MODELINV` | intern: BlockMIMachine | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 175 | Block (Maschinen-Trägerblock) | `MODEL` | intern: BlockMMachine | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 176 | Block (Maschinen-Trägerblock) | `PIPING` | intern: BlockPiping | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 177 | Block (Maschinen-Trägerblock) | `SHAFT` | intern: BlockShaft | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 178 | Block (Maschinen-Trägerblock) | `TRANS` | intern: BlockTrans | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 179 | Block (Maschinen-Trägerblock) | `MODELINV2` | intern: BlockMIMachine | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 180 | Block (Maschinen-Trägerblock) | `SOLAR` | intern: BlockSolar | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 181 | Block (Maschinen-Trägerblock) | `BCENGINE` | intern: BlockModEngine | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 182 | Block (Maschinen-Trägerblock) | `MODEL2` | intern: BlockMMachine | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 183 | Block (Maschinen-Trägerblock) | `DIRMODEL2` | intern: BlockDMMachine | rotarycraft | Ersetzt (Flattening) | — | Metadaten-Sammelblock → ein Block je Maschine |
| 184 | Block | `DECO` | block.deco | rotarycraft | Portieren | Datagen-Prüfung: Modell/Loot/Lang |  |
| 185 | Block | `CANOLA` | Canola Plant | rotarycraft | Portieren | Datagen-Prüfung: Modell/Loot/Lang |  |
| 186 | Block | `LIGHT` | Light Block | rotarycraft | Portieren | Datagen-Prüfung: Modell/Loot/Lang |  |
| 187 | Block | `BEAM` | Beam Block | rotarycraft | Portieren | Datagen-Prüfung: Modell/Loot/Lang |  |
| 188 | Block | `BRIDGE` | Bridge Block | rotarycraft | Portieren | Datagen-Prüfung: Modell/Loot/Lang |  |
| 189 | Block | `MININGPIPE` | Mining Pipe | rotarycraft | Portieren | Datagen-Prüfung: Modell/Loot/Lang |  |
| 190 | Block | `BLASTGLASS` | Blast Glass | rotarycraft | Portieren | Datagen-Prüfung: Modell/Loot/Lang |  |
| 191 | Block | `BLASTPANE` | Blast Glass Pane | rotarycraft | Portieren | Datagen-Prüfung: Modell/Loot/Lang |  |
| 192 | Block | `BEDROCKSLICE` | Bedrock Slice | rotarycraft | Portieren | Datagen-Prüfung: Modell/Loot/Lang |  |
| 193 | Block | `DECOTANK` | block.decotank | rotarycraft | Portieren | Datagen-Prüfung: Modell/Loot/Lang |  |
| 194 | Item | `SCREWDRIVER` | Screwdriver | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 195 | Item | `METER` | Angular Transducer | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 196 | Item | `DEBUG` | Magic Wand | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest | Entwicklerwerkzeug |
| 197 | Item | `WORLDEDIT` | WorldEdit Tool | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest | Admin-Werkzeug; Vanilla /fill deckt Teil ab – Freigabe |
| 198 | Item | `HANDBOOK` | RotaryCraft Handbook | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 199 | Item | `YEAST` | Yeast | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 200 | Item | `ETHANOL` | Ethanol Crystals | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 201 | Item | `CANOLA` | item.canola | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 202 | Item | `SPRING` | Wind Spring | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 203 | Item | `ULTRASOUND` | Ultrasound | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 204 | Item | `MOTION` | Motion Tracker | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 205 | Item | `VACUUM` | Vacuum | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 206 | Item | `STUNGUN` | Knockback Gun | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 207 | Item | `GRAVELGUN` | Gravel Gun | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 208 | Item | `FIREBALL` | Fireball Launcher | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 209 | Item | `BEDPICK` | Bedrock Pickaxe | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 210 | Item | `BEDAXE` | Bedrock Axe | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 211 | Item | `BEDSHOVEL` | Bedrock Shovel | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 212 | Item | `NVG` | Night Vision Goggles | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 213 | Item | `HANDCRAFT` | Handheld Crafting Tool | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 214 | Item | `RAILGUN` | RailGun Ammunition | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 215 | Item | `BUCKET` | Bucket | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 216 | Item | `TARGET` | TNT Cannon Targeting Aid | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 217 | Item | `IOGOGGLES` | I/O Goggles | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 218 | Item | `SLIDE` | Projector Slide | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 219 | Item | `KEY` | Cannon Key | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 220 | Item | `SHELL` | Explosive Shell | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 221 | Item | `MINECART` | Ethanol Minecart | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 222 | Item | `BEDHELM` | Bedrock Helmet | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 223 | Item | `BEDCHEST` | Bedrock Chestplate | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 224 | Item | `BEDLEGS` | Bedrock Leggings | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 225 | Item | `BEDBOOTS` | Bedrock Boots | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 226 | Item | `TILESELECTOR` | Tile Selector | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 227 | Item | `BEDPACK` | Bedrock Jetpack | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 228 | Item | `STEELPICK` | HSLA Steel Pickaxe | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 229 | Item | `STEELAXE` | HSLA Steel Axe | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 230 | Item | `STEELSHOVEL` | HSLA Steel Shovel | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 231 | Item | `STEELHELMET` | HSLA Steel Helmet | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 232 | Item | `STEELCHEST` | HSLA Steel Chestplate | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 233 | Item | `STEELLEGS` | HSLA Steel Leggings | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 234 | Item | `STEELBOOTS` | HSLA Steel Boots | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 235 | Item | `STRONGCOIL` | High-Strength Spring | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 236 | Item | `JETPACK` | Ethanol Jetpack | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 237 | Item | `PUMP` | Spring-Powered Pump | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 238 | Item | `JUMP` | Spring Boots | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 239 | Item | `BEDJUMP` | Bedrock Spring Boots | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 240 | Item | `FUEL` | Portable Backup Fuel Tank | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 241 | Item | `BEDHOE` | Bedrock Hoe | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 242 | Item | `STEELHOE` | HSLA Steel Hoe | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 243 | Item | `BEDSWORD` | Bedrock Sword | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 244 | Item | `STEELSWORD` | HSLA Steel Sword | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 245 | Item | `BEDSHEARS` | Bedrock Shears | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 246 | Item | `STEELSHEARS` | HSLA Steel Shears | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 247 | Item | `FLAMETHROWER` | item.flamethrower | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 248 | Item | `DISK` | Music Box Record | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 249 | Item | `UPGRADE` | item.engineupgrade | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 250 | Item | `CRAFTPATTERN` | Crafting Pattern | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 251 | Item | `BEDSICKLE` | Bedrock Scythe | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 252 | Item | `STEELSICKLE` | HSLA Steel Scythe | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 253 | Item | `GRAFTER` | Spring-Powered Grafter | rotarycraft | Nicht reproduzierbar | — | Forestry nicht für 1.21.1 NeoForge |
| 254 | Item | `BEDGRAFTER` | Bedrock Grafter | rotarycraft | Nicht reproduzierbar | — | Forestry nicht für 1.21.1 NeoForge |
| 255 | Item | `BEDSAW` | Bedrock Handsaw | rotarycraft | Nicht reproduzierbar | — | ForgeMicroblock existiert nicht für 1.21.1 |
| 256 | Item | `BEDREVEAL` | Bedrock Helmet of Revealing | rotarycraft | Nicht reproduzierbar | — | Thaumcraft existiert nicht für 1.21.1 |
| 257 | Item | `MACHINE` | item.placer | rotarycraft | Ersetzt (Flattening) | — | Platzier-Item → BlockItem je Maschine |
| 258 | Item | `ENGINE` | item.engplacer | rotarycraft | Ersetzt (Flattening) | — | Platzier-Item → BlockItem je Maschine |
| 259 | Item | `SHAFT` | item.shaftplacer | rotarycraft | Ersetzt (Flattening) | — | Platzier-Item → BlockItem je Maschine |
| 260 | Item | `GEARBOX` | item.gearplacer | rotarycraft | Ersetzt (Flattening) | — | Platzier-Item → BlockItem je Maschine |
| 261 | Item | `FLYWHEEL` | item.flyplacer | rotarycraft | Ersetzt (Flattening) | — | Platzier-Item → BlockItem je Maschine |
| 262 | Item | `ADVGEAR` | item.advplacer | rotarycraft | Ersetzt (Flattening) | — | Platzier-Item → BlockItem je Maschine |
| 263 | Item | `SHAFTCRAFT` | item.shaftcraft | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 264 | Item | `GEARCRAFT` | item.gearcraft | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 265 | Item | `FLYWHEELCRAFT` | Flywheel Core | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 266 | Item | `ENGINECRAFT` | item.enginecraft | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 267 | Item | `MISCCRAFT` | item.misccraft | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 268 | Item | `BORECRAFT` | item.borecraft | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 269 | Item | `EXTRACTS` | item.extracts | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 270 | Item | `COMPACTS` | item.compacts | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 271 | Item | `POWDERS` | item.powder | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 272 | Item | `MODINTERFACE` | item.modinterface | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 273 | Item | `MODEXTRACTS` | item.modextracts | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 274 | Item | `MODINGOTS` | item.modingots | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 275 | Item | `SPAWNER` | Monster Spawner | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 276 | Item | `STEELPACK` | HSLA Steel Jetpack | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 277 | Item | `CUSTOMEXTRACT` | item.customextract | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 278 | Item | `CUSTOMINGOT` | item.customingot | rotarycraft | Portieren (angepasst) | Datagen-Prüfung + ggf. GameTest | Metadaten-Sammelitem → ein Item je Untertyp |
| 279 | Item | `RANGEFINDER` | Range Finder | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 280 | Item | `GEARUPGRADE` | Integrated Gearbox Upgrade | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 281 | Item | `HELDPISTON` | Handheld Piston | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 282 | Item | `VOIDRAIL` | Voidmetal Railgun Ammo | rotarycraft | Portieren | Datagen-Prüfung + ggf. GameTest |  |
| 283 | Entity | `RAILGUN` | RailGun Shot | rotarycraft | Portieren | GameTest: Spawn + Treffer |  |
| 284 | Entity | `FREEZEGUN` | Freeze Gun Shot | rotarycraft | Portieren | GameTest: Spawn + Treffer |  |
| 285 | Entity | `ICE` | Ice Block | rotarycraft | Portieren | GameTest: Spawn + Treffer |  |
| 286 | Entity | `GASCART` | Gas Minecart | rotarycraft | Portieren | GameTest: Spawn + Treffer |  |
| 287 | Entity | `EXPLOSIVE` | Explosive Shell | rotarycraft | Portieren | GameTest: Spawn + Treffer |  |
| 288 | Entity | `SHOCKWAVE` | Shock Wave | rotarycraft | Portieren | GameTest: Spawn + Treffer |  |
| 289 | Entity | `DISCHARGE` | Discharge | rotarycraft | Portieren | GameTest: Spawn + Treffer |  |
| 290 | Entity | `CUSTOMTNT` | CustomTNT | rotarycraft | Portieren | GameTest: Spawn + Treffer |  |
| 291 | Entity | `FLAKSHOT` | Flak Shot | rotarycraft | Portieren | GameTest: Spawn + Treffer |  |
| 292 | Entity | `GATLING` | Gatling Round | rotarycraft | Portieren | GameTest: Spawn + Treffer |  |
| 293 | Entity | `FLAMESHOT` | Burning Liquid | rotarycraft | Portieren | GameTest: Spawn + Treffer |  |
| 294 | Sound | `ELECTRIC` | elecengine.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 295 | Sound | `WIND` | windengine.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 296 | Sound | `STEAM` | steamengine.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 297 | Sound | `CAR` | gasengine.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 298 | Sound | `HYDRO` | hydroengine.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 299 | Sound | `MICRO` | microengine.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 300 | Sound | `JET` | jetengine.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 301 | Sound | `KNOCKBACK` | knockback.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 302 | Sound | `PULSEJET` | pulsejet.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 303 | Sound | `PUMP` | pump.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 304 | Sound | `PILEDRIVER` | piledriver.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 305 | Sound | `SMOKE` | smokealarm.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 306 | Sound | `SPRINKLER` | sprinkler.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 307 | Sound | `FLYWHEEL` | flywheel.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 308 | Sound | `PROJECTOR` | projector.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 309 | Sound | `LOWBASS` | basslo.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 310 | Sound | `BASS` | bass.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 311 | Sound | `HIBASS` | basshi.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 312 | Sound | `LOWHARP` | harplo.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 313 | Sound | `HARP` | harp.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 314 | Sound | `HIHARP` | harphi.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 315 | Sound | `LOWPLING` | plinglo.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 316 | Sound | `PLING` | pling.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 317 | Sound | `HIPLING` | plinghi.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 318 | Sound | `FRICTION` | friction.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 319 | Sound | `CRAFT` | craft.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 320 | Sound | `AIRCOMP` | compress.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 321 | Sound | `PNEUMATIC` | pneu.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 322 | Sound | `LINEBUILDER` | linebuild.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 323 | Sound | `JETPACK` | pack.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 324 | Sound | `DIESEL` | diesel.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 325 | Sound | `BELT` | belt.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 326 | Sound | `FAN` | fan.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 327 | Sound | `SPARK` | spark.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 328 | Sound | `DYNAMO` | dynamo.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 329 | Sound | `INGESTION` | ingest_short.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 330 | Sound | `FRIDGE` | fridge.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 331 | Sound | `JETSTART` | jetstart.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 332 | Sound | `SONIC` | sonic.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 333 | Sound | `SHORTJET` | shortjet.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 334 | Sound | `AFTERBURN` | afterburner.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 335 | Sound | `RUMBLE` | rumble2.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 336 | Sound | `COIL` | coil.ogg (Loop) | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 337 | Sound | `GATLINGRELOAD` | gatlingreload.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 338 | Sound | `GATLING` | gatling.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 339 | Sound | `FLAMETURRET` | flameturret.ogg | rotarycraft | Portieren | sounds.json-Prüfung |  |
| 340 | Fluid | `rc ethanol` | rc ethanol | rotarycraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → NeoForge FluidType |
| 341 | Fluid | `rc jet fuel` | rc jet fuel | rotarycraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → NeoForge FluidType |
| 342 | Fluid | `rc lubricant` | rc lubricant | rotarycraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → NeoForge FluidType |
| 343 | Fluid | `rc liquid nitrogen` | rc liquid nitrogen | rotarycraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → NeoForge FluidType |
| 344 | Fluid | `molten hsla` | molten hsla | rotarycraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → NeoForge FluidType |
| 345 | Fluid | `poison` | poison | rotarycraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → NeoForge FluidType; Herkunft 'poison' unklar (Fluid ohne Block?) |
| 346 | MobEffect | `FREEZE` | Freeze (FreezePotion) | rotarycraft | Portieren | GameTest: Effekt anwenden | Potion-ID-Konfig entfällt (Registry) |
| 347 | MobEffect | `DEAFNESS` | Deafness (PotionDeafness) | rotarycraft | Portieren (angepasst) | Client-Test manuell | Stummschalten über PlaySoundEvent |
| 348 | Weltgenerierung | `EXTRAIRON` | Zusätzliches Eisenerz (Config 'Iron Ore Density') | rotarycraft | Portieren (angepasst) | Datagen: placed_feature vorhanden | IWorldGenerator → Biome Modifier + PlacedFeature |
| 349 | Pflanze | `CANOLA` | Canola-Feldfrucht | rotarycraft | Portieren | GameTest: Wachstum |  |
| 350 | Advancement | `RCUSEBOOK` | Actually Doing The Research | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 351 | Advancement | `DUMBEXTRACTOR` | Not How It Works | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 352 | Advancement | `MAKESTEEL` | Steelmaker | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 353 | Advancement | `FAILSTEEL` | Remedial Materials Science | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 354 | Advancement | `WORKTABLE` | Constructive | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 355 | Advancement | `MAKEYEAST` | Fermenter | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 356 | Advancement | `EXTRACTOR` | Multiplier | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 357 | Advancement | `PCB` | Advanced Control | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 358 | Advancement | `PUMP` | Pumped | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 359 | Advancement | `GPR` | Sneak Peek | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 360 | Advancement | `BORER` | Getting Bored | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 361 | Advancement | `JETFUEL` | Liquid Power | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 362 | Advancement | `RECYCLE` | Yay for Recycling | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 363 | Advancement | `JETENGINE` | A Matter of Thrust | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 364 | Advancement | `MAKERAILGUN` | Overkill | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 365 | Advancement | `SUCKEDINTOJET` | This Really Sucks | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 366 | Advancement | `BEDROCKBREAKER` | Unbreakable? Really? | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 367 | Advancement | `STEAMENGINE` | I Think I Can | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 368 | Advancement | `STEELSHAFT` | Engaged | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 369 | Advancement | `CVT` | Versatility | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 370 | Advancement | `BEDROCKSHAFT` | Now This Is Unbreakable | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 371 | Advancement | `BEDROCKTOOLS` | No More Tool Replacement! | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 372 | Advancement | `JETCHICKEN` | Doing It Wrong | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 373 | Advancement | `JETFAIL` | ...Oops | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 374 | Advancement | `LIGHTFALL` | Long Way Down | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 375 | Advancement | `SPRINKLER` | Green Thumb | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 376 | Advancement | `FLOODLIGHT` | Illuminating | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 377 | Advancement | `DAMAGEGEARS` | Grind My Gears | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 378 | Advancement | `DIAMONDGEARS` | Efficiency | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 379 | Advancement | `MRADS32` | Overspeed | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 380 | Advancement | `GIGAWATT` | Overpowered | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 381 | Advancement | `RAILDRAGON` | Blown Out Of The Sky | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 382 | Advancement | `RAILKILLED` | Too Close For Comfort | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 383 | Advancement | `GRAVELGUN` | Sniped | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 384 | Advancement | `LANDMINE` | Watch Your Step | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 385 | Advancement | `NETHERHEATRAY` | Boom Miner | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 386 | Advancement | `GPRSPAWNER` | Buried Treasure | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 387 | Advancement | `GPRENDPORTAL` | Who Needs Ender Eyes? | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 388 | Advancement | `CUTKNOT` | Cutting the Knot | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 389 | Advancement | `RAREEXTRACT` | Fortune XVIII | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 390 | Advancement | `MASSIVEHIT` | Hand Cannon | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 391 | Advancement | `OVERPRESSURE` | Into The Red | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 392 | Advancement | `DOUBLEKILL` | Two birds, one stone | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 393 | Advancement | `INSANITY` | Insanity | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 394 | Advancement | `INSTANTBED` | Insanity II | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 395 | Advancement | `PULSEFIRE` | Pulse Jet Arsonist | rotarycraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 396 | Handbuch | `HANDBOOK` | RotaryCraft Handbook (249 Einträge in HandbookRegistry) | rotarycraft | Portieren (angepasst) | Client-Test manuell | XML-Texte aus Resources/*.xml weiterverwenden |
| 397 | Befehl | `findmachines` | /findmachines | rotarycraft | Portieren (angepasst) | Unit: Brigadier-Parse | CommandBase → Brigadier (RegisterCommandsEvent) |
| 398 | Konfiguration | `ConfigRegistry` | 91 Optionen | rotarycraft | Portieren (angepasst) | Config-Ladetest | ModConfigSpec; reine Fremdmod-Optionen entfallen |
| 399 | Maschinenrezepte | `RecipeManagers` | Grinder, Extractor, Blast Furnace, Compactor, Centrifuge, Crystallizer, Drying Bed, Friction Heater, Lava Maker, Magnetizer, Pulse Jet, Wetter, Worktable | rotarycraft | Portieren (angepasst) | Datagen + Unit: Rezeptauflösung | Java-Rezeptlisten → RecipeType/RecipeSerializer + Datapack-JSON |
| 400 | Benutzerrezepte | `CustomRecipeList` | *.recipes_* Dateien (CustomRecipeExamples) | rotarycraft | Ersetzt durch Vanilla | — | Datapacks übernehmen diese Aufgabe |
| 401 | Coremod-Patch | `FURNACEINTERCEPT` | Friction Heater übernimmt Vanilla-Ofen-Tick | rotarycraft | Portieren (angepasst) | GameTest: Ofen mit Friction Heater | ASM → Mixin oder alternative Lösung; siehe 05 |
| 402 | Inter-Mod-API | `API` | Shaft-Power-API, IO-API, Reservoir/Worktable/Extract-API, 20 API-Events | rotarycraft | Portieren (angepasst) | Unit + GameTest | Lizenz: API-Code darf ohne Rücksprache nicht verändert werden – siehe 03 |
| 403 | Mod-Integration | `—` | NEI-Rezeptanzeigen (24 Handler) | rotarycraft | Optional (JEI) | Ladetest ohne Fremdmod | JEI 1.21.1 vorhanden |
| 404 | Mod-Integration | `—` | WAILA-Tooltips | rotarycraft | Optional (Jade) | Ladetest ohne Fremdmod | Jade 1.21.1 vorhanden |
| 405 | Mod-Integration | `—` | ComputerCraft/OpenComputers Lua (34 Methoden) | rotarycraft | Optional (CC: Tweaked) | Ladetest ohne Fremdmod | OpenComputers nicht für 1.21.1 |
| 406 | Mod-Integration | `—` | MineTweaker (Friction/Grinder/PulseJet) | rotarycraft | Ersetzt durch Datapacks | — | Rezepte werden datengetrieben |
| 407 | Mod-Integration | `—` | Thaumcraft Aspekte/Infusion, Foki | rotarycraft | Nicht reproduzierbar | — | Thaumcraft fehlt |
| 408 | Mod-Integration | `—` | Forestry Bienen/Grafter, Canola-Biene | rotarycraft | Nicht reproduzierbar | — | Forestry fehlt |
| 409 | Mod-Integration | `—` | AgriCraft Canola | rotarycraft | Nicht reproduzierbar | — | AgriCraft 1.21.1 nicht verifiziert |
| 410 | Mod-Integration | `—` | ChromatiCraft-Interaktionen | rotarycraft | Nicht reproduzierbar | — | ChromatiCraft (Reika) nur 1.7.10 |
| 411 | Mod-Integration | `—` | AE2-Interaktionen | rotarycraft | Unklar / Freigabe | — | AE2 1.21.1 vorhanden, Umfang prüfen |
| 412 | Mod-Integration | `—` | IC2/GregTech/Thermal/EnderIO/Mekanism/Railcraft u. a. | rotarycraft | Teilweise / Freigabe | — | je Mod getrennt prüfen; viele fehlen auf 1.21.1 |
| 413 | Mod-Integration | `—` | Modded-Erze für Extractor (ModOreList) | rotarycraft | Portieren (angepasst) | — | über c:-Tags statt Ore Dictionary |

## ReactorCraft

| # | Kategorie | Original-ID | Name | Zielmod | Status | Test | Anmerkung |
|---|---|---|---|---|---|---|---|
| 414 | Maschine (BlockEntity) | `FUEL` | Fuel Core | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 415 | Maschine (BlockEntity) | `CONTROL` | Control Rod | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 416 | Maschine (BlockEntity) | `COOLANT` | Coolant Cell | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 417 | Maschine (BlockEntity) | `CPU` | Central Control | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 418 | Maschine (BlockEntity) | `TURBINECORE` | Turbine | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 419 | Maschine (BlockEntity) | `CONDENSER` | Condenser | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 420 | Maschine (BlockEntity) | `STEAMLINE` | Steam Line | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 421 | Maschine (BlockEntity) | `FLUIDEXTRACTOR` | Centrifugal Fluid Extractor | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 422 | Maschine (BlockEntity) | `CENTRIFUGE` | Isotope Centrifuge | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 423 | Maschine (BlockEntity) | `PROCESSOR` | Uranium Processor | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 424 | Maschine (BlockEntity) | `WASTECONTAINER` | Spent Fuel Container | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 425 | Maschine (BlockEntity) | `BOILER` | Steam Boiler | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 426 | Maschine (BlockEntity) | `GRATE` | Steam Grate | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 427 | Maschine (BlockEntity) | `PUMP` | Pressurizer | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 428 | Maschine (BlockEntity) | `SYNTHESIZER` | Fluid Synthesizer | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 429 | Maschine (BlockEntity) | `MAGNET` | Toroid Magnet | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 430 | Maschine (BlockEntity) | `ELECTROLYZER` | Electrolyzer | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 431 | Maschine (BlockEntity) | `TRITIZER` | Neutron Irradiation Chamber | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 432 | Maschine (BlockEntity) | `BREEDER` | Breeder Reactor Core | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 433 | Maschine (BlockEntity) | `SODIUMBOILER` | Sodium Heater | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 434 | Maschine (BlockEntity) | `EXCHANGER` | Heat Exchanger | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 435 | Maschine (BlockEntity) | `STORAGE` | Nuclear Waste Disposal Drum | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 436 | Maschine (BlockEntity) | `INJECTOR` | Fusion Plasma Injector | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 437 | Maschine (BlockEntity) | `HEATER` | Hydrogen Preheater | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 438 | Maschine (BlockEntity) | `GASPIPE` | Gas Duct | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 439 | Maschine (BlockEntity) | `MAGNETPIPE` | Magnetic Containment Pipe | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 440 | Maschine (BlockEntity) | `ABSORBER` | Neutron Absorber | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 441 | Maschine (BlockEntity) | `SOLENOID` | Solenoid Magnet | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 442 | Maschine (BlockEntity) | `COLLECTOR` | Gas Collector | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 443 | Maschine (BlockEntity) | `PEBBLEBED` | Pebble Bed Reactor Core | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 444 | Maschine (BlockEntity) | `CO2HEATER` | Carbon Dioxide Heat Exchanger | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 445 | Maschine (BlockEntity) | `FLYWHEEL` | Turbine Flywheel | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 446 | Maschine (BlockEntity) | `REFLECTOR` | Neutron Reflector | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 447 | Maschine (BlockEntity) | `GENERATOR` | Turbine Generator | reactorcraft | Portieren (angepasst) | GameTest: platzieren + Funktion | Ausgabe RF→FE, ElectriCraft optional; EU entfällt |
| 448 | Maschine (BlockEntity) | `MARKER` | Tokamak Blueprint Highlighter | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 449 | Maschine (BlockEntity) | `TURBINEMETER` | Turbine Dynamometer | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 450 | Maschine (BlockEntity) | `BIGTURBINE` | High-Pressure Turbine | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 451 | Maschine (BlockEntity) | `DIFFUSER` | Steam Diffuser | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 452 | Maschine (BlockEntity) | `THORIUM` | Thorium Fuel Core | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 453 | Maschine (BlockEntity) | `WASTEPIPE` | Nuclear Waste Duct | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 454 | Maschine (BlockEntity) | `FUELDUMP` | Fuel Dump Valve | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 455 | Maschine (BlockEntity) | `SOLAR` | Solar Tower Sodium Heat Exchanger | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 456 | Maschine (BlockEntity) | `SOLARTOP` | Solar Tower Sodium Cycler | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 457 | Maschine (BlockEntity) | `MINITURBINE` | machine.miniturbine | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 458 | Maschine (BlockEntity) | `HEATPIPE` | Heat Pipe | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 459 | Maschine (BlockEntity) | `WASTEDECAYER` | Forced Fission Chamber | reactorcraft | Portieren | GameTest: platzieren + Funktion |  |
| 460 | Block | `REACTOR` | Reactor | reactorcraft | Ersetzt (Flattening) | Datagen-Prüfung | Metadaten-Sammelblock → ein Block je Maschine |
| 461 | Block | `MATS` | Reactor Materials | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 462 | Block | `CORIUMFLOWING` | Molten Corium (Flowing) | reactorcraft | Portieren | Datagen-Prüfung |  |
| 463 | Block | `MODELREACTOR` | ReactorModelled | reactorcraft | Ersetzt (Flattening) | Datagen-Prüfung | Metadaten-Sammelblock → ein Block je Maschine |
| 464 | Block | `MACHINE` | Machine | reactorcraft | Ersetzt (Flattening) | Datagen-Prüfung | Metadaten-Sammelblock → ein Block je Maschine |
| 465 | Block | `MODELMACHINE` | MachineModelled | reactorcraft | Ersetzt (Flattening) | Datagen-Prüfung | Metadaten-Sammelblock → ein Block je Maschine |
| 466 | Block | `ORE` | Ore | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 467 | Block | `FLUORITE` | Fluorite | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 468 | Block | `FLUORITEORE` | Fluorite Ore | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 469 | Block | `STEAM` | Steam | reactorcraft | Portieren | Datagen-Prüfung |  |
| 470 | Block | `DUCT` | Duct | reactorcraft | Portieren | Datagen-Prüfung |  |
| 471 | Block | `LINE` | Line | reactorcraft | Portieren | Datagen-Prüfung |  |
| 472 | Block | `INJECTORMULTI` | multiblock.injector | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 473 | Block | `HEATERMULTI` | multiblock.heater | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 474 | Block | `SOLENOIDMULTI` | multiblock.solenoid | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 475 | Block | `GENERATORMULTI` | multiblock.generator | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 476 | Block | `TURBINEMULTI` | multiblock.turbine | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 477 | Block | `FLYWHEELMULTI` | multiblock.flywheel | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 478 | Block | `LAMP` | Tritium Lamp | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 479 | Block | `THORIUM` | Thorium Fuel | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 480 | Block | `CHLORINE` | Chlorine Gas | reactorcraft | Portieren | Datagen-Prüfung |  |
| 481 | Block | `HF` | Fluorine Gas | reactorcraft | Portieren | Datagen-Prüfung |  |
| 482 | Item | `WASTE` | Nuclear Waste | reactorcraft | Portieren | Datagen-Prüfung |  |
| 483 | Item | `FUEL` | Uranium Fuel Pellet | reactorcraft | Portieren | Datagen-Prüfung |  |
| 484 | Item | `DEPLETED` | Depleted Uranium | reactorcraft | Portieren | Datagen-Prüfung |  |
| 485 | Item | `PLACER` | Part Placer | reactorcraft | Ersetzt (Flattening) | Datagen-Prüfung | → BlockItems |
| 486 | Item | `BUCKET` | Heavy Water Bucket | reactorcraft | Portieren | Datagen-Prüfung |  |
| 487 | Item | `RAW` | Raw Materials | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Sammelitem → ein Item je Untertyp |
| 488 | Item | `FLUORITE` | Fluorite | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Sammelitem → ein Item je Untertyp |
| 489 | Item | `INGOTS` | Ingots | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Sammelitem → ein Item je Untertyp |
| 490 | Item | `CANISTER` | Fluid Canister | reactorcraft | Portieren | Datagen-Prüfung |  |
| 491 | Item | `GOGGLES` | Radiation Goggles | reactorcraft | Portieren | Datagen-Prüfung |  |
| 492 | Item | `CRAFTING` | Crafting item | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Sammelitem → ein Item je Untertyp |
| 493 | Item | `PLUTONIUM` | Plutonium Fuel Pellet | reactorcraft | Portieren | Datagen-Prüfung |  |
| 494 | Item | `BREEDERFUEL` | Breeder Reactor Fuel | reactorcraft | Portieren | Datagen-Prüfung |  |
| 495 | Item | `CLEANUP` | Radiation Cleanup Tool | reactorcraft | Portieren | Datagen-Prüfung |  |
| 496 | Item | `MAGNET` | Permanent Magnet | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Sammelitem → ein Item je Untertyp |
| 497 | Item | `REMOTE` | Reactor CPU Remote Control | reactorcraft | Portieren | Datagen-Prüfung |  |
| 498 | Item | `PELLET` | TRISO Fuel Pellet | reactorcraft | Portieren | Datagen-Prüfung |  |
| 499 | Item | `OLDPELLET` | Depleted TRISO Fuel | reactorcraft | Portieren | Datagen-Prüfung |  |
| 500 | Item | `BOOK` | ReactorCraft Handbook | reactorcraft | Portieren | Datagen-Prüfung |  |
| 501 | Item | `HAZHELMET` | Hazmat Helmet | reactorcraft | Portieren | Datagen-Prüfung |  |
| 502 | Item | `HAZCHEST` | Hazmat Chest | reactorcraft | Portieren | Datagen-Prüfung |  |
| 503 | Item | `HAZLEGS` | Hazmat Legs | reactorcraft | Portieren | Datagen-Prüfung |  |
| 504 | Item | `HAZBOOTS` | Hazmat Boots | reactorcraft | Portieren | Datagen-Prüfung |  |
| 505 | Item | `GEIGER` | Geiger Counter | reactorcraft | Portieren | Datagen-Prüfung |  |
| 506 | Item | `IRONFINDER` | Magnetic Ore Finder | reactorcraft | Portieren | Datagen-Prüfung |  |
| 507 | Crafting-Zwischenprodukt | `CANISTER` | Canister | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 508 | Crafting-Zwischenprodukt | `ROD` | Rod | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 509 | Crafting-Zwischenprodukt | `TANK` | Tank | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 510 | Crafting-Zwischenprodukt | `ALLOY` | Alloy | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 511 | Crafting-Zwischenprodukt | `BACKING` | Backing | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 512 | Crafting-Zwischenprodukt | `MAGNETIC` | Magnetic | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 513 | Crafting-Zwischenprodukt | `MAGNETCORE` | Magnetcore | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 514 | Crafting-Zwischenprodukt | `COOLANT` | Coolant | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 515 | Crafting-Zwischenprodukt | `WIRE` | Wire | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 516 | Crafting-Zwischenprodukt | `SHIELD` | Shield | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 517 | Crafting-Zwischenprodukt | `FERROINGOT` | Ferroingot | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 518 | Crafting-Zwischenprodukt | `HYSTERESIS` | Hysteresis | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 519 | Crafting-Zwischenprodukt | `HYSTERESISRING` | Hysteresisring | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 520 | Crafting-Zwischenprodukt | `GRAPHITE` | Graphite | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 521 | Crafting-Zwischenprodukt | `UDUST` | Udust | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 522 | Crafting-Zwischenprodukt | `FABRIC` | Fabric | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 523 | Crafting-Zwischenprodukt | `CARBIDEFLAKES` | Carbideflakes | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 524 | Crafting-Zwischenprodukt | `CARBIDE` | Carbide | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 525 | Crafting-Zwischenprodukt | `TURBCORE` | Turbcore | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | eigenes Item statt Metadaten |
| 526 | Erz + Weltgenerierung | `FLUORITE` | Fluorite | reactorcraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier | Dimension: Oberwelt |
| 527 | Erz + Weltgenerierung | `PITCHBLENDE` | Pitchblende | reactorcraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier | Dimension: Oberwelt |
| 528 | Erz + Weltgenerierung | `CADMIUM` | Cadmium Ore | reactorcraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier | Dimension: Oberwelt |
| 529 | Erz + Weltgenerierung | `INDIUM` | Indium Ore | reactorcraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier | Dimension: Oberwelt |
| 530 | Erz + Weltgenerierung | `SILVER` | Silver Ore | reactorcraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier | Dimension: Oberwelt; Doppelung mit ElectriCraft-Silber → gemeinsamer Tag c:ores/silver |
| 531 | Erz + Weltgenerierung | `ENDBLENDE` | Pitchblende | reactorcraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier | Dimension: End |
| 532 | Erz + Weltgenerierung | `AMMONIUM` | Ammonium Chloride | reactorcraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier | Dimension: Nether |
| 533 | Erz + Weltgenerierung | `CALCITE` | Calcite | reactorcraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier | Dimension: Oberwelt |
| 534 | Erz + Weltgenerierung | `MAGNETITE` | Magnetite Ore | reactorcraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier | Dimension: Oberwelt |
| 535 | Erz + Weltgenerierung | `THORIUM` | Thorite | reactorcraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier | Dimension: Nether |
| 536 | Fluorit-Farbe | `BLUE` | Blue Fluorite (Erz, Kristall, Block, Lampe) | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten → eigene Blöcke/Items je Farbe |
| 537 | Fluorit-Farbe | `PINK` | Pink Fluorite (Erz, Kristall, Block, Lampe) | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten → eigene Blöcke/Items je Farbe |
| 538 | Fluorit-Farbe | `ORANGE` | Orange Fluorite (Erz, Kristall, Block, Lampe) | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten → eigene Blöcke/Items je Farbe |
| 539 | Fluorit-Farbe | `MAGENTA` | Magenta Fluorite (Erz, Kristall, Block, Lampe) | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten → eigene Blöcke/Items je Farbe |
| 540 | Fluorit-Farbe | `GREEN` | Green Fluorite (Erz, Kristall, Block, Lampe) | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten → eigene Blöcke/Items je Farbe |
| 541 | Fluorit-Farbe | `RED` | Red Fluorite (Erz, Kristall, Block, Lampe) | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten → eigene Blöcke/Items je Farbe |
| 542 | Fluorit-Farbe | `WHITE` | White Fluorite (Erz, Kristall, Block, Lampe) | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten → eigene Blöcke/Items je Farbe |
| 543 | Fluorit-Farbe | `YELLOW` | Yellow Fluorite (Erz, Kristall, Block, Lampe) | reactorcraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten → eigene Blöcke/Items je Farbe |
| 544 | Material-Block | `CONCRETE` | Concrete | reactorcraft | Portieren | Datagen-Prüfung |  |
| 545 | Material-Block | `SLAG` | Corium | reactorcraft | Portieren | Datagen-Prüfung |  |
| 546 | Material-Block | `CALCITE` | Calcite Block | reactorcraft | Portieren | Datagen-Prüfung | Namenskonflikt mit Vanilla-Calcit (anderer Block) – Freigabe |
| 547 | Material-Block | `SCRUBBER` | Steam Scrubber | reactorcraft | Portieren | Datagen-Prüfung |  |
| 548 | Material-Block | `LODESTONE` | Lodestone Block | reactorcraft | Portieren | Datagen-Prüfung | Namenskonflikt mit Vanilla-Lodestone (anderer Block) |
| 549 | Material-Block | `GRAPHITE` | Graphite Block | reactorcraft | Portieren | Datagen-Prüfung |  |
| 550 | Entity | `NEUTRON` | Neutron | reactorcraft | Portieren | GameTest: Neutronenfluss/Strahlung |  |
| 551 | Entity | `RADIATION` | Radiation | reactorcraft | Portieren | GameTest: Neutronenfluss/Strahlung |  |
| 552 | Entity | `PLASMA` | Plasma | reactorcraft | Portieren | GameTest: Neutronenfluss/Strahlung |  |
| 553 | Entity | `FUSION` | Fusion | reactorcraft | Portieren | GameTest: Neutronenfluss/Strahlung |  |
| 554 | Multiblock | `HEATER` | Heater (Multiblock-Struktur) | reactorcraft | Portieren | GameTest: Strukturerkennung |  |
| 555 | Multiblock | `GENERATOR` | Generator (Multiblock-Struktur) | reactorcraft | Portieren | GameTest: Strukturerkennung |  |
| 556 | Multiblock | `INJECTOR` | Injector (Multiblock-Struktur) | reactorcraft | Portieren | GameTest: Strukturerkennung |  |
| 557 | Multiblock | `SOLENOID` | Solenoid (Multiblock-Struktur) | reactorcraft | Portieren | GameTest: Strukturerkennung |  |
| 558 | Multiblock | `HPTURBINE` | Hpturbine (Multiblock-Struktur) | reactorcraft | Portieren | GameTest: Strukturerkennung |  |
| 559 | Multiblock | `FLYWHEEL` | Flywheel (Multiblock-Struktur) | reactorcraft | Portieren | GameTest: Strukturerkennung |  |
| 560 | Reaktortyp | `FISSION` | Fission | reactorcraft | Portieren | GameTest: Betrieb + Abschaltung |  |
| 561 | Reaktortyp | `BREEDER` | Breeder | reactorcraft | Portieren | GameTest: Betrieb + Abschaltung |  |
| 562 | Reaktortyp | `HTGR` | Htgr | reactorcraft | Portieren | GameTest: Betrieb + Abschaltung |  |
| 563 | Reaktortyp | `THORIUM` | Thorium | reactorcraft | Portieren | GameTest: Betrieb + Abschaltung |  |
| 564 | Reaktortyp | `FUSION` | Fusion | reactorcraft | Portieren | GameTest: Betrieb + Abschaltung |  |
| 565 | Reaktortyp | `SOLAR` | Solar | reactorcraft | Portieren | GameTest: Betrieb + Abschaltung |  |
| 566 | Strahlungsschild | `STEEL` | Steel | reactorcraft | Portieren (angepasst) | Unit: Abschirmwerte |  |
| 567 | Strahlungsschild | `CONCRETE` | Concrete | reactorcraft | Portieren (angepasst) | Unit: Abschirmwerte |  |
| 568 | Strahlungsschild | `WATER` | Water | reactorcraft | Portieren (angepasst) | Unit: Abschirmwerte |  |
| 569 | Strahlungsschild | `BEDINGOT` | Bedrock Ingot | reactorcraft | Portieren (angepasst) | Unit: Abschirmwerte |  |
| 570 | Strahlungsschild | `LEAD` | Lead | reactorcraft | Portieren (angepasst) | Unit: Abschirmwerte | Ore-Dict 'blockLead' → Tag c:storage_blocks/lead |
| 571 | Strahlungsschild | `OBSIDIAN` | Obsidian | reactorcraft | Portieren (angepasst) | Unit: Abschirmwerte |  |
| 572 | Strahlungsschild | `BLASTGLASS` | Blast Glass | reactorcraft | Portieren (angepasst) | Unit: Abschirmwerte |  |
| 573 | Sound | `TURBINE` | turbine-vol.ogg | reactorcraft | Portieren | sounds.json-Prüfung |  |
| 574 | Sound | `FUSION` | fusion.ogg | reactorcraft | Portieren | sounds.json-Prüfung |  |
| 575 | Sound | `CONTROL` | control.ogg | reactorcraft | Portieren | sounds.json-Prüfung |  |
| 576 | Sound | `GENERATOR_RF` | gen_rf.ogg | reactorcraft | Portieren | sounds.json-Prüfung |  |
| 577 | Sound | `GENERATOR_EU` | gen_eu.ogg | reactorcraft | Portieren | sounds.json-Prüfung |  |
| 578 | Sound | `GENERATOR_ELC` | gen_elc.ogg | reactorcraft | Portieren | sounds.json-Prüfung |  |
| 579 | Sound | `SCRAM` | scram.ogg | reactorcraft | Portieren | sounds.json-Prüfung |  |
| 580 | Fluid | `rc ammonia` | rc ammonia | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 581 | Fluid | `rc chlorine` | rc chlorine | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 582 | Fluid | `rc co2` | rc co2 | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 583 | Fluid | `rc corium` | rc corium | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 584 | Fluid | `rc deuterium` | rc deuterium | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 585 | Fluid | `rc fusion plasma` | rc fusion plasma | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 586 | Fluid | `rc heavy water` | rc heavy water | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 587 | Fluid | `rc hot co2` | rc hot co2 | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 588 | Fluid | `rc hot lifbe` | rc hot lifbe | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 589 | Fluid | `rc hotsodium` | rc hotsodium | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 590 | Fluid | `rc hydrofluoric acid` | rc hydrofluoric acid | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 591 | Fluid | `rc lifbe fuel preheat` | rc lifbe fuel preheat | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 592 | Fluid | `rc lifbe fuel` | rc lifbe fuel | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 593 | Fluid | `rc lifbe` | rc lifbe | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 594 | Fluid | `rc liquid oxygen` | rc liquid oxygen | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 595 | Fluid | `rc lithium` | rc lithium | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 596 | Fluid | `rc lowpammonia` | rc lowpammonia | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 597 | Fluid | `rc lowpwater` | rc lowpwater | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 598 | Fluid | `rc nuclear waste` | rc nuclear waste | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 599 | Fluid | `rc oxygen` | rc oxygen | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 600 | Fluid | `rc sodium` | rc sodium | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 601 | Fluid | `rc tritium` | rc tritium | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 602 | Fluid | `rc uranium hexafluoride` | rc uranium hexafluoride | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 603 | Fluid | `rc warmsodium` | rc warmsodium | reactorcraft | Portieren (angepasst) | Unit: FluidType registriert | Forge-Fluid → FluidType |
| 604 | MobEffect | `RADIATION` | Radiation (PotionRadiation) | reactorcraft | Portieren | GameTest: Strahlung + Hazmat-Schutz |  |
| 605 | Persistenz | `RadiationChunks` | Strahlungsdaten / Kontamination | reactorcraft | Portieren (angepasst) | GameTest: Speichern/Laden | Data Attachment bzw. SavedData je nach Datenart |
| 606 | Advancement | `RECUSEBOOK` | Knowledge is power | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 607 | Advancement | `MINEURANIUM` | The Nuclear Age | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 608 | Advancement | `MINECADMIUM` | Diamoooh...wait... | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 609 | Advancement | `PEBBLE` | Pebble Bed | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 610 | Advancement | `UF6` | Hexa-what? | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 611 | Advancement | `DEPLETED` | Mostly useless | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 612 | Advancement | `FISSION` | Splitting the Atom | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 613 | Advancement | `PLUTONIUM` | Synthesis | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 614 | Advancement | `PUPOISON` | Unsafe Handling | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 615 | Advancement | `HOLDWASTE` | That Was Dumb | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 616 | Advancement | `DECAY` | Half-Lives | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 617 | Advancement | `WASTELEAK` | Spent Fuel...Crater | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 618 | Advancement | `AMMONIA` | Efficiency | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 619 | Advancement | `NH3EXPLODE` | Autoignition | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 620 | Advancement | `GIGATURBINE` | Behold Real Power | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 621 | Advancement | `HOTCORE` | Running Hot | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 622 | Advancement | `SCRAM` | That Was Close | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 623 | Advancement | `MELTDOWN` | Time to Move | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 624 | Advancement | `HEAVYWATER` | Like Water, but Heavy | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 625 | Advancement | `CANDU` | Can Do | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 626 | Advancement | `PLASMA` | HOW hot? | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 627 | Advancement | `ESCAPE` | Containment Breach | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 628 | Advancement | `MELTPIPE` | Not-So-Containing Pipe | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 629 | Advancement | `FUSION` | Stellar Energy | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 630 | Advancement | `FIFTYGW` | Permanent Surplus | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 631 | Advancement | `PEBBLEFAIL` | How did you manage THAT? | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 632 | Advancement | `THORIUMDUMP` | Not really the point | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 633 | Advancement | `PLASMADIE` | Face Full Of Fire | reactorcraft | Portieren (angepasst) | Datagen: Advancement-JSON | Achievement → Advancement |
| 634 | Handbuch | `BOOK` | ReactorCraft Handbook (88 Einträge) | reactorcraft | Portieren (angepasst) | Client-Test manuell |  |
| 635 | Befehl | `clearsteam` | /clearsteam | reactorcraft | Portieren (angepasst) | Unit: Brigadier-Parse |  |
| 636 | Konfiguration | `ReactorOptions` | 17 Optionen | reactorcraft | Portieren (angepasst) | Config-Ladetest |  |
| 637 | Netzwerk | `ReactorPackets` | CPU-Steuerung (Toggle/Raise/Lower), Erz-Strahlung | reactorcraft | Portieren (angepasst) | Unit: StreamCodec Roundtrip | → CustomPacketPayload |
| 638 | Mod-Integration | `NEI` | NEI-Handler (Centrifuge, Electrolyzer, Synthesizer, UProcessor) | reactorcraft | Optional (JEI) | Ladetest ohne JEI |  |
| 639 | Mod-Integration | `—` | IC2-Reaktor-/EU-Bezüge, Thaumcraft, Botania (Alfheim-Abfall), TFC, Gendustry u. a. | reactorcraft | Nicht reproduzierbar / Freigabe | — | Zielmods auf 1.21.1 überwiegend nicht vorhanden |

## ElectriCraft

| # | Kategorie | Original-ID | Name | Zielmod | Status | Test | Anmerkung |
|---|---|---|---|---|---|---|---|
| 640 | Maschine (BlockEntity) | `WIRE` | electri.wire | electricraft | Portieren | GameTest: Netz aufbauen, Strom/Spannung |  |
| 641 | Maschine (BlockEntity) | `GENERATOR` | Induction Generator | electricraft | Portieren | GameTest: Netz aufbauen, Strom/Spannung |  |
| 642 | Maschine (BlockEntity) | `MOTOR` | Induction Motor | electricraft | Portieren | GameTest: Netz aufbauen, Strom/Spannung |  |
| 643 | Maschine (BlockEntity) | `RESISTOR` | Resistor | electricraft | Portieren | GameTest: Netz aufbauen, Strom/Spannung |  |
| 644 | Maschine (BlockEntity) | `RELAY` | Relay | electricraft | Portieren | GameTest: Netz aufbauen, Strom/Spannung |  |
| 645 | Maschine (BlockEntity) | `BATTERY` | machine.electribattery | electricraft | Portieren | GameTest: Netz aufbauen, Strom/Spannung |  |
| 646 | Maschine (BlockEntity) | `CABLE` | RF Transfer Cable | electricraft | Portieren (angepasst) | GameTest: Netz aufbauen, Strom/Spannung | RF → NeoForge Energy (FE) |
| 647 | Maschine (BlockEntity) | `METER` | Electric Meter | electricraft | Portieren | GameTest: Netz aufbauen, Strom/Spannung |  |
| 648 | Maschine (BlockEntity) | `RFBATTERY` | RF Storage Battery | electricraft | Portieren (angepasst) | GameTest: Netz aufbauen, Strom/Spannung | RF → NeoForge Energy (FE) |
| 649 | Maschine (BlockEntity) | `TRANSFORMER` | Transformer | electricraft | Portieren | GameTest: Netz aufbauen, Strom/Spannung |  |
| 650 | Maschine (BlockEntity) | `EUSPLIT` | EU Splitter | electricraft | Nicht reproduzierbar | — | IC2-EU: kein IC2 für 1.21.1 NeoForge |
| 651 | Maschine (BlockEntity) | `EUCABLE` | Superconductor EU Cable | electricraft | Nicht reproduzierbar | — | IC2-EU: kein IC2 für 1.21.1 NeoForge |
| 652 | Maschine (BlockEntity) | `EUBATTERY` | EU Storage Battery (RBSU) | electricraft | Nicht reproduzierbar | — | IC2-EU: kein IC2 für 1.21.1 NeoForge |
| 653 | Maschine (BlockEntity) | `FUSE` | Fuse | electricraft | Portieren | GameTest: Netz aufbauen, Strom/Spannung |  |
| 654 | Maschine (BlockEntity) | `WIRELESSPAD` | Wireless Energy Transfer Pad | electricraft | Portieren | GameTest: Netz aufbauen, Strom/Spannung |  |
| 655 | Maschine (BlockEntity) | `PRECISERESISTOR` | Precision Resistor | electricraft | Portieren | GameTest: Netz aufbauen, Strom/Spannung |  |
| 656 | Block | `WIRE` | Wire | electricraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 657 | Block | `MACHINE` | Converter | electricraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 658 | Block | `ORE` | ElectriOre | electricraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 659 | Block | `BATTERY` | ElectriBattery | electricraft | Portieren (angepasst) | Datagen-Prüfung | Metadaten-Varianten → eigene Blöcke |
| 660 | Block | `CABLE` | RFCable | electricraft | Portieren (angepasst) | Datagen-Prüfung |  |
| 661 | Block | `RFBATTERY` | ElectriRFBattery | electricraft | Portieren (angepasst) | Datagen-Prüfung |  |
| 662 | Block | `EUSPLIT` | EUSplitter | electricraft | Nicht reproduzierbar | — |  |
| 663 | Block | `EUCABLE` | EUCable | electricraft | Nicht reproduzierbar | — |  |
| 664 | Block | `EUBATTERY` | ElectriEUBattery | electricraft | Nicht reproduzierbar | — |  |
| 665 | Block | `CHARGEPAD` | ElectriChargePad | electricraft | Portieren (angepasst) | Datagen-Prüfung |  |
| 666 | Item | `PLACER` | item.placer | electricraft | Ersetzt (Flattening) | Datagen-Prüfung |  |
| 667 | Item | `INGOTS` | item.Electriingots | electricraft | Portieren (angepasst) | Datagen-Prüfung |  |
| 668 | Item | `WIRE` | Wire | electricraft | Portieren (angepasst) | Datagen-Prüfung |  |
| 669 | Item | `BATTERY` | Battery | electricraft | Portieren (angepasst) | Datagen-Prüfung |  |
| 670 | Item | `CRAFTING` | item.crafting | electricraft | Portieren (angepasst) | Datagen-Prüfung |  |
| 671 | Item | `CRYSTAL` | item.electricrystal | electricraft | Portieren (angepasst) | Datagen-Prüfung |  |
| 672 | Item | `RFBATTERY` | RF Storage Battery | electricraft | Portieren (angepasst) | Datagen-Prüfung |  |
| 673 | Item | `BOOK` | ElectriCraft Handbook | electricraft | Portieren (angepasst) | Datagen-Prüfung |  |
| 674 | Item | `EUBATTERY` | EU Storage Battery (RBSU) | electricraft | Nicht reproduzierbar | Datagen-Prüfung |  |
| 675 | Draht-Typ | `STEEL` | Steel Wire (+ isoliert) | electricraft | Portieren | Unit: Strom-/Spannungsgrenzen |  |
| 676 | Draht-Typ | `TIN` | Tin Wire (+ isoliert) | electricraft | Portieren | Unit: Strom-/Spannungsgrenzen |  |
| 677 | Draht-Typ | `NICKEL` | Nickel Wire (+ isoliert) | electricraft | Portieren | Unit: Strom-/Spannungsgrenzen |  |
| 678 | Draht-Typ | `ALUMINUM` | Aluminum Wire (+ isoliert) | electricraft | Portieren | Unit: Strom-/Spannungsgrenzen |  |
| 679 | Draht-Typ | `COPPER` | Copper Wire (+ isoliert) | electricraft | Portieren | Unit: Strom-/Spannungsgrenzen |  |
| 680 | Draht-Typ | `SILVER` | Silver Wire (+ isoliert) | electricraft | Portieren | Unit: Strom-/Spannungsgrenzen |  |
| 681 | Draht-Typ | `GOLD` | Gold Wire (+ isoliert) | electricraft | Portieren | Unit: Strom-/Spannungsgrenzen |  |
| 682 | Draht-Typ | `PLATINUM` | Platinum Wire (+ isoliert) | electricraft | Portieren | Unit: Strom-/Spannungsgrenzen |  |
| 683 | Draht-Typ | `SUPERCONDUCTOR` | Superconductor Wire (+ isoliert) | electricraft | Portieren | Unit: Strom-/Spannungsgrenzen |  |
| 684 | Batterie-Typ | `REDSTONE` | Redstone Battery | electricraft | Portieren | Unit: Kapazität |  |
| 685 | Batterie-Typ | `GLOWSTONE` | Glowstone Battery | electricraft | Portieren | Unit: Kapazität |  |
| 686 | Batterie-Typ | `LAPIS` | Lapis Battery | electricraft | Portieren | Unit: Kapazität |  |
| 687 | Batterie-Typ | `ENDER` | Ender Battery | electricraft | Portieren | Unit: Kapazität |  |
| 688 | Batterie-Typ | `DIAMOND` | Diamond Battery | electricraft | Portieren | Unit: Kapazität |  |
| 689 | Batterie-Typ | `STAR` | Star Battery | electricraft | Portieren | Unit: Kapazität |  |
| 690 | Erz + Weltgenerierung | `COPPER` | Copper Ore | electricraft | Freigabe: Vanilla | Datagen: Feature/Biome Modifier | Vanilla 1.21.1 hat Kupfererz/-barren → Regel 3 spricht für Vanilla-Kupfer |
| 691 | Erz + Weltgenerierung | `TIN` | Tin Ore | electricraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier |  |
| 692 | Erz + Weltgenerierung | `SILVER` | Silver Ore | electricraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier | Doppelung mit ReactorCraft-Silber |
| 693 | Erz + Weltgenerierung | `NICKEL` | Nickel Ore | electricraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier |  |
| 694 | Erz + Weltgenerierung | `ALUMINUM` | Aluminum Ore | electricraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier |  |
| 695 | Erz + Weltgenerierung | `PLATINUM` | Platinum Ore | electricraft | Portieren (angepasst) | Datagen: Feature/Biome Modifier |  |
| 696 | Crafting-Zwischenprodukt | `BLUEDUST` | Bluedust | electricraft | Portieren (angepasst) | Datagen-Prüfung | Ore-Dict → c:-Tags |
| 697 | Crafting-Zwischenprodukt | `DIAMONDDUST` | Diamonddust | electricraft | Portieren (angepasst) | Datagen-Prüfung | Ore-Dict → c:-Tags |
| 698 | Crafting-Zwischenprodukt | `QUARTZDUST` | Quartzdust | electricraft | Portieren (angepasst) | Datagen-Prüfung | Ore-Dict → c:-Tags |
| 699 | Crafting-Zwischenprodukt | `CRYSTALDUST` | Crystaldust | electricraft | Portieren (angepasst) | Datagen-Prüfung | Ore-Dict → c:-Tags |
| 700 | Energiekristall | `CRYSTAL` | Energy Crystal (Item-Ladung) | electricraft | Portieren (angepasst) | Unit: Laden/Entladen | NBT → Data Component |
| 701 | Netzwerk | `ElectriNetworkManager` | Drahtnetz-Simulation (Spannung/Strom, Repath) | electricraft | Portieren | Unit + GameTest | Server-Tick-Event (LevelTickEvent) |
| 702 | Netzwerk | `ElectriPackets` | RF-Kabel, Transformator | electricraft | Portieren (angepasst) | Unit: StreamCodec Roundtrip |  |
| 703 | Handbuch | `BOOK` | ElectriCraft Handbook (31 Einträge) | electricraft | Portieren (angepasst) | Client-Test manuell |  |
| 704 | Konfiguration | `ElectriOptions` | Optionen (u. a. Ore Discretization) | electricraft | Portieren (angepasst) | Config-Ladetest |  |
| 705 | Mod-Integration | `—` | WAILA, OpenComputers/ComputerCraft Lua, IC2 | electricraft | Teilweise / Freigabe | Ladetest ohne Fremdmod | Jade/CC: Tweaked optional; IC2 entfällt |

## DragonAPI

| # | Kategorie | Original-ID | Name | Zielmod | Status | Test | Anmerkung |
|---|---|---|---|---|---|---|---|
| 706 | Bibliothek | `—` | Mathe/Physik (ReikaEngLibrary, MathSci) | rotarycraft (intern) | Portieren | Unit: Formelwerte |  |
| 707 | Bibliothek | `—` | Datenstrukturen (BlockArray, StructuredBlockArray, Maps, StepTimer, HybridTank …) | rotarycraft (intern) | Portieren (angepasst) | Unit |  |
| 708 | Bibliothek | `—` | Multiblock-/Struktur-Prüfung (FilledBlockArray, StructureRenderer) | rotarycraft (intern) | Portieren (angepasst) | GameTest |  |
| 709 | Bibliothek | `—` | Handbuch-GUI-Framework, GUI-Bausteine, PianoKeyboard | rotarycraft (intern) | Portieren (angepasst) | Client-Test manuell |  |
| 710 | Basis | `—` | TileEntityBase/BlockTEBase (Sync, Owner, Redstone) | rotarycraft (intern) | Ersetzt durch NeoForge/Vanilla | — |  |
| 711 | Basis | `—` | DragonAPIMod-Lifecycle, Registrierungsframework (Enum-Registries) | — | Ersetzt durch NeoForge | — |  |
| 712 | Netzwerk | `—` | APIPacketHandler / PacketTypes | — | Ersetzt durch Payload-API | — |  |
| 713 | Konfiguration | `—` | ControlledConfig / ConfigList | — | Ersetzt durch ModConfigSpec | — |  |
| 714 | IO | `—` | SoundLoader, XMLInterface, MIDIInterface | rotarycraft (intern) | Teilweise ersetzt | — |  |
| 715 | Rendering | `—` | ISBRH, Tessellator-Helfer, IconPrefabs, Shader-Registry | — | Ersetzt durch BakedModel/BER/RenderType | Client-Test |  |
| 716 | Coremod | `—` | ASM-Patcher (181 Patches), API-/Method-Stripper, Class-Reparenter | — | Nicht übernommen / Freigabe | — |  |
| 717 | Coremod | `—` | Zusatz-Events via ASM (SetBlockEvent, CreeperExplodeEvent, …) | — | Teilweise durch NeoForge-Events ersetzt | — |  |
| 718 | Befehle | `—` | 48 Admin-/Debug-Befehle (/dragon …) | — | Nicht übernommen / Freigabe | — |  |
| 719 | Tracker | `—` | Update-Checker, Donator/Patreon, Crash-Notifications, Integrity/Pack-Tracker, Remote-Assets | — | Nicht übernommen / Freigabe | — |  |
| 720 | Tracker | `—` | TickScheduler, KeybindHandler, ChunkManager, PlayerHandler | rotarycraft (intern) | Ersetzt durch NeoForge (Events, KeyMapping, TicketController) | — |  |
| 721 | Mod-Integration | `—` | ModInteract (25 000 Zeilen, ~100 1.7.10-Mods) | — | Nur für tatsächlich benötigte Ziele (JEI/Jade/CC) | — |  |
| 722 | Gebündelt | `—` | Fremd-APIs im JAR (appeng, cofh, minechem, InfinityRaider, Reika-Mod-APIs) | — | Nicht übernommen | — |  |
| 723 | Access Transformer | `—` | DragonAPI_at.cfg (211 Zeilen) | — | Nur benötigte Einträge, neu für 1.21.1 | — |  |
