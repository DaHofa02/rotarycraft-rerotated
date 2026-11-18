/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotaryme.KineticInputBlockEntity;
import reika.rotaryme.KineticOutputBlockEntity;
import reika.rotaryme.KineticOutputMenu;
import reika.rotaryme.RotaryME;

/** Factory zone of the addon: a simulated survival player builds a kinetic storage loop. */
@GameTestHolder(RotaryME.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneKinetics {

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Gas engine (looking east: drives west) -> kinetic input -> drive (with a 16k kinetic cell put in through its GUI) + creative energy cell
	 * -> kinetic output north of the drive (looking east: drives west), filled with a lubricant bucket and set to 256 rad/s and 8192 W in its GUI: the stored
	 * energy comes back out as 256 rad/s at 32 Nm.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "kineticzone")
	public static void playerBuildsAKineticStore(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		p.give(new ItemStack(RotaryBlocks.GAS_ENGINE.get()));
		p.give(new ItemStack(RotaryME.KINETIC_INPUT.get()));
		p.give(new ItemStack(RotaryME.KINETIC_OUTPUT.get()));
		p.give(KineticTests.ae2Item("drive"));
		p.give(KineticTests.ae2Item("creative_energy_cell"));
		p.give(new ItemStack(RotaryME.CELLS[2].get()));
		p.give(new ItemStack(RotaryItems.LUBRICANT_BUCKET.get()));
		BlockPos engine = new BlockPos(6, 2, 4), in = new BlockPos(5, 2, 4), drive = new BlockPos(4, 2, 4), out = new BlockPos(4, 2, 3); //north of the drive: the drive's front (towards the player) takes no cable
		p.placeSneaking(engine, new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		((EngineBlockEntity)h.getBlockEntity(engine)).addFuel(50000); //what a fuel line would bring
		p.placeSneaking(in, new ItemStack(RotaryME.KINETIC_INPUT.get()), Direction.EAST); //input from the engine to the east
		p.placeSneaking(drive, KineticTests.ae2Item("drive"), Direction.EAST);
		p.placeSneaking(new BlockPos(4, 2, 5), KineticTests.ae2Item("creative_energy_cell"), Direction.EAST);
		p.placeSneaking(out, new ItemStack(RotaryME.KINETIC_OUTPUT.get()), Direction.EAST); //drives west
		h.assertTrue(p.open(drive) != null, "drive opens no GUI");
		p.putInto(0, RotaryME.CELLS[2].get(), 1);
		p.closeMenu();
		p.hold(RotaryItems.LUBRICANT_BUCKET.get());
		p.useOn(out, Direction.UP);
		h.assertTrue(p.count(Items.BUCKET) == 1, "bucket not emptied");
		KineticOutputMenu m = (KineticOutputMenu)p.open(out);
		m.tile.setOutput(256, 8192); //what the GUI's Set button sends
		p.closeMenu();
		KineticInputBlockEntity ie = (KineticInputBlockEntity)h.getBlockEntity(in);
		KineticOutputBlockEntity oe = (KineticOutputBlockEntity)h.getBlockEntity(out);
		h.succeedWhen(() -> {
			h.assertTrue(ie.storedTotal > 0, "input stored nothing: "+ie.getPower()+" W, connected "+ie.isConnected());
			h.assertTrue(oe.getOmega() == 256 && oe.getTorque() == 32, "output "+oe.getOmega()+" rad/s "+oe.getTorque()+" Nm, problem "+oe.problem+", tank "+oe.getLubricant()+", meta "+oe.getBlockMetadata()+", "+KineticTests.state(oe.getGridNode(Direction.EAST))+"; input meta "+ie.getBlockMetadata()+" "+KineticTests.state(ie.getGridNode(Direction.UP)));
			p.leave();
		});
	}

	/**
	 * Gas engine -> bundled bus -> chest, a drive (item cell put in through its GUI) and a creative energy cell beside the bus; the player
	 * puts cobblestone into the bus's white filter: while white is on (test hook for Project Red's cable), the stock goes into the chest.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "kineticzone")
	public static void playerBuildsABundledBus(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		p.give(new ItemStack(RotaryBlocks.GAS_ENGINE.get()));
		p.give(new ItemStack(RotaryME.BUNDLED_BUS.get()));
		p.give(new ItemStack(Items.CHEST));
		p.give(KineticTests.ae2Item("drive"));
		p.give(KineticTests.ae2Item("creative_energy_cell"));
		p.give(KineticTests.ae2Item("item_storage_cell_1k"));
		p.give(new ItemStack(Items.COBBLESTONE));
		BlockPos engine = new BlockPos(6, 2, 4), bus = new BlockPos(5, 2, 4), chest = new BlockPos(4, 2, 4), drive = new BlockPos(5, 2, 3);
		p.placeSneaking(engine, new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		((EngineBlockEntity)h.getBlockEntity(engine)).addFuel(50000); //what a fuel line would bring
		p.placeSneaking(bus, new ItemStack(RotaryME.BUNDLED_BUS.get()), Direction.EAST); //power from the east, exports west
		p.place(chest, Items.CHEST, Direction.EAST);
		p.placeSneaking(drive, KineticTests.ae2Item("drive"), Direction.EAST); //its front faces west, the bus is at its side
		p.placeSneaking(new BlockPos(5, 2, 2), KineticTests.ae2Item("creative_energy_cell"), Direction.EAST);
		h.assertTrue(p.open(drive) != null, "drive opens no GUI");
		p.putInto(0, KineticTests.ae2Item("item_storage_cell_1k").getItem(), 1);
		p.closeMenu();
		h.assertTrue(p.open(bus) != null, "bus opens no GUI");
		p.putInto(0, Items.COBBLESTONE, 1);
		p.closeMenu();
		reika.rotaryme.BundledBusBlockEntity be = (reika.rotaryme.BundledBusBlockEntity)h.getBlockEntity(bus);
		reika.rotaryme.BundledBusBlockEntity.testInput = 1;
		boolean[] stocked = {false};
		h.succeedWhen(() -> {
			var g = KineticTests.grid(h, bus);
			h.assertTrue(g != null && be.getGridNode(Direction.UP).isActive(), "bus not on the network: "+KineticTests.state(be.getGridNode(Direction.UP)));
			if (!stocked[0]) { //what an ME interface or terminal would put in
				g.getStorageService().getInventory().insert(appeng.api.stacks.AEItemKey.of(Items.COBBLESTONE), 100, appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.empty());
				stocked[0] = true;
				h.fail("stocked");
			}
			h.assertTrue(be.moved == 100, "moved "+be.moved+", filter "+be.getFilters().getStackInSlot(0)+", channels "+be.getActiveChannels());
			reika.rotaryme.BundledBusBlockEntity.testInput = null;
			p.leave();
		});
	}
}
