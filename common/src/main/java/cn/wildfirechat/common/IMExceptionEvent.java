package cn.wildfirechat.common;

public class IMExceptionEvent {
    public interface EventType {
        int RDBS_Exception = 1;
        int MONGO_Exception = 2;
        int RPC_Exception = 3;
        int PUSH_SERVER_Exception = 4;
        int ADMIN_API_Exception = 5;
        int CHANNEL_API_Exception = 6;
        int ROBOT_API_Exception = 7;
        int SHORT_LINK_Exception = 8;
        int EVENT_CALLBACK_Exception = 9;
        int CONFERENCE_Exception = 10;
        int HEART_BEAT = 100;
    }

    public int event_type;
    public String msg;
    public String call_stack;
    public int count;
}
