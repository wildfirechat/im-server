package cn.wildfirechat.sdk;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

public class Main {
    public static void main(String[] args) throws Exception {
        AdminHttpUtils.init("http://localhost:18080", "123456");

        InputOutputUserInfo userInfo = new InputOutputUserInfo();
        userInfo.setUserId("userId1");
        userInfo.setName("user1");
        userInfo.setMobile("13900000000");
        userInfo.setDisplayName("user 1");

        IMResult<OutputCreateUser>  resultCreateUser = UserAdmin.createUser(userInfo);
        if (resultCreateUser != null && resultCreateUser.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create user " + resultCreateUser.getResult().getName() + " success");
        } else {
            System.out.println("Create user failure");
            System.exit(-1);
        }

        IMResult<InputOutputUserInfo> resultGetUserInfo1 = UserAdmin.getUserByName(userInfo.getName());
        if (resultGetUserInfo1 != null && resultGetUserInfo1.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (userInfo.getUserId().equals(resultGetUserInfo1.getResult().getUserId())
                && userInfo.getName().equals(resultGetUserInfo1.getResult().getName())
                && userInfo.getMobile().equals(resultGetUserInfo1.getResult().getMobile())
                && userInfo.getDisplayName().equals(resultGetUserInfo1.getResult().getDisplayName())) {
                System.out.println("get user info success");
            } else {
                System.out.println("get user info by name failure");
                System.exit(-1);
            }
        } else {
            System.out.println("get user info by name failure");
            System.exit(-1);
        }

        IMResult<InputOutputUserInfo> resultGetUserInfo2 = UserAdmin.getUserByMobile(userInfo.getMobile());
        if (resultGetUserInfo2 != null && resultGetUserInfo2.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (userInfo.getUserId().equals(resultGetUserInfo2.getResult().getUserId())
                && userInfo.getName().equals(resultGetUserInfo2.getResult().getName())
                && userInfo.getMobile().equals(resultGetUserInfo2.getResult().getMobile())
                && userInfo.getDisplayName().equals(resultGetUserInfo2.getResult().getDisplayName())) {
                System.out.println("get user info success");
            } else {
                System.out.println("get user info by mobile failure");
                System.exit(-1);
            }
        } else {
            System.out.println("get user info by mobile failure");
            System.exit(-1);
        }

        IMResult<InputOutputUserInfo> resultGetUserInfo3 = UserAdmin.getUserByUserId(userInfo.getUserId());
        if (resultGetUserInfo3 != null && resultGetUserInfo3.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (userInfo.getUserId().equals(resultGetUserInfo3.getResult().getUserId())
                && userInfo.getName().equals(resultGetUserInfo3.getResult().getName())
                && userInfo.getMobile().equals(resultGetUserInfo3.getResult().getMobile())
                && userInfo.getDisplayName().equals(resultGetUserInfo3.getResult().getDisplayName())) {
                System.out.println("get user info success");
            } else {
                System.out.println("get user info by userId failure");
                System.exit(-1);
            }
        } else {
            System.out.println("get user info by userId failure");
            System.exit(-1);
        }

        IMResult<OutputGetIMTokenData> resultGetToken = UserAdmin.getUserToken(userInfo.getUserId(), "client111");
        if (resultGetToken != null && resultGetToken.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get token success: " + resultGetToken.getResult().getToken());
        } else {
            System.out.println("get user token failure");
            System.exit(-1);
        }

        IMResult<Void> resultVoid =UserAdmin.updateUserBlockStatus(userInfo.getUserId(), 1);
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("block user done");
        } else {
            System.out.println("block user failure");
            System.exit(-1);
        }

        IMResult<OutputUserStatus> resultCheckUserStatus = UserAdmin.checkUserBlockStatus(userInfo.getUserId());
        if (resultCheckUserStatus != null && resultCheckUserStatus.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (resultCheckUserStatus.getResult().getStatus() == 1) {
                System.out.println("check user status success");
            } else {
                System.out.println("user status not correct");
                System.exit(-1);
            }
        } else {
            System.out.println("block user failure");
            System.exit(-1);
        }

        IMResult<OutputUserBlockStatusList> resultBlockStatusList = UserAdmin.getBlockedList();
        if (resultBlockStatusList != null && resultBlockStatusList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            boolean success = false;
            for (InputOutputUserBlockStatus blockStatus : resultBlockStatusList.getResult().getStatusList()) {
                if (blockStatus.getUserId().equals(userInfo.getUserId()) && blockStatus.getStatus() == 1) {
                    System.out.println("get block list done");
                    success = true;
                    break;
                }
            }
            if (!success) {
                System.out.println("block user status is not expected");
                System.exit(-1);
            }
        } else {
            System.out.println("block user failure");
            System.exit(-1);
        }

    }
}
