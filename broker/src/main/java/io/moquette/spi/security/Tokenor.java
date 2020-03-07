package io.moquette.spi.security;


import com.hazelcast.util.StringUtil;
import io.moquette.spi.impl.security.AES;

public class Tokenor {
	private static String KEY = "testim";
	private static long expiredTime = Long.MAX_VALUE;

	public static void setKey(String key) {
	    if (!StringUtil.isNullOrEmpty(key)) {
            KEY = key;
        }
    }

    public static void setExpiredTime(long expiredTime) {
        Tokenor.expiredTime = expiredTime;
    }

    public static String getUserId(byte[] password) {
        try {
            String signKey =
                DES.decryptDES(new String(password));

            if (signKey.startsWith(KEY + "|")) {
                signKey = signKey.substring(KEY.length() + 1);
                long timestamp = Long.parseLong(signKey.substring(0, signKey.indexOf('|')));
                if (expiredTime > 0 && System.currentTimeMillis() - timestamp > expiredTime) {
                    return null;
                }
                String id = signKey.substring(signKey.indexOf('|') + 1);
                return id;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
        return null;
    }
    public static String getToken(String username) {
        String signKey = KEY + "|" + (System.currentTimeMillis()) + "|" + username;
        try {
            return DES.encryptDES(signKey);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
