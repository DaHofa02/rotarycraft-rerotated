/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import reika.rotarycraft.compat.PneumaticCompat;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The pneumatic engine, written for the port from the original's behaviour: the converter engine of the magnetostatic engine (tiers,
 * upgrades, speed choice, redstone mode, cooling, same GUI) running on PneumaticCraft compressed air taken in at its back (a 20-bar air
 * handler of 30000 mL), power/520 mL a tick divided by its efficiency. Needs PneumaticCraft; without it, it does nothing.
 */
public class PneumaticEngineBlockEntity extends MagneticEngineBlockEntity {

	public static final int VOLUME = 30000;

	@Nullable
	private Object air;
	@Nullable
	private CompoundTag pendingAir;

	public PneumaticEngineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.PNEUMATIC_ENGINE.get(), pos, state);
	}

	/** The PneumaticCraft air handler (an IAirHandlerMachine), or null without PneumaticCraft. */
	@Nullable
	public Object getAirHandler() {
		if (air == null && reika.rotarycraft.compat.OptionalMods.pneumaticCraft()) {
			air = PneumaticCompat.createHandler(VOLUME, this.getAirSide());
			if (pendingAir != null)
				PneumaticCompat.load(air, pendingAir);
			pendingAir = null;
		}
		return air;
	}

	public Direction getAirSide() {
		return write == null ? Direction.EAST : write.getOpposite();
	}

	public int getAir() {
		Object h = this.getAirHandler();
		return h == null ? 0 : PneumaticCompat.getAir(h);
	}

	@Override
	public int getMaxStorage() {
		return Integer.MAX_VALUE;
	}

	@Nullable
	@Override
	public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
		return null;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		Object h = world.isClientSide ? null : this.getAirHandler();
		if (h != null) {
			this.getIOSides(this.getBlockMetadata());
			PneumaticCompat.setSide(h, this.getAirSide());
			PneumaticCompat.tick(h, this);
			stored = Math.max(0, PneumaticCompat.getAir(h));
		}
		int before = stored;
		super.updateEntity(world, pos, state);
		if (h != null && stored < before)
			PneumaticCompat.addAir(h, stored-before);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		if (air != null)
			PneumaticCompat.save(air, NBT);
		else if (pendingAir != null)
			NBT.put("air", pendingAir.getCompound("air"));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		if (NBT.contains("air")) {
			if (air != null)
				PneumaticCompat.load(air, NBT);
			else
				pendingAir = NBT.copy();
		}
	}
}
