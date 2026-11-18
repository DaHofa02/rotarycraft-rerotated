/*******************************************************************************
 * Practice factory zone (see Factory): the port's magnetostatic engine and dynamo set up by a simulated survival player. Not part of the
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
import reika.rotarycraft.content.blockentity.DynamoBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.content.blockentity.MagneticEngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFEConverters {

	public static final List<String> BLOCKS = List.of("rotarycraft:magnetic_engine", "rotarycraft:dynamo");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> dynamo -> magnetostatic engine, all driving west. The player upgrades the engine to tier 1
	 * and clicks its speed up to 256 rad/s in the GUI: it runs on the dynamo's FE at 32 Nm.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void dynamoDrivesAMagneticEngine(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.DYNAMO.get()), new ItemStack(RotaryBlocks.MAGNETIC_ENGINE.get()), new ItemStack(Items.DIRT),
				new ItemStack(RotaryItems.UPGRADE_MAGNET_1.get()));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.DYNAMO.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.MAGNETIC_ENGINE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.take(p, chest, RotaryItems.UPGRADE_MAGNET_1.get(), 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST); //looking east: drives west
		BlockPos dyn = new BlockPos(5, 3, 5), mag = new BlockPos(4, 3, 5);
		p.place(dyn.below(), Items.DIRT, Direction.EAST);
		p.place(mag.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(dyn, new ItemStack(RotaryBlocks.DYNAMO.get()), Direction.EAST); //input from the engine to the east, FE out west
		p.placeSneaking(mag, new ItemStack(RotaryBlocks.MAGNETIC_ENGINE.get()), Direction.EAST); //FE in at the east, drives west
		p.hold(RotaryItems.UPGRADE_MAGNET_1.get());
		p.useOn(mag, Direction.UP);
		h.assertTrue(p.open(mag) != null, "magnetostatic engine opens no GUI");
		for (int i = 0; i < 9; i++)
			p.player.containerMenu.clickMenuButton(p.player, 1); //"+" from off to 2^8 rad/s
		p.closeMenu();
		DynamoBlockEntity d = (DynamoBlockEntity)h.getBlockEntity(dyn);
		MagneticEngineBlockEntity te = (MagneticEngineBlockEntity)h.getBlockEntity(mag);
		h.succeedWhen(() -> {
			h.assertTrue(te.getTier() == 1 && te.getTargetSpeed() == 256, "engine tier "+te.getTier()+", set to "+te.getTargetSpeed());
			h.assertTrue(te.getOmega() == 256 && te.getTorque() == 32, "engine "+te.getOmega()+" rad/s "+te.getTorque()+" Nm; dynamo "+d.getPower()+" W, "+d.delivered+" FE");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
