package com.xiaoleilu.loServer;

import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.StaticLog;
import com.xiaoleilu.hutool.util.DateUtil;
import com.xiaoleilu.loServer.action.Action;
import com.xiaoleilu.loServer.action.ClassUtil;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.ActionHandler;
import com.xiaoleilu.loServer.handler.HttpFileServerHandler;
import io.moquette.spi.IMessagesStore;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * LoServer starter<br>
 * 用于启动服务器的主对象<br>
 * 使用LoServer.start()启动服务器<br>
 * 服务的Action类和端口等设置在ServerSetting中设置
 * @author Looly
 *
 */
public class LoFileServer {
    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(LoFileServer.class);
	private int port;
    private IMessagesStore messagesStore;
    private Channel channel;

    public LoFileServer(int port, IMessagesStore messagesStore) {
        this.port = port;
        this.messagesStore = messagesStore;
    }

    /**
	 * 启动服务
	 * @throws InterruptedException 
	 */
	public void start() throws InterruptedException {
		long start = System.currentTimeMillis();
		
		// Configure the server.
		final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		final EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			final ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024) // 服务端可连接队列大小
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_SNDBUF, 1024*1024*10)
                .option(ChannelOption.SO_RCVBUF, 1024*1024*10)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new HttpRequestDecoder());
                        socketChannel.pipeline().addLast(new HttpResponseEncoder());
                        socketChannel.pipeline().addLast(new ChunkedWriteHandler());
                        socketChannel.pipeline().addLast(new HttpObjectAggregator(100 * 1024 * 1024));
                        socketChannel.pipeline().addLast(new HttpFileServerHandler());
                    }
                });
			
			channel = b.bind(port).sync().channel();
			Logger.info("***** Welcome To LoServer on port [{}], startting spend {}ms *****", port, DateUtil.spendMs(start));
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("端口 {} 已经被占用。请检查该端口被那个程序占用，找到程序停掉。\n查找端口被那个程序占用的命令是: netstat -tunlp | grep {}", port, port);
            System.out.println("端口 " + port + " 已经被占用。请检查该端口被那个程序占用，找到程序停掉。\n查找端口被那个程序占用的命令是: netstat -tunlp | grep " + port);
            System.exit(-1);
        }
	}
    public void shutdown() {
        if (this.channel != null) {
            this.channel.close();
            try {
                this.channel.closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
