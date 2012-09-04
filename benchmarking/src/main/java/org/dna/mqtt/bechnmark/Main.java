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
    private static final int NUM_CONCURRENT_PRODS = 2;
    
    public static void main(String[] args) throws InterruptedException {
        ExecutorService consumerPool = Executors.newFixedThreadPool(PUBLISHER_POOL_SIZE);
        ExecutorService producerPool = Executors.newFixedThreadPool(PUBLISHER_POOL_SIZE);
        
        consumerPool.submit(new ConsumerFuture("Cons1"));
        
        //force wait to let the consumer be registered before the producer
        Thread.sleep(1000);
        int len = Producer.PUB_LOOP / NUM_CONCURRENT_PRODS;
        for (int i = 0; i < NUM_CONCURRENT_PRODS; i++) {
            producerPool.submit(new Producer("Prod " + i, i * len, len));
        }

        consumerPool.shutdown();
        producerPool.shutdown();
    }
}
