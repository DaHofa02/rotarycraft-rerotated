/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.ClutchBlockEntity;
import reika.rotarycraft.content.blockentity.BlastFurnaceBlockEntity;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.content.blockentity.FrictionHeaterBlockEntity;
import reika.rotarycraft.content.blockentity.EngineControllerBlockEntity;
import reika.rotarycraft.content.blockentity.FlywheelBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.IOMachineBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.ShaftBlockEntity;
import reika.rotarycraft.content.blockentity.SplitterBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;

/**
 * Original: Reika.RotaryCraft.Items.Tools.ItemScrewdriver - rotates machines, sneak-use switches modes. Only the branches for the
 * ported machines are present; the others arrive with their machines. Vanilla blocks are rotated by vanilla's own mechanics.
 */
public class ScrewdriverItem extends Item {

	public ScrewdriverItem(Properties props) {
		super(props);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level world = ctx.getLevel();
		Player ep = ctx.getPlayer();
		BlockEntity te = world.getBlockEntity(ctx.getClickedPos());
		if (!(te instanceof RotaryCraftBlockEntity t) || ep == null)
			return InteractionResult.PASS;
		if (world.isClientSide)
			return InteractionResult.SUCCESS;
		int damage = t.getBlockMetadata();
		boolean sneak = ep.isShiftKeyDown();
		if (te instanceof IOMachineBlockEntity io)
			io.iotick = 512;
		if (te instanceof reika.rotarycraft.api.interfaces.Screwdriverable sc) {
			boolean flag = sneak ? sc.onShiftRightClick(world, ctx.getClickedPos(), ctx.getClickedFace()) : sc.onRightClick(world, ctx.getClickedPos(), ctx.getClickedFace());
			if (flag)
				return InteractionResult.SUCCESS;
		}

		if (te instanceof EngineBlockEntity clicked) {
			int dmg = damage;
			while (damage > 3)
				damage -= 4;
			clicked.setBlockMetadata(damage == 3 ? dmg-3 : dmg+1);
			clicked.onRotate();
		}
		else if (te instanceof FlywheelBlockEntity || te instanceof GearboxBlockEntity && !sneak) {
			t.setBlockMetadata(damage != 3 && damage != 7 && damage != 11 && damage != 15 ? damage+1 : damage-3);
		}
		else if (te instanceof GearboxBlockEntity gbx) {
			gbx.reduction = !gbx.reduction;
		}
		else if (te instanceof ShaftBlockEntity ts) {
			if (damage < 5)
				ts.setBlockMetadata(damage+1);
			if (damage == 5)
				ts.setBlockMetadata(0);
			if (damage > 5 && damage < 9)
				ts.setBlockMetadata(damage+1);
			if (damage == 9)
				ts.setBlockMetadata(6);
		}
		else if (te instanceof ClutchBlockEntity tc && sneak) {
			tc.needsRedstone = !tc.needsRedstone;
		}
		else if (te instanceof SplitterBlockEntity clicked) {
			if (!sneak) {
				if (damage < 7 || (damage < 15 && damage > 7))
					clicked.setBlockMetadata(damage+1);
				if (damage == 7)
					clicked.setBlockMetadata(0);
				if (damage == 15)
					clicked.setBlockMetadata(8);
			}
			else { // Toggle in/out
				clicked.setBlockMetadata(damage < 8 ? damage+8 : damage-8);
			}
		}
		else if (te instanceof EngineControllerBlockEntity clicked) {
			if (sneak) {
				clicked.redstoneMode = !clicked.redstoneMode;
				ep.sendSystemMessage(Component.literal(clicked.redstoneMode ? "ECU is now redstone-operated." : "ECU is now manually controlled."));
			}
			else {
				clicked.increment();
				ep.sendSystemMessage(Component.literal(String.format("ECU set to %.2f%% speed.", 100D*clicked.getSpeedMultiplier())));
			}
		}
		else if (te instanceof BevelGearBlockEntity) {
			//Original: the bevel gears have a single "direction" for the screwdriver; their 24 routes are chosen in the bevel gear GUI.
		}
		else {
			//Original generic case: cycle through MachineRegistry.getNumberDirections() (6 for the clutch, 4 for the dynamometer).
			int max = (te instanceof ClutchBlockEntity ? 6 : te instanceof DynamometerBlockEntity || te instanceof BlastFurnaceBlockEntity || te instanceof FrictionHeaterBlockEntity ? 4 : 1)-1;
			t.setBlockMetadata(damage < max ? damage+1 : 0);
			world.updateNeighborsAt(ctx.getClickedPos(), t.getBlockState().getBlock());
		}
		t.syncAllData();
		return InteractionResult.CONSUME;
	}
}
