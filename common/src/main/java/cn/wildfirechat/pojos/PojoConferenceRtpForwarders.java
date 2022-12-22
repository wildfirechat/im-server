package cn.wildfirechat.pojos;

import java.util.List;

public class PojoConferenceRtpForwarders {
    public static class RtpForwarder {
        public static class RtpStream {
            public long streamId;
            public String type;
            public String host;
            public int port;
            public long ssrc;
            public int pt;
        }

        public String publisherId;
        public List<RtpStream> streams;
    }
    public String roomId;
    public List<RtpForwarder> forwarders;
}
