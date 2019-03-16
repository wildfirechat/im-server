package com.xiaoleilu.loServer.handler;

import io.netty.util.internal.StringUtil;
import io.moquette.server.config.MediaServerConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.UUID;

import static com.xiaoleilu.loServer.handler.HttpResponseHelper.getFileExt;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpFileServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(HttpFileServerHandler.class);
    private static final HttpDataFactory factory = new DefaultHttpDataFactory(false);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof FullHttpRequest) {
                FullHttpRequest request = (FullHttpRequest) msg;
                String requestId = UUID.randomUUID().toString().replace("-", "");
                logger.info("HttpFileServerHandler received a request: method=" + request.getMethod() + ", uri=" + request.getUri() + ", requestId=" + requestId);

                if (!request.getDecoderResult().isSuccess()) {
                    logger.warn("http decode failed!");
                    HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST);
                    return;
                }

                if (request.getMethod() == HttpMethod.GET) {
                    download(ctx, request, requestId);
                } else if (request.getMethod() == HttpMethod.POST) {
                    multipartUpload(ctx, request, requestId);
                } else if (request.getMethod() == HttpMethod.DELETE) {
                    delete(ctx, request, requestId);
                } else if (request.getMethod() == HttpMethod.OPTIONS) {
                    doOptions(ctx, request);
                } else {
                    logger.warn("METHOD_NOT_ALLOWED!(methodName=" + request.getMethod().name() + ")");
                    HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
                }
            } else {
                HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "Not a http request");
            }
        } catch (Exception e) {
            logger.error("HttpFileServerHandler.channelRead0() process error!", e);
            HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            if (ctx.channel().isActive()) {
                HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                logger.warn("exceptionCaught:" + cause.getMessage());
            }
        } catch (Exception e) {
            logger.warn("exceptionCaught error!" + e.getMessage());
        }
    }


    /**
     * multipart上传
     */
    private void multipartUpload(ChannelHandlerContext ctx, FullHttpRequest request, String requestId) {
        HttpPostRequestDecoder decoder = null;
        try {
            decoder = new HttpPostRequestDecoder(factory, request);
        } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
            logger.error("Failed to decode file data!", e1);
            HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "Failed to decode file data!");
            return;
        }


        if (decoder != null) {
            if (request instanceof HttpContent) {
                HttpContent chunk = (HttpContent) request;
                try {
                    decoder.offer(chunk);
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
                    logger.warn("BAD_REQUEST, Failed to decode file data");
                    HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "Failed to decode file data");
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
                
                readHttpDataChunkByChunk(ctx, decoder, requestId, HttpHeaders.isKeepAlive(request));

                if (chunk instanceof LastHttpContent) {
                    releaseRequest(request, decoder);
                }
            } else {
                logger.warn("BAD_REQUEST, Not a http request");
                HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "Not a http request");
            }
        }
    }

    /**
     * readHttpDataChunkByChunk
     */
    private void readHttpDataChunkByChunk(ChannelHandlerContext ctx, HttpPostRequestDecoder decoder, String requestId, boolean isKeepAlive) {
        try {
            while (decoder.hasNext()) {
                InterfaceHttpData data = decoder.next();
                if (data != null) {
                    try {
                        writeFileUploadData(data, ctx, requestId, isKeepAlive);
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
    private void writeFileUploadData(InterfaceHttpData data, ChannelHandlerContext ctx, String requestId, boolean isKeepAlive) {
        try {
            if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                FileUpload fileUpload = (FileUpload) data;

                String remoteFileName = fileUpload.getFilename();
                long remoteFileSize = fileUpload.length();
                if (StringUtil.isNullOrEmpty(remoteFileName)) {
                    logger.warn("remoteFileName is empty!");
                    HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "file name is empty");
                    return;
                }

                if (StringUtil.isNullOrEmpty(requestId)) {
                    logger.warn("requestId is empty!");
                    HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "requestId is empty!");
                    return;
                }

                if (remoteFileSize > 10 * 1024 * 1024) {
                    logger.warn("file over limite!(" + remoteFileSize + ")");
                    HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "file over limite!");
                    return;
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
                    HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "Invalid file extention name");
                    return;
                }

                int remoteFileTotalSize = (int) remoteFileSize;

                HttpFileServerController.getInstance().mapChannelHandlerContext(requestId, ctx);

                ByteBuf byteBuf = null;
                int savedThunkSize = 0; // 分片接收保存的大小
                int offset = 0; // 断点续传开始位置

                File tmpFile = new File("./" + MediaServerConfig.FILE_STROAGE_ROOT + "/" + requestId);

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
                            HttpResponseHelper.sendResponse(requestId, ctx, HttpResponseStatus.OK, HttpHeaders.Values.APPLICATION_JSON, "{\"key\":\"" + requestId + "\"}", true, false);
                            break;
                        }
                    } catch (Exception e) {
                        logger.error("save thunckData error!", e);
                        if (fileUpload != null)
                            fileUpload.release();

                        if (byteBuf != null)
                            byteBuf.release();

                        HttpResponseHelper.sendResponse(requestId, ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        isError = true;

                        return;
                    } finally {
                        thunkData = null;
                        if (isError) {
                            tmpFile.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("writeHttpData error!", e);
            HttpResponseHelper.sendResponse(requestId, ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
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

    /**
     * 下载
     */
    private void download(ChannelHandlerContext ctx, FullHttpRequest request, String requestId) {
//        try {
//            final String httpUri = request.getUri();
//            if (httpUri.toLowerCase().equals("/favicon.ico")) {
//                logger.warn("invalid httpUri(" + httpUri + ", requestId=" + requestId + ")");
//                HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "invalid httpUri");
//                return;
//            }
//
//            String typedDownloadInfo = MetaDataIndexHandler.getTypedDownloadInfo(httpUri);
//            if (StringUtil.isNullOrEmpty(typedDownloadInfo)) {
//                logger.warn("invalid httpUri(" + httpUri + ", requestId=" + requestId + ")");
//                HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "invalid httpUri");
//                return;
//            }
//
//            MetaDataIndex metadataIndex = MetaDataIndexHandler.parseTypedDownloadInfo(typedDownloadInfo);
//            if (metadataIndex == null) {
//                logger.warn("invalid typedDownloadInfo!(" + typedDownloadInfo + ", requestId=" + requestId + ")");
//                HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "invalid downloadFileName");
//                return;
//            }
//
//            int fileSize = MetaDataHandler.getDataFieldLength(metadataIndex);
//            int chunkSize = FileStorageConst.Stroage_Remote_Invoke_Thunk_Size;
//            int chunkCount = fileSize % chunkSize == 0 ? (int) (fileSize / chunkSize) : (int) (fileSize / chunkSize + 1);
//
//            ClusterNode toNode = StorageClusterRouterManager.getInstance().getDownloadNode(metadataIndex.getClusterNode());
//            if(toNode == null) {
//                logger.warn("toNode is null!(" + typedDownloadInfo + ", requestId=" + requestId + ")");
//                HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.NOT_FOUND, "File Not Existed");
//                return;
//            }
//
//            ClusterNode fromNode = StorageClusterRouterManager.getInstance().getSelfNode();
//            ActorSystem actorSystem = StorageClusterRouterManager.getInstance().getStorageActorSystem();
//            ActorRef toActorRef = actorSystem.actorOf(toNode.getIp(), toNode.getRpcPort(), ActorType.DownloadActor.getName());
//            ActorRef fromActorRef = actorSystem.actorOf(fromNode.getIp(),fromNode.getRpcPort(), ActorType.HttpFileServerActor.getName());
//
//            HttpFileServerController.getInstance().mapChannelHandlerContext(requestId, ctx);
//
//            DownloadMsg downloadMsg = new DownloadMsg();
//            downloadMsg.setRequestId(requestId);
//            downloadMsg.setIsKeepAlive(HttpHeaders.isKeepAlive(request));
//            downloadMsg.setClusterNode(metadataIndex.getClusterNode());
//            downloadMsg.setFileSize(fileSize);
//            downloadMsg.setChunkIndex(0);
//            downloadMsg.setChunkSize(chunkSize);
//            downloadMsg.setChunkCount(chunkCount);
//            downloadMsg.setTime(metadataIndex.getTime());
//            downloadMsg.setBigFileIndex(metadataIndex.getBigFileIndex());
//            downloadMsg.setOffset(metadataIndex.getOffset());
//            downloadMsg.setMetaDataTotalLength(metadataIndex.getMetaDataTotalLength());
//            downloadMsg.setFileType(metadataIndex.getFileType());
//            downloadMsg.setBigFilePath(metadataIndex.getBigFilePath());
//            downloadMsg.setStorageEngineVersion(metadataIndex.getStorageEngineVersion());
//            toActorRef.tell(downloadMsg, fromActorRef);
//        } catch (Exception e) {
//            logger.error("download error!", e);
//            HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
//        } finally {
//            releaseRequest(request, null);
//        }
    }

    /**
     * 删除
     */
    private void delete(ChannelHandlerContext ctx, FullHttpRequest request, String requestId) {
//        try {
//            final String httpUri = request.getUri();
//            String typedDownloadInfo = MetaDataIndexHandler.getTypedDownloadInfo(httpUri);
//            if (StringUtil.isNullOrEmpty(typedDownloadInfo)) {
//                logger.warn("invalid httpUri(" + httpUri + ")");
//                HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "invalid httpUri");
//                return;
//            }
//
//            MetaDataIndex metadataIndex = MetaDataIndexHandler.parseTypedDownloadInfo(typedDownloadInfo);
//            if (metadataIndex == null) {
//                logger.warn("invalid typedDownloadInfo!(" + typedDownloadInfo + ")");
//                HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.BAD_REQUEST, "invalid downloadFileName");
//            }
//
//            ClusterNode toNode = StorageClusterRouterManager.getInstance().getClusterNode(metadataIndex.getClusterNode());
//            ClusterNode fromNode = StorageClusterRouterManager.getInstance().getSelfNode();
//            ActorSystem actorSystem = StorageClusterRouterManager.getInstance().getStorageActorSystem();
//            ActorRef toActorRef = actorSystem.actorOf(toNode.getIp(), toNode.getRpcPort(), ActorType.DeleteActor.getName());
//            ActorRef fromActorRef = actorSystem.actorOf(fromNode.getIp(),fromNode.getRpcPort(), ActorType.HttpFileServerActor.getName());
//
//            HttpFileServerController.getInstance().mapChannelHandlerContext(requestId, ctx);
//
//            DeleteMsg downloadMsg = new DeleteMsg();
//            downloadMsg.setRequestId(requestId);
//            downloadMsg.setIsKeepAlive(HttpHeaders.isKeepAlive(request));
//            downloadMsg.setClusterNode(metadataIndex.getClusterNode());
//            downloadMsg.setTime(metadataIndex.getTime());
//            downloadMsg.setBigFileIndex(metadataIndex.getBigFileIndex());
//            downloadMsg.setOffset(metadataIndex.getOffset());
//            downloadMsg.setMetaDataTotalLength(metadataIndex.getMetaDataTotalLength());
//            downloadMsg.setFileType(metadataIndex.getFileType());
//            downloadMsg.setBigFilePath(metadataIndex.getBigFilePath());
//            downloadMsg.setStorageEngineVersion(metadataIndex.getStorageEngineVersion());
//            toActorRef.tell(downloadMsg, fromActorRef);
//        } catch (Exception e) {
//            logger.error("delete error!", e);
//            HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
//        } finally {
//            releaseRequest(request, null);
//        }
    }

    /**
     * HTTP能力探测
     */
    private void doOptions(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            boolean isKeepAlive = HttpHeaders.isKeepAlive(request);
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NO_CONTENT);
            response.headers().set(CONTENT_TYPE, HttpHeaders.Values.APPLICATION_JSON);
            response.headers().set(TRANSFER_ENCODING, "chunked");
            if (isKeepAlive)
                response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.headers().set(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, DELETE, OPTIONS");
            response.headers().set(ACCESS_CONTROL_ALLOW_HEADERS, "DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Authorization");

            ChannelFuture future = ctx.channel().writeAndFlush(response);
            if (!isKeepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (Exception e) {
            logger.error("doOptions error!", e);
            HttpResponseHelper.sendResponse("", ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        } finally {
            releaseRequest(request, null);
        }
    }

    /**
     * releaseRequest
     */
    private void releaseRequest(FullHttpRequest request, HttpPostRequestDecoder decoder) {
        try {
            if (request != null)
                request.release();

            request = null;

            if (decoder != null)
                decoder.destroy();

            decoder = null;
        } catch (Exception e) {
            logger.error("reset error!", e);
        }
    }
}
