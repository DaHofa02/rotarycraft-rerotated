/*******************************************************************************
 * Factory zone: mob farm (RotaryCraft mob harvester). Private NeoForge 1.21.1 port of RotaryCraft/ReactorCraft by Reika Kalseki; not for
 * distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.MobHarvesterBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneMobFarm {

	public static final List<String> BLOCKS = List.of("rotarycraft:mob_harvester");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel line -> gas engine (65536 W) -> mob harvester (19 damage per hit). The player, who is online (the harvester
	 * hits in its placer's name), sets a cow onto it with a spawn egg: the cow dies at once and leaves beef.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 6000, batch = "factory")
	public static void harvesterKillsACow(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		p.join();
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.MOB_HARVESTER.get()), new ItemStack(Items.COW_SPAWN_EGG));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 4), RotaryFluids.ETHANOL.get());

		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.MOB_HARVESTER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.COW_SPAWN_EGG, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 4), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 4), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 4);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.MOB_HARVESTER.get()), Direction.EAST);
		MobHarvesterBlockEntity te = (MobHarvesterBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getPlacer() == p.player, "the harvester does not find its placer");

		AABB area = new AABB(h.absolutePos(new BlockPos(4, 2, 4))).inflate(12);
		boolean[] spawned = {false};
		h.succeedWhen(() -> {
			h.assertTrue(te.getPower() == 65536 && te.getDamage() == 19, "harvester at "+te.getPower()+" W, damage "+te.getDamage());
			if (!spawned[0]) {
				p.use(pos, Items.COW_SPAWN_EGG);
				spawned[0] = true;
				h.assertTrue(p.count(Items.COW_SPAWN_EGG) == 0 && !h.getLevel().getEntitiesOfClass(Cow.class, area).isEmpty(), "no cow from the spawn egg");
				h.fail("cow set onto the harvester");
			}
			h.assertTrue(h.getLevel().getEntitiesOfClass(Cow.class, area, Cow::isAlive).isEmpty(), "the cow is still alive");
			int beef = p.count(Items.BEEF);
			for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, area))
				if (e.getItem().is(Items.BEEF))
					beef += e.getItem().getCount();
			h.assertTrue(beef >= 1, "no beef from the cow");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
