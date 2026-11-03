/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.item;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import reika.reactorcraft.content.entity.RadiationEntity;
import reika.reactorcraft.registry.ReactorComponents;

/**
 * Original: Reika.ReactorCraft.Items.ItemRadiationCleaner (ChargeableTool; the charge in kJ was the item damage, here a component) -
 * right-clicking a water source within 5 blocks takes it in (1000 mB, up to 32000); otherwise it sprays while held (like drawing a
 * bow) for up to min(5 ticks per kJ, water/25) ticks: a spray of rain particles 0.5 to 4.5 blocks ahead, and every 5th tick each
 * radiation cloud within 1 block of that spot shrinks by one (gone at 0). Letting go costs 1 kJ per 5 ticks and 25 mB per tick (not in
 * creative); like the original, holding it until the use time runs out costs nothing. Charged in the RotaryCraft worktable with a
 * wound coil (not ported yet; creative has a charged, full one).
 */
public class RadiationCleanerItem extends Item implements reika.rotarycraft.api.interfaces.ChargeableTool {

	public static final int CAPACITY = 32000;
	public static final int CREATIVE_CHARGE = 32000;
	private static final int WATER_PER_TICK = 25;
	private static final int TICK_PER_KJ = 5;

	public RadiationCleanerItem(Properties props) {
		super(props.stacksTo(1));
	}

	public static int getCharge(ItemStack is) {
		return is.getOrDefault(ReactorComponents.CHARGE.get(), 0);
	}

	public static int getWater(ItemStack is) {
		return is.getOrDefault(ReactorComponents.WATER.get(), 0);
	}

	public static void setWater(ItemStack is, int level) {
		is.set(ReactorComponents.WATER.get(), Math.max(0, Math.min(CAPACITY, level)));
	}

	public static ItemStack charged(Item item) {
		ItemStack is = new ItemStack(item);
		is.set(ReactorComponents.CHARGE.get(), CREATIVE_CHARGE);
		setWater(is, CAPACITY);
		return is;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player ep, InteractionHand hand) {
		ItemStack is = ep.getItemInHand(hand);
		if (!world.isClientSide && getWater(is) < CAPACITY) {
			HitResult mov = ep.pick(5, 1F, true);
			if (mov instanceof BlockHitResult bh && mov.getType() == HitResult.Type.BLOCK) {
				BlockPos p = bh.getBlockPos();
				if (world.getBlockState(p).is(Blocks.WATER) && world.getFluidState(p).isSource()) {
					setWater(is, getWater(is)+1000);
					world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
					return InteractionResultHolder.success(is);
				}
			}
		}
		ep.startUsingItem(hand);
		return InteractionResultHolder.consume(is);
	}

	@Override
	public void onUseTick(Level world, LivingEntity ep, ItemStack is, int count) {
		if (getCharge(is) <= 0 || getWater(is) <= 0)
			return;
		double r = 1;
		double d = 2.5-2+world.random.nextDouble()*4;
		Vec3 vec = ep.getLookAngle();
		double dx = ep.getX()+vec.x*d;
		double dy = ep.getY()+ep.getEyeHeight()+vec.y*d;
		double dz = ep.getZ()+vec.z*d;
		if (count%TICK_PER_KJ == 0 && !world.isClientSide) {
			for (RadiationEntity e : world.getEntitiesOfClass(RadiationEntity.class, new AABB(dx, dy, dz, dx, dy, dz).inflate(r)))
				e.clean();
		}
		if (world.isClientSide) {
			int n = 8-4+world.random.nextInt(9);
			for (int i = 0; i < n; i++) {
				double v = 0.1875-0.0625+world.random.nextDouble()*0.125;
				double vx = vec.x*v-0.001+world.random.nextDouble()*0.002;
				double vy = vec.y*v;
				double vz = vec.z*v-0.001+world.random.nextDouble()*0.002;
				world.addParticle(ParticleTypes.RAIN, dx, dy, dz, vx, vy, vz);
			}
		}
	}

	@Override
	public void releaseUsing(ItemStack is, Level world, LivingEntity ep, int ticksLeft) {
		if (!(ep instanceof Player p && p.getAbilities().instabuild)) {
			int used = this.getUseDuration(is, ep)-ticksLeft;
			is.set(ReactorComponents.CHARGE.get(), Math.max(0, getCharge(is)-(used/TICK_PER_KJ)));
			setWater(is, getWater(is)-WATER_PER_TICK*used);
		}
	}

	@Override
	public UseAnim getUseAnimation(ItemStack is) {
		return UseAnim.BOW;
	}

	@Override
	public int getUseDuration(ItemStack is, LivingEntity e) {
		return Math.min(72000, Math.min(getCharge(is)*TICK_PER_KJ, getWater(is)/WATER_PER_TICK));
	}

	/** Original getItemDisplayName: the charge in the name. */
	@Override
	public Component getName(ItemStack is) {
		return Component.translatable(this.getDescriptionId(is)).append(" ("+getCharge(is)+" kJ)");
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext ctx, List<Component> li, TooltipFlag flag) {
		li.add(Component.literal(String.format("Water: %d/%d mB", getWater(is), CAPACITY)));
	}

	/** Original setCharged (worktable with a wound coil): the tool takes the coil's charge, the coil keeps the tool's old one. */
	@Override
	public int setCharged(ItemStack is, int charge, boolean strongcoil) {
		int ret = is.getOrDefault(ReactorComponents.CHARGE.get(), 0);
		is.set(ReactorComponents.CHARGE.get(), charge);
		return ret;
	}
}
