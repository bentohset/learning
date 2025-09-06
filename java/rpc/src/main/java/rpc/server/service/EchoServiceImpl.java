package rpc.server.service;

import rpc.EchoRequest;
import rpc.EchoResponse;

public class EchoServiceImpl {

    public EchoResponse echo(EchoRequest req) {
        return EchoResponse.newBuilder()
                .setMessage("Echo: " + req.getMessage())
                .build();
    }

}
