/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.reactorcraft.content.menu.CpuMenu;
import reika.reactorcraft.reactor.CpuBlockEntity;
import reika.reactorcraft.reactor.LinkableReactorCore;
import reika.reactorcraft.registry.ReactorComponents;

/**
 * Original: Reika.ReactorCraft.Items.ItemRemoteControl (ChargeableTool; the charge in kJ was the item damage, here a component) - used
 * on a reactor CPU it links to it; used on a fuel core or control rod afterwards it adds that part to the linked CPU's temperature
 * checks. Used in the air with charge left it opens the linked CPU's GUI from up to 4*log2(charge) blocks away, for 1 kJ. Above 8192
 * kJ the original also worked across dimensions; here the CPU must be in the player's dimension (a client cannot show a block entity
 * of another dimension). Charged in the RotaryCraft worktable with a wound coil (not ported yet; creative has a charged one).
 */
public class RemoteControlItem extends Item implements reika.rotarycraft.api.interfaces.ChargeableTool {

	public static final int CREATIVE_CHARGE = 32000;

	public RemoteControlItem(Properties props) {
		super(props.stacksTo(1));
	}

	public static int getCharge(ItemStack is) {
		return is.getOrDefault(ReactorComponents.CHARGE.get(), 0);
	}

	@Nullable
	public static GlobalPos getLink(ItemStack is) {
		return is.get(ReactorComponents.LINKED_CPU.get());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player ep, InteractionHand hand) {
		ItemStack is = ep.getItemInHand(hand);
		if (!world.isClientSide && ep instanceof ServerPlayer sp && this.canUse(is, world, ep)) {
			GlobalPos link = getLink(is);
			if (world.getBlockEntity(link.pos()) instanceof CpuBlockEntity te) {
				is.set(ReactorComponents.CHARGE.get(), getCharge(is)-1);
				BlockPos pos = link.pos();
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new CpuMenu(id, inv, te), te.getBlockState().getBlock().getName()),
						buf -> buf.writeBlockPos(pos));
			}
		}
		return InteractionResultHolder.sidedSuccess(is, world.isClientSide);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level world = ctx.getLevel();
		ItemStack is = ctx.getItemInHand();
		BlockEntity te = world.getBlockEntity(ctx.getClickedPos());
		if (te instanceof CpuBlockEntity) {
			if (!world.isClientSide) {
				is.set(ReactorComponents.LINKED_CPU.get(), GlobalPos.of(world.dimension(), te.getBlockPos()));
				if (ctx.getPlayer() != null)
					ctx.getPlayer().sendSystemMessage(Component.literal("Linking to "+te.getBlockState().getBlock().getName().getString()+" at "+posString(te.getBlockPos())));
			}
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		if (te instanceof LinkableReactorCore core && !world.isClientSide) {
			CpuBlockEntity cpu = this.getLinkedCPU(is, world);
			if (cpu != null) {
				cpu.addTemperatureCheck(core);
				if (ctx.getPlayer() != null)
					ctx.getPlayer().sendSystemMessage(Component.literal("Linking "+te.getBlockState().getBlock().getName().getString()+" to "
							+cpu.getBlockState().getBlock().getName().getString()+" at "+posString(cpu.getBlockPos())));
			}
		}
		return InteractionResult.PASS;
	}

	private static String posString(BlockPos p) {
		return p.getX()+", "+p.getY()+", "+p.getZ();
	}

	@Nullable
	public CpuBlockEntity getLinkedCPU(ItemStack is, Level world) {
		GlobalPos link = getLink(is);
		if (link == null || !(world instanceof ServerLevel sl))
			return null;
		ServerLevel w = sl.getServer().getLevel(link.dimension());
		return w != null && w.isLoaded(link.pos()) && w.getBlockEntity(link.pos()) instanceof CpuBlockEntity te ? te : null;
	}

	public boolean canUse(ItemStack is, Level world, Player ep) {
		GlobalPos link = getLink(is);
		if (getCharge(is) <= 0 || link == null || link.dimension() != world.dimension())
			return false;
		BlockPos p = link.pos();
		double dd = Math.sqrt(Math.pow(ep.getBlockX()-p.getX(), 2)+Math.pow(ep.getBlockY()-p.getY(), 2)+Math.pow(ep.getBlockZ()-p.getZ(), 2));
		return getRange(is)+0.5 >= dd;
	}

	public static int getRange(ItemStack is) {
		int c = getCharge(is);
		return c <= 0 ? 0 : 4*(int)(Math.log(c)/Math.log(2));
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext ctx, List<Component> li, TooltipFlag flag) {
		GlobalPos link = getLink(is);
		if (link != null)
			li.add(Component.literal(String.format("Linked to CPU in world %s at %d, %d, %d", link.dimension().location(), link.pos().getX(), link.pos().getY(), link.pos().getZ())));
		else
			li.add(Component.literal("No linked CPU"));
		li.add(Component.literal("Charge: "+getCharge(is)+" kJ"));
	}

	/** Original setCharged (worktable with a wound coil): the tool takes the coil's charge, the coil keeps the tool's old one. */
	@Override
	public int setCharged(ItemStack is, int charge, boolean strongcoil) {
		int ret = is.getOrDefault(ReactorComponents.CHARGE.get(), 0);
		is.set(ReactorComponents.CHARGE.get(), charge);
		return ret;
	}
}
