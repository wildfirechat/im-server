package cn.wildfirechat.common;

import java.util.Date;

public class ModifiedSnowflakeIdGenerator {

    public static final int TOTAL_BITS = 1 << 6;

    private static final long SIGN_BITS = 1;

    private static final long TIME_STAMP_BITS = 41L;

    private static final long DATA_CENTER_ID_BITS = 5L;

    private static final long WORKER_ID_BITS = 5L;

    private static final long SEQUENCE_BITS = 12L;

    private static final int MAX_BACKWORDS_MILLI_SECONDS =  3000;

    /**
     * 时间向左位移位数 22位
     */
    private static final long TIMESTAMP_LEFT_SHIFT = WORKER_ID_BITS + DATA_CENTER_ID_BITS + SEQUENCE_BITS;

    /**
     * IDC向左位移位数 17位
     */
    private static final long DATA_CENTER_ID_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;

    /**
     * 机器ID 向左位移位数 12位
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 序列掩码，用于限定序列最大值为4095
     */
    private static final long SEQUENCE_MASK =  -1L ^ (-1L << SEQUENCE_BITS);

    /**
     * 最大支持机器节点数0~31，一共32个
     */
    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);
    /**
     * 最大支持数据中心节点数0~31，一共32个
     */
    private static final long MAX_DATA_CENTER_ID = -1L ^ (-1L << DATA_CENTER_ID_BITS);

    /**
     * 最大时间戳 2199023255551
     */
    private static final long MAX_DELTA_TIMESTAMP = -1L ^ (-1L << TIME_STAMP_BITS);

    /**
     * Customer epoch
     */
    private final long twepoch;

    private final long workerId;

    private final long dataCenterId;

    private long sequence = 0L;

    private long lastTimestamp = -1L;

    /**
     *
     * @param workerId 机器ID
     * @param dataCenterId  IDC ID
     */
    public ModifiedSnowflakeIdGenerator(long workerId, long dataCenterId) {
        this(workerId, dataCenterId, null);
    }

    /**
     *
     * @param workerId  机器ID
     * @param dataCenterId IDC ID
     * @param epochDate 初始化时间起点
     */
    public ModifiedSnowflakeIdGenerator(long workerId, long dataCenterId, Date epochDate) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("worker Id can't be greater than "+ MAX_WORKER_ID + " or less than 0");
        }
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("datacenter Id can't be greater than {" + MAX_DATA_CENTER_ID + "} or less than 0");
        }

        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
        if (epochDate != null) {
            this.twepoch = epochDate.getTime();
        } else {
            //2010-10-11
            this.twepoch = 1286726400000L;
        }

    }

    public long genID() throws Exception {
        try {
            return nextId();
        } catch (Exception e) {
            throw e;
        }
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    /**
     * 通过移位解析出sequence，sequence有效位为[0,12]
     * 所以先向左移64-12，然后再像右移64-12，通过两次移位就可以把无效位移除了
     * @param id
     * @return
     */
    public long getSequence2(long id) {
        return (id << (TOTAL_BITS - SEQUENCE_BITS)) >>> (TOTAL_BITS - SEQUENCE_BITS);
    }

    /**
     * 通过移位解析出workerId，workerId有效位为[13,17], 左右两边都有无效位
     * 先向左移 41+5+1，移除掉41bit-时间，5bit-IDC、1bit-sign，
     * 然后右移回去41+5+1+12，从而移除掉12bit-序列号
     * @param id
     * @return
     */
    public long getWorkerId2(long id) {
        return (id << (TIME_STAMP_BITS + DATA_CENTER_ID_BITS + SIGN_BITS)) >>> (TIME_STAMP_BITS + DATA_CENTER_ID_BITS + SEQUENCE_BITS + SIGN_BITS);
    }
    /**
     * 通过移位解析出IDC_ID，dataCenterId有效位为[18,23]，左边两边都有无效位
     * 先左移41+1，移除掉41bit-时间和1bit-sign
     * 然后右移回去41+1+5+12，移除掉右边的5bit-workerId和12bit-序列号
     * @param id
     * @return
     */
    public long getDataCenterId2(long id) {
        return (id << (TIME_STAMP_BITS + SIGN_BITS)) >>> (TIME_STAMP_BITS + WORKER_ID_BITS + SEQUENCE_BITS + SIGN_BITS);
    }
    /**
     * 41bit-时间，左边1bit-sign为0，可以忽略，不用左移，所以只需要右移，并加上起始时间twepoch即可。
     * @param id
     * @return
     */
    public long getGenerateDateTime2(long id) {
        return (id >>> (DATA_CENTER_ID_BITS + WORKER_ID_BITS + SEQUENCE_BITS)) + twepoch;
    }

    public long getSequence(long id) {
        return id & ~(-1L << SEQUENCE_BITS);
    }

    public long getWorkerId(long id) {
        return id >> WORKER_ID_SHIFT & ~(-1L << WORKER_ID_BITS);
    }

    public long getDataCenterId(long id) {
        return id >> DATA_CENTER_ID_SHIFT & ~(-1L << DATA_CENTER_ID_BITS);
    }

    public long getGenerateDateTime(long id) {
        return (id >> TIMESTAMP_LEFT_SHIFT & ~(-1L << 41L)) + twepoch;
    }

    /**
     * 优化时钟回调
     * @return
     * @throws Exception
     */
    private synchronized long nextId() throws Exception {
        long timestamp = timeGen();
        // 1、出现时钟回拨问题，直接抛异常
//        if (timestamp < lastTimestamp) {
//            long refusedTimes = lastTimestamp - timestamp;
//            // 可自定义异常类
//            throw new UnsupportedOperationException(String.format("Clock moved backwards. Refusing for %d seconds", refusedTimes));
//        }
        // 2、时间等于lastTimestamp，取当前的sequence + 1
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // Exceed the max sequence, we wait the next second to generate id
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        }else if (timestamp < lastTimestamp) {
            long refusedTimes = lastTimestamp - timestamp;
            if (refusedTimes<MAX_BACKWORDS_MILLI_SECONDS){
                timestamp=lastTimestamp;
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) {
                    timestamp = tilNextMillis(lastTimestamp);
                }
//                lastTimestamp = timestamp;
            }else {
                // 可自定义异常类
                throw new UnsupportedOperationException(String.format("Clock moved backwards. Refusing for %d mseconds", refusedTimes));
            }
        }else {
            // 3、时间大于lastTimestamp没有发生回拨， sequence 从0开始
            this.sequence = 0L;
//            lastTimestamp = timestamp;
        }

        if(timestamp >= lastTimestamp){
            lastTimestamp = timestamp;
        }


        return allocate(timestamp - this.twepoch);
    }

    private long allocate(long deltaSeconds) {
        return (deltaSeconds << TIMESTAMP_LEFT_SHIFT) | (this.dataCenterId << DATA_CENTER_ID_SHIFT) | (this.workerId << WORKER_ID_SHIFT) | this.sequence;
    }

    private long timeGen() {
        long currentTimestamp = System.currentTimeMillis();
        // 时间戳超出最大值
        if (currentTimestamp - twepoch > MAX_DELTA_TIMESTAMP) {
            throw new UnsupportedOperationException("Timestamp bits is exhausted. Refusing ID generate. Now: " + currentTimestamp);
        }
        return currentTimestamp;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        if(timestamp <= lastTimestamp){
            if (lastTimestamp-timestamp<MAX_BACKWORDS_MILLI_SECONDS){
                timestamp=lastTimestamp+1;
            }else{
                throw new UnsupportedOperationException(String.format("Clock moved backwards. Refusing for %d mseconds", lastTimestamp-timestamp));
            }
        }
//        while (timestamp <= lastTimestamp) {
//            timestamp = timeGen();
//        }
        return timestamp;
    }

    /**
     * 测试
     * @param args
     */
    public static void main(String[] args) throws Exception {
        ModifiedSnowflakeIdGenerator snowflakeIdGenerator = new ModifiedSnowflakeIdGenerator(1,2);
        long l = System.currentTimeMillis();
        for (int i = 0; i < 5000000; i++) {
            long id = snowflakeIdGenerator.genID();
        }


        long l1 = System.currentTimeMillis();

        System.out.println("time:"+(l1-l));


//        System.out.println("ID=" + id + ", lastTimestamp=" + snowflakeIdGenerator.getLastTimestamp());
//        System.out.println("ID二进制：" + Long.toBinaryString(id));
//        System.out.println("解析ID:");
//        System.out.println("Sequence=" + snowflakeIdGenerator.getSequence(id));
//        System.out.println("WorkerId=" + snowflakeIdGenerator.getWorkerId(id));
//        System.out.println("DataCenterId=" + snowflakeIdGenerator.getDataCenterId(id));
//        System.out.println("GenerateDateTime=" + snowflakeIdGenerator.getGenerateDateTime(id));
//
//        System.out.println("Sequence2=" + snowflakeIdGenerator.getSequence2(id));
//        System.out.println("WorkerId2=" + snowflakeIdGenerator.getWorkerId2(id));
//        System.out.println("DataCenterId2=" + snowflakeIdGenerator.getDataCenterId2(id));
//        System.out.println("GenerateDateTime2=" + snowflakeIdGenerator.getGenerateDateTime2(id));
    }

}
