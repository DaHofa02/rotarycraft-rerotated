/*******************************************************************************
 * Original: Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary
 * @author Reika Kalseki. Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.lib.math;

/** The subset of DragonAPI's math library used by the ported mods. Bodies are unchanged from the original. */
public final class ReikaMathLibrary {

	public static final double e = Math.E;
	public static final double pi = Math.PI;

	private ReikaMathLibrary() {}

	/** Returns the pythagorean sum of the three inputs. */
	public static double py3d(double dx, double dy, double dz) {
		return Math.sqrt(dx*dx+dy*dy+dz*dz);
	}

	/** Returns true if the input is within [target-range,target+range]. */
	public static boolean approxr(double input, double target, double range) {
		double low = input - range;
		double hi = input + range;
		return target >= low && target <= hi;
	}

	/** Returns the value of a double raised to an integer power. */
	public static double intpow(double base, int pow) {
		double val = 1.0D;
		if (pow == 0)
			return val;
		if (pow > 0) {
			for (int i = 0; i < pow; i++) {
				val *= base;
			}
		}
		else {
			pow = -pow;
			for (int i = 0; i < pow; i++) {
				val /= base;
			}
		}
		return val;
	}

	/** Returns the value of an integer raised to a POSITIVE integer power. */
	public static int intpow2(int base, int pow) {
		int val = 1;
		for (int i = 0; i < pow; i++) {
			val *= base;
		}
		return val;
	}

	public static long longpow(int base, int pow) {
		long val = 1;
		for (int i = 0; i < pow; i++) {
			val *= base;
		}
		return val;
	}

	public static double doubpow(double base, double pow) {
		return Math.pow(base, pow);
	}

	public static int logbase2(long inp) {
		return inp > 0 ? 63-Long.numberOfLeadingZeros(inp) : 0;
	}

	public static double logbase(long inp, int base) {
		return logbase((double)inp, base);
	}

	/** Returns the logarithm of a specified base. */
	public static double logbase(double inp, double base) {
		if (base == 2 && isInteger(inp) && isPowerOfTwo((long)inp))
			return logbase2((long)inp);
		return Math.log(inp)/(Math.log(base));
	}

	public static boolean isInteger(double num) {
		return Math.floor(num) == num;
	}

	public static boolean isPowerOfTwo(long num) {
		return num > 0 && (num & (num-1)) == 0;
	}

	public static boolean isValueInsideBoundsIncl(int low, int hi, int val) {
		return val >= low && val <= hi;
	}

	public static double getThousandBase(double val) {
		if (Math.abs(val) == Double.POSITIVE_INFINITY)
			return val;
		if (Double.isNaN(val))
			return val;
		boolean neg = val < 0;
		val = Math.abs(val);
		while (val >= 1000) {
			val /= 1000D;
		}
		while (val < 1 && val > 0) {
			val *= 1000D;
		}
		return neg ? -val : val;
	}

	public static double linterpolate(double x, double x1, double x2, double y1, double y2) {
		return y1+(x-x1)/(x2-x1)*(y2-y1);
	}
}
