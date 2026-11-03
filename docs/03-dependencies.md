# 03 – Abhängigkeiten

## 1. Zielplattform (verifiziert am 2026-10-18)

| Komponente | Version | Nachweis |
|---|---|---|
| Minecraft | 1.21.1 | Vorgabe |
| NeoForge | **21.1.262** (neueste 1.21.1-Version im Maven) | `https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml` |
| Offizielles MDK | `NeoForgeMDKs/MDK-1.21.1-ModDevGradle` nutzt `neo_version=21.1.259` | `gradle.properties` des MDK |
| Gradle-Plugin | `net.neoforged.moddev` **2.0.148** | Plugin-Maven-Metadaten + MDK-`build.gradle` |
| Gradle | 9.2.1 (Wrapper des MDK) | MDK `gradle-wrapper.properties` |
| Parchment-Mappings | 1.21.1 / 2024.11.17 | MDK `gradle.properties` |
| Java | 21. Lokal installiert: Amazon Corretto 21.0.9 | `java -version` |

Vorschlag: **NeoForge 21.1.262** (aktuellste stabile 1.21.1-Version) mit MDK-Struktur und ModDevGradle 2.0.148. Ob 21.1.262 mit Parchment 2024.11.17 baut, wird erst in Phase D geprüft.

Geprüfte NeoForge-1.21.1-APIs (Doku `docs.neoforged.net/docs/1.21.1/…`):
- Capabilities: `Capabilities.ItemHandler/FluidHandler/EnergyStorage.{BLOCK,ENTITY,ITEM}`, Registrierung in `RegisterCapabilitiesEvent` (Mod-Bus), `BlockCapabilityCache`, `level.invalidateCapabilities(pos)`
- Netzwerk: `RegisterPayloadHandlersEvent` → `PayloadRegistrar` (`playToClient`/`playToServer`/`playBidirectional`), `CustomPacketPayload` + `StreamCodec`, `PacketDistributor.sendToPlayer/sendToPlayersTrackingChunk/…`
- Weitere APIs (DeferredRegister, Data Attachments, Data Components, ModConfigSpec, Biome Modifiers, GameTest, TicketController) werden vor ihrer Verwendung in Phase C/D einzeln gegen die 1.21.1-Doku und den NeoForge-Quelltext geprüft.

## 2. Interne Abhängigkeiten (Original)

```
DragonAPI  ◄── RotaryCraft (required) ◄── ReactorCraft (required)
    ▲               ▲                 ◄── ElectriCraft (required)
    └───────────────┴── ReactorCraft, ElectriCraft (required DragonAPI)

ReactorCraft ··► ElectriCraft   optional (TileEntityReactorGenerator: @Strippable/@ModDependent)
RotaryCraft  ··► ReactorCraft   optional (RotaryRecipes: Legierung als Zutat; Sprinkler entfernt EntityRadiation;
                                          Achievements mit ReactorCraft-Icons; ModList.REACTORCRAFT-Guards)
RotaryCraft  ··► ElectriCraft   optional (ModList.ELECTRICRAFT, 2 Stellen)
```

Gemessene Kopplung (Import-Anweisungen):

| Von → nach | Importe | Hauptpakete |
|---|---|---|
| RotaryCraft → DragonAPI | 421 Klassen | Libraries (IO, Registry, Java, MathSci, Rendering, World), Interfaces.TileEntity, Instantiable.Data/Rendering, ModInteract |
| ReactorCraft → DragonAPI | 193 Klassen | Instantiable.Data, Libraries, Interfaces.TileEntity, HybridTank, StepTimer |
| ElectriCraft → DragonAPI | 103 Klassen | Libraries.MathSci/Java/IO, ModInteract.Lua, Interfaces.TileEntity |
| ReactorCraft + ElectriCraft → RotaryCraft | 338 Importe | Auxiliary (126), Registry (65), API (61), Base (46), TileEntities (12) |
| ReactorCraft → ElectriCraft | 1 Datei | `WrappableWireSource`, `WireNetwork` |

Zusammen werden 468 verschiedene DragonAPI-Klassen direkt genutzt, also rund 37 % der 1 249 DragonAPI-Dateien. Nicht direkt genutzt werden vor allem: ASM-Patcher, Befehle, die meisten Tracker und der Großteil von `ModInteract`.

## 3. Externe Abhängigkeiten (Original, 1.7.10) und Lage auf 1.21.1

Gezählt als `ModList.<MOD>`-Bezüge im Code (RC = RotaryCraft, ReC = ReactorCraft, EC = ElectriCraft). Verfügbarkeit für **NeoForge 1.21.1** über die Modrinth-API geprüft (Loader `neoforge`, Version `1.21.1`). „Nicht auf Modrinth“ heißt **nicht**, dass es keinen Port gibt; ein CurseForge-Port ist dann nicht ausgeschlossen.

| Fremdmod | Bezüge | Zweck im Original | 1.21.1 NeoForge | Bewertung |
|---|---|---|---|---|
| NotEnoughItems | RC 2, EC 1, ReC (eigene Handler) | Rezeptanzeige | NEI fehlt. **JEI 19.64.0.455** verfügbar | optional → JEI |
| WAILA | RC 15, ReC 5, EC 20 | Tooltips beim Anschauen | **Jade 15.10.6** verfügbar | optional → Jade |
| ComputerCraft / OpenComputers | RC 2+1, ReC 1, EC 1 + Lua-Pakete | Lua-Steuerung | **CC: Tweaked 1.120.2** verfügbar; OpenComputers nicht | optional → CC: Tweaked |
| Applied Energistics 2 | RC 54, ReC 1 | Bundled Bus, Item-Interop, Meteorite-Erkennung u. a. | **AE2 19.2.18** verfügbar | Umfang prüfen, Freigabe |
| IndustrialCraft 2 | RC 28, ReC 7, EC 17 | EU-Energie, Rezepte | nicht bekannt (kein 1.21.1-Port) | nicht reproduzierbar |
| BuildCraft (Energy/Factory/Transport/Silicon) | RC 14, ReC 1 | Fuel Engine, Statements, Rohre | nicht bekannt | nicht reproduzierbar |
| Thermal Expansion / Foundation / Dynamics | RC 31, ReC 1, EC 1 | RF, Rezepte, Erze | Thermal auf Modrinth nicht gefunden | RF → NeoForge Energy (FE) eingebaut |
| CoFH-API (RF) | über PowerTypes.RF | Energie | durch **NeoForge Energy** (`IEnergyStorage`) ersetzt | ersetzt durch NeoForge |
| PneumaticCraft | RC 9 | Druckluft (Compressor, Pneumatic Engine) | **PneumaticCraft: Repressurized 8.2.23** verfügbar, API ungeprüft | optional, Freigabe |
| Railcraft | RC 10 | Dampf, Rezepte | **Railcraft Reborn 1.2.12** verfügbar | Steam-Fluid unklar, Freigabe |
| Mekanism | RC 2 | Rezepte, Interop | **Mekanism 10.7.19.85** verfügbar | niedrig priorisiert |
| Immersive Engineering | RC 4 | Interop | **IE 12.4.2-194** verfügbar | niedrig priorisiert |
| Botania | RC 29, ReC 3 | Livingwood/-rock-Getriebe, Alfheim | auf Modrinth nicht gefunden | unklar |
| Forestry | RC 28 | Bienen, Grafter, Hefe | auf Modrinth nicht gefunden | nicht reproduzierbar (Stand jetzt) |
| Thaumcraft | RC 24, ReC 2, EC 1 | Aspekte, Infusion, Fokus | nicht vorhanden | nicht reproduzierbar |
| ChromatiCraft (Reika) | RC 45, ReC 10, EC 1 | Interaktionen | nur 1.7.10 | nicht reproduzierbar |
| Satisforestry, GeoStrata, VoidMonster, MeteorCraft, ExpandedRedstone, LegacyCraft (Reika) | RC 13/6/1/1/1/2 | Interaktionen | nur 1.7.10 | nicht reproduzierbar |
| Tinkers' Construct | RC 7 | Erzflocken in der Schmelze | auf Modrinth nicht gefunden | unklar |
| Weitere (GregTech, EnderIO, TwilightForest, TFC, Mystcraft, MagicCrops, AgriCraft, Hexcraft, DartCraft, ProjectRed, ArsMagica, BloodMagic, Chisel, DraconicEvolution, MFFS, Galacticraft, Multipart, MineFactory, Gendustry, ColorLight, Mimicry, CondensedOres …) | je 1–8 | Rezept- und Interaktions-Sonderfälle | überwiegend kein 1.21.1-Port bekannt, nicht einzeln geprüft | Freigabe pro Mod, Vorschlag: entfällt |

Gebündelte Fremd-APIs im DragonAPI-JAR (appeng, cofh, minechem, InfinityRaider) werden **nicht** übernommen. Integrationen werden gegen die echten 1.21.1-Artefakte als `compileOnly` bzw. optionale Abhängigkeit gebaut.

## 4. Vanilla- und NeoForge-Ersatz für DragonAPI-Funktionen (Regel 3)

| DragonAPI / Original | Ersatz in 1.21.1 |
|---|---|
| Enum-Registries + `GameRegistry` | `DeferredRegister` (Blocks, Items, BlockEntityTypes, EntityTypes, MenuTypes, SoundEvents, Fluids/FluidTypes, MobEffects, RecipeTypes/Serializers, CreativeModeTabs, DataComponentTypes, AttachmentTypes) |
| `TileEntityBase`-Sync über eigene Pakete | `getUpdatePacket`/`getUpdateTag` (`ClientboundBlockEntityDataPacket`), `ContainerData` für GUIs |
| `APIPacketHandler`, `PacketRegistry` (67 + 4 + 2 Typen) | `CustomPacketPayload` + `StreamCodec` |
| `ControlledConfig` | `ModConfigSpec` (COMMON/SERVER/CLIENT) |
| Ore Dictionary, `ModOreList` | Item- und Block-Tags, Konventions-Tags `c:` |
| `IWorldGenerator`, Retrogen | `ConfiguredFeature`/`PlacedFeature` + NeoForge Biome Modifier (Datapack) |
| Achievements | Advancements (Datagen) |
| Code-Rezepte, `CustomRecipeList`, MineTweaker | Datapack-Rezepte, eigene `RecipeType`/`RecipeSerializer` |
| `ChunkManager` / Forge-Tickets | `TicketController` (NeoForge) |
| `KeybindHandler` | `RegisterKeyMappingsEvent` |
| `TickScheduler`, `TickRegistry` | `LevelTickEvent`/`ServerTickEvent`, `BlockEntityTicker` |
| HybridTank / Fluid-Interop | `FluidTank` + `Capabilities.FluidHandler` |
| Inventar-Interop (`ReikaInventoryHelper`, ItemHandlers) | `IItemHandler` + `Capabilities.ItemHandler` |
| Item-NBT | Data Components |
| Entity-/Chunk-Zusatzdaten | Data Attachments bzw. `SavedData` |
| `ISBRH`, IconPrefabs, Sprite-Sheets | JSON-Block-/Itemmodelle, `BlockEntityRenderer`, Texture-Atlas |
| Potion-ID-Konfiguration | `MobEffect`-Registry (IDs entfallen) |
| `/dragon …`-Admin-Befehle | großteils Vanilla (`/fill`, `/locate biome`, `/data get`, `/kill @e[…]`, `/forceload`, `/tick`, `/loot`, `/item`) |
| `SpecialDayTracker`, Update-Checker, Donator/Patreon, Remote-Assets | kein Ersatz nötig, kein Spielfeature. NeoForge hat eine eigene Update-Prüfung (`updateJSONURL`) |

## 5. Lizenzbedingungen (Zusammenfassung)

Siehe `01-original-mods.md` §5. Konsequenzen für die Abhängigkeiten:
- Die Original-JARs werden **nicht** als Abhängigkeit eingebunden; 1.7.10-Code ist auf 1.21.1 ohnehin nicht lauffähig.
- Code aus `API`-Paketen darf laut Lizenz „not be modified from the original without first contacting Reika“. Ein Port **muss** ihn aber ändern (neue Minecraft-Typen). Das ist eine **Freigabefrage**.
- Drittbibliotheken für die Zielmods: **keine** über NeoForge hinaus. Optionale `compileOnly`-Abhängigkeiten nur für JEI, Jade und CC: Tweaked, falls freigegeben.
