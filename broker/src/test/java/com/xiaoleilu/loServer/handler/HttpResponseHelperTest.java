package com.xiaoleilu.loServer.handler;

import org.junit.Assert;
import org.junit.Test;

public class HttpResponseHelperTest {
    @Test
    public void getMultipartUploadResponseBody() throws Exception {
        final String remoteFileName = "foo.txt";
        final String fileUrl = "http://www.google.com/";

        final String actual =
            HttpResponseHelper.getMultipartUploadResponseBody(remoteFileName, fileUrl);

        Assert.assertEquals("{\"rc_url\":{\"path\":\"http://www.google.com/\",\"type\":0}}", actual);
    }

    @Test
    public void getFileExt() {
        Assert.assertEquals("野火", HttpResponseHelper.getFileExt("...野火"));
        Assert.assertEquals("", HttpResponseHelper.getFileExt("look_no_dots"));
        // For some reason, the "toLowerCase" in the non-empty case was not tested?
    }
}
