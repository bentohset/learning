package rpc.client.service;

import rpc.EchoRequest;
import rpc.EchoResponse;
import rpc.RpcRequest;
import rpc.RpcResponse;
import rpc.client.RpcClient;
import rpc.common.Constants;

public class EchoServiceStub {

    private final RpcClient client;

    public EchoServiceStub(RpcClient client) {
        this.client = client;
    }

    public EchoResponse echo(String msg) throws Exception {
        System.out.println("[client.serviceEchoServiceStub] echoing: " + msg);
        EchoRequest request = EchoRequest.newBuilder().setMessage(msg).build();

//        System.out.println("[client.serviceEchoServiceStub] sent req to client ");
        RpcResponse resp = client.send(Constants.ECHO_SERVICE_NAME, Constants.ECHO_SERVICE_METHOD, request.toByteString());

//        System.out.println("[client.serviceEchoServiceStub] client resp: " + resp);
        if (!resp.getSuccess()) {
            System.out.println("[client.serviceEchoServiceStub] ERROR: " + resp.getError());
            throw new RuntimeException(resp.getError());
        }
        return EchoResponse.parseFrom(resp.getPayload());
    }

}
