/*******************************************************************************
 * Practice factory zone (see Factory): the port's worm gear built by a simulated survival player. Not part of the original mods.
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
import reika.rotarycraft.content.blockentity.AdvancedGearBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneAdvancedGear {

	public static final List<String> BLOCKS = List.of("rotarycraft:worm_gear", "rotarycraft:high_gear", "rotarycraft:industrial_coil", "rotarycraft:bedrock_coil", "rotarycraft:cvt");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> worm gear (64 times the torque) -> high-ratio gear, lubricated by a hose from a barrel above
	 * and switched to speed mode with a sneaking screwdriver click: 256 times the worm gear's speed comes out.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void wormGearMultipliesTorque(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.WORM_GEAR.get()), new ItemStack(Items.DIRT),
				new ItemStack(RotaryBlocks.HIGH_GEAR.get()), new ItemStack(RotaryBlocks.HOSE.get()), new ItemStack(RotaryItems.SCREWDRIVER.get()));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.WORM_GEAR.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.take(p, chest, RotaryBlocks.HIGH_GEAR.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.HOSE.get().asItem(), 1);
		Factory.take(p, chest, RotaryItems.SCREWDRIVER.get(), 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST); //looking east: drives west
		BlockPos pos = new BlockPos(5, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.WORM_GEAR.get()), Direction.EAST);
		AdvancedGearBlockEntity te = (AdvancedGearBlockEntity)h.getBlockEntity(pos);
		BlockPos hi = pos.west();
		p.place(hi.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(hi, new ItemStack(RotaryBlocks.HIGH_GEAR.get()), Direction.EAST);
		p.placeSneaking(hi.above(), new ItemStack(RotaryBlocks.HOSE.get()), Direction.EAST);
		Factory.fluidBarrel(h, hi.above(2), RotaryFluids.LUBRICANT.get());
		p.hold(RotaryItems.SCREWDRIVER.get());
		p.player.setShiftKeyDown(true);
		p.use(hi);
		p.player.setShiftKeyDown(false);
		AdvancedGearBlockEntity high = (AdvancedGearBlockEntity)h.getBlockEntity(hi);
		h.assertFalse(high.torquemode, "high-ratio gear still in torque mode");

		h.succeedWhen(() -> {
			h.assertTrue(te.getTorque() == 8192 && te.getOmega() == 7, "worm gear (meta "+te.getBlockMetadata()+") out "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			h.assertTrue(high.getOmega() == 7*256 && high.getTorque() == 8192/256, "high-ratio gear out "+high.getOmega()+" rad/s @ "+high.getTorque()+" Nm, lubricant "+high.getLubricant());
			Factory.assertBuilt(h, BLOCKS.subList(0, 2), new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> industrial coil: it charges. The player types 256 rad/s and 2 Nm into its GUI and puts a
	 * redstone block on it: it gives that out. A bedrock coil stands beside it, its GUI opened once.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void coilStoresAndReleases(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.INDUSTRIAL_COIL.get()), new ItemStack(RotaryBlocks.BEDROCK_COIL.get()), new ItemStack(Items.DIRT),
				new ItemStack(Items.REDSTONE_BLOCK));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.INDUSTRIAL_COIL.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEDROCK_COIL.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.takeExactly(p, chest, Items.REDSTONE_BLOCK, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5), bed = new BlockPos(2, 3, 2);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.INDUSTRIAL_COIL.get()), Direction.EAST);
		p.place(bed.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(bed, new ItemStack(RotaryBlocks.BEDROCK_COIL.get()), Direction.EAST);
		h.assertTrue(p.open(bed) != null, "bedrock coil opens no GUI");
		p.closeMenu();
		AdvancedGearBlockEntity te = (AdvancedGearBlockEntity)h.getBlockEntity(pos);
		boolean[] released = {false};
		int[] peak = {0, 0};
		h.onEachTick(() -> {
			if (te.getOmega() > peak[0]) {
				peak[0] = te.getOmega();
				peak[1] = te.getTorque();
			}
		});
		h.succeedWhen(() -> {
			if (!released[0]) {
				h.assertTrue(te.getEnergy() > 0, "coil not charging");
				h.assertTrue(p.open(pos) != null, "coil opens no GUI");
				p.player.containerMenu.clickMenuButton(p.player, 256*2); //what typing 256 into the speed field sends
				p.player.containerMenu.clickMenuButton(p.player, 2*2+1); //and 2 into the torque field
				p.closeMenu();
				p.placeSneaking(pos.above(), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST); //sneaking: a plain click opens the coil's GUI
				released[0] = true;
				h.fail("released");
			}
			//the small charge from a gas engine runs out within a few ticks: check what came out while it lasted
			h.assertTrue(peak[0] == 256 && peak[1] > 0 && peak[1] <= 2, "coil gave at most "+peak[0]+" rad/s @ "+peak[1]+" Nm");
			Factory.assertBuilt(h, BLOCKS.subList(2, 4), new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> CVT. The player pours in a lubricant bucket, puts 7 belts and the required belt in through
	 * its GUI and sets the ratio to 8 (speed): 4096 rad/s at 16 Nm come out.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void cvtSpeedsUp(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		net.minecraft.world.item.Item belt = RotaryItems.part(reika.rotarycraft.content.item.CraftingPart.BELT);
		net.minecraft.world.item.Item lube = RotaryFluids.LUBRICANT.get().getBucket();
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.CVT.get()), new ItemStack(Items.DIRT), new ItemStack(belt), new ItemStack(lube));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.CVT.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, belt, 8);
		Factory.takeExactly(p, chest, lube, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.CVT.get()), Direction.EAST);
		p.use(pos, lube);
		h.assertTrue(p.count(Items.BUCKET) == 1, "bucket not emptied");
		h.assertTrue(p.open(pos) != null, "CVT opens no GUI");
		for (int i = 0; i < 7; i++)
			p.putInto(i, belt, 1);
		p.putInto(31, belt, 1);
		p.player.containerMenu.clickMenuButton(p.player, 10+8); //the "+" button up to 8:1
		p.closeMenu();
		AdvancedGearBlockEntity te = (AdvancedGearBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.getMaxRatio() == 8 && te.getCvtRatio() == 8 && te.getLubricant() == 1000, "max "+te.getMaxRatio()+", ratio "+te.getCvtRatio()+", lube "+te.getLubricant());
			h.assertTrue(te.getOmega() == 4096 && te.getTorque() == 16, "CVT out "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			Factory.assertBuilt(h, BLOCKS.subList(4, 5), new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
