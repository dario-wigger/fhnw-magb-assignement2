package imageprocessing.fourier;

import utils.Complex;

/**
 * Fast Fourier Transforms in 1D
 * @author Christoph Stamm
 *
 */
public class FFT1D {
	/**
	 * Computes the FFT of x[], assuming its length is a power of 2
	 * Difference to DFT1D: for m > N/2 subtraction is used while DFT always adds terms
	 * @param x input of size 2^k
	 * @return complex Fourier spectrum 
	 */
    public static Complex[] fft(Complex[] x) {
        int N = x.length;

        // base case
        if (N == 1) {
        	return new Complex[] { x[0] };
        } else {
	        // radix 2 Cooley-Tukey FFT
	        assert (N & 1) == 0 : "N is not a power of 2";
	
	        // fft of even terms
	        Complex[] even = new Complex[N/2];
	        for (int k = 0; k < N/2; k++) {
	            even[k] = x[2*k];
	        }
	        Complex[] q = fft(even);
	
	        // fft of odd terms
	        Complex[] odd  = even;  // reuse the array
	        for (int k = 0; k < N/2; k++) {
	            odd[k] = x[2*k + 1];
	        }
	        Complex[] r = fft(odd);
	
	        // combine
	        Complex[] y = new Complex[N];
	        for (int k = 0; k < N/2; k++) {
	            final double kth = -2*k*Math.PI/N;
	            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
	            y[k]       = q[k].add(wk.mul(r[k]));
	            y[k + N/2] = q[k].sub(wk.mul(r[k]));
	        }
	        return y;
        }
    }

    /**
     * Computes the inverse FFT of x[], assuming its length is a power of 2
     * @param x complex Fourier spectrum
     * @return (complex) output data
     */
    public static Complex[] ifft(Complex[] x) {
        int N = x.length;
        Complex[] y = new Complex[N];

        // take conjugate
        for (int i = 0; i < N; i++) {
            y[i] = x[i].conjugate();
        }

        // compute forward FFT
        y = fft(y);

        // take conjugate again
        // divide by N
        for (int i = 0; i < N; i++) {
            y[i].conj();
            y[i].divide(N);
        }

        return y;
    }
}
