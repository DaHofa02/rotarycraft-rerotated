/*******************************************************************************
 * Practice factory zone (see Factory): ReactorCraft fuel processing, built and run by a simulated survival player. Not part of the
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
import reika.reactorcraft.content.blockentity.UProcessorBlockEntity;
import reika.reactorcraft.registry.FluoriteTypes;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneProcessing {

	public static final List<String> BLOCKS = List.of("reactorcraft:processor");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Uranium processor, placed looking east (front west): a water barrel feeds a pipe on its north side (an input side; the process
	 * only runs while the input tank holds water), and through its GUI the player puts in fluorite crystals, a water bucket filled at
	 * the barrel and uranium ingots. Fluorite and water make hydrofluoric acid, acid and an ingot make 1000 mB uranium hexafluoride.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1000, batch = "factory")
	public static void processorMakesUF6(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack fluorite = new ItemStack(ReactorItems.fluorite(FluoriteTypes.GREEN).get());
		ItemStack uranium = new ItemStack(ReactorItems.ingot(ReactorItems.Ingot.URANIUM).get());
		Factory.infiniteChest(h, chest, new ItemStack(ReactorBlocks.PROCESSOR.get()), new ItemStack(RotaryBlocks.PIPE.get()), fluorite, uranium,
				new ItemStack(Items.BUCKET));
		BlockPos proc = new BlockPos(4, 2, 4);
		Factory.fluidBarrel(h, new BlockPos(4, 3, 3), Fluids.WATER);
		Factory.take(p, chest, ReactorBlocks.PROCESSOR.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.take(p, chest, fluorite, 4);
		Factory.take(p, chest, uranium, 2);
		Factory.takeExactly(p, chest, Items.BUCKET, 1);
		p.place(proc, ReactorBlocks.PROCESSOR.get(), Direction.EAST);
		p.placeSneaking(proc.north(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.use(new BlockPos(4, 3, 3), Items.BUCKET);
		p.open(proc);
		p.putInto(0, fluorite.getItem(), 4);
		p.putInto(1, Items.WATER_BUCKET, 1);
		p.putInto(2, uranium.getItem(), 2);
		p.closeMenu();
		UProcessorBlockEntity te = (UProcessorBlockEntity)h.getBlockEntity(proc);
		h.succeedWhen(() -> {
			h.assertTrue(te.getOutputTank().getFluidAmount() >= 1000 && te.getOutputTank().getFluid().getFluid().isSame(ReactorFluids.get("uranium_hexafluoride")),
					"UF6 "+te.getOutputTank().getFluidAmount()+" mB; water "+te.getInputTank().getFluidAmount()+", acid "+te.getIntermediateTank().getFluidAmount());
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 2, 8));
			p.leave();
		});
	}
}
