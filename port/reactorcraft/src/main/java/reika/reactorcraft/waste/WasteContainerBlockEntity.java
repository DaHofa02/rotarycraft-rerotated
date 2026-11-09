/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.waste;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import reika.reactorcraft.radiation.Isotopes;
import reika.reactorcraft.radiation.RadiationEffects;
import reika.reactorcraft.radiation.RadiationIntensity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.ReactorCraft.TileEntities.Waste.TileEntityWasteContainer - 27 slots (one item each) for short-lived waste; the decay
 * heat (about 3.2 C per item and second) warms it; above ambient a water source next to it takes 278 C (and boils away above 100 C,
 * else turns into flowing water); smoke and fizzing above 150/300 C; above 600 C it melts down (explosion 9, lethal contamination
 * radius 9). It leaks waste neutrons. Only long-lived waste (none, since it only takes short-lived waste) can be taken out by machines.
 */
public class WasteContainerBlockEntity extends WasteUnitBlockEntity implements reika.rotarycraft.content.blockentity.HeatConduction {

	public static final int WIDTH = 9;
	public static final int HEIGHT = 3;
	/** Original ReikaNuclearHelper.getWasteDecayHeat: Avogadro * 0.5 eV over 15000 kg at c = 1. */
	public static final double WASTE_DECAY_HEAT = 6.02e23*0.5*1.602e-19/15000;
	/** Original ReikaThermoHelper.getTemperatureIncrease(1, 15000, WATER_BLOCK_HEAT). */
	public static final double WATER_COOLING = 4.18e6/15000;

	public WasteContainerBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.WASTE_CONTAINER.get(), pos, state, WIDTH*HEIGHT);
	}

	@Override
	public String getName() {
		return "Waste Container";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (this.thermalTick()) {
			temperature += this.countWaste()*WASTE_DECAY_HEAT;
			this.distributeHeat(world, pos);
			if (this.isRemoved())
				return;
		}
		if (!world.isClientSide) {
			this.decayWaste(world, pos);
			this.feed(world, pos);
		}
	}

	private void distributeHeat(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (temperature > Tamb) {
			for (Direction side : Direction.values()) {
				BlockPos p = pos.relative(side);
				BlockState s = world.getBlockState(p);
				if (s.getFluidState().isSource() && s.getFluidState().is(net.minecraft.tags.FluidTags.WATER) && s.getBlock() instanceof LiquidBlock) {
					temperature -= WATER_COOLING;
					if (!world.isClientSide) {
						if (temperature > 100)
							world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
						else
							world.setBlockAndUpdate(p, Fluids.FLOWING_WATER.getFlowing(2, false).createLegacyBlock());
					}
					break;
				}
			}
		}
		if (temperature < Tamb)
			temperature = Tamb;
		if (temperature > this.getMaxTemperature()) {
			if (!world.isClientSide)
				this.onMeltdown(world, pos);
		}
		else if (temperature > this.getMaxTemperature()/2 && rand.nextInt(6) == 0)
			this.smoke(world, pos);
		else if (temperature > this.getMaxTemperature()/4 && rand.nextInt(20) == 0)
			this.smoke(world, pos);
	}

	private void smoke(Level world, BlockPos pos) {
		if (world instanceof ServerLevel sl)
			sl.sendParticles(ParticleTypes.SMOKE, pos.getX()+rand.nextDouble(), pos.getY()+1, pos.getZ()+rand.nextDouble(), 1, 0, 0, 0, 0);
		world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
	}

	public void onMeltdown(Level world, BlockPos pos) {
		world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 9, true, Level.ExplosionInteraction.BLOCK);
		RadiationEffects.contaminateArea(world, pos, 9, 4, 1.5, true, RadiationIntensity.LETHAL);
	}

	public int getMaxTemperature() {
		return 600;
	}

	@Override
	public boolean leaksRadiation() {
		return true;
	}

	@Override
	public boolean isValidIsotope(Isotopes i) {
		return !isLongLivedWaste(i);
	}

	@Override
	protected boolean canRemoveItem(int slot, ItemStack is) {
		return isLongLivedWaste(is);
	}

	@Override
	protected boolean accountForOutGameTime() {
		return false;
	}

	@Override
	protected double getBaseDecayRate() {
		return 1;
	}

	/** Original collapseInventory: items slide to the higher slots. */
	@Override
	protected void collapseInventory() {
		int n = inv.getSlots();
		for (int i = 0; i < n; i++) {
			for (int k = n-1; k > 0; k--) {
				if (inv.getStackInSlot(k).isEmpty() && !inv.getStackInSlot(k-1).isEmpty()) {
					inv.setStackInSlot(k, inv.getStackInSlot(k-1));
					inv.setStackInSlot(k-1, ItemStack.EMPTY);
				}
			}
		}
	}

	/** Original TileEntityWasteContainer.allowExternalHeating (HeatConduction via TemperatureTE): false. */
	@Override
	public boolean allowExternalHeating() {
		return false;
	}

	/** Original TileEntityReactorBase.allowHeatExtraction: true. */
	@Override
	public boolean allowHeatExtraction() {
		return true;
	}
}
