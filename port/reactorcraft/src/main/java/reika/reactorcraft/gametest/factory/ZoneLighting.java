/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft lamp built and run by a simulated survival player. Not part of the original
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
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.LampBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneLighting {

	public static final List<String> BLOCKS = List.of("rotarycraft:lamp");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * The player sets a lamp on a dirt block and puts a wound coil in through its GUI: the air around fills with light. The dirt under it
	 * swapped for a redstone block puts the light out; mined away again, the lamp lights up once more.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600, batch = "factory")
	public static void lampLightsAndObeysRedstone(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.LAMP.get()), new ItemStack(Items.DIRT), new ItemStack(Items.REDSTONE_BLOCK),
				new ItemStack(Items.IRON_PICKAXE), CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), 200));
		Factory.take(p, chest, RotaryBlocks.LAMP.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.REDSTONE_BLOCK, 1);
		Factory.takeExactly(p, chest, Items.IRON_PICKAXE, 1);
		Factory.takeExactly(p, chest, RotaryItems.SPRING.get(), 1);

		BlockPos pos = new BlockPos(4, 3, 4), probe = new BlockPos(4, 3, 6);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.LAMP.get(), Direction.EAST);
		h.assertTrue(p.open(pos) != null, "lamp opens no GUI");
		p.putInto(0, RotaryItems.SPRING.get(), 1);
		p.closeMenu();
		LampBlockEntity te = (LampBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getCoilCharge() == 200 && p.count(RotaryItems.SPRING.get()) == 0, "coil not in the lamp: "+te.getInventory().getStackInSlot(0));

		int[] stage = {0};
		h.succeedWhen(() -> {
			if (stage[0] == 0) {
				h.assertBlockPresent(Blocks.LIGHT, probe);
				p.mine(pos.below(), Items.IRON_PICKAXE); //the dirt under the lamp becomes a redstone block
				p.placeSneaking(pos.below(), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST);
				stage[0] = 1;
				h.fail("redstone block under the lamp");
			}
			if (stage[0] == 1) {
				h.assertBlockNotPresent(Blocks.LIGHT, probe);
				h.assertFalse(te.isLit(), "lamp lit with a signal");
				p.mine(pos.below(), Items.IRON_PICKAXE);
				stage[0] = 2;
				h.fail("redstone block mined");
			}
			h.assertBlockPresent(Blocks.LIGHT, probe);
			h.assertTrue(te.isLit(), "lamp dark again");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
