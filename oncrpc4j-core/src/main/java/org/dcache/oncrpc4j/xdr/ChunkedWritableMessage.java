package org.dcache.oncrpc4j.xdr;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import org.dcache.oncrpc4j.rpc.RpcMessageParserTCP;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.FileChunk;
import org.glassfish.grizzly.asyncqueue.WritableMessage;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.nio.transport.TCPNIOConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOUtils;

/**
 * A {@link WritableMessage} that allows multiple chunks as well as {@code sendTo} via {@link FileChunk}.
 * 
 * @author Christian Kohlschütter
 */
public class ChunkedWritableMessage implements FileChunk, Closeable {
    private final boolean nioConn;
    private final List<WritableMessage> chunks;
    private int remaining = 0;
    private boolean streaming;
    private final Connection<InetSocketAddress> connection;

    public ChunkedWritableMessage(Connection<InetSocketAddress> connection, List<WritableMessage> messageChunks,
            boolean streaming) {
        this.connection = connection;
        this.chunks = messageChunks;
        this.streaming = streaming;
        for (WritableMessage wm : messageChunks) {
            remaining += wm.remaining();
        }

        nioConn = (((Connection<?>) connection) instanceof TCPNIOConnection);
    }

    @Override
    public boolean hasRemaining() {
        return remaining > 0;
    }

    @Override
    public int remaining() {
        return remaining;
    }

    @Override
    public boolean release() {
        boolean ok = true;
        for (WritableMessage msg : chunks) {
            ok &= msg.release();
        }
        return ok;
    }

    @Override
    public boolean isExternal() {
        return true;
    }

    @Override
    public long writeTo(WritableByteChannel c) throws IOException {
        long written = 0;
        if (remaining <= 0) {
            return -1;
        }

        try {
            if (streaming) {
                ByteBuffer bb = ByteBuffer.allocate(4);
                bb.putInt(remaining | RpcMessageParserTCP.RPC_LAST_FRAG);
                bb.flip();

                while (bb.hasRemaining()) {
                    c.write(bb);
                }
                streaming = false;
            }

            for (WritableMessage msg : chunks) {
                if (!msg.hasRemaining()) {
                    continue;
                }

                if (msg instanceof FileChunk) {
                    long w = ((FileChunk) msg).writeTo(c);
                    written += w;
                    remaining -= w;
                    continue;
                }

                Buffer buffer = (Buffer) msg;

                if (nioConn) {
                    TCPNIOConnection conn = (TCPNIOConnection) (Connection<?>) connection;
                    while (buffer.hasRemaining()) {
                        int wri = buffer.isComposite() ? TCPNIOUtils.writeCompositeBuffer(
                                conn, (CompositeBuffer) buffer)
                                : TCPNIOUtils.writeSimpleBuffer(conn, buffer);
                        written += wri;
                        remaining -= wri;
                    }
                } else {
                    ByteBuffer bb = buffer.toByteBuffer().slice();
                    int w;
                    while (bb.hasRemaining()) {
                        w = c.write(bb);
                        written += w;
                        remaining -= w;
                    }
                }
            }

            chunks.clear();

            return written;
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            remaining = 0;
            chunks.clear();
        }
    }

    @Override
    public void close() throws IOException {
        for (WritableMessage wm : chunks) {
            if (wm instanceof Closeable) {
                ((Closeable) wm).close();
            }
        }
        chunks.clear();
    }
}
