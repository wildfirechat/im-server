package win.liyufan.im;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 频率限制计数器，由于线程并发问题，并不一定严格准确，但总体可用
 */
public class FrequencyLimistCounter {

    private final Map<String, Integer> CACHE_MAP = new HashMap<>();
    private final Map<String, Long> CACHE_TIME_MAP = new HashMap<>();

    private long CACHE_HOLD_TIME = 5 * 1000;

    private int FREQUENCY_LIMIT = 100;

    private final SpinLock mLock = new SpinLock();


    private long lastFullScanTime = System.currentTimeMillis();
    //2个小时全部扫描一次
    private static final long fullScanDuration = 2*60*60*1000;

    public FrequencyLimistCounter(int limitTimeSec, int limitCount) {
        this.CACHE_HOLD_TIME = limitTimeSec * 1000;
        this.FREQUENCY_LIMIT = limitCount;
    }

    public FrequencyLimistCounter() {
    }

    /**
     * 增加对象计数并返回是否超频
     * @param cacheName
     * @return ture 超频; false 不超频
     */
    public boolean increaseAndCheck(String cacheName) {
        mLock.lock();
        try {
            tryFullScan();
            Long cacheHoldTime = CACHE_TIME_MAP.get(cacheName);

            if (cacheHoldTime != null && cacheHoldTime < System.currentTimeMillis()) {
                CACHE_MAP.remove(cacheName);
                CACHE_TIME_MAP.remove(cacheName);
            }

            Integer count = CACHE_MAP.get(cacheName);
            if (count != null) {
                CACHE_MAP.put(cacheName, ++count);
            } else {
                count = 0;
                CACHE_MAP.put(cacheName, ++count);
                CACHE_TIME_MAP.put(cacheName, (System.currentTimeMillis() + CACHE_HOLD_TIME));//缓存失效时间
            }
            if (count >= FREQUENCY_LIMIT) {
                return true;
            }
            return false;

        } finally {
            mLock.unLock();
        }
    }

    //去掉超时的数据，如果在一个低频使用的系统中，会残留大量超时无用的信息
    private void tryFullScan() {
        if (System.currentTimeMillis() - lastFullScanTime > fullScanDuration) {
            lastFullScanTime = System.currentTimeMillis();
            for (Map.Entry<String, Long> entry : CACHE_TIME_MAP.entrySet()
                 ) {
                if (entry.getValue() < lastFullScanTime) {
                    CACHE_MAP.remove(entry.getKey());
                    CACHE_TIME_MAP.remove(entry.getKey());
                }
            }
        }
    }
    public static void main(String[] args) {
        FrequencyLimistCounter counter = new FrequencyLimistCounter(5, 100);
        String name = "test";

        for (int i = 0; i < 50; i++) {
            if(counter.increaseAndCheck(name)) {
                System.out.println("test failure 1");
                System.exit(-1);
            }
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        for (int i = 0; i < 49; i++) {
            if(counter.increaseAndCheck(name)) {
                System.out.println("test failure 2");
                System.exit(-1);
            }
        }

        if(!counter.increaseAndCheck(name)) {
            System.out.println("test failure 3");
            System.exit(-1);
        }

        try {
            Thread.sleep(5*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 99; i++) {
            if(counter.increaseAndCheck(name)) {
                System.out.println("test failure 4");
                System.exit(-1);
            }
        }

        try {
            Thread.sleep(5*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 99; i++) {
            if(counter.increaseAndCheck(name)) {
                System.out.println("test failure 5");
                System.exit(-1);
            }
        }
    }
}
