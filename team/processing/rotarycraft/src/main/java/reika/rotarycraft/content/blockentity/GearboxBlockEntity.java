/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.api.interfaces.ComplexIO;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.lib.math.ReikaEngLibrary;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntityGearbox.
 * Ratio is 2^(1+meta/4); meta%4 is the 4-sided direction, exactly as in the original. Not ported: the Botania mana receiver
 * (living gearboxes are not ported), the ChromatiCraft repair hook and the DAMAGEGEARS achievement (comes with the advancements).
 */
public class GearboxBlockEntity extends OneDTransmitterBlockEntity implements TemperatureMachine, reika.rotarycraft.content.piping.PipeConnector {

	public boolean reduction = true; // Reduction gear if true, accelerator if false

	private int damage = 0;

	private GearboxTypes type = GearboxTypes.WOOD;

	private final FluidTank tank = new FluidTank(24000, fs -> fs.is(RotaryFluids.LUBRICANT.get()));
	private boolean failed;

	private int temperature;
	private int tempTimer;

	private GearboxTypes bearingTier = GearboxTypes.WOOD;

	private static final int MAX_DAMAGE = 480;

	public static final double BEARINGREDUCTION = 0.25;
	public static final double BEARINGINCREASE = 1.0;

	public GearboxBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.GEARBOX.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Gearbox";
	}

	public GearboxTypes getGearboxType() {
		return type;
	}

	public GearboxTypes getBearingTier() {
		return bearingTier != null ? bearingTier : type;
	}

	public void setBearingTier(GearboxTypes tier) {
		bearingTier = tier;
		this.syncAllData();
	}

	private int getBearingTierOffset() {
		return this.getBearingTier().material.ordinal()-type.material.ordinal();
	}

	public int getMaxLubricant() {
		return type.getMaxLubricant();
	}

	public int getDamage() {
		return damage;
	}

	public double getDamagedPowerFactor() {
		return Math.pow(0.99, damage);
	}

	public int getDamagePercent() {
		return getDamagePercent(damage);
	}

	@Override
	protected void readFromSplitter(Level world, BlockPos pos, SplitterBlockEntity spl) { //Complex enough to deserve its own function
		int sratio = spl.getRatioFromMode();
		if (sratio == 0)
			return;
		boolean favorbent = false;
		if (sratio < 0) {
			favorbent = true;
			sratio = -sratio;
		}
		if (reduction) {
			if (spl.isInlineTarget(pos)) { //We are the inline
				omega = spl.omega/ratio; //omega always constant
				if (sratio == 1) { //Even split, favorbent irrelevant
					torque = spl.torque/2*ratio;
				}
				else if (favorbent) {
					torque = spl.torque/sratio*ratio;
				}
				else {
					torque = ratio*(int)(spl.torque*((sratio-1D)/(sratio)));
				}
			}
			else if (spl.isBendTarget(pos)) { //We are the bend
				omega = spl.omega/ratio; //omega always constant
				if (sratio == 1) { //Even split, favorbent irrelevant
					torque = spl.torque/2*ratio;
				}
				else if (favorbent) {
					torque = ratio*(int)(spl.torque*((sratio-1D)/(sratio)));
				}
				else {
					torque = spl.torque/sratio*ratio;
				}
			}
			else { //We are not one of its write-to blocks
				torque = 0;
				omega = 0;
				power = 0;
			}
		}
		else {
			if (spl.isInlineTarget(pos)) { //We are the inline
				omega = spl.omega*ratio; //omega always constant
				if (sratio == 1) { //Even split, favorbent irrelevant
					torque = spl.torque/2/ratio;
				}
				else if (favorbent) {
					torque = spl.torque/sratio/ratio;
				}
				else {
					torque = (int)(spl.torque*((sratio-1D))/sratio)/(ratio);
				}
			}
			else if (spl.isBendTarget(pos)) { //We are the bend
				omega = spl.omega*ratio; //omega always constant
				if (sratio == 1) { //Even split, favorbent irrelevant
					torque = spl.torque/2/ratio;
				}
				else if (favorbent) {
					torque = (int)(spl.torque*((sratio-1D)/(sratio)))/ratio;
				}
				else {
					torque = spl.torque/sratio/ratio;
				}
			}
			else { //We are not one of its write-to blocks
				torque = 0;
				omega = 0;
				power = 0;
			}
		}
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		tickcount++;
		this.getIOSides(world, pos, this.getBlockMetadata());

		if ((world.getGameTime()&31) == 0)
			world.updateNeighborsAt(pos, state.getBlock());

		this.transferPower(world, pos, this.getBlockMetadata());
		power = (long)omega*(long)torque;
		this.getLubeAndApplyDamage(world, pos);
		tempTimer++;
		if (tempTimer >= 20) {
			tempTimer = 0;
			this.updateTemperature(world, pos);
		}

		this.basicPowerReceiver();
	}

	private void getLubeAndApplyDamage(Level world, BlockPos pos) {
		if (type.needsLubricant() && omega > 0 && this.getBearingTier().material.ordinal() < MaterialRegistry.BEDROCK.ordinal()) {
			if (tank.isEmpty()) {
				if (!world.isClientSide && damage < MAX_DAMAGE && rand.nextInt(40) == 0 && this.getTicksExisted() >= 100) {
					damage++;
				}
				if (rand.nextDouble()*rand.nextDouble() > this.getDamagedPowerFactor()) {
					if (type.material.isFlammable() && !world.isClientSide)
						ignite(world, pos);
					if (world instanceof ServerLevel sl)
						sl.sendParticles(ParticleTypes.CRIT, pos.getX()+rand.nextFloat(), pos.getY()+rand.nextFloat(), pos.getZ()+rand.nextFloat(), 1, -0.5+rand.nextFloat(), rand.nextFloat(), -0.5+rand.nextFloat(), 0);
					if (rand.nextInt(5) == 0) {
						world.playSound(null, pos, MachineSounds.getDamageNoise(type.material), SoundSource.BLOCKS, 1F, 1F);
					}
				}
			}
			else if (!world.isClientSide && this.consumesLubricant()) {
				if (tickcount >= 80) {
					tank.drain(Math.max(1, (int)(DifficultyEffects.LUBEUSAGE.getChance()*this.getLubricantConsumptionFactor())), IFluidHandler.FluidAction.EXECUTE);
					tickcount = 0;
				}
			}
		}
	}

	/** Original: ReikaWorldHelper.ignite - sets fire to an adjacent air block. */
	static void ignite(Level world, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			BlockPos p = pos.relative(dir);
			if (world.isEmptyBlock(p)) {
				world.setBlock(p, BaseFireBlock.getState(world, p), Block.UPDATE_ALL);
				return;
			}
		}
	}

	public boolean consumesLubricant() {
		return type.consumesLubricant() && this.getBearingTier().material.ordinal() < MaterialRegistry.DIAMOND.ordinal();
	}

	private double getLubricantConsumptionFactor() {
		double base = type.getLubricantConsumeRate(omegain)*ReikaMathLibrary.logbase(omegain, 2)/4;
		if (type != this.getBearingTier()) {
			base *= this.getBearingLubricantFactor();
		}
		return base;
	}

	public double getBearingLubricantFactor() {
		if (!this.getBearingTier().needsLubricant())
			return -1;
		if (!this.getBearingTier().consumesLubricant())
			return 0;
		int offset = this.getBearingTierOffset();
		double add = offset > 0 ? -BEARINGREDUCTION*offset : -offset*BEARINGINCREASE;
		return Math.max(0.1, 1+add);
	}

	public void getIOSides(Level world, BlockPos pos, int metadata) {
		while (metadata > 3)
			metadata -= 4;
		super.getIOSides(world, pos, metadata, false);
	}

	/** The ratio encoded in the orientation metadata, valid before the first tick. */
	public int getRatioFromMetadata() {
		return (int)ReikaMathLibrary.intpow(2, 1+this.getBlockMetadata()/4);
	}

	private void calculateRatio() {
		int tratio = 1+this.getBlockMetadata()/4;
		ratio = (int)ReikaMathLibrary.intpow(2, tratio);
	}

	@Override
	protected void readFromCross(ShaftBlockEntity cross) {
		if (cross.isWritingTo(this)) {
			if (reduction) {
				omegain = cross.readomega[0]/ratio;
				torquein = cross.readtorque[0]*ratio;
			}
			else {
				omegain = cross.readomega[0]*ratio;
				torquein = cross.readtorque[0]/ratio;
			}
		}
		else if (cross.isWritingTo2(this)) {
			if (reduction) {
				omegain = cross.readomega[1]/ratio;
				torquein = cross.readtorque[1]*ratio;
			}
			else {
				omegain = cross.readomega[1]*ratio;
				torquein = cross.readtorque[1]/ratio;
			}
		}
		else {
			omegain = torquein = 0;
		}
	}

	@Override
	protected void transferPower(Level world, BlockPos pos, int meta) {
		this.calculateRatio();
		if (world.isClientSide)
			return;
		performRatio = true;
		omegain = torquein = 0;
		BlockEntity te = this.getAdjacentTileEntity(read);
		if (this.isProvider(te)) {
			if (te instanceof ShaftBlockEntity devicein) {
				if (devicein.isCross()) {
					this.readFromCross(devicein);
					performRatio = false;
				}
				else if (devicein.isWritingTo(this)) {
					torquein = devicein.torque;
					omegain = devicein.omega;
				}
			}
			else if (te instanceof SimpleProvider) {
				this.copyStandardPower(te);
			}
			else if (te instanceof ComplexIO pwr) {
				Direction dir = this.getInputForgeDirection().getOpposite();
				omegain = pwr.getSpeedToSide(dir);
				torquein = pwr.getTorqueToSide(dir);
			}
			else if (te instanceof ShaftPowerEmitter sp) {
				if (sp.isEmitting() && sp.canWriteTo(read.getOpposite())) {
					torquein = sp.getTorque();
					omegain = sp.getOmega();
				}
			}
			else if (te instanceof SplitterBlockEntity devicein) {
				if (devicein.isSplitting()) {
					this.readFromSplitter(world, pos, devicein);
					performRatio = false;
				}
				else if (devicein.isWritingTo(this)) {
					torquein = devicein.torque;
					omegain = devicein.omega;
				}
			}
		}
		else {
			omega = 0;
			torque = 0;
			power = 0;
			return;
		}

		if (performRatio) {
			if (reduction) {
				omega = omegain / ratio;
				if (torquein <= RotaryConfig.torquelimit/ratio)
					torque = torquein * ratio;
				else {
					torque = RotaryConfig.torquelimit;
					this.overloadEffect(world, pos);
				}
			}
			else {
				if (omegain <= RotaryConfig.omegalimit/ratio)
					omega = omegain * ratio;
				else {
					omega = RotaryConfig.omegalimit;
					this.overloadEffect(world, pos);
				}
				torque = torquein / ratio;
			}
		}
		torque *= this.getDamagedPowerFactor();
		int tempEff = temperature+Math.max(0, world.getBrightness(LightLayer.BLOCK, pos)-10);
		if (torque*(long)omega >= 131072L) {
			tempEff += ReikaMathLibrary.logbase2(torque*(long)omega/131072L);
		}
		if (tempEff <= (type.material == MaterialRegistry.WOOD ? -15 : -20)) {
			if (type.needsLubricant()) {
				double c = -(tempEff+20)/40D;
				omega /= Math.pow(1.4, c);
			}
			else if (type.material == MaterialRegistry.WOOD) {
				double c = Math.min(1, ((-tempEff)-15)*0.025);
				if (c > 0 && rand.nextDouble() < c) {
					damage++;
				}
			}
		}
		if (torque <= 0)
			omega = 0;

		if (!type.material.isInfiniteStrength())
			this.testFailure();
	}

	private void overloadEffect(Level world, BlockPos pos) {
		if (world instanceof ServerLevel sl)
			sl.sendParticles(ParticleTypes.CRIT, pos.getX()+rand.nextFloat(), pos.getY()+rand.nextFloat(), pos.getZ()+rand.nextFloat(), 1, -0.5+rand.nextFloat(), rand.nextFloat(), -0.5+rand.nextFloat(), 0);
		world.playSound(null, pos, MachineSounds.getDamageNoise(type.material), SoundSource.BLOCKS, 0.1F, 1F);
	}

	public void fail(Level world, BlockPos pos) {
		failed = true;
		world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 1F, Level.ExplosionInteraction.BLOCK);
		ItemStack item = switch(type) {
			case WOOD -> new ItemStack(RotaryItems.part(CraftingPart.SAWDUST));
			case STONE -> new ItemStack(Items.GRAVEL);
			case STEEL, TUNGSTEN -> new ItemStack(RotaryItems.part(CraftingPart.SCRAP));
			case DIAMOND -> new ItemStack(Items.DIAMOND);
			case BEDROCK -> new ItemStack(RotaryItems.part(CraftingPart.BEDROCKDUST));
		};
		for (int i = 0; i < this.getRatio(); i++) {
			Containers.dropItemStack(world, pos.getX()+0.5, pos.getY()+1.25, pos.getZ()+0.5, item.copy());
		}
		world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
	}

	public boolean repair(int dmg) {
		if (damage <= 0)
			return false;
		damage -= dmg;
		if (damage < 0)
			damage = 0;
		failed = false;
		return true;
	}

	public void testFailure() {
		if (ReikaEngLibrary.mat_rotfailure(type.material.rho, 0.0625, type.getOmegaForRotFailure(omega, omegain), type.material.tensile)) {
			this.fail(level, worldPosition);
		}
		else if (ReikaEngLibrary.mat_twistfailure(Math.max(torque, torquein), 0.0625, type.material.shear/16D)) {
			this.fail(level, worldPosition);
		}
	}

	public int getLubricantScaled(int par1) {
		if (this.getMaxLubricant() == 0)
			return 0;
		return tank.getFluidAmount()*par1/this.getMaxLubricant();
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("reduction", reduction);
		NBT.putInt("damage", damage);
		NBT.putBoolean("fail", failed);
		NBT.putInt("temp", temperature);
		NBT.putString("bearing", this.getBearingTier().name());
		NBT.putString("geartype", type.name());
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		reduction = !NBT.contains("reduction") || NBT.getBoolean("reduction");
		damage = NBT.getInt("damage");
		failed = NBT.getBoolean("fail");
		temperature = NBT.getInt("temp");
		if (NBT.contains("bearing"))
			bearingTier = GearboxTypes.valueOf(NBT.getString("bearing"));
		if (NBT.contains("geartype"))
			type = GearboxTypes.valueOf(NBT.getString("geartype"));
		tank.readFromNBT(registries, NBT.getCompound("tank"));
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		type = input.getOrDefault(RotaryComponents.GEARBOX_TYPE.get(), GearboxTypes.WOOD);
		bearingTier = type;
		RotaryComponents.GearboxData data = input.get(RotaryComponents.GEARBOX_DATA.get());
		if (data != null) {
			damage = data.damage();
			this.setLubricant(data.lubricant());
			bearingTier = data.bearing();
		}
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(RotaryComponents.GEARBOX_TYPE.get(), type);
		components.set(RotaryComponents.GEAR_RATIO.get(), this.getRatioFromMetadata());
		components.set(RotaryComponents.GEARBOX_DATA.get(), new RotaryComponents.GearboxData(damage, this.getLubricant(), this.getBearingTier()));
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return this.getMaxLubricant() > 0 ? 15*tank.getFluidAmount()/this.getMaxLubricant() : 0;
	}

	@Override
	public void onEMP() {}

	/** Original canConnectToPipe: hoses and bedrock pipes, on every side but the top (bottom when flipped). */
	@Override
	public boolean canConnectToPipe(reika.rotarycraft.content.piping.PipeType m) {
		return m == reika.rotarycraft.content.piping.PipeType.HOSE || m == reika.rotarycraft.content.piping.PipeType.BEDROCK_PIPE;
	}

	@Override
	public boolean canConnectToPipeOnSide(reika.rotarycraft.content.piping.PipeType p, Direction side) {
		return side != (isFlipped ? Direction.DOWN : Direction.UP);
	}

	@Override
	public reika.rotarycraft.content.piping.PipeConnector.Flow getFlowForSide(Direction side) {
		return side != (isFlipped ? Direction.DOWN : Direction.UP) ? reika.rotarycraft.content.piping.PipeConnector.Flow.INPUT : reika.rotarycraft.content.piping.PipeConnector.Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return this.getFluidHandler(from).fill(resource, action);
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	/** Fluid access from pipes: lubricant only, from any side except the top (or bottom when flipped). */
	public IFluidHandler getFluidHandler(@Nullable Direction from) {
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
				return GearboxBlockEntity.this.getMaxLubricant();
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return stack.is(RotaryFluids.LUBRICANT.get());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				if (from == (isFlipped ? Direction.DOWN : Direction.UP) || !this.isFluidValid(0, resource))
					return 0;
				int space = GearboxBlockEntity.this.getMaxLubricant()-GearboxBlockEntity.this.getLubricant();
				if (space <= 0)
					return 0;
				FluidStack fs = resource.getAmount() > space ? resource.copyWithAmount(space) : resource;
				int filled = tank.fill(fs, action);
				if (filled > 0 && action.execute())
					GearboxBlockEntity.this.setChanged();
				return filled;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return FluidStack.EMPTY;
			}
		};
	}

	public int getLubricant() {
		return tank.getFluidAmount();
	}

	public void setLubricant(int amt) {
		tank.setFluid(amt > 0 ? new FluidStack(RotaryFluids.LUBRICANT.get(), amt) : FluidStack.EMPTY);
	}

	public void fillWithLubricant() {
		this.setLubricant(this.getMaxLubricant());
	}

	public boolean canTakeLubricant(int amt) {
		return tank.getFluidAmount()+amt <= this.getMaxLubricant();
	}

	public void addLubricant(int amt) {
		this.setLubricant(this.getLubricant()+amt);
	}

	public void clearLubricant() {
		tank.setFluid(FluidStack.EMPTY);
	}

	public void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (omega > 0 && type.generatesHeat(omega, Tamb)) {
			temperature++;
			world.playSound(null, pos, MachineSounds.getDamageNoise(type.material), SoundSource.BLOCKS, 0.67F, 1);
		}
		if (temperature > 90 && rand.nextBoolean() && type.takesTemperatureDamage()) {
			damage++;
			world.playSound(null, pos, MachineSounds.getDamageNoise(type.material), SoundSource.BLOCKS, 1, 1);
		}
		if (temperature > Tamb)
			temperature = Math.max(Tamb, temperature-(omega == 0 ? 2 : 1));
		else if (temperature < Tamb)
			temperature = Math.min(Tamb, temperature+3);

		if (temperature > 120) {
			this.overheat(world, pos);
		}
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


	/** Original canBeCooledWithFins. */
	@Override
	public boolean canBeCooledWithFins() {
		return true;
	}

	@Override
	public int getMaxTemperature() {
		return 1000;
	}

	public void overheat(Level world, BlockPos pos) {
		if (type.material.isFlammable() && !world.isClientSide)
			ignite(world, pos);
	}

	public static int getDamagePercent(int val) {
		return (int)(100*(1-Math.pow(0.99, val)));
	}

	@Override
	public boolean allowExternalHeating() {
		return true;
	}
}
