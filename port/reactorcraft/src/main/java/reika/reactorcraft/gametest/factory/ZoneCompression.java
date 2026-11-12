/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft fluid compressor built and run by a simulated survival player. Not part of the
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

import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.FluidCompressorBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneCompression {

	public static final List<String> BLOCKS = List.of("rotarycraft:fluid_compressor");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> bedrock gearbox 8:1 (1024 Nm) -> bevel gears -> fluid compressor above them; a water
	 * barrel over a pipe feeds it from the side. It fills to 2500 mB; the player then mines it and the item keeps the water.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600, batch = "factory")
	public static void compressorFillsAndKeepsItsWater(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), gbx,
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.FLUID_COMPRESSOR.get()), new ItemStack(RotaryBlocks.PIPE.get()),
				new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.IRON_PICKAXE));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, gbx, 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.FLUID_COMPRESSOR.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 5);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		Factory.takeExactly(p, chest, Items.IRON_PICKAXE, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		p.place(new BlockPos(5, 2, 5), Items.DIRT, Direction.EAST);
		p.placeSneaking(new BlockPos(5, 3, 5), gbx, Direction.EAST);
		BlockPos pos = new BlockPos(4, 4, 5), pipe = new BlockPos(3, 4, 5);
		p.place(new BlockPos(4, 2, 5), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(4, 3, 5), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.FLUID_COMPRESSOR.get(), Direction.EAST);
		p.place(new BlockPos(3, 2, 5), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(3, 3, 5), Items.DIRT, Direction.EAST);
		p.place(pipe, RotaryBlocks.PIPE.get(), Direction.EAST);
		Factory.fluidBarrel(h, pipe.above(), Fluids.WATER); //a reservoir drains downwards only
		p.mine(new BlockPos(4, 3, 5), Items.IRON_SHOVEL);
		p.placeSneaking(new BlockPos(4, 3, 5), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		FluidCompressorBlockEntity te = (FluidCompressorBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.torque == 1024, "compressor at "+te.omega+" rad/s @ "+te.torque+" Nm");
			h.assertTrue(te.getTank().getFluid().is(Fluids.WATER) && te.getTank().getFluidAmount() == 2500, "compressor holds "+te.getTank().getFluidAmount()+" mB");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 5, 8));
			p.mine(pos, Items.IRON_PICKAXE);
			ItemStack item = ItemStack.EMPTY;
			for (int i = 0; i < p.player.getInventory().getContainerSize(); i++)
				if (p.player.getInventory().getItem(i).is(RotaryBlocks.FLUID_COMPRESSOR.get().asItem()))
					item = p.player.getInventory().getItem(i);
			SimpleFluidContent c = item.get(RotaryComponents.TANK_FLUID.get());
			h.assertTrue(c != null && c.getAmount() == 2500 && c.is(Fluids.WATER), "mined compressor item holds "+c);
			p.leave();
		});
	}
}
