/*******************************************************************************
 * Practice factory zone (see Factory): small RotaryCraft machines - winder, cooling fin, fermenter, drying bed - built and run by a
 * simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.DryingBedBlockEntity;
import reika.rotarycraft.content.blockentity.FermenterBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.WinderBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSmallMachines {

	public static final List<String> BLOCKS = List.of("rotarycraft:winder", "rotarycraft:cooling_fin", "rotarycraft:fermenter", "rotarycraft:drying_bed");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * DC engine -> wooden 2:1 gearbox with a cooling fin set on its top (aimed at the gearbox below) -> winder with a spring put in through
	 * its GUI: the spring is wound to torque/stiffness = 8 kJ. A gas engine on a fuel line from an ethanol barrel drives a fermenter that
	 * gets a water bucket filled at a water barrel, sugar and dirt: it makes yeast. A drying bed gets another water bucket and dries it
	 * into four salt.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1500, batch = "factory")
	public static void winderFermenterDryingBed(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.WOOD, 2); //wood needs no lubricant: a dry steel gearbox wears and loses torque
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.DC_ENGINE.get()), new ItemStack(Items.REDSTONE_BLOCK), gbx,
				new ItemStack(RotaryBlocks.COOLING_FIN.get()), new ItemStack(RotaryBlocks.WINDER.get()), new ItemStack(RotaryItems.SPRING.get()),
				new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), new ItemStack(RotaryBlocks.FERMENTER.get()),
				new ItemStack(Items.SUGAR), new ItemStack(Items.DIRT), new ItemStack(Items.BUCKET), new ItemStack(RotaryBlocks.DRYING_BED.get()));
		Factory.fluidBarrel(h, new BlockPos(8, 2, 8), Fluids.WATER);
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());

		// winder line along z = 2, looking east
		Factory.take(p, chest, RotaryBlocks.DC_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, Items.REDSTONE_BLOCK, 1);
		Factory.take(p, chest, gbx, 1);
		Factory.take(p, chest, RotaryBlocks.COOLING_FIN.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.WINDER.get().asItem(), 1);
		Factory.takeExactly(p, chest, RotaryItems.SPRING.get(), 1);
		p.place(new BlockPos(6, 2, 2), RotaryBlocks.DC_ENGINE.get(), Direction.EAST);
		p.place(new BlockPos(6, 2, 1), Items.REDSTONE_BLOCK, Direction.EAST);
		p.place(new BlockPos(5, 2, 2), gbx, Direction.EAST);
		p.placeSneaking(new BlockPos(5, 3, 2), new ItemStack(RotaryBlocks.COOLING_FIN.get()), Direction.EAST);
		p.place(new BlockPos(4, 2, 2), RotaryBlocks.WINDER.get(), Direction.EAST);
		p.open(new BlockPos(4, 2, 2));
		p.putInto(0, RotaryItems.SPRING.get(), 1);
		p.closeMenu();

		// fermenter line along z = 5 on a fuel line
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.FERMENTER.get().asItem(), 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(5, 3, 5), new ItemStack(RotaryBlocks.FERMENTER.get()), Direction.EAST);
		BlockPos ferm = new BlockPos(5, 3, 5), bed = new BlockPos(3, 2, 7), water = new BlockPos(8, 2, 8);
		Factory.takeExactly(p, chest, Items.BUCKET, 1);
		p.use(water, Items.BUCKET);
		p.use(ferm, Items.WATER_BUCKET);
		Factory.take(p, chest, Items.SUGAR, 1);
		Factory.take(p, chest, Items.DIRT, 1);
		p.open(ferm);
		p.putInto(0, Items.SUGAR, 4);
		p.putInto(1, Items.DIRT, 4);
		p.closeMenu();

		// drying bed
		Factory.take(p, chest, RotaryBlocks.DRYING_BED.get().asItem(), 1);
		p.place(bed, RotaryBlocks.DRYING_BED.get(), Direction.EAST);
		p.use(water, Items.BUCKET);
		p.use(bed, Items.WATER_BUCKET);

		WinderBlockEntity w = (WinderBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 2));
		FermenterBlockEntity f = (FermenterBlockEntity)h.getBlockEntity(ferm);
		DryingBedBlockEntity d = (DryingBedBlockEntity)h.getBlockEntity(bed);
		h.assertTrue(((RotaryCraftBlockEntity)h.getBlockEntity(new BlockPos(5, 3, 2))).getBlockMetadata() == 0, "cooling fin not aimed at the gearbox below");
		h.assertTrue(f.getWater() == 1000 && d.getTank().getFluidAmount() == 1000, "water: fermenter "+f.getWater()+", bed "+d.getTank().getFluidAmount());
		h.succeedWhen(() -> {
			ItemStack spring = w.getInventory().getStackInSlot(0);
			h.assertTrue(w.getMaxWind() == 8 && (spring.isEmpty() || CoilItem.getCharge(spring) == 8), //a wound spring may snap, as in the original
					"winder "+w.getOmega()+" rad/s @ "+w.getTorque()+" Nm, max "+w.getMaxWind()+", spring "+(spring.isEmpty() ? "snapped" : CoilItem.getCharge(spring)+" kJ"));
			h.assertTrue(f.getInventory().getStackInSlot(2).is(RotaryItems.YEAST.get()), "fermenter at "+f.getTemperature()+" C, "+f.omega+" rad/s: "+f.getInventory().getStackInSlot(2));
			ItemStack salt = d.getInventory().getStackInSlot(0);
			h.assertTrue(salt.is(RotaryItems.part(CraftingPart.SALT)) && salt.getCount() == 4, "drying bed "+salt);
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 3, 8));
			p.leave();
		});
	}
}
