/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolExecutorWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadPoolExecutorWrapper.class);
    private final ScheduledExecutorService executor;
    private final int count;
    private final AtomicInteger runCounter;
    private final String name;

    public ThreadPoolExecutorWrapper(ScheduledExecutorService executor, int count, String name) {
        this.executor = executor;
        this.count = count;
        this.runCounter = new AtomicInteger();
        this.name = name;
    }

    public void execute(Runnable task) {
        int startCount = runCounter.incrementAndGet();
        LOG.debug("Submit task and current task count {}", startCount);
        final long startTime = System.currentTimeMillis();
        executor.execute(() -> {
            try {
                task.run();
            } finally {
                int endCount = runCounter.decrementAndGet();
                LOG.debug("Finish task and current task count {} use time {}", endCount, System.currentTimeMillis()-startTime);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
