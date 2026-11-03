# 04 – Assets und Ressourcen

Grundsatz (start.md, Regel 6): Alle vorhandenen Texturen, Modelle, Animationen, Sounds, Übersetzungen und Texte werden **unverändert übernommen**, wo das Zielsystem sie lesen kann. Neue Assets entstehen nur, wenn ein Format auf 1.21.1 nicht mehr nutzbar ist. Auch dann wird nur das Format konvertiert, nicht die Gestaltung, und jeder Fall wird dokumentiert.

## 1. Fundorte

Assets liegen an **zwei** Stellen, die beide nötig sind:

| Ort | Inhalt |
|---|---|
| Original-**JAR**, `assets/<modid>/` | Block- und Item-Icons (`textures/blocks`, `textures/items`), Rüstungstexturen (`textures/models/armor`), `.mcmeta`-Animationen, **Sprachdateien** (`lang/*.lang`). **Nur hier vorhanden, nicht im Quellbaum.** |
| **Quellbaum**, `<Mod>/Textures`, `/Sounds`, `/Resources`, `/Shaders` | Modell- und Tile-Entity-Texturen, GUI-Texturen, Item-Sprite-Sheets, Sounds (OGG), Handbuchtexte (XML), Shader (GLSL). Zur Laufzeit per Classpath-Pfad geladen (z. B. `/Reika/RotaryCraft/Textures/…`). Ebenfalls im JAR enthalten. |

## 2. Bestandsliste

### RotaryCraft
| Typ | Anzahl | Ort | Bemerkung |
|---|---|---|---|
| Tile-Entity-/Modelltexturen | 193 | `Textures/TileEntityTex` | meist 128×128, zu den Java-Modellen gehörig |
| GUI-Texturen | 120 | `Textures/GUI` | |
| Projektor-Folien | 24 | `Textures/Projector` | |
| Misc / Entity / Terrain | 15 / 6 / 5 | `Textures/…` | Terrain: Sprite-Sheets für Flüssigkeiten und Rohre |
| Item-**Sprite-Sheets** | 11 | `Textures/Items` | `items.png`–`items4.png` 256×256 (16×16-Raster, Index = Zahl in `ItemRegistry`), Extract-/Ingot-Sheets 64×16 |
| Block-Icons | 230 | JAR `textures/blocks` | inkl. 60 in `blocks/old` (vermutlich ungenutzt, **zu prüfen**) |
| Item-Icons | 240 | JAR `textures/items` | davon **239 in `items/tinkertools`** (Tinkers-Construct-Werkzeugteile aus HSLA/Bedrock) |
| Rüstungstexturen | 13 | JAR `textures/models/armor` | |
| `.mcmeta`-Animationen | 13 | JAR | |
| Sounds | 49 OGG | `Sounds/` | 46 Registry-Einträge; „#“-Präfix = Endlosschleife |
| Sprachdateien | 6 (en_US mit 525 Schlüsseln, de_DE, pl_PL, pt_BR, ru_RU, zh_CN) | JAR `lang/` | |
| Handbuch | 12 XML + Übersetzungen (es_ES, fr_FR, hu_HU, pl_PL, pt_BR) | `Resources/` | `categories, converter, engines, info, machines, misc, resource, thaum, tools, trans …` |
| Achievement-Texte | `AchievementDesc.txt` | `Resources/` | |
| Musik | `demomusic.rcmusic`, `demomusic.txt` | `Resources/` | Demo-Lied für die Music Box |
| Shader | 2 Paare (`heatripple`, `heatglow`, .vert/.frag) | `Shaders/` | |
| Modelle | 122 Java-Modellklassen | `Models/`, `ModInterface/` | Techne-Export (Box-Listen mit UV-Offsets) |
| Logos | `logo.png`, `logo2.png` | Wurzel | für `logoFile` in `neoforge.mods.toml` |

### ReactorCraft
| Typ | Anzahl | Ort | Bemerkung |
|---|---|---|---|
| Tile-Entity-Texturen | 28 | `Textures/TileEntity` | |
| GUI | 17 | `Textures/GUI` | |
| Effekttexturen | 12 (`fusion*`, `plasma1–7`, `radiation*`) | `Textures/` | |
| Item-Sprite-Sheet | 1 | `Textures/Items` | |
| Block-Icons | 228 | JAR | 78 Multiblock-, 20 Erz-, 20 Material-, 17 Fluid-, 12 Lampentexturen, Abfall-Zerfallsstufen |
| Rüstung | 2 | JAR | Hazmat |
| `.mcmeta` | 27 | JAR | |
| Sounds | 8 OGG | `Sounds/` | 7 Registry-Einträge (`turbine.ogg` + `turbine-vol.ogg`) |
| Sprache | 5 (en_US 251 Schlüssel, de_DE, pt_BR, ru_RU, zh_CN) | JAR | |
| Handbuch | 20 XML (inkl. es_ES, pt_BR, ru_RU) | `Resources/` | |
| Modelle | 21 Java-Modellklassen | `Models/` | |

### ElectriCraft
| Typ | Anzahl | Ort | Bemerkung |
|---|---|---|---|
| TE-Texturen | 11 | `Textures/` | Motor, Generator, Sicherung (4 Hitzestufen), Relais, Widerstand, Transformator, Messgerät |
| GUI / Items | 2 / 1 | `Textures/` | |
| Block-Icons | 91 | JAR | 36 Draht, 16 Batterie, 8 „striated“, 7 Wireless, Erze; 11 in `blocks/old` |
| `.mcmeta` | 3 | JAR | RF/EU-Kabel-Animationen |
| Sprache | 4 (en_US 70 Schlüssel, de_DE, ru_RU, zh_CN) | JAR | |
| Handbuch | 6 XML | `Resources/` | |
| Modelle | 7 Java-Modellklassen | | |

### DragonAPI
| Typ | Anzahl | Bemerkung |
|---|---|---|
| PNG | 14 im Quellbaum, 9 Block-Icons im JAR | Hilfs-Icons, Overlays |
| Shader | `reika_effect`, `reika_stencil` (.vert/.frag) | |
| Text | 8 TXT, 1 XML | u. a. Listen, Changelog-Daten |

## 3. Kompatibilität mit 1.21.1 und notwendige Umwandlungen

| Asset | Weiterverwendbar? | Notwendige Maßnahme (nur Format, kein Redesign) |
|---|---|---|
| PNG-Texturen (Block, Item, TE, GUI, Entity) | **Ja** | Neue Pfade: `textures/block/…`, `textures/item/…` (Singular). Dateinamen klein schreiben, `#` und Leerzeichen entfernen (Resource-Location-Regeln: `[a-z0-9/._-]`). Beispiele: `wastedecayer_#0.png`, `textures (2).png`. |
| `.mcmeta` | **Ja** | Format weitgehend gleich; `pack.mcmeta` mit `pack_format` für 1.21.1 neu anlegen |
| Item-Sprite-Sheets (256×256) | **Nein, als Sheet nicht** | 1.21 kennt nur Einzeltexturen. Die Sheets werden **pixelgenau in 16×16-Einzelbilder zerlegt** (automatisches Skript, keine Bildänderung). Das gilt als „technisch erzwungene Konvertierung“. |
| Terrain-Sheets (`liquids.png`, `piping.png`) | wie oben | Zerlegen. Fluide brauchen `still`/`flow`-Texturen, die aus den vorhandenen Teilbildern genommen werden. |
| Java-Modelle (Techne, `ModelRenderer`) | **Nicht direkt** | Zwei Wege: (a) Umsetzung in 1.21-`ModelPart`/`LayerDefinition` mit **identischen Boxen und UVs** (mechanisch übertragbar, die Box-Daten stehen im Code), gerendert per `BlockEntityRenderer`; (b) statische Teile als JSON-Blockmodell. Vorschlag: (a) für bewegte Teile, (b) nur bei rein statischen Blöcken. Geometrie bleibt gleich. |
| ISBRH-Blockrenderer (Rohre, Kabel, Drähte, verbundenes Glas) | **Nein** | Ersatz durch Multipart-Blockstates mit JSON-Modellen bzw. BER. Gleiches Aussehen, neue Datenform. |
| OGG-Sounds | **Ja** | `sounds.json` anlegen, `SoundEvent`s registrieren. Schleifen-Sounds als `AbstractTickableSoundInstance` (Client). |
| `.lang`-Dateien | **Inhalt ja, Format nein** | Umwandlung in `lang/*.json` mit **neuen Schlüsseln** (`block.rotarycraft.gearbox` …). Texte werden 1:1 übernommen; die Zuordnung alter zu neuen Schlüsseln wird als Tabelle dokumentiert. Dateinamen werden klein geschrieben (`de_de.json`). |
| Handbuch-XML | **Ja** | Bleibt XML. Laden über Resource-Manager (`assets/<modid>/handbook/…`) statt Classpath |
| GLSL-Shader (GLSL 1.x, Fixed-Function-Ära) | **Wahrscheinlich nein** | 1.21 nutzt Core-Profile-Shader mit Shader-JSON (`RegisterShadersEvent`). Der GLSL-Code muss angepasst werden, der Effekt bleibt gleich. Risiko siehe 05. |
| `.rcmusic` / Music-Box-Daten | **Ja** | Eigenes Format, Parser wird portiert |
| `items/tinkertools` (239) | **Ungenutzt** | Tinkers' Construct ist für 1.21.1 nicht verifiziert. Die Dateien werden archiviert, aber nicht eingebunden (Freigabe). |
| `blocks/old` (71 insgesamt) | **Vermutlich ungenutzt** | Vor der Entscheidung prüfen, ob noch referenziert. Nicht löschen, höchstens nicht einbinden. |

## 4. Fehlende oder unklare Assets

- **Deepslate-Erzvarianten:** Seit 1.17 hat jedes Oberwelt-Erz eine Deepslate-Variante. Die Originale haben dafür **keine Texturen**. Möglichkeiten: (a) keine Deepslate-Variante, Erze nur im Steinbereich; (b) neue Texturen erzeugen (Overlay auf die Vanilla-Deepslate-Textur). (b) wäre ein **neues Asset** → **Freigabe nötig**. Vorschlag: (a).
- **Item-Modelle für ehemals per Renderer gezeichnete Items** (Maschinen-Platzier-Items mit `ItemMachineRenderer`): ein BEWLR (`IClientItemExtensions`) mit demselben Java-Modell, also ohne neues Asset.
- **`poison`-Fluid (RotaryCraft):** Textur und Herkunft sind unklar.
- **Lang-Abdeckung:** Viele Namen stehen nicht in `.lang`, sondern fest im Code (z. B. ReactorCraft „Part Placer“, „Raw Materials“). Sie werden in die JSON-Sprachdateien übernommen. Für Sprachen außer Englisch fehlen solche Einträge dann, was dem Original entspricht.
