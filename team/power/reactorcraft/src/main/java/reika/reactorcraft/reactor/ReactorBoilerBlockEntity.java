/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.powergen.SteamLineBlockEntity;
import reika.reactorcraft.powergen.SteamTile;
import reika.reactorcraft.powergen.WorkingFluid;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.RandomHelper;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fission.TileEntityReactorBoiler with TileEntityNuclearBoiler and TileEntityTankedReactorMachine -
 * a 12000 mB tank (water, heavy water or ammonia, by pipe from below or by bucket/canister) that takes heat from the reactor parts around
 * it. Above 100 C and 50 C over ambient it turns 200 mB into one unit of steam each tick and cools by 5 C; steam goes up into the boiler
 * above (same working fluid) or is taken by a steam line on top. Stacked boilers level their liquid (a quarter of the difference per
 * tick). It records which reactor types heated it. Ammonia above 650 C detonates (the steam lines above burst, nearby glass breaks);
 * above 2000 C it explodes; in surroundings over 100 C (Nether) it explodes at 300 C when it holds liquid. Heavy water in it moderates
 * neutrons; with liquid it absorbs fast neutrons 1 in 10, and breeder/thorium neutrons 80 %.
 */
public class ReactorBoilerBlockEntity extends ReactorBlockEntity implements Temperatured, ReactorTyped, NeutronTile, ReactorBlock, SteamTile, PipeConnector {

	public static final int WATER_PER_STEAM = 200;
	public static final int DETTEMP = 650;
	public static final int CAPACITY = 12000;

	private final FluidTank tank = new FluidTank(CAPACITY, f -> WorkingFluid.getWorkingFluid(f.getFluid()) != null);
	private final Map<ReactorType, Double> type = new EnumMap<>(ReactorType.class);
	private WorkingFluid fluid = WorkingFluid.EMPTY;
	private int steam;

	public ReactorBoilerBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.BOILER.get(), pos, state);
		this.setReactorType(ReactorType.FISSION, 1);
	}

	@Override
	public String getName() {
		return "Reactor Boiler";
	}

	public final void setReactorType(ReactorType t, double amt) {
		type.merge(t, amt, Double::sum);
	}

	/** Original Proportionality.getLargestCategory. */
	@Override
	public ReactorType getReactorType() {
		ReactorType max = null;
		double v = Double.NEGATIVE_INFINITY;
		for (Map.Entry<ReactorType, Double> e : type.entrySet()) {
			if (e.getValue() > v) {
				v = e.getValue();
				max = e.getKey();
			}
		}
		return max;
	}

	/** The share of each reactor type in the heat this boiler received (original getReactorTypeSet/getReactorTypeFraction). */
	public Map<ReactorType, Double> getReactorTypeFractions() {
		double sum = type.values().stream().mapToDouble(Double::doubleValue).sum();
		Map<ReactorType, Double> ret = new EnumMap<>(ReactorType.class);
		for (Map.Entry<ReactorType, Double> e : type.entrySet())
			ret.put(e.getKey(), sum == 0 ? 0 : e.getValue()/sum);
		return ret;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (this.thermalTick() && !world.isClientSide) {
			this.updateTemperature(world, pos);
			if (this.isRemoved())
				return;
			if (temperature > this.getMaxTemperature()) {
				this.overheat(world, pos);
				return;
			}
		}
		this.balanceFluid(world, pos);
		if (world.isClientSide)
			return;

		if (temperature >= DETTEMP && fluid == WorkingFluid.AMMONIA) {
			this.detonateAmmonia((ServerLevel)world, pos);
			return;
		}

		if (tank.getFluidAmount() >= WATER_PER_STEAM && temperature > 100 && this.canBoilTankLiquid(world, pos)) {
			steam++;
			fluid = WorkingFluid.getWorkingFluid(tank.getFluid().getFluid());
			tank.drain(WATER_PER_STEAM, IFluidHandler.FluidAction.EXECUTE);
			temperature -= 5;
			this.setChanged();
		}

		if (steam <= 0)
			fluid = WorkingFluid.EMPTY;

		this.transferSteam(world, pos);
	}

	@Override
	protected boolean afterAmbientStep(Level world, BlockPos pos, int Tamb) {
		if (temperature >= 300 && Tamb > 100 && !tank.isEmpty()) {
			world.removeBlock(pos, false);
			world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 3F, true, Level.ExplosionInteraction.BLOCK);
			return true;
		}
		return false;
	}

	/** Original: TileEntityReactorBase records the reactor type of typed (non-boiler) parts and of boilers that heat this boiler. */
	@Override
	protected void onHeatReceived(ReactorBlockEntity from, double amount) {
		if (from instanceof ReactorBoilerBlockEntity b)
			this.setReactorType(b.getReactorType(), amount);
		else if (from instanceof ReactorTyped t && from instanceof ReactorBlock)
			this.setReactorType(t.getReactorType(), amount);
	}

	private boolean canBoilTankLiquid(Level world, BlockPos pos) {
		WorkingFluid w = WorkingFluid.getWorkingFluid(tank.getFluid().getFluid());
		if (w == null)
			return false;
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (temperature < Tamb+50)
			return false;
		return fluid == WorkingFluid.EMPTY || w == fluid;
	}

	private void transferSteam(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos.above()) instanceof ReactorBoilerBlockEntity te) {
			if (steam > 0 && fluid != WorkingFluid.EMPTY) {
				if (te.fluid == WorkingFluid.EMPTY || te.fluid == fluid) {
					te.fluid = fluid;
					te.steam += steam;
					steam = 0;
				}
			}
		}
	}

	/** Original balanceFluid: the boiler below/above with less liquid of the same kind gets a quarter of the difference (+1). */
	private void balanceFluid(Level world, BlockPos pos) {
		for (Direction dir : new Direction[]{Direction.DOWN, Direction.UP}) {
			if (world.getBlockEntity(pos.relative(dir)) instanceof ReactorBoilerBlockEntity te) {
				if (te.tank.getFluidAmount() < tank.getFluidAmount() && (te.tank.isEmpty() || te.tank.getFluid().getFluid().isSame(tank.getFluid().getFluid()))) {
					int dl = tank.getFluidAmount()-te.tank.getFluidAmount();
					int amt = dl/4+1;
					te.tank.fill(new FluidStack(tank.getFluid().getFluid(), amt), IFluidHandler.FluidAction.EXECUTE);
					tank.drain(amt, IFluidHandler.FluidAction.EXECUTE);
				}
			}
		}
	}

	private void detonateAmmonia(ServerLevel world, BlockPos pos) {
		java.util.Set<BlockPos> pipes = new java.util.HashSet<>();
		java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
		if (world.getBlockEntity(pos.above()) instanceof SteamLineBlockEntity) {
			queue.add(pos.above());
			pipes.add(pos.above());
		}
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (!pipes.contains(n) && world.getBlockEntity(n) instanceof SteamLineBlockEntity) {
					pipes.add(n);
					queue.add(n);
				}
			}
		}
		for (BlockPos c : pipes) {
			world.removeBlock(c, false);
			world.sendParticles(ParticleTypes.EXPLOSION, c.getX()+0.5, c.getY()+0.5, c.getZ()+0.5, 1, 0, 0, 0, 0);
			Containers.dropItemStack(world, c.getX(), c.getY(), c.getZ(), new ItemStack(Items.NETHER_BRICK));
		}
		world.removeBlock(pos, false);
		Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(RotaryItems.part(reika.rotarycraft.content.item.CraftingPart.SCRAP), 8+rand.nextInt(18)));
		world.sendParticles(ParticleTypes.EXPLOSION, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 1, 0, 0, 0, 0);
		world.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.2F, 1);
		boolean flag = false;
		int r = 8;
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
			BlockState s = world.getBlockState(p);
			if (!s.isAir() && (s.is(Tags.Blocks.GLASS_BLOCKS) || s.is(Tags.Blocks.GLASS_PANES))) {
				world.destroyBlock(p, true);
				flag = true;
			}
		}
		if (flag)
			world.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1, 1);
	}

	private void overheat(Level world, BlockPos pos) {
		world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 8, true, Level.ExplosionInteraction.BLOCK);
		for (int i = 0; i < 4; i++)
			Containers.dropItemStack(world, pos.getX()+rand.nextDouble(), pos.getY()+rand.nextDouble(), pos.getZ()+rand.nextDouble(),
					new ItemStack(RotaryItems.part(reika.rotarycraft.content.item.CraftingPart.SCRAP)));
	}

	public int removeSteam() {
		int s = steam;
		steam = 0;
		return s;
	}

	@Override
	public int getSteam() {
		return steam;
	}

	public WorkingFluid getWorkingFluid() {
		return fluid;
	}

	public FluidTank getTank() {
		return tank;
	}

	/** Bucket and canister by hand (original BlockReactorTile): adds 1000 mB if it fits and the liquid matches. */
	public boolean addLiquid(Fluid f, int amt) {
		if (tank.getFluidAmount()+amt > CAPACITY)
			return false;
		if (!tank.isEmpty() && !tank.getFluid().getFluid().isSame(f))
			return false;
		tank.fill(new FluidStack(f, amt), IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
		return true;
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		if (!tank.isEmpty()) {
			if (tank.getFluid().getFluid().isSame(ReactorFluids.get("heavy_water")))
				e.moderate();
			else if (e.getNeutronSpeed() == NeutronEntity.NeutronSpeed.FAST && rand.nextInt(10) == 0)
				return true;
			return RandomHelper.doWithChance(e.getNeutronType().getBoilerAbsorptionChance(), rand);
		}
		return false;
	}

	@Override
	public int getMaxTemperature() {
		return 2000;
	}

	@Override
	public boolean canDumpHeatInto(LiquidStates liq) {
		return false;
	}

	public int getTextureState(Level world, BlockPos pos) {
		Block b = this.getBlockState().getBlock();
		boolean up = world.getBlockState(pos.above()).is(b), down = world.getBlockState(pos.below()).is(b);
		return up && down ? 2 : up ? 1 : down ? 3 : 0;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side == Direction.DOWN && this.canConnectToPipe(p);
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from != Direction.DOWN)
			return 0;
		int n = tank.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == Direction.DOWN ? Flow.INPUT : Flow.NONE;
	}

	/** Capability: fill from below only, nothing can be drained. */
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
				return tank.isFluidValid(stack);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return ReactorBoilerBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return FluidStack.EMPTY;
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("steam", steam);
		fluid.saveToNBT(NBT);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		steam = NBT.getInt("steam");
		fluid = WorkingFluid.getFromNBT(NBT);
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		CompoundTag t = new CompoundTag();
		for (Map.Entry<ReactorType, Double> e : type.entrySet())
			t.putDouble(e.getKey().name(), e.getValue());
		NBT.put("types", t);
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		if (NBT.contains("types")) {
			type.clear();
			CompoundTag t = NBT.getCompound("types");
			for (ReactorType r : ReactorType.values())
				if (t.contains(r.name()))
					type.put(r, t.getDouble(r.name()));
		}
	}
}
