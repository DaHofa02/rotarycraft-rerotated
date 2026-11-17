/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Decorative.TileEntityDisplay - runs on a wound coil (one charge per 120 ticks times the
 * spring's stiffness; the original counts two ticks per tick while it runs) and shows a translucent 5x3 screen above itself with the
 * message from a written book (right-click with it), scrolling when it has more than 12 lines. The screen is argon blue by default, a dye
 * colours it (glowstone dust: argon again). It needs the 5x3 space above it to be free.
 */
public class DisplayBlockEntity extends SpringPoweredBlockEntity {

	public static final int DISPLAY_HEIGHT = 12;
	public static final int DISPLAY_WIDTH = 27;
	private static final int[] AR_RGB = {0, 128, 255}, AR_BRGB = {0, 255, 255};

	private String message = "";
	private boolean display;
	@Nullable
	private DyeColor color;
	private boolean isArgonBlue = true;

	public DisplayBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.DISPLAY.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Override
	public int getBaseDischargeTime() {
		return 120;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateCoil();
		tickcount++;
	}

	private void updateCoil() {
		if (!this.hasCoil()) {
			display = false;
			return;
		}
		display = true;
		tickcount++;
		if (tickcount > this.getUnwindTime()) {
			if (level != null && !level.isClientSide)
				this.decrementCharge();
			tickcount = 0;
		}
	}

	public boolean canDisplay() {
		return display;
	}

	public boolean hasList() {
		return message != null && !message.isEmpty();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String str) {
		message = str;
		this.syncAllData();
	}

	public void clearMessage() {
		message = "";
	}

	public DyeColor getDyeColor() {
		return color != null ? color : DyeColor.BLACK;
	}

	public boolean isArgonBlue() {
		return isArgonBlue;
	}

	public void setDyeColor(DyeColor dye) {
		color = dye;
		isArgonBlue = false;
		this.syncAllData();
	}

	public void setColorToArgon() {
		isArgonBlue = true;
		this.syncAllData();
	}

	/** Screen and border colour (RGB 0-255): argon blue, or the dye's 1.7.10 colour (ReikaDyeHelper = the wool map colours). */
	public int[] getScreenColor() {
		if (isArgonBlue)
			return AR_RGB;
		int c = this.getDyeColor().getMapColor().col;
		return new int[] {(c >> 16) & 255, (c >> 8) & 255, c & 255};
	}

	public int[] getBorderColor() {
		return isArgonBlue ? AR_BRGB : this.getScreenColor();
	}

	/** Original hasSpace: 3 blocks of air above it along 5 blocks across its face. */
	public boolean hasSpace() {
		if (level == null)
			return false;
		int meta = this.getBlockMetadata();
		for (int j = -2; j <= 2; j++) {
			int a = meta != 0 && meta != 1 ? j : 0, b = meta != 0 && meta != 1 ? 0 : j;
			for (int i = 1; i <= 3; i++)
				if (!level.getBlockState(worldPosition.offset(a, i, b)).isAir())
					return false;
		}
		return true;
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
		NBT.putInt("dye", this.getDyeColor().getId());
		NBT.putBoolean("argon", isArgonBlue);
		NBT.putString("msg", message);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		isArgonBlue = !NBT.contains("argon") || NBT.getBoolean("argon");
		color = DyeColor.byId(NBT.getInt("dye"));
		message = NBT.getString("msg");
	}
}
