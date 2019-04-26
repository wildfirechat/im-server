/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.proto.WFCMessage;
import com.qiniu.util.Auth;
import com.xiaoleilu.loServer.action.UploadFileAction;
import io.moquette.server.config.MediaServerConfig;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;

@Handler(IMTopic.GetQiniuUploadTokenTopic)
public class GetQiniuUploadTokenHandler extends IMHandler<Byte> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, Byte request, Qos1PublishHandler.IMCallback callback) {
        int type = request;
        String token;

        WFCMessage.GetUploadTokenResult.Builder resultBuilder = WFCMessage.GetUploadTokenResult.newBuilder();
        if (MediaServerConfig.USER_QINIU) {
            Auth auth = Auth.create(MediaServerConfig.QINIU_ACCESS_KEY, MediaServerConfig.QINIU_SECRET_KEY);

//#Media_Type_GENERAL = 0,
//#Media_Type_IMAGE = 1,
//#Media_Type_VOICE = 2,
//#Media_Type_VIDEO = 3,
//#Media_Type_FILE = 4,
//#Media_Type_PORTRAIT = 5,
//#Media_Type_FAVORITE = 6

            String bucketName;
            String bucketDomain;
            switch (type) {
                case 0:
                    bucketName = MediaServerConfig.QINIU_BUCKET_GENERAL_NAME;
                    bucketDomain = MediaServerConfig.QINIU_BUCKET_GENERAL_DOMAIN;
                    break;
                case 1:
                    bucketName = MediaServerConfig.QINIU_BUCKET_IMAGE_NAME;
                    bucketDomain = MediaServerConfig.QINIU_BUCKET_IMAGE_DOMAIN;
                    break;
                case 2:
                    bucketName = MediaServerConfig.QINIU_BUCKET_VOICE_NAME;
                    bucketDomain = MediaServerConfig.QINIU_BUCKET_VOICE_DOMAIN;
                    break;
                case 3:
                    bucketName = MediaServerConfig.QINIU_BUCKET_VIDEO_NAME;
                    bucketDomain = MediaServerConfig.QINIU_BUCKET_VIDEO_DOMAIN;
                    break;
                case 4:
                    bucketName = MediaServerConfig.QINIU_BUCKET_FILE_NAME;
                    bucketDomain = MediaServerConfig.QINIU_BUCKET_FILE_DOMAIN;
                    break;
                case 5:
                    bucketName = MediaServerConfig.QINIU_BUCKET_PORTRAIT_NAME;
                    bucketDomain = MediaServerConfig.QINIU_BUCKET_PORTRAIT_DOMAIN;
                    break;
                case 6:
                    bucketName = MediaServerConfig.QINIU_BUCKET_FAVORITE_NAME;
                    bucketDomain = MediaServerConfig.QINIU_BUCKET_FAVORITE_DOMAIN;
                    break;
                default:
                    bucketName = MediaServerConfig.QINIU_BUCKET_GENERAL_NAME;
                    bucketDomain = MediaServerConfig.QINIU_BUCKET_GENERAL_DOMAIN;
                    break;
            }

            token = auth.uploadToken(bucketName);
            resultBuilder.setDomain(bucketDomain)
                .setServer(MediaServerConfig.QINIU_SERVER_URL);
            resultBuilder.setPort(80);
        } else {
            token = UploadFileAction.getToken(type);
            resultBuilder.setDomain("http://" + MediaServerConfig.SERVER_IP + ":" + MediaServerConfig.HTTP_SERVER_PORT)
                .setServer(MediaServerConfig.SERVER_IP);
            resultBuilder.setPort(MediaServerConfig.HTTP_SERVER_PORT);
        }

        resultBuilder.setToken(token);

        byte[] data = resultBuilder.buildPartial().toByteArray();
        ackPayload.ensureWritable(data.length).writeBytes(data);
        return ErrorCode.ERROR_CODE_SUCCESS;
    }
}
