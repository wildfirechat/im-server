package cn.wildfirechat.pojos;

import java.util.List;

public class ChatroomMemberUpdateEvent {
    public String operatorId;
    public String chatroomId;
    public List<String> memberIds;
    public int type;
}
