/*******************************************************************************
 * Practice factory zone (see Factory): RotaryCraft belt hubs built and joined by a simulated survival player. Not part of the original
 * mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.BeltHubBlockEntity;
import reika.rotarycraft.content.blockentity.FanBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneBelt {

	public static final List<String> BLOCKS = List.of("rotarycraft:belt_hub", "rotarycraft:chain_drive", "rotarycraft:split_belt");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> belt hub; three blocks north a second hub, made the receiving end with the screwdriver
	 * (sneaking), the two joined with two belts; a fan behind the receiving hub runs at the engine's full 65536 W. Beside them a chain
	 * drive pair and a take-off belt pair are set down and joined too (chains, belts).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void beltDrivesTheFan(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Item belt = RotaryItems.part(CraftingPart.BELT), chain = RotaryItems.part(CraftingPart.CHAIN);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BELT_HUB.get()), new ItemStack(RotaryBlocks.CHAIN_DRIVE.get()), new ItemStack(RotaryBlocks.SPLIT_BELT.get()),
				new ItemStack(RotaryBlocks.FAN.get()), new ItemStack(Items.DIRT), new ItemStack(RotaryItems.SCREWDRIVER.get()), new ItemStack(belt),
				new ItemStack(chain));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BELT_HUB.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.CHAIN_DRIVE.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.SPLIT_BELT.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.FAN.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 3);
		Factory.takeExactly(p, chest, RotaryItems.SCREWDRIVER.get(), 1);
		Factory.takeExactly(p, chest, belt, 4);
		Factory.takeExactly(p, chest, chain, 2);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos drive = new BlockPos(5, 3, 5), recv = new BlockPos(5, 3, 2), fanPos = new BlockPos(6, 3, 2);
		p.place(drive.below(), Items.DIRT, Direction.EAST);
		p.place(recv.below(), Items.DIRT, Direction.EAST);
		p.place(fanPos.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(drive, new ItemStack(RotaryBlocks.BELT_HUB.get()), Direction.EAST); //looking east: input east
		p.placeSneaking(recv, new ItemStack(RotaryBlocks.BELT_HUB.get()), Direction.EAST);
		p.placeSneaking(fanPos, new ItemStack(RotaryBlocks.FAN.get()), Direction.WEST); //input west, blows east
		receiving(p, recv);
		p.use(drive, belt);
		p.use(recv, belt);
		h.assertTrue(p.count(belt) == 2, "belts left: "+p.count(belt));

		//a chain drive pair (along z = 7, 3 blocks) and a take-off belt pair (z = 0 .. 3 at x = 2), unpowered
		BlockPos c1 = new BlockPos(1, 2, 6), c2 = new BlockPos(4, 2, 6), s1 = new BlockPos(2, 2, 0), s2 = new BlockPos(2, 2, 3);
		p.placeSneaking(c1, new ItemStack(RotaryBlocks.CHAIN_DRIVE.get()), Direction.SOUTH); //meta 2: belt along x or y
		p.placeSneaking(c2, new ItemStack(RotaryBlocks.CHAIN_DRIVE.get()), Direction.SOUTH);
		p.placeSneaking(s1, new ItemStack(RotaryBlocks.SPLIT_BELT.get()), Direction.EAST);
		p.placeSneaking(s2, new ItemStack(RotaryBlocks.SPLIT_BELT.get()), Direction.EAST);
		receiving(p, c2);
		receiving(p, s2);
		p.use(c1, chain);
		p.use(c2, chain);
		p.use(s1, belt);
		p.use(s2, belt);
		h.assertTrue(p.count(chain) == 0 && p.count(belt) == 0, "chains/belts left: "+p.count(chain)+" / "+p.count(belt));

		BeltHubBlockEntity r = (BeltHubBlockEntity)h.getBlockEntity(recv);
		FanBlockEntity fan = (FanBlockEntity)h.getBlockEntity(fanPos);
		h.succeedWhen(() -> {
			h.assertTrue(r.hasValidConnection() && fan.getPower() == 65536, "fan at "+fan.getPower()+" W, belt "+r.hasValidConnection());
			h.assertTrue(((BeltHubBlockEntity)h.getBlockEntity(c2)).hasValidConnection(), "chain not joined");
			h.assertTrue(((BeltHubBlockEntity)h.getBlockEntity(s2)).hasValidConnection(), "take-off belt not joined");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}

	/** Sneak-right-click with the screwdriver: the receiving end. */
	private static void receiving(SimPlayer p, BlockPos hub) {
		p.hold(RotaryItems.SCREWDRIVER.get());
		p.player.setShiftKeyDown(true);
		try {
			p.useOn(hub, Direction.UP);
		}
		finally {
			p.player.setShiftKeyDown(false);
		}
		p.helper.assertTrue(((BeltHubBlockEntity)p.helper.getBlockEntity(hub)).isReceivingEnd, "not the receiving end: "+hub);
	}
}
