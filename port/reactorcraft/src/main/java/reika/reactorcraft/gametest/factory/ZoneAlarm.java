/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft Smoke Detector built and run by a simulated survival player. Not part of the
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
import net.minecraft.world.level.block.RedstoneLampBlock;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.SmokeDetectorBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneAlarm {

	public static final List<String> BLOCKS = List.of("rotarycraft:smoke_detector");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * The player sets a smoke detector on a dirt pillar (mined away again, the detector hangs like under a ceiling), a redstone lamp on
	 * a cobblestone post beside it and puts a wound coil into the detector's GUI. Then they light netherrack with flint and steel: the
	 * detector alarms and the lamp lights. They break the netherrack, the fire goes out and the alarm ends.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600, batch = "factory")
	public static void detectorSeesAFire(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(Items.COBBLESTONE), new ItemStack(Items.DIRT), new ItemStack(RotaryBlocks.SMOKE_DETECTOR.get()),
				new ItemStack(Items.REDSTONE_LAMP), CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), 200), new ItemStack(Items.NETHERRACK),
				new ItemStack(Items.FLINT_AND_STEEL));

		Factory.takeExactly(p, chest, Items.COBBLESTONE, 2);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.take(p, chest, RotaryBlocks.SMOKE_DETECTOR.get().asItem(), 1);
		Factory.take(p, chest, Items.REDSTONE_LAMP, 1);
		Factory.takeExactly(p, chest, RotaryItems.SPRING.get(), 1);
		Factory.takeExactly(p, chest, Items.NETHERRACK, 1);
		Factory.takeExactly(p, chest, Items.FLINT_AND_STEEL, 1);
		BlockPos det = new BlockPos(4, 4, 4);
		BlockPos lamp = new BlockPos(5, 4, 4);
		for (int y = 2; y <= 3; y++) {
			p.place(det.atY(y), Items.DIRT, Direction.EAST);
			p.place(lamp.atY(y), Items.COBBLESTONE, Direction.EAST);
		}
		p.place(det, RotaryBlocks.SMOKE_DETECTOR.get(), Direction.EAST);
		p.placeSneaking(lamp, new ItemStack(Items.REDSTONE_LAMP), Direction.EAST);
		p.mine(det.atY(3));
		p.mine(det.atY(2));
		h.assertTrue(p.count(Items.DIRT) == 2, "dirt pillar not taken back");

		h.assertTrue(p.open(det) != null, "detector opens no GUI");
		p.putInto(0, RotaryItems.SPRING.get(), 1);
		p.closeMenu();
		SmokeDetectorBlockEntity te = (SmokeDetectorBlockEntity)h.getBlockEntity(det);
		h.assertTrue(te.getCoilCharge() == 200 && p.count(RotaryItems.SPRING.get()) == 0, "coil not in the detector: "+te.getInventory().getStackInSlot(0));

		BlockPos rack = new BlockPos(1, 2, 4);
		p.place(rack, Items.NETHERRACK, Direction.EAST);
		int[] stage = {0};
		h.succeedWhen(() -> {
			boolean lit = h.getBlockState(lamp).getValue(RedstoneLampBlock.LIT);
			if (stage[0] == 0) {
				h.assertTrue(!te.isAlarming() && !lit, "alarm before the fire");
				p.use(rack, Items.FLINT_AND_STEEL);
				h.assertBlockPresent(Blocks.FIRE, rack.above());
				stage[0] = 1;
				h.fail("fire lit; checking the alarm next tick");
			}
			if (stage[0] == 1) {
				h.assertTrue(te.isAlarming() && te.soundDelay == 4 && lit, "no alarm: "+te.isAlarming()+", lamp "+lit);
				p.mine(rack);
				stage[0] = 2;
				h.fail("netherrack broken; checking that the alarm ends");
			}
			h.assertBlockPresent(Blocks.AIR, rack.above());
			h.assertTrue(!te.isAlarming() && !lit, "alarm stays: "+te.isAlarming()+", lamp "+lit);
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 5, 8));
			p.leave();
		});
	}
}
