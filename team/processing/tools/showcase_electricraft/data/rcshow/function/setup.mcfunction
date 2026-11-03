gamerule doMobSpawning false
kill @e[type=!player]
gamerule doDaylightCycle false
time set 6000
weather clear
setworldspawn 5 -60 -4
fill -2 -60 -2 12 -55 10 air
# z=0: DC engine -> wooden 4:1 gearbox -> generator (input +x, face 5 = east) -> 3 steel wires -> motor -> dynamometer (48..40 rad/s @ 16 Nm)
setblock 9 -60 0 rotarycraft:dc_engine{meta:0} replace
setblock 9 -60 1 minecraft:redstone_block replace
setblock 8 -60 0 rotarycraft:gearbox{meta:4,geartype:"WOOD",bearing:"WOOD"} replace
setblock 7 -60 0 electricraft:generator{face:5} replace
setblock 6 -60 0 electricraft:steel_wire replace
setblock 5 -60 0 electricraft:steel_wire replace
setblock 4 -60 0 electricraft:steel_wire replace
setblock 3 -60 0 electricraft:motor{face:5} replace
setblock 2 -60 0 rotarycraft:dynamometer{meta:0} replace
# z=3..: every wire type, plain and insulated, with a bend and a branch
setblock 0 -60 3 electricraft:steel_wire replace
setblock 1 -60 3 electricraft:tin_wire replace
setblock 2 -60 3 electricraft:nickel_wire replace
setblock 3 -60 3 electricraft:aluminum_wire replace
setblock 4 -60 3 electricraft:copper_wire replace
setblock 5 -60 3 electricraft:silver_wire replace
setblock 6 -60 3 electricraft:gold_wire replace
setblock 7 -60 3 electricraft:platinum_wire replace
setblock 0 -60 5 electricraft:insulated_steel_wire replace
setblock 1 -60 5 electricraft:insulated_tin_wire replace
setblock 2 -60 5 electricraft:insulated_nickel_wire replace
setblock 3 -60 5 electricraft:insulated_aluminum_wire replace
setblock 4 -60 5 electricraft:insulated_copper_wire replace
setblock 5 -60 5 electricraft:insulated_silver_wire replace
setblock 6 -60 5 electricraft:insulated_gold_wire replace
setblock 7 -60 5 electricraft:insulated_platinum_wire replace
setblock 7 -59 5 electricraft:insulated_platinum_wire replace
setblock 7 -60 6 electricraft:insulated_platinum_wire replace
# z=8: the five ores, each with its ingot in an item frame on top
setblock 1 -60 8 electricraft:tin_ore replace
setblock 3 -60 8 electricraft:silver_ore replace
setblock 5 -60 8 electricraft:nickel_ore replace
setblock 7 -60 8 electricraft:aluminum_ore replace
setblock 9 -60 8 electricraft:platinum_ore replace
summon item_frame 1 -59 8 {Facing:1b,Item:{id:"electricraft:tin_ingot",count:1},Fixed:1b}
summon item_frame 3 -59 8 {Facing:1b,Item:{id:"electricraft:silver_ingot",count:1},Fixed:1b}
summon item_frame 5 -59 8 {Facing:1b,Item:{id:"electricraft:nickel_ingot",count:1},Fixed:1b}
summon item_frame 7 -59 8 {Facing:1b,Item:{id:"electricraft:aluminum_ingot",count:1},Fixed:1b}
summon item_frame 9 -59 8 {Facing:1b,Item:{id:"electricraft:platinum_ingot",count:1},Fixed:1b}
# z=11: DC engine -> gearbox -> generator -> relay (redstone) -> resistor 10 A -> fuse 1024 A -> meter -> precision resistor 100 A -> wire -> motor -> dynamometer
fill 2 -60 11 14 -58 13 air
setblock 13 -60 11 rotarycraft:dc_engine{meta:0} replace
setblock 13 -60 12 minecraft:redstone_block replace
setblock 12 -60 11 rotarycraft:gearbox{meta:4,geartype:"WOOD",bearing:"WOOD"} replace
setblock 11 -60 11 electricraft:generator{face:5} replace
setblock 10 -60 11 electricraft:relay{face:5} replace
setblock 10 -60 12 minecraft:redstone_block replace
setblock 9 -60 11 electricraft:resistor{face:5,band1:1,band2:0,band3:0,sel:10} replace
setblock 8 -60 11 electricraft:fuse{face:5,limit:1024} replace
setblock 7 -60 11 electricraft:meter replace
setblock 6 -60 11 electricraft:precise_resistor{face:5,band1:1,band2:0,band3:0,band4:0,sel:100} replace
setblock 5 -60 11 electricraft:copper_wire replace
setblock 4 -60 11 electricraft:motor{face:5} replace
setblock 3 -60 11 rotarycraft:dynamometer{meta:0} replace
# z=16: the six batteries; the first three charged (glow full-bright), the rest empty
fill 0 -60 15 12 -58 18 air
setblock 1 -60 16 electricraft:redstone_battery{e:20000L} replace
setblock 3 -60 16 electricraft:glowstone_battery{e:20000L} replace
setblock 5 -60 16 electricraft:lapis_battery{e:20000L} replace
setblock 7 -60 16 electricraft:ender_battery replace
setblock 9 -60 16 electricraft:diamond_battery replace
setblock 11 -60 16 electricraft:star_battery replace
# z=20: transformers 1:1, 3:20 and 200:1 (coil stacks by turn count)
fill 0 -60 19 12 -58 22 air
setblock 3 -60 20 electricraft:transformer{face:4,n1:1,n2:1} replace
setblock 6 -60 20 electricraft:transformer{face:4,n1:3,n2:20} replace
setblock 9 -60 20 electricraft:transformer{face:4,n1:200,n2:1} replace
# z=24: RF battery (charged, charge bar) -> RF cables (limit 1000) -> empty RF battery
fill 0 -60 23 12 -57 26 air
setblock 3 -60 24 electricraft:rf_battery{e:4000000000000L} replace
setblock 3 -59 24 electricraft:rf_cable{limit:1000} replace
setblock 4 -59 24 electricraft:rf_cable{limit:1000} replace
setblock 5 -59 24 electricraft:rf_cable{limit:1000} replace
setblock 6 -59 24 electricraft:rf_battery{e:1000L} replace
setblock 9 -60 24 electricraft:rf_battery replace
# z=24: the five wireless pads (front towards the camera, the last one facing up)
setblock 0 -60 26 electricraft:basic_wireless_pad[facing=south] replace
setblock 2 -60 26 electricraft:improved_wireless_pad[facing=south] replace
setblock 10 -60 26 electricraft:advanced_wireless_pad[facing=south] replace
setblock 11 -60 26 electricraft:hightech_wireless_pad[facing=north] replace
setblock 12 -60 26 electricraft:superconducting_wireless_pad[facing=up] replace
