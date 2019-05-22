package cn.wildfirechat.sdk.model;

import cn.wildfirechat.common.ErrorCode;

public class IMResult<T> {

    public int code;
    public String msg;
    public T result;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public ErrorCode getErrorCode() {
        return ErrorCode.fromCode(code);
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
