/*******************************************************************************
 * Practice factory zone (see Factory): ReactorCraft waste handling, set up by a simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.PipeBlock;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.item.NuclearWasteItem;
import reika.reactorcraft.radiation.Isotopes;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.waste.WasteContainerBlockEntity;
import reika.reactorcraft.waste.WasteDecayerBlockEntity;
import reika.reactorcraft.waste.WasteStorageBlockEntity;
import reika.reactorcraft.waste.WasteUnitBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneWaste {

	public static final List<String> BLOCKS = List.of("reactorcraft:waste_container", "reactorcraft:waste_storage", "reactorcraft:waste_decayer",
			"reactorcraft:waste_pipe");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * The player puts ten short-lived waste items (Cs-134) into a waste container through its GUI - their decay heat warms it; sixteen
	 * long-lived ones (Cs-137) into a storage drum, which keeps them; one Cs-137 into the top slot of a waste decayer (it needs neutrons to
	 * work, so only the loading is checked); and lays two waste pipes, which join.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void wasteIsStoredAndHeats(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack shortLived = NuclearWasteItem.of(Isotopes.Cs134), longLived = NuclearWasteItem.of(Isotopes.Cs137);
		Factory.infiniteChest(h, chest, new ItemStack(ReactorBlocks.WASTE_CONTAINER.get()), new ItemStack(ReactorBlocks.WASTE_STORAGE.get()),
				new ItemStack(ReactorBlocks.WASTE_DECAYER.get()), new ItemStack(ReactorBlocks.WASTE_PIPE.get()), shortLived, longLived);
		for (var b : List.of(ReactorBlocks.WASTE_CONTAINER, ReactorBlocks.WASTE_STORAGE, ReactorBlocks.WASTE_DECAYER))
			Factory.take(p, chest, b.get().asItem(), 1);
		Factory.take(p, chest, ReactorBlocks.WASTE_PIPE.get().asItem(), 2);
		Factory.take(p, chest, shortLived, 10);
		Factory.take(p, chest, longLived, 17);
		BlockPos cont = new BlockPos(2, 2, 4), stor = new BlockPos(4, 2, 4), dec = new BlockPos(6, 2, 4);
		p.place(cont, ReactorBlocks.WASTE_CONTAINER.get(), Direction.NORTH);
		p.place(stor, ReactorBlocks.WASTE_STORAGE.get(), Direction.NORTH);
		p.place(dec, ReactorBlocks.WASTE_DECAYER.get(), Direction.NORTH);
		p.place(new BlockPos(3, 2, 7), ReactorBlocks.WASTE_PIPE.get(), Direction.NORTH);
		p.place(new BlockPos(4, 2, 7), ReactorBlocks.WASTE_PIPE.get(), Direction.NORTH);
		p.open(cont);
		for (int i = 0; i < 10; i++)
			Factory.putExactly(p, i, shortLived, 1);
		p.closeMenu();
		p.open(stor);
		Factory.putExactly(p, 0, longLived, 16); //right-clicked in one by one: topping up the slot by hand
		p.closeMenu();
		p.open(dec);
		Factory.putExactly(p, 14, longLived, 1);
		p.closeMenu();
		WasteContainerBlockEntity c = (WasteContainerBlockEntity)h.getBlockEntity(cont);
		WasteStorageBlockEntity s = (WasteStorageBlockEntity)h.getBlockEntity(stor);
		WasteDecayerBlockEntity d = (WasteDecayerBlockEntity)h.getBlockEntity(dec);
		int[] start = {Integer.MIN_VALUE};
		h.runAtTickTime(21, () -> start[0] = c.getTemperature());
		h.succeedWhen(() -> {
			h.assertTrue(start[0] != Integer.MIN_VALUE && c.getTemperature() >= start[0]+20, "container warmed from "+start[0]+" to "+c.getTemperature()+" C; holds "+contents(c));
			int stored = 0; //the drum slides its items to the last slots (original collapseInventory)
			for (int i = 0; i < s.getInventory().getSlots(); i++)
				if (WasteUnitBlockEntity.getIsotope(s.getInventory().getStackInSlot(i)) == Isotopes.Cs137)
					stored += s.getInventory().getStackInSlot(i).getCount();
			h.assertTrue(stored == 16, "storage holds "+stored+" Cs-137");
			h.assertTrue(WasteUnitBlockEntity.getIsotope(d.getInventory().getStackInSlot(14)) == Isotopes.Cs137, "decayer holds "+d.getInventory().getStackInSlot(14));
			h.assertTrue(h.getBlockState(new BlockPos(3, 2, 7)).getValue(PipeBlock.EAST), "waste pipes not joined");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 2, 8));
			p.leave();
		});
	}

	private static String contents(WasteContainerBlockEntity c) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < c.getInventory().getSlots(); i++)
			if (!c.getInventory().getStackInSlot(i).isEmpty())
				sb.append(i).append(':').append(WasteUnitBlockEntity.getIsotope(c.getInventory().getStackInSlot(i))).append('x').append(c.getInventory().getStackInSlot(i).getCount()).append(' ');
		return sb.toString();
	}
}
