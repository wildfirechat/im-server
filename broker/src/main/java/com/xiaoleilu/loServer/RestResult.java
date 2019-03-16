/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer;

import win.liyufan.im.ErrorCode;

public class RestResult {


    int code;
    String msg;
    Object result;

    public static RestResult ok(Object object) {
        return resultOf(ErrorCode.ERROR_CODE_SUCCESS, ErrorCode.ERROR_CODE_SUCCESS.getMsg(), object);
    }

    public static RestResult ok() {
        return resultOf(ErrorCode.ERROR_CODE_SUCCESS, ErrorCode.ERROR_CODE_SUCCESS.getMsg(), null);
    }

    public static RestResult resultOf(ErrorCode errorCode) {
        return resultOf(errorCode, errorCode.msg, null);
    }

    public static RestResult resultOf(ErrorCode errorCode, String msg) {
        return resultOf(errorCode, msg, null);
    }

    public static RestResult resultOf(ErrorCode errorCode, String msg, Object object) {
        RestResult result = new RestResult();
        result.code = errorCode.code;
        result.msg = msg;
        result.result = object;
        return result;
    }

    public void setErrorCode(ErrorCode errorCode) {
        setCode(errorCode.code);
    }
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

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
