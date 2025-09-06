package rpc.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import rpc.common.FrameDecoder;
import rpc.common.FrameEncoder;

public class RpcServer {

    private final int port;
    EventLoopGroup boss;
    EventLoopGroup worker;

    public RpcServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        boss = new NioEventLoopGroup(1);
        worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast("ServerFrameDecoder", new FrameDecoder());
                            ch.pipeline().addLast("ServerFrameEncoder", new FrameEncoder());
                            ch.pipeline().addLast(new ServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = bootstrap.bind(port).sync();
            System.out.println("[server.RpcServer] server started on port " + port);
            f.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    public void stop() throws InterruptedException {
        if (boss != null) boss.shutdownGracefully().sync();
        if (worker != null) worker.shutdownGracefully().sync();
        System.out.println("[server.RpcServer] server stopped.");
    }
}
