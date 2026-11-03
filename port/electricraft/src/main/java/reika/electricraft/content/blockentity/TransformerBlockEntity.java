/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.electricraft.network.Overloadable;
import reika.electricraft.network.WireEmitter;
import reika.electricraft.network.WireReceiver;
import reika.electricraft.registry.ElectriBlockEntities;
import reika.rotarycraft.content.blockentity.TemperatureMachine;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.ElectriCraft.TileEntities.TileEntityTransformer - takes current at the back and gives it out at the front with the
 * turns ratio n1:n2 (set in its GUI): V*n2/n1, A*n1/n2 times the efficiency 1/r^0.1. Over 4096 A in or out it explodes (strength 12);
 * it heats with the power it carries and explodes (strength 3) at 1000 C.
 */
public class TransformerBlockEntity extends NetworkBlockEntity implements WireEmitter, WireReceiver, Overloadable, TemperatureMachine {

	public static final int MAXTEMP = 1000;
	public static final int MAXCURRENT = 4096;

	private int Vin, Ain, Vout, Aout, Vold, Aold;
	private int n1 = 1;
	private int n2 = 1;
	private Direction facing;
	private int temperature;
	private int tempTimer;

	public TransformerBlockEntity(BlockPos pos, BlockState state) {
		super(ElectriBlockEntities.TRANSFORMER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Transformer";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		super.updateEntity(world, pos, state);
		if (!world.isClientSide && network != null) {
			Vin = network.getTerminalVoltage(this);
			Ain = network.getTerminalCurrent(this);
			boolean chg = Vin != Vold || Ain != Aold;
			if (this.getTicksExisted() == 0 || chg) {
				this.calculateOutput();
				network.updateWires();
				this.syncAllData();
			}
			Vold = Vin;
			Aold = Ain;
			if (Ain > MAXCURRENT || Aout > MAXCURRENT)
				this.overload(Math.max(Ain, Aout));
		}
		tempTimer++;
		if (tempTimer >= 20) {
			tempTimer = 0;
			if (!world.isClientSide)
				this.updateTemperature(world, pos);
		}
	}

	@Override
	public void overload(int current) {
		if (level != null && !level.isClientSide)
			level.explode(null, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, 12, true, Level.ExplosionInteraction.BLOCK);
	}

	public double getRatio() {
		return (double)n2/n1;
	}

	public boolean isStepUp() {
		return n2 > n1;
	}

	public int getN1() {
		return n1;
	}

	public int getN2() {
		return n2;
	}

	/** Original setRatio: zero turns are ignored. */
	public void setRatio(int n1, int n2) {
		if (n1 <= 0 || n2 <= 0)
			return;
		boolean chg = n1 != this.n1 || n2 != this.n2;
		this.n1 = n1;
		this.n2 = n2;
		this.calculateOutput();
		if (chg && network != null)
			network.updateWires();
		this.syncAllData();
	}

	public String getRatioForDisplay() {
		return String.format("%d:%d", n1, n2);
	}

	private void calculateOutput() {
		double ratio = this.getRatio();
		if (ratio == 0) {
			Vout = Aout = 0;
		}
		else {
			Vout = (int)(Vin*ratio);
			Aout = (int)(Ain/ratio*this.getEfficiency());
		}
	}

	public double getEfficiency() {
		double r = this.getRatio();
		if (r < 1)
			r = 1D/r;
		return 1/Math.pow(r, 0.1);
	}

	public int getInputVoltage() {
		return Vin;
	}

	public int getInputCurrent() {
		return Ain;
	}

	/**
	 * Original updateTemperature, once a second. The liquid nitrogen cooling of the original (tank fed by pipes from the sides) is not
	 * ported - the fluid does not exist in this port - so the uncooled factor 0.35 always applies.
	 */
	private void updateTemperature(Level world, BlockPos pos) {
		if (Aout > 0 && Vout > 0) {
			double ratio = this.getRatio();
			double r = ratio >= 0 ? ratio : 1/ratio;
			double d = 0.35;
			temperature += d*Math.sqrt(Ain*Vin)+Math.max(0, ReikaMathLibrary.logbase(r+1, 2));
		}
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (temperature > Tamb)
			temperature -= (temperature-Tamb)/5;
		else
			temperature += (temperature-Tamb)/5;
		if (temperature-Tamb <= 4 && temperature > Tamb)
			temperature--;
		if (temperature > MAXTEMP) {
			temperature = MAXTEMP;
			world.removeBlock(pos, false);
			world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 3, true, Level.ExplosionInteraction.BLOCK);
			return;
		}
		if (temperature < Tamb)
			temperature = Tamb;
	}

	@Override
	public void addTemperature(int temp) {
		temperature += temp;
	}

	@Override
	public int getTemperature() {
		return temperature;
	}

	@Override
	public void setTemperature(int temp) {
		temperature = temp;
	}

	@Override
	public int getMaxTemperature() {
		return MAXTEMP;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public boolean canNetworkOnSide(Direction dir) {
		return dir == this.getFacing() || dir == this.getFacing().getOpposite();
	}

	@Override
	public boolean canReceivePowerFromSide(Direction dir) {
		return dir == this.getFacing().getOpposite();
	}

	@Override
	public boolean canReceivePower() {
		return true;
	}

	@Override
	public int getGenVoltage() {
		return Vout;
	}

	@Override
	public int getGenCurrent() {
		return Aout;
	}

	@Override
	public boolean canEmitPowerToSide(Direction dir) {
		return dir == this.getFacing();
	}

	@Override
	public boolean canEmitPower() {
		return true;
	}

	public Direction getFacing() {
		return facing != null ? facing : Direction.EAST;
	}

	public void setFacing(Direction dir) {
		facing = dir;
		this.setChanged();
	}

	/** Original incrementFacing (screwdriver). */
	public void incrementFacing() {
		int o = this.getFacing().ordinal();
		this.setFacing(o == 5 ? Direction.values()[2] : Direction.values()[o+1]);
		this.rebuildNetwork();
		this.syncAllData();
	}

	/** Original getAABB. */
	public VoxelShape getShape() {
		int dx = Math.abs(this.getFacing().getStepX());
		int dz = Math.abs(this.getFacing().getStepZ());
		double d = 0.2875;
		double d2 = 0.0625;
		return Shapes.box(dz*d+dx*d2, 0, dx*d+dz*d2, 1-dz*d-dx*d2, 1, 1-dx*d-dz*d2);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("volt", Vin);
		NBT.putInt("amp", Ain);
		NBT.putInt("volt2", Vout);
		NBT.putInt("amp2", Aout);
		NBT.putInt("n1", n1);
		NBT.putInt("n2", n2);
		NBT.putInt("face", this.getFacing().ordinal());
		NBT.putInt("temp", temperature);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		Vin = NBT.getInt("volt");
		Ain = NBT.getInt("amp");
		Vout = NBT.getInt("volt2");
		Aout = NBT.getInt("amp2");
		n1 = Math.max(1, NBT.getInt("n1"));
		n2 = Math.max(1, NBT.getInt("n2"));
		facing = NBT.contains("face") ? Direction.values()[NBT.getInt("face")] : null;
		temperature = NBT.getInt("temp");
	}
}
