/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.entity.player.Player;

import reika.reactorcraft.powergen.SteamInjectorBlockEntity;
import reika.reactorcraft.powergen.TurbineCoreBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;

/**
 * Original: Reika.ReactorCraft.Blocks.Multi.BlockTurbineMulti with DragonAPI BlockMultiBlock and SlicedBlockBlueprint - the high pressure
 * turbine's multiblock: turbine blades ('t'), housing ('h') and steam injectors ('e', with the lubricant tank). Placing blades or housing
 * checks the structure in the direction the player looks: from the exhaust end, up to 7 high pressure turbine cores in a row, and around
 * them the slices of the original blueprint (the largest, 11x11, for the 7th stage down to the 5x5 injector ring at the inlet; 'b'
 * places must not be multiblock parts, 'x' are free). When it matches, every connected part becomes "formed" (invisible, the turbine
 * draws the housing; 4 times as hard) and the cores may run; breaking any part un-forms the whole connected structure (cores above
 * 2048 rad/s then fly apart).
 */
public class TurbineMultiBlock extends BaseEntityBlock {

	public static final BooleanProperty FORMED = BooleanProperty.create("formed");

	/** 0 blades, 1 housing, 2 steam injector (original metadata). */
	public final int variant;
	private final MapCodec<TurbineMultiBlock> codec;

	public static final String[][] SLICES = {
		{"xbbhhhhhbbx", "bbhhhthhhbb", "bhhttttthhb", "hhttttttthh", "hhttttttthh", "httttxtttth", "hhttttttthh", "hhttttttthh", "bhhttttthhb", "bbhhhthhhbb", "xbbhhhhhbbx"},
		{"xxbbbbbbbxx", "xbbhhhhhbbx", "bbhhttthhbb", "bhhttttthhb", "bhttttttthb", "bhtttxttthb", "bhttttttthb", "bhhttttthhb", "bbhhttthhbb", "xbbhhhhhbbx", "xxbbbbbbbxx"},
		{"xxxbbbbbxxx", "xbbbhhhbbbx", "xbhhhhhhhbx", "bbhhttthhbb", "bhhttttthhb", "bhhttxtthhb", "bhhttttthhb", "bbhhttthhbb", "xbhhhhhhhbx", "xbbbhhhbbbx", "xxxbbbbbxxx"},
		{"xxxxxxxxxxx", "xxbbbbbbbxx", "xbbhhhhhbbx", "xbhhttthhbx", "xbhttttthbx", "xbhttxtthbx", "xbhttttthbx", "xbhhttthhbx", "xbbhhhhhbbx", "xxbbbbbbbxx", "xxxxxxxxxxx"},
		{"xxxxxxxxxxx", "xxxbbbbbxxx", "xxbbhhhbbxx", "xbbhhhhhbbx", "xbhhttthhbx", "xbhhtxthhbx", "xbhhttthhbx", "xbbhhhhhbbx", "xxbbhhhbbxx", "xxxbbbbbxxx", "xxxxxxxxxxx"},
		{"xxxxxxxxxxx", "xxxxxxxxxxx", "xxbbbbbbbxx", "xxbhhhhhbxx", "xxbhhthhbxx", "xxbhtxthbxx", "xxbhhthhbxx", "xxbhhhhhbxx", "xxbbbbbbbxx", "xxxxxxxxxxx", "xxxxxxxxxxx"},
		{"xxxxxxxxxxx", "xxxxxxxxxxx", "xxxbbbbbxxx", "xxbbhhhbbxx", "xxbhhhhhbxx", "xxbhhxhhbxx", "xxbhhhhhbxx", "xxbbhhhbbxx", "xxxbbbbbxxx", "xxxxxxxxxxx", "xxxxxxxxxxx"},
		{"xxxxxxxxxxx", "xxxxxxxxxxx", "xxxxxxxxxxx", "xxxbbbbbxxx", "xxxbeeebxxx", "xxxbexebxxx", "xxxbeeebxxx", "xxxbbbbbxxx", "xxxxxxxxxxx", "xxxxxxxxxxx", "xxxxxxxxxxx"},
	};

	public TurbineMultiBlock(Properties props, int variant) {
		super(props);
		this.variant = variant;
		codec = simpleCodec(p -> new TurbineMultiBlock(p, variant));
		this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return codec;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return variant == 2 ? new SteamInjectorBlockEntity(pos, state) : null;
	}

	/** Original getBlockHardness: 4 times as hard while formed. */
	@Override
	protected float getDestroyProgress(BlockState state, Player ep, BlockGetter world, BlockPos pos) {
		float f = super.getDestroyProgress(state, ep, world, pos);
		return state.getValue(FORMED) ? f/4 : f;
	}

	public static boolean isPart(BlockState s) {
		return s.getBlock() instanceof TurbineMultiBlock;
	}

	private static boolean isPartOrCore(Level world, BlockPos p) {
		return isPart(world.getBlockState(p)) || world.getBlockEntity(p) instanceof reika.reactorcraft.powergen.HiPTurbineBlockEntity;
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		if (!world.isClientSide && variant <= 1 && placer instanceof Player) {
			Direction dir = placer.getDirection();
			if (checkForFullMultiBlock(world, pos, dir))
				onCreateFullMultiBlock(world, pos);
		}
	}

	/** Original BlockMultiBlock.breakBlock: removing a part un-forms the structures beside it. */
	@Override
	protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		if (!world.isClientSide && !state.is(newState.getBlock())) {
			for (Direction d : Direction.values())
				breakMultiBlock(world, pos.relative(d));
		}
		super.onRemove(state, world, pos, newState, moved);
	}

	/** Original StructuredBlockArray.recursiveAddWithBounds: the connected parts (6-neighbourhood, within 12 blocks). */
	private static Set<BlockPos> connected(Level world, BlockPos start, boolean withCores) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		if (!(withCores ? isPartOrCore(world, start) : isPart(world.getBlockState(start))))
			return seen;
		seen.add(start);
		queue.add(start);
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (Math.abs(n.getX()-start.getX()) > 12 || Math.abs(n.getY()-start.getY()) > 12 || Math.abs(n.getZ()-start.getZ()) > 12)
					continue;
				if (!seen.contains(n) && (withCores ? isPartOrCore(world, n) : isPart(world.getBlockState(n)))) {
					seen.add(n);
					queue.add(n);
				}
			}
		}
		return seen;
	}

	public static boolean checkForFullMultiBlock(Level world, BlockPos pos, Direction dir) {
		Set<BlockPos> blocks = connected(world, pos, false);
		if (blocks.isEmpty())
			return false;
		int minX = blocks.stream().mapToInt(BlockPos::getX).min().getAsInt(), maxX = blocks.stream().mapToInt(BlockPos::getX).max().getAsInt();
		int minY = blocks.stream().mapToInt(BlockPos::getY).min().getAsInt(), maxY = blocks.stream().mapToInt(BlockPos::getY).max().getAsInt();
		int minZ = blocks.stream().mapToInt(BlockPos::getZ).min().getAsInt(), maxZ = blocks.stream().mapToInt(BlockPos::getZ).max().getAsInt();
		int mx = minX+(maxX-minX+1)/2, my = minY+(maxY-minY+1)/2, mz = minZ+(maxZ-minZ+1)/2;
		int sx = dir.getStepX() == 0 ? mx : dir.getStepX() < 0 ? maxX : minX;
		int sz = dir.getStepZ() == 0 ? mz : dir.getStepZ() < 0 ? maxZ : minZ;
		int n = 0;
		for (int i = 0; i < SLICES.length; i++) {
			BlockPos p = new BlockPos(sx+i*dir.getStepX(), my, sz+i*dir.getStepZ());
			if (world.getBlockEntity(p) instanceof TurbineCoreBlockEntity tc && tc.needsMultiblock()) {
				n++;
				tc.markForMulti();
			}
			else
				break;
		}
		if (n <= 0 || n > 7)
			return false;
		int start = SLICES.length-n-1;
		for (int i = start; i < SLICES.length; i++) {
			int d = i-start;
			if (!checkSlice(world, sx+d*dir.getStepX(), my, sz+d*dir.getStepZ(), dir, SLICES[i]))
				return false;
		}
		return true;
	}

	/** Original SlicedBlockBlueprint.checkAgainst (reference 5, 5): rows go down from y+5, columns along the slice plane. */
	private static boolean checkSlice(Level world, int x, int y, int z, Direction plane, String[] slice) {
		for (int i = 0; i < slice[0].length(); i++) {
			for (int k = 0; k < slice.length; k++) {
				int dx = plane.getStepX() == 0 ? x-5+i : x;
				int dz = plane.getStepZ() == 0 ? z-5+i : z;
				BlockState s = world.getBlockState(new BlockPos(dx, y+5-k, dz));
				char c = slice[k].charAt(i);
				switch (c) {
					case 'x':
						break;
					case 'b':
						if (isPart(s))
							return false;
						break;
					default:
						int want = c == 't' ? 0 : c == 'h' ? 1 : 2;
						if (!(s.getBlock() instanceof TurbineMultiBlock tm) || tm.variant != want)
							return false;
				}
			}
		}
		return true;
	}

	public static void onCreateFullMultiBlock(Level world, BlockPos pos) {
		for (BlockPos c : connected(world, pos, true)) {
			BlockState s = world.getBlockState(c);
			if (isPart(s)) {
				if (!s.getValue(FORMED))
					world.setBlock(c, s.setValue(FORMED, true), 3);
			}
			else if (world.getBlockEntity(c) instanceof TurbineCoreBlockEntity te) {
				te.setHasMultiBlock(true);
			}
		}
	}

	public static void breakMultiBlock(Level world, BlockPos pos) {
		for (BlockPos c : connected(world, pos, true)) {
			BlockState s = world.getBlockState(c);
			if (isPart(s)) {
				if (s.getValue(FORMED))
					world.setBlock(c, s.setValue(FORMED, false), 3);
			}
			else if (world.getBlockEntity(c) instanceof TurbineCoreBlockEntity te) {
				te.setHasMultiBlock(false);
			}
		}
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		return List.of(new ItemStack(this));
	}
}
