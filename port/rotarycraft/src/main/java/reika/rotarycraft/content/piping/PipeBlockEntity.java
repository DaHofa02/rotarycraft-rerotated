/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.piping;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import reika.rotarycraft.content.blockentity.TemperatureMachine;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.lib.world.TemperatureEnvironment;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * Original: Reika.RotaryCraft.TileEntities.Piping.TileEntityPipe - the liquid pipe. Carries any fluid except fuels and lubricant,
 * takes the temperature of its fluid and melts into lava above {@link #getMaxTemperature()}.
 * Not ported: the uranium hexafluoride / hydrofluoric acid corrosion (ReactorCraft fluids, not ported yet).
 */
public class PipeBlockEntity extends PipingBlockEntity implements TemperatureMachine {

	@Nullable
	private Fluid liquid;
	private int liquidLevel = 0;

	private int temperature;

	public PipeBlockEntity(BlockPos pos, BlockState state) {
		this(RotaryBlockEntities.PIPE.get(), pos, state);
	}

	protected PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		super.updateEntity(world, pos, state);
		if (world.isClientSide || world.getBlockEntity(pos) != this)
			return;

		if (rand.nextInt(60) == 0)
			TemperatureEnvironment.temperatureEnvironment(world, pos, this.getTemperature());

		if (liquid != null) {
			int temp = liquid.getFluidType().getTemperature();
			temperature = temp > 750 ? temp-425 : temp-273;
			if (temperature > this.getMaxTemperature())
				this.overheat(world, pos);
		}
		else {
			temperature = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		}
	}

	@Override
	public int getMaxTemperature() {
		return 2500;
	}

	@Override
	public PipeType getPipeType() {
		return PipeType.PIPE;
	}

	@Override
	protected void onIntake(BlockEntity te) {}

	@Override
	public boolean canConnectToPipe(PipeType p, Direction dir) {
		return p.isStandardPipe() || p == PipeType.VALVE || p == PipeType.SEPARATION || p == PipeType.BYPASS || p == PipeType.SUCTION;
	}

	@Nullable
	@Override
	public Fluid getFluidType() {
		return liquid;
	}

	public boolean contains(@Nullable Fluid f) {
		return f != null && liquid != null && f.isSame(liquid);
	}

	@Override
	protected void setFluid(@Nullable Fluid f) {
		liquid = f;
	}

	@Override
	public int getFluidLevel() {
		return liquidLevel;
	}

	@Override
	protected void setLevel(int amt) {
		liquidLevel = amt;
	}

	@Override
	protected boolean interactsWithMachines() {
		return true;
	}

	/** Original: everything but the fuels, lubricant and gases of RotaryCraft/ReactorCraft; only the RotaryCraft ones exist so far. */
	@Override
	public boolean isValidFluid(Fluid f) {
		return !f.isSame(RotaryFluids.JET_FUEL.get()) && !f.isSame(RotaryFluids.LUBRICANT.get()) && !f.isSame(RotaryFluids.ETHANOL.get());
	}

	@Override
	public boolean canReceiveFromPipeOn(Direction side) {
		return true;
	}

	@Override
	public boolean canEmitToPipeOn(Direction side) {
		return true;
	}

	@Override
	public boolean canIntakeFromIFluidHandler(Direction side) {
		return side.getAxis() == Direction.Axis.Y;
	}

	@Override
	public boolean canOutputToIFluidHandler(Direction side) {
		return side.getAxis() != Direction.Axis.Y;
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

	public int getThermalDamage() {
		return this.getTemperature() >= 100 ? this.getTemperature()/400 : 0;
	}

	/** Original overheat: the whole connected run of this pipe type turns into lava. */
	public void overheat(Level world, BlockPos pos) {
		Block b = this.getBlockState().getBlock();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(pos);
		seen.add(pos);
		while (!queue.isEmpty()) {
			BlockPos c = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos n = c.relative(d);
				if (!seen.contains(n) && world.isLoaded(n) && world.getBlockState(n).is(b)) {
					seen.add(n);
					queue.add(n);
				}
			}
		}
		for (BlockPos c : seen) {
			world.playSound(null, c, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1);
			if (world instanceof ServerLevel sl)
				sl.sendParticles(ParticleTypes.LAVA, c.getX()+0.5, c.getY()+0.5, c.getZ()+0.5, 36, 0.5, 0.5, 0.5, 0);
			world.setBlockAndUpdate(c, Blocks.LAVA.defaultBlockState());
		}
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("temp", temperature);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		temperature = NBT.getInt("temp");
	}
}
