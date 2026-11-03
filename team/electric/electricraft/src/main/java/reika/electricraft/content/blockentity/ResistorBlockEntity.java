/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.electricraft.network.CurrentThrottle;
import reika.electricraft.registry.ElectriBlockEntities;

/**
 * Original: TileEntityResistorBase, TileEntityResistor and TileEntityPreciseResistor - limits the current of every path through it to
 * the value of its colour code: digits then a power-of-ten multiplier (resistor: 2 digits, precision resistor: 3), set by right-clicking
 * a band with a dye. Without a value (0 A) it does not connect.
 */
public class ResistorBlockEntity extends WireComponentBlockEntity implements CurrentThrottle {

	/** Original ColorBand: the resistor colour code with the dye colours of ReikaDyeHelper. */
	public enum ColorBand {
		BLACK(DyeColor.BLACK, 0x191919),
		BROWN(DyeColor.BROWN, 0x664C33),
		RED(DyeColor.RED, 0x993333),
		ORANGE(DyeColor.ORANGE, 0xD87F33),
		YELLOW(DyeColor.YELLOW, 0xE5E533),
		GREEN(DyeColor.LIME, 0x7FCC19),
		BLUE(DyeColor.BLUE, 0x334CB2),
		PURPLE(DyeColor.PURPLE, 0x7F3FB2),
		GRAY(DyeColor.GRAY, 0x4C4C4C),
		WHITE(DyeColor.WHITE, 0xFFFFFF);

		public final DyeColor dye;
		public final int color;

		public static final ColorBand[] bandList = values();

		ColorBand(DyeColor dye, int color) {
			this.dye = dye;
			this.color = color;
		}

		public static ColorBand getBandFromColor(DyeColor color) {
			for (ColorBand b : bandList)
				if (b.dye == color)
					return b;
			return null;
		}
	}

	private final ColorBand[] bands;
	private int selectedCurrent;

	public ResistorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int bands) {
		super(type, pos, state);
		this.bands = new ColorBand[bands];
		Arrays.fill(this.bands, ColorBand.BLACK);
	}

	public static ResistorBlockEntity resistor(BlockPos pos, BlockState state) {
		return new ResistorBlockEntity(ElectriBlockEntities.RESISTOR.get(), pos, state, 3);
	}

	public static ResistorBlockEntity precise(BlockPos pos, BlockState state) {
		return new ResistorBlockEntity(ElectriBlockEntities.PRECISE_RESISTOR.get(), pos, state, 4);
	}

	@Override
	public String getName() {
		return bands.length == 4 ? "Precision Resistor" : "Resistor";
	}

	public ColorBand[] getColorBands() {
		return bands;
	}

	@Override
	public final int getResistance() {
		return 0;
	}

	@Override
	public final int getCurrentLimit() {
		return selectedCurrent;
	}

	/** Original setColor: band digit 1..n; the multiplier (last band) only takes black..purple (10^0..10^7). */
	public final boolean setColor(DyeColor color, int digit) {
		ColorBand band = color != null ? ColorBand.getBandFromColor(color) : null;
		if (band == null)
			return false;
		if (band.ordinal() > 7 && digit == bands.length)
			return false;
		bands[digit-1] = band;
		selectedCurrent = this.calculateCurrentLimit();
		if (level != null && !level.isClientSide && network != null)
			network.updateWires();
		this.syncAllData();
		return true;
	}

	/** Original calculateCurrentLimit: the digits read as a decimal number times 10^multiplier (int arithmetic as in the original). */
	protected int calculateCurrentLimit() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bands.length-1; i++)
			sb.append(bands[i].ordinal());
		int base = Integer.parseInt(sb.toString());
		int mult = 1;
		for (int i = 0; i < bands[bands.length-1].ordinal(); i++)
			mult *= 10;
		return base*mult;
	}

	@Override
	public final boolean canConnect() {
		return selectedCurrent > 0;
	}

	@Override
	public float getHeight() {
		return 0.75F;
	}

	@Override
	public float getWidth() {
		return 0.5F;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		for (int i = 0; i < bands.length; i++)
			NBT.putInt("band"+(i+1), bands[i].ordinal());
		NBT.putInt("sel", selectedCurrent);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		for (int i = 0; i < bands.length; i++)
			bands[i] = ColorBand.bandList[Math.min(NBT.getInt("band"+(i+1)), ColorBand.bandList.length-1)];
		selectedCurrent = NBT.getInt("sel");
	}
}
