/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft power train, built and run by a simulated survival player. Not part of the
 * original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.FlywheelBlock;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.block.ShaftBlock;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.content.blockentity.FlywheelBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.Flywheels;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZonePowerTrain {

	public static final List<String> BLOCKS = List.of("rotarycraft:dc_engine", "rotarycraft:shaft", "rotarycraft:shaft_junction", "rotarycraft:gearbox",
			"rotarycraft:clutch", "rotarycraft:bevel_gears", "rotarycraft:flywheel", "rotarycraft:dynamometer");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Two DC engines (256 rad/s, 4 Nm each, on redstone blocks) meet in a shaft junction (merge: 256 rad/s @ 8 Nm), a wooden 4:1 gearbox
	 * turns that into 64 rad/s @ 32 Nm, a clutch on a lever passes it, bevel gears turn it 90 degrees north, and through a shaft and a
	 * wooden flywheel it reaches a dynamometer. Everything comes out of one infinite chest and is placed by the player looking along the
	 * line (the machines take their orientation from the look, as in the game). With the lever off nothing arrives; the player flips the
	 * lever, the flywheel spins up to the full 64 rad/s, and the dynamometer reads 64 rad/s @ 16 Nm: a wooden flywheel passes at most its
	 * rated 16 Nm (original Flywheels.WOOD maxTorque), so half of the 32 Nm stays behind it.
	 *
	 * Plan at y = 2 (x = 2..7 left to right, north up):
	 *
	 *   z=1  D  .  .  .  .  .      D dynamometer, F flywheel, S steel shafts, B bevel gears, C clutch, L lever,
	 *   z=2  F  .  .  E  R  .      G wooden 4:1 gearbox, J shaft junction, E DC engines, R redstone blocks
	 *   z=3  S  .  .  S  .  .
	 *   z=4  B  C  G  J  S  E
	 *   z=5  .  L  .  .  .  R
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1600, batch = "factory")
	public static void powerTrain(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(1, 2, 7);
		ItemStack shaft = ShaftBlock.shaftItem(MaterialRegistry.STEEL, false);
		ItemStack gearbox = GearboxBlock.gearboxItem(GearboxTypes.WOOD, 4);
		ItemStack flywheel = FlywheelBlock.flywheelItem(Flywheels.WOOD);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.DC_ENGINE.get()), shaft, new ItemStack(RotaryBlocks.SHAFT_JUNCTION.get()), gearbox,
				new ItemStack(RotaryBlocks.CLUTCH.get()), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), flywheel,
				new ItemStack(RotaryBlocks.DYNAMOMETER.get()), new ItemStack(Items.REDSTONE_BLOCK), new ItemStack(Items.LEVER));
		Factory.take(p, chest, RotaryBlocks.DC_ENGINE.get().asItem(), 2);
		Factory.take(p, chest, shaft, 3);
		Factory.take(p, chest, RotaryBlocks.SHAFT_JUNCTION.get().asItem(), 1);
		Factory.take(p, chest, gearbox, 1);
		Factory.take(p, chest, RotaryBlocks.CLUTCH.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, flywheel, 1);
		Factory.take(p, chest, RotaryBlocks.DYNAMOMETER.get().asItem(), 1);
		Factory.take(p, chest, Items.REDSTONE_BLOCK, 2);
		Factory.take(p, chest, Items.LEVER, 1);

		// main line along z = 4, power flowing west: the player looks east
		p.place(new BlockPos(7, 2, 4), RotaryBlocks.DC_ENGINE.get(), Direction.EAST);
		p.place(new BlockPos(7, 2, 5), Items.REDSTONE_BLOCK, Direction.EAST);
		p.place(new BlockPos(6, 2, 4), shaft, Direction.EAST);
		p.place(new BlockPos(5, 2, 4), RotaryBlocks.SHAFT_JUNCTION.get(), Direction.EAST); // merge: inputs east and north, output west
		p.place(new BlockPos(4, 2, 4), gearbox, Direction.EAST);
		p.place(new BlockPos(3, 2, 4), RotaryBlocks.CLUTCH.get(), Direction.EAST);
		p.place(new BlockPos(3, 2, 5), Items.LEVER, Direction.EAST); // on the floor beside the clutch, off
		// second engine north of the junction, writing south: the player looks north
		p.place(new BlockPos(5, 2, 2), RotaryBlocks.DC_ENGINE.get(), Direction.NORTH);
		p.place(new BlockPos(6, 2, 2), Items.REDSTONE_BLOCK, Direction.NORTH);
		p.place(new BlockPos(5, 2, 3), shaft, Direction.NORTH);
		// after the bend the power flows north: the player looks south; the bevel gears last, so they find both neighbours
		p.place(new BlockPos(2, 2, 3), shaft, Direction.SOUTH);
		p.place(new BlockPos(2, 2, 2), flywheel, Direction.SOUTH);
		p.place(new BlockPos(2, 2, 1), RotaryBlocks.DYNAMOMETER.get(), Direction.SOUTH);
		p.place(new BlockPos(2, 2, 4), RotaryBlocks.BEVEL_GEARS.get(), Direction.SOUTH);

		DynamometerBlockEntity dyna = (DynamometerBlockEntity)h.getBlockEntity(new BlockPos(2, 2, 1));
		FlywheelBlockEntity fly = (FlywheelBlockEntity)h.getBlockEntity(new BlockPos(2, 2, 2));
		boolean[] leverOn = {false};
		h.runAtTickTime(40, () -> {
			h.assertTrue(dyna.power == 0 && fly.omega == 0, "power arrived with the clutch open: "+dyna.omega+" rad/s @ "+dyna.torque+" Nm");
			p.emptyHand();
			p.use(new BlockPos(3, 2, 5));
			leverOn[0] = true;
		});
		h.succeedWhen(() -> {
			h.assertTrue(leverOn[0], "lever not flipped yet");
			h.assertTrue(dyna.torque == 16 && dyna.omega == 64 && dyna.power == 64L*16,
					"dynamometer "+dyna.omega+" rad/s @ "+dyna.torque+" Nm, flywheel "+fly.omega+" rad/s @ "+fly.torque+" Nm");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 2, 8));
			p.leave();
		});
	}
}
