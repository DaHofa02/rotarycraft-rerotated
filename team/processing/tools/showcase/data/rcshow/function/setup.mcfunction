gamerule doMobSpawning false
kill @e[type=!player]
gamerule doDaylightCycle false
time set 6000
weather clear
gamerule doWeatherCycle false
fill -2 -60 -2 12 -55 12 air
setblock 9 -60 0 rotarycraft:dc_engine{meta:0} replace
setblock 9 -60 1 minecraft:redstone_block replace
setblock 8 -60 0 rotarycraft:shaft{meta:0,shafttype:"STEEL"} replace
setblock 7 -60 0 rotarycraft:gearbox{meta:4,geartype:"WOOD",bearing:"WOOD"} replace
setblock 6 -60 0 rotarycraft:shaft{meta:0,shafttype:"WOOD"} replace
setblock 5 -60 0 rotarycraft:dynamometer{meta:0} replace
setblock 4 -60 0 rotarycraft:shaft{meta:0,shafttype:"STONE"} replace
setblock 3 -60 0 rotarycraft:flywheel{meta:1,typeIdx:2} replace
setblock 9 -60 3 rotarycraft:gas_engine{meta:0,fuel:{Fluid:{id:"rotarycraft:ethanol",amount:100000}}} replace
setblock 8 -60 3 rotarycraft:gearbox{meta:8,geartype:"STEEL",bearing:"STEEL",tank:{Fluid:{id:"rotarycraft:lubricant",amount:8000}}} replace
setblock 7 -60 3 rotarycraft:bevel_gears{posn:2} replace
setblock 7 -60 4 rotarycraft:dynamometer{meta:3} replace
setblock -1 -60 7 rotarycraft:dc_engine{meta:2} replace
setblock 1 -60 7 rotarycraft:wind_engine{meta:2} replace
setblock 3 -60 7 rotarycraft:steam_engine{meta:2} replace
setblock 5 -60 7 rotarycraft:gas_engine{meta:2} replace
setblock 7 -60 7 rotarycraft:ac_engine{meta:2} replace
setblock 9 -60 7 rotarycraft:sport_engine{meta:2} replace
setblock 11 -60 7 rotarycraft:micro_engine{meta:2} replace
setblock 0 -60 10 rotarycraft:shaft{meta:2,shafttype:"WOOD"} replace
setblock 1 -60 10 rotarycraft:shaft{meta:2,shafttype:"STONE"} replace
setblock 2 -60 10 rotarycraft:shaft{meta:2,shafttype:"STEEL"} replace
setblock 3 -60 10 rotarycraft:shaft{meta:2,shafttype:"TUNGSTEN"} replace
setblock 4 -60 10 rotarycraft:shaft{meta:2,shafttype:"DIAMOND"} replace
setblock 5 -60 10 rotarycraft:shaft{meta:2,shafttype:"BEDROCK"} replace
setblock 6 -60 10 rotarycraft:shaft{meta:6,shafttype:"STEEL"} replace
setblock 7 -60 10 rotarycraft:clutch{meta:2} replace
setblock 8 -60 10 rotarycraft:shaft_junction{meta:1} replace
setblock 9 -60 10 rotarycraft:shaft_junction{meta:9,bedrock:1b} replace
setblock 1 -60 12 rotarycraft:gearbox{meta:2,geartype:"STONE",bearing:"STONE"} replace
setblock 2 -60 12 rotarycraft:gearbox{meta:6,geartype:"STEEL",bearing:"STEEL"} replace
setblock 3 -60 12 rotarycraft:gearbox{meta:10,geartype:"DIAMOND",bearing:"DIAMOND"} replace
setblock 4 -60 12 rotarycraft:gearbox{meta:14,geartype:"BEDROCK",bearing:"BEDROCK"} replace
setblock 6 -60 12 rotarycraft:flywheel{meta:2,typeIdx:0} replace
setblock 7 -60 12 rotarycraft:flywheel{meta:2,typeIdx:1} replace
setblock 8 -60 12 rotarycraft:flywheel{meta:2,typeIdx:2} replace
setblock 9 -60 12 rotarycraft:flywheel{meta:2,typeIdx:3} replace
setblock 10 -60 12 rotarycraft:flywheel{meta:2,typeIdx:4} replace
setblock 11 -60 12 rotarycraft:flywheel{meta:2,typeIdx:5} replace
setblock 12 -60 12 rotarycraft:flywheel{meta:2,typeIdx:6} replace
fill -2 -60 15 12 -55 23 air
setblock 2 -60 16 rotarycraft:jet_engine{meta:2,burn:1b,burning:1b,temperature:1500,fuel:{Fluid:{id:"rotarycraft:jet_fuel",amount:200000}}} replace
setblock 5 -60 16 rotarycraft:hydro_engine{meta:2} replace
setblock 8 -60 16 rotarycraft:ecu[color=0] replace
setblock 8 -59 16 rotarycraft:gas_engine{meta:2,fuel:{Fluid:{id:"rotarycraft:ethanol",amount:100000}}} replace
setblock 10 -60 16 rotarycraft:ecu[color=5] replace
setblock 4 -60 19 rotarycraft:blast_furnace[facing=north]{meta:2,temp:700,inv:{Size:16,Items:[{Slot:0,id:"minecraft:coal",count:16},{Slot:11,id:"minecraft:gunpowder",count:16},{Slot:14,id:"minecraft:sand",count:16},{Slot:1,id:"minecraft:iron_ingot",count:8},{Slot:2,id:"minecraft:iron_ingot",count:8}]}} replace
setblock 8 -60 19 rotarycraft:worktable{inv:{Size:19,Items:[{Slot:0,id:"rotarycraft:steelingot",count:1},{Slot:1,id:"rotarycraft:pcb",count:1},{Slot:2,id:"rotarycraft:steelingot",count:1},{Slot:3,id:"rotarycraft:steelingot",count:1},{Slot:4,id:"minecraft:gold_ingot",count:1},{Slot:5,id:"rotarycraft:steelingot",count:1},{Slot:6,id:"rotarycraft:steelingot",count:1},{Slot:7,id:"minecraft:redstone",count:1},{Slot:8,id:"rotarycraft:steelingot",count:1}]}} replace
setblock 4 -60 20 rotarycraft:friction_heater{meta:2} replace
setblock 4 -60 21 rotarycraft:gas_engine{meta:2,fuel:{Fluid:{id:"rotarycraft:ethanol",amount:100000}}} replace
setblock 10 -60 19 minecraft:furnace[facing=south]{Items:[{Slot:0b,id:"minecraft:raw_iron",count:32}]} replace
setblock 10 -60 20 rotarycraft:friction_heater{meta:2} replace
setblock 10 -60 21 rotarycraft:gas_engine{meta:2,fuel:{Fluid:{id:"rotarycraft:ethanol",amount:100000}}} replace
setblock 8 -60 22 rotarycraft:bedrock_breaker{meta:1,step:2} replace
setblock 6 -60 22 rotarycraft:bedrock_slice[facing=east,level=8] replace
setblock 2 -60 22 rotarycraft:extractor{tank:{Fluid:{id:"minecraft:water",amount:8000}},inv:{Size:10,Items:[{Slot:0,id:"minecraft:iron_ore",count:12},{Slot:1,id:"rotarycraft:goldoredust",count:3},{Slot:7,id:"rotarycraft:ironoreflakes",count:5}]}} replace
fill -2 -60 25 12 -55 34 air
fill 0 -60 27 4 -60 27 rotarycraft:pipe{fluid:"minecraft:water",level:4000} replace
fill 0 -60 29 4 -60 29 rotarycraft:pipe{fluid:"minecraft:lava",level:4000} replace
fill 0 -60 31 4 -60 31 rotarycraft:hose{fluid:"rotarycraft:lubricant",level:4000} replace
fill 0 -60 33 4 -60 33 rotarycraft:fuel_line{fluid:"rotarycraft:jet_fuel",level:4000} replace
setblock 6 -60 27 rotarycraft:pipe replace
setblock 6 -59 27 rotarycraft:pipe replace
setblock 7 -59 27 rotarycraft:pipe replace
setblock 6 -60 28 rotarycraft:pipe replace
setblock 6 -60 29 rotarycraft:bedrock_pipe replace
setblock 7 -60 29 rotarycraft:bedrock_pipe replace
setblock 10 -60 30 minecraft:netherrack replace
setblock 10 -59 30 minecraft:fire replace
setblock 10 -58 30 rotarycraft:steam_engine{meta:2,water:{Fluid:{id:"minecraft:water",amount:4000}}} replace
setblock 10 -58 31 rotarycraft:pipe{fluid:"minecraft:water",level:8000} replace
setblock 10 -58 32 rotarycraft:pipe replace
fill -2 -60 35 12 -55 38 air
setblock 8 -60 33 rotarycraft:grinder{meta:0,tank:{Fluid:{id:"rotarycraft:lubricant",amount:2500}},inv:{Size:3,Items:[{Slot:0,id:"rotarycraft:canola_seeds",count:32},{Slot:1,id:"rotarycraft:canola_husks",count:5}]}} replace
setblock 9 -60 33 rotarycraft:gas_engine{meta:0,fuel:{Fluid:{id:"rotarycraft:ethanol",amount:100000}}} replace
fill 0 -61 36 6 -61 36 minecraft:farmland[moisture=7] replace
setblock 0 -60 36 rotarycraft:canola[age=0] replace
setblock 1 -60 36 rotarycraft:canola[age=2] replace
setblock 2 -60 36 rotarycraft:canola[age=4] replace
setblock 3 -60 36 rotarycraft:canola[age=6] replace
setblock 4 -60 36 rotarycraft:canola[age=8] replace
setblock 5 -60 36 rotarycraft:canola[age=9] replace
setblock 6 -60 36 rotarycraft:canola[age=9] replace
setblock 7 -61 36 minecraft:water replace
setblock 6 -60 38 rotarycraft:fermenter[facing=north]{meta:2,temperature:25,tank:{Fluid:{id:"minecraft:water",amount:3000}},inv:{Size:3,Items:[{Slot:0,id:"minecraft:sugar",count:16},{Slot:1,id:"minecraft:dirt",count:16},{Slot:2,id:"rotarycraft:yeast",count:3}]}} replace
setblock 6 -60 39 rotarycraft:gas_engine{meta:2,fuel:{Fluid:{id:"rotarycraft:ethanol",amount:100000}}} replace
setblock 8 -60 38 rotarycraft:centrifuge{tank:{Fluid:{id:"rotarycraft:lubricant",amount:4000}},inv:{Size:10,Items:[{Slot:0,id:"rotarycraft:sludge",count:20},{Slot:1,id:"rotarycraft:cleansludge",count:7},{Slot:2,id:"rotarycraft:compost",count:2}]}} replace
setblock 10 -60 38 rotarycraft:fractionator{press:420,in:{Fluid:{id:"rotarycraft:ethanol",amount:9000}},out:{Fluid:{id:"rotarycraft:jet_fuel",amount:60000}},inv:{Size:8,Items:[{Slot:0,id:"minecraft:blaze_powder",count:8},{Slot:1,id:"rotarycraft:coaldust",count:8},{Slot:2,id:"minecraft:magma_cream",count:8},{Slot:3,id:"minecraft:pink_dye",count:8},{Slot:4,id:"rotarycraft:netherrackdust",count:8},{Slot:5,id:"rotarycraft:tar",count:8},{Slot:6,id:"minecraft:ghast_tear",count:1}]}} replace
fill -2 -60 43 12 -55 49 air
fill 0 -61 44 4 -61 48 minecraft:stone replace
fill 1 -61 45 3 -61 47 minecraft:water replace
setblock 2 -60 46 rotarycraft:pump{meta:1} replace
setblock 3 -60 46 rotarycraft:gas_engine{meta:0,fuel:{Fluid:{id:"rotarycraft:ethanol",amount:100000}}} replace
setblock 6 -60 45 rotarycraft:reservoir{tank:{Fluid:{id:"minecraft:water",amount:40000}}} replace
setblock 7 -60 45 rotarycraft:reservoir{tank:{Fluid:{id:"minecraft:water",amount:20000}}} replace
setblock 6 -60 46 rotarycraft:reservoir{tank:{Fluid:{id:"minecraft:water",amount:30000}}} replace
setblock 7 -60 46 rotarycraft:reservoir{tank:{Fluid:{id:"minecraft:water",amount:10000}}} replace
setblock 9 -60 45 rotarycraft:reservoir{cover:1b,tank:{Fluid:{id:"rotarycraft:lubricant",amount:50000}}} replace
setblock 9 -60 47 rotarycraft:reservoir{tank:{Fluid:{id:"minecraft:lava",amount:30000}}} replace
setblock 11 -60 45 rotarycraft:pulse_jet_furnace{temp:870,fuel:{Fluid:{id:"rotarycraft:jet_fuel",amount:6000}},water:{Fluid:{id:"minecraft:water",amount:2000}},inv:{Size:3,Items:[{Slot:0,id:"minecraft:obsidian",count:8},{Slot:2,id:"rotarycraft:blast_glass",count:3}]}} replace
fill 11 -60 47 12 -59 47 rotarycraft:blast_glass replace
# z=59: cooling fins - on top of a gearbox (target below), on its side, and the three settings (full, half, quarter) on stone
fill 0 -60 58 10 -56 60 air
setblock 3 -60 59 rotarycraft:gearbox replace
setblock 3 -59 59 rotarycraft:cooling_fin{meta:0} replace
setblock 4 -60 59 rotarycraft:cooling_fin{meta:3} replace
setblock 7 -60 59 minecraft:stone replace
setblock 7 -59 59 rotarycraft:cooling_fin{meta:0,setting:0} replace
setblock 8 -60 59 minecraft:stone replace
setblock 8 -59 59 rotarycraft:cooling_fin{meta:0,setting:1} replace
setblock 9 -60 59 minecraft:stone replace
setblock 9 -59 59 rotarycraft:cooling_fin{meta:0,setting:2} replace
