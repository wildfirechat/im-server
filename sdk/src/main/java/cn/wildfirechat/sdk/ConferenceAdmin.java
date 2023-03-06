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

    public static IMResult<Boolean> existsConferences(String conferenceId) throws Exception {
        String path = APIPath.Conference_Exist;
        PojoConferenceRoomId data = new PojoConferenceRoomId(conferenceId, false);
        return AdminHttpUtils.httpJsonPost(path, data, Boolean.class);
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

    public static IMResult<Void> enableRecording(String roomId, boolean advance, boolean recording) throws Exception {
        String path = APIPath.Conference_Recording;
        PojoConferenceRecording create = new PojoConferenceRecording();
        create.roomId = roomId;
        create.recording = recording;
        create.advance = advance;
        return AdminHttpUtils.httpJsonPost(path, create, Void.class);
    }

    public static IMResult<Void> destroy(String roomId, boolean advance) throws Exception {
        String path = APIPath.Conference_Destroy;
        PojoConferenceRoomId conferenceRoomId = new PojoConferenceRoomId(roomId, advance);
        return AdminHttpUtils.httpJsonPost(path, conferenceRoomId, Void.class);
    }

    public static IMResult<Void> rtpForward(String roomId, String userId, String rtpHost, int audioPort, int audioPt, long audioSSRC, int videoPort, int videoPt, long videoSSRC) throws Exception {
        String path = APIPath.Conference_Rtp_Forward;
        PojoConferenceRtpForwardReq req = new PojoConferenceRtpForwardReq(roomId, userId, rtpHost, audioPort, audioPt, audioSSRC, videoPort, videoPt, videoSSRC);
        return AdminHttpUtils.httpJsonPost(path, req, Void.class);
    }

    public static IMResult<Void> stopRtpForward(String roomId, String userId, long streamId) throws Exception {
        String path = APIPath.Conference_Stop_Rtp_Forward;
        PojoConferenceStopRtpForwardReq req = new PojoConferenceStopRtpForwardReq(roomId, userId, streamId);
        return AdminHttpUtils.httpJsonPost(path, req, Void.class);
    }

    public static IMResult<PojoConferenceRtpForwarders> listRtpForwarders(String roomId) throws Exception {
        String path = APIPath.Conference_List_Rtp_Forward;
        PojoConferenceRoomId req = new PojoConferenceRoomId();
        req.roomId = roomId;
        return AdminHttpUtils.httpJsonPost(path, req, PojoConferenceRtpForwarders.class);
    }
}
