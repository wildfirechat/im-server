package cn.wildfirechat.pojos;

import java.util.ArrayList;
import java.util.List;

public class PojoConferenceParticipantList {
    public PojoConferenceParticipantList(List<PojoConferenceParticipant> participantList) {
        this.participantList = participantList;
    }

    public PojoConferenceParticipantList() {
        participantList = new ArrayList<>();
    }

    public List<PojoConferenceParticipant> participantList;

    public void addConferenceInfo(PojoConferenceParticipant participant) {
        this.participantList.add(participant);
    }

}
