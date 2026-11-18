/*******************************************************************************
 * Practice factory zone (see Factory): the port's purifier set up by a simulated survival player. Not part of the original mods.
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.PurifierBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZonePurifier {

	public static final List<String> BLOCKS = List.of("rotarycraft:purifier");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Gas engine (looking west: drives east) -> purifier; the player walls in a spot east of it with stone, pours lava there and fills the
	 * purifier through its GUI with gunpowder, sand and stand-in "mod steel" (netherite scrap): it heats up and makes steel.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1400, batch = "factory")
	public static void purifierMakesSteel(GameTestHelper h) {
		PurifierBlockEntity.TEST_STEELS.add(Items.NETHERITE_SCRAP);
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		p.give(new ItemStack(RotaryBlocks.GAS_ENGINE.get()));
		p.give(new ItemStack(RotaryBlocks.PURIFIER.get()));
		p.give(new ItemStack(Items.STONE, 3));
		p.give(new ItemStack(Items.LAVA_BUCKET));
		p.give(new ItemStack(Items.GUNPOWDER, 8));
		p.give(new ItemStack(Items.SAND, 8));
		p.give(new ItemStack(Items.NETHERITE_SCRAP, 4));
		BlockPos engine = new BlockPos(1, 2, 4), pur = new BlockPos(2, 2, 4), lava = new BlockPos(3, 2, 4);
		p.place(engine, RotaryBlocks.GAS_ENGINE.get(), Direction.WEST);
		((EngineBlockEntity)h.getBlockEntity(engine)).addFuel(50000); //what a fuel line would bring
		p.place(pur, RotaryBlocks.PURIFIER.get(), Direction.EAST);
		for (BlockPos s : new BlockPos[] {new BlockPos(4, 2, 4), new BlockPos(3, 2, 3), new BlockPos(3, 2, 5)})
			p.place(s, Items.STONE, Direction.EAST);
		Vec3 eye = Vec3.atLowerCornerOf(h.absolutePos(lava)).add(0.5, 3, 0.5);
		p.hold(Items.LAVA_BUCKET);
		p.player.moveTo(eye.x, eye.y-p.player.getEyeHeight(), eye.z, 0, 90);
		p.player.setYHeadRot(0);
		p.useItem();
		h.assertTrue(p.count(Items.LAVA_BUCKET) == 0, "lava not poured");
		h.assertTrue(p.open(pur) != null, "purifier opens no GUI");
		p.putInto(0, Items.GUNPOWDER, 8);
		p.putInto(7, Items.SAND, 8);
		p.putInto(1, Items.NETHERITE_SCRAP, 4);
		p.closeMenu();
		PurifierBlockEntity te = (PurifierBlockEntity)h.getBlockEntity(pur);
		h.succeedWhen(() -> {
			h.assertTrue(te.converted >= 1, "converted "+te.converted+" at "+te.temperature+" C, "+te.getPower()+" W");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 3, 8));
			h.setBlock(lava, net.minecraft.world.level.block.Blocks.STONE);
			PurifierBlockEntity.TEST_STEELS.remove(Items.NETHERITE_SCRAP);
			p.leave();
		});
	}
}
