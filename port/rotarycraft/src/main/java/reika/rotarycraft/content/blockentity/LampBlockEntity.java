/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.World.TileEntityLamp - runs on a wound coil (one charge per 120 ticks times the spring's
 * stiffness) and, without a redstone signal, fills the air around it with invisible light blocks of level 15: up to 12 blocks along the
 * six axes and every second block (2, 4, ... up to 0.8 * 12) along the twelve diagonals. With a signal, or once the coil has run down,
 * the lights go out again; the coil does not unwind while there is a signal.
 * Changes: the original's light block is vanilla's {@code minecraft:light} (level 15). Light blocks already standing count as free
 * spots when the lamp collects them (the original took air only, so lights left from before a reload stayed forever), and breaking the
 * lamp removes only its light blocks (the original set every collected spot to air, even if something had been built there since).
 */
public class LampBlockEntity extends SpringPoweredBlockEntity {

	public static final int MAXRANGE = 12;

	private final List<BlockPos> light = new ArrayList<>();
	private boolean canlight;

	public LampBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.LAMP.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Lamp";
	}

	@Override
	public int getBaseDischargeTime() {
		return 120;
	}

	public int getRange() {
		return MAXRANGE;
	}

	public boolean isLit() {
		return canlight;
	}

	/** The spots the lamp lights (collected the first time it lights up). */
	public List<BlockPos> getLightSpots() {
		return light;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		boolean red = world.hasNeighborSignal(pos);
		if (!red)
			this.updateCoil();
		if (world.isClientSide)
			return;
		if (red)
			canlight = false;
		if (!canlight)
			this.goDark(world);
		else if (light.isEmpty())
			this.collect(world, pos);
		else {
			for (BlockPos c : light)
				if (world.getBlockState(c).isAir())
					world.setBlockAndUpdate(c, lightBlock());
		}
	}

	private static BlockState lightBlock() {
		return Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
	}

	private void collect(Level world, BlockPos pos) {
		int r0 = this.getRange();
		for (int i = 1; i <= r0; i++) {
			this.add(world, pos.offset(i, 0, 0));
			this.add(world, pos.offset(0, i, 0));
			this.add(world, pos.offset(0, 0, i));
			this.add(world, pos.offset(-i, 0, 0));
			this.add(world, pos.offset(0, -i, 0));
			this.add(world, pos.offset(0, 0, -i));
		}
		for (int r = 2; r <= r0*0.8; r += 2) {
			for (int dy = -r; dy <= r; dy += r) {
				this.add(world, pos.offset(r, dy, r));
				this.add(world, pos.offset(-r, dy, r));
				this.add(world, pos.offset(r, dy, -r));
				this.add(world, pos.offset(-r, dy, -r));
			}
		}
	}

	private void add(Level world, BlockPos c) {
		if (this.canEditAt(world, c))
			light.add(c);
	}

	public boolean canEditAt(Level world, BlockPos c) {
		BlockState s = world.getBlockState(c);
		return s.isAir() || s.is(Blocks.LIGHT);
	}

	private void goDark(Level world) {
		for (BlockPos c : light)
			if (world.getBlockState(c).is(Blocks.LIGHT))
				world.setBlockAndUpdate(c, Blocks.AIR.defaultBlockState());
	}

	private void updateCoil() {
		if (!this.hasCoil()) {
			canlight = false;
			return;
		}
		tickcount++;
		if (tickcount > this.getUnwindTime()) {
			this.decrementCharge();
			tickcount = 0;
		}
		canlight = true;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		super.onBlockBroken(world, pos);
		this.goDark(world);
	}
}
