package io.moquette.spi.impl.security;

import org.junit.Test;
import org.junit.Assert;

public class AESTest {

    @Test
    public void testAESEncrypt() {
        byte[] encrypted = new byte[]{-31, 117, 20, -91, 78,
            24, 55, -104, 79, -83, 125, -55, -58, -96, 67, 122};

        Assert.assertArrayEquals(encrypted,
            AES.AESEncrypt("foo", "0x000x110x220x33"));

        Assert.assertNull(AES.AESEncrypt(new byte[]{1, 2}, (byte[]) null));
        Assert.assertNull(AES.AESEncrypt(new byte[]{1, 2}, new byte[]{0x00}));
    }

    @Test
    public void testAESDecrypt() {
        byte[] encrypted1 = AES.AESEncrypt("", "0x000x110x220x33");

        Assert.assertNull(AES.AESDecrypt(encrypted1,
            "0x000x110x220x33", true));
        Assert.assertNull(AES.AESDecrypt(new byte[]{1, 2},
            "0x000x110x220x33", true));
        Assert.assertNull(AES.AESDecrypt(null, (byte[]) null, true));
        Assert.assertNull(AES.AESDecrypt(new byte[]{1, 2},
            new byte[]{0x00}, true));
        Assert.assertNull(AES.AESDecrypt(new byte[]{76, 92, -80, -71, -28,
                -45, 31, 121, -58, -124, 16, -99, -81, 59, -84, -70},
            "0x000x110x220x33", true));

        byte[] encrypted2 = AES.AESEncrypt("foobar", "0x000x110x220x33");
        byte[] foobarBytes = new byte[]{102, 111, 111, 98, 97, 114};

        Assert.assertArrayEquals(foobarBytes, AES.AESDecrypt(encrypted2,
            "0x000x110x220x33", true));
    }
}
