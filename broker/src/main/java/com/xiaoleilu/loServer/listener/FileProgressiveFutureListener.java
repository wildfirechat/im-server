package com.xiaoleilu.loServer.listener;

import java.io.RandomAccessFile;

import com.xiaoleilu.hutool.io.IoUtil;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.StaticLog;

import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import org.slf4j.LoggerFactory;

/**
 * 文件进度指示监听
 * @author Looly
 *
 */
public class FileProgressiveFutureListener implements ChannelProgressiveFutureListener{
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(FileProgressiveFutureListener.class);
	
	private RandomAccessFile raf;
	
	public FileProgressiveFutureListener(RandomAccessFile raf) {
		this.raf = raf;
	}

	@Override
	public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
        Logger.debug("Transfer progress: {} / {}", progress, total);
	}

	@Override
	public void operationComplete(ChannelProgressiveFuture future) {
		IoUtil.close(raf);
        Logger.debug("Transfer complete.");
	}

	/**
	 * 构建文件进度指示监听
	 * @param raf RandomAccessFile
	 * @return 文件进度指示监听
	 */
	public static FileProgressiveFutureListener build(RandomAccessFile raf){
		return new FileProgressiveFutureListener(raf);
	}
}
