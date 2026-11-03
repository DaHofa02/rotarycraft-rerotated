/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.electricraft.network.WireNetwork;
import reika.electricraft.network.WireReceiver;
import reika.electricraft.registry.ElectriBlockEntities;
import reika.electricraft.registry.ElectriComponents;
import reika.rotarycraft.api.power.PowerTracker;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.lib.math.BlackbodyColor;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.ElectriCraft.TileEntities.TileEntityMotor - takes current on its facing side and drives the machine on the opposite
 * side with torque = 8·current and omega = terminal voltage/8. With several sources, one motor only draws its share; an amplifier
 * (red gold ingot x2, tungsten x4, bedrock x1000; right-click) lets it draw more. Its fins glow with the power; wool muffles it.
 */
public class MotorBlockEntity extends NetworkBlockEntity implements WireReceiver, ShaftPowerEmitter, PowerSourceTracker, ShaftMerger {

	private static final int soundtime = (int)(EngineType.DC.getSoundLength()*2.04F);
	private int soundTimer = 0;

	protected int omega;
	protected int torque;
	protected long power;
	protected int iotick;
	@Nullable
	private Direction facing;
	private int maxAmp = 1;
	private float sourceSize;

	public MotorBlockEntity(BlockPos pos, BlockState state) {
		super(ElectriBlockEntities.MOTOR.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Electric Motor";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		super.updateEntity(world, pos, state);
		if (iotick > 0)
			iotick -= 8;
		if (!world.isClientSide && network != null) {
			torque = this.getEffectiveCurrent(world, pos)*WireNetwork.TORQUE_PER_AMP;
			omega = network.getTerminalVoltage(this)/WireNetwork.TORQUE_PER_AMP;
		}
		power = (long)omega*(long)torque;
		if (power > 0) {
			soundTimer++;
			if (soundTimer >= soundtime) {
				soundTimer = 0;
				if (!world.isClientSide)
					RotarySounds.play(world, pos, RotarySounds.ELECTRIC.get(), this.getSoundVolume(world, pos), 0.333F);
			}
		}
		else {
			torque = omega = 0;
		}
		if (world.isClientSide)
			return;
		BlockEntity tg = this.getAdjacentTileEntity(this.getFacing().getOpposite());
		if (tg instanceof ShaftPowerReceiver rec) {
			rec.setOmega(omega);
			rec.setTorque(torque);
			rec.setPower(power);
		}
	}

	@Override
	public void onNetworkChanged() {
		PowerSourceList pwr = this.getPowerSources(this, null);
		sourceSize = Math.max(network.getNumberSourcesPer(this), pwr.isBedrock() ? 1 : pwr.size()/(float)Math.max(1, network.getNumberSinks()));
	}

	private int getEffectiveCurrent(Level world, BlockPos pos) {
		int in = network.getTerminalCurrent(this);
		if (network.getNumberSources() <= 1)
			return in;
		float f = Math.min(1, maxAmp/sourceSize);
		int curlim = Math.min(in, network.getTotalCurrent()/network.getNumberSources()*maxAmp);
		if ((sourceSize > maxAmp || in > curlim) && in > 0) {
			if (rand.nextInt(10) == 0) {
				world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
				if (world instanceof ServerLevel sl)
					sl.sendParticles(ParticleTypes.SMOKE, pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5, 1, 0.2, 0, 0.2, 0);
			}
		}
		return (int)(curlim*f);
	}

	/** Original upgrade: the amplifier only ever goes up. */
	public boolean upgrade(ItemStack is) {
		boolean flag = false;
		if (is.is(RotaryItems.part(CraftingPart.REDGOLDINGOT)) && maxAmp < 2) {
			maxAmp = 2;
			flag = true;
		}
		else if (is.is(RotaryItems.part(CraftingPart.TUNGSTENINGOT)) && maxAmp < 4) {
			maxAmp = 4;
			flag = true;
		}
		else if (is.is(RotaryItems.part(CraftingPart.BEDINGOT)) && maxAmp < 1000) {
			maxAmp = 1000;
			flag = true;
		}
		if (flag && network != null)
			network.updateWires();
		return flag;
	}

	public int getMaxAmp() {
		return maxAmp;
	}

	/** Original getSoundVolume: muffled (0.1) when wrapped in wool above, below and on both free sides. */
	private float getSoundVolume(Level world, BlockPos pos) {
		if (world.getBlockState(pos.below()).is(BlockTags.WOOL) && world.getBlockState(pos.above()).is(BlockTags.WOOL))
			return 0.1F;
		Direction dir = this.getFacing();
		for (Direction side : Direction.values()) {
			if (side != dir && side != dir.getOpposite() && side != Direction.DOWN) {
				if (!world.getBlockState(pos.relative(side)).is(BlockTags.WOOL))
					return 0.36F;
			}
		}
		return 0.1F;
	}

	public final Direction getFacing() {
		return facing != null ? facing : Direction.EAST;
	}

	public void setFacing(Direction dir) {
		facing = dir;
		this.setChanged();
	}

	public void incrementFacing() {
		int o = this.getFacing().ordinal();
		this.setFacing(o == 5 ? Direction.values()[2] : Direction.values()[o+1]);
		this.rebuildNetwork();
		this.syncAllData();
	}

	@Override
	public final int getOmega() {
		return omega;
	}

	@Override
	public final int getTorque() {
		return torque;
	}

	@Override
	public final long getPower() {
		return power;
	}

	@Override
	public final int getIORenderAlpha() {
		return iotick;
	}

	@Override
	public final void setIORenderAlpha(int io) {
		iotick = io;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public boolean canNetworkOnSide(Direction dir) {
		return dir == this.getFacing();
	}

	@Override
	public boolean canWriteTo(Direction from) {
		return from == this.getFacing().getOpposite();
	}

	@Override
	public boolean isEmitting() {
		return true;
	}

	@Override
	public boolean canReceivePowerFromSide(Direction dir) {
		return this.canNetworkOnSide(dir);
	}

	@Override
	public boolean canReceivePower() {
		return true;
	}

	@Override
	public BlockPos getEmittingPos() {
		return worldPosition.relative(this.getFacing().getOpposite());
	}

	@Override
	public long getMaxPower() {
		return power;
	}

	@Override
	public long getCurrentPower() {
		return power;
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		return network != null ? network.getInputSources(io, caller, this) : new PowerSourceList();
	}

	@Override
	public BlockPos getIoOffset() {
		return BlockPos.ZERO;
	}

	@Override
	public void onPowerLooped(PowerTracker pwr) {
		if (power > 0)
			this.fail();
	}

	@Override
	public void fail() {
		level.removeBlock(worldPosition, false);
	}

	/** Original getFinColor: blackbody colour of 400*log2(power)-6500 K, half mixed with the fin grey. */
	public int getFinColor() {
		if (level == null)
			return 0x515168;
		int c = BlackbodyColor.getColorForTemperature(400*ReikaMathLibrary.logbase2(power)-6500);
		return c == 0 ? 0x515168 : BlackbodyColor.mixColors(c, 0x515168, 0.5F);
	}

	@Override
	public final void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		c.add(this.getAdjacentTileEntity(this.getFacing().getOpposite()));
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		Integer amp = input.get(ElectriComponents.MOTOR_AMP.get());
		if (amp != null)
			maxAmp = amp;
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (maxAmp > 1)
			components.set(ElectriComponents.MOTOR_AMP.get(), maxAmp);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("face", this.getFacing().ordinal());
		NBT.putInt("omg", omega);
		NBT.putInt("tq", torque);
		NBT.putLong("pwr", power);
		NBT.putInt("amp", maxAmp);
		NBT.putInt("io", iotick);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		facing = NBT.contains("face") ? Direction.values()[NBT.getInt("face")] : null;
		omega = NBT.getInt("omg");
		torque = NBT.getInt("tq");
		power = NBT.getLong("pwr");
		maxAmp = Math.max(1, NBT.getInt("amp"));
		iotick = NBT.getInt("io");
	}
}
