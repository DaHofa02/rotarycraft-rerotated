/*******************************************************************************
 * Tests for the ported ReactorCraft permanent magnets (original ReactorItems.MAGNET and its RecipesCompactor entries). Private NeoForge
 * 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.content.blockentity.CompactorBlockEntity;
import reika.rotarycraft.gametest.CompactorTests;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class MagnetTests {

	/** Four lodestone (magnetite) at 5000 kPa and 100 C become 2 of the weakest magnet; names show the field strength. */
	@GameTest(template = "platform_9x4x9", timeoutTicks = 3200)
	public static void compactorMakesMagnets(GameTestHelper h) {
		CompactorBlockEntity c = CompactorTests.rig(h, 2);
		c.setTemperature(150);
		for (int i = 0; i < 4; i++)
			c.getInventory().setStackInSlot(i, new ItemStack(ReactorItems.raw(ReactorItems.Raw.MAGNETITE).get()));
		h.assertTrue(new ItemStack(ReactorItems.MAGNETS.get(7).get()).getHoverName().getString().endsWith("(16.384 T)"), "strongest magnet name "
				+new ItemStack(ReactorItems.MAGNETS.get(7).get()).getHoverName().getString());
		h.assertTrue(new ItemStack(ReactorItems.MAGNETS.get(0).get()).getHoverName().getString().endsWith("(1.000 mT)"), "weakest magnet name");
		h.succeedWhen(() -> {
			ItemStack out = c.getInventory().getStackInSlot(4);
			h.assertTrue(out.is(ReactorItems.MAGNETS.get(0).get()) && out.getCount() == 2, "output "+out+" at "+c.getPressure()+" kPa, "+c.getTemperature()+" C");
		});
	}
}
