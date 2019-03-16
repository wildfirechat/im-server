/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;

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
    ERROR_CODE_USER_FORBIDDEN(8, "user forbidden"),

    //Message error
    ERROR_CODE_NOT_IN_GROUP(9, "not in group"),
    ERROR_CODE_INVALID_MESSAGE(10, "invalid message"),

    //Group error
    ERROR_CODE_GROUP_ALREADY_EXIST(11, "group aleady exist"),


    //user error
    ERROR_CODE_PASSWORD_INCORRECT(15, "password incorrect"),

    //user error
    ERROR_CODE_FRIEND_ALREADY_REQUEST(16, "already send request"),
    ERROR_CODE_FRIEND_REQUEST_BLOCKED(18, "friend request blocked"),
    ERROR_CODE_FRIEND_REQUEST_OVERTIME(19, "friend request overtime"),

    ERROR_CODE_NOT_IN_CHATROOM(20, "not in chatroom"),

    ERROR_CODE_NOT_IN_CHANNEL(21, "not in channel"),

    ERROR_CODE_SENSITIVE_MATCHED(242, "sensitive matched"),
    ERROR_CODE_SIGN_EXPIRED(243, "sign expired"),
    ERROR_CODE_AUTH_FAILURE(244, "auth failure"),
    ERROR_CODE_CLIENT_COUNT_OUT_OF_LIMIT(245, "client count out of limit"),
    ERROR_CODE_IN_BLACK_LIST(246, "user in balck list"),
    ERROR_CODE_FORBIDDEN_SEND_MSG(247, "forbidden send msg globally"),
    ERROR_CODE_NOT_RIGHT(248, "no right to operate"),
    ERROR_CODE_TIMEOUT(249, "timeout"),
    ERROR_CODE_OVER_FREQUENCY(250, "over frequency"),
    INVALID_PARAMETER(251, "Invalid parameter"),
    ERROR_CODE_NOT_EXIST(253, "not exist"),
    ERROR_CODE_NOT_IMPLEMENT(254, "not implement"),


    ERROR_CODE_ASYNC_HANDLER(255, "异步执行，服务器内部逻辑需要此代码，为正常情况，不能返回客户端"),;

    public int code;
    public String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if(errorCode.code == code) {
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
