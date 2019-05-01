/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action;

import com.hazelcast.util.StringUtil;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.RequireAuthentication;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.*;
import io.moquette.server.config.MediaServerConfig;
import io.moquette.spi.impl.security.AES;
import io.moquette.spi.security.DES;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static com.xiaoleilu.loServer.handler.HttpResponseHelper.getFileExt;

@Route("/fs")
@HttpMethod("POST")
@RequireAuthentication
public class UploadFileAction extends Action {
    private static final String KEY = "imfile";
    private static final Logger logger = LoggerFactory.getLogger(UploadFileAction.class);
    private static final HttpDataFactory factory = new DefaultHttpDataFactory(false);

    public static class InvalidateTokenExecption extends Exception {

    }

    public static String getToken(int type) {
        String signKey = KEY + "|" + (System.currentTimeMillis()) + "|" + type;
        try {
            return DES.encryptDES(signKey);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static int validateToken(String token) throws InvalidateTokenExecption {
        try {
            String signKey = DES.decryptDES(token);
            String[] parts = signKey.split("\\|");
            if(parts.length == 3) {
                if(parts[0].equals(KEY)) {
                    long timestamp = Long.parseLong(parts[1]);
                    if(Math.abs(System.currentTimeMillis() - timestamp) < 2 * 60 * 60 * 1000) {
                        return Integer.parseInt(parts[2]);
                    }
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
        throw new InvalidateTokenExecption();
    }

    @Override
    public boolean action(Request r, Response response) {
        if (r.getNettyRequest() instanceof FullHttpRequest) {

            FullHttpRequest request = (FullHttpRequest) r.getNettyRequest();
            String requestId = UUID.randomUUID().toString().replace("-", "");
            logger.info("HttpFileServerHandler received a request: method=" + request.getMethod() + ", uri=" + request.getUri() + ", requestId=" + requestId);

            if (!request.getDecoderResult().isSuccess()) {
                logger.warn("http decode failed!");
                response.setStatus(HttpResponseStatus.BAD_REQUEST);
                response.setContent("http decode failed");
                return true;
            }

            multipartUpload(request, requestId, response);

        }
        return true;
    }


    /**
     * multipart上传
     */
    private void multipartUpload(FullHttpRequest request, String requestId, Response response) {
        HttpPostRequestDecoder decoder = null;
        try {
            decoder = new HttpPostRequestDecoder(factory, request);
        } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
            logger.error("Failed to decode file data!", e1);
            response.setStatus(HttpResponseStatus.BAD_REQUEST);
            response.setContent("Failed to decode file data!");
            return;
        }


        if (decoder != null) {
            if (request instanceof HttpContent) {
                HttpContent chunk = (HttpContent) request;
                try {
                    decoder.offer(chunk);
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
                    logger.warn("BAD_REQUEST, Failed to decode file data");
                    response.setStatus(HttpResponseStatus.BAD_REQUEST);
                    response.setContent("Failed to decode file data!");
                    return;
                }

                long fileTotalSize = 0;
                if (request.headers().contains("X-File-Total-Size")) {
                    try {
                        fileTotalSize = Integer.parseInt(request.headers().get("X-File-Total-Size"));
                    } catch (Exception e) {
                        logger.warn("invalid X-File-Total-Size value!");
                    }
                }

                readHttpDataChunkByChunk(response, decoder, requestId, HttpHeaders.isKeepAlive(request));

                if (chunk instanceof LastHttpContent) {

                }
            } else {
                logger.warn("BAD_REQUEST, Not a http request");
                response.setStatus(HttpResponseStatus.BAD_REQUEST);
                response.setContent("Not a http request");
            }
        }
    }

    /**
     * readHttpDataChunkByChunk
     */
    private void readHttpDataChunkByChunk(Response response, HttpPostRequestDecoder decoder, String requestId, boolean isKeepAlive) {
        try {
            int[] bucket = new int[1];
            bucket[0] = -1;
            while (decoder.hasNext()) {
                InterfaceHttpData data = decoder.next();
                if (data != null) {
                    try {
                        if(!writeFileUploadData(data, response, requestId, isKeepAlive, bucket)) {
                            break;
                        }
                    } finally {
                        data.release();
                    }
                }
            }
        } catch (Exception e) {
            logger.info("chunk end");
        }
    }

    /**
     * writeFileUploadData
     */
    private boolean writeFileUploadData(InterfaceHttpData data, Response response, String requestId, boolean isKeepAlive, int[] bucket) {
        try {
            if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                FileUpload fileUpload = (FileUpload) data;

                String remoteFileName = fileUpload.getFilename();
                long remoteFileSize = fileUpload.length();

                if(bucket[0] == -1) {
                    logger.warn("Not authenticated!");

                    response.setStatus(HttpResponseStatus.BAD_REQUEST);
                    response.setContent("Not authenticated!");
                    return false;
                }

                if (StringUtil.isNullOrEmpty(remoteFileName)) {
                    logger.warn("remoteFileName is empty!");

                    response.setStatus(HttpResponseStatus.BAD_REQUEST);
                    response.setContent("file name is empty");
                    return false;
                }

                if (StringUtil.isNullOrEmpty(requestId)) {
                    logger.warn("requestId is empty!");
                    response.setStatus(HttpResponseStatus.BAD_REQUEST);
                    response.setContent("requestId is empty!");
                    return false;
                }

                if (remoteFileSize > 50 * 1024 * 1024) {
                    logger.warn("file over limite!(" + remoteFileSize + ")");
                    response.setStatus(HttpResponseStatus.BAD_REQUEST);
                    response.setContent("file over limite!");
                    return false;
                }



                String remoteFileExt = "";
                if (remoteFileName.lastIndexOf(".") == -1) {
                    remoteFileExt = "octetstream";
                    remoteFileName = remoteFileName + "." + remoteFileExt;

                } else {
                    remoteFileExt = getFileExt(remoteFileName);
                }

                if (StringUtil.isNullOrEmpty(remoteFileExt) || remoteFileExt.equals("ing")) {
                    logger.warn("Invalid file extention name");
                    response.setStatus(HttpResponseStatus.BAD_REQUEST);
                    response.setContent("Invalid file extention name");
                    return false;
                }

                int remoteFileTotalSize = (int) remoteFileSize;


                ByteBuf byteBuf = null;
                int savedThunkSize = 0; // 分片接收保存的大小
                int offset = 0; // 断点续传开始位置

                Date nowTime=new Date();
                SimpleDateFormat time=new SimpleDateFormat("yyyy/MM/dd/HH");
                String datePath = time.format(nowTime);

                datePath = "fs/" + bucket[0] + "/" + datePath; //add bucket

                String dir = "./" + MediaServerConfig.FILE_STROAGE_ROOT + "/" + datePath;

                File dirFile = new File(dir);
                boolean bFile  = dirFile.exists();

                if(!bFile) {
                    bFile = dirFile.mkdirs();
                    if (!bFile) {
                        response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        response.setContent("服务器错误：无法创建文件");
                        return false;
                    }
                }


                String filePath = dir + "/" + requestId;
                logger.info("the file path is " + filePath);

                File tmpFile = new File(filePath);

                logger.info("before write the file");
                boolean isError = false;
                while (true) {
                    byte[] thunkData;
                    try {
                        byteBuf = fileUpload.getChunk(128 * 1024);
                        int readableBytesSize = byteBuf.readableBytes();
                        thunkData = new byte[readableBytesSize];
                        byteBuf.readBytes(thunkData);

                        put(tmpFile, offset, thunkData);

                        savedThunkSize += readableBytesSize;
                        offset += readableBytesSize;

                        if (savedThunkSize >= remoteFileSize) {
                            byteBuf.release();
                            fileUpload.release();

                            response.setStatus(HttpResponseStatus.OK);
                            String relativePath = datePath + "/" +  requestId;
                            response.setContent("{\"key\":\"" + relativePath + "\"}");
                            break;
                        }
                    } catch (Exception e) {
                        logger.error("save thunckData error!", e);
                        if (fileUpload != null)
                            fileUpload.release();

                        if (byteBuf != null)
                            byteBuf.release();

                        response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        response.setContent("服务器错误：" + e.getMessage());
                        isError = true;

                        return false;
                    } finally {
                        thunkData = null;
                        if (isError) {
                            tmpFile.delete();
                        }
                    }
                }
            } else if(data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                Attribute attribute = (Attribute)data;
                if(attribute.getName().equals("token")) {
                    String token = attribute.getValue();

                    try {
                        bucket[0] = validateToken(token);
                    } catch (InvalidateTokenExecption e) {
                        logger.error("无效的token!", e);
                        response.setStatus(HttpResponseStatus.BAD_REQUEST);
                        response.setContent("无效的token：" + e.getMessage());
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("writeHttpData error!", e);
            response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            response.setContent("服务器错误：" + e.getMessage());
            return false;
        }
        return true;
    }

    public static void put(File file, long pos, byte[] data) throws Exception {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rwd");
            raf.seek(pos);
            raf.write(data);
        } finally {
            try {
                if (raf != null)
                    raf.close();
            } catch (Exception e) {
                logger.warn("release error!", e);
            }
        }
    }
}
