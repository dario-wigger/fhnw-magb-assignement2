package imageprocessing.fourier;

/**
 * This class contains a Java implementation of the Fast Hartley Transform. It
 * is based on Pascal code in NIH Image contributed by Arlo Reeves
 * (http://imagej.nih.gov/ij/docs/ImageFFT/). The Fast Hartley Transform was
 * restricted by U.S. Patent No. 4,646,256, but was placed in the public domain
 * by Stanford University in 1995 and is now freely available.
 */
public class FHT1D {
	private float[] m_tempArr;
	private int[] m_bitrev;
	private float[] m_C;
	private float[] m_S;
	
	void initializeTables(int maxN) {
		if (m_S == null) {
			makeSinCosTables(maxN);
			makeBitReverseTable(maxN);
			m_tempArr = new float[maxN];
		}
	}

	void makeSinCosTables(int maxN) {
		int n = maxN / 4;
		m_C = new float[n];
		m_S = new float[n];
		double theta = 0.0;
		double dTheta = 2.0 * Math.PI / maxN;
		for (int i = 0; i < n; i++) {
			m_C[i] = (float) Math.cos(theta);
			m_S[i] = (float) Math.sin(theta);
			theta += dTheta;
		}
	}

	void makeBitReverseTable(int maxN) {
		m_bitrev = new int[maxN];
		int nLog2 = log2(maxN);
		for (int i = 0; i < maxN; i++)
			m_bitrev[i] = bitRevX(i, nLog2);
	}

	/** Performs an optimized 1D FHT. */
	void dfht3(float[] x, int base, boolean inverse, int maxN) {
		int i, stage, gpNum, gpSize, numGps, Nlog2;
		int bfNum, numBfs;
		int Ad0, Ad1, Ad2, Ad3, Ad4, CSAd;
		float rt1, rt2, rt3, rt4;

		if (m_S == null)
			initializeTables(maxN);
		Nlog2 = log2(maxN);
		bitRevRArr(x, base, Nlog2, maxN); // bitReverse the input array
		gpSize = 2; // first & second stages - do radix 4 butterflies once thru
		numGps = maxN / 4;
		for (gpNum = 0; gpNum < numGps; gpNum++) {
			Ad1 = gpNum * 4;
			Ad2 = Ad1 + 1;
			Ad3 = Ad1 + gpSize;
			Ad4 = Ad2 + gpSize;
			rt1 = x[base + Ad1] + x[base + Ad2]; // a + b
			rt2 = x[base + Ad1] - x[base + Ad2]; // a - b
			rt3 = x[base + Ad3] + x[base + Ad4]; // c + d
			rt4 = x[base + Ad3] - x[base + Ad4]; // c - d
			x[base + Ad1] = rt1 + rt3; // a + b + (c + d)
			x[base + Ad2] = rt2 + rt4; // a - b + (c - d)
			x[base + Ad3] = rt1 - rt3; // a + b - (c + d)
			x[base + Ad4] = rt2 - rt4; // a - b - (c - d)
		}

		if (Nlog2 > 2) {
			// third + stages computed here
			gpSize = 4;
			numBfs = 2;
			numGps = numGps / 2;
			// IJ.write("FFT: dfht3 "+Nlog2+" "+numGps+" "+numBfs);
			for (stage = 2; stage < Nlog2; stage++) {
				for (gpNum = 0; gpNum < numGps; gpNum++) {
					Ad0 = gpNum * gpSize * 2;
					Ad1 = Ad0; // 1st butterfly is different from others - no mults needed
					Ad2 = Ad1 + gpSize;
					Ad3 = Ad1 + gpSize / 2;
					Ad4 = Ad3 + gpSize;
					rt1 = x[base + Ad1];
					x[base + Ad1] = x[base + Ad1] + x[base + Ad2];
					x[base + Ad2] = rt1 - x[base + Ad2];
					rt1 = x[base + Ad3];
					x[base + Ad3] = x[base + Ad3] + x[base + Ad4];
					x[base + Ad4] = rt1 - x[base + Ad4];
					for (bfNum = 1; bfNum < numBfs; bfNum++) {
						// subsequent BF's dealt with together
						Ad1 = bfNum + Ad0;
						Ad2 = Ad1 + gpSize;
						Ad3 = gpSize - bfNum + Ad0;
						Ad4 = Ad3 + gpSize;

						CSAd = bfNum * numGps;
						rt1 = x[base + Ad2] * m_C[CSAd] + x[base + Ad4] * m_S[CSAd];
						rt2 = x[base + Ad4] * m_C[CSAd] - x[base + Ad2] * m_S[CSAd];

						x[base + Ad2] = x[base + Ad1] - rt1;
						x[base + Ad1] = x[base + Ad1] + rt1;
						x[base + Ad4] = x[base + Ad3] + rt2;
						x[base + Ad3] = x[base + Ad3] - rt2;

					} /* end bfNum loop */
				} /* end gpNum loop */
				gpSize *= 2;
				numBfs *= 2;
				numGps = numGps / 2;
			} /* end for all stages */
		} /* end if Nlog2 > 2 */

		if (inverse) {
			for (i = 0; i < maxN; i++)
				x[base + i] = x[base + i] / maxN;
		}
	}

	void transposeR(float[] x, int maxN) {
		int r, c;
		float rTemp;

		for (r = 0; r < maxN; r++) {
			for (c = r; c < maxN; c++) {
				if (r != c) {
					rTemp = x[r * maxN + c];
					x[r * maxN + c] = x[c * maxN + r];
					x[c * maxN + r] = rTemp;
				}
			}
		}
	}

	int log2(int x) {
		int count = 15;
		if (x > 32768)
			count = 31;
		while (!btst(x, count))
			count--;
		return count;
	}

	private boolean btst(int x, int bit) {
		// int mask = 1;
		return ((x & (1 << bit)) != 0);
	}

	void bitRevRArr(float[] x, int base, int bitlen, int maxN) {
		for (int i = 0; i < maxN; i++)
			m_tempArr[i] = x[base + m_bitrev[i]];
		for (int i = 0; i < maxN; i++)
			x[base + i] = m_tempArr[i];
	}

	private int bitRevX(int x, int bitlen) {
		int temp = 0;
		for (int i = 0; i <= bitlen; i++)
			if ((x & (1 << i)) != 0)
				temp |= (1 << (bitlen - i - 1));
		return temp & 0x0000ffff;
	}
/*
	private int bset(int x, int bit) {
		x |= (1 << bit);
		return x;
	}
*/
}
