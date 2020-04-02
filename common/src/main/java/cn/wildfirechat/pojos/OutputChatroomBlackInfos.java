package cn.wildfirechat.pojos;

import java.util.ArrayList;
import java.util.List;

public class OutputChatroomBlackInfos {
    public static class OutputChatroomBlackInfo {
        public String userId;
        public int state;
        public long expiredTime;

        public OutputChatroomBlackInfo() {
        }

        public OutputChatroomBlackInfo(String userId, int state, long expiredTime) {
            this.userId = userId;
            this.state = state;
            this.expiredTime = expiredTime;
        }
    }
    public List<OutputChatroomBlackInfo> infos = new ArrayList<>();
    public void addBlackInfo(String userId, int state, long expiredTime) {
        infos.add(new OutputChatroomBlackInfo(userId, state, expiredTime));
    }
}
