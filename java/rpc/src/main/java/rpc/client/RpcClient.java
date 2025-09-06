package rpc.client;

import com.google.protobuf.ByteString;
import io.netty.channel.Channel;
import rpc.RpcRequest;
import rpc.RpcResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class RpcClient {

    private final ConnectionPool pool;
    private final AtomicLong requestIdGen = new AtomicLong(1);

    public RpcClient(ConnectionPool pool) {
        this.pool = pool;
    }

    public RpcResponse send(String service, String method, ByteString payload) throws Exception {
        System.out.println("[client.RpcClient] sending request...");
        long requestId = requestIdGen.getAndIncrement();
        RpcRequest req = RpcRequest.newBuilder()
                .setRequestId(requestId)
                .setService(service)
                .setMethod(method)
                .setPayload(payload)
                .build();

        Channel ch = pool.acquire();
        try {
            ClientHandler handler = ch.pipeline().get(ClientHandler.class);
            CompletableFuture<RpcResponse> future = handler.sendRequest(req, requestId);

            RpcResponse resp = future.get();
//            System.out.println("client.RpcClient received future.get: " + resp);

            return resp;
        } finally {
            pool.release(ch);
        }
    }

    public CompletableFuture<RpcResponse> sendAsync(RpcRequest req) throws Exception {
        Channel ch = pool.acquire();
        ClientHandler handler = ch.pipeline().get(ClientHandler.class);
        long requestId = requestIdGen.getAndIncrement();
        CompletableFuture<RpcResponse> future = handler.sendRequest(req, requestId);
        pool.release(ch);
        return future;
    }

}
