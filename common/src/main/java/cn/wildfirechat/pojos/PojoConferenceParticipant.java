package cn.wildfirechat.pojos;

import java.util.List;

public class PojoConferenceParticipant {
    public static class Stream {
        public String type;
        public String mid;
        public String codec;
    }

    public String userId;
    public boolean publishing;
    public List<Stream> streams;
}
