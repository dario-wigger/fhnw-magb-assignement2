package utils;

/**
 * Complex numbers: real- and imaginary parts are of type double
 * @author Christoph Stamm
 *
 */
public class Complex {
	public double m_re, m_im;
	
	/**
	 * Creates the complex number (0, 0)
	 */
	public Complex() {
	}
	
	/**
	 * Creates the complex number (re, im)
	 * @param re real part
	 * @param im imaginary part
	 */
	public Complex(double re, double im) {
		m_re = re;
		m_im = im;
	}
	
	/**
	 * Returns the absolute value sqrt(re^2 + im^2)
	 * @return
	 */
	public double abs() {
		return Math.hypot(m_re, m_im);
	}
	
	/**
	 * Returns the square of the absolute value: re^2 + im^2
	 * @return
	 */
	public double abs2() {
		return m_re*m_re + m_im*m_im;
	}
	
	/**
	 * Returns the angle in radiant
	 * @return
	 */
	public double arg() {
		return Math.atan2(m_im, m_re);
	}
	
	/**
	 * Complex addition
	 * @param c
	 * @return this + c
	 */
	public Complex add(Complex c) {
		return new Complex(m_re + c.m_re, m_im + c.m_im);
	}
	
	/**
	 * Complex subtraction
	 * @param c
	 * @return this - c
	 */
	public Complex sub(Complex c) {
		return new Complex(m_re - c.m_re, m_im - c.m_im);		
	}
	
	/**
	 * Complex scaling
	 * @param s
	 * @return s*this
	 */
	public Complex mul(double s) {
		return new Complex(s*m_re, s*m_im);		
	}
	
	/**
	 * Complex multiplication
	 * @param c
	 * @return this*c
	 */
	public Complex mul(Complex c) {
		return new Complex(m_re*c.m_re - m_im*c.m_im, m_re*c.m_im + m_im*c.m_re);
	}
	
	/**
	 * Complex division
	 * @param c
	 * @return this/c
	 */
	public Complex div(Complex c) {
		final double den = c.abs2();
		return new Complex((m_re*c.m_re + m_im*c.m_im)/den, (m_im*c.m_re - m_re*c.m_im)/den);
	}
	
	/**
	 * Complex conjugation
	 * @return complex conjugated
	 */
	public Complex conjugate() {  
		return new Complex(m_re, -m_im); 
	}
	
	/**
	 * Complex addition
	 * @param c
	 */
	public void plus(Complex c) {
		m_re += c.m_re;
		m_im += c.m_im;
	}
	
	/**
	 * Complex subtraction
	 * @param c
	 */
	public void minus(Complex c) {
		m_re -= c.m_re;
		m_im -= c.m_im;		
	}
	
	/**
	 * Complex multiplication: this := this*c
	 * @param c
	 */
	public void multiply(Complex c) {
		double re = m_re*c.m_re - m_im*c.m_im;
		double im = m_re*c.m_im + m_im*c.m_re;
		m_re = re;
		m_im = im;
	}
	
	/**
	 * Complex scaling: this := s*this
	 * @param s
	 */
	public void multiply(double s) {
		m_re *= s;
		m_im *= s;
	}
	
	/**
	 * Complex division: this := this/c
	 * @param c
	 */
	public void divide(Complex c) {
		final double den = c.abs2();
		final double re = m_re*c.m_re + m_im*c.m_im;
		final double im = m_im*c.m_re - m_re*c.m_im;
		m_re = re/den;
		m_im = im/den;
	}
	
	/**
	 * Complex reciprocal scaling: this := this/s
	 * @param s
	 */
	public void divide(double s) {
		m_re /= s;
		m_im /= s;
	}
	
	/**
	 * Complex conjugate
	 */
	public void conj() {  
		m_im = -m_im; 
	}

	public boolean isZero() {
		return m_re == 0 && m_im == 0;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Complex) {
			Complex c = (Complex)o;
			return m_re == c.m_re && m_im == c.m_im;
		} else {
			return false;
		}
	}
	
	@Override 
	public String toString() {
		return "(" + m_re + "," + m_im + ")";
	}
}
