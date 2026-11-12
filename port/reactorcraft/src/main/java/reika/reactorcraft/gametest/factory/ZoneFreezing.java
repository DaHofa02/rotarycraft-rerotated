/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft crystallizer built and run by a simulated survival player. Not part of the
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
import reika.rotarycraft.content.blockentity.CrystallizerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFreezing {

	public static final List<String> BLOCKS = List.of("rotarycraft:crystallizer");

	/**
	 * Lava barrel over a pipe -> crystallizer, driven by a wind engine east of it (1024 rad/s @ 8 Nm = 8192 W; the crystallizer needs
	 * 1024 rad/s and 2048 W). Lava freezes at room temperature: every 400 ticks 1000 mB become a stone, which the player takes out.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 3000, batch = "factory")
	public static void crystallizerFreezesLava(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(1, 2, 1);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.CRYSTALLIZER.get()), new ItemStack(RotaryBlocks.WIND_ENGINE.get()),
				new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.DIRT));
		Factory.take(p, chest, RotaryBlocks.CRYSTALLIZER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.WIND_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 3);

		//on the big platform: the wind engine needs about 32 clear blocks downwind (east) for its full 1024 rad/s
		BlockPos pos = new BlockPos(4, 3, 24), wind = new BlockPos(5, 3, 24), pipe = new BlockPos(3, 3, 24);
		for (BlockPos b : new BlockPos[] {pipe, pos, wind})
			p.place(b.below(), Items.DIRT, Direction.EAST);
		p.place(wind, RotaryBlocks.WIND_ENGINE.get(), Direction.EAST); //looks east: drives west, its rotor is east of it
		p.place(pos, RotaryBlocks.CRYSTALLIZER.get(), Direction.EAST); //looking east: takes its power from the east
		p.place(pipe, RotaryBlocks.PIPE.get(), Direction.EAST);
		Factory.fluidBarrel(h, pipe.above(), Fluids.LAVA); //a reservoir drains downwards only
		CrystallizerBlockEntity te = (CrystallizerBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.omega == 1024 && te.getPower() >= 2048, "crystallizer at "+te.omega+" rad/s, "+te.getPower()+" W");
			h.assertTrue(te.getTank().getFluid().is(Fluids.LAVA), "no lava in the crystallizer");
			ItemStack out = te.getInventory().getStackInSlot(0);
			h.assertTrue(out.is(Items.STONE), "no stone yet, progress "+te.freezeTick+" at "+te.getTemperature()+" deg C");
			p.open(pos);
			p.shiftClick(0);
			p.closeMenu();
			h.assertTrue(p.count(Items.STONE) >= 1 && te.getInventory().getStackInSlot(0).isEmpty(), "player has "+p.count(Items.STONE)+" stone");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(47, 5, 47));
			p.leave();
		});
	}
}
