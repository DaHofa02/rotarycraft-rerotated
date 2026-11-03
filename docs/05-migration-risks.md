# 05 – Migrationsrisiken, fehlende Informationen, inkompatible APIs

Bewertung: **H** = hoch (gefährdet Machbarkeit oder Zeitplan), **M** = mittel, **N** = niedrig.

## 1. Übergeordnete Risiken

| # | Risiko | Bew. | Begründung / Befund | Gegenmaßnahme |
|---|---|---|---|---|
| R1 | **Lizenz** | **H** | Abgeleitete Werke nur privat, Weitergabe nur mit Erlaubnis, „Derivative mods … EXPRESSLY FORBIDDEN“, API-Code darf nicht ohne Rücksprache geändert werden (01 §5) | **Freigabe vor Phase D**: nur privater Gebrauch, ggf. Erlaubnis von Reika einholen. JARs nicht veröffentlichen. |
| R2 | **Umfang** | **H** | Rund 240 000 Zeilen Inhalts-Code (RC 178k, ReC 45k, EC 16k) und 468 benötigte DragonAPI-Klassen. 201 Maschinen, über 600 Registry-Objekte nach dem Flattening, 723 Einträge in der Feature-Matrix. Fast keine Zeile ist ohne Änderung übernehmbar (Minecraft-API von 1.7.10 auf 1.21.1: 14 Hauptversionen). | Migration in vertikalen Scheiben (06 §7). Jede Scheibe wird gebaut und getestet, bevor die nächste beginnt. Ein vollständiger Port ist ein **mehrmonatiges Vorhaben** und in einer einzelnen Sitzung nicht realistisch. |
| R3 | **Rendering** | **H** | 338 Dateien mit direkten `GL11`-Aufrufen, 78 mit `Tessellator`, 150 Java-Modelle, ISBRH-Renderer. 1.21 hat keine Immediate-Mode-GL-Aufrufe mehr (stattdessen `PoseStack`, `VertexConsumer`, `RenderType`, `MultiBufferSource`). | Jeder Renderer wird **neu geschrieben**. Modellgeometrie wird aus den Box-Daten mechanisch übertragen (04 §3). Ein Skript wandelt die Techne-Modellklassen in `LayerDefinition` um. |
| R4 | **Flattening (Metadaten)** | **H** | Blöcke und Items sind Metadaten-Sammler (z. B. 20 Maschinen-Trägerblöcke, `ItemMulti`, `getStackOfMetadata`). In 1.13+ gibt es keine Metadaten mehr. | Ein Block und ein Item je Untertyp; Zustände als BlockState-Properties. Die Zuordnungstabelle alt → neu wird Teil der Doku. **Alte Welten sind nicht migrierbar** (1.7.10 → 1.21.1 geht ohnehin nicht). |
| R5 | **Coremods / ASM** | **H** | DragonAPI-Coremod (181 Patches) und RotaryCraft-Coremod (`FURNACEINTERCEPT`). 24 genutzte DragonAPI-Events existieren nur über ASM. | Je Event prüfen, ob es einen NeoForge-Ersatz gibt (Tabelle §3). Für den Rest **gezielte Mixins** nur dort, wo ein Feature sonst entfällt, mit Begründung je Mixin. DragonAPI-Bugfix-Patches für 1.7.10 entfallen. |
| R6 | **Performance Neutronen-Entities** | M | ReactorCraft simuliert Neutronen als echte Entities. Das 1.21-Entity-System ist teurer (Tracking, Synchronisation). | Verhalten identisch nachbauen und mit einem GameTest-Benchmark messen. Bei Problemen dokumentieren und Freigabe einholen, bevor das Verfahren geändert wird. |
| R7 | **Energie- und Netztick über Chunkgrenzen** | M | Wellenleistung wird pro Tick über Nachbarblöcke weitergereicht (`POWERCLIENT`-Option: auch clientseitig). ElectriCraft-Netze haben einen globalen Manager. Portal Shaft überträgt zwischen Dimensionen. | Nur serverseitig rechnen, Clientwerte über `getUpdateTag`/Payload synchronisieren. Netzmanager pro `ServerLevel` (Data Attachment am Level oder `SavedData`). |
| R8 | **Fremdmod-Funktionen** | M | 11 RC-, 7 EC- und 1 ReC-Matrixeintrag sind „nicht reproduzierbar“ (IC2, BuildCraft, Forestry, Thaumcraft, Multipart, ChromatiCraft). Weitere sind unklar (Botania, Steam, PneumaticCraft). | Klar als fehlend dokumentieren, nichts nachbauen (Regel 2). Freigabe einholen. |
| R9 | **Unvollständige Doku zur Zielversion** | M | Viele Beispiele im Netz gelten für 1.20.x oder 1.21.2+ und weichen ab (z. B. Rezept-API, Item-Modelle, `ItemStack`-Komponenten). | Nur `docs.neoforged.net/docs/1.21.1/`, die NeoForge-21.1-Quellen (über MDG im Gradle-Cache) und Parchment-Mappings verwenden. Jede Signatur gegen die Quellen prüfen. |
| R10 | **Versionsabweichung Quelltext/JAR** | N | DragonAPI-Quelltext v33a, JAR V33b. Für ReC/EC ist der Commit nicht versioniert. | Abweichungen bei Bedarf mit dem Bytecode-Vergleich (`javap`) gezielt prüfen. Annahme: geringfügig. |
| R11 | **Kein Original-Build-Skript** | N | Kein `build.gradle` in den Ordnern; das Original lässt sich nicht zum Vergleich bauen. | Vergleichsgrundlage sind Quelltext und die Original-JARs (statische Analyse). Laufvergleich in einer 1.7.10-Instanz nur, wenn der Nutzer eine bereitstellt. |

## 2. Inkompatible APIs und Systeme (Auszug, nach Bereich)

| Bereich | 1.7.10 (Original) | 1.21.1 NeoForge | Auswirkung |
|---|---|---|---|
| Positionen | `x, y, z`-Ints, `ForgeDirection` | `BlockPos`, `Direction` | überall |
| Blöcke | Metadaten, `Block.setBlock(x,y,z,id,meta,flags)` | `BlockState` + Properties, `Level.setBlock(pos,state,flags)` | 293 `setBlock`-Stellen |
| Tile Entities | `TileEntity.updateEntity()` | `BlockEntity` + `BlockEntityTicker`, `BlockEntityType` | 239 TE-Klassen |
| NBT-Speicherung | `writeToNBT/readFromNBT` | `saveAdditional/loadAdditional(CompoundTag, HolderLookup.Provider)` | alle TEs |
| Item-Daten | `ItemStack.stackTagCompound` | **Data Components** | Werkzeugladung, Jetpack-Treibstoff, Energiekristall, Handbuch, Schlüssel … |
| Inventare | `IInventory`, `ISidedInventory` | `IItemHandler` (Capability), `Container`/`AbstractContainerMenu` | 63 Container |
| GUIs | `GuiContainer`, `IGuiHandler` | `AbstractContainerScreen`, `MenuType`, `IMenuTypeExtension` | 85 GUIs |
| Fluide | Forge `Fluid`, `IFluidHandler` (alt) | `FluidType`, `FlowingFluid`, `Capabilities.FluidHandler` | 29 Fluide |
| Energie | RF (CoFH), EU (IC2) | NeoForge Energy (FE) | RF → FE, EU entfällt |
| Netzwerk | `FMLProxyPacket`, DragonAPI-PacketHandler | `CustomPacketPayload`/`StreamCodec` | 73 Pakettypen |
| Rezepte | `GameRegistry.addRecipe`, Code-Listen | Datapack-JSON, `RecipeType`, `RecipeSerializer`, `RecipeInput` | ca. 826 Rezeptaufrufe, 13+ Maschinenrezepttypen |
| Tags | Ore Dictionary | Tags (`c:`) | Erz- und Barren-Interop |
| Weltgenerierung | `IWorldGenerator` | `ConfiguredFeature`/`PlacedFeature` + Biome Modifier | 3 Generatoren, 16 Erze |
| Achievements | `Achievement`/`AchievementPage` | Advancements | 74 |
| Tränke | `Potion` mit konfigurierbarer ID | `MobEffect` (Registry) | 3 |
| Entities | `EntityRegistry.registerModEntity` | `EntityType.Builder`, `DeferredRegister` | 15 |
| Befehle | `CommandBase` | Brigadier, `RegisterCommandsEvent` | 2 |
| Sound | Classpath-Loader (DragonAPI) | `SoundEvent` + `sounds.json` | 53 |
| Rendering | GL11, Tessellator, ISBRH, IIcon | PoseStack/VertexConsumer/RenderType, JSON-Modelle, Atlas | siehe R3 |
| Chunkloading | `ForgeChunkManager` | `TicketController` | Chunk Loader, Fission-Kerne |
| Explosionen | `world.newExplosion` | `Level.explode(..., ExplosionInteraction)` | Kernschmelze, Kanonen, Selbstzerstörung |
| Spieler-Flug | Capabilities-Flags + Kick-Umgehung (`KICKFLYING`) | `ServerGamePacketListenerImpl`-Flugprüfung | Jetpack: Kick-Umgehung braucht ggf. Mixin/AT → Risiko |
| Optional-Abhängigkeiten | `@Strippable`/`@ModDependent` (ASM) | Klassen-Isolation + `ModList.get().isLoaded()` | 78 Stellen. Die Interop-Klassen werden getrennt, damit nichts fehlt, wenn der Fremdmod nicht installiert ist. |

## 3. ASM-Events → NeoForge-Ersatz (Prüfstand Phase A, Signaturen in Phase C verifizieren)

| DragonAPI-Event | Genutzt für | Kandidat in NeoForge 1.21.1 | Status |
|---|---|---|---|
| `FarmlandTrampleEvent` | Sprungstiefel ohne Zertrampeln | `BlockEvent.FarmlandTrampleEvent` | vermutlich 1:1 |
| `EnderLookAggroEvent` | Visier gegen Enderman | `EnderManAngerEvent` | vermutlich 1:1 |
| `CreeperExplodeEvent` | „Dirty bombs“ (ReC) | `ExplosionEvent.Start/Detonate` | prüfen |
| `LivingFarDespawnEvent` | Spawner Controller | `MobDespawnEvent` | prüfen |
| `PlayerPlaceBlockEvent` | Worktable-Aufbau | `BlockEvent.EntityPlaceEvent` | vermutlich 1:1 |
| `BlockConsumedByFireEvent` | Schmierschlauch brennt | kein bekanntes Event; Block-eigene Logik (`onCaughtFire`/`isFlammable`) | prüfen |
| `SetBlockEvent` | EMP-Effekte, Hydro-Engine-Ströme | kein allgemeines Event → BlockState-Callbacks (`neighborChanged`, `onPlace`) | Verhalten ggf. angepasst |
| `EntityPushOutOfBlocksEvent` | Items nicht aus Maschinen drücken | kein Event bekannt → Mixin oder Blockform | Risiko |
| `SlotEvent.Add/RemoveFromSlot` | Rüstungseffekte, Uncrafting-Sperre | `LivingEquipmentChangeEvent` (Rüstung); Container-Slot-Logik | prüfen |
| `BlockTickEvent`, `TileUpdateEvent` | Tick-Beobachtung | `LevelTickEvent`, eigene Ticker | anpassen |
| `GrassSustainCropEvent` | Canola auf Gras | `Block.canSustainPlant`/`mayPlaceOn` | anpassen |
| `TileEntityMoveEvent`, `TeleposeEvent` | Schutz vor Frames/Blood Magic | Fremdmod-spezifisch | entfällt |
| `ItemSizeTextEvent`, `RenderItemInSlotEvent` | Schrottwert-Anzeige, Slot-Overlays | `RegisterItemDecorationsEvent` | prüfen |
| `EntityRenderingLoopEvent` | EMP-Funken | `RenderLevelStageEvent` | anpassen |
| `PlaySoundEvent` (Forge) | Deafness-Effekt | `PlaySoundEvent` (NeoForge, Client) | vermutlich 1:1 |
| `NEIRecipeCheckEvent`, `MTReloadEvent` | NEI/MineTweaker | entfällt (JEI/Datapacks) | entfällt |
| Vanilla-Ofen-Patch (RotaryCraft) | Friction Heater übernimmt den Ofen | kein Event. Varianten: (a) Mixin auf `AbstractFurnaceBlockEntity.serverTick`; (b) Heater setzt Brenn- und Kochzeit des Ofens über AT/Accessor | **Freigabe**: (b) bevorzugt, falls verhaltensgleich |

## 4. Fehlende oder unklare Informationen

1. Inhalt der DragonAPI-Änderung v33a → v33b.
2. Herkunft und Zweck des RotaryCraft-Fluids `poison`.
3. Ob `blocks/old` und `items/tinkertools` noch irgendwo referenziert werden.
4. Genaue 1.21.1-Verfügbarkeit von Botania, Thermal, Forestry und Tinkers' Construct (auf Modrinth nicht gefunden).
5. Welche Fluid-Konvention für „Steam“ auf 1.21.1 gilt (Tag `c:steam`?), nötig für Boiler und Steam Turbine.
6. Ob eine echte Regressionsbasis (laufende 1.7.10-Instanz) zum Verhaltensvergleich vorhanden ist.
7. Gewünschte Behandlung von Vanilla-Überschneidungen: Kupfer (EC), Calcit (ReC, anderer Block mit gleichem Namen), Lodestone (ReC, gleicher Name), Composter (RC, andere Funktion).
8. Ob Admin- und Entwicklerwerkzeuge (World Edit Tool, Debug-Item, DragonAPI-Befehle) übernommen werden sollen.
