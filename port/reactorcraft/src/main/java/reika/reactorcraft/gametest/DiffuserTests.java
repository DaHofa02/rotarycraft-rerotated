/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.powergen.SteamDiffuserBlockEntity;
import reika.reactorcraft.powergen.SteamLineBlockEntity;
import reika.reactorcraft.powergen.WorkingFluid;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

/**
 * Original TileEntitySteamDiffuser (see SteamDiffuserBlockEntity). The steam fluid comes from Mekanism, which the ReactorCraft test runs load
 * (c:steam); every unit of ReactorCraft steam the diffuser takes becomes RATIO*1000 = 80000 mB of it.
 */
@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DiffuserTests {

	private static final String PLATFORM = "platform_9x4x9";

	/** Loads steam into a steam line the way a saved world would (as SteamTests.giveSteam). */
	private static void giveSteam(GameTestHelper h, BlockEntity te, int steam, WorkingFluid f) {
		CompoundTag t = te.saveWithoutMetadata(h.getLevel().registryAccess());
		t.putInt("energy", steam);
		f.saveToNBT(t);
		te.loadWithComponents(t, h.getLevel().registryAccess());
	}

	/** Steam line at (3,1,4), diffuser east of it facing west (metadata 0). */
	private static SteamDiffuserBlockEntity setup(GameTestHelper h, int steam, WorkingFluid f) {
		h.setBlock(new BlockPos(3, 1, 4), ReactorBlocks.STEAM_LINE.get());
		giveSteam(h, h.getBlockEntity(new BlockPos(3, 1, 4)), steam, f);
		h.setBlock(new BlockPos(4, 1, 4), ReactorBlocks.STEAM_DIFFUSER.get());
		SteamDiffuserBlockEntity te = (SteamDiffuserBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		te.setBlockMetadata(0);
		return te;
	}

	/**
	 * 8 units of water steam in the line: the diffuser takes all of them (a quarter of the difference +1 per tick, efficiency 1) and turns
	 * each into 80000 mB of steam fluid - 640000 mB in the end, line and diffuser empty.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void diffuserTurnsSteamIntoFluid(GameTestHelper h) {
		Fluid steamFluid = SteamDiffuserBlockEntity.getSteamFluid();
		h.assertTrue(steamFluid != null, "no fluid in c:steam (Mekanism not loaded?)");
		h.assertTrue(SteamDiffuserBlockEntity.RATIO == 80, "RATIO "+SteamDiffuserBlockEntity.RATIO);
		SteamDiffuserBlockEntity te = setup(h, 8, WorkingFluid.WATER);
		SteamLineBlockEntity line = (SteamLineBlockEntity)h.getBlockEntity(new BlockPos(3, 1, 4));
		h.runAtTickTime(40, () -> {
			h.assertTrue(line.getSteam() == 0 && te.getSteam() == 0, "steam left: line "+line.getSteam()+", diffuser "+te.getSteam());
			h.assertTrue(te.getTank().getFluid().getFluid().isSame(steamFluid) && te.getTank().getFluidAmount() == 8*80000,
					"tank "+te.getTank().getFluid().getFluid()+" "+te.getTank().getFluidAmount()+" mB");
			h.succeed();
		});
	}

	/** Ammonia steam has the working fluid efficiency 2: the same 8 units in the line give 16 units in the diffuser, 1280000 mB. */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void diffuserCountsAmmoniaDouble(GameTestHelper h) {
		SteamDiffuserBlockEntity te = setup(h, 8, WorkingFluid.AMMONIA);
		h.runAtTickTime(40, () -> {
			h.assertTrue(te.getTank().getFluidAmount() == 16*80000, "tank "+te.getTank().getFluidAmount()+" mB");
			h.succeed();
		});
	}

	/**
	 * Much steam: the first conversion already fills the 2500 B tank (units are used up even if their fluid does not fit, as in the
	 * original). A pipe behind the diffuser (east) drains it; a pipe on its side (south) does not connect and stays empty.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void diffuserFillsTankAndFeedsPipeBehind(GameTestHelper h) {
		SteamDiffuserBlockEntity te = setup(h, 1000, WorkingFluid.WATER);
		h.runAtTickTime(2, () -> {
			h.assertTrue(te.getTank().getFluidAmount() == SteamDiffuserBlockEntity.CAPACITY, "tank "+te.getTank().getFluidAmount()+" mB");
			h.setBlock(new BlockPos(5, 1, 4), RotaryBlocks.PIPE.get());
			h.setBlock(new BlockPos(4, 1, 5), RotaryBlocks.PIPE.get());
		});
		h.runAtTickTime(60, () -> {
			PipingBlockEntity back = (PipingBlockEntity)h.getBlockEntity(new BlockPos(5, 1, 4));
			PipingBlockEntity side = (PipingBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 5));
			h.assertTrue(back.getFluidLevel() > 0 && back.getFluidType() != null && back.getFluidType().isSame(SteamDiffuserBlockEntity.getSteamFluid()),
					"pipe behind: "+back.getFluidType()+" "+back.getFluidLevel());
			h.assertTrue(side.getFluidLevel() == 0, "pipe on the side took "+side.getFluidLevel()+" mB");
			h.succeed();
		});
	}
}
