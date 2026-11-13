/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft grindstone built and run by a simulated survival player. Not part of the
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
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.GrindstoneBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSharpening {

	public static final List<String> BLOCKS = List.of("rotarycraft:grindstone");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> bedrock gearbox 2:1 (256 Nm) -> grindstone; a water barrel over a pipe on top of the
	 * grindstone. The player puts a worn iron sword in through the GUI and takes it back mended.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 800, batch = "factory")
	public static void grindstoneMendsASword(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 2);
		ItemStack worn = new ItemStack(Items.IRON_SWORD);
		worn.setDamageValue(100);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), gbx,
				new ItemStack(RotaryBlocks.GRINDSTONE.get()), new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.DIRT), worn);
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, gbx, 1);
		Factory.take(p, chest, RotaryBlocks.GRINDSTONE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.takeExactly(p, chest, Items.IRON_SWORD, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		p.place(new BlockPos(5, 2, 5), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(4, 2, 5), Items.DIRT, Direction.EAST);
		p.placeSneaking(new BlockPos(5, 3, 5), gbx, Direction.EAST);
		BlockPos pos = new BlockPos(4, 3, 5);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.GRINDSTONE.get()), Direction.EAST); //looking east: east-west axle
		p.placeSneaking(pos.above(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		Factory.fluidBarrel(h, pos.above(2), Fluids.WATER);
		GrindstoneBlockEntity te = (GrindstoneBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getBlockMetadata() == 1, "grindstone meta "+te.getBlockMetadata());
		h.assertTrue(p.open(pos) != null, "grindstone opens no GUI");
		p.putInto(0, Items.IRON_SWORD, 1);
		p.closeMenu();

		h.succeedWhen(() -> {
			h.assertTrue(te.torque == 256, "grindstone at "+te.omega+" rad/s @ "+te.torque+" Nm");
			ItemStack is = te.getInventory().getStackInSlot(0);
			h.assertTrue(is.getDamageValue() <= 95, "sword at "+is.getDamageValue()+", "+te.getTank().getFluidAmount()+" mB water");
			p.open(pos);
			p.shiftClick(0);
			p.closeMenu();
			int dmg = -1;
			for (int i = 0; i < p.player.getInventory().getContainerSize(); i++)
				if (p.player.getInventory().getItem(i).is(Items.IRON_SWORD))
					dmg = p.player.getInventory().getItem(i).getDamageValue();
			h.assertTrue(dmg >= 0 && dmg <= 95, "player's sword at "+dmg);
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
