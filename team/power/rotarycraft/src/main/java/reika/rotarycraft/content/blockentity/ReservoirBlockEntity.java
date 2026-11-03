/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.lib.world.TemperatureEnvironment;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * Original: Reika.RotaryCraft.TileEntities.Storage.TileEntityReservoir - an open 64000 mB tank. Neighbouring reservoirs join walls and
 * level out (half the difference per flow step, same fluid only); an uncovered one collects 25 mB rain per tick, and gases lighter
 * than air evaporate from it. Unless walled in, the contents heat or cool the surroundings (radius 2, every second; above 2500 °C it
 * melts into lava) and flammable contents explode next to fire, lava or in a hot biome. Entities in an uncovered reservoir get the
 * fluid's effects. Pipes fill it from every side but the top and drain it from the bottom; buckets and glass bottles work by hand.
 * A glass pane covers it. The item keeps contents and cover.
 * Not ported: the creative variants, other mods' tank handlers and fluid effects, XP fluid extraction, ChromatiCraft rifts.
 */
public class ReservoirBlockEntity extends RotaryCraftBlockEntity implements PipeConnector {

	public static final int CAPACITY = 64000;

	public boolean isCovered = false;
	private final FluidTank tank = new FluidTank(CAPACITY);
	/** Original adjacent[10]: numpad layout of the eight horizontal neighbours (2 south, 4 west, 6 east, 8 north, corners 1/3/7/9). */
	private boolean[] adjacent = new boolean[10];
	private int flowTimer = 0;
	private int tempTimer = 0;

	public ReservoirBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.RESERVOIR.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Reservoir";
	}

	public int getLiquidScaled(int par1) {
		return (tank.getFluidAmount()*par1)/CAPACITY;
	}

	private boolean isReservoir(Level world, BlockPos p) {
		return world.getBlockEntity(p) instanceof ReservoirBlockEntity;
	}

	public void updateSides(Level world, BlockPos p) {
		adjacent[8] = this.isReservoir(world, p.north());
		adjacent[4] = this.isReservoir(world, p.west());
		adjacent[6] = this.isReservoir(world, p.east());
		adjacent[2] = this.isReservoir(world, p.south());
		adjacent[1] = this.isReservoir(world, p.offset(-1, 0, 1));
		adjacent[3] = this.isReservoir(world, p.offset(1, 0, 1));
		adjacent[7] = this.isReservoir(world, p.offset(-1, 0, -1));
		adjacent[9] = this.isReservoir(world, p.offset(1, 0, -1));
		this.syncAllData();
	}

	/** Original updateNeighbors (on placing and breaking): the eight horizontal neighbours re-check their sides. */
	public static void updateNeighbors(Level world, BlockPos pos) {
		for (int i = -1; i <= 1; i++) {
			for (int k = -1; k <= 1; k++) {
				if (i != 0 || k != 0) {
					BlockPos d = pos.offset(i, 0, k);
					if (world.getBlockEntity(d) instanceof ReservoirBlockEntity tr)
						tr.updateSides(world, d);
				}
			}
		}
	}

	public boolean hasNearbyReservoir(int loc) {
		return adjacent[loc];
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		if (this.getTicksExisted() == 0)
			this.updateSides(world, pos);
		flowTimer++;
		if (flowTimer >= PipingBlockEntity.getTickDelay()) {
			flowTimer = 0;
			this.transferBetween(world, pos);
		}
		if (!isCovered) {
			Biome biome = world.getBiome(pos).value();
			if (world.isRaining() && biome.getPrecipitationAt(pos) == Biome.Precipitation.RAIN && world.isRainingAt(pos.above())) {
				if (this.isEmpty() || (this.getFluid().isSame(Fluids.WATER) && this.getFluidAmount() < CAPACITY))
					this.addLiquid(25, Fluids.WATER);
			}
			if (!tank.isEmpty() && this.getFluid().getFluidType().isLighterThanAir())
				tank.drain(100, IFluidHandler.FluidAction.EXECUTE); //evaporate
		}
		tempTimer++;
		if (!this.isEmpty() && tempTimer >= (isCovered ? 30 : 20)) {
			tempTimer = 0;
			if (!this.isSurrounded(world, pos))
				this.temperatureEffects(world, pos);
		}
	}

	private void temperatureEffects(Level world, BlockPos pos) {
		Fluid f = this.getFluid();
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		int temp = f.getFluidType().getTemperature()-273;
		int dT = temp-Tamb;
		int r = 2;
		for (int i = -r; i <= r; i++) {
			for (int j = -r; j <= r; j++) {
				for (int k = -r; k <= r; k++) {
					double dd = Math.sqrt(i*i+j*j+k*k)+1;
					int hiT = (int)(Tamb+dT/dd/2D);
					TemperatureEnvironment.temperatureEnvironment(world, pos.offset(i, j, k), hiT);
					if (temp > 2500)
						world.playSound(null, pos.offset(i, j, k), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 1F);
				}
			}
		}
		if (temp > 2500) {
			BlockState lava = Blocks.LAVA.defaultBlockState();
			world.setBlockAndUpdate(pos, lava);
			for (Direction d : Direction.Plane.HORIZONTAL)
				world.setBlockAndUpdate(pos.relative(d), lava);
			world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1F);
			return;
		}
		boolean hot = Tamb >= 300;
		for (Direction d : Direction.values()) {
			BlockState b = world.getBlockState(pos.relative(d));
			hot |= b.is(Blocks.FIRE) || b.getFluidState().is(FluidTags.LAVA);
		}
		if (hot && isFlammable(f)) {
			world.removeBlock(pos, false);
			world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 4, true, Level.ExplosionInteraction.BLOCK);
		}
	}

	/** Original ReikaFluidHelper.isFlammable: by fluid name (fuels, ethanol, oils, gases, hydrocarbons and other organics). */
	public static boolean isFlammable(Fluid f) {
		String s = BuiltInRegistries.FLUID.getKey(f).getPath().toLowerCase(Locale.ENGLISH);
		if (s.contains("fuel") || s.contains("ethanol") || s.contains("oil") || s.equals("creosote") || s.contains("gas"))
			return true;
		if (s.endsWith("ane") || s.endsWith("ene") || s.endsWith("yne"))
			return true;
		return s.endsWith("ol") || s.endsWith("al") || s.endsWith("one");
	}

	/** Original isSurrounded(false): reservoirs on all four sides, or solid blocks all around. */
	private boolean isSurrounded(Level world, BlockPos pos) {
		if (adjacent[2] && adjacent[4] && adjacent[6] && adjacent[8])
			return true;
		for (Direction d : Direction.values()) {
			BlockPos p = pos.relative(d);
			if (!world.getBlockState(p).isSolidRender(world, p))
				return false;
		}
		return true;
	}

	private void transferBetween(Level world, BlockPos pos) {
		if (tank.getFluidAmount() >= CAPACITY)
			return;
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			if (this.adjacentOnSide(dir) && world.getBlockEntity(pos.relative(dir)) instanceof ReservoirBlockEntity tile && this.canMixWith(tile)) {
				int diff = tile.getFluidAmount()-this.getFluidAmount();
				if (diff > 1) {
					Fluid f = tile.getFluid();
					tile.tank.drain(diff/2, IFluidHandler.FluidAction.EXECUTE);
					tank.fill(new FluidStack(f, diff/2), IFluidHandler.FluidAction.EXECUTE);
					tile.setChanged();
					this.setChanged();
				}
			}
		}
	}

	public boolean adjacentOnSide(Direction dir) {
		return switch (dir) {
			case EAST -> adjacent[6];
			case NORTH -> adjacent[8];
			case SOUTH -> adjacent[2];
			case WEST -> adjacent[4];
			default -> false;
		};
	}

	private boolean canMixWith(ReservoirBlockEntity tile) {
		if (tile.isEmpty())
			return false;
		return tank.isEmpty() || this.getFluid().isSame(tile.getFluid());
	}

	/** Original isConnectedOnSide: the wall is left out towards a reservoir with the same (or no) fluid. */
	public boolean isConnectedOnSide(Direction dir) {
		if (!this.adjacentOnSide(dir) || level == null)
			return false;
		if (!(level.getBlockEntity(worldPosition.relative(dir)) instanceof ReservoirBlockEntity te))
			return false;
		return te.isEmpty() || this.isEmpty() || te.getFluid().isSame(this.getFluid());
	}

	/** Original applyFluidEffectsToEntity. */
	public void applyFluidEffectsToEntity(LivingEntity e) {
		if (tank.isEmpty() || isCovered)
			return;
		Fluid f = this.getFluid();
		if (f.isSame(RotaryFluids.JET_FUEL.get()))
			e.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
		if (f.isSame(RotaryFluids.ETHANOL.get())) {
			MobEffectInstance eff = e.getEffect(MobEffects.CONFUSION);
			int dura = eff != null ? eff.getDuration()+1 : 1;
			e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.min(dura, 600), 3));
		}
		int T = f.getFluidType().getTemperature();
		if (f.isSame(Fluids.LAVA) || T > 500) {
			e.hurt(level.damageSources().lava(), 4);
			e.igniteForSeconds(12);
		}
		if (T < 250)
			e.hurt(level.damageSources().wither(), 1);
		if (e.isOnFire() && isFlammable(f)) {
			level.removeBlock(worldPosition, false);
			level.explode(e, e.getX(), e.getY(), e.getZ(), 4F, true, Level.ExplosionInteraction.BLOCK);
		}
	}

	public boolean canAcceptFluid(Fluid f) {
		return tank.isEmpty() || f.isSame(this.getFluid());
	}

	public int getFluidAmount() {
		return tank.getFluidAmount();
	}

	public Fluid getFluid() {
		return tank.getFluid().getFluid();
	}

	public FluidStack getContents() {
		return tank.getFluid();
	}

	public void setLevel(int amt, Fluid f) {
		tank.setFluid(amt > 0 ? new FluidStack(f, amt) : FluidStack.EMPTY);
		this.setChanged();
	}

	public void addLiquid(int amt, Fluid f) {
		tank.fill(new FluidStack(f, amt), IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
	}

	public void removeLiquid(int amt) {
		tank.drain(amt, IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
	}

	public boolean isEmpty() {
		return tank.isEmpty();
	}

	@Override
	public int getRedstoneOverride() {
		return (int)(15*tank.getFluidAmount()/(float)CAPACITY);
	}

	@Override
	public boolean canConnectToPipe(PipeType m) {
		return m.isStandardPipe() || m == PipeType.HOSE || m == PipeType.FUEL_LINE || m == PipeType.VALVE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == Direction.DOWN ? Flow.OUTPUT : Flow.INPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from == Direction.UP)
			return 0;
		int amt = tank.fill(resource, action);
		if (amt > 0 && action.execute())
			this.setChanged();
		return amt;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from == Direction.UP)
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
				return true;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return ReservoirBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.isSameFluidSameComponents(resource, tank.getFluid()) ? ReservoirBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return ReservoirBlockEntity.this.drain(side, maxDrain, action);
			}
		};
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public void onEMP() {}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		RotaryComponents.ReservoirData d = input.get(RotaryComponents.RESERVOIR_DATA.get());
		if (d != null) {
			tank.setFluid(d.fluid().copy());
			isCovered = d.cover();
		}
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (isCovered || !tank.isEmpty())
			components.set(RotaryComponents.RESERVOIR_DATA.get(), new RotaryComponents.ReservoirData(tank.getFluid().copy(), isCovered));
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.putBoolean("cover", isCovered);
		int bits = 0;
		for (int i = 0; i < adjacent.length; i++)
			if (adjacent[i])
				bits |= 1 << i;
		NBT.putInt("sides", bits);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		isCovered = NBT.getBoolean("cover");
		int bits = NBT.getInt("sides");
		for (int i = 0; i < adjacent.length; i++)
			adjacent[i] = (bits & (1 << i)) != 0;
	}
}
