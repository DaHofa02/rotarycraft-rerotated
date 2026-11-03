/*******************************************************************************
 * Practice factory (see Factory): every placeable block of RotaryCraft, ReactorCraft and ElectriCraft must be built in some zone. The zones
 * are found by themselves: every class in this package with a {@code public static final List<String> BLOCKS}. Not part of the original
 * mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FactoryCoverage {

	private static final String PACKAGE = FactoryCoverage.class.getPackageName();

	/** The zones: classes of this package (from the mod's scan data) with a static BLOCKS list. */
	public static List<Class<?>> zones() {
		Set<String> names = new TreeSet<>();
		ModList.get().getAllScanData().forEach(sd -> sd.getClasses().forEach(c -> {
			String n = c.clazz().getClassName();
			if (n.startsWith(PACKAGE+".") && !n.contains("$"))
				names.add(n);
		}));
		List<Class<?>> li = new ArrayList<>();
		for (String n : names) {
			try {
				Class<?> c = Class.forName(n);
				Field f = c.getField("BLOCKS");
				if (Modifier.isStatic(f.getModifiers()))
					li.add(c);
			}
			catch (ReflectiveOperationException e) {
				//not a zone
			}
		}
		return li;
	}

	@SuppressWarnings("unchecked")
	@GameTest(template = "platform_9x4x9", batch = "factory")
	public static void everyBlockIsInTheFactory(GameTestHelper h) throws ReflectiveOperationException {
		Set<String> covered = new TreeSet<>();
		StringBuilder per = new StringBuilder();
		for (Class<?> z : zones()) {
			List<String> b = (List<String>)z.getField("BLOCKS").get(null);
			covered.addAll(b);
			per.append(z.getSimpleName()).append(' ').append(b.size()).append(", ");
		}
		List<String> missing = new ArrayList<>();
		for (String id : Factory.required())
			if (!covered.contains(id))
				missing.add(id);
		ReactorCraft.LOGGER.info("FACTORY zones: {}required {}, covered {}, missing {}: {}", per, Factory.required().size(), covered.size(), missing.size(), missing);
		h.assertTrue(missing.isEmpty(), missing.size()+" blocks are in no factory zone: "+missing);
		h.succeed();
	}
}
