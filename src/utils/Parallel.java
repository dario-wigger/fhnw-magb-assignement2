package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Parallel for loop
 * 
 * @author Christoph Stamm
 *
 */
public class Parallel {
	public static interface IntLoopBody {
	    void run(int i);
	}
	
	public static interface LoopBody<T> {
	    void run(T i);
	}

	public static interface RedDataCreator<T> {
		T run();
	}
	
	public static interface RedLoopBody<T> {
	    void run(int i, T data);
	}
	
	public static interface Reducer<T> {
	    void run(T data);
	}
	
	private static class ReductionData<T> {
		Future<?> future;
		T data;
	}
	
	static final int nCPU = Runtime.getRuntime().availableProcessors();

	/**
	 * Parallel for each: executes the loopBody for each item in the collection
	 * @param collection
	 * @param loopBody
	 */
	public static <T> void forEach(Iterable <T> collection, final LoopBody<T> loopBody) {
	    ExecutorService executor = Executors.newFixedThreadPool(nCPU);
	    List<Future<?>> futures  = new ArrayList<Future<?>>(nCPU);

	    for (final T param : collection) {
	    	futures.add(executor.submit(() -> loopBody.run(param) ));
	    }

        for (Future<?> f : futures) {
        	try { 
        		f.get();
		    } catch (InterruptedException | ExecutionException e) { 
		    	System.out.println(e); 
		    }
        }
	    executor.shutdown();     
	}

	/**
	 * Parallel for: executes the loopBody for each int value in the semi-open range [start, stop)
	 * @param start < stop
	 * @param stop
	 * @param loopBody
	 */
	public static void For(int start, int stop, final IntLoopBody loopBody) {
		For(start, stop, 1, loopBody);
	}

	/**
	 * Parallel for: executes the loopBody for values in the semi-open range [start, stop) with step size delta
	 * @param start != stop
	 * @param stop
	 * @param delta if start < stop then delta has to be positive, if start > stop then delta must be negative
	 * @param loopBody
	 */
	public static void For(int start, int stop, int delta, final IntLoopBody loopBody) {
		assert delta != 0 : "delta must be not 0: " + delta;
		
		if (start == stop) return;
		
		ExecutorService executor = null;
		List<Future<?>> futures = null;
		
		if (start < stop) {
			assert delta > 0 : "delta must be positive: " + delta;
		    int chunkSize = (stop - start + nCPU - 1)/nCPU;
		    int rest = chunkSize%delta;
		    if (rest != 0) chunkSize += delta - rest;
	        final int nThreads = (stop - start + chunkSize - 1)/chunkSize;
	        executor = Executors.newFixedThreadPool(nThreads);
	        futures  = new ArrayList<Future<?>>(nThreads);
	
	        for (int i=start; i < stop; ) {
	            final int iStart = i;
	            i += chunkSize;
	            final int iStop = (i < stop) ? i : stop;
	            
		        futures.add(executor.submit(() -> {
	            	for (int j = iStart; j < iStop; j += delta) 
	            		loopBody.run(j);
	            }));     
		    }
		} else {
			// start > stop
			assert delta < 0 : "delta must be negative: " + delta;
		    int chunkSize = (start - stop + nCPU - 1)/nCPU;
		    int rest = chunkSize%delta;
		    if (rest != 0) chunkSize -= delta + rest;
	        final int nThreads = (start - stop + chunkSize - 1)/chunkSize;
	        executor = Executors.newFixedThreadPool(nThreads);
	        futures  = new ArrayList<Future<?>>(nThreads);
	
	        for (int i=start; i > stop; ) {
	            final int iStart = i;
	            i -= chunkSize;
	            final int iStop = (i > stop) ? i : stop;
	            
		        futures.add(executor.submit(() -> {
	            	for (int j = iStart; j > iStop; j += delta) 
	            		loopBody.run(j);
	            }));  
	        }
		}

        for (Future<?> f : futures) {
        	try { 
        		f.get();
		    } catch (InterruptedException | ExecutionException e) { 
		    	System.out.println(e); 
		    }
        }
	    executor.shutdown();     
	}

	/**
	 * Parallel for: executes the loopBody for each int value in the semi-open range [start, stop)
	 * and performs a reduction.
	 * @param start < stop
	 * @param stop
	 * @param creator initializes temporary reduction data
	 * @param loopBody
	 * @param reducer computes reduction of temporary reduction data and stores it in external result
	 */
	public static <T> void For(int start, int stop, final RedDataCreator<T> creator, final RedLoopBody<T> loopBody, final Reducer<T> reducer) {
		For(start, stop, 1, creator, loopBody, reducer);
	}
	
	/**
	 * Parallel for: executes the loopBody for each int value in the semi-open range [start, stop) with step size delta
	 * and performs a reduction.
	 * @param start != stop
	 * @param stop
	 * @param delta if start < stop then delta has to be positive, if start > stop then delta must be negative
	 * @param creator initializes temporary reduction data
	 * @param loopBody
	 * @param reducer computes reduction of temporary reduction data and stores it in external result
	 */
	public static <T> void For(int start, int stop, int delta, final RedDataCreator<T> creator, final RedLoopBody<T> loopBody, final Reducer<T> reducer) {
		assert delta != 0 : "delta must be not 0: " + delta;
		
		if (start == stop) return;
		
		ExecutorService executor = null;
		List<ReductionData<T>> redData = null;
		
		if (start < stop) {
			assert delta > 0 : "delta must be positive: " + delta;
		    int chunkSize = (stop - start + nCPU - 1)/nCPU;
		    int rest = chunkSize%delta;
		    if (rest != 0) chunkSize += delta - rest;
	        final int nThreads = (stop - start + chunkSize - 1)/chunkSize;
	        executor = Executors.newFixedThreadPool(nThreads);
	        redData  = new ArrayList<ReductionData<T>>(nThreads);
	
	        for (int i = start; i < stop; ) {
	            final int iStart = i;
	            i += chunkSize;
	            final int iStop = (i < stop) ? i : stop;
	            final ReductionData<T> rd = new ReductionData<T>();
	                        
	            rd.data = creator.run();
		        rd.future = executor.submit(() -> {
		            for (int j = iStart; j < iStop; j += delta) {
		            	loopBody.run(j, rd.data);
		            }
		        });
		        redData.add(rd);
		    }
		} else {
			// start > stop
			assert delta < 0 : "delta must be negative: " + delta;
		    int chunkSize = (start - stop + nCPU - 1)/nCPU;
		    int rest = chunkSize%delta;
		    if (rest != 0) chunkSize -= delta + rest;
	        final int nThreads = (start - stop + chunkSize - 1)/chunkSize;
	        executor = Executors.newFixedThreadPool(nThreads);
	        redData  = new ArrayList<ReductionData<T>>(nThreads);
	
	        for (int i = start; i > stop; ) {
	            final int iStart = i;
	            i -= chunkSize;
	            final int iStop = (i > stop) ? i : stop;
	            final ReductionData<T> rd = new ReductionData<T>();
	                        
	            rd.data = creator.run();
		        rd.future = executor.submit(() -> {
		            for (int j = iStart; j > iStop; j += delta) {
		            	loopBody.run(j, rd.data);
		            }
		        });
		        redData.add(rd);
		    }
		}

        for (ReductionData<T> rd : redData) {
        	try { 
        		rd.future.get();
        		if (rd.data != null) {
        			reducer.run(rd.data);
        		}
		    } catch (InterruptedException | ExecutionException e) { 
				e.printStackTrace();
		    }
        }
	    executor.shutdown();     
	}
}


