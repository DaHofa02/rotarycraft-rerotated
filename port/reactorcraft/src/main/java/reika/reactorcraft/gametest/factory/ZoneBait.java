/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft bait box built and run by a simulated survival player. Not part of the original
 * mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.BaitBoxBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneBait {

	public static final List<String> BLOCKS = List.of("rotarycraft:bait_box");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> bait box west of it. The player puts wheat in through the GUI and lets a cow out of a
	 * spawn egg at the far side of the zone: the cow comes over to the box.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600, batch = "factory")
	public static void baitBoxLuresACow(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BAIT_BOX.get()), new ItemStack(Items.DIRT), new ItemStack(Items.WHEAT), new ItemStack(Items.COW_SPAWN_EGG));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BAIT_BOX.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.WHEAT, 4);
		Factory.takeExactly(p, chest, Items.COW_SPAWN_EGG, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.BAIT_BOX.get(), Direction.EAST);
		BaitBoxBlockEntity te = (BaitBoxBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(p.open(pos) != null, "bait box opens no GUI");
		p.putInto(0, Items.WHEAT, 4);
		p.closeMenu();
		p.hold(Items.COW_SPAWN_EGG);
		p.useOn(new BlockPos(1, 1, 1), Direction.UP);
		p.leave();

		h.succeedWhen(() -> {
			List<Cow> cows = h.getLevel().getEntitiesOfClass(Cow.class, new AABB(h.absolutePos(new BlockPos(4, 2, 4))).inflate(8));
			h.assertTrue(!cows.isEmpty(), "no cow");
			BlockPos b = h.absolutePos(pos);
			double d = Math.sqrt(cows.get(0).distanceToSqr(b.getX()+0.5, b.getY()-1, b.getZ()+0.5));
			h.assertTrue(d < 2.5, "cow "+d+" from the box, "+te.power+" W");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
		});
	}
}
