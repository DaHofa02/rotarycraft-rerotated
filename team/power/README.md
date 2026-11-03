# RotaryCraft / ReactorCraft / ElectriCraft – private NeoForge 1.21.1 port

Private port of Reika Kalseki's 1.7.10 mods. **Not for distribution** (see `../docs/01-original-mods.md` §5).

## Requirements
- Java 21 (e.g. Amazon Corretto or Temurin 21)
- Internet access for the first build (Gradle, NeoForge, Minecraft are downloaded)

## Build
```bash
./gradlew build
```
Outputs:
- `rotarycraft/build/libs/rotarycraft-1.21.1-<version>.jar`
- `reactorcraft/build/libs/reactorcraft-1.21.1-<version>.jar` (no content yet)
- `electricraft/build/libs/electricraft-1.21.1-<version>.jar` (no content yet)

## Tests
```bash
./gradlew :rotarycraft:test
```
```bash
./gradlew :rotarycraft:runGameTestServer
```

## Run in development
```bash
./gradlew :reactorcraft:runClient
```
The `reactorcraft` run configurations load all three mods; `rotarycraft:runClient` loads RotaryCraft alone.

## Install
Copy `rotarycraft-*.jar` (and, once they have content, `reactorcraft-*.jar` / `electricraft-*.jar`) into the `mods` folder of a NeoForge **21.1.x** installation for Minecraft **1.21.1**. ReactorCraft and ElectriCraft require RotaryCraft. No other mods are needed.

## Configuration
`config/rotarycraft-common.toml`. With the default `tableMachines = false`, machines are only craftable in the Worktable, as in the original.

## Test instance
```bash
./start_test_instance.sh --demo
```
Starts a development client with all three mods; `--demo` adds a creative test world with the ported machines.

## Regenerating converted assets and data
The originals are expected next to this folder (`../RotaryCraft`) and the original JAR extracted somewhere:
```bash
python3 tools/gen_rotarycraft_assets.py ../RotaryCraft <extracted RotaryCraft jar>
```
```bash
python3 -I tools/gen_rotarycraft_recipes.py
```
