/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Production.TileEntityPump. Stands on a pool and pumps its source blocks one by one (the
 * connected fluid in the two layers below, up to 16 blocks out, farthest first) into a 24000 mB tank; min. 8 Nm / 1024 W, powered from
 * either long side (the stronger one counts); time max(1, 300 - 30*log2(omega+1)). Water is multiplied with power (x2 at 16x, 64x,
 * 256x, 1024x and 4096x the minimum). Living things in the pool below take 5 damage per tick; after 400 such ticks the impeller breaks
 * and the pump stops. The tank empties into liquid pipes on the four sides; pipes only take more while they hold less than the
 * pump's back pressure (1000 * (1 + log2(torque/8)) mB).
 */
public class PumpBlockEntity extends PowerReceiverBlockEntity implements PipeConnector {

	public static final int CAPACITY = 24*1000;
	/** Original DurationRegistry.PUMP. */
	private static final int DURATION_BASE = 300, DURATION_SCALE = 30;

	private final List<BlockPos> blocks = new ArrayList<>();
	private int soundtick = 200;
	private int damage = 0;

	private final FluidTank tank = new FluidTank(CAPACITY);

	public PumpBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.PUMP.get(), pos, state, 8, 1, 1024);
	}

	@Override
	public String getName() {
		return "Pump";
	}

	/** Original getIOSides: meta 1 reads east and west, meta 0 north and south. */
	public void getIOSides(int metadata) {
		switch (metadata) {
			case 1 -> {
				read = Direction.EAST;
				read2 = Direction.WEST;
			}
			case 0 -> {
				read = Direction.NORTH;
				read2 = Direction.SOUTH;
			}
		}
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		soundtick++;
		tickcount++;
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPowerDoubleSided();
		power = (long)omega*(long)torque;
		FluidState below = world.getFluidState(pos.below());
		if (below.isEmpty())
			return;
		Fluid f = source(below.getType());
		if (blocks.isEmpty()) {
			Set<BlockPos> seen = new HashSet<>();
			this.addLiquid(world, pos.below(), pos.getX()-16, pos.getY()-2, pos.getZ()-16, pos.getX()+16, pos.getY()-1, pos.getZ()+16, f, seen);
			Collections.reverse(blocks);
		}
		if (damage > 400)
			power = 0;
		if (blocks.isEmpty())
			return;
		if (power >= MINPOWER && torque >= MINTORQUE && tank.getFluidAmount() < CAPACITY && tickcount >= this.getOperationTime()) {
			BlockPos loc = blocks.remove(0);
			this.harvest(world, loc);
			tickcount = 0;
		}
		if (power > MINPOWER && torque >= MINTORQUE && soundtick >= 100) {
			soundtick = 0;
			RotarySounds.play(world, pos, RotarySounds.PUMP.get(), 0.5F, 1);
		}
		if (power > MINPOWER && torque >= MINTORQUE)
			this.suckUpMobs(world, pos);
	}

	private static Fluid source(Fluid f) {
		return f instanceof FlowingFluid ff ? ff.getSource() : f;
	}

	/** Original BlockArray.recursiveAddLiquidWithBounds: connected blocks of the fluid (source or flowing) inside the box. */
	private void addLiquid(Level world, BlockPos p, int x1, int y1, int z1, int x2, int y2, int z2, Fluid liquid, Set<BlockPos> seen) {
		if (p.getX() < x1 || p.getY() < y1 || p.getZ() < z1 || p.getX() > x2 || p.getY() > y2 || p.getZ() > z2)
			return;
		FluidState fs = world.getFluidState(p);
		if (fs.isEmpty() || !source(fs.getType()).isSame(liquid))
			return;
		if (!seen.add(p))
			return;
		blocks.add(p);
		this.addLiquid(world, p.east(), x1, y1, z1, x2, y2, z2, liquid, seen);
		this.addLiquid(world, p.west(), x1, y1, z1, x2, y2, z2, liquid, seen);
		this.addLiquid(world, p.above(), x1, y1, z1, x2, y2, z2, liquid, seen);
		this.addLiquid(world, p.below(), x1, y1, z1, x2, y2, z2, liquid, seen);
		this.addLiquid(world, p.south(), x1, y1, z1, x2, y2, z2, liquid, seen);
		this.addLiquid(world, p.north(), x1, y1, z1, x2, y2, z2, liquid, seen);
	}

	private void suckUpMobs(Level world, BlockPos pos) {
		AABB box = new AABB(pos.below());
		List<LivingEntity> inbox = world.getEntitiesOfClass(LivingEntity.class, box);
		boolean alive = false;
		for (LivingEntity e : inbox) {
			e.hurt(world.damageSources().generic(), 5);
			alive |= e.isAlive();
		}
		if (!inbox.isEmpty() && alive)
			damage++;
		if (damage >= 400)
			this.breakPump(world, pos);
	}

	public boolean isBroken() {
		return damage >= 400;
	}

	private void breakPump(Level world, BlockPos pos) {
		world.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1F, 1F);
	}

	/** Original harvest: a source block becomes air, its 1000 mB (water multiplied with power) go into the tank. */
	public void harvest(Level world, BlockPos loc) {
		FluidState fs = world.getFluidState(loc);
		if (fs.isEmpty() || !fs.isSource())
			return;
		Fluid f = fs.getType();
		FluidStack in = new FluidStack(f, 1000);
		if (tank.fill(in, IFluidHandler.FluidAction.SIMULATE) < 1000)
			return;
		world.setBlock(loc, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		int mult = 1;
		if (f.isSame(Fluids.WATER)) {
			long r = power/MINPOWER;
			for (long t : new long[] {16, 64, 256, 1024, 4096})
				if (r >= t)
					mult *= 2;
		}
		tank.fill(new FluidStack(f, 1000*mult), IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
	}

	public int getOperationTime() {
		return (int)Math.max(1, DURATION_BASE-DURATION_SCALE*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	public int getMaxBackPressure() {
		return 1000*(1+ReikaMathLibrary.logbase2(torque/MINTORQUE));
	}

	public int getFluidLevel() {
		return tank.getFluidAmount();
	}

	public FluidStack getFluid() {
		return tank.getFluid();
	}

	@Override
	public boolean canConnectToPipe(PipeType m) {
		return m.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side.getAxis() != Direction.Axis.Y;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis() != Direction.Axis.Y ? Flow.OUTPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from != null && from.getAxis() == Direction.Axis.Y)
			return FluidStack.EMPTY;
		FluidStack fs = tank.drain(maxDrain, action);
		if (!fs.isEmpty() && action.execute())
			this.setChanged();
		return fs;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return tank.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return false;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return 0;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.isSameFluidSameComponents(resource, tank.getFluid()) ? PumpBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return PumpBlockEntity.this.drain(side, maxDrain, action);
			}
		};
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (power < MINPOWER || torque < MINTORQUE)
			return;
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public void onEMP() {}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("dmg", damage);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		damage = NBT.getInt("dmg");
	}
}
