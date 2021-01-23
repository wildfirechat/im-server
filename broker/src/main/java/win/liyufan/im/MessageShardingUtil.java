/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class MessageShardingUtil {
    private static SpinLock mLock = new SpinLock();
    private static volatile int rotateId = 0;
    private static volatile long timeId = 0;
    private static int nodeId = 0;

    private static int rotateIdWidth = 15;
    private static int rotateIdMask = 0x7FFF;

    private static int nodeIdWidth = 6;
    private static int nodeIdMask = 0x3F;

    private static final long T201801010000 = 1514736000000L;

    public static void setNodeId(int nodeId) {
        MessageShardingUtil.nodeId = nodeId;
    }

    /**
     * ID = timestamp(43) + nodeId(6) + rotateId(15)
     * 所以时间限制是到2157/5/15（2的42次幂代表的时间 + (2018-1970)）。节点数限制是小于64，每台服务器每毫秒最多发送32768条消息
     * @return
     */
    public static long generateId() throws Exception {
        mLock.lock();

        rotateId = (rotateId + 1)&rotateIdMask;
        long id = System.currentTimeMillis() - T201801010000;

        if (id > timeId) {
            timeId = id;
            rotateId = 1;
        } else if(id == timeId) {
            if (rotateId == (rotateIdMask - 1)) { //当前空间已经用完，等待下一个空间打开
                while (id <= timeId) {
                    id = System.currentTimeMillis() - T201801010000;
                }
                mLock.unLock();
                return generateId();
            }
        } else { //id < timeId;
            if (rotateId > (rotateIdMask -1)*9/10) { //空间已经接近用完
                if (timeId - id < 3000) { //时间回拨小于3秒，等待时间赶上回拨之前记录
                    while (id < timeId) {
                        id = System.currentTimeMillis() - T201801010000;
                    }
                    mLock.unLock();
                    return generateId();
                } else { //时间回拨大于3秒，抛出异常，这段时间消息将不可用。
                    mLock.unLock();
                    throw new Exception("Time turn back " + (timeId - id) + " ms, it too long!!!");
                }
            } else {
                id = timeId;
            }

        }

        id <<= nodeIdWidth;
        id += (nodeId & nodeIdMask);

        id <<= rotateIdWidth;
        id += rotateId;

        mLock.unLock();


        return id;
    }

    public static String getMessageTable(long mid) {
        if (DBUtil.IsEmbedDB) {
            return "t_messages";
        }

        Calendar calendar = Calendar.getInstance();
        if (mid != Long.MAX_VALUE && mid != 0) {
            mid >>= (nodeIdWidth + rotateIdWidth);
            Date date = new Date(mid + T201801010000);
            calendar.setTime(date);
        } else {
            Date date = new Date(System.currentTimeMillis());
            calendar.setTime(date);
        }
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        year %= 3;
        return "t_messages_" + (year * 12 + month);
    }
    static Calendar getCalendarFromMessageId(long mid) {
        Calendar calendar = Calendar.getInstance();
        if (mid != Long.MAX_VALUE && mid != 0) {
            mid >>= (nodeIdWidth + rotateIdWidth);
            Date date = new Date(mid + T201801010000);
            calendar.setTime(date);
        } else {
            Date date = new Date(System.currentTimeMillis());
            calendar.setTime(date);
        }
        return calendar;
    }
    public static long getMsgIdFromTimestamp(long timestamp) {
        long id = timestamp - T201801010000;

        id <<= rotateIdWidth;
        id <<= nodeIdWidth;

        return id;
    }

    public static String getPreviousMessageTable(long mid) {
        return getMessageTable(mid, -1);
    }

    public static String getMessageTable(long mid, int offset) {
        if (DBUtil.IsEmbedDB) {
            return null;
        }

        mid >>= (nodeIdWidth + rotateIdWidth);
        Date date = new Date(mid + T201801010000);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        year %= 3;

        month = month + offset;
        while (month < 0) {
            month += 12;
            year = (year + 3 - 1)%3;
        }
        while (month >= 12) {
            month -= 12;
            year = (year + 1)%3;
        }

        return "t_messages_" + (year * 12 + month);
    }

    public static void main(String[] args) throws Exception {
        ConcurrentHashMap<Long, Integer> messageIds = new ConcurrentHashMap<>();

        int threadCount = 1000;
        int loop = 1000000;
        for (int i = 0; i < threadCount; i++) {
            new Thread(()->{
                for (int j = 0; j < loop; j++) {
                    try {
                        long mid = generateId();
                        if(messageIds.put(mid, j) != null) {
                            System.out.println("Duplicated message id !!!!!!" + mid);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        Thread.sleep(1000 * 60 * 10);
    }
}
