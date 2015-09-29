package org.eclipse.moquette.server;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * This tests that the broker shuts down properly and all the threads it creates are closed.
 *
 * @author kazvictor
 */
public class ServerShutdownTest {
    private static final Logger LOG = LoggerFactory.getLogger(ServerShutdownTest.class);

    private final ScheduledExecutorService threadCounter = Executors.newScheduledThreadPool(1);

    @Test
    public void testShutdown() throws IOException, InterruptedException {

        final int initialThreadCount = Thread.activeCount();
        LOG.info("*** testShutdown ***");
        LOG.debug("Initial Thread Count = " + initialThreadCount);

        //Start the server
        Server broker = new Server();
        broker.startServer();
        //stop the server
        broker.stopServer();

        //wait till the thread count is back to the initial thread count
        final CountDownLatch threadsStoppedLatch = new CountDownLatch(1);
        threadCounter.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                int threadCount = Thread.activeCount();
                LOG.debug("Current Thread Count = " + threadCount);
                //plus 1 for this thread
                if (threadCount == initialThreadCount + 1) {
                    threadsStoppedLatch.countDown();
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        //wait till the countdown latch is triggered. Either the threads stop or the timeout is readed.
        boolean threadsStopped = threadsStoppedLatch.await(5, TimeUnit.SECONDS);
        //shutdown the threadCounter.
        threadCounter.shutdown();

        assertTrue("Broker did not shutdown properly. Not all broker threads were stopped.", threadsStopped);
    }
}
