package cn.wildfirechat.pojos;

import java.util.ArrayList;
import java.util.List;

public class PojoConferenceInfoList {
    public PojoConferenceInfoList(List<PojoConferenceInfo> conferenceInfoList) {
        this.conferenceInfoList = conferenceInfoList;
    }

    public PojoConferenceInfoList() {
        conferenceInfoList = new ArrayList<>();
    }

    public List<PojoConferenceInfo> conferenceInfoList;

    public void addConferenceInfo(PojoConferenceInfo conferenceInfo) {
        this.conferenceInfoList.add(conferenceInfo);
    }

}
