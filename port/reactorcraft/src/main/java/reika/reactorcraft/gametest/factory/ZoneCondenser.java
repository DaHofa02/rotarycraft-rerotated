/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft aggregator built and run by a simulated survival player. Not part of the
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
import reika.rotarycraft.content.blockentity.AggregatorBlockEntity;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.lib.world.BiomeHumidity;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneCondenser {

	public static final List<String> BLOCKS = List.of("rotarycraft:aggregator");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Jet fuel barrel -> fuel line -> microturbine (131072 rad/s @ 16 Nm) -> bevel gears (east -> up) -> aggregator: above 4096 rad/s it
	 * condenses max(2, (int)(16^2 * humidity)) mB of water per tick (the operation time is down to one tick) and a pipe beside it carries
	 * the water off.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 6000, batch = "factory")
	public static void aggregatorMakesWater(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.AGGREGATOR.get()), new ItemStack(RotaryBlocks.PIPE.get()),
				new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 4), RotaryFluids.JET_FUEL.get());

		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.MICRO_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.AGGREGATOR.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 4);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 4), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 4), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.EAST);
		// the aggregator on a dirt pillar, whose top block is dug away again and replaced by the bevel gears (placed last: east -> up)
		BlockPos pos = new BlockPos(5, 4, 4);
		p.place(new BlockPos(5, 2, 4), Items.DIRT, Direction.EAST);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.AGGREGATOR.get(), Direction.EAST);
		p.mine(pos.below(), Items.IRON_SHOVEL);
		p.placeSneaking(pos.below(), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		// a pipe west of it, on its own dirt pillar
		BlockPos pipe = new BlockPos(4, 4, 4);
		p.place(new BlockPos(4, 2, 4), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(4, 3, 4), Items.DIRT, Direction.EAST);
		p.placeSneaking(pipe, new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);

		AggregatorBlockEntity te = (AggregatorBlockEntity)h.getBlockEntity(pos);
		PipingBlockEntity out = (PipingBlockEntity)h.getBlockEntity(pipe);
		int made = Math.max(2, (int)(256*BiomeHumidity.get(h.getLevel().getBiome(te.getBlockPos()))));
		h.succeedWhen(() -> {
			h.assertTrue(te.omega == 131072 && te.torque == 16, "aggregator at "+te.omega+" rad/s @ "+te.torque+" Nm");
			h.assertTrue(te.getProductionPerTick(h.getLevel().getBiome(te.getBlockPos())) == made, "makes "+te.getProductionPerTick(h.getLevel().getBiome(te.getBlockPos()))+" mB/t, expected "+made);
			h.assertTrue(te.getWater() > 1000, "only "+te.getWater()+" mB in the aggregator");
			h.assertTrue(out.getFluidLevel() > 0 && out.getFluidType() == Fluids.WATER, "the pipe took nothing: "+out.getFluidLevel());
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 5, 8));
			p.leave();
		});
	}
}
