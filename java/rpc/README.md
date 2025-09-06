# Java RPC Framework

Compile proto before running demo

```bash
bash ./compile-proto.sh
```

## Pre-requisites

- protobuf

## Process Flow

**Sample log:**

```text
[server.RpcServer] server started on port 8888
[client.serviceEchoServiceStub] echoing: Helloworld
[client.RpcClient] sending request...
[client.ClientHandler] sending request
[ClientFrameEncoder] encoding 35
[ServerFrameDecoder] decode begin
[server.ServerHandler] received req <ByteString@257c9289 size=12 contents="\n\nHelloworld">
[ServerFrameEncoder] encoding 24
[ClientFrameDecoder] decode begin
Stub client received response: Echo: Helloworld
FINISHED, cleaning up resources...
[server.RpcServer] server stopped.
[server.RpcServer] server stopped.
```

**Flow:**
1. Client sends a request
2. ClientHandler receives requests and puts into pipeline
3. Client pipeline encodes the request
    - netty calls FrameDecoder in server thread as soon as enough bytes arrive
4. Server receives TCP packets and decodes frames
5. ServerHandler processes the decoded packet and builds a response
6. Server encodes the response
    - netty sends TCP response back to client
7. Client thread receives and triggers the Decoder in client pipeline
8. CompletableFuture that is being tracked is completed

```text
Time ─────────────────────────────────────────────►

Client Thread               Server Thread
  │                             │
  │ echo("Helloworld")          │
  │---------------------------->│
  │ ClientHandler.sendRequest() │
  │ [client.ClientHandler]      │
  │                             │
  │ FrameEncoder encodes 35     │
  │ [ClientFrameEncoder]        │
  │---------------------------->│ TCP send
  │                             │
  │                             │ FrameDecoder receives
  │                             │ [ServerFrameDecoder] decode begin
  │                             │
  │                             │ ServerHandler reads request
  │                             │ [server.ServerHandler] received req
  │                             │
  │                             │ ServerHandler builds response
  │                             │
  │                             │ FrameEncoder encodes 24
  │                             │ [ServerFrameEncoder]
  │                             │
  │ <---------------------------│ TCP send back response
  │                             │
  │ ClientFrameDecoder receives │
  │ [ClientFrameDecoder] decode │
  │ ClientHandler future completes
  │ Stub prints response        │
  │ "Echo: Helloworld"          │
  │                             │
Cleanup/shutdown               │

```


## Pipeline

Order matters for `initChannel()` but is per inbound or outbound
- outbound: called in reverse order, handler then encoder
- inbound: decoder first then handler

Decoder - inbound only and decodes responses from server
Encoder - outbound only and encodes requests to be sent
Client/ServerHandler - inbound

**Client:**

Bootstrap only initiates connection and has no acceptor socket. 
Handler is a pipeline for a single client socket.

Outbound: send request to server
- ClientHandler.sendRequest --> Encode

Inbound: read responses from server. 
- Decode --> ClientHandler.channelRead0

```java
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

```

**Server:**

ServerBootstrap accepts connection.
ChildHandler is a pipeline for each accepted client socket.

Inbound: read bytes from socket
- Decode --> ServerHandler.channelRead0

Outbound: write bytes to socket
- ServerHandler.write --> Encode

```java
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
```