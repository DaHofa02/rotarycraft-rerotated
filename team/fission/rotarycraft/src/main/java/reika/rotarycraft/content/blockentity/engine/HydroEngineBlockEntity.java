/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.engine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryDamage;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Engine.TileEntityHydroEngine. Runs on a falling column of fluid beside its paddles,
 * needs lubricant, can be chained into arrays (torque adds up) and breaks above 4x (bedrock: 16x) its rated torque.
 * Not ported: the stream mode (needs the Streams mod's FixedFlowBlock, not available for 1.21.1) and the Galacticraft gravity bonus.
 */
public class HydroEngineBlockEntity extends EngineBlockEntity {

	public boolean failed;
	private boolean bedrock;
	private FluidType fluidType;
	private double fluidFallSpeed;

	/** Original: ReikaPhysicsHelper.g */
	private static final double GRAVITY = 9.81D;

	public HydroEngineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.HYDRO_ENGINE.get(), pos, state, EngineType.HYDRO);
	}

	@Override
	protected void consumeFuel(float scale) {
	}

	@Override
	protected void internalizeFuel() {
	}

	public boolean isBedrock() {
		return bedrock;
	}

	@Override
	protected boolean getRequirements(Level world, BlockPos pos, int meta) {
		boolean hasLube = !lubricant.isEmpty();
		if (hasLube)
			this.distributeLubricant(world, pos);
		else
			return false;
		if (this.doesBlockObstructBlades(world, pos.above())) {
			omega = 0;
			return false;
		}
		if (this.doesBlockObstructBlades(world, pos.below())) {
			omega = 0;
			return false;
		}
		BlockPos col = this.getWaterColumnPos();
		for (int d = -1; d <= 1; d++) {
			for (int i = -1; i <= 1; i++) {
				if (i == 0 && d == 0)
					continue;
				BlockPos p = new BlockPos(pos.getX()+(col.getX()-pos.getX())*d, pos.getY()+i, pos.getZ()+(col.getZ()-pos.getZ())*d);
				if (this.doesBlockObstructBlades(world, p)) {
					omega = 0;
					return false;
				}
			}
		}
		if (!isLiquidAColumn(world, col))
			return false;
		this.getFluidData(world, pos, col);
		if (fluidType != null) {
			if (fluidType.getTemperature() >= 900) {
				if (rand.nextDouble() < 0.02) { //Original: ReikaRandomHelper.doWithChance(2), i.e. 2 %
					world.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
					boolean lube = !lubricant.isEmpty();
					world.explode(null, null, null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, lube ? 3 : 2, lube, Level.ExplosionInteraction.BLOCK);
				}
			}
			if (fluidType.isLighterThanAir() || fluidType.getDensity() <= 0)
				return false;
		}
		return true;
	}

	/** Original: ReikaWorldHelper.isLiquidAColumn - flowing (non-source) fluid here, above and below. */
	private static boolean isLiquidAColumn(Level world, BlockPos p) {
		FluidState f = world.getFluidState(p);
		if (f.isEmpty() || f.isSource())
			return false;
		FluidState up = world.getFluidState(p.above());
		FluidState down = world.getFluidState(p.below());
		return up.getFluidType() == f.getFluidType() && !up.isSource() && down.getFluidType() == f.getFluidType() && !down.isSource();
	}

	private void distributeLubricant(Level world, BlockPos pos) {
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			if (dir == this.getWriteDirection() || dir.getOpposite() == this.getWriteDirection()) {
				BlockEntity te = this.getAdjacentTileEntity(dir);
				if (te instanceof HydroEngineBlockEntity hy) {
					int it = hy.lubricant.getFluidAmount();
					int dL = lubricant.getFluidAmount()-it;
					if (dL > 3) {
						hy.addLubricant(dL/4);
						lubricant.drain(dL/4, IFluidHandler.FluidAction.EXECUTE);
					}
				}
				//Original: also fills an adjacent Reservoir (ported with the Reservoir, step 4)
			}
		}
		if (!failed && !lubricant.isEmpty() && omega > 0) {
			if (world.getGameTime()%10 == 0)
				lubricant.drain(1, IFluidHandler.FluidAction.EXECUTE);
		}
	}

	private boolean doesBlockObstructBlades(Level world, BlockPos p) {
		return !failed && !world.getBlockState(p).canBeReplaced();
	}

	private BlockPos getWaterColumnPos() {
		return switch(this.getBlockMetadata()) {
			case 0 -> worldPosition.offset(0, 0, -1);
			case 1 -> worldPosition.offset(0, 0, 1);
			case 2 -> worldPosition.offset(1, 0, 0);
			case 3 -> worldPosition.offset(-1, 0, 0);
			default -> worldPosition;
		};
	}

	/** Original: ReikaWorldHelper.findFluidSurface - first y above the top fluid block of the column. */
	private static int findFluidSurface(Level world, BlockPos p, FluidType type) {
		int dy = p.getY();
		while (dy < world.getMaxBuildHeight() && world.getFluidState(new BlockPos(p.getX(), dy, p.getZ())).getFluidType() == type)
			dy++;
		return dy;
	}

	private void getFluidData(Level world, BlockPos pos, BlockPos col) {
		FluidState fs = world.getFluidState(col);
		FluidType f = fs.isEmpty() ? null : fs.getFluidType();
		fluidType = f;
		if (f == null || f.isLighterThanAir() || f.getDensity() <= 0) {
			fluidFallSpeed = 0;
			return;
		}
		int top = findFluidSurface(world, col, f);
		double dy = top-pos.getY();
		dy = Math.pow(dy, 1.5)/32;
		fluidFallSpeed = 0.92*Math.sqrt(2*GRAVITY*dy)/Math.max(0.25, Math.pow(f.getViscosity()/1000, 0.375));
	}

	public int getWaterfallHeightForDisplay() {
		BlockPos col = this.getWaterColumnPos();
		FluidState fs = level.getFluidState(col);
		if (fs.isEmpty() || fs.getFluidType().isLighterThanAir() || fs.getFluidType().getDensity() <= 0)
			return 0;
		return Mth.ceil(findFluidSurface(level, col, fs.getFluidType()))-worldPosition.getY();
	}

	private int getEffectiveSpeed() {
		double omg = fluidFallSpeed*2;
		return Math.min((int)omg, type.getSpeed());
	}

	private int getEffectiveTorque() {
		double mdot = Math.min(12000, fluidType.getDensity())*fluidFallSpeed; //*1 since area is 1m^2
		double tau = 0.0625*mdot*fluidFallSpeed;
		return Math.min((int)tau, type.getTorque());
	}

	private void dealPanelDamage(Level world, BlockPos pos, int meta) {
		int a = 0;
		int b = 0;
		if (meta < 2)
			b = 1;
		else
			a = 1;
		AABB box = new AABB(pos).inflate(a, 1, b);
		for (LivingEntity ent : world.getEntitiesOfClass(LivingEntity.class, box)) {
			ent.hurt(RotaryDamage.source(world, RotaryDamage.HYDROKINETIC), 1);
		}
	}

	private boolean isPartOfArray() {
		return this.isBackEndOfArray() || this.isFrontOfArray();
	}

	public boolean isReversed() {
		return false; //only stream-powered engines (not ported) can run reversed
	}

	public boolean isBackEndOfArray() {
		return this.getAdjacentTileEntity(write) instanceof HydroEngineBlockEntity te && !te.failed;
	}

	public boolean isFrontOfArray() {
		BlockEntity from = level.getBlockEntity(this.getBackPos());
		BlockEntity to = this.getAdjacentTileEntity(write);
		if (from instanceof HydroEngineBlockEntity && !(to instanceof EngineBlockEntity))
			return true;
		return to instanceof HydroEngineBlockEntity te && te.failed;
	}

	private int getArrayTorqueMultiplier() {
		List<HydroEngineBlockEntity> li = new ArrayList<>();
		int size = 1;
		BlockEntity te = this.getAdjacentTileEntity(write.getOpposite());
		while (te instanceof HydroEngineBlockEntity eng && te != this && !li.contains(eng)) {
			li.add(eng);
			if (eng.getRequirements(level, eng.getBlockPos(), eng.getBlockMetadata())) {
				if (eng.omega == omega && !eng.failed) {
					size++;
					te = eng.getAdjacentTileEntity(eng.write.getOpposite());
				}
				else {
					if (level instanceof ServerLevel sl)
						sl.sendParticles(ParticleTypes.CRIT, eng.getBlockPos().getX()+0.5, eng.getBlockPos().getY()+0.5, eng.getBlockPos().getZ()+0.5, 5, 0.5, 0.5, 0.5, 0);
					if (rand.nextInt(3) == 0)
						level.playSound(null, eng.getBlockPos(), SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 1, 1);
					break;
				}
			}
		}
		return size;
	}

	@Override
	protected void playSounds(Level world, BlockPos pos, float pitchMultiplier, float volume) {
		soundtick++;
		if (this.isMuffled(world, pos)) {
			volume *= 0.3125F;
		}
		if (soundtick < this.getSoundLength(1F/pitchMultiplier) && soundtick < 2000)
			return;
		soundtick = 0;
		if (this.isFrontOfArray() || !this.isPartOfArray())
			RotarySounds.play(world, pos, RotarySounds.HYDRO.get(), 1F*volume, 0.9F*pitchMultiplier);
	}

	@Override
	public int getFuelLevel() {
		return 0;
	}

	@Override
	protected int getMaxSpeed(Level world, BlockPos pos, int meta) {
		return Math.max(1, this.getEffectiveSpeed());
	}

	@Override
	protected int getGenTorque(Level world, BlockPos pos, int meta) {
		if (failed)
			return 1;
		int torque = this.getEffectiveTorque()*this.getArrayTorqueMultiplier();
		double ratio = (double)torque/EngineType.HYDRO.getTorque();
		int r = bedrock ? 16 : 4;
		if (ratio > r) {
			this.fail(world, pos);
		}
		return torque;
	}

	private void fail(Level world, BlockPos pos) {
		world.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1, 1);
		world.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.2F, 0.5F);
		failed = true;
	}

	@Override
	protected void affectSurroundings(Level world, BlockPos pos, int meta) {
		this.dealPanelDamage(world, pos, meta);
		this.spawnParticles(world, pos);
		if (failed && write != null) {
			Direction left = write.getCounterClockWise();
			for (int a = -1; a <= 1; a++) {
				for (int h = -1; h <= 1; h++) {
					if (a == 0 && h == 0)
						continue;
					BlockPos p = pos.relative(left, a).above(h);
					BlockState b = world.getBlockState(p);
					if (!b.isAir() && b.getFluidState().isEmpty())
						world.destroyBlock(p, true);
				}
			}
		}
	}

	private void spawnParticles(Level world, BlockPos pos) {
		if (!(world instanceof ServerLevel sl))
			return;
		BlockPos col = this.getWaterColumnPos();
		BlockPos opp = new BlockPos(2*pos.getX()-col.getX(), pos.getY(), 2*pos.getZ()-col.getZ());
		for (BlockPos p : new BlockPos[] {pos, opp, col})
			sl.sendParticles(ParticleTypes.RAIN, p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, 16, 0.5, 0.5, 0.5, 0);
		if (failed) {
			if (rand.nextInt(5) == 0)
				world.playSound(null, pos, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 1, 1);
			sl.sendParticles(ParticleTypes.CRIT, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 3, 0.75, 0.75, 0.75, 0);
		}
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		failed = NBT.getBoolean("fail");
		bedrock = NBT.getBoolean("bedrock");
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("fail", failed);
		NBT.putBoolean("bedrock", bedrock);
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		bedrock = input.getOrDefault(RotaryComponents.BEDROCK_HYDRO.get(), false);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (bedrock)
			components.set(RotaryComponents.BEDROCK_HYDRO.get(), true);
	}

	public void makeBedrock() {
		if (!bedrock) {
			bedrock = true;
			failed = false;
			this.syncAllData();
		}
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		PowerSourceList psl = super.getPowerSources(io, caller);
		List<HydroEngineBlockEntity> li = new ArrayList<>();
		List<HydroEngineBlockEntity> li2 = new ArrayList<>();
		BlockEntity te = this.getAdjacentTileEntity(write.getOpposite());
		while (te instanceof HydroEngineBlockEntity eng && te != this && !li2.contains(eng)) {
			li2.add(eng);
			if (eng.getRequirements(level, eng.getBlockPos(), eng.getBlockMetadata()) && eng.omega == omega && !eng.failed) {
				li.add(eng);
				te = eng.getAdjacentTileEntity(eng.write.getOpposite());
			}
			else {
				break;
			}
		}
		for (HydroEngineBlockEntity e : li)
			psl.addSource(e);
		return psl;
	}
}
