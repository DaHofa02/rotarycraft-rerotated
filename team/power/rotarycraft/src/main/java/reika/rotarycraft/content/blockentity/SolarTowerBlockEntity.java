/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.api.interfaces.SodiumSolarUpgrades;
import reika.rotarycraft.api.power.PowerGenerator;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.content.solar.SolarPlant;
import reika.rotarycraft.content.solar.SolarPlantBlock;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.TemperatureEnvironment;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Production.TileEntitySolar - a stack of these is a solar tower. The bottom block of the
 * plant's primary tower collects the water of the blocks above, and while there are mirrors it gives shaft power downwards: 512 rad/s
 * (sodium 4096) at min(16384, brightness * tower multiplier * (mirrors+1)) Nm (sodium: min(65536, 1.75 * ... * (mirrors+1)^1.5) and only
 * from 800 C), using 10+16*log2(P) mB rounded up to the next 10/100/1000 per tick (sodium 10+64*log2(P), /128, scaled by the share of
 * full power; used sodium goes to a ReactorCraft sodium output underneath). The top block counts the mirrors and their brightness
 * every 100 ticks; a ReactorCraft sodium receiver on top is heated by them and allows sodium. Every tower block heats its
 * surroundings (7x7 above it) with 5 * mirrors * brightness C (up to 1750 C, with block damage on; from 1500 C it melts into lava) and
 * sets living things there on fire from 400 C. Liquids go in from any side; nothing comes out.
 */
public class SolarTowerBlockEntity extends IOMachineBlockEntity implements SimpleProvider, PipeConnector, PowerGenerator, SolarPlantBlock {

	public static final int GENOMEGA = 512;
	public static final int GENOMEGA_SODIUM = 4096;

	public static final int MAXTORQUE = 16384;
	public static final int MAXTORQUE_SODIUM = 65536;

	public static final int CAPACITY = 4000;

	private float overallBrightness;
	private int size;
	private int topLocation = -1;

	private int temperature;
	private int currentConsumption;
	private int mirrorTimer;

	private final FluidTank tank = new FluidTank(CAPACITY) {
		@Override
		protected void onContentsChanged() {
			SolarTowerBlockEntity.this.setChanged();
		}
	};

	@Nullable
	private SolarPlant plant;

	public SolarTowerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SOLAR_TOWER.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	/** Original FluidRegistry.getFluid("rc sodium"): ReactorCraft's sodium, if ReactorCraft is there. */
	public static Fluid sodium() {
		return BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("reactorcraft", "sodium"));
	}

	private static boolean isSodium(FluidStack fs) {
		Fluid na = sodium();
		return !fs.isEmpty() && na != Fluids.EMPTY && fs.getFluid().isSame(na);
	}

	private boolean isWater() {
		return !tank.isEmpty() && tank.getFluid().getFluid().isSame(Fluids.WATER);
	}

	@Override
	public void searchForPlant(Level world, BlockPos pos) {
		if (plant != null)
			return;
		plant = SolarPlant.build(world, pos);
		size = -1;
	}

	@Override
	@Nullable
	public SolarPlant getPlant() {
		return plant;
	}

	@Override
	public void setPlant(@Nullable SolarPlant p) {
		plant = p;
		size = -1;
	}

	@Override
	public boolean canProvidePower() {
		return true;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.searchForPlant(world, pos);
		topLocation = this.getTopOfTower();

		if (!world.isClientSide) {
			int temp = (int)(5*size*overallBrightness);
			for (int i = -3; i <= 3; i++) {
				for (int j = -3; j <= 3; j++) {
					if (RotaryConfig.BLOCKDAMAGE.get()) {
						TemperatureEnvironment.temperatureEnvironment(world, pos.offset(i, 1, j), Math.min(temp, 1750));
						if (temp >= 1500) {
							world.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
							return;
						}
					}
				}
			}
			if (temp > 400) {
				AABB above = new AABB(pos.getX()-3, pos.getY()+1, pos.getZ()-3, pos.getX()+4, pos.getY()+2, pos.getZ()+4);
				for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, above))
					if (!e.hasEffect(MobEffects.FIRE_RESISTANCE))
						e.igniteForSeconds(3);
			}
		}
		if (!(world.getBlockEntity(pos.below()) instanceof SolarTowerBlockEntity)) {
			BlockPos c = plant != null ? plant.getPrimaryTower() : null;
			if (c != null && c.getX() == pos.getX() && c.getZ() == pos.getZ())
				this.generatePower(world, pos);
			else
				power = omega = torque = 0;
		}
		else {
			write = null;
		}
		BlockEntity up = world.getBlockEntity(pos.above());
		if (!world.getBlockState(pos.above()).isAir() && !(up instanceof SodiumSolarUpgrades)) {
			if (up instanceof SolarTowerBlockEntity t)
				temperature = t.temperature;
			return;
		}

		if (world.getBlockEntity(new BlockPos(pos.getX(), topLocation+1, pos.getZ())) instanceof SodiumSolarUpgrades.SodiumSolarReceiver ss && ss.isActive()) {
			ss.tick(size, overallBrightness);
			temperature = ss.getTemperature();
		}

		if (plant != null) {
			if (this.isTopOfTower()) {
				mirrorTimer++;
				if (mirrorTimer >= 100 || size == -1) {
					mirrorTimer = 0;
					overallBrightness = plant.getOverallBrightness(world);
					size = plant.mirrorCount();
				}
			}
			else {
				overallBrightness = this.getArrayOverallBrightness();
				size = this.getArraySize();
			}
		}
		else {
			size = -1;
			overallBrightness = 0;
		}

		if (write != null)
			this.basicPowerReceiver();
	}

	private void generatePower(Level world, BlockPos pos) {
		this.getTowerWater(world, pos);
		int amt = this.getConsumedFluid();
		write = Direction.DOWN;
		boolean water = this.isWater();
		omega = water ? GENOMEGA : GENOMEGA_SODIUM;
		torque = this.getGenTorque();
		if (this.getArraySize() <= 0 || torque == 0 || tank.getFluidAmount() < amt || (!water && temperature < 800)) {
			omega = 0;
			torque = 0;
		}
		power = (long)omega*(long)torque;
		if (isSodium(tank.getFluid()))
			amt = (int)Math.max(1, amt*power/((double)GENOMEGA_SODIUM*MAXTORQUE_SODIUM));
		currentConsumption = amt;
		if (power > 0 && tank.getFluidAmount() > 0 && amt > 0) {
			if (!water) {
				if (world.getBlockEntity(pos.below()) instanceof SodiumSolarUpgrades.SodiumSolarOutput ss && ss.isActive())
					amt = ss.receiveSodium(amt);
			}
			if (amt > 0)
				tank.drain(amt, IFluidHandler.FluidAction.EXECUTE);
		}
	}

	private int getGenTorque() {
		if (tank.isEmpty() || plant == null)
			return 0;
		boolean water = this.isWater();
		int cap = water ? MAXTORQUE : MAXTORQUE_SODIUM;
		float f = water ? 1 : 1.75F;
		float p = water ? 1 : 1.5F;
		return Math.min(cap, (int)(f*this.getArrayOverallBrightness()*plant.getTowerMultiplier()*Math.pow(this.getArraySize()+1, p)));
	}

	public int getConsumedFluid() {
		boolean sodium = isSodium(tank.getFluid());
		int p = ReikaMathLibrary.logbase2(power);
		if (sodium)
			p = Math.max(1, p);
		int base = 10+(sodium ? 64 : 16)*p;
		int rnd = 10;
		if (base >= 1000)
			rnd = 1000;
		else if (base >= 100)
			rnd = 100;
		int ret = base-base%rnd+rnd; //original ReikaMathLibrary.roundUpToX
		if (sodium)
			ret *= 1/128D;
		return ret;
	}

	public int getCurrentConsumption() {
		return currentConsumption;
	}

	public int getTemperature() {
		return temperature;
	}

	public FluidTank getTank() {
		return tank;
	}

	public int getArraySize() {
		return level != null && level.getBlockEntity(new BlockPos(worldPosition.getX(), topLocation, worldPosition.getZ())) instanceof SolarTowerBlockEntity t ? t.size : 0;
	}

	public float getArrayOverallBrightness() {
		return level != null && level.getBlockEntity(new BlockPos(worldPosition.getX(), topLocation, worldPosition.getZ())) instanceof SolarTowerBlockEntity t ? t.overallBrightness : 0;
	}

	public int getTopOfTower() {
		int y = worldPosition.getY();
		while (level.getBlockEntity(new BlockPos(worldPosition.getX(), y, worldPosition.getZ())) instanceof SolarTowerBlockEntity)
			y++;
		return y-1;
	}

	public int getBottomOfTower() {
		int y = worldPosition.getY();
		while (level.getBlockEntity(new BlockPos(worldPosition.getX(), y, worldPosition.getZ())) instanceof SolarTowerBlockEntity)
			y--;
		return y+1;
	}

	public boolean isTopOfTower() {
		return worldPosition.getY() == this.getTopOfTower();
	}

	/** Original getTowerWater: the bottom block takes the liquid of the tower blocks above that hold the same liquid. */
	private void getTowerWater(Level world, BlockPos pos) {
		int lvl = tank.getFluidAmount();
		Fluid f = tank.isEmpty() ? null : tank.getFluid().getFluid();
		BlockPos c = pos.above();
		while (world.getBlockEntity(c) instanceof SolarTowerBlockEntity tile) {
			Fluid f2 = tile.tank.isEmpty() ? null : tile.tank.getFluid().getFluid();
			if (f == null && f2 != null)
				f = f2;
			if (f2 != null && f.isSame(f2)) {
				lvl += tile.tank.getFluidAmount();
				tile.tank.setFluid(FluidStack.EMPTY);
			}
			c = c.above();
		}
		tank.setFluid(f == null || lvl <= 0 ? FluidStack.EMPTY : new FluidStack(f, lvl));
	}

	private boolean canUseSodium() {
		return level.getBlockEntity(new BlockPos(worldPosition.getX(), this.getTopOfTower()+1, worldPosition.getZ())) instanceof SodiumSolarUpgrades.SodiumSolarReceiver ss
				&& ss.isActive();
	}

	private boolean canFill(Fluid f) {
		return f.isSame(Fluids.WATER) || (isSodium(new FluidStack(f, 1)) && this.canUseSodium());
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return true;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (resource.isEmpty() || !this.canFill(resource.getFluid()))
			return 0;
		return tank.fill(resource, action);
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return Flow.INPUT;
	}

	/** Capability: fill from every side, nothing can be drained. */
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
				return SolarTowerBlockEntity.this.canFill(stack.getFluid());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return SolarTowerBlockEntity.this.fill(side, resource, action);
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
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		return new PowerSourceList().addSource(this);
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		c.add(this.getAdjacentTileEntity(write));
	}

	@Override
	public long getMaxPower() {
		return (long)torque*omega;
	}

	@Override
	public long getCurrentPower() {
		return power;
	}

	@Override
	public BlockPos getEmittingPos() {
		return write != null ? worldPosition.relative(write) : worldPosition;
	}

	/** Original breakBlock: the plant is rebuilt by its remaining blocks. */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		if (plant != null)
			plant.invalidate(world);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("solar", tank.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("temp", temperature);
		NBT.putInt("flow", currentConsumption);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("solar"));
		temperature = NBT.getInt("temp");
		currentConsumption = NBT.getInt("flow");
	}
}
