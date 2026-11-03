#!/bin/sh
# Creates <project>/run/saves/showcase (flat world) with a showcase datapack, using a short dedicated-server run.
# Usage: sh tools/make_showcase.sh [project] [datapack dir]   (default: rotarycraft tools/showcase)
set -e
cd "$(dirname "$0")/.."
PROJ=${1:-rotarycraft}
PACK=${2:-tools/showcase}
RUN=$PROJ/run
mkdir -p "$RUN"
echo "eula=true" > "$RUN/eula.txt"
cat > "$RUN/server.properties" <<'P'
online-mode=false
level-name=saves/showcase
level-type=minecraft\:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
generate-structures=false
spawn-protection=0
P
# start from a fresh world (a player who died there would otherwise stay dead)
rm -rf "$RUN/saves/showcase"
mkdir -p "$RUN/saves/showcase/datapacks"
rm -rf "$RUN/saves/showcase/datapacks/rcshow"
cp -R "$PACK" "$RUN/saves/showcase/datapacks/rcshow"
# Generate the world: start the dedicated server once and stop it as soon as it is up (the datapack builds the scene on load).
LOG="$RUN/logs/latest.log"
rm -f "$LOG"
./gradlew :$PROJ:runServer --console=plain > /dev/null 2>&1 &
PID=$!
i=0
until grep -q "Done (" "$LOG" 2>/dev/null; do
    sleep 2; i=$((i+1))
    if [ $i -gt 150 ]; then echo "server did not start"; kill $PID; exit 1; fi
done
sleep 5
pkill -f "$PROJ.*runServer|net.neoforged.*server" 2>/dev/null || true
kill $PID 2>/dev/null || true
wait $PID 2>/dev/null || true
test -f "$RUN/saves/showcase/level.dat" && echo "showcase world created"
