package com.xiaoleilu.loServer.handler;

import com.xiaoleilu.hutool.log.StaticLog;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContentCompressor;

/**
 * 解决大文件传输与Gzip压缩冲突问题
 * @author Looly
 *
 */
public class HttpChunkContentCompressor extends HttpContentCompressor {
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		StaticLog.debug("Write object [{}]", msg);
		
		if (msg instanceof FileRegion || msg instanceof DefaultHttpResponse) {
			//文件传输不经过Gzip压缩
			ctx.write(msg, promise);
		}else{
			super.write(ctx, msg, promise);
		}
	}
}
