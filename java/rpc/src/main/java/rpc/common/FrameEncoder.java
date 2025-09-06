package rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class FrameEncoder extends MessageToByteEncoder<byte[]> {

    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out) {
        System.out.println("[" + ctx.name() + "] encoding " + msg.length);
        out.writeInt(msg.length);
        out.writeBytes(msg);
    }

}
