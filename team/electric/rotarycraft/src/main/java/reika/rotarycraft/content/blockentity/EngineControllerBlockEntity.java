/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.content.block.EcuBlock;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.EngineType.EngineClass;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * Original: Reika.RotaryCraft.TileEntities.Auxiliary.TileEntityEngineController (Engine Control Unit). Sits below an engine (above a
 * flipped one), throttles it to one of five settings (set with the screwdriver, or from the redstone strength in redstone mode) and feeds
 * it fuel from its own tank. Not ported: Project Red bundled-cable input (the dye colour is kept and shown, as in the original without
 * Project Red), the Fuel Engine and air/oxygen fluids (not ported yet), and the ComputerCraft/OpenComputers Lua call.
 */
public class EngineControllerBlockEntity extends RotaryCraftBlockEntity implements reika.rotarycraft.content.piping.PipeConnector {

	public static final int FUELCAP = 3000;

	private final FluidTank tank = new FluidTank(FUELCAP);

	public boolean redstoneMode;
	private int redstoneTick = 0;
	private int prevRedstone;

	private EngineSettings setting = EngineSettings.FULL;

	/** Original ReikaDyeHelper ordinal (0 black .. 15 white), -1 none. */
	private int signalColor = -1;

	public enum EngineSettings {
		SHUTDOWN(0, 0),
		STANDBY(16, 64),
		LOW(4, 8),
		MEDIUM(2, 2),
		FULL(1, 1);

		public final int speedFactor;
		public final int fuelFactor;

		public static final EngineSettings[] list = values();

		EngineSettings(int speed, int fuel) {
			speedFactor = speed;
			fuelFactor = fuel;
		}

		public double getSpeedDecimal() {
			if (this == SHUTDOWN)
				return 0;
			return 100D/speedFactor;
		}

		public int getEfficiencyFactor() {
			if (this == SHUTDOWN)
				return 0;
			return fuelFactor/speedFactor;
		}
	}

	public EngineControllerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.ECU.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Engine Control Unit";
	}

	public boolean consumeFuel() {
		return setting.fuelFactor != 0;
	}

	public boolean canProducePower() {
		return setting.speedFactor != 0;
	}

	public boolean playSound() {
		return this.canProducePower();
	}

	public float getSpeedMultiplier() {
		if (this.canProducePower())
			return 1F/setting.speedFactor;
		return 0;
	}

	public int getSpeedFactor() {
		if (this.canProducePower())
			return setting.speedFactor;
		return 0;
	}

	public int getFuelMultiplier(EngineClass e) {
		int base = setting.fuelFactor;
		if (e == EngineClass.TURBINE)
			base /= 8;
		return Math.max(1, base);
	}

	public float getSoundStretch() {
		return switch(setting) {
			case FULL -> 1F;
			case LOW -> 0.6F;
			case MEDIUM -> 0.8F;
			case SHUTDOWN -> 0F;
			case STANDBY -> 0.4F;
		};
	}

	public EngineSettings getSetting() {
		return setting;
	}

	/** Original setColor: dye right-click; the side texture shows the colour (blockstate here). */
	public void setColor(int dye) {
		signalColor = dye;
		if (level != null && !level.isClientSide) {
			BlockState bs = this.getBlockState();
			if (bs.hasProperty(EcuBlock.COLOR))
				level.setBlock(worldPosition, bs.setValue(EcuBlock.COLOR, dye+1), 3);
			this.syncAllData();
			level.updateNeighborsAt(worldPosition, bs.getBlock());
		}
	}

	public void increment() {
		int l = EngineSettings.list.length;
		int o = setting.ordinal();
		o++;
		if (o >= l)
			o = 0;
		setting = EngineSettings.list[o];
		this.syncAllData();
	}

	public void setSetting(int ordinal) {
		int o = Math.max(0, Math.min(ordinal, EngineSettings.list.length-1));
		setting = EngineSettings.list[o];
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (redstoneTick > 0)
			redstoneTick--;
		int power = redstoneTick == 0 ? world.getDirectSignalTo(pos) : prevRedstone;
		if (redstoneTick == 0 && power == 0 && world.hasNeighborSignal(pos))
			power = 15;
		if (prevRedstone != power)
			redstoneTick = 60;
		prevRedstone = power;

		if (redstoneMode && !world.isClientSide) {
			boolean signal = power == 15;
			EngineSettings old = setting;
			setting = signal ? EngineSettings.SHUTDOWN : EngineSettings.list[4-power/3];
			if (old != setting)
				this.syncAllData();
		}
		if (world.isClientSide || tank.isEmpty())
			return;

		if (world.getBlockEntity(pos.above()) instanceof EngineBlockEntity eng)
			if (this.transferToEngine(eng, false))
				return;

		if (world.getBlockEntity(pos.below()) instanceof EngineBlockEntity eng)
			if (this.transferToEngine(eng, true))
				return;
	}

	private boolean transferToEngine(EngineBlockEntity te, boolean flip) {
		if (te.isFlipped != flip)
			return false;
		FluidStack liq = tank.getFluid();
		Fluid f = te.getEngineType().getFuelType();
		if (f == null || liq.isEmpty() || !f.isSame(liq.getFluid()))
			return false;
		if (te.getFuelLevel()+liq.getAmount() > EngineBlockEntity.FUELCAP)
			return false;
		te.addFuel(liq.getAmount()/4+1);
		tank.drain(liq.getAmount()/4+1, IFluidHandler.FluidAction.EXECUTE);
		return true;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);

		tank.writeToNBT(registries, NBT);

		NBT.putInt("lvl", setting.ordinal());

		NBT.putBoolean("redstone", redstoneMode);

		NBT.putInt("color", signalColor);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);

		tank.readFromNBT(registries, NBT);

		setting = EngineSettings.list[Math.max(0, Math.min(NBT.getInt("lvl"), EngineSettings.list.length-1))];

		redstoneMode = NBT.getBoolean("redstone");

		signalColor = NBT.contains("color") ? NBT.getInt("color") : -1;
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	public static String getSettingsAsString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < EngineSettings.list.length; i++) {
			EngineSettings set = EngineSettings.list[i];
			String n = set.name().charAt(0)+set.name().substring(1).toLowerCase(java.util.Locale.ENGLISH);
			sb.append(String.format("%s: %.2f%% Speed, %dx Fuel Efficiency", n, set.getSpeedDecimal(), set.getEfficiencyFactor()));
			if (i < EngineSettings.list.length-1)
				sb.append("\n");
		}
		return sb.toString();
	}

	/** Original canFill: the fuel of the engine above, otherwise jet fuel or ethanol (fuel-engine fuels and oxygen are not ported). */
	public boolean canFill(Fluid fluid) {
		BlockEntity te = this.getAdjacentTileEntity(Direction.UP);
		if (te instanceof EngineBlockEntity eng) {
			Fluid f = eng.getEngineType().getFuelType();
			return eng.getEngineType() != EngineType.STEAM && eng.getEngineType().burnsFuel() && f != null && fluid.isSame(f);
		}
		return fluid.isSame(RotaryFluids.JET_FUEL.get()) || fluid.isSame(RotaryFluids.ETHANOL.get());
	}

	/** Original canConnectToPipe: fuel lines and bedrock pipes, on every side, in both directions. */
	@Override
	public boolean canConnectToPipe(reika.rotarycraft.content.piping.PipeType m) {
		return m == reika.rotarycraft.content.piping.PipeType.FUEL_LINE || m == reika.rotarycraft.content.piping.PipeType.BEDROCK_PIPE;
	}

	@Override
	public boolean canConnectToPipeOnSide(reika.rotarycraft.content.piping.PipeType p, Direction side) {
		return true;
	}

	@Override
	public reika.rotarycraft.content.piping.PipeConnector.Flow getFlowForSide(Direction side) {
		return reika.rotarycraft.content.piping.PipeConnector.Flow.DUAL;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return this.getFluidHandler(from).fill(resource, action);
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return this.getFluidHandler(from).drain(maxDrain, action);
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
				return FUELCAP;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return EngineControllerBlockEntity.this.canFill(stack.getFluid());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				if (!EngineControllerBlockEntity.this.canFill(resource.getFluid()))
					return 0;
				int amt = tank.fill(resource, action);
				if (amt > 0 && action.execute())
					EngineControllerBlockEntity.this.setChanged();
				return amt;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return tank.getFluid().getFluid().isSame(resource.getFluid()) ? tank.drain(resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return tank.drain(maxDrain, action);
			}
		};
	}
}
