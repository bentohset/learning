package rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.Arrays;
import java.util.List;

public class FrameDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        System.out.println(name(ctx) + " decode begin");
        if (in.readableBytes() < 4) {
            // min length is 4
            System.out.println(name(ctx) + " Does not meet min length to decode");
            return;
        }
        in.markReaderIndex();
        int len = in.readInt();
        if (in.readableBytes() < len) {
            System.out.println(name(ctx) + " Less bytes than len " + len);
            in.resetReaderIndex();
            return;
        }
        byte[] bytes = new byte[len];
//        System.out.println(name(ctx) + " decode done");
        in.readBytes(bytes);
        out.add(bytes);
    }

    /**
     * For debug information on who is calling this
     * @return String Server or
     */
    private boolean isServer(ChannelHandlerContext ctx) {
        return ctx.channel().parent() != null;
    }

    private String name(ChannelHandlerContext ctx) {
        return "[" + ctx.name() + "]";
    }
}
