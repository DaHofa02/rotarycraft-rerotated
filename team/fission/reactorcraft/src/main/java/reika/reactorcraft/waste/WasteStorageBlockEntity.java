/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.waste;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.reactorcraft.radiation.Isotopes;
import reika.reactorcraft.radiation.RadiationEffects;
import reika.reactorcraft.radiation.RadiationIntensity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.ReactorCraft.TileEntities.Waste.TileEntityWasteStorage - 12 slots (16 each) for long-lived waste only, nothing can be
 * taken out again; decays 1.75 times faster and keeps decaying while the game is closed (real time). Living things in its line of
 * sight within sqrt(waste count) blocks get moderate radiation sickness (1 in 20 ticks). In the Nether or surroundings above 100 C it
 * smokes and with 1 in 200 per tick explodes (4, the original passes y as the z coordinate of the explosion - kept).
 */
public class WasteStorageBlockEntity extends WasteUnitBlockEntity {

	public WasteStorageBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.WASTE_STORAGE.get(), pos, state, 12);
	}

	@Override
	public String getName() {
		return "Waste Storage";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (rand.nextInt(20) == 0)
			this.sickenMobs(world, pos);
		if (!world.isClientSide) {
			this.decayWaste(world, pos);
			this.feed(world, pos);
		}
		if (world.dimension() == Level.NETHER || AmbientTemperature.getAmbientTemperatureAt(world, pos) > 100) {
			if (this.hasWaste()) {
				if (world instanceof ServerLevel sl)
					sl.sendParticles(ParticleTypes.SMOKE, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 3, 0.6, 0.6, 0.6, 0);
				if (rand.nextInt(4) == 0)
					world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
				if (rand.nextInt(200) == 0 && !world.isClientSide) {
					world.removeBlock(pos, false);
					world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getY()+0.5, 4F, true, Level.ExplosionInteraction.BLOCK);
				}
			}
		}
	}

	private void sickenMobs(Level world, BlockPos pos) {
		if (world.isClientSide)
			return;
		int r = this.getRange();
		AABB box = new AABB(pos).inflate(r);
		List<LivingEntity> li = world.getEntitiesOfClass(LivingEntity.class, box);
		for (LivingEntity e : li) {
			if (!RadiationIntensity.MODERATE.hasSufficientShielding(e)) {
				double dd = e.position().distanceTo(pos.getCenter());
				if (canBlockSee(world, pos, e.position(), dd))
					RadiationEffects.applyEffects(e, RadiationIntensity.MODERATE);
			}
		}
	}

	public int getRange() {
		return (int)Math.sqrt(this.countWaste());
	}

	@Override
	public int getInventoryStackLimit() {
		return 16;
	}

	/** Original isAppropriateWasteSlot: a matching stack with room takes it, otherwise only an empty slot. */
	@Override
	protected boolean isValidSlot(int slot, ItemStack is) {
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack in = inv.getStackInSlot(i);
			if (ItemStack.isSameItemSameComponents(is, in) && in.getCount()+is.getCount() <= Math.min(this.getInventoryStackLimit(), is.getMaxStackSize()))
				return i == slot;
		}
		return inv.getStackInSlot(slot).isEmpty();
	}

	@Override
	public boolean leaksRadiation() {
		return false;
	}

	@Override
	public boolean isValidIsotope(Isotopes i) {
		return isLongLivedWaste(i);
	}

	@Override
	protected boolean canRemoveItem(int slot, ItemStack is) {
		return false;
	}

	@Override
	protected boolean accountForOutGameTime() {
		return true;
	}

	@Override
	protected double getBaseDecayRate() {
		return 1.75;
	}

	/** Original collapseInventory: items slide to the higher slots and equal stacks merge. */
	@Override
	protected void collapseInventory() {
		int n = inv.getSlots();
		for (int i = 0; i < n; i++) {
			for (int k = n-1; k > 0; k--) {
				ItemStack a = inv.getStackInSlot(k-1);
				if (a.isEmpty())
					continue;
				ItemStack b = inv.getStackInSlot(k);
				if (b.isEmpty()) {
					inv.setStackInSlot(k, a);
					inv.setStackInSlot(k-1, ItemStack.EMPTY);
				}
				else if (ItemStack.isSameItemSameComponents(a, b) && a.getCount()+b.getCount() <= Math.min(this.getInventoryStackLimit(), b.getMaxStackSize())) {
					inv.setStackInSlot(k, b.copyWithCount(a.getCount()+b.getCount()));
					inv.setStackInSlot(k-1, ItemStack.EMPTY);
				}
			}
		}
	}
}
