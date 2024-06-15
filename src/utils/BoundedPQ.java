package utils;

import java.util.*;

/**
 * Space bounded priority queue. The elements are ordered by the natural order of the elements.
 * Stores a bounded number of elements in order of their priority. 
 * Identical priorities are supported.
 * Element with lowest priority will be replaced first.
 * 
 * @author Christoph Stamm
 *
 * @param <E> data type of elements
 */
public class BoundedPQ<E extends Comparable<? super E>> implements Iterable<E> {
	private int m_capacity;			// maximum number of elements in the priority queue
	private PriorityQueue<E> m_pq;	// priority queue (min heap)
	
	/**
	 * Creates a bounded priority queue for a maximum of capacity elements
	 * @param capacity
	 */
	public BoundedPQ(int capacity) {
		m_capacity = capacity;
		m_pq = new PriorityQueue<E>(m_capacity + 1);
	}
	
	/**
	 * Adds a new element e to the priority queue
	 * @param e
	 */
	public void add(E e) {
		m_pq.add(e);
		if (m_pq.size() > m_capacity) {
			m_pq.poll();
		}
	}
	
	/**
	 * Returns an element with highest priority
	 * @return
	 */
	public E getMax() {
		E max = m_pq.peek();
		for(E e : m_pq) {
			if (e.compareTo(max) > 0) max = e;
		}
		return max;
	}
	
	/**
	 * Returns an element with lowest priority
	 * @return
	 */
	public E getMin() {
		return m_pq.peek();
	}

	/**
	 * Removes and returns an element with highest priority
	 * @return
	 */
	public E removeMax() {
		E max = getMax();
		m_pq.remove(max);
		return max;
	}
	
	/**
	 * Removes and returns an element with lowest priority
	 * @return
	 */
	public E removeMin() {
		return m_pq.poll();
	}
	
	/**
	 * Returns the number of elements in the priority queue
	 * @return
	 */
	public int size() {
		return m_pq.size();
	}

	/**
	 * Returns the maximum number of elements that can be stored in the bounded priority queue
	 * @return
	 */
	public int capacity() {
		return m_capacity;
	}

	@Override
	public Iterator<E> iterator() {
		return m_pq.iterator();
	}
}
