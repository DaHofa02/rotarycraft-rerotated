/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import reika.rotarycraft.network.RotaryNetwork;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The CCTV camera, written for the port from the original's behaviour: spring powered (a wound spring in its slot unwinds as it runs) and
 * tagged with three dyes. A CCTV screen with the same dyes shows its view: the player's camera moves to it, looking where it is aimed, until
 * they sneak or the spring runs down. A screwdriver turns it 5 degrees to the side, sneaking tilts it 5 degrees down (between -60 and 60).
 */
public class CctvBlockEntity extends SpringPoweredBlockEntity implements RemoteControlled {

	private final ItemStackHandler dyes = new ItemStackHandler(3) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return is.getItem() instanceof DyeItem;
		}

		@Override
		protected void onContentsChanged(int slot) {
			CctvBlockEntity.this.setChanged();
		}
	};
	/** Pitch, -60 (up) to 60 (down). */
	public float theta;
	/** Yaw, as Minecraft's (0 = south). */
	public float yaw;
	private boolean on;
	private int tickcount2;
	private final Set<UUID> viewers = new HashSet<>();
	public int views;

	public CctvBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.CCTV.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Override
	public int getBaseDischargeTime() {
		return 120;
	}

	public ItemStackHandler getDyes() {
		return dyes;
	}

	public boolean isOn() {
		return on;
	}

	@Override
	public int[] getColors() {
		int[] c = new int[3];
		for (int i = 0; i < 3; i++)
			c[i] = dyes.getStackInSlot(i).getItem() instanceof DyeItem d ? d.getDyeColor().getId() : -1;
		return c;
	}

	/** Screwdriver: turn (yaw +5) or, sneaking, tilt (pitch -5, wrapping between -60 and 60). */
	public void turn(boolean tilt) {
		if (tilt) {
			theta -= 5;
			if (theta < -60)
				theta = 60;
		}
		else
			yaw = (yaw+5)%360;
		this.syncAllData();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		on = this.hasCoil();
		if (!on) {
			if (!world.isClientSide && !viewers.isEmpty())
				this.release(world);
			return;
		}
		tickcount2++;
		if (tickcount2 > this.getUnwindTime()) {
			if (!world.isClientSide)
				this.decrementCharge();
			tickcount2 = 0;
		}
	}

	private void release(Level world) {
		if (world.getServer() != null)
			for (UUID id : viewers) {
				ServerPlayer sp = world.getServer().getPlayerList().getPlayer(id);
				if (sp != null)
					PacketDistributor.sendToPlayer(sp, RotaryNetwork.CctvView.off(worldPosition));
			}
		viewers.clear();
	}

	/** Called by a matching screen: the player sees through this camera while it runs. */
	@Override
	public void activate(ServerPlayer ep) {
		if (!on)
			return;
		views++;
		viewers.add(ep.getUUID());
		PacketDistributor.sendToPlayer(ep, new RotaryNetwork.CctvView(worldPosition, yaw, theta, true));
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		super.onBlockBroken(world, pos);
		this.release(world);
		for (int i = 0; i < 3; i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), dyes.getStackInSlot(i).copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("dyes", dyes.serializeNBT(registries));
		NBT.putFloat("theta", theta);
		NBT.putFloat("yaw", yaw);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		dyes.deserializeNBT(registries, NBT.getCompound("dyes"));
		theta = NBT.getFloat("theta");
		yaw = NBT.getFloat("yaw");
	}
}
