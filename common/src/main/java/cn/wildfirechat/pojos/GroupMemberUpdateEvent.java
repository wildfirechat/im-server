package cn.wildfirechat.pojos;

import java.util.List;

public class GroupMemberUpdateEvent {
    public String operatorId;
    public String groupId;
    public List<String> memberIds;
    public int type;
    public String value;
}
