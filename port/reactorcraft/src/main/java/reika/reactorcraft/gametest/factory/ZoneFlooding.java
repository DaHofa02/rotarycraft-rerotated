/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft Liquid Spiller built and run by a simulated survival player. Not part of the
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.FlooderBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFlooding {

	public static final List<String> BLOCKS = List.of("rotarycraft:spiller");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * The player walls in a shaft two blocks deep with cobblestone, sets the spiller on top (on a dirt pillar that is mined away again),
	 * a DC engine with a redstone block beside it (256 rad/s @ 4 Nm = 1024 W, one block every (int)(22 - 2*log2(257)) = 5 ticks) and
	 * a pipe from the water barrel above. The spiller fills the shaft with two water sources and keeps the rest in its 4000 mB tank.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1200, batch = "factory")
	public static void spillerFloodsAShaft(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(Items.COBBLESTONE), new ItemStack(Items.DIRT), new ItemStack(RotaryBlocks.SPILLER.get()),
				new ItemStack(RotaryBlocks.DC_ENGINE.get()), new ItemStack(Items.REDSTONE_BLOCK), new ItemStack(RotaryBlocks.PIPE.get()));
		Factory.fluidBarrel(h, new BlockPos(4, 6, 4), Fluids.WATER);

		Factory.takeExactly(p, chest, Items.COBBLESTONE, 8);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.take(p, chest, RotaryBlocks.SPILLER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.DC_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, Items.REDSTONE_BLOCK, 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		BlockPos shaft = new BlockPos(4, 2, 4);
		for (int y = 2; y <= 3; y++) {
			for (Direction d : Direction.Plane.HORIZONTAL)
				p.place(shaft.atY(y).relative(d), Items.COBBLESTONE, Direction.EAST);
			p.place(shaft.atY(y), Items.DIRT, Direction.EAST);
		}
		BlockPos spiller = new BlockPos(4, 4, 4);
		p.place(spiller, RotaryBlocks.SPILLER.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(5, 4, 4), new ItemStack(RotaryBlocks.DC_ENGINE.get()), Direction.EAST); //meta 0: output west, into the spiller
		p.placeSneaking(new BlockPos(5, 5, 4), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST);
		p.placeSneaking(new BlockPos(4, 5, 4), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST); //between the barrel and the spiller
		p.mine(shaft.atY(3));
		p.mine(shaft);
		h.assertTrue(p.count(Items.DIRT) == 2, "dirt pillar not taken back");

		FlooderBlockEntity f = (FlooderBlockEntity)h.getBlockEntity(spiller);
		h.succeedWhen(() -> {
			h.assertTrue(f.getPower() == 1024 && f.getOperationTime() == 5, "spiller at "+f.getOmega()+" rad/s @ "+f.getTorque()+" Nm, time "+f.getOperationTime());
			for (int y = 2; y <= 3; y++) {
				FluidState fs = h.getBlockState(shaft.atY(y)).getFluidState();
				h.assertTrue(fs.isSource() && fs.getType().isSame(Fluids.WATER), "no water source at "+shaft.atY(y)+": "+h.getBlockState(shaft.atY(y)));
			}
			h.assertTrue(f.getFluidLevel() == FlooderBlockEntity.CAPACITY && f.getRemainingArea() == 0, "tank "+f.getFluidLevel()+" mB, "+f.getRemainingArea()+" blocks left");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 6, 8));
			p.leave();
		});
	}
}
