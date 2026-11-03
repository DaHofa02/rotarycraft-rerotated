/*******************************************************************************
 * Tests for the ported ReactorCraft tritizer / neutron irradiation chamber (original TileEntityTritizer). Private NeoForge 1.21.1 port
 * of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.block.ReactorCoreBlock;
import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.reactor.TritizerBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TritizerTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static TritizerBlockEntity tritizer(GameTestHelper h, BlockPos p) {
		h.setBlock(p, ReactorBlocks.TRITIZER.get());
		return (TritizerBlockEntity)h.getBlockEntity(p);
	}

	private static int irradiate(GameTestHelper h, TritizerBlockEntity te, NeutronEntity.NeutronType type, int n) {
		int absorbed = 0;
		for (int i = 0; i < n; i++) {
			NeutronEntity e = new NeutronEntity(h.getLevel(), te.getBlockPos(), Direction.EAST, type);
			if (te.onNeutron(e, h.getLevel(), te.getBlockPos()))
				absorbed++;
		}
		return absorbed;
	}

	/**
	 * Water in, fission neutrons: 25 % of them make 100 mB heavy water each (and are absorbed) until the 1000 mB tank runs dry; decay and
	 * waste neutrons do nothing; only liquids with a reaction go in, and only from the top.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void waterBecomesHeavyWater(GameTestHelper h) {
		TritizerBlockEntity te = tritizer(h, new BlockPos(4, 1, 4));
		h.assertTrue(te.fill(Direction.UP, new FluidStack(ReactorFluids.get("tritium"), 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took tritium");
		h.assertTrue(te.fill(Direction.NORTH, new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "filled from the side");
		h.assertTrue(te.fill(Direction.UP, new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "no water taken");
		h.assertTrue(irradiate(h, te, NeutronEntity.NeutronType.DECAY, 200) == 0 && te.getInput().getFluidAmount() == 1000, "decay neutrons reacted");
		int a = irradiate(h, te, NeutronEntity.NeutronType.FISSION, 20);
		h.assertTrue(a >= 1 && a <= 10 && te.getOutput().getFluidAmount() == a*100 && te.getInput().getFluidAmount() == 1000-a*100,
				a+" reactions, in "+te.getInput().getFluidAmount()+", out "+te.getOutput().getFluidAmount());
		irradiate(h, te, NeutronEntity.NeutronType.FISSION, 400);
		h.assertTrue(te.getInput().isEmpty() && te.getOutput().getFluidAmount() == 1000
				&& te.getOutput().getFluid().getFluid().isSame(ReactorFluids.get("heavy_water")), "out "+te.getOutput().getFluid().getAmount());
		h.assertTrue(te.drain(Direction.UP, 1000, IFluidHandler.FluidAction.SIMULATE).isEmpty() && te.drain(Direction.DOWN, 300, IFluidHandler.FluidAction.EXECUTE).getAmount() == 300,
				"drain sides");
		h.succeed();
	}

	/** Deuterium -> tritium with 75 % at 25 mB per reaction. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void deuteriumBecomesTritium(GameTestHelper h) {
		TritizerBlockEntity te = tritizer(h, new BlockPos(4, 1, 4));
		te.fill(Direction.UP, new FluidStack(ReactorFluids.get("deuterium"), 1000), IFluidHandler.FluidAction.EXECUTE);
		int a = irradiate(h, te, NeutronEntity.NeutronType.FISSION, 20);
		h.assertTrue(a >= 10 && te.getOutput().getFluidAmount() == a*25 && te.getOutput().getFluid().getFluid().isSame(ReactorFluids.get("tritium")),
				a+" reactions, out "+te.getOutput().getFluidAmount());
		h.succeed();
	}

	/** A column: the top tritizer passes its input and output down to the one below; sides show the column position (1 top, 3 bottom). */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void columnPassesLiquidsDown(GameTestHelper h) {
		TritizerBlockEntity bottom = tritizer(h, new BlockPos(4, 1, 4));
		TritizerBlockEntity top = tritizer(h, new BlockPos(4, 2, 4));
		top.fill(Direction.UP, new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		irradiate(h, top, NeutronEntity.NeutronType.FISSION, 400);
		h.runAtTickTime(5, () -> {
			h.assertTrue(top.getInput().isEmpty() && top.getOutput().isEmpty(), "top still holds "+top.getInput().getFluidAmount()+"/"+top.getOutput().getFluidAmount());
			h.assertTrue(bottom.getOutput().getFluidAmount() == 1000, "bottom output "+bottom.getOutput().getFluidAmount());
			h.assertTrue(h.getBlockState(new BlockPos(4, 2, 4)).getValue(ReactorCoreBlock.STATE) == 3 && h.getBlockState(new BlockPos(4, 1, 4)).getValue(ReactorCoreBlock.STATE) == 1,
					"column states "+h.getBlockState(new BlockPos(4, 2, 4))+", "+h.getBlockState(new BlockPos(4, 1, 4)));
			h.succeed();
		});
	}
}
