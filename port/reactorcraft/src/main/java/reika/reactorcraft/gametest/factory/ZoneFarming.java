/*******************************************************************************
 * Practice factory zone (see Factory): RotaryCraft farming machines built and run by a simulated survival player. Not part of the
 * original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.FanBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.lib.world.CropHelper;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFarming {

	public static final List<String> BLOCKS = List.of("rotarycraft:fan");

	private static final String PLATFORM = "platform_9x8x9";

	/** The player tills dirt with a hoe, plants wheat and ripens it with bone meal (vanilla 1.21). */
	private static void plantRipeWheat(SimPlayer p, BlockPos chest, BlockPos... soil) {
		Factory.takeExactly(p, chest, Items.DIRT, soil.length);
		Factory.takeExactly(p, chest, Items.DIAMOND_HOE, 1);
		Factory.takeExactly(p, chest, Items.WHEAT_SEEDS, soil.length);
		Factory.take(p, chest, Items.BONE_MEAL, 32);
		for (BlockPos s : soil) {
			p.place(s, Items.DIRT, Direction.EAST);
			p.hold(Items.DIAMOND_HOE);
			p.useOn(s, Direction.UP);
			p.hold(Items.WHEAT_SEEDS);
			p.useOn(s, Direction.UP);
			for (int i = 0; i < 16 && !CropHelper.isRipe(p.helper.getBlockState(s.above())); i++)
				p.use(s.above(), Items.BONE_MEAL);
		}
	}

	private static int countAround(GameTestHelper h, SimPlayer p, net.minecraft.world.item.Item item) {
		int n = p.count(item);
		for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(4, 2, 4))).inflate(12)))
			if (e.getItem().is(item))
				n += e.getItem().getCount();
		return n;
	}

	/**
	 * Ethanol barrel -> fuel line -> gas engine (65536 W) -> fan blowing west along z = 5 (range 8 + 64512/1024 = 71). In front of it the
	 * player tills three dirt blocks, plants wheat and ripens it with bone meal, sets a cobweb into the beam and a dirt wall at the end: the fan harvests the ripe
	 * wheat (replanted at age 0, wheat dropped) and blows the web away. Then the player puts a diffuser in: a wide fan, range 39.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 6000, batch = "factory")
	public static void fanHarvestsWheat(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.FAN.get()), new ItemStack(Items.DIRT), new ItemStack(Items.DIAMOND_HOE), new ItemStack(Items.WHEAT_SEEDS),
				new ItemStack(Items.BONE_MEAL), new ItemStack(Items.COBWEB), new ItemStack(RotaryItems.part(CraftingPart.DIFFUSER)));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());

		BlockPos[] soil = {new BlockPos(3, 2, 5), new BlockPos(2, 2, 4), new BlockPos(1, 2, 6)};
		plantRipeWheat(p, chest, soil);
		for (BlockPos s : soil)
			h.assertTrue(CropHelper.isRipe(h.getBlockState(s.above())), "wheat at "+s.above()+" not ripe: "+h.getBlockState(s.above()));
		int wheat0 = countAround(h, p, Items.WHEAT);

		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.FAN.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.COBWEB, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos fanPos = new BlockPos(5, 3, 5);
		p.placeSneaking(fanPos, new ItemStack(RotaryBlocks.FAN.get()), Direction.EAST);
		BlockPos web = new BlockPos(4, 3, 5);
		p.placeSneaking(web, new ItemStack(Items.COBWEB), Direction.EAST); //in the beam, against the fan's front
		Factory.takeExactly(p, chest, Items.DIRT, 6);
		for (int y = 2; y <= 3; y++) //a wall at the end of the field that catches what the fan blows away
			for (int z = 4; z <= 6; z++)
				p.place(new BlockPos(0, y, z), Items.DIRT, Direction.EAST);
		FanBlockEntity fan = (FanBlockEntity)h.getBlockEntity(fanPos);
		h.assertTrue(fan.getBlockMetadata() == 0 && fan.getFacing() == Direction.WEST, "fan meta "+fan.getBlockMetadata()+" blows "+fan.getFacing());

		boolean[] upgraded = {false};
		h.succeedWhen(() -> {
			h.assertTrue(fan.getPower() == 65536, "fan at "+fan.getOmega()+" rad/s @ "+fan.getTorque()+" Nm");
			h.assertTrue(fan.getRange() == (upgraded[0] ? 39 : 71), "fan range "+fan.getRange());
			for (BlockPos s : soil) {
				h.assertBlockPresent(Blocks.WHEAT, s.above());
				h.assertTrue(h.getBlockState(s.above()).getValue(CropBlock.AGE) < 7, "wheat at "+s.above()+" not harvested yet"); //replanted at 0 (may grow again)
			}
			h.assertBlockNotPresent(Blocks.COBWEB, web);
			int wheat = countAround(h, p, Items.WHEAT)-wheat0;
			h.assertTrue(wheat >= 3, "only "+wheat+" wheat harvested");
			if (!upgraded[0]) { //then the diffuser: a wide fan
				Factory.takeExactly(p, chest, RotaryItems.part(CraftingPart.DIFFUSER), 1);
				p.use(fanPos, RotaryItems.part(CraftingPart.DIFFUSER));
				upgraded[0] = true;
				h.assertTrue(fan.wideAreaBlow && p.count(RotaryItems.part(CraftingPart.DIFFUSER)) == 0, "diffuser not installed");
				h.fail("installed the diffuser; checking the wide fan next tick");
			}
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
