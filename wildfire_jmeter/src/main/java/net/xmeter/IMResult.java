package net.xmeter;

public class IMResult<T> {
    public enum IMResultCode {
        //General error
        IMRESULT_CODE_SUCCESS(0, "success"),
        IMRESULT_CODE_SECRECT_KEY_MISMATCH(1, "secrect key mismatch"),
        IMRESULT_CODE_INVALID_DATA(2, "invalid data"),
        IMRESULT_CODE_NODE_NOT_EXIST(3, "node not exist"),
        IMRESULT_CODE_SERVER_ERROR(4, "server error"),
        IMRESULT_CODE_NOT_MODIFIED(5, "not modified"),


        //Auth error
        IMRESULT_CODE_TOKEN_ERROR(6, "token error"),
        IMRESULT_CODE_USER_FORBIDDEN(8, "user forbidden"),

        //Message error
        IMRESULT_CODE_NOT_IN_GROUP(9, "not in group"),
        IMRESULT_CODE_INVALID_MESSAGE(10, "invalid message"),

        //Group error
        IMRESULT_CODE_GROUP_ALREADY_EXIST(11, "group aleady exist"),


        //user error
        IMRESULT_CODE_PASSWORD_INCORRECT(15, "password incorrect"),

        //user error
        IMRESULT_CODE_FRIEND_ALREADY_REQUEST(16, "already send request"),
        IMRESULT_CODE_FRIEND_REQUEST_BLOCKED(18, "friend request blocked"),
        IMRESULT_CODE_FRIEND_REQUEST_OVERTIME(19, "friend request overtime"),

        IMRESULT_CODE_NOT_IN_CHATROOM(20, "not in chatroom"),

        IMRESULT_CODE_CLIENT_COUNT_OUT_OF_LIMIT(245, "client count out of limit"),
        IMRESULT_CODE_IN_BLACK_LIST(246, "user in balck list"),
        IMRESULT_CODE_FORBIDDEN_SEND_MSG(247, "forbidden send msg globally"),
        IMRESULT_CODE_NOT_RIGHT(248, "no right to operate"),
        IMRESULT_CODE_TIMEOUT(249, "timeout"),
        IMRESULT_CODE_OVER_FREQUENCY(250, "over frequency"),
        INVALID_PARAMETER(251, "Invalid parameter"),
        IMRESULT_CODE_NOT_EXIST(253, "not exist"),
        IMRESULT_CODE_NOT_IMPLEMENT(254, "not implement"),


        IMRESULT_CODE_ASYNC_HANDLER(255, "异步执行，服务器内部逻辑需要此代码，为正常情况，不能返回客户端"),;

        public int code;
        public String msg;

        IMResultCode(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public static IMResultCode fromCode(int code) {
            for (IMResultCode errorCode : IMResultCode.values()) {
                if(errorCode.code == code) {
                    return errorCode;
                }
            }
            return IMRESULT_CODE_SERVER_ERROR;
        }
        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }


    public int code;
    public String msg;
    public T result;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
