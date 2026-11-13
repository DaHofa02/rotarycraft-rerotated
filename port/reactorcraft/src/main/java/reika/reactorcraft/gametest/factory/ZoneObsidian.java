/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft obsidian maker built and run by a simulated survival player. Not part of the
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
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.ObsidianMakerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneObsidian {

	public static final List<String> BLOCKS = List.of("rotarycraft:obsidian_maker");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Jet fuel barrel -> fuel lines -> microturbine -> bedrock gearbox 16:1 -> bevel gears -> obsidian maker above them, with a water
	 * barrel over a pipe on one side and a lava barrel over a pipe on the other. Still cold (it takes minutes to reach 550 deg C), it makes
	 * cobblestone, which the player takes out.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1500, batch = "factory")
	public static void makerMakesCobblestone(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), gbx,
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.OBSIDIAN_MAKER.get()), new ItemStack(RotaryBlocks.PIPE.get()),
				new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 4), RotaryFluids.JET_FUEL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.MICRO_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, gbx, 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.OBSIDIAN_MAKER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 2);
		Factory.takeExactly(p, chest, Items.DIRT, 7);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		for (int x = 8; x >= 7; x--)
			p.place(new BlockPos(x, 2, 4), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(7, 3, 4), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.EAST);
		p.place(new BlockPos(6, 2, 4), Items.DIRT, Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 4), gbx, Direction.EAST);
		BlockPos pos = new BlockPos(5, 4, 4);
		p.place(new BlockPos(5, 2, 4), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(5, 3, 4), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.OBSIDIAN_MAKER.get(), Direction.EAST);
		for (int z : new int[] {3, 5}) {
			p.place(new BlockPos(5, 2, z), Items.DIRT, Direction.EAST);
			p.place(new BlockPos(5, 3, z), Items.DIRT, Direction.EAST);
			p.place(new BlockPos(5, 4, z), RotaryBlocks.PIPE.get(), Direction.EAST);
		}
		Factory.fluidBarrel(h, new BlockPos(5, 5, 3), Fluids.WATER);
		Factory.fluidBarrel(h, new BlockPos(5, 5, 5), Fluids.LAVA);
		p.mine(new BlockPos(5, 3, 4), Items.IRON_SHOVEL);
		p.placeSneaking(new BlockPos(5, 3, 4), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		ObsidianMakerBlockEntity te = (ObsidianMakerBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.omega >= te.MINSPEED && te.getWater() > 0 && te.getLava() > 0, te.omega+" rad/s, "+te.getWater()+" mB water, "+te.getLava()+" mB lava");
			int cob = 0;
			for (int i = 0; i < 9; i++)
				cob += te.getInventory().getStackInSlot(i).is(Items.COBBLESTONE) ? te.getInventory().getStackInSlot(i).getCount() : 0;
			h.assertTrue(cob >= 2, cob+" cobblestone at "+te.getTemperature()+" C");
			p.open(pos);
			for (int i = 0; i < 9; i++)
				p.shiftClick(i);
			p.closeMenu();
			h.assertTrue(p.count(Items.COBBLESTONE) >= 2, "player has "+p.count(Items.COBBLESTONE)+" cobblestone");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 5, 8));
			p.leave();
		});
	}
}
