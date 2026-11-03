/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.Random;

import reika.rotarycraft.RotaryConfig;

/** Original: Reika.RotaryCraft.Registry.DifficultyEffects. Values unchanged. */
public enum DifficultyEffects {

	BEDROCKDUST(3, 2, 1),
	PIPECRAFT(32, 16, 8),
	PARTCRAFT(6, 3, 2),
	BELTCRAFT(16, 8, 2),
	COMPACTOR(2, 2, 1),
	SMALLERCRAFT(2, 2, 1),
	BONUSSTEEL(2D, 1D, 0.5D),
	BLASTCONSUME(0.4F, 1F, 3F),
	JETFAILURE(4500, 1800, 900),
	CONSUMEFRAC(0.03125F, 0.25F, 0.75F),
	PRODUCEFRAC(new int[]{1600,3200}, new int[]{1000,2200}, new int[]{400,800}),
	BREAKCOIL(0.01D, 0.05D, 0.15D),
	FURNACEMELT(1800, 600, 150),
	CANOLA(new int[]{128,280}, new int[]{64,160}, new int[]{8,64}),
	RAILGUNCRAFT(16, 8, 2),
	LUBEUSAGE(0.25F, 1F, 2.5F),
	JETINGESTFAIL(0.05F, 0.2F, 0.5F),
	FRACTIONTEAR(0F, 0F, 0.05F),
	;

	public enum Difficulty {
		EASY, MEDIUM, HARD;
	}

	private static final Random rand = new Random();

	private final boolean isChance;
	private final boolean isRandom;
	private final int[] ints = new int[3];
	private final int[] minimum = new int[3];
	private final int[] maximum = new int[3];
	private final float[] chance = new float[3];
	private final double[] doubles = new double[3];

	DifficultyEffects(int easy, int med, int hard) {
		isChance = false;
		isRandom = false;
		ints[0] = easy;
		ints[1] = med;
		ints[2] = hard;
	}

	DifficultyEffects(int[] easy, int[] med, int[] hard) {
		isChance = false;
		isRandom = true;
		minimum[0] = easy[0];
		minimum[1] = med[0];
		minimum[2] = hard[0];
		maximum[0] = easy[1];
		maximum[1] = med[1];
		maximum[2] = hard[1];
	}

	/** Chances (out of 1F) */
	DifficultyEffects(float easy, float med, float hard) {
		isChance = true;
		isRandom = false;
		chance[0] = easy;
		chance[1] = med;
		chance[2] = hard;
	}

	DifficultyEffects(double easy, double med, double hard) {
		isChance = false;
		isRandom = false;
		doubles[0] = easy;
		doubles[1] = med;
		doubles[2] = hard;
	}

	public int getInt() {
		int i = getDifficulty().ordinal();
		if (isRandom)
			return minimum[i]+rand.nextInt(1+maximum[i]-minimum[i]);
		return ints[i];
	}

	public int getMaxAmount() {
		int i = getDifficulty().ordinal();
		return isRandom ? maximum[i] : ints[i];
	}

	public int getAverageAmount() {
		int i = getDifficulty().ordinal();
		return isRandom ? (maximum[i]+minimum[i])/2 : ints[i];
	}

	public float getChance() {
		return chance[getDifficulty().ordinal()];
	}

	public double getDouble() {
		return doubles[getDifficulty().ordinal()];
	}

	public boolean testChance() {
		if (!isChance)
			throw new IllegalStateException(this+" is not chance, but was called for it!");
		return rand.nextFloat() < this.getChance();
	}

	public static Difficulty getDifficulty() {
		int val = RotaryConfig.SPEC.isLoaded() ? RotaryConfig.DIFFICULTY.get() : 2;
		return Difficulty.values()[Math.max(1, Math.min(3, val))-1];
	}
}
