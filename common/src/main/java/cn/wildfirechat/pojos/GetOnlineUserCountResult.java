package cn.wildfirechat.pojos;

import java.util.ArrayList;

public class GetOnlineUserCountResult extends ArrayList<GetOnlineUserCountResult.Node> {
    public static class Node {
        public int node;
        public int count;
    }
}
