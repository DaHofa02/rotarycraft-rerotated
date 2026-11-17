/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.World.TileEntityWeatherController - with 32768 W from below and open sky above it, once its
 * cooldown has run out it looks at its 18 slots: sawdust clears the sky, silver iodide brings rain, silver iodide with redstone a
 * thunderstorm and silver iodide with glowstone dust a superstorm (thunder with a lightning strike 1 tick in 20 somewhere within 64 blocks).
 * If the weather is not already as wanted, the items are used up (fired into the sky with a bang) and the cooldown is set to 200-599 ticks.
 * Changes: the BANRAIN config and the WeatherControlEvent are left out; the ore dictionary's "dustWood" is just the port's sawdust.
 */
public class WeatherControllerBlockEntity extends PowerReceiverBlockEntity {

	public enum RainMode {
		NONE, SUN, RAIN, THUNDER, SUPERSTORM;

		public boolean isRain() {
			return this.ordinal() > SUN.ordinal();
		}

		public boolean isThunder() {
			return this.ordinal() > RAIN.ordinal();
		}

		public boolean hasAction() {
			return this != NONE;
		}
	}

	private final ItemStackHandler inv = new ItemStackHandler(18) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return isValidWeatherItem(is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			WeatherControllerBlockEntity.this.setChanged();
		}
	};
	private int cooldown;
	private RainMode rainmode = RainMode.NONE;

	public WeatherControllerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.WEATHER_CONTROLLER.get(), pos, state, 1, 1, 32768);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Original: canExtractItem is always false. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new net.neoforged.neoforge.items.wrapper.RangedWrapper(inv, 0, 18) {
			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return ItemStack.EMPTY;
			}
		};
	}

	private static Item sawdust() {
		return RotaryItems.part(CraftingPart.SAWDUST);
	}

	private static Item silverIodide() {
		return RotaryItems.part(CraftingPart.SILVERIODIDE);
	}

	public static boolean isValidWeatherItem(ItemStack is) {
		return is.is(sawdust()) || is.is(silverIodide()) || is.is(Items.REDSTONE) || is.is(Items.GLOWSTONE_DUST);
	}

	public int getCooldown() {
		return cooldown;
	}

	public RainMode getMode() {
		return rainmode;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN; //getPowerBelow
		if (world.isClientSide)
			return;
		this.readPower();
		if (cooldown > 0)
			cooldown--;
		if (power < MINPOWER || !world.canSeeSky(pos.above()) || !(world instanceof ServerLevel sl))
			return;
		if (rainmode == RainMode.SUPERSTORM) {
			this.setWeather(sl, true, true);
			if (world.random.nextInt(20) == 0) {
				int xl = pos.getX()-64+world.random.nextInt(129);
				int zl = pos.getZ()-64+world.random.nextInt(129);
				int yl = world.getHeight(Heightmap.Types.MOTION_BLOCKING, xl, zl);
				LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(world);
				if (bolt != null) {
					bolt.moveTo(xl, yl, zl);
					world.addFreshEntity(bolt);
				}
			}
		}
		if (cooldown <= 0) {
			rainmode = this.getRainMode(world);
			if (!isAlready(world, rainmode) && rainmode.hasAction())
				this.setWeather(sl, rainmode.isRain(), rainmode.isThunder());
		}
	}

	/** Original: WorldInfo.setRaining/setThundering - the durations are left to the level's random weather cycle. */
	private void setWeather(ServerLevel sl, boolean rain, boolean thunder) {
		int time = 12000+sl.random.nextInt(12000);
		sl.setWeatherParameters(rain ? 0 : time, rain ? time : 0, rain, thunder);
	}

	private static boolean isAlready(Level world, RainMode m) {
		return m.isRain() == world.isRaining() && m.isThunder() == world.isThundering();
	}

	private int find(Item item) {
		for (int i = 0; i < inv.getSlots(); i++)
			if (inv.getStackInSlot(i).is(item))
				return i;
		return -1;
	}

	private void decr(Item item) {
		int slot = this.find(item);
		if (slot >= 0)
			inv.extractItem(slot, 1, false);
	}

	private RainMode getRainMode(Level world) {
		Item is = null;
		Item is2 = null;
		boolean silverio = this.find(silverIodide()) >= 0;
		boolean redstone = this.find(Items.REDSTONE) >= 0;
		boolean glowdust = this.find(Items.GLOWSTONE_DUST) >= 0;
		RainMode mode;
		if (this.find(sawdust()) >= 0) {
			mode = RainMode.SUN;
			is = sawdust();
		}
		else if (silverio) {
			mode = RainMode.RAIN;
			is = silverIodide();
			if (redstone) {
				mode = RainMode.THUNDER;
				is2 = Items.REDSTONE;
			}
			else if (glowdust) {
				mode = RainMode.SUPERSTORM;
				is2 = Items.GLOWSTONE_DUST;
			}
		}
		else
			mode = RainMode.NONE;
		if (isAlready(world, mode))
			return rainmode;
		cooldown = 200+world.random.nextInt(400);
		if (mode.hasAction())
			this.fire(world, is, is2);
		switch (mode) {
			case SUN -> this.decr(sawdust());
			case RAIN -> this.decr(silverIodide());
			case THUNDER -> {
				this.decr(silverIodide());
				this.decr(Items.REDSTONE);
			}
			case SUPERSTORM -> {
				this.decr(silverIodide());
				this.decr(Items.GLOWSTONE_DUST);
			}
			default -> {}
		}
		return mode;
	}

	private void fire(Level world, @Nullable Item is, @Nullable Item is2) {
		BlockPos p = worldPosition;
		world.playSound(null, p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 1);
		for (Item item : new Item[]{is, is2}) {
			if (item == null)
				continue;
			ItemEntity ei = new ItemEntity(world, p.getX()+0.5, p.getY()+1.0625, p.getZ()+0.5, new ItemStack(item));
			ei.setDeltaMovement((world.random.nextDouble()-0.5)*0.4, 3, (world.random.nextDouble()-0.5)*0.4);
			ei.setPickUpDelay(5000);
			ei.lifespan = 100; //original: age 5900 of 6000
			world.addFreshEntity(ei);
		}
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("cool", cooldown);
		NBT.putInt("mode", rainmode.ordinal());
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		cooldown = NBT.getInt("cool");
		rainmode = RainMode.values()[Math.floorMod(NBT.getInt("mode"), RainMode.values().length)];
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
