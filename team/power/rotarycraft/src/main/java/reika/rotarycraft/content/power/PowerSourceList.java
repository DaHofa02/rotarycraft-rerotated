/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.power;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.api.power.PowerGenerator;
import reika.rotarycraft.api.power.PowerTracker;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.blockentity.IOMachineBlockEntity;
import reika.rotarycraft.content.blockentity.SplitterBlockEntity;

/** Original: Reika.RotaryCraft.Auxiliary.PowerSourceList. Collects the engines feeding a machine; detects shaft loops. */
public class PowerSourceList implements PowerTracker {

	private final HashSet<PowerWrapper> engines = new HashSet<>();
	private final HashSet<ShaftMerger> mergers = new HashSet<>();

	private ShaftMerger caller;

	private boolean isLooping = false;
	private boolean isBedrock = false;
	private boolean errored = false;

	@Override
	public long getMaxGennablePower() {
		long pwr = 0;
		for (PowerWrapper eng : engines) {
			pwr += eng.generator.getMaxPower();
		}
		return pwr;
	}

	@Override
	public long getRealMaxPower() {
		long pwr = 0;
		for (PowerWrapper eng : engines) {
			pwr += eng.generator.getCurrentPower();
		}
		return pwr;
	}

	public PowerSourceList addSource(PowerGenerator te) {
		engines.add(new PowerWrapper(te));
		return this;
	}

	@Override
	public boolean isLooping() {
		return isLooping;
	}

	public static PowerSourceList getAllFrom(Level world, Direction dir, BlockPos pos, PowerSourceTracker io, ShaftMerger caller) {
		PowerSourceList pwr = new PowerSourceList();

		BlockEntity tile = world.getBlockEntity(pos);

		if (caller != null) {
			if (pwr.passesThrough(caller) || (tile instanceof ShaftMerger sm && pwr.passesThrough(sm)) || tile == caller) {
				pwr.isLooping = true;
				caller.onPowerLooped(pwr);
				return pwr;
			}
		}

		if (tile instanceof ShaftMerger sm) {
			pwr.mergers.add(sm);
			if (tile == caller) {
				pwr.isLooping = true;
			}
			pwr.isBedrock = tile instanceof SplitterBlockEntity spl && spl.isBedrock(); //require ALL to be bedrock
		}

		if (pwr.errored) {
			fail(pwr, caller, world, pos);
		}

		try {
			if (tile instanceof IOMachineBlockEntity te) {
				if (!te.isWritingTo(io) && !te.isWritingTo2(io)) {
					return pwr;
				}
				if (te.isReadingFrom(io) || te.isReadingFrom2(io) || te.isReadingFrom3(io) || te.isReadingFrom4(io)) {
					return pwr;
				}
				pwr.addAll(te.getPowerSources(io, caller));
			}
			else if (tile instanceof PowerSourceTracker pst) {
				pwr.addAll(pst.getPowerSources(io, caller));
			}
			else if (tile instanceof PowerGenerator gen) {
				pwr.addSource(gen);
			}
			pwr.caller = caller;

			if (pwr.passesThrough(caller)) {
				pwr.isLooping = true;
			}

			return pwr;
		}
		catch (StackOverflowError e) {
			RotaryCraft.LOGGER.error("PowerSourceList SOE @ {}, called from {}!", pwr, caller);
			pwr.errored = true;
			return pwr;
		}
	}

	private static void fail(PowerSourceList pwr, ShaftMerger caller, Level world, BlockPos pos) {
		if (caller != null) {
			caller.fail();
		}
		else {
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 3, Level.ExplosionInteraction.NONE);
		}
	}

	public void addAll(PowerSourceList pwr) {
		for (PowerWrapper te : pwr.engines) {
			this.addSource(te.generator);
		}
		errored = errored || pwr.errored;
	}

	@Override
	public String toString() {
		if (engines.isEmpty()) {
			return "[None]";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		for (PowerWrapper gen : engines) {
			sb.append(gen.generator);
		}
		return sb.toString();
	}

	public ArrayList<String> getMessages() {
		ArrayList<String> li = new ArrayList<>();
		if (engines.isEmpty()) {
			li.add("[None]");
		}
		else {
			for (PowerWrapper gen : engines) {
				li.add("  "+gen.generator.toString());
			}
		}
		return li;
	}

	@Override
	public boolean contains(PowerGenerator te) {
		return engines.contains(new PowerWrapper(te));
	}

	@Override
	public boolean calledFrom(ShaftMerger sm) {
		return caller == sm;
	}

	@Override
	public boolean passesThrough(ShaftMerger sm) {
		return mergers.contains(sm);
	}

	@Override
	public boolean isEngineSpam() {
		if (this.isEmpty())
			return false;
		if (this.isBedrock())
			return false;
		long sum = this.getMaxGennablePower();
		long avg = sum/engines.size();
		return avg > 0 && sum/avg > 4;
	}

	public int size() {
		return engines.size();
	}

	public boolean isEmpty() {
		return engines.isEmpty();
	}

	public boolean isBedrock() {
		return isBedrock;
	}

	public static PowerSourceList combine(PowerSourceList in1, PowerSourceList in2, ShaftMerger caller) {
		PowerSourceList psl = new PowerSourceList();
		psl.engines.addAll(in1.engines);
		psl.engines.addAll(in2.engines);

		psl.mergers.addAll(in1.mergers);
		psl.mergers.addAll(in2.mergers);

		if (psl.mergers.contains(caller)) {
			psl.isLooping = true;
		}

		psl.caller = caller;

		return psl;
	}

	/** Identifies a generator by its level and position, like the original WorldLocation-based wrapper. */
	private record PowerWrapper(PowerGenerator generator) {

		private Object key() {
			BlockEntity te = (BlockEntity)generator;
			return Objects.hash(te.getLevel() == null ? null : te.getLevel().dimension(), te.getBlockPos());
		}

		@Override
		public int hashCode() {
			return this.key().hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof PowerWrapper w))
				return false;
			BlockEntity a = (BlockEntity)generator;
			BlockEntity b = (BlockEntity)w.generator;
			return a.getBlockPos().equals(b.getBlockPos()) && Objects.equals(a.getLevel() == null ? null : a.getLevel().dimension(), b.getLevel() == null ? null : b.getLevel().dimension());
		}
	}
}
