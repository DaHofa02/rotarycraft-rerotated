/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The self-destruct mechanism, written for the port from the original's behaviour: it arms itself while it gets shaft power (any side) and
 * goes off when that power stops - a dead man's switch. Then for 32 ticks it sets off small explosions (strength 3) at random spots up to 6
 * blocks away (only where its owner may build), and finally a strength-12 explosion at itself. Self-destruct blocks next to it go off with it.
 */
public class SelfDestructBlockEntity extends PowerReceiverBlockEntity {

	public static final int COUNTDOWN = 32;
	/** Tests switch the real explosions off and count them. */
	public static boolean allowExplosion = true;

	private boolean armed;
	private int countdown = -1;
	public int smallBlasts;
	public boolean finished;

	public SelfDestructBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SELF_DESTRUCT.get(), pos, state, 1, 1, 1);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public boolean isArmed() {
		return armed;
	}

	public boolean isCountingDown() {
		return countdown >= 0;
	}

	public void trigger() {
		if (countdown < 0 && !finished)
			countdown = 0;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		if (countdown < 0) {
			if (power > 0)
				armed = true;
			else if (armed)
				this.trigger();
			return;
		}
		countdown++;
		for (Direction d : Direction.values())
			if (world.getBlockEntity(pos.relative(d)) instanceof SelfDestructBlockEntity other)
				other.trigger();
		if (countdown <= COUNTDOWN) {
			BlockPos p = pos.offset(world.random.nextInt(13)-6, world.random.nextInt(13)-6, world.random.nextInt(13)-6);
			smallBlasts++;
			if (allowExplosion && (this.getPlacer() == null || world.mayInteract(this.getPlacer(), p)))
				world.explode(null, p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, 3F, true, Level.ExplosionInteraction.BLOCK);
			return;
		}
		finished = true;
		countdown = -1;
		armed = false;
		if (allowExplosion) {
			world.removeBlock(pos, false);
			world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 12F, true, Level.ExplosionInteraction.BLOCK);
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("armed", armed);
		NBT.putInt("count", countdown);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		armed = NBT.getBoolean("armed");
		countdown = NBT.contains("count") ? NBT.getInt("count") : -1;
	}
}
