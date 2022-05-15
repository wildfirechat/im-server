package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.ChannelHttpUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.List;

import static cn.wildfirechat.proto.ProtoConstants.ApplicationType.ApplicationType_Channel;
import static cn.wildfirechat.proto.ProtoConstants.ApplicationType.ApplicationType_Robot;

//仅专业版支持，社区版不支持
public class ChannelServiceApi {
    private final ChannelHttpUtils channelHttpUtils;

    public ChannelServiceApi(String imurl, String channelId, String secret) {
        channelHttpUtils = new ChannelHttpUtils(imurl, channelId, secret);
    }

    public IMResult<InputOutputUserInfo> getUserInfo(String userId) throws Exception {
        String path = APIPath.Channel_User_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(userId, null, null);
        return channelHttpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    public IMResult<InputOutputUserInfo> getUserInfoByName(String userName) throws Exception {
        String path = APIPath.Channel_User_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(null, userName, null);
        return channelHttpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    public IMResult<InputOutputUserInfo> getUserInfoByMobile(String mobile) throws Exception {
        String path = APIPath.Channel_User_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(null, null, mobile);
        return channelHttpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    public IMResult<Void> modifyChannelInfo(/*ProtoConstants.ModifyChannelInfoType*/int type, String value) throws Exception {
        String path = APIPath.Channel_Update_Profile;
        InputModifyChannelInfo modifyChannelInfo = new InputModifyChannelInfo();
        modifyChannelInfo.setType(type);
        modifyChannelInfo.setValue(value);
        return channelHttpUtils.httpJsonPost(path, modifyChannelInfo, Void.class);
    }

    public IMResult<OutputGetChannelInfo> getChannelInfo() throws Exception {
        String path = APIPath.Channel_Get_Profile;
        return channelHttpUtils.httpJsonPost(path, null, OutputGetChannelInfo.class);
    }

    public IMResult<SendMessageResult> sendMessage(int line, List<String> targets, MessagePayload payload) throws Exception {
        String path = APIPath.Channel_Message_Send;
        SendChannelMessageData messageData = new SendChannelMessageData();
        messageData.setLine(0);
        messageData.setTargets(targets);
        messageData.setPayload(payload);
        return channelHttpUtils.httpJsonPost(path, messageData, SendMessageResult.class);
    }

    public IMResult<Void> subscribe(String userId) throws Exception {
        String path = APIPath.Channel_Subscribe;
        InputChannelSubscribe input = new InputChannelSubscribe();
        input.setTarget(userId);
        input.setSubscribe(1);
        return channelHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public IMResult<Void> unsubscribe(String userId) throws Exception {
        String path = APIPath.Channel_Subscribe;
        InputChannelSubscribe input = new InputChannelSubscribe();
        input.setTarget(userId);
        input.setSubscribe(0);
        return channelHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public IMResult<OutputStringList> getSubscriberList() throws Exception {
        String path = APIPath.Channel_Subscriber_List;
        return channelHttpUtils.httpJsonPost(path, null, OutputStringList.class);
    }

    public IMResult<OutputApplicationUserInfo> applicationGetUserInfo(String authCode) throws Exception {
        String path = APIPath.Channel_Application_Get_UserInfo;
        InputApplicationGetUserInfo input = new InputApplicationGetUserInfo();
        input.setAuthCode(authCode);
        return channelHttpUtils.httpJsonPost(path, input, OutputApplicationUserInfo.class);
    }

    public OutputApplicationConfigData getApplicationSignature() {
        int nonce = (int)(Math.random() * 100000 + 3);
        long timestamp = System.currentTimeMillis()/1000;
        String str = nonce + "|" + channelHttpUtils.getChannelId() + "|" + timestamp + "|" + channelHttpUtils.getChannelSecret();
        String sign = DigestUtils.sha1Hex(str);
        OutputApplicationConfigData configData = new OutputApplicationConfigData();
        configData.setAppId(channelHttpUtils.getChannelId());
        configData.setAppType(ApplicationType_Channel);
        configData.setTimestamp(timestamp);
        configData.setNonceStr(nonce+"");
        configData.setSignature(sign);
        return configData;
    }

}
