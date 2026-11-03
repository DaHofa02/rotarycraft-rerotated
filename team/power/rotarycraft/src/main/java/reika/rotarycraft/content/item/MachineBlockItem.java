/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import reika.rotarycraft.content.block.EngineBlock;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.lib.math.ReikaEngLibrary;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.Flywheels;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryComponents;

/**
 * Item form of a machine. Names and Shift tooltips follow the original placer items (ItemShaftPlacer, ItemGearPlacer,
 * ItemFlywheelPlacer): "material + Shaft", "material ratio:1 Gearbox", "type Flywheel".
 */
public class MachineBlockItem extends BlockItem {

	public MachineBlockItem(Block block, Properties props) {
		super(block, props);
	}

	@Override
	public Component getName(ItemStack is) {
		Block b = this.getBlock();
		Component base = Component.translatable(this.getDescriptionId());
		if (b == RotaryBlocks.SHAFT.get()) {
			if (Boolean.TRUE.equals(is.get(RotaryComponents.SHAFT_CROSS.get())))
				return Component.translatable("item.rotarycraft.shaft_cross");
			MaterialRegistry mat = is.getOrDefault(RotaryComponents.SHAFT_MATERIAL.get(), MaterialRegistry.WOOD);
			return Component.translatable("material.rotarycraft."+mat.getSerializedName()).append(" ").append(base);
		}
		if (b == RotaryBlocks.GEARBOX.get()) {
			GearboxTypes type = is.getOrDefault(RotaryComponents.GEARBOX_TYPE.get(), GearboxTypes.WOOD);
			int ratio = is.getOrDefault(RotaryComponents.GEAR_RATIO.get(), 2);
			return Component.translatable("material.rotarycraft."+type.material.getSerializedName()).append(" "+ratio+":1 ").append(base);
		}
		if (b == RotaryBlocks.FLYWHEEL.get()) {
			Flywheels f = is.getOrDefault(RotaryComponents.FLYWHEEL_TYPE.get(), Flywheels.WOOD);
			return Component.translatable("flywheel.rotarycraft."+f.getSerializedName()).append(" ").append(base);
		}
		return super.getName(is);
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext ctx, List<Component> li, TooltipFlag flag) {
		Block b = this.getBlock();
		boolean shift = net.neoforged.fml.loading.FMLEnvironment.dist.isClient() && ClientKeys.isShiftDown();
		if (b == RotaryBlocks.RESERVOIR.get()) {
			//Original: TileEntityReservoir.getDisplayTags
			RotaryComponents.ReservoirData d = is.get(RotaryComponents.RESERVOIR_DATA.get());
			if (d != null) {
				if (!d.fluid().isEmpty())
					li.add(Component.literal("Contents: "+d.fluid().getAmount()+" mB of "+d.fluid().getHoverName().getString()));
				if (d.cover())
					li.add(Component.literal("Covered"));
			}
		}
		if (b == RotaryBlocks.SHAFT.get() && !Boolean.TRUE.equals(is.get(RotaryComponents.SHAFT_CROSS.get()))) {
			if (shift) {
				MaterialRegistry mat = is.getOrDefault(RotaryComponents.SHAFT_MATERIAL.get(), MaterialRegistry.WOOD);
				li.add(Component.literal(String.format("Max Speed: %s", PowerFormat.formatSpeed(mat.getMaxShaftSpeed()))));
				li.add(Component.literal(String.format("Max Torque: %s", PowerFormat.formatTorque(mat.getMaxShaftTorque()))));
			}
			else {
				li.add(holdShift("load data"));
			}
		}
		if (b instanceof EngineBlock eb) {
			//Original: ItemEnginePlacer.addInformation
			if (shift) {
				EngineType type = eb.getEngineType();
				double power = type.getPower();
				double speed = type.getSpeed();
				double torque = type.getTorque();
				li.add(Component.literal(String.format("Power: %.3f %sW", ReikaMathLibrary.getThousandBase(power), ReikaEngLibrary.getSIPrefix(power))));
				li.add(Component.literal(String.format("Torque: %.3f %sNm", ReikaMathLibrary.getThousandBase(torque), ReikaEngLibrary.getSIPrefix(torque))));
				li.add(Component.literal(String.format("Speed: %.3f %srad/s", ReikaMathLibrary.getThousandBase(speed), ReikaEngLibrary.getSIPrefix(speed))));
			}
			else {
				li.add(holdShift("power data"));
			}
			Integer fod = is.get(RotaryComponents.JET_DAMAGE.get());
			if (fod != null)
				li.add(Component.literal(String.format("Damage: %.1f%s", fod*12.5F, "%")));
			if (Boolean.TRUE.equals(is.get(RotaryComponents.BEDROCK_HYDRO.get())))
				li.add(Component.literal("Bedrock Upgrade"));
		}
		if (b == RotaryBlocks.FLYWHEEL.get()) {
			if (shift) {
				Flywheels f = is.getOrDefault(RotaryComponents.FLYWHEEL_TYPE.get(), Flywheels.WOOD);
				if (f.maxSpeed < Integer.MAX_VALUE)
					li.add(Component.literal(String.format("Max Speed: %d rad/s", f.maxSpeed)));
				li.add(Component.literal(String.format("Required Torque: %d Nm", f.getMinTorque())));
				if (f.maxTorque < Integer.MAX_VALUE)
					li.add(Component.literal(String.format("Max Torque: %d Nm", f.maxTorque)));
				li.add(Component.literal(String.format("Decay Rate: 1/%d", f.decayTime)));
			}
			else {
				li.add(holdShift("load data"));
			}
		}
		if (b == RotaryBlocks.GEARBOX.get()) {
			RotaryComponents.GearboxData data = is.get(RotaryComponents.GEARBOX_DATA.get());
			GearboxTypes mat = is.getOrDefault(RotaryComponents.GEARBOX_TYPE.get(), GearboxTypes.WOOD);
			if (data != null) {
				if (mat.isDamageableGear())
					li.add(Component.literal("Damage: "+GearboxBlockEntity.getDamagePercent(data.damage())+"%"));
				if (mat.needsLubricant())
					li.add(Component.literal("Lubricant: "+data.lubricant()+" mB"));
				MaterialRegistry matl = data.bearing().material;
				ChatFormatting ec = ChatFormatting.WHITE;
				if (matl.ordinal() != mat.material.ordinal())
					ec = matl.ordinal() < mat.material.ordinal() ? ChatFormatting.RED : ChatFormatting.GREEN;
				li.add(Component.literal("Bearing type: ").append(Component.translatable("material.rotarycraft."+matl.getSerializedName()).withStyle(ec)));
			}
		}
	}

	static Component holdShift(String what) {
		return Component.literal("Hold ").withStyle(ChatFormatting.GRAY).append(Component.literal("Shift").withStyle(ChatFormatting.GREEN)).append(Component.literal(" for "+what).withStyle(ChatFormatting.GRAY));
	}
}
