/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft firework machine built and filled by a simulated survival player. Not part of the
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
import reika.rotarycraft.content.blockentity.FireworkMachineBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFireworks {

	public static final List<String> BLOCKS = List.of("rotarycraft:firework_machine");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * A microturbine on two fuel lines (a jet fuel barrel on the second) drives the firework machine west of it; the player fills it with paper,
	 * gunpowder, blue dye, glowstone dust and gold nuggets through its GUI: it starts launching rockets.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void fireworkMachineCelebrates(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()),
				new ItemStack(RotaryBlocks.FIREWORK_MACHINE.get()), new ItemStack(Items.PAPER), new ItemStack(Items.GUNPOWDER), new ItemStack(Items.BLUE_DYE),
				new ItemStack(Items.GLOWSTONE_DUST), new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.DIRT));
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.MICRO_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.FIREWORK_MACHINE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.PAPER, 8);
		Factory.takeExactly(p, chest, Items.GUNPOWDER, 16);
		Factory.takeExactly(p, chest, Items.BLUE_DYE, 8);
		Factory.takeExactly(p, chest, Items.GLOWSTONE_DUST, 4);
		Factory.takeExactly(p, chest, Items.GOLD_NUGGET, 4);
		BlockPos engine = new BlockPos(6, 3, 5), pos = new BlockPos(5, 3, 5);
		p.place(engine.below(), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.place(engine.below().east(), RotaryBlocks.FUEL_LINE.get(), Direction.EAST); //the barrel stands on this one
		p.placeSneaking(engine, new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.EAST); //looking east: drives west
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.FIREWORK_MACHINE.get(), Direction.EAST);
		h.assertTrue(p.open(pos) != null, "firework machine opens no GUI");
		p.putInto(0, Items.PAPER, 8);
		p.putInto(1, Items.GUNPOWDER, 16);
		p.putInto(2, Items.BLUE_DYE, 8);
		p.putInto(3, Items.GLOWSTONE_DUST, 4);
		p.putInto(4, Items.GOLD_NUGGET, 4);
		p.closeMenu();
		Factory.fluidBarrel(h, engine.east(), RotaryFluids.JET_FUEL.get());
		FireworkMachineBlockEntity te = (FireworkMachineBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.launched >= 1, "no rocket yet at "+te.getOmega()+" rad/s, "+te.getPower()+" W");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
