package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

import java.util.List;

public class MessageAdmin {
    public static IMResult<SendMessageResult> sendMessage(String sender, Conversation conversation, MessagePayload payload) throws Exception {
        return sendMessage(sender, conversation, payload, null);
    }
    //toUsers为发送给会话中部分用户用的，正常为null，仅当需要指定群/频道/聊天室中部分接收用户时使用
    public static IMResult<SendMessageResult> sendMessage(String sender, Conversation conversation, MessagePayload payload, List<String> toUsers) throws Exception {
        String path = APIPath.Msg_Send;
        SendMessageData messageData = new SendMessageData();
        messageData.setSender(sender);
        messageData.setConv(conversation);
        messageData.setPayload(payload);
        messageData.setToUsers(toUsers);
        return AdminHttpUtils.httpJsonPost(path, messageData, SendMessageResult.class);
    }

    public static IMResult<Void> recallMessage(String operator, long messageUid) throws Exception {
        String path = APIPath.Msg_Recall;
        RecallMessageData messageData = new RecallMessageData();
        messageData.setOperator(operator);
        messageData.setMessageUid(messageUid);
        return AdminHttpUtils.httpJsonPost(path, messageData, Void.class);
    }

    public static IMResult<Void> deleteMessage(long messageUid) throws Exception {
        String path = APIPath.Msg_Delete;
        DeleteMessageData deleteMessageData = new DeleteMessageData();
        deleteMessageData.setMessageUid(messageUid);
        return AdminHttpUtils.httpJsonPost(path, deleteMessageData, Void.class);
    }

    public static IMResult<Void> updateMessageContent(String operator, long messageUid, MessagePayload payload, boolean distribute) throws Exception {
        String path = APIPath.Msg_Update;
        UpdateMessageContentData updateMessageContentData = new UpdateMessageContentData();
        updateMessageContentData.setOperator(operator);
        updateMessageContentData.setMessageUid(messageUid);
        updateMessageContentData.setPayload(payload);
        updateMessageContentData.setDistribute(distribute?1:0);
        updateMessageContentData.setUpdateTimestamp(0);
        return AdminHttpUtils.httpJsonPost(path, updateMessageContentData, Void.class);
    }



    /**
     * 撤回群发或者广播的消息
     * @param operator 操作者
     * @param messageUid 消息唯一ID
     * @return
     * @throws Exception
     */
    public static IMResult<Void> recallBroadCastMessage(String operator, long messageUid) throws Exception {
        String path = APIPath.Msg_RecallBroadCast;
        RecallMessageData messageData = new RecallMessageData();
        messageData.setOperator(operator);
        messageData.setMessageUid(messageUid);
        return AdminHttpUtils.httpJsonPost(path, messageData, Void.class);
    }

    public static IMResult<Void> recallMultiCastMessage(String operator, long messageUid, List<String> receivers) throws Exception {
        String path = APIPath.Msg_RecallMultiCast;
        RecallMultiCastMessageData messageData = new RecallMultiCastMessageData();
        messageData.operator = operator;
        messageData.messageUid = messageUid;
        messageData.receivers = receivers;

        return AdminHttpUtils.httpJsonPost(path, messageData, Void.class);
    }

    public static IMResult<BroadMessageResult> broadcastMessage(String sender, int line, MessagePayload payload) throws Exception {
        String path = APIPath.Msg_Broadcast;
        BroadMessageData messageData = new BroadMessageData();
        messageData.setSender(sender);
        messageData.setLine(line);
        messageData.setPayload(payload);
        return AdminHttpUtils.httpJsonPost(path, messageData, BroadMessageResult.class);
    }

    public static IMResult<MultiMessageResult> multicastMessage(String sender, List<String> receivers, int line, MessagePayload payload) throws Exception {
        String path = APIPath.Msg_Multicast;
        MulticastMessageData messageData = new MulticastMessageData();
        messageData.setSender(sender);
        messageData.setTargets(receivers);
        messageData.setLine(line);
        messageData.setPayload(payload);
        return AdminHttpUtils.httpJsonPost(path, messageData, MultiMessageResult.class);
    }
}
