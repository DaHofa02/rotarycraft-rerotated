/*******************************************************************************
 * Tests for the ported RotaryCraft drying bed (original TileEntityDryingBed, RecipesDryingBed). Private NeoForge 1.21.1 port of
 * RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.DryingBedBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DryingBedTests {

	private static final String PLATFORM = "platform_9x4x9";

	/**
	 * A stack of four water buckets is refused (4000 mB do not fit); 1000 mB water: after 400 ticks all four portions dry at once into 4 salt; liquid from below is refused, other
	 * liquids without a recipe too.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 500)
	public static void waterDriesIntoSalt(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 1, 4), RotaryBlocks.DRYING_BED.get());
		DryingBedBlockEntity te = (DryingBedBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		h.assertTrue(te.fill(Direction.DOWN, new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE) == 0, "filled from below");
		h.assertTrue(te.fill(Direction.UP, new FluidStack(net.minecraft.core.registries.BuiltInRegistries.FLUID.get(
				net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("rotarycraft", "lubricant")), 100), IFluidHandler.FluidAction.SIMULATE) == 0, "took lubricant");
		Player ep = h.makeMockPlayer(GameType.SURVIVAL);
		ep.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 1, 2))));
		ItemStack bucket = new ItemStack(Items.WATER_BUCKET, 4);
		ep.setItemInHand(InteractionHand.MAIN_HAND, bucket);
		BlockPos a = h.absolutePos(new BlockPos(4, 1, 4));
		h.getLevel().getBlockState(a).useItemOn(bucket, h.getLevel(), ep, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(a), Direction.UP, a, false));
		//original: the whole stack of buckets has to fit (4 x 1000 > 2000 mB), so it is refused
		h.assertTrue(te.getTank().isEmpty() && ep.getMainHandItem().getCount() == 4, "bucket stack: "+te.getTank().getFluidAmount());
		te.fill(Direction.UP, new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		h.runAtTickTime(390, () -> h.assertTrue(te.getInventory().getStackInSlot(0).isEmpty(), "dried early"));
		h.runAtTickTime(410, () -> {
			ItemStack out = te.getInventory().getStackInSlot(0);
			h.assertTrue(out.is(RotaryItems.part(CraftingPart.SALT)) && out.getCount() == 4 && te.getTank().isEmpty(), "out "+out+", tank "+te.getTank().getFluidAmount());
			h.succeed();
		});
	}

	/** One water bucket by hand fills 1000 mB and gives the empty bucket back; lava dries 1000 mB into one gold nugget. */
	@GameTest(template = PLATFORM, timeoutTicks = 500)
	public static void bucketAndLava(GameTestHelper h) {
		h.setBlock(new BlockPos(2, 1, 4), RotaryBlocks.DRYING_BED.get());
		DryingBedBlockEntity w = (DryingBedBlockEntity)h.getBlockEntity(new BlockPos(2, 1, 4));
		Player ep = h.makeMockPlayer(GameType.SURVIVAL);
		ep.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 1, 2)))); //machines check the player's reach
		ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
		ep.setItemInHand(InteractionHand.MAIN_HAND, bucket);
		BlockPos a = h.absolutePos(new BlockPos(2, 1, 4));
		h.getLevel().getBlockState(a).useItemOn(bucket, h.getLevel(), ep, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(a), Direction.UP, a, false));
		h.assertTrue(w.getTank().getFluidAmount() == 1000 && ep.getMainHandItem().is(Items.BUCKET), "bucket: "+w.getTank().getFluidAmount()+", hand "+ep.getMainHandItem());
		h.setBlock(new BlockPos(6, 1, 4), RotaryBlocks.DRYING_BED.get());
		DryingBedBlockEntity l = (DryingBedBlockEntity)h.getBlockEntity(new BlockPos(6, 1, 4));
		l.fill(Direction.NORTH, new FluidStack(Fluids.LAVA, 1500), IFluidHandler.FluidAction.EXECUTE);
		h.runAtTickTime(410, () -> {
			h.assertTrue(l.getInventory().getStackInSlot(0).is(Items.GOLD_NUGGET) && l.getInventory().getStackInSlot(0).getCount() == 1
					&& l.getTank().getFluidAmount() == 500, "lava bed "+l.getInventory().getStackInSlot(0)+", "+l.getTank().getFluidAmount());
			h.succeed();
		});
	}

	/** Loading from saved NBT (as a structure or /setblock does): liquid, product and progress come back. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void loadsFromNbt(GameTestHelper h) throws Exception {
		h.setBlock(new BlockPos(4, 1, 4), RotaryBlocks.DRYING_BED.get());
		DryingBedBlockEntity te = (DryingBedBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		te.loadWithComponents(net.minecraft.nbt.TagParser.parseTag("{tank:{Fluid:{id:\"minecraft:water\",amount:1500}},inv:{Size:1,Items:[{Slot:0,id:\"rotarycraft:salt\",count:12}]},progress:100}"),
				h.getLevel().registryAccess());
		h.assertTrue(te.getTank().getFluidAmount() == 1500 && te.getInventory().getStackInSlot(0).getCount() == 12 && te.progress == 100,
				"tank "+te.getTank().getFluidAmount()+", inv "+te.getInventory().getStackInSlot(0)+", progress "+te.progress);
		h.succeed();
	}
}
