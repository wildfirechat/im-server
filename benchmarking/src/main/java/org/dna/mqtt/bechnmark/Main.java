package org.dna.mqtt.bechnmark;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * Benchmark tool entry point.
 * 
 * You could set the number of producers and consumers
 * 
 * @author andrea
 */
public class Main {
    private static final int PUBLISHER_POOL_SIZE = 2;
    
    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(PUBLISHER_POOL_SIZE);
        
        pool.submit(new Producer("Client1"));
        
        pool.shutdown();
    }
}
