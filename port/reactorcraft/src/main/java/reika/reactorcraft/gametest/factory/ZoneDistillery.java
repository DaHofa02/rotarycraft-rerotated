/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft centrifuge and fractionation unit, driven fast from below by gas engines through
 * speed-up gearboxes and bevel gears, built and run by a simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.CentrifugeBlockEntity;
import reika.rotarycraft.content.blockentity.FractionatorBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneDistillery {

	public static final List<String> BLOCKS = List.of("rotarycraft:centrifuge", "rotarycraft:fractionator");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Two lines, each: ethanol barrel -> fuel line -> gas engine (512 rad/s @ 128 Nm) -> bedrock gearbox switched to speed-up with the
	 * screwdriver -> bevel gears turning the power up into the machine standing on them (both read their power from below).
	 * The centrifuge (8:1, 4096 rad/s @ 16 Nm) gets canola husks through its GUI and fills its tank with lubricant (90 mB per husk).
	 * The fractionation unit (16:1, 8192 rad/s @ 8 Nm = its minimum 65536 W) gets its six ingredients and a ghast tear through its GUI and
	 * ethanol from a fuel line at its side; it makes jet fuel.
	 *
	 * Plan (x = 2..8, looking east; machines at y = 4 on bevel gears at y = 3 on a dirt block):
	 *
	 *   z=2  .  .  .  C/B  G  E  barrel/fuel line      centrifuge line
	 *   z=5  .  .  L  F/B  G  E  barrel/fuel line      fractionator line, L = ethanol fuel line with a barrel above it
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1200, batch = "factory")
	public static void centrifugeAndFractionator(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos a = new BlockPos(0, 2, 8), b = new BlockPos(1, 2, 8);
		ItemStack gbx8 = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 8); //bedrock gears need no lubricant and do not wear
		ItemStack gbx16 = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16);
		Factory.infiniteChest(h, a, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), gbx8, gbx16,
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.CENTRIFUGE.get()), new ItemStack(RotaryBlocks.FRACTIONATOR.get()),
				new ItemStack(RotaryItems.SCREWDRIVER.get()), new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL));
		Factory.infiniteChest(h, b, new ItemStack(RotaryItems.CANOLA_HUSKS.get()), new ItemStack(Items.BLAZE_POWDER),
				new ItemStack(RotaryItems.part(CraftingPart.COALDUST)), new ItemStack(Items.MAGMA_CREAM), new ItemStack(Items.PINK_DYE),
				new ItemStack(RotaryItems.part(CraftingPart.NETHERRACKDUST)), new ItemStack(RotaryItems.part(CraftingPart.TAR)), new ItemStack(Items.GHAST_TEAR));
		Factory.take(p, a, RotaryBlocks.FUEL_LINE.get().asItem(), 5);
		Factory.take(p, a, RotaryBlocks.GAS_ENGINE.get().asItem(), 2);
		Factory.takeExactly(p, a, gbx8.getItem(), 1); //both gearboxes are the same item, the ratio is a component
		Factory.take(p, a, gbx16, 1);
		Factory.take(p, a, RotaryBlocks.BEVEL_GEARS.get().asItem(), 2);
		Factory.take(p, a, RotaryBlocks.CENTRIFUGE.get().asItem(), 1);
		Factory.take(p, a, RotaryBlocks.FRACTIONATOR.get().asItem(), 1);
		Factory.take(p, a, RotaryItems.SCREWDRIVER.get(), 1);
		Factory.take(p, a, Items.DIRT, 4);
		Factory.take(p, a, Items.IRON_SHOVEL, 1);

		BlockPos cen = line(h, p, 2, gbx8, RotaryBlocks.CENTRIFUGE.get().asItem());
		BlockPos fra = line(h, p, 5, gbx16, RotaryBlocks.FRACTIONATOR.get().asItem());
		Factory.fluidBarrel(h, new BlockPos(4, 5, 5), RotaryFluids.ETHANOL.get());
		p.placeSneaking(new BlockPos(4, 4, 5), new ItemStack(RotaryBlocks.FUEL_LINE.get()), Direction.EAST); //set against the fractionator

		// load the machines through their GUIs
		Factory.take(p, b, RotaryItems.CANOLA_HUSKS.get(), 3);
		p.open(cen);
		p.putInto(0, RotaryItems.CANOLA_HUSKS.get(), 3);
		p.closeMenu();
		Item[] ingredients = {Items.BLAZE_POWDER, RotaryItems.part(CraftingPart.COALDUST), Items.MAGMA_CREAM, Items.PINK_DYE,
				RotaryItems.part(CraftingPart.NETHERRACKDUST), RotaryItems.part(CraftingPart.TAR)};
		for (Item i : ingredients)
			Factory.takeExactly(p, b, i, 8);
		Factory.takeExactly(p, b, Items.GHAST_TEAR, 1);
		p.open(fra);
		for (int i = 0; i < 6; i++)
			p.putInto(i, ingredients[i], 8);
		p.putInto(6, Items.GHAST_TEAR, 1);
		p.closeMenu();

		CentrifugeBlockEntity c = (CentrifugeBlockEntity)h.getBlockEntity(cen);
		FractionatorBlockEntity f = (FractionatorBlockEntity)h.getBlockEntity(fra);
		h.succeedWhen(() -> {
			h.assertTrue(c.omega == 4096 && c.torque == 16, "centrifuge input "+c.omega+" rad/s @ "+c.torque+" Nm");
			h.assertTrue(c.getFluid().is(RotaryFluids.LUBRICANT.get()) && c.getFluidLevel() >= 90, "centrifuge tank "+c.getFluidLevel()+" mB, husks left "+c.getInventory().getStackInSlot(0));
			h.assertTrue(f.omega == 8192 && f.power >= 65536, "fractionator input "+f.omega+" rad/s @ "+f.torque+" Nm");
			h.assertTrue(f.getFuelLevel() > 0, "no jet fuel: ethanol "+f.getInputLevel()+" mB, pressure "+f.getPressure()+" kPa");
			h.assertTrue(f.getInventory().getStackInSlot(6).is(Items.GHAST_TEAR), "ghast tear used up");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 5, 8));
			p.leave();
		});
	}

	/**
	 * One line along z: barrel (8, 3, z) over a fuel line, a fuel line under the gas engine (7, 3, z), the gearbox (6, 3, z) switched to
	 * speed-up, the machine (5, 4, z) set on two dirt blocks, the upper one dug away again and replaced by the bevel gears (placed last,
	 * so they find their route east -> up). Returns the machine's position.
	 */
	private static BlockPos line(GameTestHelper h, SimPlayer p, int z, ItemStack gearbox, Item machine) {
		Factory.fluidBarrel(h, new BlockPos(8, 3, z), RotaryFluids.ETHANOL.get());
		p.place(new BlockPos(8, 2, z), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.place(new BlockPos(7, 2, z), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(7, 3, z), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos g = new BlockPos(6, 3, z), m = new BlockPos(5, 4, z);
		p.placeSneaking(g, gearbox, Direction.EAST);
		p.hold(RotaryItems.SCREWDRIVER.get());
		p.player.setShiftKeyDown(true); //sneak-right-click with the screwdriver: reduction <-> speed-up
		try {
			p.useOn(g, Direction.UP);
		}
		finally {
			p.player.setShiftKeyDown(false);
		}
		h.assertTrue(!((GearboxBlockEntity)h.getBlockEntity(g)).reduction, "gearbox at "+g+" not switched to speed-up");
		p.place(new BlockPos(5, 2, z), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(5, 3, z), Items.DIRT, Direction.EAST);
		p.place(m, machine, Direction.EAST);
		p.mine(new BlockPos(5, 3, z), Items.IRON_SHOVEL);
		p.placeSneaking(new BlockPos(5, 3, z), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		return m;
	}
}
