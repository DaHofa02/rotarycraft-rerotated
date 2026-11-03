/*******************************************************************************
 * Tests for the ported RotaryCraft compactor (original TileEntityCompactor, RecipesCompactor). Private NeoForge 1.21.1 port of
 * RotaryCraft by Reika Kalseki; not for distribution.
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.CompactorBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CompactorTests {

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/**
	 * Microturbine (131072 rad/s @ 16 Nm) -> two bedrock gearboxes 16:1 reduction (512 rad/s @ 4096 Nm = 2 MW) -> compactor (meta 0 reads from
	 * the east), at (x, 1, 4) with the turbine at x+3. Shared with the ReactorCraft magnet test.
	 */
	public static CompactorBlockEntity rig(GameTestHelper h, int x) {
		EngineBlockEntity e = place(h, RotaryBlocks.MICRO_ENGINE.get(), x+3, 1, 4, 0, EngineBlockEntity.class);
		IFluidHandler fh = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, e.getBlockPos(), Direction.DOWN);
		fh.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 20000), IFluidHandler.FluidAction.EXECUTE);
		for (int i = 1; i <= 2; i++) {
			GearboxBlockEntity g = place(h, RotaryBlocks.GEARBOX.get(), x+i, 1, 4, 12, GearboxBlockEntity.class);
			g.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16));
			g.setBlockMetadata(12);
			g.fillWithLubricant();
			g.reduction = true;
		}
		return place(h, RotaryBlocks.COMPACTOR.get(), x, 1, 4, 0, CompactorBlockEntity.class);
	}

	/** Four ice at 24000 kPa become 2 packed ice; the pressure rises by 128*log2(4096) = 1536 kPa per second while turning. */
	@GameTest(template = "platform_9x4x9", timeoutTicks = 3200)
	public static void compactorPacksIce(GameTestHelper h) {
		CompactorBlockEntity c = rig(h, 2);
		for (int i = 0; i < 4; i++)
			c.getInventory().setStackInSlot(i, new ItemStack(Items.ICE));
		h.succeedWhen(() -> {
			h.assertTrue(c.torque == 4096, "input "+c.omega+" rad/s @ "+c.torque+" Nm"); //the turbine may still be spinning up
			ItemStack out = c.getInventory().getStackInSlot(4);
			h.assertTrue(out.is(Items.PACKED_ICE) && out.getCount() == 2, "output "+out+" at "+c.getPressure()+" kPa");
			h.assertTrue(c.getInventory().getStackInSlot(0).isEmpty() && c.getPressure() >= 24000, "ice left "+c.getInventory().getStackInSlot(0));
		});
	}

	/** Without enough pressure nothing happens, and mixed inputs are never pressed. */
	@GameTest(template = "platform_9x4x9", timeoutTicks = 400)
	public static void compactorNeedsFourEqualItems(GameTestHelper h) {
		CompactorBlockEntity c = rig(h, 2);
		c.setPressure(30000);
		for (int i = 0; i < 3; i++)
			c.getInventory().setStackInSlot(i, new ItemStack(Items.ICE));
		c.getInventory().setStackInSlot(3, new ItemStack(Items.BLAZE_POWDER));
		h.runAtTickTime(350, () -> {
			h.assertTrue(c.getInventory().getStackInSlot(4).isEmpty() && c.compactorCookTime == 0, "pressed mixed items");
			h.succeed();
		});
	}
}
