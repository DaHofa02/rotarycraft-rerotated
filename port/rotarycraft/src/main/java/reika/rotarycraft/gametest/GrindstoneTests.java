/*******************************************************************************
 * Tests for the ported RotaryCraft grindstone (original TileEntityGrindstone). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.GrindstoneBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryComponents;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class GrindstoneTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (512 rad/s @ 128 Nm) -> bedrock gearbox 2:1 (256 rad/s @ 256 Nm) -> grindstone at (4, 2, z) (meta 1: east-west axle). */
	private static GrindstoneBlockEntity rig(GameTestHelper h, int z) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 6, 2, z, 0, EngineBlockEntity.class).addFuel(50000);
		GearboxBlockEntity g = place(h, RotaryBlocks.GEARBOX.get(), 5, 2, z, 0, GearboxBlockEntity.class);
		g.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 2));
		g.setBlockMetadata(0);
		g.fillWithLubricant();
		g.reduction = true;
		GrindstoneBlockEntity te = place(h, RotaryBlocks.GRINDSTONE.get(), 4, 2, z, 1, GrindstoneBlockEntity.class);
		te.getTank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		return te;
	}

	private static ItemStack sword(int damage) {
		ItemStack is = new ItemStack(Items.IRON_SWORD);
		is.setDamageValue(damage);
		return is;
	}

	/** DurationRegistry.GRINDSTONE: max(1, 80 - 6*log2(omega+1)); tools, swords and shears only; the repair limit. */
	@GameTest(template = PLATFORM)
	public static void grindstoneFollowsTheOriginalNumbers(GameTestHelper h) {
		GrindstoneBlockEntity te = place(h, RotaryBlocks.GRINDSTONE.get(), 4, 2, 4, 1, GrindstoneBlockEntity.class);
		int[][] cases = {{256, 31}, {512, 25}, {65536, 1}};
		for (int[] c : cases) {
			te.omega = c[0];
			h.assertTrue(te.getOperationTime() == c[1], c[0]+" rad/s: "+te.getOperationTime()+" ticks");
		}
		h.assertTrue(GrindstoneBlockEntity.isSharpenable(new ItemStack(Items.SHEARS)) && GrindstoneBlockEntity.isSharpenable(new ItemStack(Items.DIAMOND_PICKAXE))
				&& !GrindstoneBlockEntity.isSharpenable(new ItemStack(Items.DIRT)) && !GrindstoneBlockEntity.isSharpenable(new ItemStack(Items.BOW)), "valid items");
		ItemStack s = sword(100);
		h.assertTrue(GrindstoneBlockEntity.getMinimumDamageForItem(s) == 0, "fresh sword limit");
		s.set(RotaryComponents.GRINDSTONE_REPAIRS.get(), 9);
		h.assertTrue(GrindstoneBlockEntity.getMinimumDamageForItem(s) == 250-5, "limit with 9 repairs left: "+GrindstoneBlockEntity.getMinimumDamageForItem(s));
		h.succeed();
	}

	/** Every 31 ticks one point of damage is mended for 100 mB of water: 1000 mB mend 10 points; the tool keeps 490 repairs. */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void grindstoneMendsATool(GameTestHelper h) {
		GrindstoneBlockEntity te = rig(h, 4);
		te.getInventory().setStackInSlot(0, sword(100));
		h.succeedWhen(() -> {
			ItemStack is = te.getInventory().getStackInSlot(0);
			h.assertTrue(te.power == 65536 && te.torque == 256, "grindstone at "+te.omega+" rad/s @ "+te.torque+" Nm");
			h.assertTrue(is.getDamageValue() == 90 && te.getTank().isEmpty(), "sword at "+is.getDamageValue()+", "+te.getTank().getFluidAmount()+" mB left");
			h.assertTrue(is.get(RotaryComponents.GRINDSTONE_REPAIRS.get()) == 490, "repairs left "+is.get(RotaryComponents.GRINDSTONE_REPAIRS.get()));
		});
	}

	/**
	 * A sword with 10 repairs left at damage 247 is mended twice (to 245, 8 repairs left: limit 246) and then no more; only then can it be
	 * taken out by automation. Only water goes in.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void grindstoneWearsToolsForGood(GameTestHelper h) {
		GrindstoneBlockEntity te = rig(h, 4);
		ItemStack s = sword(247);
		s.set(RotaryComponents.GRINDSTONE_REPAIRS.get(), 10);
		te.getInventory().setStackInSlot(0, s);
		IItemHandler items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(new BlockPos(4, 2, 4)), Direction.UP);
		IFluidHandler fluids = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(new BlockPos(4, 2, 4)), Direction.UP);
		h.assertTrue(items.extractItem(0, 1, true).isEmpty(), "unfinished sword came out");
		h.assertTrue(fluids.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "lava went in");
		h.runAtTickTime(200, () -> {
			ItemStack is = te.getInventory().getStackInSlot(0);
			h.assertTrue(is.getDamageValue() == 245 && is.get(RotaryComponents.GRINDSTONE_REPAIRS.get()) == 8, "sword at "+is.getDamageValue()+", repairs "+is.get(RotaryComponents.GRINDSTONE_REPAIRS.get()));
			h.assertTrue(te.getTank().getFluidAmount() == 800 && !te.hasValidItem(), te.getTank().getFluidAmount()+" mB left");
			h.assertTrue(items.extractItem(0, 1, false).is(Items.IRON_SWORD), "finished sword did not come out");
			h.succeed();
		});
	}
}
