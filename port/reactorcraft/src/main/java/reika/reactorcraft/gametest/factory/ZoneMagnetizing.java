/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft magnetizer built and run by a simulated survival player. Not part of the
 * original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.MagnetizerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.GearboxTypes.GearPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneMagnetizing {

	public static final List<String> BLOCKS = List.of("rotarycraft:magnetizer");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Jet fuel barrel -> fuel lines -> microturbine -> bedrock gearbox 16:1 (8192 rad/s) -> magnetizer. The player gives it the redstone
	 * upgrade, puts a tungsten shaft core in through the GUI and takes it out again once it is magnetized.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1500, batch = "factory")
	public static void magnetizerChargesACore(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16);
		Item core = RotaryItems.gearPart(GearboxTypes.TUNGSTEN, GearPart.SHAFTCORE);
		Item up = RotaryItems.UPGRADE_REDSTONE.get();
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), gbx,
				new ItemStack(RotaryBlocks.MAGNETIZER.get()), new ItemStack(Items.DIRT), new ItemStack(core), new ItemStack(up));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.JET_FUEL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.MICRO_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, gbx, 1);
		Factory.take(p, chest, RotaryBlocks.MAGNETIZER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.takeExactly(p, chest, core, 1);
		Factory.takeExactly(p, chest, up, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.EAST);
		p.place(new BlockPos(5, 2, 5), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(4, 2, 5), Items.DIRT, Direction.EAST);
		p.placeSneaking(new BlockPos(5, 3, 5), gbx, Direction.EAST);
		BlockPos pos = new BlockPos(4, 3, 5);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.MAGNETIZER.get()), Direction.EAST);
		MagnetizerBlockEntity te = (MagnetizerBlockEntity)h.getBlockEntity(pos);
		p.use(pos, up);
		h.assertTrue(te.hasRedstoneUpgrade() && p.count(up) == 0, "redstone upgrade not installed");
		h.assertTrue(p.open(pos) != null, "magnetizer opens no GUI");
		p.putInto(0, core, 1);
		p.closeMenu();
		h.assertTrue(te.hasCore(), "core not in the magnetizer");

		h.succeedWhen(() -> {
			h.assertTrue(te.omega >= te.MINSPEED, "magnetizer at "+te.omega+" rad/s"); //the microturbine spins up for about 1900 ticks
			h.assertTrue(te.getCoreMagnetization() >= 1, "core at "+te.getCoreMagnetization()+" uT");
			p.open(pos);
			p.shiftClick(0);
			p.closeMenu();
			int m = 0;
			for (int i = 0; i < p.player.getInventory().getContainerSize(); i++)
				if (p.player.getInventory().getItem(i).is(core))
					m = p.player.getInventory().getItem(i).getOrDefault(RotaryComponents.MAGNETIZATION.get(), 0);
			h.assertTrue(m >= 1 && !te.hasCore(), "player's core at "+m+" uT");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
