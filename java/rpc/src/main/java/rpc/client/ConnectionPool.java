package rpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import rpc.common.FrameDecoder;
import rpc.common.FrameEncoder;

import java.util.concurrent.ArrayBlockingQueue;

public class ConnectionPool {

    private final ArrayBlockingQueue<Channel> pool;
    private final EventLoopGroup group;

    public ConnectionPool(String host, int port, int poolSize) throws InterruptedException {
        pool = new ArrayBlockingQueue<>(poolSize);
        group = new NioEventLoopGroup(poolSize);

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast("ClientFrameDecoder", new FrameDecoder());
                        ch.pipeline().addLast("ClientFrameEncoder", new FrameEncoder());
                        ch.pipeline().addLast(new ClientHandler());
                    }
                });

        for (int i = 0; i < poolSize; i++) {
            Channel ch = bootstrap.connect(host, port).sync().channel();
            pool.offer(ch);
        }
    }

    public Channel acquire() throws InterruptedException {
        return pool.take();
    }

    public void release(Channel channel) {
        pool.offer(channel);
    }

    public void shutdown() throws InterruptedException {
        for (Channel ch : pool) {
            if (ch.isActive()) ch.close().sync();
        }
        group.shutdownGracefully().sync(); // shutdown Netty EventLoopGroup
    }

}
