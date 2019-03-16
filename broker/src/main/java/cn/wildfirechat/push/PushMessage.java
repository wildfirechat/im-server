/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.push;


public class PushMessage {
    public String sender;
    public String senderName;
    public int convType;
    public String target;
    public String targetName;
    public int line;
    public int cntType;
    public long serverTime;
    //消息的类型，普通消息通知栏；voip要透传。
    public int pushMessageType;
    //推送类型，android推送分为小米/华为/魅族等。ios分别为开发和发布。
    public int pushType;
    public String pushContent;
    public int unReceivedMsg;
    public int mentionedType;
    public String packageName;
    public String deviceToken;
    public String voipDeviceToken;
    public boolean isHiddenDetail;
    public String language;

    public PushMessage(String sender, int conversationType, String target, int line, int messageContentType, long serverTime, String senderName, String targetName, int unReceivedMsg, int mentionedType, boolean isHiddenDetail, String language) {
        this.sender = sender;
        this.convType = conversationType;
        this.target = target;
        this.senderName = senderName;
        this.targetName = targetName;
        this.line = line;
        this.cntType = messageContentType;
        this.serverTime = serverTime;
        this.unReceivedMsg = unReceivedMsg;
        this.pushMessageType = (cntType == 400 ? PushServer.PushMessageType.PUSH_MESSAGE_TYPE_VOIP_INVITE : (cntType == 402 ? PushServer.PushMessageType.PUSH_MESSAGE_TYPE_VOIP_BYE : PushServer.PushMessageType.PUSH_MESSAGE_TYPE_NORMAL));
        this.mentionedType = mentionedType;
        this.isHiddenDetail = isHiddenDetail;
        this.language = language;
    }
}
