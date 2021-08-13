package cn.wildfirechat.pojos;

import java.util.ArrayList;
import java.util.List;

public class HealthCheckResult extends ArrayList<HealthCheckResult.Node>
{
    public static class Node {
        public String node;
        public CPU cpu;
        public Memory memory;
        public Disk disk;
    }

    public static class Memory {
        public long avail;
        public long max;
        public long free;
    }
    public static class Disk {
        public long usable;
        public long free;
        public long space;
    }
    public static class CPU {
        public int cores;
        public double load;
    }
}
