/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft/DragonAPI). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.lib.data;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;

/** Original: Reika.DragonAPI.Instantiable.ParallelTicker - several named tick counters with caps. */
public class ParallelTicker {

	private final Map<String, Integer> tickers = new HashMap<>();
	private final Map<String, Integer> caps = new HashMap<>();

	public int getTickOf(String key) {
		return tickers.getOrDefault(key, -1);
	}

	public int getCapOf(String key) {
		return caps.getOrDefault(key, -1);
	}

	public void updateTicker(String key) {
		if (tickers.containsKey(key)) {
			tickers.put(key, tickers.get(key)+1);
		}
	}

	public ParallelTicker addTicker(String key) {
		tickers.put(key, 0);
		return this;
	}

	public ParallelTicker addTicker(String key, int cap) {
		this.addTicker(key);
		this.setCap(key, cap);
		return this;
	}

	public void setCap(String key, int cap) {
		if (tickers.containsKey(key)) {
			caps.put(key, cap);
		}
	}

	private boolean isAtCap(String key) {
		if (tickers.containsKey(key) && caps.containsKey(key)) {
			return tickers.get(key) >= caps.get(key);
		}
		return false;
	}

	public boolean checkCap(String key) {
		boolean cap = this.isAtCap(key);
		if (cap)
			this.resetTicker(key);
		return cap;
	}

	public void resetTicker(String key) {
		this.setTickOf(key, 0);
	}

	public void setTickOf(String key, int val) {
		if (tickers.containsKey(key)) {
			tickers.put(key, val);
		}
	}

	public float getPortionOfCap(String key) {
		if (tickers.containsKey(key) && caps.containsKey(key)) {
			if (caps.get(key) == 0)
				return -1F;
			else
				return (float)tickers.get(key)/(float)caps.get(key);
		}
		return 0F;
	}

	public void writeToNBT(CompoundTag NBT, String label) {
		CompoundTag tag = new CompoundTag();
		for (Map.Entry<String, Integer> e : tickers.entrySet()) {
			CompoundTag t = new CompoundTag();
			t.putInt("tick", e.getValue());
			if (caps.containsKey(e.getKey()))
				t.putInt("cap", caps.get(e.getKey()));
			tag.put(e.getKey(), t);
		}
		NBT.put(label, tag);
	}

	public void readFromNBT(CompoundTag NBT, String label) {
		CompoundTag tag = NBT.getCompound(label);
		for (String key : tag.getAllKeys()) {
			CompoundTag t = tag.getCompound(key);
			tickers.put(key, t.getInt("tick"));
			if (t.contains("cap"))
				caps.put(key, t.getInt("cap"));
		}
	}
}
