package com.xiaoleilu.loServer.handler;

import org.junit.Assert;
import org.junit.Test;

public class HttpResponseHelperTest {
    @Test
    public void getMultipartUploadResponseBody() throws Exception {
        final String remoteFileName = "foo.txt";
        final String fileUrl = "file:somefile";
        final String actual =
            HttpResponseHelper.getMultipartUploadResponseBody(remoteFileName, fileUrl);

        Assert.assertEquals("{\"rc_url\":{\"path\":\"file:somefile\",\"type\":0}}", actual);
    }

    @Test
    public void getFileExt() {
        Assert.assertEquals("野火", HttpResponseHelper.getFileExt("...野火"));
        Assert.assertEquals("", HttpResponseHelper.getFileExt("foo"));
        // For some reason, the "toLowerCase" in the non-empty case was not tested?
    }
}
