/*******************************************************************************
 * Tests for the ported RotaryCraft refrigerator (original TileEntityRefrigerator). Private NeoForge 1.21.1 port of RotaryCraft by
 * Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RefrigeratorBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RefrigeratorTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (512 rad/s @ 128 Nm) -> steel gearbox 16:1 reduction (32 rad/s @ 2048 Nm) -> refrigerator (meta 0 reads from the east). */
	private static RefrigeratorBlockEntity rig(GameTestHelper h, boolean gearbox) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), gearbox ? 6 : 5, 1, 4, 0, EngineBlockEntity.class).addFuel(50000);
		if (gearbox) {
			GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 5, 1, 4, 12, GearboxBlockEntity.class);
			gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.STEEL, 16));
			gbx.setBlockMetadata(12);
			gbx.fillWithLubricant();
			gbx.reduction = true;
		}
		return place(h, RotaryBlocks.REFRIGERATOR.get(), 4, 1, 4, 0, RefrigeratorBlockEntity.class);
	}

	/**
	 * At 2048 Nm (torque/MINTORQUE = 1) a cycle takes 1000 - 80*log2(33) = 596 ticks, uses one ice and makes 100 mB of liquid nitrogen,
	 * which a pipe can take out of any side.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 900)
	public static void fridgeMakesNitrogen(GameTestHelper h) {
		RefrigeratorBlockEntity f = rig(h, true);
		f.getInventory().setStackInSlot(0, new ItemStack(Items.ICE, 3));
		h.runAtTickTime(40, () -> {
			h.assertTrue(f.omega == 32 && f.torque == 2048, "input "+f.omega+" rad/s @ "+f.torque+" Nm");
			h.assertTrue(f.getOperationTime() == 596 && f.getProducedLN2() == 100, "time "+f.getOperationTime()+", "+f.getProducedLN2()+" mB");
		});
		h.succeedWhen(() -> {
			h.assertTrue(f.getTank().getFluidAmount() == 100 && f.getTank().getFluid().is(RotaryFluids.LIQUID_NITROGEN.get()), "tank "+f.getTank().getFluid().getAmount());
			h.assertTrue(f.getInventory().getStackInSlot(0).getCount() == 2, "ice "+f.getInventory().getStackInSlot(0));
			ItemStack dry = f.getInventory().getStackInSlot(1);
			h.assertTrue(dry.isEmpty() || dry.is(RotaryItems.part(CraftingPart.DRYICE)), "slot 1 "+dry);
			IFluidHandler cap = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(new BlockPos(4, 1, 4)), Direction.UP);
			h.assertTrue(cap != null && cap.drain(40, IFluidHandler.FluidAction.SIMULATE).getAmount() == 40, "LN2 not drainable from the top");
			h.assertTrue(cap.fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), 100), IFluidHandler.FluidAction.SIMULATE) == 0, "accepted LN2 in");
		});
	}

	/** Straight from a gas engine (128 Nm < 2048) nothing happens. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void fridgeNeedsTorque(GameTestHelper h) {
		RefrigeratorBlockEntity f = rig(h, false);
		f.getInventory().setStackInSlot(0, new ItemStack(Items.ICE, 3));
		h.runAtTickTime(60, () -> {
			h.assertTrue(f.torque == 128, "input torque "+f.torque);
			h.assertTrue(f.time == 0 && f.getTank().isEmpty(), "progress "+f.time);
			h.succeed();
		});
	}

	/** Breaking a full refrigerator hurts living things within 5 blocks for 4 hearts (freeze damage). */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void fridgeBreakFreezes(GameTestHelper h) {
		RefrigeratorBlockEntity f = place(h, RotaryBlocks.REFRIGERATOR.get(), 4, 1, 4, 0, RefrigeratorBlockEntity.class);
		f.getTank().fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), RefrigeratorBlockEntity.CAPACITY), IFluidHandler.FluidAction.EXECUTE);
		Cow cow = h.spawn(EntityType.COW, new BlockPos(6, 1, 4));
		h.runAtTickTime(5, () -> h.setBlock(new BlockPos(4, 1, 4), Blocks.AIR));
		h.runAtTickTime(10, () -> {
			h.assertTrue(Math.abs(cow.getHealth()-(cow.getMaxHealth()-8)) < 0.01, "cow health "+cow.getHealth());
			h.assertTrue(cow.getLastDamageSource() != null && cow.getLastDamageSource().is(reika.rotarycraft.registry.RotaryDamage.FREEZE), "damage "+cow.getLastDamageSource());
			h.succeed();
		});
	}
}
