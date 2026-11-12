/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft line builder built and run by a simulated survival player. Not part of the
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
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.LineBuilderBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneRam {

	public static final List<String> BLOCKS = List.of("rotarycraft:line_builder");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Jet fuel barrel -> fuel lines -> microturbine -> two bedrock gearboxes 16:1 (512 rad/s @ 4096 Nm) -> line builder (needs 1024 Nm and
	 * 131072 W) with a stone block in front of it. The player puts two cobblestone in through the GUI: the line grows westwards.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1200, batch = "factory")
	public static void lineBuilderBuildsALine(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), gbx,
				new ItemStack(RotaryBlocks.LINE_BUILDER.get()), new ItemStack(Items.DIRT), new ItemStack(Items.STONE), new ItemStack(Items.COBBLESTONE));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.JET_FUEL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.MICRO_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, gbx, 2);
		Factory.take(p, chest, RotaryBlocks.LINE_BUILDER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 4);
		Factory.takeExactly(p, chest, Items.STONE, 1);
		Factory.takeExactly(p, chest, Items.COBBLESTONE, 2);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.EAST);
		for (int x = 5; x >= 2; x--)
			p.place(new BlockPos(x, 2, 5), Items.DIRT, Direction.EAST);
		p.placeSneaking(new BlockPos(5, 3, 5), gbx, Direction.EAST);
		p.placeSneaking(new BlockPos(4, 3, 5), gbx, Direction.EAST);
		BlockPos pos = new BlockPos(3, 3, 5);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.LINE_BUILDER.get()), Direction.EAST);
		p.place(new BlockPos(2, 3, 5), Items.STONE, Direction.EAST);
		LineBuilderBlockEntity te = (LineBuilderBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getPushDirection() == Direction.WEST, "line builder meta "+te.getBlockMetadata()+" pushes "+te.getPushDirection());
		h.assertTrue(p.open(pos) != null, "line builder opens no GUI");
		p.putInto(0, Items.COBBLESTONE, 2);
		p.closeMenu();

		h.succeedWhen(() -> {
			Object[] line = {h.getBlockState(new BlockPos(2, 3, 5)).getBlock(), h.getBlockState(new BlockPos(1, 3, 5)).getBlock(), h.getBlockState(new BlockPos(0, 3, 5)).getBlock()};
			h.assertTrue(line[0] == Blocks.COBBLESTONE && line[1] == Blocks.COBBLESTONE && line[2] == Blocks.STONE,
					"line "+line[0]+" "+line[1]+" "+line[2]+"; "+te.power+" W @ "+te.torque+" Nm");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
