/*******************************************************************************
 * Tests for the port's bedrock tools and armour and the magnetizer's lodestone upgrade. Private NeoForge 1.21.1 port of RotaryCraft; not
 * for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.MagnetizerBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.item.BedrockTools;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BedrockToolTests {

	private static final String PLATFORM = "platform_9x8x9";

	/** All bedrock tools and armour are unbreakable; the pickaxe digs obsidian at 20 and gets its drop; the paxel digs stone, logs and dirt. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void bedrockToolsAreUnbreakable(GameTestHelper h) {
		for (var i : new net.neoforged.neoforge.registries.DeferredItem<?>[] {RotaryItems.BEDROCK_PICKAXE, RotaryItems.BEDROCK_AXE, RotaryItems.BEDROCK_SHOVEL,
				RotaryItems.BEDROCK_SWORD, RotaryItems.BEDROCK_HOE, RotaryItems.BEDROCK_SHEARS, RotaryItems.BEDROCK_SCYTHE, RotaryItems.BEDROCK_PAXEL,
				RotaryItems.BEDROCK_HELMET, RotaryItems.BEDROCK_CHESTPLATE, RotaryItems.BEDROCK_LEGGINGS, RotaryItems.BEDROCK_BOOTS})
			h.assertTrue(!new ItemStack(i.get()).isDamageableItem(), i.getId()+" can wear out");
		ItemStack pick = new ItemStack(RotaryItems.BEDROCK_PICKAXE.get());
		h.assertTrue(pick.getDestroySpeed(Blocks.OBSIDIAN.defaultBlockState()) == 20 && pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "pickaxe on obsidian");
		ItemStack paxel = new ItemStack(RotaryItems.BEDROCK_PAXEL.get());
		for (var b : new net.minecraft.world.level.block.Block[] {Blocks.STONE, Blocks.OAK_LOG, Blocks.DIRT, Blocks.OBSIDIAN})
			h.assertTrue(paxel.isCorrectToolForDrops(b.defaultBlockState()) && paxel.getDestroySpeed(b.defaultBlockState()) == 20, "paxel on "+b);
		h.assertTrue(!BedrockTools.paxelModLoaded(), "a paxel mod in the test run?");
		h.succeed();
	}

	/** The axe fells a tree of 6 logs (trunk and a branch) in one go; sneaking it would take one. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void bedrockAxeFellsTrees(GameTestHelper h) {
		for (int y = 2; y <= 6; y++)
			h.setBlock(new BlockPos(4, y, 4), Blocks.OAK_LOG);
		h.setBlock(new BlockPos(5, 6, 4), Blocks.OAK_LOG);
		var stand = h.spawn(EntityType.ARMOR_STAND, new Vec3(1.5, 2, 1.5));
		int n = BedrockTools.Axe.fell(h.getLevel(), h.absolutePos(new BlockPos(4, 2, 4)), Blocks.OAK_LOG, stand);
		h.assertTrue(n == 5 && h.getBlockState(new BlockPos(5, 6, 4)).isAir(), "felled "+n);
		stand.discard();
		h.succeed();
	}

	/** The sword wears a struck iron chestplate down by 100. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void bedrockSwordWearsArmour(GameTestHelper h) {
		var stand = h.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 2, 4.5));
		stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
		ItemStack sword = new ItemStack(RotaryItems.BEDROCK_SWORD.get());
		sword.getItem().hurtEnemy(sword, stand, stand);
		h.assertTrue(stand.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 100, "chestplate damage "+stand.getItemBySlot(EquipmentSlot.CHEST).getDamageValue());
		stand.discard();
		h.succeed();
	}

	/** The hoe tills the 3x3 of dirt around the clicked block. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void bedrockHoeTills3x3(GameTestHelper h) {
		for (BlockPos p : BlockPos.betweenClosed(new BlockPos(3, 1, 3), new BlockPos(5, 1, 5)))
			h.setBlock(p, Blocks.DIRT);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		p.give(new ItemStack(RotaryItems.BEDROCK_HOE.get()));
		p.hold(RotaryItems.BEDROCK_HOE.get());
		p.useOn(new BlockPos(4, 1, 4), Direction.UP);
		for (BlockPos q : BlockPos.betweenClosed(new BlockPos(3, 1, 3), new BlockPos(5, 1, 5)))
			h.assertTrue(h.getBlockState(q).is(Blocks.FARMLAND), "not tilled at "+q);
		p.leave();
		h.succeed();
	}

	/** The scythe cutting one ripe wheat cuts the other ripe wheat around it, not the young one. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void bedrockScytheHarvests(GameTestHelper h) {
		var ripe = ((CropBlock)Blocks.WHEAT).getStateForAge(7);
		for (int x = 2; x <= 6; x++) {
			h.setBlock(new BlockPos(x, 1, 4), Blocks.FARMLAND);
			h.setBlock(new BlockPos(x, 2, 4), x == 6 ? Blocks.WHEAT.defaultBlockState() : ripe);
		}
		var stand = h.spawn(EntityType.ARMOR_STAND, new Vec3(1.5, 2, 1.5));
		ItemStack scythe = new ItemStack(RotaryItems.BEDROCK_SCYTHE.get());
		scythe.getItem().mineBlock(scythe, h.getLevel(), ripe, h.absolutePos(new BlockPos(2, 2, 4)), stand);
		for (int x = 3; x <= 5; x++)
			h.assertTrue(h.getBlockState(new BlockPos(x, 2, 4)).isAir(), "ripe wheat left at "+x);
		h.assertTrue(h.getBlockState(new BlockPos(6, 2, 4)).is(Blocks.WHEAT), "young wheat cut");
		stand.discard();
		h.succeed();
	}

	/** The helmet worn gives night vision. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void bedrockHelmetGivesNightVision(GameTestHelper h) {
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		ItemStack helm = new ItemStack(RotaryItems.BEDROCK_HELMET.get());
		p.player.setItemSlot(EquipmentSlot.HEAD, helm);
		helm.getItem().inventoryTick(helm, h.getLevel(), p.player, 39, false);
		h.assertTrue(p.player.hasEffect(MobEffects.NIGHT_VISION), "no night vision");
		p.leave();
		h.succeed();
	}

	/** Lodestone upgrade: the magnetizer's cycle time halves, and it takes the upgrade only once. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void lodestoneUpgradeSpeedsTheMagnetizer(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 2, 4), RotaryBlocks.MAGNETIZER.get());
		MagnetizerBlockEntity m = (MagnetizerBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		int before = m.getOperationTime();
		ItemStack up = new ItemStack(RotaryItems.UPGRADE_LODESTONE.get());
		h.assertTrue(m.canUpgradeWith(up), "upgrade refused");
		m.upgrade(up);
		h.assertTrue(m.hasLodestoneUpgrade() && !m.canUpgradeWith(up) && m.getOperationTime() == Math.max(2, before/2), "time "+before+" -> "+m.getOperationTime());
		h.succeed();
	}
}
