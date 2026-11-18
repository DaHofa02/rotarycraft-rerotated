#!/usr/bin/env python3
"""Writes the port's own RotaryCraft handbook (assets/rotarycraft/handbook/en_us.json). All text here is written for the port, describing how
the port's machines behave; none of it comes from the original mod's handbook. Usage: python3 -I tools/gen_handbook.py
"""
import json
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "rotarycraft/src/main/resources/assets/rotarycraft/handbook/en_us.json")

C = []


def cat(title, *entries):
    C.append({"title": title, "entries": [dict(id=e[0], text=e[1], **({"icon": e[2]} if len(e) > 2 else {})) for e in entries]})


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
    ("magnetic_engine", "Turns Forge Energy (FE) at its back into shaft power. Six tiers (magnet upgrades) from 8 Nm up to 8192 Nm; choose the speed in its GUI. It heats up with power; liquid nitrogen keeps it cool, without it it explodes at 500 C. 520 W of shaft power per FE a tick."),
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
    ("air_compressor", "Needs PneumaticCraft. Pumps shaft power as compressed air into the PneumaticCraft machine in front, 1 mL per 520 W a tick."),
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
    ("bedrock_tools", "Bedrock tools never wear out. The axe fells a whole tree unless you sneak, the hoe tills 3x3, the scythe cuts the plants around the one you cut, the sword wears down the target's armour. With a paxel mod installed you can combine pickaxe, axe and shovel into a bedrock paxel.", "rotarycraft:bedrock_pickaxe"),
    ("bedrock_armor", "Bedrock armour never wears out and protects better than netherite. The helmet lets you see in the dark.", "rotarycraft:bedrock_helmet"),
    ("upgrades", "Engine upgrades are used on a machine: performance (gas to sport engine), afterburner (jet engine), redstone clock, magnet tiers and efficiency (magnetostatic engine), flux (dynamo) and lodestone (magnetizer).", "rotarycraft:upgrade_magnet_1"),
    ("lubricant", "Keeps gearboxes and engines from wearing out. Ground from canola seeds or distilled from oil.", "rotarycraft:lubricant_bucket"),
    ("spring", "A spring wound in the winder runs spring-powered tools and machines like the CCTV.", "rotarycraft:spring"),
)

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w") as f:
    json.dump({"categories": C}, f, indent=1, ensure_ascii=False)
print(sum(len(c["entries"]) for c in C), "entries")
