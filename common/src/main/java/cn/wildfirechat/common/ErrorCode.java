/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.common;

public enum ErrorCode {
    //General error
    ERROR_CODE_SUCCESS(0, "success"),
    ERROR_CODE_SECRECT_KEY_MISMATCH(1, "secrect key mismatch"),
    ERROR_CODE_INVALID_DATA(2, "invalid data"),
    ERROR_CODE_NODE_NOT_EXIST(3, "node not exist"),
    ERROR_CODE_SERVER_ERROR(4, "server error"),
    ERROR_CODE_NOT_MODIFIED(5, "not modified"),


    //Auth error
    ERROR_CODE_TOKEN_ERROR(6, "token error"),
    ERROR_CODE_KICKED_OFF(7, "kicked off"),
    ERROR_CODE_USER_FORBIDDEN(8, "user forbidden"),

    //Message error
    ERROR_CODE_NOT_IN_GROUP(9, "not in group"),
    ERROR_CODE_INVALID_MESSAGE(10, "invalid message"),

    //Group error
    ERROR_CODE_GROUP_ALREADY_EXIST(11, "group already exist"),
    ERROR_CODE_ALREADY_IN_GROUP(12, "member already in group"),

    //user error
    ERROR_CODE_FRIEND_ALREADY_REQUEST(16, "already send request"),
    ERROR_CODE_FRIEND_REQUEST_BLOCKED(18, "friend request blocked"),
    ERROR_CODE_FRIEND_REQUEST_EXPIRED(19, "friend request expired"),

    ERROR_CODE_NOT_IN_CHATROOM(20, "not in chatroom"),

    ERROR_CODE_NOT_IN_CHANNEL(21, "not in channel"),

    ERROR_CODE_NOT_LICENSED(22, "not licensed"),
    ERROR_CODE_ALREADY_FRIENDS(23, "already friends"),

    ERROR_CODE_RECALL_TIME_EXPIRED(24, "time expired"),
    ERROR_CODE_LOCK_ALREADY_LOCKED(25, "already locked"),
    ERROR_CODE_NOT_YOUR_LOCKED(26, "unload failure, not your lock"),
    ERROR_CODE_ROBOT_NO_TOKEN(27, "robot no token"),
    ERROR_CODE_WS_NOT_CONFIGURED_CORRECTLY(28, "ws not configured correctly"),
    ERROR_CODE_CONFERENCE_ROOM_NOT_EXIST(60, "conference room not exist"),
    ERROR_CODE_CONFERENCE_SERVICE_NOT_AVAILABLE(61, "conference service not available"),

    ERROR_CODE_APPLICATION_TOKEN_ERROR_OR_TIMEOUT(70, "application token error or timeout"),

    ERROR_CODE_SECRET_CHAT_ACCEPTED_BY_OTHER_CLIENT(81, "secret chat accepted by other client"),
    ERROR_CODE_SECRET_CHAT_SESSION_NOT_EXIST(82, "secret chat session not exist"),
    ERROR_CODE_SECRET_CHAT_NOT_SESSION_CLIENT(83, "not secret chat session client"),
    ERROR_CODE_SECRET_CHAT_SESSION_NOT_READY(84, "secret chat session not ready"),
    ERROR_CODE_SECRET_CHAT_SESSION_DESTROYED(85, "secret chat session destroyed"),
    ERROR_CODE_SECRET_CHAT_MO_DISABLED(86, "origin side disable secret chat"),
    ERROR_CODE_SECRET_CHAT_MT_DISABLED(87, "target side disable secret chat"),

    ERROR_CODE_FUNCTION_DISABLED(220, "function disabled"),

    ERROR_CODE_CHANNEL_NO_EXIST(236, "channel no exist"),
    ERROR_CODE_CHANNEL_NO_SECRET(237, "channel no secret"),
    ERROR_CODE_USER_NOT_PREPARED(238, "user not prepared"),
    ERROR_CODE_API_NOT_SIGNED(239, "api not signed or sign parameter not completion"),
    ERROR_CODE_GROUP_EXCEED_MAX_MEMBER_COUNT(240, "group exceed max member count"),
    ERROR_CODE_GROUP_MUTED(241, "group is muted"),
    ERROR_CODE_SENSITIVE_MATCHED(242, "sensitive matched"),
    ERROR_CODE_SIGN_EXPIRED(243, "sign expired"),
    ERROR_CODE_AUTH_FAILURE(244, "auth failure"),
    ERROR_CODE_CLIENT_COUNT_OUT_OF_LIMIT(245, "client count out of limit"),
    ERROR_CODE_IN_BLACK_LIST(246, "user in black list"),
    ERROR_CODE_FORBIDDEN_SEND_MSG(247, "forbidden send msg globally"),
    ERROR_CODE_NOT_RIGHT(248, "no right to operate"),
    ERROR_CODE_TIMEOUT(249, "timeout"),
    ERROR_CODE_OVER_FREQUENCY(250, "over frequency"),
    INVALID_PARAMETER(251, "Invalid parameter"),
    INVALID_ASYNC_HANDLING(252, "Async handling"),
    ERROR_CODE_NOT_EXIST(253, "not exist"),
    ERROR_CODE_NOT_IMPLEMENT(254, "not implement"),


    ERROR_CODE_SUCCESS_GZIPED(255, "success withe gzip response"),;

    public int code;
    public String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if(errorCode.code == (code&0xff)) {
                return errorCode;
            }
        }
        return ERROR_CODE_SERVER_ERROR;
    }
    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
