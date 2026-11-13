/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.TileEntityItemRefresher - with at least 16384 W from any side, dropped items within
 * 4 + (power - 16384)/1024 blocks (at most 128) never reach their despawn age, and items lying still are tossed up a little (0.4).
 * Change: 1.21 offers no way to set an item's age, so the item's lifespan is moved on instead (the item stays 20 ticks short of it).
 */
public class ItemRefresherBlockEntity extends PowerReceiverBlockEntity {

	public static final int MAXRANGE = 128;

	public ItemRefresherBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.ITEM_REFRESHER.get(), pos, state, 1, 1, 16384);
	}

	@Override
	public String getName() {
		return "Item Refresher";
	}

	public int getRange() {
		return (int)Math.min(MAXRANGE, 4+(power-MINPOWER)/1024);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		if (power < MINPOWER)
			return;
		int r = this.getRange();
		for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(r))) {
			if (item.getAge() > item.lifespan-20)
				item.lifespan = item.getAge()+20;
			Vec3 v = item.getDeltaMovement();
			if (v.y == 0)
				item.setDeltaMovement(v.x, 0.4, v.z);
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}
}
