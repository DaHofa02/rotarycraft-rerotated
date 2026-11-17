/*******************************************************************************
 * Tests for the ported RotaryCraft spy cam and CCTV screen (original TileEntitySpyCam, TileEntityScreen). Private NeoForge 1.21.1 port of
 * RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.ScreenBlockEntity;
import reika.rotarycraft.content.blockentity.SpyCamBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.content.menu.SpyCamViewMenu;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SpyCamTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	private static SpyCamBlockEntity cam(GameTestHelper h, int x, int y, int z, ItemStack... dyes) {
		SpyCamBlockEntity cam = place(h, RotaryBlocks.SPY_CAM.get(), x, y, z, 0, SpyCamBlockEntity.class);
		cam.getInventory().setStackInSlot(0, CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), 50));
		for (int i = 0; i < dyes.length; i++)
			cam.getDyes().setStackInSlot(i, dyes[i]);
		return cam;
	}

	/**
	 * The spy cam three blocks up maps the highest block in each column below it (a two-high stone pillar: its top) and marks a cow on the
	 * floor with its 1.7.10 id 92; its colour code is its three dyes.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void spyCamMapsTheArea(GameTestHelper h) {
		SpyCamBlockEntity cam = cam(h, 4, 4, 4, new ItemStack(Items.RED_DYE), new ItemStack(Items.GREEN_DYE), new ItemStack(Items.BLUE_DYE));
		h.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
		h.setBlock(new BlockPos(2, 3, 2), Blocks.STONE);
		Cow cow = h.spawn(EntityType.COW, new BlockPos(6, 2, 5));
		cow.setNoAi(true);
		int r = SpyCamBlockEntity.MAXRANGE;
		h.succeedWhen(() -> {
			h.assertTrue(cam.isOn(), "camera off");
			h.assertTrue(java.util.Arrays.equals(cam.getColors(), new int[] {DyeColor.RED.getId(), DyeColor.GREEN.getId(), DyeColor.BLUE.getId()}), "colours");
			h.assertTrue(cam.getHeightAt(r-2, r-2) == h.absolutePos(new BlockPos(2, 3, 2)).getY(), "pillar top at "+cam.getHeightAt(r-2, r-2));
			h.assertTrue(cam.getHeightAt(r+1, r+1) == h.absolutePos(new BlockPos(5, 1, 5)).getY(), "floor at "+cam.getHeightAt(r+1, r+1));
			h.assertTrue(cam.getMobAt(r+1, r+2) == 92, "cow not on the map: "+cam.getMobAt(r+1, r+2));
		});
	}

	/**
	 * A screen powered from below (gas engine -> bevel gears up) with red, green and blue dyes calls up the camera with the same code (a
	 * camera with other dyes beside it is not chosen); without the third dye it does nothing and gives a signal.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void screenCallsUpTheCamera(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		ScreenBlockEntity screen = place(h, RotaryBlocks.SCREEN.get(), 4, 3, 4, 0, ScreenBlockEntity.class);
		screen.getInventory().setStackInSlot(0, new ItemStack(Items.RED_DYE));
		screen.getInventory().setStackInSlot(1, new ItemStack(Items.GREEN_DYE));
		cam(h, 1, 2, 7, new ItemStack(Items.RED_DYE), new ItemStack(Items.GREEN_DYE), new ItemStack(Items.WHITE_DYE));
		SpyCamBlockEntity right = cam(h, 7, 2, 7, new ItemStack(Items.RED_DYE), new ItemStack(Items.GREEN_DYE), new ItemStack(Items.BLUE_DYE));
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		boolean[] third = {false};
		h.succeedWhen(() -> {
			h.assertTrue(screen.getPower() == 65536, "screen at "+screen.getPower()+" W");
			if (!third[0]) {
				h.assertTrue(!screen.canRun() && screen.getRedstoneOverride() == 15, "runs with two dyes");
				screen.getInventory().setStackInSlot(2, new ItemStack(Items.BLUE_DYE));
				third[0] = true;
				h.fail("two dyes; now three");
			}
			h.assertTrue(screen.canRun() && screen.findCamera() == right && screen.getRedstoneOverride() == 0, "camera "+screen.findCamera());
			screen.activate(p.player);
			h.assertTrue(p.player.containerMenu instanceof SpyCamViewMenu m && m.tile == right, "menu "+p.player.containerMenu);
			p.closeMenu();
			p.leave();
		});
	}
}
