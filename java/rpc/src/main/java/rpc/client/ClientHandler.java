package rpc.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import rpc.RpcRequest;
import rpc.RpcResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClientHandler extends SimpleChannelInboundHandler<byte[]> {

    // hashcode resp : future map of currently inflight RPC requests
    private final ConcurrentMap<Long, CompletableFuture<RpcResponse>> inflight = new ConcurrentHashMap<>();

    // channel the handler is connected to, accepts requests and sends
    private Channel channel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.channel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
        RpcResponse resp = RpcResponse.parseFrom(msg);
        long reqId = resp.getRequestId();
        CompletableFuture<RpcResponse> future = inflight.remove(reqId);
        if (future != null) {
            future.complete(resp);
        }
    }

    // completableFuture for async sending
    public CompletableFuture<RpcResponse> sendRequest(RpcRequest request, long requestId) {
        System.out.println("[client.ClientHandler] sending request");
        if (channel == null || !channel.isActive()) {
            CompletableFuture<RpcResponse> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("Channel not active"));
            System.out.println("[client.ClientHandler] channel not active");
            return failed;
        }
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        inflight.put(requestId, future);

        // send data to pipeline
        channel.writeAndFlush(request.toByteArray()).addListener(f -> {
            if (!f.isSuccess()) {
                inflight.remove(requestId);
                future.completeExceptionally(f.cause());
            }
        });
//        System.out.println("client.ClientHandler flushed to channel");

        return future;
    }

}
