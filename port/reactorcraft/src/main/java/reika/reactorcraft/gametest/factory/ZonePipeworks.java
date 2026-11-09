/*******************************************************************************
 * Practice factory zone (see Factory): the special RotaryCraft pipes - separator, pipe pump, bypass, suction pipe - built and run by a
 * simulated survival player. Not part of the original mods.
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
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZonePipeworks {

	public static final List<String> BLOCKS = List.of("rotarycraft:separation", "rotarycraft:pipe_pump", "rotarycraft:bypass", "rotarycraft:suction");

	private static final String PLATFORM = "platform_9x8x9";

	private static boolean connected(GameTestHelper h, BlockPos p, Direction d) {
		return h.getBlockState(p).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d));
	}

	/** Sets the item down against the neighbour on side {@code d} (clicking that neighbour's facing side, sneaking), as a player does. */
	private static void placeAgainst(SimPlayer p, BlockPos rel, Direction d, ItemStack is) {
		p.hold(is);
		p.player.setShiftKeyDown(true);
		try {
			p.useOn(rel.relative(d), d.getOpposite());
		}
		finally {
			p.player.setShiftKeyDown(false);
		}
	}

	/**
	 * Water barrel -> pipes -> separator (takes from the side, gives only downwards) -> pipe below it. That pipe is the input of a pipe
	 * pump (input east), driven from the south by a microturbine on a jet fuel line the player lays into the floor; the pump moves the
	 * water into the pipe west of it. Beyond: two bypasses, the second set against the first so they join, and a suction pipe, which joins
	 * a liquid pipe but takes nothing from it (it only pulls from other mods' fluid handlers).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600, batch = "factory")
	public static void separatorPipePumpBypassSuction(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(RotaryBlocks.SEPARATION.get()),
				new ItemStack(RotaryBlocks.PIPE_PUMP.get()), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), new ItemStack(RotaryBlocks.FUEL_LINE.get()),
				new ItemStack(RotaryBlocks.BYPASS.get()), new ItemStack(RotaryBlocks.SUCTION.get()), new ItemStack(Items.IRON_PICKAXE));
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 5);
		Factory.take(p, chest, RotaryBlocks.SEPARATION.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE_PUMP.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.MICRO_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.BYPASS.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.SUCTION.get().asItem(), 1);
		Factory.take(p, chest, Items.IRON_PICKAXE, 1);

		// water: barrel at (7,4,4) gives down into (7,3,4); pipes west to the separator at (5,3,4); its output pipe below at (5,2,4)
		Factory.fluidBarrel(h, new BlockPos(7, 4, 4), Fluids.WATER);
		p.placeSneaking(new BlockPos(7, 3, 4), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 4), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.place(new BlockPos(5, 2, 4), RotaryBlocks.PIPE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(5, 3, 4), new ItemStack(RotaryBlocks.SEPARATION.get()), Direction.EAST);
		// jet fuel line in the floor under the microturbine, fed by a barrel above its far end
		BlockPos f1 = new BlockPos(4, 1, 6), f2 = new BlockPos(4, 1, 5);
		p.mine(f1, Items.IRON_PICKAXE);
		p.mine(f2, Items.IRON_PICKAXE);
		p.place(f1, RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.place(f2, RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		// pipe pump at (4,2,4), input east; microturbine south of it writing north (the player looks south)
		p.placeSneaking(new BlockPos(4, 2, 4), new ItemStack(RotaryBlocks.PIPE_PUMP.get()), Direction.EAST);
		p.place(new BlockPos(3, 2, 4), RotaryBlocks.PIPE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(4, 2, 5), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.SOUTH);
		Factory.fluidBarrel(h, new BlockPos(4, 2, 6), RotaryFluids.JET_FUEL.get());
		// two bypasses, the second set against the first; a suction pipe after a liquid pipe
		BlockPos b1 = new BlockPos(3, 2, 2), b2 = new BlockPos(2, 2, 2);
		p.place(b1, RotaryBlocks.BYPASS.get(), Direction.EAST);
		placeAgainst(p, b2, Direction.EAST, new ItemStack(RotaryBlocks.BYPASS.get()));
		BlockPos sp = new BlockPos(2, 2, 4), sucp = new BlockPos(1, 2, 4);
		p.placeSneaking(sp, new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.placeSneaking(sucp, new ItemStack(RotaryBlocks.SUCTION.get()), Direction.EAST);

		PipingBlockEntity down = (PipingBlockEntity)h.getBlockEntity(new BlockPos(5, 2, 4));
		PipingBlockEntity west = (PipingBlockEntity)h.getBlockEntity(new BlockPos(3, 2, 4));
		PipingBlockEntity suction = (PipingBlockEntity)h.getBlockEntity(sucp);
		h.succeedWhen(() -> {
			h.assertTrue(down.getFluidLevel() > 0 || west.getFluidLevel() > 0, "nothing came out of the separator");
			h.assertTrue(connected(h, new BlockPos(3, 2, 4), Direction.EAST) && connected(h, new BlockPos(5, 2, 4), Direction.WEST), "pipes not joined to the pump");
			h.assertTrue(west.getFluidLevel() > 0 && west.getFluidType() == Fluids.WATER, "the pump moved nothing west: "+west.getFluidLevel());
			h.assertTrue(connected(h, b2, Direction.EAST) && connected(h, b1, Direction.WEST), "bypass set against the other did not join it");
			h.assertTrue(connected(h, sucp, Direction.EAST) && suction.getFluidLevel() == 0, "suction pipe: joined "+connected(h, sucp, Direction.EAST)+", took "+suction.getFluidLevel());
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 3, 8));
			p.leave();
		});
	}
}
