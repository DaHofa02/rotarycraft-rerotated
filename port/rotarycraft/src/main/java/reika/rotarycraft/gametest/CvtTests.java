/*******************************************************************************
 * Tests for the port's CVT (RotaryCraft advanced gears). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.AdvancedGearBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CvtTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	private static void belts(AdvancedGearBlockEntity te, int n) {
		for (int i = 0; i < 31; i++)
			te.belts.setStackInSlot(i, i < n ? new ItemStack(RotaryItems.part(CraftingPart.BELT)) : ItemStack.EMPTY);
	}

	/** Belts in a row allow ratios up to the largest power of two at most belts+1; a gap ends the row. The auto mode's choice of ratio. */
	@GameTest(template = PLATFORM)
	public static void cvtFollowsTheOriginalNumbers(GameTestHelper h) {
		AdvancedGearBlockEntity te = place(h, RotaryBlocks.CVT.get(), 4, 2, 4, 0, AdvancedGearBlockEntity.class);
		int[][] cases = {{0, 1}, {1, 2}, {2, 2}, {3, 4}, {7, 8}, {30, 16}, {31, 32}};
		for (int[] c : cases) {
			belts(te, c[0]);
			h.assertTrue(te.getMaxRatio() == c[1], c[0]+" belts: max "+te.getMaxRatio());
		}
		belts(te, 31);
		te.belts.setStackInSlot(2, ItemStack.EMPTY);
		h.assertTrue(te.getMaxRatio() == 2, "gap ignored: "+te.getMaxRatio());
		belts(te, 31);
		te.setTargetTorque(100);
		h.assertTrue(te.autoRatio(150) == 1 && te.autoRatio(1000) == 10 && te.autoRatio(10) == -10, "auto "+te.autoRatio(150)+" "+te.autoRatio(1000)+" "+te.autoRatio(10));
		h.assertFalse(te.belts.isItemValid(0, new ItemStack(net.minecraft.world.item.Items.STONE)), "stone as a belt");
		h.succeed();
	}

	/**
	 * Gas engine (512 rad/s @ 128 Nm) into a CVT with all belts: nothing without lubricant or without the required belt; manual 4 -> 2048 rad/s
	 * @ 32 Nm; manual -8 -> 64 rad/s @ 1024 Nm; auto with a 1000 Nm target picks -8 as well.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void cvtChangesRatio(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		AdvancedGearBlockEntity te = place(h, RotaryBlocks.CVT.get(), 2, 2, 4, 1, AdvancedGearBlockEntity.class);
		belts(te, 31);
		te.setCvtRatio(4);
		int[] phase = {0};
		h.succeedWhen(() -> {
			switch (phase[0]) {
				case 0 -> {
					h.assertTrue(te.getOmega() == 0, "turns without lubricant or required belt");
					te.addLubricantFromBucket(1000);
					phase[0] = 1;
					h.fail("lubricated");
				}
				case 1 -> {
					h.assertTrue(te.getOmega() == 0, "turns without the required belt");
					te.belts.setStackInSlot(31, new ItemStack(RotaryItems.part(CraftingPart.BELT)));
					phase[0] = 2;
					h.fail("belt in");
				}
				case 2 -> {
					h.assertTrue(te.getOmega() == 2048 && te.getTorque() == 32, "manual 4: "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
					te.setCvtRatio(-8);
					phase[0] = 3;
					h.fail("torque");
				}
				case 3 -> {
					h.assertTrue(te.getOmega() == 64 && te.getTorque() == 1024, "manual -8: "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
					te.setCvtRatio(1);
					te.setTargetTorque(1000);
					te.stepCvtMode();
					te.stepCvtMode(); //manual -> redstone -> auto
					phase[0] = 4;
					h.fail("auto");
				}
				default -> h.assertTrue(te.getCvtMode() == AdvancedGearBlockEntity.CvtMode.AUTO && te.getTorque() == 1024 && te.getOmega() == 64,
						"auto: "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			}
		});
	}
}
