/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The EMP machine, written for the port from the original's behaviour: with at least 268435456 W from below it charges (power/20 J a tick);
 * at 4.184 GJ (one tonne of TNT) it fires once: every RotaryCraft machine within 64 blocks is shut down for good (as the original's onEMP),
 * and every Forge Energy store within range is drained. Then it is spent.
 * Changes: the other mods' targets of the original (IC2, Thaumcraft nodes, ChromatiCraft pylons, powered armour, robots) are left out.
 */
public class EmpBlockEntity extends PowerReceiverBlockEntity {

	public static final long BLAST_ENERGY = 4184000000L;
	public static final int RANGE = 64;

	private long energy;
	private boolean fired;
	public int disabled;
	public long drained;

	public EmpBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.EMP.get(), pos, state, 1, 1, 268435456);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public long getEnergy() {
		return energy;
	}

	public boolean hasFired() {
		return fired;
	}

	/** For tests: charge without a 268 MW source. */
	public void addEnergy(long j) {
		energy += j;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN;
		if (world.isClientSide || fired)
			return;
		this.readPower();
		if (power >= MINPOWER)
			energy += power/20;
		if (energy >= BLAST_ENERGY)
			this.fire(world, pos);
	}

	private void fire(Level world, BlockPos pos) {
		fired = true;
		for (int cx = (pos.getX()-RANGE) >> 4; cx <= (pos.getX()+RANGE) >> 4; cx++)
			for (int cz = (pos.getZ()-RANGE) >> 4; cz <= (pos.getZ()+RANGE) >> 4; cz++) {
				LevelChunk ch = world.getChunkSource().getChunkNow(cx, cz);
				if (ch == null)
					continue;
				for (BlockEntity te : ch.getBlockEntities().values().toArray(new BlockEntity[0])) {
					if (te == this || te.getBlockPos().distSqr(pos) > RANGE*RANGE)
						continue;
					if (te instanceof RotaryCraftBlockEntity rc && !(te instanceof EmpBlockEntity)) {
						rc.onEMP();
						disabled++;
					}
					IEnergyStorage es = world.getCapability(Capabilities.EnergyStorage.BLOCK, te.getBlockPos(), null);
					if (es != null && es.canExtract())
						drained += es.extractEnergy(Integer.MAX_VALUE, false);
				}
			}
		this.syncAllData();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return fired ? 15 : 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putLong("energy", energy);
		NBT.putBoolean("fired", fired);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		energy = NBT.getLong("energy");
		fired = NBT.getBoolean("fired");
	}
}
