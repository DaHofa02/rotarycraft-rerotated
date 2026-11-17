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

	public static final List<String> BLOCKS = List.of("rotarycraft:worm_gear", "rotarycraft:high_gear");

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
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
