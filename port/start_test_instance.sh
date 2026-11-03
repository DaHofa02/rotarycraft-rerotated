#!/bin/sh
# Starts a Minecraft 1.21.1 test client with RotaryCraft, ReactorCraft and ElectriCraft loaded (development run, offline player "Dev"),
# together with JEI (recipe and multiblock viewer of the ports), Mekanism and GregTech CEu Modern (compatibility checks; GregTech is
# copied into reactorcraft/run/mods, its FE <-> EU converters are switched on in reactorcraft/run/config/gtceu.yaml). In a world, the op
# commands "/reikadev compat" (every block of the three mods plus working Mekanism/vanilla combinations), "/reikadev base" (a working
# base built block by block, with Mekanism and GregTech power lines) and "/reikadev tokamak" build test scenes.
#
#   ./start_test_instance.sh          start the client; create or pick a world in the menu
#   ./start_test_instance.sh --demo   additionally put the "RotaryCraft Demo" world (flat, creative, the ported machines
#                                     set up and running) into the world list
#
# The instance lives in reactorcraft/run (worlds: reactorcraft/run/saves). Needs Java 21; the first start downloads
# Minecraft/NeoForge and takes a few minutes.
set -e
cd "$(dirname "$0")"

if [ -z "$JAVA_HOME" ] && [ -x /usr/libexec/java_home ]; then
    JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
    export JAVA_HOME
fi
if ! "${JAVA_HOME:+$JAVA_HOME/bin/}java" -version 2>&1 | grep -q 'version "21'; then
    echo "Java 21 is required (set JAVA_HOME to a Java 21 installation)." >&2
    exit 1
fi

if [ "$1" = "--demo" ]; then
    echo "Creating the demo world ..."
    rm -rf rotarycraft/run/saves/showcase
    ./tools/make_showcase.sh
    mkdir -p reactorcraft/run/saves
    rm -rf "reactorcraft/run/saves/RotaryCraft Demo"
    cp -R rotarycraft/run/saves/showcase "reactorcraft/run/saves/RotaryCraft Demo"
    rm -f "reactorcraft/run/saves/RotaryCraft Demo/session.lock"
    echo "Demo world ready: under Singleplayer it is listed as \"saves/showcase\" (folder: RotaryCraft Demo)."
fi

exec ./gradlew :reactorcraft:runClient
