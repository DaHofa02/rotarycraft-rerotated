/*******************************************************************************
 * Tests for the ported RotaryCraft fluid compressor (original TileEntityFluidCompressor). Private NeoForge 1.21.1 port of RotaryCraft by
 * Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.FluidCompressorBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryComponents;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FluidCompressorTests {

	private static final String PLATFORM = "platform_9x8x9";
	private static final BlockPos POS = new BlockPos(4, 3, 4);

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (512 rad/s @ 128 Nm) -> bedrock gearbox 8:1 (64 rad/s @ 1024 Nm) -> bevel gears east->up -> compressor above them. */
	private static FluidCompressorBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 6, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		GearboxBlockEntity g = place(h, RotaryBlocks.GEARBOX.get(), 5, 2, 4, 8, GearboxBlockEntity.class);
		g.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 8));
		g.setBlockMetadata(8);
		g.fillWithLubricant();
		g.reduction = true;
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		return place(h, RotaryBlocks.FLUID_COMPRESSOR.get(), 4, 3, 4, 0, FluidCompressorBlockEntity.class);
	}

	/** Capacity 10^(floor(log2(torque)/2))/40 mB, eight times for gases; nothing below 1024 Nm or 16384 W. */
	@GameTest(template = PLATFORM)
	public static void compressorCapacityFollowsTheOriginal(GameTestHelper h) {
		FluidCompressorBlockEntity te = place(h, RotaryBlocks.FLUID_COMPRESSOR.get(), 4, 3, 4, 0, FluidCompressorBlockEntity.class);
		Fluid gas = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("reactorcraft", "chlorine"));
		h.assertTrue(FluidCompressorBlockEntity.isGaseous(gas) && !FluidCompressorBlockEntity.isGaseous(Fluids.WATER), "chlorine is no gas");
		int[][] cases = {{1024, 2500, 20000}, {4096, 25000, 200000}, {16384, 250000, 2000000}, {1023, 0, 0}};
		for (int[] c : cases) {
			te.torque = c[0];
			te.omega = 64;
			te.power = 65536;
			h.assertTrue(te.getCapacity(Fluids.WATER) == c[1] && te.getCapacity(gas) == c[2], c[0]+" Nm: "+te.getCapacity(Fluids.WATER)+" / "+te.getCapacity(gas)+" mB");
		}
		te.torque = 1024;
		te.power = 16383;
		h.assertTrue(te.getCapacity(Fluids.WATER) == 0, "takes fluid below 16384 W");
		h.succeed();
	}

	/**
	 * At 1024 Nm from below it takes 2500 mB of water and no other fluid; it can be drained from any side; without power it takes nothing
	 * but keeps its water, which also stays with the item when it is broken.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void compressorStoresWaterAndKeepsIt(GameTestHelper h) {
		FluidCompressorBlockEntity te = rig(h);
		IFluidHandler side = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(POS), Direction.NORTH);
		h.succeedWhen(() -> {
			h.assertTrue(te.torque == 1024 && te.power == 65536, "compressor at "+te.omega+" rad/s @ "+te.torque+" Nm");
			h.assertTrue(side.fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE) == 2500, "not 2500 mB");
			h.assertTrue(side.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "lava went in");
			h.assertTrue(side.drain(500, IFluidHandler.FluidAction.EXECUTE).getAmount() == 500, "no water out");
			h.setBlock(new BlockPos(6, 2, 4), net.minecraft.world.level.block.Blocks.AIR); //engine gone
			SimpleFluidContent c = te.collectComponents().get(RotaryComponents.TANK_FLUID.get());
			h.assertTrue(c != null && c.getAmount() == 2000 && c.is(Fluids.WATER), "item data "+c);
		});
	}

	/** Unpowered it takes nothing. */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void compressorNeedsItsTorque(GameTestHelper h) {
		place(h, RotaryBlocks.FLUID_COMPRESSOR.get(), 4, 3, 4, 0, FluidCompressorBlockEntity.class);
		h.runAtTickTime(20, () -> {
			IFluidHandler side = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(POS), Direction.NORTH);
			h.assertTrue(side.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "unpowered compressor took water");
			h.succeed();
		});
	}
}
