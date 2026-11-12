/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft spillway built and run by a simulated survival player. Not part of the original
 * mods.
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
import reika.rotarycraft.content.blockentity.SpillwayBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneWaterfall {

	public static final List<String> BLOCKS = List.of("rotarycraft:spillway");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * The player builds a cobblestone pillar, sets a spillway beside it facing the pillar's side and pours a water bucket on top of the
	 * pillar: the water falls down past the spillway, which collects 50 mB a tick.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void spillwayCollectsAWaterfall(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.SPILLWAY.get()), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.WATER_BUCKET));
		Factory.take(p, chest, RotaryBlocks.SPILLWAY.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.COBBLESTONE, 4);
		Factory.takeExactly(p, chest, Items.WATER_BUCKET, 1);
		for (int y = 2; y <= 5; y++)
			p.place(new BlockPos(6, y, 4), Items.COBBLESTONE, Direction.EAST);
		BlockPos pos = new BlockPos(4, 2, 4);
		p.place(pos, RotaryBlocks.SPILLWAY.get(), Direction.EAST); //looking east: takes water from the east
		//pours the bucket looking straight down onto the pillar (a bucket aims with the player's eyes, as in ZoneUtilities)
		net.minecraft.world.phys.Vec3 eye = net.minecraft.world.phys.Vec3.atLowerCornerOf(h.absolutePos(BlockPos.ZERO)).add(6.5, 7.5, 4.5);
		p.hold(Items.WATER_BUCKET);
		p.player.moveTo(eye.x, eye.y-p.player.getEyeHeight(), eye.z, 0, 90);
		p.player.setYHeadRot(0);
		p.useItem();
		SpillwayBlockEntity te = (SpillwayBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getDrainSide() == Direction.EAST, "spillway faces "+te.getDrainSide());

		h.succeedWhen(() -> {
			h.assertTrue(te.getWaterLevel() >= 4000 && te.isActive(), "spillway holds "+te.getWaterLevel()+" mB");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
