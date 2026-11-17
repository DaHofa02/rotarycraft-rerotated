/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft cave scanner built and used by a simulated survival player. Not part of the
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
import reika.rotarycraft.content.blockentity.CaveScannerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneCaveScanner {

	public static final List<String> BLOCKS = List.of("rotarycraft:cave_scanner");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * A gas turbine on fuel lines in a trench under a jet fuel barrel, choked by a cobblestone wall, drives a cave scanner west of it. It
	 * maps the floor; the player right-clicks it looking down, which moves the scanned cube 4 blocks down.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600, batch = "factory")
	public static void caveScannerMapsTheFloor(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.JET_ENGINE.get()),
				new ItemStack(Items.COBBLESTONE_WALL), new ItemStack(RotaryBlocks.CAVE_SCANNER.get()), new ItemStack(Items.IRON_PICKAXE));
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.JET_ENGINE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.COBBLESTONE_WALL, 1);
		Factory.take(p, chest, RotaryBlocks.CAVE_SCANNER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.IRON_PICKAXE, 1);
		BlockPos jet = new BlockPos(7, 2, 4), pos = new BlockPos(6, 2, 4);
		for (BlockPos t : new BlockPos[] {jet.below(), jet.below().south()}) {
			p.mine(t, Items.IRON_PICKAXE);
			p.place(t, RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		}
		p.placeSneaking(jet, new ItemStack(RotaryBlocks.JET_ENGINE.get()), Direction.EAST);
		p.place(jet.east(), Items.COBBLESTONE_WALL, Direction.EAST);
		p.place(pos, RotaryBlocks.CAVE_SCANNER.get(), Direction.EAST);
		Factory.fluidBarrel(h, jet.south(), RotaryFluids.JET_FUEL.get());
		CaveScannerBlockEntity te = (CaveScannerBlockEntity)h.getBlockEntity(pos);
		int r = te.getRange();

		boolean[] moved = {false};
		h.succeedWhen(() -> {
			h.assertTrue(te.on, "scanner off at "+te.getPower()+" W");
			BlockPos d = h.absolutePos(new BlockPos(2, 2, 2)).subtract(te.getSource());
			h.assertTrue(te.hasPointAt(d.getX()+r, d.getY()+r, d.getZ()+r), "floor corner not mapped");
			if (!moved[0]) {
				p.player.setXRot(90); //looking down
				p.use(pos);
				moved[0] = true;
				h.assertTrue(te.getSource().equals(h.absolutePos(pos.below(4))), "source at "+te.getSource());
				h.fail("moved; checking the new map");
			}
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
