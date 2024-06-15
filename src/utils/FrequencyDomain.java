package utils;

import java.util.Arrays;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

/**
 * Frequency domain object used to store the result in Fourier Transforms
 * 
 * @author Christoph Stamm
 *
 */
public class FrequencyDomain implements Cloneable {
	public int m_width, m_height;	// image size
	public int m_depth;				// image bit depth
	public double m_powerScale;		// scale factor used in power spectrum, 0 = undefined scale
	public double m_min;			// log of min transformed value
	public PaletteData m_palette;	// image palette
	public Complex[][] m_g;			// transformed image
	
	/**
	 * @param inData input image
	 * @param g Fourier coefficients
	 */
	public FrequencyDomain(ImageData inData, Complex[][] g) {
		m_width = inData.width;
		m_height = inData.height;
		m_depth = inData.depth;
		m_palette = inData.palette;
		m_g = g;
	}
	
	/**
	 * @param inData input image
	 * @param width output width
	 * @param height output height
	 * @param g Fourier coefficients
	 */
	public FrequencyDomain(ImageData inData, int width, int height, Complex[][] g) {
		m_width = width;
		m_height = height;
		m_depth = inData.depth;
		m_palette = inData.palette;
		m_g = g;
	}

	/**
	 * Copy constructor
	 * @param fd
	 */
	public FrequencyDomain(FrequencyDomain fd) {
		m_width = fd.m_width;
		m_height = fd.m_height;
		m_depth = fd.m_depth;
		m_palette = fd.m_palette;
		m_powerScale = fd.m_powerScale;
		m_min = fd.m_min;
		m_g = new Complex[fd.m_g.length][];
		
		Parallel.For(0, m_g.length, v -> {
			m_g[v] = Arrays.copyOf(fd.m_g[v], fd.m_g[v].length);
		});
	}
	
	private FrequencyDomain() {	
	}
	
	private FrequencyDomain(FrequencyDomain fd, boolean dummy) {
		m_width = fd.m_width;
		m_height = fd.m_height;
		m_depth = fd.m_depth;
		m_palette = fd.m_palette;
		m_powerScale = fd.m_powerScale;
		m_min = fd.m_min;
		m_g = new Complex[fd.m_g.length][];	
		
		Parallel.For(0, m_g.length, v -> {
			m_g[v] = new Complex[fd.m_g[v].length];
		});
	}
	
	/**
	 * Returns amplitude at given position
	 * @param u x-coordinate
	 * @param v y-coordinate
	 * @return amplitude
	 */
	public double getAmplitude(int u, int v) {
		return m_g[v][u].abs();
	}
	
	/**
	 * Returns phase at given position
	 * @param u x-coordinate
	 * @param v y-coordinate
	 * @return phase
	 */
	public double getPhase(int u, int v) {
		return m_g[v][u].arg();
	}
	
	public int getSpectrumWidth() { return m_g[0].length; }
	public int getSpectrumHeight() { return m_g.length; }
	
	/**
	 * Sets amplitude and phase at given position
	 * @param u x-coordinate
	 * @param v y-coordinate
	 * @param amp amplitude
	 * @param phi phase
	 */
	public void setValue(int u, int v, double amp, double phi) { 
		m_g[v][u] = new Complex(amp*Math.cos(phi), amp*Math.sin(phi));
	}
	
	/**
	 * Return power spectral density
	 * @return
	 */
	public double meanPower() {
		double[] sum = new double[1];
		
		Parallel.For(0, m_g.length, 
			// creator
			() -> new double[1],
			// loop body
			(v, s) -> {
				for (int u = 0; u < m_g[v].length; u++) {
					s[0] += m_g[v][u].abs2();
				}
			},
			// reducer
			s -> {
				sum[0] += s[0];
			}
		);
		return sum[0]/getSpectrumWidth()/getSpectrumHeight();
	}

	@Override
	public FrequencyDomain clone() {
		return new FrequencyDomain(this);
	}

	/**	
	 * Swap quadrants B and D and A and C  
	 * so the power spectrum origin is at the center.
	<pre>
	    B A
	    C D
	</pre>
	 * B.w = ceil(w/2) = w1
	 * B.h = ceil(h/2) = h1
	 * D.w = floor(w/2) = w2
	 * D.h = floor(h/2) = h2
	 * @return specturm with swapped quadrants
	 */
	public FrequencyDomain swapQuadrants() {
		final int w = getSpectrumWidth();
		final int h = getSpectrumHeight();
		FrequencyDomain fd = new FrequencyDomain();
		fd.m_width = m_width;
		fd.m_height = m_height;
		fd.m_depth = m_depth;
		fd.m_palette = m_palette;
		fd.m_powerScale = m_powerScale;
		fd.m_min = m_min;
		fd.m_g = new Complex[h][w];
		
		final int w2 = w/2,  w1 = w - w2;
		final int h2 = h/2, h1 = h - h2;

		Parallel.For(0, h2, v -> {
			final int h1v = h1 + v;
			final int h2v = h2 + v;
			
			for(int u = 0; u < w2; u++) {
				fd.m_g[v][u] = m_g[h1v][w1 + u];
				fd.m_g[v][w2 + u] = m_g[h1v][u];
				fd.m_g[h2v][u] = m_g[v][w1 + u];
				fd.m_g[h2v][w2 + u] = m_g[v][u];
			}
		});
		if (h1 != h2) {
			final int h22 = h2 << 1;
			for(int u = 0; u < w2; u++) {
				fd.m_g[h22][u] = m_g[h2][w1 + u];
				fd.m_g[h22][w2 + u] = m_g[h2][u];
			}
		}
		if (w1 != w2) {
			final int w22 = w2 << 1;
			for(int v = 0; v < h2; v++) {
				fd.m_g[v][w22] = m_g[h1 + v][w2];
				fd.m_g[h2 + v][w22] = m_g[v][w2];
			}
			if (h1 != h2) {
				final int h22 = h2 << 1;
				fd.m_g[h22][w22] = m_g[h2][w2];
			}
		}
		
		return fd;
	}
	
	public void multiply(int u, int v, double d) {
		m_g[v][u].multiply(d);
	}
	
	public void multiply(double d) {
		Parallel.For(0, m_g.length, v -> {
			for(int u = 0; u < m_g[v].length; u++) {
				m_g[v][u].multiply(d);
			}
		});
	}
	
	public void multiply(FrequencyDomain fd) {
		assert m_g.length == fd.m_g.length;
		
		Parallel.For(0, m_g.length, v -> {
			assert m_g[v].length == fd.m_g[v].length;
			for(int u = 0; u < m_g[v].length; u++) {
				m_g[v][u].multiply(fd.m_g[v][u]);
			}
		});
	}

	public FrequencyDomain mul(double d) {
		FrequencyDomain fd = new FrequencyDomain(this, true);
		
		Parallel.For(0, m_g.length, v -> {
			for(int u = 0; u < m_g[v].length; u++) {
				fd.m_g[v][u] = m_g[v][u].mul(d);
			}
		});
		return fd;
	}
	
	public FrequencyDomain mul(FrequencyDomain fd2) {
		assert m_g.length == fd2.m_g.length;
		FrequencyDomain fd = new FrequencyDomain(this, true);
		
		Parallel.For(0, m_g.length, v -> {
			assert m_g[v].length == fd2.m_g[v].length;
			for(int u = 0; u < m_g[v].length; u++) {
				fd.m_g[v][u] = m_g[v][u].mul(fd2.m_g[v][u]);
			}
		});
		return fd;
	}

	public void divide(FrequencyDomain fd) {
		assert m_g.length == fd.m_g.length;

		Parallel.For(0, m_g.length, v -> {
			assert m_g[v].length == fd.m_g[v].length;
			for(int u = 0; u < m_g[v].length; u++) {
				if (fd.m_g[v][u].abs2() == 0) {
					// division by zero
					m_g[v][u].m_re = 0;
					m_g[v][u].m_im = 0;
				} else {
					m_g[v][u].divide(fd.m_g[v][u]);
				}
			}
		});
	}
		
	public FrequencyDomain div(FrequencyDomain fd2) {
		assert m_g.length == fd2.m_g.length;
		FrequencyDomain fd = new FrequencyDomain(this, true);
		
		Parallel.For(0, m_g.length, v -> {
			assert m_g[v].length == fd2.m_g[v].length;
			for(int u = 0; u < m_g[v].length; u++) {
				if (fd2.m_g[v][u].abs2() == 0) {
					// division by zero
					m_g[v][u].m_re = 0;
					m_g[v][u].m_im = 0;
				} else {
					fd.m_g[v][u] = m_g[v][u].div(fd2.m_g[v][u]);
				}
			}
		});
		return fd;
	}
		
	@Override
	public boolean equals(Object o) {
		if (o instanceof FrequencyDomain) {
			FrequencyDomain fd = (FrequencyDomain)o;
			if (m_width != fd.m_width) return false;
			if (m_height != fd.m_height) return false;
			if (m_powerScale != fd.m_powerScale) return false;
			if (m_min != fd.m_min) return false;
			if (m_palette != fd.m_palette) return false;
			for(int i = 0; i < m_height; i++) {
				for(int j = 0; j < m_width; j++) {
					if (!m_g[i][j].equals(fd.m_g[i][j])) 
						return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
}
