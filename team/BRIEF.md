# Practice factory - common brief for every zone agent

Project: private NeoForge 1.21.1 / Java 21 port of the Minecraft mods RotaryCraft, ReactorCraft, ElectriCraft (Gradle, ModDevGradle).
You work ONLY in your own copy: /Users/hofer/Downloads/minecraft_update/team/<zone>/ (a full copy of the project; never touch
/Users/hofer/Downloads/minecraft_update/port or another agent's copy). Docs of the port (German) are in /Users/hofer/Downloads/minecraft_update/docs
(08-implementation-status.md describes every ported machine and how it works - read the parts for your blocks). Original 1.7.10
sources for reference: /Users/hofer/Downloads/minecraft_update/{RotaryCraft,ReactorCraft,ElectriCraft,DragonAPI}.

## Goal
A practice GameTest "factory" zone: a simulated SURVIVAL player builds every block of your list into WORKING setups - the way a real player
would use them (machines connected, powered, fuelled, producing) - and the test checks they really work.

## Rules (strict)
1. Read reactorcraft/src/main/java/reika/reactorcraft/gametest/factory/Factory.java first and use it.
2. The ONLY supplies: Factory.infiniteChest(h, pos, stacks...) for items and Factory.fluidBarrel(h, pos, fluid) for fluids. The player
   (reika.rotarycraft.gametest.SimPlayer, GameType.SURVIVAL) starts empty and gets every item with Factory.take(p, chest, item, n) through
   the chest GUI. Fluids reach machines from barrels through pipes (RotaryCraft pipe/hose/fuel_line/bedrock_pipe - laid by the player), or
   buckets/canisters the player fills at a barrel or machine. Tools (pickaxe, screwdriver, wire cutter ...) also come from a chest.
3. No shortcuts: besides the supplies and SimPlayer.floor(...) no setBlock; never set energy, heat, temperature, speed, torque, fluid
   amounts or inventories directly. The player places blocks with SimPlayer.place/placeSneaking/build, uses buckets/tools with use/useOn,
   fills GUIs with open/putInto, takes outputs with shiftClick. Power comes from engines/reactors/generators the player built and fuelled.
   Reading state for assertions (getters on block entities) is fine.
4. Put your zone in ONE new class (more GameTest methods in it are fine) in package reika.reactorcraft.gametest.factory, file
   reactorcraft/src/main/java/reika/reactorcraft/gametest/factory/Zone<Name>.java, annotated like the other tests:
   @GameTestHolder(ReactorCraft.MOD_ID) @PrefixGameTestTemplate(false), template Factory.BIG ("platform_48x12x48", floor y=1 after
   SimPlayer.floor(h,0,0,47,47), build at y>=2) or the smaller platform_9x8x9; batch = "factory"; generous timeoutTicks for warm-ups.
   It MUST have `public static final List<String> BLOCKS = List.of(...)` with the registry ids of every block it builds (your list
   below - all of them), and each test method must end with Factory.assertBuilt(h, <its blocks>, from, to) plus real "it works" checks
   (output produced, speed/torque/power reached, temperature rising, energy stored, fluid moved ...).
5. Look at the existing live tests for patterns: rotarycraft/.../gametest/LiveBaseTests.java, electricraft/.../gametest/LiveElectricTests.java,
   reactorcraft/.../gametest/LiveReactorTests.java, and the many machine tests in the gametest packages (they show which inputs, powers,
   orientations and fuels each machine needs - but they use setBlock shortcuts which you must NOT use).
6. Run: in your copy `./gradlew :reactorcraft:runGameTestServer > /tmp/... 2>&1` (all tests of the three mods; 1-3 min; timeout 600000 ms,
   or background). Grep "failed at" and "required tests". All tests, old and yours, must pass at the end. Debugging: read the log lines,
   add temporary prints, and look at how the machine code works.
7. If you find a REAL bug in the mods (not in your test), you may fix it in your copy with a minimal change and a comment. Do not refactor,
   do not reformat, do not change other tests. List every changed non-test file in your report.
8. If a block truly cannot work in a GameTest (needs sky light, real time beyond reason, randomness) still place it and assert the most
   that can be checked (e.g. it is formed / connected / ticking), and say so in the report.

## Report (your final answer, under 500 words)
- Your zone class path and its test methods with one line each on what is built and checked.
- The final summary line of your last full run ("All N required tests passed :)").
- Every non-test file you changed (path + one line why) - the lead merges them.
- Anything you could not make work, with the reason.
