package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;
import cn.wildfirechat.sdk.utilities.ChannelHttpUtils;
import cn.wildfirechat.sdk.utilities.RobotHttpUtils;

import java.util.List;

//仅专业版支持，社区版不支持
public class ChannelServiceApi {
    private ChannelHttpUtils httpUtils;

    public ChannelServiceApi(String imurl, String channelId, String secret) {
        httpUtils = new ChannelHttpUtils(imurl, channelId, secret);
    }

    public IMResult<InputOutputUserInfo> getUserInfo(String userId) throws Exception {
        String path = APIPath.Channel_User_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(userId, null, null);
        return httpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    public IMResult<InputOutputUserInfo> getUserInfoByName(String userName) throws Exception {
        String path = APIPath.Channel_User_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(null, userName, null);
        return httpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    public IMResult<InputOutputUserInfo> getUserInfoByMobile(String mobile) throws Exception {
        String path = APIPath.Channel_User_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(null, null, mobile);
        return httpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    public IMResult<Void> modifyChannelInfo(/*ProtoConstants.ModifyChannelInfoType*/int type, String value) throws Exception {
        String path = APIPath.Channel_Update_Profile;
        InputModifyChannelInfo modifyChannelInfo = new InputModifyChannelInfo();
        modifyChannelInfo.setType(type);
        modifyChannelInfo.setValue(value);
        return httpUtils.httpJsonPost(path, modifyChannelInfo, Void.class);
    }

    public IMResult<OutputGetChannelInfo> getChannelInfo() throws Exception {
        String path = APIPath.Channel_Get_Profile;
        return httpUtils.httpJsonPost(path, null, OutputGetChannelInfo.class);
    }

    public IMResult<SendMessageResult> sendMessage(int line, List<String> targets, MessagePayload payload) throws Exception {
        String path = APIPath.Channel_Message_Send;
        SendChannelMessageData messageData = new SendChannelMessageData();
        messageData.setLine(0);
        messageData.setTargets(targets);
        messageData.setPayload(payload);
        return httpUtils.httpJsonPost(path, messageData, SendMessageResult.class);
    }

    public IMResult<Void> subscribe(String target) throws Exception {
        String path = APIPath.Channel_Subscribe;
        InputChannelSubscribe input = new InputChannelSubscribe();
        input.setTarget(target);
        input.setSubscribe(1);
        return httpUtils.httpJsonPost(path, input, Void.class);
    }

    public IMResult<Void> unsubscribe(String target) throws Exception {
        String path = APIPath.Channel_Subscribe;
        InputChannelSubscribe input = new InputChannelSubscribe();
        input.setTarget(target);
        input.setSubscribe(0);
        return httpUtils.httpJsonPost(path, input, Void.class);
    }

    public IMResult<OutputStringList> getSubscriberList() throws Exception {
        String path = APIPath.Channel_Subscriber_List;
        return httpUtils.httpJsonPost(path, null, OutputStringList.class);
    }

}
