/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.reactor.NeutronTile;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.api.interfaces.Shockable;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.entity.DischargeEntity;
import reika.rotarycraft.content.piping.FluidPipeView;
import reika.rotarycraft.content.piping.PipeConnector;

/**
 * Original: Reika.ReactorCraft.TileEntities.TileEntityMagneticPipe with TileEntityReactorPiping - a gold and blast glass duct for fusion
 * plasma between the heater, the injectors and other magnetic pipes. It moves fluid like the other reactor pipes (a quarter plus one of
 * the difference each tick). It must be charged by Van de Graaff discharges (at least 256 C; it gains charge^1.1): charge spreads to
 * neighbouring magnetic pipes (a quarter of the difference each tick) and drops by 1 % per tick; water beside it takes all of it in a
 * spark. An uncharged pipe holding hot fluid (plasma) melts into lava. It glows while charged.
 */
public class MagneticPipeBlockEntity extends RotaryCraftBlockEntity implements FluidPipeView, Shockable, NeutronTile {

	public static final net.minecraft.world.level.block.state.properties.BooleanProperty CHARGED =
			net.minecraft.world.level.block.state.properties.BooleanProperty.create("charged");

	@Nullable
	private Fluid fluid;
	private int level;
	private int charge;

	public MagneticPipeBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.MAGNETIC_PIPE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Magnetic Pipe";
	}

	public static boolean isValidFluid(Fluid f) {
		return f.isSame(ReactorFluids.get("fusion_plasma"));
	}

	/** Original isPlasmaAcceptingBlock. */
	public static boolean connectsTo(@Nullable BlockEntity te) {
		return te instanceof MagneticPipeBlockEntity || te instanceof FusionHeaterBlockEntity || te instanceof FusionInjectorBlockEntity;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		Fluid f = fluid;
		this.intakeFluid(world, pos);
		if (level <= 0) {
			level = 0;
			fluid = null;
		}
		else {
			this.dumpContents(world, pos);
		}
		if (f != fluid)
			this.syncAllData();

		this.distributeCharge(world, pos);
		this.updateCharge(world, pos);

		if (charge <= 0) {
			charge = 0;
			if (fluid != null && fluid.getFluidType().getTemperature() > 5000) {
				world.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
				world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F+(rand.nextFloat()-rand.nextFloat())*0.8F);
				if (world instanceof ServerLevel sl)
					sl.sendParticles(ParticleTypes.LAVA, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 5, 0.5, 0.5, 0.5, 0);
				return;
			}
		}
		boolean glow = charge > 0;
		if (state.hasProperty(CHARGED) && state.getValue(CHARGED) != glow)
			world.setBlock(pos, state.setValue(CHARGED, glow), 2);
	}

	private void distributeCharge(Level world, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			if (world.getBlockEntity(pos.relative(dir)) instanceof MagneticPipeBlockEntity tile) {
				int dq = charge-tile.charge;
				if (dq > 0) {
					tile.charge += dq/4;
					charge -= dq/4;
				}
			}
		}
	}

	private void updateCharge(Level world, BlockPos pos) {
		Direction dir = null;
		for (Direction d : Direction.values()) {
			if (world.getFluidState(pos.relative(d)).is(FluidTags.WATER)) {
				dir = d;
				break;
			}
		}
		if (dir != null) {
			world.addFreshEntity(new DischargeEntity(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, charge,
					pos.getX()+0.5+dir.getStepX(), pos.getY()+0.5+dir.getStepY(), pos.getZ()+0.5+dir.getStepZ()));
			charge = 0;
		}
		else {
			if (charge > 1)
				charge *= 0.99;
			else
				charge = 0;
		}
	}

	private static int forcedQuarter(int max) {
		return max <= 0 ? 0 : max/4+1;
	}

	private boolean canIntakeFluid(Fluid f) {
		return isValidFluid(f) && (fluid == null || f.isSame(fluid));
	}

	private void intakeFluid(Level world, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			if (!this.isConnectedDirectly(dir))
				continue;
			BlockEntity te = world.getBlockEntity(pos.relative(dir));
			if (te instanceof MagneticPipeBlockEntity tp) {
				if (tp.fluid != null) {
					int todrain = forcedQuarter(tp.level-level);
					if (todrain > 0 && this.canIntakeFluid(tp.fluid)) {
						fluid = tp.fluid;
						level += todrain;
						tp.level -= todrain;
					}
				}
			}
			else if (te instanceof PipeConnector pc && connectsTo(te)) {
				if (pc.getFlowForSide(dir.getOpposite()).canOutput) {
					FluidStack fs = pc.drain(dir.getOpposite(), Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
					if (!fs.isEmpty()) {
						int todrain = forcedQuarter(fs.getAmount()-level);
						if (todrain > 0 && this.canIntakeFluid(fs.getFluid())) {
							level += todrain;
							fluid = fs.getFluid();
							pc.drain(dir.getOpposite(), todrain, IFluidHandler.FluidAction.EXECUTE);
						}
					}
				}
			}
		}
	}

	private void dumpContents(Level world, BlockPos pos) {
		if (level <= 0 || fluid == null)
			return;
		for (Direction dir : Direction.values()) {
			if (level <= 0) {
				fluid = null;
				return;
			}
			if (!this.isConnectedDirectly(dir))
				continue;
			BlockEntity te = world.getBlockEntity(pos.relative(dir));
			if (te instanceof MagneticPipeBlockEntity tp) {
				if (tp.canIntakeFluid(fluid)) {
					int toadd = this.getPipeOutput(level-tp.level);
					if (toadd > 0) {
						tp.fluid = fluid;
						tp.level += toadd;
						level -= toadd;
					}
				}
			}
			else if (te instanceof PipeConnector pc && connectsTo(te)) {
				if (pc.getFlowForSide(dir.getOpposite()).canIntake) {
					int toadd = this.getPipeOutput(level);
					if (toadd > 0) {
						int added = pc.fill(dir.getOpposite(), new FluidStack(fluid, toadd), IFluidHandler.FluidAction.EXECUTE);
						if (added > 0)
							level -= added;
					}
				}
			}
		}
	}

	private int getPipeOutput(int max) {
		return Math.min(forcedQuarter(max), level-5);
	}

	@Override
	@Nullable
	public Fluid getFluidType() {
		return fluid;
	}

	@Override
	public int getFluidLevel() {
		return level;
	}

	public void setFluid(@Nullable Fluid f, int amt) {
		fluid = f;
		level = amt;
	}

	public int getCharge() {
		return charge;
	}

	public void setCharge(int c) {
		charge = c;
	}

	@Override
	public boolean isConnectedDirectly(Direction dir) {
		return this.getBlockState().getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir));
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public void onDischarge(int charge, double range) {
		this.charge += Math.pow(charge, 1.1);
	}

	@Override
	public int getMinDischarge() {
		return 256;
	}

	@Override
	public float getAimX() {
		return 0.5F;
	}

	@Override
	public float getAimY() {
		return 0.5F;
	}

	@Override
	public float getAimZ() {
		return 0.5F;
	}

	@Override
	public boolean canDischargeLongRange() {
		return true;
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		return false;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		if (fluid != null)
			NBT.putString("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
		NBT.putInt("level", level);
		NBT.putInt("chg", charge);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		fluid = NBT.contains("fluid") ? BuiltInRegistries.FLUID.get(ResourceLocation.parse(NBT.getString("fluid"))) : null;
		level = NBT.getInt("level");
		charge = NBT.getInt("chg");
	}
}
