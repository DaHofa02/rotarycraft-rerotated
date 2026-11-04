/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.reactor.NeutronReflectorBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;

/** Original TileEntityNeutronReflector.onNeutron (see NeutronReflectorBlockEntity). */
@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ReflectorTests {

	private static final String PLATFORM = "platform_9x4x9";

	/**
	 * 4000 fast fission neutrons flying east into a reflector: every one comes out thermal; one in four is sent back west (not absorbed),
	 * of the others half are absorbed - expected 25 % reflected, 37.5 % absorbed, 37.5 % passing on (bounds about 6 standard deviations).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void reflectorSendsBackAQuarter(GameTestHelper h) {
		BlockPos p = new BlockPos(4, 1, 4);
		h.setBlock(p, ReactorBlocks.NEUTRON_REFLECTOR.get());
		NeutronReflectorBlockEntity te = (NeutronReflectorBlockEntity)h.getBlockEntity(p);
		int n = 4000, reflected = 0, absorbed = 0, notThermal = 0, badReflection = 0;
		for (int i = 0; i < n; i++) {
			NeutronEntity e = new NeutronEntity(h.getLevel(), te.getBlockPos(), Direction.EAST, NeutronEntity.NeutronType.FISSION);
			double vx = e.getDeltaMovement().x, vz = e.getDeltaMovement().z;
			boolean caught = te.onNeutron(e, h.getLevel(), te.getBlockPos());
			if (e.getNeutronSpeed() != NeutronEntity.NeutronSpeed.THERMAL)
				notThermal++;
			if (caught)
				absorbed++;
			else if (e.getDeltaMovement().x == -vx && vx != 0) {
				reflected++;
				if (e.getDeltaMovement().z != -vz || !e.hurtMarked)
					badReflection++;
			}
		}
		h.assertTrue(notThermal == 0, notThermal+" neutrons not moderated");
		h.assertTrue(badReflection == 0, badReflection+" reflections without reversed z motion or velocity sync");
		h.assertTrue(reflected >= 840 && reflected <= 1160, "reflected "+reflected+" of "+n+" (expected ~1000)");
		h.assertTrue(absorbed >= 1310 && absorbed <= 1690, "absorbed "+absorbed+" of "+n+" (expected ~1500)");
		h.succeed();
	}
}
