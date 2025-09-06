package rpc.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import rpc.EchoRequest;
import rpc.EchoResponse;
import rpc.RpcRequest;
import rpc.RpcResponse;
import rpc.common.Constants;
import rpc.server.service.EchoServiceImpl;

public class ServerHandler extends SimpleChannelInboundHandler<byte[]> {

    private final EchoServiceImpl echoService = new EchoServiceImpl();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
        RpcRequest req = RpcRequest.parseFrom(msg);
        RpcResponse.Builder respBuilder = RpcResponse.newBuilder()
                        .setRequestId(req.getRequestId());

        System.out.println("[server.ServerHandler] received req " + req.getPayload());

        try {
            if (Constants.ECHO_SERVICE_NAME.equals(req.getService()) && Constants.ECHO_SERVICE_METHOD.equals(req.getMethod())) {
//                System.out.println("[server.ServerHandler] matched service and method");
                EchoRequest echoReq = EchoRequest.parseFrom(req.getPayload());
                EchoResponse echoResp = echoService.echo(echoReq);
                respBuilder.setSuccess(true).setPayload(echoResp.toByteString());
            } else {
                System.out.println("[server.ServerHandler] ERROR: unknown service/method");
                respBuilder.setSuccess(false).setError("Unknown service/method");
            }
        } catch (Exception e) {
            System.out.println("[server.ServerHandler] ERROR: some error" + e.getMessage());
            respBuilder.setSuccess(false).setError(e.getMessage());
        }

//        System.out.println("[server.ServerHandler] write and flush to ctx");
        ctx.writeAndFlush(respBuilder.build().toByteArray()).addListener(f -> {
            if (!f.isSuccess()) f.cause().printStackTrace();
        });
    }
}
