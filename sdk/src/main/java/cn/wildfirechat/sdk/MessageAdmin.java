package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.HttpUtils;

public class MessageAdmin {
    public static IMResult<SendMessageResult> sendMessage(String sender, Conversation conversation, MessagePayload payload) throws Exception {
        String path = APIPath.Msg_Send;
        SendMessageData messageData = new SendMessageData();
        messageData.setSender(sender);
        messageData.setConv(conversation);
        messageData.setPayload(payload);
        return HttpUtils.httpJsonPost(path, messageData, SendMessageResult.class);
    }

    public static IMResult<Void> recallMessage(String operator, long messageUid) throws Exception {
        String path = APIPath.Msg_Recall;
        RecallMessageData messageData = new RecallMessageData();
        messageData.setOperator(operator);
        messageData.setMessageUid(messageUid);
        return HttpUtils.httpJsonPost(path, messageData, Void.class);
    }
}
