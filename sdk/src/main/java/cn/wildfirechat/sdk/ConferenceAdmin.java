package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

public class ConferenceAdmin {
    public static IMResult<PojoConferenceInfoList> listConferences() throws Exception {
        String path = APIPath.Conference_List;
        return AdminHttpUtils.httpJsonPost(path, null, PojoConferenceInfoList.class);
    }

    public static IMResult<PojoConferenceParticipantList> listParticipants(String roomId, boolean advance) throws Exception {
        String path = APIPath.Conference_List_Participant;
        PojoConferenceRoomId data = new PojoConferenceRoomId(roomId, advance);
        return AdminHttpUtils.httpJsonPost(path, data, PojoConferenceParticipantList.class);
    }

    public static IMResult<Void> createRoom(String roomId, String description, String pin, int maxPublisher, boolean advance, int bitrate, boolean recording, boolean permanent) throws Exception {
        String path = APIPath.Conference_Create;
        PojoConferenceCreate create = new PojoConferenceCreate();
        create.roomId = roomId;
        create.description = description;
        create.pin = pin;
        create.max_publishers = maxPublisher;
        create.advance = advance;
        create.bitrate = bitrate;
        create.recording = recording;
        create.permanent = permanent;
        return AdminHttpUtils.httpJsonPost(path, create, Void.class);
    }

    public static IMResult<Void> destroy(String roomId, boolean advance) throws Exception {
        String path = APIPath.Conference_Destroy;
        PojoConferenceRoomId conferenceRoomId = new PojoConferenceRoomId(roomId, advance);
        return AdminHttpUtils.httpJsonPost(path, conferenceRoomId, Void.class);
    }
}
