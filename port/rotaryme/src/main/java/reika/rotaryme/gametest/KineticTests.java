/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.blockentity.storage.DriveBlockEntity;

import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotaryme.KineticCellItem;
import reika.rotaryme.KineticInputBlockEntity;
import reika.rotaryme.KineticKey;
import reika.rotaryme.KineticOutputBlockEntity;
import reika.rotaryme.KineticOutputMenu;
import reika.rotaryme.MEConfig;
import reika.rotaryme.RotaryME;

@GameTestHolder(RotaryME.MOD_ID)
@PrefixGameTestTemplate(false)
public class KineticTests {

	private static final String PLATFORM = "platform_9x8x9";

	static Block ae2(String id) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ae2", id));
	}

	static ItemStack ae2Item(String id) {
		return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ae2", id)));
	}

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** A drive with the given cells and a creative energy cell next to it (an ad-hoc network, no controller). */
	private static DriveBlockEntity network(GameTestHelper h, BlockPos drive, BlockPos energy, ItemStack... cells) {
		h.setBlock(drive, ae2("drive"));
		h.setBlock(energy, ae2("creative_energy_cell"));
		DriveBlockEntity d = (DriveBlockEntity)h.getBlockEntity(drive);
		for (int i = 0; i < cells.length; i++)
			d.getInternalInventory().setItemDirect(i, cells[i]);
		return d;
	}

	/** The grid of one of the addon's blocks (their node is the same on every side). */
	static IGrid grid(GameTestHelper h, BlockPos pos) {
		IGridNode n = GridHelper.getNodeHost(h.getLevel(), h.absolutePos(pos)).getGridNode(Direction.UP);
		return n == null ? null : n.getGrid();
	}

	static String state(IGridNode n) {
		return n == null ? "no node" : "powered "+n.isPowered()+", online "+n.isOnline()+", active "+n.isActive()+", grid nodes "+n.getGrid().size();
	}

	/**
	 * Gas engine -> kinetic input (from the west) -> drive with a 1k kinetic cell: power/20 J a tick go into the network until the cell is
	 * full at (1024-8)*128 = 130048 J.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void inputFillsAKineticCell(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 0, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		KineticInputBlockEntity in = place(h, RotaryME.KINETIC_INPUT.get(), 1, 2, 4, 1, KineticInputBlockEntity.class);
		ItemStack cell = new ItemStack(RotaryME.CELLS[0].get());
		long cap = ((KineticCellItem)cell.getItem()).getCapacityJoules();
		network(h, new BlockPos(2, 2, 4), new BlockPos(2, 2, 5), cell);
		h.assertTrue(cap == 130048, "1k capacity "+cap);
		h.succeedWhen(() -> {
			IGrid g = grid(h, new BlockPos(1, 2, 4));
			long stored = g == null ? 0 : g.getStorageService().getInventory().getAvailableStacks().get(KineticKey.INSTANCE);
			h.assertTrue(in.getPower() > 0 && in.isConnected(), "input "+in.getPower()+" W, connected "+in.isConnected());
			h.assertTrue(stored == cap && in.storedTotal == cap, "stored "+stored+" J, input put in "+in.storedTotal+" J");
		});
	}

	/**
	 * A drive with a 4k kinetic cell (filled to 520192 J) and a 1k fluid cell: the output set to 1024 rad/s and 1048576 W (1024 Nm) needs
	 * lubricant first, then runs on the network's lubricant and energy (power/20/0.95 J a tick) until the energy is gone.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void outputRunsOnNetworkEnergyAndLubricant(GameTestHelper h) {
		BlockPos drive = new BlockPos(3, 2, 4);
		network(h, drive, new BlockPos(3, 2, 5), new ItemStack(RotaryME.CELLS[1].get()), ae2Item("fluid_storage_cell_1k"));
		KineticOutputBlockEntity out = place(h, RotaryME.KINETIC_OUTPUT.get(), 2, 2, 4, 0, KineticOutputBlockEntity.class);
		out.setOutput(1024, 1048576);
		h.assertTrue(out.getSetTorque() == 1024, "torque "+out.getSetTorque());
		double perTick = 1048576/20D/(1-MEConfig.OUTPUT_LOSS.get());
		int[] stage = {0};
		long[] start = {0};
		h.succeedWhen(() -> {
			IGrid g = grid(h, new BlockPos(2, 2, 4));
			h.assertTrue(g != null && out.getGridNode(Direction.EAST).isActive(), "no network: "+state(out.getGridNode(Direction.EAST)));
			switch (stage[0]) {
				case 0 -> {
					long j = g.getStorageService().getInventory().insert(KineticKey.INSTANCE, Long.MAX_VALUE, Actionable.MODULATE, IActionSource.empty());
					h.assertTrue(j == 520192, "4k took "+j+" J");
					start[0] = j;
					stage[0] = 1;
					h.fail("filled");
				}
				case 1 -> {
					h.assertTrue(out.problem == 3 && out.getOmega() == 0, "without lubricant: problem "+out.problem);
					g.getStorageService().getInventory().insert(AEFluidKey.of(RotaryFluids.LUBRICANT.get()), 8000, Actionable.MODULATE, IActionSource.empty());
					stage[0] = 2;
					h.fail("lubricant");
				}
				case 2 -> {
					h.assertTrue(out.getOmega() == 1024 && out.getTorque() == 1024, "running: "+out.getOmega()+" rad/s "+out.getTorque()+" Nm, problem "+out.problem);
					stage[0] = 3;
					h.fail("running");
				}
				default -> {
					h.assertTrue(out.problem == 2 && out.getOmega() == 0, "out of energy: problem "+out.problem+", used "+out.usedTotal);
					long ticks = Math.round(out.usedTotal/perTick);
					h.assertTrue(out.usedTotal <= start[0] && start[0]-out.usedTotal < perTick+1, "used "+out.usedTotal+" of "+start[0]);
					h.assertTrue(out.lubeFromNetwork > 0 && out.lubeFromNetwork <= Math.ceil(ticks*1048576D/64000/20)+1, "lubricant "+out.lubeFromNetwork+" mB over "+ticks+" ticks");
				}
			}
		});
	}

	/** The output's own tank goes first: with a lubricant bucket in it, nothing is taken from the network. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void outputUsesItsOwnTankFirst(GameTestHelper h) {
		BlockPos drive = new BlockPos(3, 2, 4);
		network(h, drive, new BlockPos(3, 2, 5), new ItemStack(RotaryME.CELLS[2].get()));
		KineticOutputBlockEntity out = place(h, RotaryME.KINETIC_OUTPUT.get(), 2, 2, 4, 0, KineticOutputBlockEntity.class);
		out.addLubricant(1000);
		out.setOutput(512, 512*64);
		boolean[] filled = {false};
		h.succeedWhen(() -> {
			IGrid g = grid(h, new BlockPos(2, 2, 4));
			h.assertTrue(g != null && out.getGridNode(Direction.EAST).isActive(), "no network: "+state(out.getGridNode(Direction.EAST)));
			if (!filled[0]) {
				g.getStorageService().getInventory().insert(KineticKey.INSTANCE, 1000000, Actionable.MODULATE, IActionSource.empty());
				filled[0] = true;
				h.fail("filled");
			}
			h.assertTrue(out.getOmega() == 512 && out.getLubricant() < 1000 && out.lubeFromNetwork == 0, "running "+out.getOmega()+", tank "+out.getLubricant());
		});
	}
}
