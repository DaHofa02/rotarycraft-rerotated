/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.cells.IBasicCellItem;

/**
 * Kinetic storage cell, 1k to 1M bytes; one type (kinetic energy) at 1 byte = 128 J, so a 1k cell holds about 131 kJ and a 1M cell about
 * 134 MJ (less the per-type overhead of 1/128 of the cell, as AE2's own cells have).
 */
public class KineticCellItem extends Item implements IBasicCellItem {

	private final int kilobytes;

	public KineticCellItem(int kilobytes, Properties props) {
		super(props.stacksTo(1));
		this.kilobytes = kilobytes;
	}

	@Override
	public AEKeyType getKeyType() {
		return KineticKeyType.INSTANCE;
	}

	@Override
	public int getBytes(ItemStack is) {
		return kilobytes*1024;
	}

	@Override
	public int getBytesPerType(ItemStack is) {
		return kilobytes*8;
	}

	@Override
	public int getTotalTypes(ItemStack is) {
		return 1;
	}

	@Override
	public double getIdleDrain() {
		return 0.5+Math.log(kilobytes)/Math.log(4)*0.5;
	}

	public long getCapacityJoules() {
		return (long)(kilobytes*1024-kilobytes*8)*KineticKeyType.JOULES_PER_BYTE;
	}

	@Override
	public FuzzyMode getFuzzyMode(ItemStack is) {
		return FuzzyMode.IGNORE_ALL;
	}

	@Override
	public void setFuzzyMode(ItemStack is, FuzzyMode mode) {}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext ctx, List<Component> li, TooltipFlag flag) {
		this.addCellInformationToTooltip(is, li);
		li.add(Component.translatable("tooltip.rotaryme.cell_capacity", RotaryME.formatJoules(this.getCapacityJoules())));
	}
}
