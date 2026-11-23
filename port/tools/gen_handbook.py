#!/usr/bin/env python3
"""Writes the port's own handbooks: RotaryCraft, ReactorCraft and ElectriCraft (assets/<mod>/handbook/en_us.json, English only). All text
here is written for the port, describing how the port's machines behave; none of it comes from the original mods' handbooks.
Usage: python3 -I tools/gen_handbook.py
"""
import json
import os

PORT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")

BOOKS = {}
C = None


def book(mod):
    """Starts (or continues) the book of a mod; the following cat() calls add to it."""
    global C
    C = BOOKS.setdefault(mod, [])



def cat(title, *entries):
    C.append({"title": title, "entries": [dict(id=e[0], text=e[1], **({"icon": e[2]} if len(e) > 2 else {}), **({"title": e[3]} if len(e) > 3 else {})) for e in entries]})


book("rotarycraft")

cat("Basics",
    ("intro", "Machines here run on shaft power: a speed (rad/s) and a torque (Nm), whose product is the power in watts. Engines make it, shafts, gears and belts carry it, and machines use it. Most machines need a minimum torque, speed and power; more power usually means faster work. Use this book while looking at a machine to jump to its page.", "rotarycraft:handbook"),
    ("tools_intro", "The screwdriver turns machines and changes their settings, the angular transducer (meter) reads speed, torque and power at any machine. Lubricant keeps gearboxes alive; without it they wear out. Coloured lines on machines show where power goes in (blue) and out (red) when you hold a screwdriver.", "rotarycraft:screwdriver"),
)

cat("Engines",
    ("dc_engine", "The smallest engine: 4 Nm at 256 rad/s from a redstone signal. Good for testing and small tools."),
    ("wind_engine", "8 Nm at up to 1024 rad/s. Needs open sky and works best high up with plenty of free air around it."),
    ("steam_engine", "32 Nm at 512 rad/s. Burns heat into steam: give it water and a heat source below (fire or lava)."),
    ("gas_engine", "128 Nm at 512 rad/s on ethanol from below and water at the back. A solid early workhorse."),
    ("ac_engine", "512 Nm at 256 rad/s. Runs on a magnetized shaft core and an alternating redstone signal (or the redstone upgrade)."),
    ("sport_engine", "256 Nm at 1024 rad/s on ethanol with water and an additive; faster and stronger than the gas engine."),
    ("hydro_engine", "Huge torque (16384 Nm) at a low 32 rad/s from falling water flowing past it; chain several for a waterfall plant."),
    ("micro_engine", "A small turbine: 16 Nm at 131072 rad/s on jet fuel. Very fast; use gears to trade speed for torque."),
    ("jet_engine", "1024 Nm at 65536 rad/s on jet fuel, loud and dangerous: it pulls in and shreds what stands in front. The afterburner upgrade adds more."),
    ("ecu", "The engine control unit sits under a piston or turbine engine and sets how hard it runs, trading power for fuel."),
    ("magnetic_engine", "Turns Forge Energy (FE) at its back into shaft power. Six tiers (magnet upgrades) from 8 Nm up to 8192 Nm; choose the speed in its GUI. It heats up with power; liquid nitrogen keeps it cool, without it it explodes at 500 C. 520 W of shaft power per FE a tick (config: wattsPerFE)."),
    ("fuel_engine", "2048 Nm at up to 256 rad/s on liquid fuel from other mods (diesel, gasoline and the like). Needs lubricant; water keeps it cool. It slows down when hot and explodes at 750 C."),
    ("steam_turbine", "The magnetostatic engine's design running on steam (from another mod, or the boiler) taken in at the back. Same tiers, upgrades and speed setting."),
    ("pneumatic_engine", "Needs PneumaticCraft. The converter engine running on compressed air fed into its back."),
)

cat("Transmission",
    ("shaft", "Carries power in a straight line. Weak materials break under too much speed or torque; bedrock shafts never break. A shaft with its output facing a portal turns into a portal shaft."),
    ("gearbox", "Trades speed for torque (or back) by 2, 4, 8 or 16. Needs lubricant; worn gears lose efficiency and finally break."),
    ("bevel_gears", "Turns power round a corner or up and down. Placed last next to its input, it finds its direction by itself; the screwdriver changes it."),
    ("clutch", "A shaft you can switch with redstone."),
    ("multi_clutch", "Sends power to one of several outputs depending on the redstone strength."),
    ("shaft_junction", "Joins two inputs into one output (the power adds when the speeds match) or splits one input into two."),
    ("flywheel", "Stores rotation: it takes time to spin up and keeps the output going for a while when the input stops. Heavier flywheels need more torque."),
    ("worm_gear", "A fixed 64:1 reduction for lots of torque at low speed, with some loss."),
    ("high_gear", "A 256:1 gear for very large ratios; sneak with the screwdriver to switch between torque and speed mode."),
    ("cvt", "A continuously variable transmission: set the ratio by hand, by redstone, or let it choose. Needs belts and lubricant."),
    ("industrial_coil", "Winds up power to store it and gives it back as a steady output. Overcharging it is dangerous."),
    ("bedrock_coil", "The bedrock version of the industrial coil: far more capacity."),
    ("belt_hub", "Pairs of belt hubs carry power over a distance through a belt; the belt can slip and wear."),
    ("chain_drive", "Like the belt hub with a chain: stronger, no slip."),
    ("split_belt", "A belt that takes power off a passing line."),
    ("distribution_clutch", "Divides one input between several outputs in settable shares."),
    ("bus_controller", "Feeds a power bus: a set of bus blocks that hand power to the machines touching them."),
    ("power_bus", "Part of a power bus network; set each side to give power or not."),
    ("dynamometer", "Measures and shows the power passing through it."),
    ("portal_shaft", "Appears when a shaft's output faces a nether or end portal. With the same kind of portal and a shaft at the matching spot on the other side, it carries power between dimensions and keeps both ends loaded."),
    ("dynamo", "Takes shaft power at its back and pushes FE into the receiver in front: up to 1024 Nm (2048 with the flux upgrade) and 8192 rad/s count, 520 W per FE a tick."),
)

cat("Processing",
    ("grinder", "Grinds items into their ground form (ores into dust, canola into lubricant). At least 128 Nm and 4096 W."),
    ("extractor", "Four stages from ore to flakes, each with its own power need: drill, two washing steps with water, and drying. Up to 16 flakes per ore."),
    ("blast_furnace", "A heated furnace for alloys and steel; needs heat from a friction heater or similar."),
    ("worktable", "A crafting table that can also craft RotaryCraft machines and repair or upgrade some items."),
    ("friction_heater", "Heats the furnace or blast furnace in front of it with shaft power."),
    ("fermenter", "Makes yeast from sugar, or sludge from plants, given yeast, water and warmth."),
    ("compactor", "Squeezes coal into anthracite, then prismane and lonsdaleite under huge pressure and heat."),
    ("centrifuge", "Spins items into their parts with chances, some with a fluid output."),
    ("fractionator", "Makes jet fuel from its ingredients and a ghast tear at high speed."),
    ("pulse_jet_furnace", "A pulse-jet heated furnace that melts and recycles metal, with water cooling."),
    ("big_furnace", "A wide furnace that smelts many stacks at once with shaft power."),
    ("magnetizer", "Magnetizes shaft cores for the AC engine; needs speed and an alternating redstone signal. The lodestone upgrade makes it faster and stronger."),
    ("crystallizer", "Freezes fluids into their solid form when it is cold enough."),
    ("lava_maker", "Melts stone and other blocks into lava (or other molten fluids) with a lot of power."),
    ("drop_processor", "Turns a block item into what the block would drop when mined (ores into their ores and so on)."),
    ("auto_crafter", "Crafts from craft patterns, taking the ingredients from the inventory above it. Request, continuous and keep-in-stock modes."),
    ("purifier", "Turns other mods' steel into RotaryCraft steel. Needs 16384 W, 600 C (only fire or lava next to it get it that hot), gunpowder and sand."),
    ("distillery", "Power from below: crude oil becomes six times as much lubricant; bioethanol and biofuel become ethanol. Output at the top."),
    ("boiler", "Shaft power from below heats water into steam (from another mod's steam fluid). The steam goes out at the top."),
    ("grindstone", "Sharpens and repairs tools with shaft power."),
    ("drying_bed", "Dries fluids into their solid form in the sun."),
    ("composter", "Turns plant matter into compost when kept warm with yeast."),
)

cat("Farming and nature",
    ("fertilizer", "Speeds up plant growth around it with a fertilizer fluid."),
    ("sprinkler", "Waters crops in a circle below it, from a pipe."),
    ("lawn_sprinkler", "A wider ground sprinkler for lawns and fields."),
    ("woodcutter", "Cuts down the tree in front of it and collects the wood; may replant."),
    ("mob_harvester", "Damages mobs standing on it, as if a player had killed them."),
    ("auto_breeder", "Feeds the animals around it so they breed."),
    ("bait_box", "Attracts or repels mobs depending on the bait inside."),
    ("defoliator", "Sprays poison to kill leaves and plants in an area."),
    ("terraformer", "Turns the biome around it into the one chosen in its GUI, using an item (and water for wet biomes) for each patch."),
    ("aggregator", "Condenses water out of the air, more where it is humid."),
    ("canola", "Canola grows into seeds that grind into lubricant."),
)

cat("Fluids and pipes",
    ("pipe_pump", "Pumps fluid from the block or tank below it into pipes."),
    ("pump", "Pumps water or lava out of the world into pipes."),
    ("reservoir", "Stores fluid; reservoirs side by side share their contents."),
    ("fluid_compressor", "Compresses a lot of fluid into a small tank."),
    ("filling_station", "Fills fluid containers, and takes fuel items into its tank."),
    ("bucket_filler", "Fills or empties buckets from the pipes connected to it."),
    ("spiller", "Pours its fluid into the world around it."),
    ("spillway", "Lets water flow over it to power or route it."),
    ("hydrator", "Makes blocks around it wet."),
    ("wetter", "Wets items in a fluid."),
    ("refrigerator", "Cools things down and makes liquid nitrogen."),
    ("air_compressor", "Needs PneumaticCraft. Pumps shaft power as compressed air into the PneumaticCraft machine in front, 1 mL per 520 W a tick (config: wattsPerFE)."),
    ("rotary_to_create", "Needs Create. Shaft power goes in at its back (steel side), a Create shaft turns at its front (casing side): speed/16 RPM, at most 256 RPM, with a stress capacity of power/16 W less 5 % SU. Ratio, loss and speed mapping are in the config (conversion.create)."),
    ("create_to_rotary", "Needs Create. A Create shaft drives it at the casing side; it takes a stress load you choose (right-click doubles it, sneak-right-click halves it, 1 to 1024 SU per RPM) and gives shaft power at the steel side: RPM*16 rad/s and load*RPM*16 W less 5 %. Overstressed, it stops."),
)

cat("World and utility",
    ("fan", "Blows entities away, cools machines and can spread fire or harvest leaves, depending on speed."),
    ("blower", "Blows items through it from one inventory to another."),
    ("vacuum", "Pulls in items and experience around it."),
    ("heater", "Heats machines next to it."),
    ("cooling_fin", "Carries heat away from the machine it is attached to."),
    ("lamp", "A strong light, powered by shaft power."),
    ("floodlight", "Lights a long beam ahead of it."),
    ("light_bridge", "Projects a solid bridge of light that you can walk on."),
    ("bridge", "A block of the light bridge."),
    ("beam_mirror", "Reflects a beam, such as the heat ray's or sunlight, to where it points."),
    ("line_builder", "Pushes a line of blocks forward, one at a time."),
    ("filler", "Fills an area with blocks."),
    ("obsidian_maker", "Makes obsidian from water and lava."),
    ("firestarter", "Lights fires around it."),
    ("aerosolizer", "Spreads potion effects in the air around it."),
    ("particle_emitter", "Makes particles, for decoration."),
    ("weather_controller", "Changes the weather with the item you put in."),
    ("chunk_loader", "Keeps chunks around it loaded while it gets power; more power, larger radius."),
    ("spawner_controller", "Controls a mob spawner it sits next to: slower, faster or off."),
    ("item_refresher", "Keeps dropped items around it from despawning."),
    ("item_cannon", "Shoots items to a target set in its GUI, even far away."),
    ("music_box", "Plays tunes you enter in its GUI: 16 channels of notes."),
    ("firework_machine", "Builds and launches fireworks from dyes and gunpowder."),
    ("pile_driver", "Hammers down through blocks below it."),
    ("borer", "A powerful digger that cuts a tunnel ahead of it, its shape set in its GUI."),
    ("mining_pipe", "The pipe the borer leaves behind."),
    ("sonic_borer", "Blasts a tunnel with sound."),
    ("bedrock_breaker", "Grinds bedrock into bedrock dust with enormous torque."),
    ("bedrock_slice", "A partly ground bedrock block."),
    ("winder", "Winds springs with shaft power, and unwinds them to run spring tools."),
    ("van_de_graff", "Builds up electric charge from power below and discharges it at things nearby."),
    ("fuel_enhancer", "Mixes additives into fuel for more power."),
    ("solar_mirror", "Focuses sunlight on a solar tower to boil water for power."),
    ("solar_tower", "Collects the light from solar mirrors."),
    ("blast_glass", "Glass that withstands explosions."),
)

cat("Sensors and logistics",
    ("smoke_detector", "Sounds an alarm and gives a redstone signal when smoke or fire is near."),
    ("player_detector", "Gives a redstone signal when a player is near."),
    ("mob_radar", "Shows the mobs around it on a radar screen."),
    ("cave_scanner", "Maps the caves below it."),
    ("gpr", "Ground-penetrating radar: shows what lies under the ground."),
    ("display", "A big screen that shows a text."),
    ("cctv", "A camera you look through from a CCTV screen; needs a wound spring."),
    ("cctv_screen", "Shows the view of the camera with the same three dye colours."),
    ("spy_cam", "Shows a top view of the area around it."),
    ("projector", "Projects a map from its slot onto the first solid wall within 12 blocks."),
    ("sorting", "Sorts items into the inventories next to it by their kind."),
    ("item_filter", "Lets only matching items pass: by item, components, mod or tags."),
    ("scaleable_chest", "A chest with many pages of storage."),
)

cat("Weapons",
    ("force_field", "A dome kept up by power from below (at least 524288 W) and an open sky: it pushes hostile mobs out and stops projectiles not shot by players. More power, larger dome."),
    ("containment", "A dome that keeps hostile mobs in. The wither needs 524288 W, the dragon 2097152 W."),
    ("heat_ray", "A beam that burns and melts what it hits, far ahead."),
    ("air_gun", "Blasts everything standing on the ground in front of it away; needs 512 Nm."),
    ("landmine", "Goes off when stepped on. Gunpowder sets the blast; blaze powder adds fire, a spider eye poison, glass shrapnel and TNT sets off mines next to it."),
    ("self_destruct", "A dead man's switch: while powered it is armed, and when the power stops it blows up after a series of smaller blasts."),
    ("sonic_weapon", "Sends out sound that blinds, hurts and finally kills creatures nearby; the volume falls off with distance. A helmet protects players."),
    ("emp", "Charges up with a huge amount of power and then shuts down all machines within 64 blocks for good. Fires once."),
    ("machine_gun", "Fires arrows straight ahead while something living is in front of it. Needs 1024 rad/s."),
    ("tnt_cannon", "Lobs lit TNT at the target set in its GUI; more power, longer reach."),
    ("block_cannon", "Shoots blocks to the target set in its GUI, where they land."),
    ("railgun", "A turret firing metal slugs (iron, gold, diamond or netherite ingots) at the nearest hostile or stranger within 164 blocks. Needs over 4 MW."),
    ("freeze_gun", "A turret that freezes targets in place with snowballs (snow and ice blocks are turned into snowballs)."),
    ("laser_gun", "A turret that burns targets up to 256 blocks away, without ammunition. Needs over 8 MW."),
    ("multi_cannon", "A rotary turret: a stream of small hits from ball bearings."),
    ("anti_air_gun", "A turret that only fires at flying targets, with flak made of scrap."),
    ("flame_turret", "A turret that sets targets on fire with fuel from its tank: ethanol, jet fuel, oil or other fuels."),
)

cat("Tools and items",
    ("bedrock_tools", "Bedrock tools never wear out. The axe fells a whole tree unless you sneak, the hoe tills 3x3, the scythe cuts the plants around the one you cut, the sword wears down the target's armour. Some come enchanted and stay that way: the pickaxe with silk touch and fortune V, the sword with sharpness V and looting V, the scythe with fortune V. The axe refuses silk touch. With a paxel mod installed you can combine pickaxe, axe and shovel into a bedrock paxel (enchanted like the pickaxe).", "rotarycraft:bedrock_pickaxe"),
    ("bedrock_armor", "Bedrock armour never wears out and protects better than netherite. Each piece carries its own enchantment: helmet projectile protection IV and respiration III, chestplate blast protection IV, leggings fire protection IV, boots feather falling IV. The helmet also lets you see in the dark.", "rotarycraft:bedrock_helmet"),
    ("upgrades", "Engine upgrades are used on a machine: performance (gas to sport engine), afterburner (jet engine), redstone clock, magnet tiers and efficiency (magnetostatic engine), flux (dynamo) and lodestone (magnetizer).", "rotarycraft:upgrade_magnet_1"),
    ("lubricant", "Keeps gearboxes and engines from wearing out. Ground from canola seeds or distilled from oil.", "rotarycraft:lubricant_bucket"),
    ("spring", "A spring wound in the winder runs spring-powered tools and machines like the CCTV.", "rotarycraft:spring"),
)

book("reactorcraft")

cat("Basics",
    ("intro", "This book covers fission, breeding, fusion and the plants that turn their heat into shaft power. Reactor parts trade heat with their neighbours once a second and drift towards the surrounding temperature; most of them melt down or explode when they run too hot. Keep a CPU, control rods and coolant around every core. Use this book while looking at a ReactorCraft block to open its page.", "reactorcraft:handbook"),
    ("radiation", "Fuel, waste and broken reactors give off radiation. It causes radiation sickness, which hurts and lingers. A full hazmat suit shields against all common levels; radiation goggles make contamination clouds visible, the Geiger counter clicks near them, and the radiation cleaner washes them away with water.", "reactorcraft:geiger"),
    ("neutrons", "Fission sends out neutrons that fly in straight lines through blocks. A neutron hitting fuel can split it again, which is what keeps a reactor going; water, graphite and reflectors slow neutrons down so they react more easily, while inserted control rods, concrete and other shielding catch them.", "reactorcraft:fuel_rod"),
)

cat("Fuel processing",
    ("processor", "Works without shaft power. Fluorite crystals and a fluid make hydrofluoric acid in it, and the acid turns uranium ingots (with water) into uranium hexafluoride gas, or molten lithium and emerald dust into thorium reactor salt."),
    ("isocentrifuge", "Spins uranium hexafluoride at very high speed (over 262144 rad/s from below) and sorts it into enriched uranium dust and depleted dust."),
    ("synthesizer", "A vessel heated by its surroundings (fire, lava or a friction heater). Hot enough, it makes ammonia from water, quicklime and ammonium chloride, or preheats molten thorium fuel. The hotter it is, the faster it works."),
    ("fluid_extractor", "Pumps from source blocks on at least three of its sides, for instance heavy water out of the sea. Powered from above or below with at least 512 Nm and 65536 W."),
    ("electrolyzer", "Runs on the discharges of a RotaryCraft Van de Graaff generator next to it; stronger discharges make it work faster. It splits heavy water into deuterium and oxygen, and, once heated past 800 C, salt into chlorine and sodium."),
    ("tritizer", "A column block that holds a fluid in the neutron stream of a reactor; neutrons turn what it holds into a heavier form, such as deuterium into tritium."),
    ("gas_collector", "Fixed to the back of a furnace or similar heat source, it catches the carbon dioxide it gives off."),
    ("fuel", "Uranium fuel pellets go into fuel cores. Each pellet burns down in a hundred steps and leaves nuclear waste behind.", "reactorcraft:fuel"),
    ("breeder_fuel", "Breeder fuel slowly converts in a breeder core; when it is done it comes out as plutonium.", "reactorcraft:breeder_fuel"),
    ("plutonium", "Plutonium fuel burns like uranium but is radioactive to carry.", "reactorcraft:plutonium"),
    ("pellet", "Fuel pebbles for the pebble bed reactor.", "reactorcraft:pellet"),
)

cat("Fission reactors",
    ("fuel_rod", "The fuel core: four fuel slots and room for the waste it makes. Neutrons that hit its fuel split atoms and make heat. Cores stacked in a column pass fuel down and waste on."),
    ("control_rod", "Inserted rods absorb neutrons and calm the reactor; retracted rods let it run. A redstone signal or the CPU moves them, and a SCRAM drops them all at once."),
    ("cpu", "The reactor's control desk: it finds the control rods connected to it, lets you move them together or one by one, and shows the temperatures of parts linked with the remote control."),
    ("coolant_cell", "Holds a bucket's worth of water, heavy water, sodium or fuel salt; it carries heat away and slows neutrons."),
    ("neutron_reflector", "Sends some neutrons back into the reactor and slows them, so less fuel is wasted."),
    ("reactor_boiler", "Heated by the reactor around it, it boils water (or heavy water or ammonia) into steam for the steam line above it."),
    ("breeder_core", "A fast reactor core that burns breeder fuel and turns it into plutonium. It runs hot and is cooled with liquid sodium."),
    ("sodium_heater", "Takes the heat of breeder cores into liquid sodium; the hot sodium then boils water elsewhere."),
    ("pebble_bed", "A gas-cooled core filled with fuel pebbles fed in from the top; the bottom pebble can be taken out when spent."),
    ("co2_heater", "Heats carbon dioxide with the pebble bed's heat for a gas loop."),
    ("thorium_core", "A molten-salt core: liquid thorium fuel flows in at the top and leaves hot at the bottom. A fuel dump under it drains it if it gets too hot."),
    ("fuel_dump", "Sits under a thorium core and drains its fuel when the core overheats, spilling it if it cannot hold it."),
    ("thorium_fuel", "Spilled molten thorium fuel from a fuel dump.", "reactorcraft:lifbefuel_canister", "Thorium Fuel"),
    ("corium", "Molten reactor core: what is left after a meltdown. It burns, spreads and is radioactive.", "minecraft:magma_block", "Corium"),
    ("remotecpu", "Links fuel cores and control rods to a CPU so that it can watch their temperatures.", "reactorcraft:remotecpu"),
)

cat("Steam and power",
    ("steam_line", "Carries steam from boilers to turbines; lines level their steam with each other."),
    ("steam_grate", "Lets steam from the lines out as a rising steam cloud, for open turbine setups."),
    ("steam", "Rising steam clouds; turbines above use them.", "reactorcraft:steam_line", "Steam"),
    ("turbine_core", "A line of up to five turbine stages driven by steam from below; it speeds up as long as steam keeps coming and sends its speed to the flywheel or generator behind it."),
    ("hip_turbine", "The large high-pressure turbine, built as a multiblock from blades, housing and steam injectors. It only runs while the whole structure is in place and must be lubricated."),
    ("turbine_blades", "Blades for the high-pressure turbine multiblock."),
    ("turbine_housing", "The casing of the high-pressure turbine multiblock."),
    ("steam_injector", "Feeds steam and lubricant into the high-pressure turbine multiblock."),
    ("steam_diffuser", "Spreads steam from a line over a turbine's face."),
    ("condenser", "Turns spent steam back into low-pressure water so it can be pumped round again."),
    ("pressurizer", "Pumps low-pressure water back up to boiler pressure, with shaft power from below."),
    ("heat_exchanger", "With shaft power from below, it cools a hot coolant (sodium, carbon dioxide, fuel salt) and boils water with that heat."),
    ("turbine_flywheel", "A ring of flywheel parts on the output side of a turbine: it passes the turbine's speed on as shaft power and smooths it."),
    ("flywheel_core", "The centre of the turbine flywheel ring."),
    ("flywheel_dampener", "Part of the turbine flywheel ring that takes vibration out."),
    ("flywheel_frame", "Part of the turbine flywheel ring that holds it together."),
    ("reactor_generator", "The end of the generator multiblock: it turns the speed of a turbine ten blocks away into a huge torque output."),
    ("generator_core", "The rotor at the middle of the generator multiblock."),
    ("generator_winding", "Copper windings around the generator's rotor."),
    ("generator_housing", "The shell of the generator multiblock."),
    ("steam_bypass", "Lets surplus steam past the generator multiblock."),
    ("turbine_meter", "Placed under a turbine, it reads its speed and steam."),
    ("heat_pipe", "Moves heat, not fluid, from one reactor part to another."),
    ("solar_top", "A sodium receiver on top of a RotaryCraft solar tower: the mirrors heat liquid sodium in it."),
    ("solar_exchanger", "Under a solar tower, it takes the heated sodium and passes its heat on."),
)

cat("Fusion",
    ("fusion_marker", "A building aid: with redstone it outlines where the magnets, injectors and solenoid of a tokamak go."),
    ("toroid_magnet", "One of the 36 magnets of the tokamak ring; each one must point at the next to steer the plasma round."),
    ("solenoid", "The central magnet of the tokamak, powered from below with high torque; it holds the plasma in place."),
    ("fusion_injector", "Part of the ring that injects hot deuterium and tritium plasma."),
    ("fusion_heater", "Preheats plasma with the beam of a heat ray until it is hot enough to fuse."),
    ("magnetic_pipe", "Carries plasma between the heater and the injectors; it must be kept magnetized."),
    ("neutron_absorber", "Catches the fast neutrons from fusion and turns them into heat for a boiler or heat exchanger."),
    ("gas_duct", "A duct for gases such as hydrogen, carbon dioxide or ammonia."),
    ("magnet_0", "Permanent magnets made in the compactor from lodestone, each level from the one before; the strongest go into the solenoid.", "reactorcraft:magnet_0", "Magnets"),
)

cat("Waste",
    ("waste", "Spent fuel turns into nuclear waste. It never despawns, is highly radioactive and contaminates the area if dropped. Store it.", "reactorcraft:waste"),
    ("waste_container", "Holds short-lived waste while it cools down; water next to it helps."),
    ("waste_storage", "Locks long-lived waste away for good; it keeps decaying even while the game is closed."),
    ("waste_decayer", "Lets waste decay faster; feed it from the top and take the decayed products out at the bottom."),
    ("waste_pipe", "A concrete duct for liquid nuclear waste."),
)

cat("Materials and gear",
    ("pitchblende_ore", "Uranium ore. ReactorCraft also adds ores for thorium, cadmium, indium, silver, calcite, ammonium chloride and magnetite, some only in the Nether or the End.", "reactorcraft:pitchblende_ore", "Ores"),
    ("blue_fluorite", "Fluorite comes in eight colours and is ground for the fluorine the processor needs. Neutrons and radiation make fluorite glow.", "reactorcraft:blue_fluorite", "Fluorite"),
    ("calcite_block", "A storage block of calcite."),
    ("concrete", "A heavy building block that stops neutrons and radiation well."),
    ("graphite_block", "A block of graphite; it slows neutrons down."),
    ("empty_canister", "Carries a bucket's worth of any ReactorCraft fluid or gas.", "reactorcraft:empty_canister"),
    ("heavy_water_bucket", "Heavy water slows neutrons without catching them; it works as coolant and moderator.", "reactorcraft:heavy_water_bucket"),
    ("hazmat_helmet", "The hazmat suit: all four pieces together protect against radiation.", "reactorcraft:hazmat_helmet", "Hazmat Suit"),
    ("goggles", "Worn on the head, they make contamination clouds visible.", "reactorcraft:goggles"),
    ("geiger", "Clicks when radiation is close; the closer, the faster. It needs a charge.", "reactorcraft:geiger"),
    ("cleaner", "Takes in water from a source block and sprays it to wash contamination away.", "reactorcraft:cleaner"),
    ("ironfinder", "Held, it points at magnetic ores close by.", "reactorcraft:ironfinder"),
)

book("electricraft")

cat("Basics",
    ("intro", "ElectriCraft carries shaft power as electricity over wires. A generator turns shaft power into voltage and current, wires carry it, and a motor at the other end turns it back into shaft power. Voltage follows the generator's speed, current its torque. Use this book while looking at an ElectriCraft block to open its page.", "electricraft:handbook"),
    ("copper_wire", "Wires join into a network. Each metal has its own current limit and resistance: steel and tin carry little, copper, silver and gold much more, and superconductors carry anything with no loss. Too much current melts a wire into lava. Insulated and bare wires of a metal carry the same.", "electricraft:copper_wire", "Wires"),
)

cat("Power",
    ("generator", "Takes shaft power on its front and feeds the wire network at its back: the voltage is eight times the speed, the current an eighth of the torque. It explodes far above 400 MW. The screwdriver turns it."),
    ("motor", "Takes current from the network and drives the machine at its back: the torque is eight times the current, the speed an eighth of the voltage. With several motors on one network, each only takes its share; an amplifier (red gold, tungsten or bedrock ingots) lets one draw more. Wool next to it makes it quieter."),
    ("transformer", "Steps voltage up or down with the turns ratio set in its GUI, trading voltage for current. It loses a little, warms up with the power it carries, and explodes when it carries too much current or gets too hot."),
    ("redstone_battery", "Batteries charge from the network on every side but the top and, with a redstone signal, give out a fixed voltage and current from the top. Six tiers, from redstone to nether star; the charge stays on the item when broken.", "electricraft:redstone_battery", "Batteries"),
)

cat("Wire parts",
    ("resistor", "Limits the current that passes through it to the value of its coloured bands (two digits and a multiplier). Right-click a band with a dye to change it."),
    ("precise_resistor", "A resistor with three digits for finer values."),
    ("relay", "Conducts only while it has a redstone signal."),
    ("fuse", "Burns out for good when the current goes over its rating; replace it afterwards."),
    ("meter", "Shows the voltage and current flowing through it and gives a comparator signal for the power."),
)

cat("Forge Energy",
    ("rf_cable", "A cable for Forge Energy (FE) that links energy blocks of any mod; right-click it to set the network's limit."),
    ("rf_battery", "A huge FE store: it takes energy on every side but the top and gives it out of the top, pushing it upwards with a redstone signal."),
    ("rf_crystal", "The energy crystal used to build the FE battery.", "electricraft:rf_crystal"),
    ("basic_wireless_pad", "Wireless chargers pass FE from their sides to the energy block two blocks in front of them. Higher tiers move more and lose less.", "electricraft:basic_wireless_pad", "Wireless Chargers"),
)

cat("Materials",
    ("tin_ore", "ElectriCraft adds ores for tin, silver, nickel, aluminum and platinum; their ingots make the wires.", "electricraft:tin_ore", "Ores"),
    ("bluedust", "Dusts and crystals for batteries and parts: blue, diamond, quartz and crystal dust.", "electricraft:bluedust", "Dusts"),
    ("diamond_crystal", "Battery crystals hold the charge of a battery tier and are crafted into it.", "electricraft:diamond_crystal", "Battery Crystals"),
)

for mod, cats in BOOKS.items():
    out = os.path.join(PORT, mod, "src/main/resources/assets", mod, "handbook/en_us.json")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w") as f:
        json.dump({"categories": cats}, f, indent=1, ensure_ascii=False)
    print(mod, sum(len(c["entries"]) for c in cats), "entries")
