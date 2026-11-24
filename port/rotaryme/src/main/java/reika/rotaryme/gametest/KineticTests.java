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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.storage.DriveBlockEntity;

import reika.rotarycraft.content.blockentity.IOMachineBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotaryme.BundledBusBlockEntity;
import reika.rotaryme.KineticCellItem;
import reika.rotaryme.KineticInputBlockEntity;
import reika.rotaryme.KineticKey;
import reika.rotaryme.KineticOutputBlockEntity;
import reika.rotaryme.KineticOutputMenu;
import reika.rotaryme.MEConfig;
import reika.rotaryme.RotaryME;

import java.util.HashSet;
import java.util.Set;

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
	 * Gas engine -> kinetic input (from the west) -> drive with a 1k kinetic cell: power/20 J a tick less 5 % go into the network until the
	 * cell is full at (1024-8)*8192 = 8323072 J (the test fills all but the last 100 kJ by hand); each acceleration card takes 1 % off the loss, a fifth card does not fit.
	 */
	/** With a redstone card the output only runs while powered (or, inverted, while unpowered); without the card redstone does nothing. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void redstoneCardSwitchesOutput(GameTestHelper h) {
		KineticOutputBlockEntity out = place(h, RotaryME.KINETIC_OUTPUT.get(), 4, 2, 4, 0, KineticOutputBlockEntity.class);
		h.assertTrue(out.getUpgrades().size() == KineticOutputBlockEntity.CARD_SLOTS, "card slots "+out.getUpgrades().size());
		h.assertTrue(!out.isStoppedByRedstone(), "stopped without a card");
		var left = out.getUpgrades().toItemHandler().insertItem(4, new ItemStack(RotaryME.redstoneCard()), false);
		h.assertTrue(left.isEmpty() && out.hasRedstoneCard(), "redstone card not accepted");
		h.assertTrue(out.isStoppedByRedstone(), "runs without a signal");
		h.setBlock(new BlockPos(4, 3, 4), Blocks.REDSTONE_BLOCK);
		h.assertTrue(!out.isStoppedByRedstone(), "stopped with a signal");
		out.toggleRedstoneMode();
		h.assertTrue(out.isStoppedByRedstone(), "inverted mode runs with a signal");
		h.succeed();
	}

	/** The screwdriver turns both kinetic blocks a quarter round each click and shows their in/out boxes; four clicks bring them back. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void screwdriverTurnsKineticBlocks(GameTestHelper h) {
		KineticOutputBlockEntity out = place(h, RotaryME.KINETIC_OUTPUT.get(), 2, 2, 2, 0, KineticOutputBlockEntity.class);
		KineticInputBlockEntity in = place(h, RotaryME.KINETIC_INPUT.get(), 6, 2, 6, 0, KineticInputBlockEntity.class);
		for (var te : new IOMachineBlockEntity[] {out, in}) {
			BlockPos abs = te.getBlockPos();
			var player = h.makeMockPlayer(GameType.SURVIVAL);
			var driver = new ItemStack(RotaryItems.SCREWDRIVER.get());
			player.setItemInHand(InteractionHand.MAIN_HAND, driver);
			Set<Direction> seen = new HashSet<>();
			for (int i = 0; i < 4; i++) {
				driver.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
				h.assertTrue(te.iotick > 0, te.getName()+" shows no in/out boxes");
				h.assertTrue(te.getBlockMetadata() == (i+1)%4, te.getName()+" facing "+te.getBlockMetadata()+" after "+(i+1)+" clicks");
				seen.add(te.getReadDirection());
			}
			h.assertTrue(seen.size() == 4, te.getName()+" input sides "+seen);
		}
		h.succeed();
	}

	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void inputFillsAKineticCell(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 0, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		KineticInputBlockEntity in = place(h, RotaryME.KINETIC_INPUT.get(), 1, 2, 4, 1, KineticInputBlockEntity.class);
		ItemStack cell = new ItemStack(RotaryME.CELLS[0].get());
		long cap = ((KineticCellItem)cell.getItem()).getCapacityJoules();
		network(h, new BlockPos(2, 2, 4), new BlockPos(2, 2, 5), cell);
		h.assertTrue(cap == 8323072, "1k capacity "+cap);
		h.assertTrue(Math.abs(in.getLoss()-0.05) < 1e-9, "loss "+in.getLoss());
		var cards = in.getUpgrades().toItemHandler();
		for (int i = 0; i < 4; i++)
			cards.insertItem(i, new ItemStack(RotaryME.speedCard()), false);
		h.assertTrue(in.getCards() == 4 && Math.abs(in.getLoss()-0.01) < 1e-9, "4 cards: "+in.getCards()+", loss "+in.getLoss());
		cards.extractItem(3, 1, false);
		h.assertTrue(Math.abs(in.getLoss()-0.02) < 1e-9, "3 cards: loss "+in.getLoss());
		long rest = 100000;
		boolean[] prefilled = {false};
		h.succeedWhen(() -> {
			IGrid g = grid(h, new BlockPos(1, 2, 4));
			if (!prefilled[0] && g != null && g.getStorageService().getInventory().insert(KineticKey.INSTANCE, cap-rest, Actionable.MODULATE, IActionSource.empty()) > 0)
				prefilled[0] = true;
			long stored = g == null ? 0 : g.getStorageService().getInventory().getAvailableStacks().get(KineticKey.INSTANCE);
			h.assertTrue(in.getPower() > 0 && in.isConnected(), "input "+in.getPower()+" W, connected "+in.isConnected());
			h.assertTrue(prefilled[0] && stored == cap && in.storedTotal <= rest && in.storedTotal > 0, "stored "+stored+" J, input put in "+in.storedTotal+" J");
		});
	}

	/**
	 * A drive with a 4k kinetic cell (520192 J put in, of its 33 MJ) and a 1k fluid cell: the output set to 1024 rad/s and 1048576 W (1024 Nm) needs
	 * lubricant first, then runs on the network's lubricant and energy (power/20/0.95 J a tick) until the energy is gone.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void outputRunsOnNetworkEnergyAndLubricant(GameTestHelper h) {
		BlockPos drive = new BlockPos(3, 2, 4);
		network(h, drive, new BlockPos(3, 2, 5), new ItemStack(RotaryME.CELLS[1].get()), ae2Item("fluid_storage_cell_1k"));
		KineticOutputBlockEntity out = place(h, RotaryME.KINETIC_OUTPUT.get(), 2, 2, 4, 0, KineticOutputBlockEntity.class);
		out.setOutput(1024, 1024);
		h.assertTrue(out.getSetTorque() == 1024, "torque "+out.getSetTorque());
		double perTick = 1048576/20D; //the output is lossless
		int[] stage = {0};
		long[] start = {0};
		h.succeedWhen(() -> {
			IGrid g = grid(h, new BlockPos(2, 2, 4));
			h.assertTrue(g != null && out.getGridNode(Direction.EAST).isActive(), "no network: "+state(out.getGridNode(Direction.EAST)));
			switch (stage[0]) {
				case 0 -> {
					long j = g.getStorageService().getInventory().insert(KineticKey.INSTANCE, 520192, Actionable.MODULATE, IActionSource.empty());
					h.assertTrue(j == 520192, "4k took "+j+" J");
					start[0] = j;
					stage[0] = 1;
					h.fail("filled");
				}
				case 1 -> {
					h.assertTrue(out.problem == 3 && out.getOmega() == 0, "without lubricant: problem "+out.problem);
					h.assertTrue(out.getNetworkLubricant() == 0 && !out.usesOwnTank(), "network lubricant "+out.getNetworkLubricant());
					g.getStorageService().getInventory().insert(AEFluidKey.of(RotaryFluids.LUBRICANT.get()), 8000, Actionable.MODULATE, IActionSource.empty());
					stage[0] = 2;
					h.fail("lubricant");
				}
				case 2 -> {
					h.assertTrue(out.getNetworkLubricant() > 0 && out.getNetworkLubricant() <= 8000, "network lubricant "+out.getNetworkLubricant());
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

	/** The output's own tank goes first: with a lubricant bucket in it, nothing is taken from the network; 4 acceleration cards quarter the use. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void outputUsesItsOwnTankFirst(GameTestHelper h) {
		BlockPos drive = new BlockPos(3, 2, 4);
		network(h, drive, new BlockPos(3, 2, 5), new ItemStack(RotaryME.CELLS[2].get()));
		KineticOutputBlockEntity out = place(h, RotaryME.KINETIC_OUTPUT.get(), 2, 2, 4, 0, KineticOutputBlockEntity.class);
		out.addLubricant(1000);
		out.setOutput(512, 64);
		double base = out.getLubricantPerTick();
		var cards = out.getUpgrades().toItemHandler();
		for (int i = 0; i < 4; i++)
			cards.insertItem(i, new ItemStack(RotaryME.speedCard()), false);
		h.assertTrue(Math.abs(out.getLubricantPerTick()*4-base) < 1e-12 && out.getEnergyPerTick() == 512*64/20D, "4 cards: "+out.getLubricantPerTick()+" of "+base+" mB/t");
		boolean[] filled = {false};
		h.succeedWhen(() -> {
			IGrid g = grid(h, new BlockPos(2, 2, 4));
			h.assertTrue(g != null && out.getGridNode(Direction.EAST).isActive(), "no network: "+state(out.getGridNode(Direction.EAST)));
			if (!filled[0]) {
				g.getStorageService().getInventory().insert(KineticKey.INSTANCE, 1000000, Actionable.MODULATE, IActionSource.empty());
				filled[0] = true;
				h.fail("filled");
			}
			h.assertTrue(out.getOmega() == 512 && out.getLubricant() < 1000 && out.lubeFromNetwork == 0 && out.usesOwnTank(), "running "+out.getOmega()+", tank "+out.getLubricant());
		});
	}

	/**
	 * Gas engine -> bundled bus (power from the west, chest to the east) beside a drive with an item cell holding 200 cobblestone: with
	 * cobblestone in the white filter it moves nothing while white is off, then a stack per 10 ticks once white is on (Project Red's
	 * bundled input is replaced by the test hook here).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void bundledBusExportsOnSignal(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 0, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		BundledBusBlockEntity bus = place(h, RotaryME.BUNDLED_BUS.get(), 1, 2, 4, 1, BundledBusBlockEntity.class);
		h.setBlock(new BlockPos(2, 2, 4), Blocks.CHEST);
		network(h, new BlockPos(1, 2, 3), new BlockPos(1, 3, 4), ae2Item("item_storage_cell_1k")); //energy cell on the bus: a set drive faces north with its front
		bus.getFilters().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
		BundledBusBlockEntity.testInput = 0;
		int[] stage = {0};
		h.succeedWhen(() -> {
			IGrid g = grid(h, new BlockPos(1, 2, 4));
			h.assertTrue(g != null && bus.getGridNode(Direction.UP).isActive() && bus.getActiveChannels() == 16, "bus: "+state(bus.getGridNode(Direction.UP))+", channels "+bus.getActiveChannels());
			var chest = (ChestBlockEntity)h.getBlockEntity(new BlockPos(2, 2, 4));
			switch (stage[0]) {
				case 0 -> {
					long in = g.getStorageService().getInventory().insert(AEItemKey.of(Items.COBBLESTONE), 200, Actionable.MODULATE, IActionSource.empty());
					h.assertTrue(in == 200, "cell took "+in);
					stage[0] = 1;
					h.fail("stocked");
				}
				case 1 -> {
					h.assertTrue(bus.moved == 0 && chest.isEmpty(), "moved without a signal");
					if (h.getTick() < 60)
						h.fail("waiting");
					BundledBusBlockEntity.testInput = 1;
					stage[0] = 2;
					h.fail("signal");
				}
				default -> {
					h.assertTrue(bus.moved == 200, "moved "+bus.moved);
					BundledBusBlockEntity.testInput = null;
				}
			}
		});
	}
}
