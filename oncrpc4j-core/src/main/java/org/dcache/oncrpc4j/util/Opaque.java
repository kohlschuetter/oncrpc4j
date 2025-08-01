/*
 * Copyright (c) 2009 - 2020 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.oncrpc4j.util;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import org.glassfish.grizzly.Buffer;

import com.google.common.io.BaseEncoding;

/**
 * Describes something that can be used as a key for {@link java.util.HashMap} and that can be converted to a
 * {@code byte[]} and a Base64 string representation.
 * <p>
 * Note that {@link Opaque}s that are <em>stored</em> in {@link java.util.HashMap} need to be immutable. Call
 * {@link #toImmutableOpaque()} when necessary (e.g., when using {@link java.util.HashMap#put(Object, Object)},
 * {@link java.util.HashMap#computeIfAbsent(Object, java.util.function.Function)}, etc.
 */
public interface Opaque {
    static final Opaque EMPTY_OPAQUE = new OpaqueImpl(new byte[0]) {

        @Override
        public byte[] toBytes() {
            return _opaque;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public String toBase64() {
            return "";
        }

        @Override
        public int numBytes() {
            return 0;
        }
    };

    /**
     * Returns an {@link Opaque}, encoding the given String using UTF-8.
     * 
     * @param s The string.
     * @return The Opaque.
     */
    static Opaque forUtf8Bytes(String s) {
        if (s.isEmpty()) {
            return EMPTY_OPAQUE;
        }
        return forImmutableBytes(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns a {@link Opaque} for a number of zero bytes.
     * 
     * @param num The number of zero bytes.
     * @return The Opaque.
     */
    static Opaque forNZeroBytes(int num) {
        return new OpaqueZero(num);
    }

    /**
     * Returns an immutable {@link Opaque} instance based on a copy of the given bytes.
     * 
     * @param bytes The bytes.
     * @return The {@link Opaque} instance.
     */
    static Opaque forBytes(byte[] bytes) {
        if (bytes.length == 0) {
            return EMPTY_OPAQUE;
        }
        return new OpaqueImmutableImpl(bytes.clone());
    }

    /**
     * Returns an {@link Opaque} instance based the given byte array <em>by reference</em>.
     * <p>
     * Note that this is assuming that the byte array does not change afterwards outside the scope of {@link Opaque}, it
     * is assumed that the caller relinquishes ownership of the array.
     * 
     * @param bytes The bytes.
     * @return The {@link Opaque} instance.
     */
    static Opaque forImmutableBytes(byte[] bytes) {
        if (bytes.length == 0) {
            return EMPTY_OPAQUE;
        }
        return new OpaqueImmutableImpl(bytes);
    }

    /**
     * Returns an mutable {@link Opaque} instance based on the given byte array.
     * <p>
     * Note that the returned {@link Opaque} is typically not suitable for <em>storing</em> in a
     * {@link java.util.HashMap}, but merely for lookups. Call {@link #toImmutableOpaque()} when necessary.
     * 
     * @param bytes The bytes.
     * @return The {@link Opaque} instance.
     */
    static Opaque forMutableByteArray(byte[] bytes) {
        if (bytes.length == 0) {
            return EMPTY_OPAQUE;
        }
        return new OpaqueImpl(bytes);
    }

    /**
     * Returns an immutable {@link Opaque} instance based on a copy of the {@code length} bytes from the given
     * {@link ByteBuffer}, starting at the current position for the remaining bytes.
     * 
     * @param buf The buffer.
     * @param length The number of bytes.
     * @return The {@link Opaque} instance.
     */
    static Opaque forBytes(ByteBuffer buf, int length) {
        if (length == 0 || buf.remaining() == 0) {
            return EMPTY_OPAQUE;
        }
        byte[] bytes = new byte[length];
        buf.get(bytes);

        return new OpaqueImmutableImpl(bytes);
    }

    /**
     * Returns an {@link Opaque} instance backed on the byte contents of the given {@link ByteBuffer}, for the given
     * number of bytes starting from the given absolute index.
     * <p>
     * It is assumed that the given {@link ByteBuffer}'s contents are valid and unchanged for the entire lifetime of the
     * returned {@link Opaque}; position and limit may be changed by the returned {@link Opaque}.
     * <p>
     * Note that the returned {@link Opaque} is typically not suitable for <em>storing</em> in a
     * {@link java.util.HashMap}, but merely for lookups. Call {@link #toImmutableOpaque()} when necessary.
     * 
     * @param buf The buffer backing the {@link Opaque}.
     * @param index The absolute index to start from.
     * @param length The number of bytes.
     * @return The {@link Opaque} instance.
     * @see #toImmutableOpaque()
     */
    static Opaque forOwnedByteBuffer(ByteBuffer buf, int index, int length) {
        if (buf.order() != ByteOrder.BIG_ENDIAN) {
            buf = buf.duplicate();
        }
        return new OpaqueByteBufferImpl(buf, index, length);
    }

    static Opaque forOwnedBuffer(Buffer buf, int index, int length) {
        if (buf.order() != ByteOrder.BIG_ENDIAN) {
            buf = buf.duplicate();
        }
        return new OpaqueBufferImpl(buf, index, length);
    }

    /**
     * Default implementation for {@link #hashCode()}.
     * 
     * @param obj The instance object.
     * @return The hash code.
     * @see #hashCode()
     */
    static int defaultHashCode(Opaque obj) {
        return Arrays.hashCode(obj.toBytes());
    }

    /**
     * Default implementation for {@link #equals(Object)}.
     * 
     * @param obj The instance object.
     * @param other The other object.
     * @return {@code true} if equal.
     * @see #equals(Object)
     */
    static boolean defaultEquals(Opaque obj, Object other) {
        if (other == obj) {
            return true;
        }
        if (!(other instanceof Opaque)) {
            return false;
        }
        return Arrays.equals(obj.toBytes(), ((Opaque) other).toBytes());
    }

    /**
     * Returns a byte-representation of this opaque object.
     * 
     * @return A new array.
     */
    byte[] toBytes();

    /**
     * Returns the number of bytes in this opaque object;
     * 
     * @return The number of bytes;
     */
    int numBytes();

    /**
     * Returns a Base64 string representing this opaque object.
     * 
     * @return A Base64 string.
     */
    default String toBase64() {
        return Base64.getEncoder().withoutPadding().encodeToString(toBytes());
    }

    default String toBase16() {
        return BaseEncoding.base16().encode(toBytes());
    }

    /**
     * Returns an immutable {@link Opaque}, which may be the instance itself if it is already immutable.
     * 
     * @return An immutable opaque.
     */
    Opaque toImmutableOpaque();

    /**
     * Writes the bytes of this {@link Opaque} to the given {@link ByteBuffer}.
     * 
     * @param buf The target buffer.
     */
    default void putBytes(ByteBuffer buf) {
        buf.put(toBytes());
    }

    /**
     * Writes the bytes of this {@link Opaque} to the given {@link Buffer}.
     * 
     * @param buf The target buffer.
     */
    default void putBytes(Buffer buf) {
        buf.put(toBytes());
    }

    /**
     * Returns the hashCode based on the byte-representation of this instance.
     * <p>
     * This method must behave like {@link #defaultHashCode(Opaque)}, but may be optimized.
     * 
     * @return The hashCode.
     */
    @Override
    int hashCode();

    /**
     * Compares this object to another one.
     * <p>
     * This method must behave like {@link #defaultEquals(Opaque, Object)}, but may be optimized.
     * 
     * @return {@code true} if both objects are equal.
     */
    @Override
    boolean equals(Object o);

    /**
     * Returns the byte stored at the given position.
     * 
     * @param byteOffset The byte offset
     * @return The byte.
     */
    byte byteAt(int byteOffset);

    /**
     * Returns the {@code long} stored at the given position, using big-endian byte order.
     * 
     * @param byteOffset The byte offset
     * @return The long.
     */
    long longAt(int byteOffset);

    /**
     * Returns the {@code int} stored at the given position, using big-endian byte order.
     * 
     * @param byteOffset The byte offset
     * @return The int.
     */
    int intAt(int byteOffset);

    /**
     * Returns a copy of the number of bytes stored at the given position.
     * 
     * @param byteOffset The byte offset.
     * @param length The number of bytes starting from that offset.
     * @return A new byte array containing the bytes.
     */
    byte[] bytesAt(int byteOffset, int length);

    /**
     * Returns a {@link ByteBuffer} that is backed by the bytes in this {@link Opaque}.
     * 
     * @return The ByteBuffer.
     */
    default ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(toBytes());
    }

    public class OpaqueImpl implements Opaque {
        final byte[] _opaque;

        protected OpaqueImpl(byte[] opaque) {
            _opaque = opaque;
        }

        @Override
        public byte[] toBytes() {
            return _opaque.clone();
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(_opaque);
        }

        @Override
        public String toBase64() {
            return toBase64Impl();
        }

        protected String toBase64Impl() {
            return Base64.getEncoder().withoutPadding().encodeToString(_opaque);
        }

        @Override
        public void putBytes(ByteBuffer buf) {
            buf.put(_opaque);
        }

        @Override
        public void putBytes(Buffer buf) {
            buf.put(_opaque);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Opaque)) {
                return false;
            }

            if (o instanceof OpaqueImpl) {
                return Arrays.equals(_opaque, ((OpaqueImpl) o)._opaque);
            } else {
                Opaque other = (Opaque) o;
                if (other.numBytes() != _opaque.length) {
                    return false;
                }
                for (int i = 0, n = _opaque.length, oi = 0; i < n; i++, oi++) {
                    if (_opaque[i] != other.byteAt(oi)) {
                        return false;
                    }
                }
                return true;
            }
        }

        /**
         * Returns a (potentially non-stable) debug string.
         * 
         * @see #toBase64()
         */
        @Override
        public String toString() {
            return super.toString() + "[" + toBase16() + "]";
        }

        @Override
        public int numBytes() {
            return _opaque.length;
        }

        @Override
        public Opaque toImmutableOpaque() {
            return Opaque.forBytes(_opaque);
        }

        @Override
        public byte byteAt(int position) {
            return _opaque[position];
        }

        @Override
        public long longAt(int byteOffset) {
            return Bytes.getLong(_opaque, byteOffset);
        }

        @Override
        public int intAt(int byteOffset) {
            return Bytes.getInt(_opaque, byteOffset);
        }

        @Override
        public byte[] bytesAt(int byteOffset, int length) {
            return Arrays.copyOfRange(_opaque, byteOffset, byteOffset + length);
        }
    }

    final class OpaqueImmutableImpl extends OpaqueImpl {
        private String base64 = null;
        private int hashCode;

        protected OpaqueImmutableImpl(byte[] opaque) {
            super(opaque);
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = Arrays.hashCode(_opaque);
            }
            return hashCode;
        }

        @Override
        public String toBase64() {
            if (base64 == null) {
                base64 = toBase64Impl();
            }
            return base64;
        }

        @Override
        public Opaque toImmutableOpaque() {
            return this;
        }
    }

    final class OpaqueByteBufferImpl implements Opaque {
        private final ByteBuffer buf;
        private final int index;
        private final int length;

        private OpaqueByteBufferImpl(ByteBuffer buf, int index, int length) {
            this.buf = Objects.requireNonNull(buf);
            this.index = index;
            this.length = length;
        }

        @Override
        public byte[] toBytes() {
            byte[] bytes = new byte[length];
            buf.get(index, bytes);
            return bytes;
        }

        @Override
        public ByteBuffer asByteBuffer() {
            return buf.slice(index, length);
        }

        @Override
        public int numBytes() {
            return length;
        }

        @Override
        public String toBase64() {
            return Base64.getEncoder().withoutPadding().encodeToString(toBytes());
        }

        @Override
        public Opaque toImmutableOpaque() {
            return Opaque.forBytes(toBytes());
        }

        @Override
        public int hashCode() {
            int result = 1;
            for (int i = index, n = index + length; i < n; i++) {
                byte element = buf.get(i);
                result = 31 * result + element;
            }

            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Opaque)) {
                return false;
            }
            if (length != ((Opaque) o).numBytes()) {
                return false;
            }

            if (o instanceof OpaqueImpl) {
                byte[] otherBytes = ((OpaqueImpl) o)._opaque;
                for (int i = index, n = index + length, oi = 0; i < n; i++, oi++) {
                    if (buf.get(i) != otherBytes[oi]) {
                        return false;
                    }
                }
                return true;
            } else if (o instanceof OpaqueByteBufferImpl) {
                OpaqueByteBufferImpl other = (OpaqueByteBufferImpl) o;
                ByteBuffer otherBuf = other.buf;
                int otherIndex = other.index;
                for (int i = index, n = index + length, oi = otherIndex; i < n; i++, oi++) {
                    if (buf.get(i) != otherBuf.get(oi)) {
                        return false;
                    }
                }
                return true;
            } else {
                Opaque other = (Opaque) o;
                for (int i = index, n = index + length, oi = 0; i < n; i++, oi++) {
                    if (buf.get(i) != other.byteAt(oi)) {
                        return false;
                    }
                }
                return true;
            }
        }

        @Override
        public String toString() {
            return super.toString() + "[" + toBase16() + "]";
        }

        @Override
        public byte byteAt(int position) {
            return buf.get(index + position);
        }

        @Override
        public long longAt(int byteOffset) {
            return buf.getLong(index + byteOffset);
        }

        @Override
        public int intAt(int byteOffset) {
            return buf.getInt(index + byteOffset);
        }

        @Override
        public byte[] bytesAt(int byteOffset, int count) {
            byte[] out = new byte[count];
            buf.get(index + byteOffset, out);
            return out;
        }

        @Override
        public void putBytes(ByteBuffer out) {
            out.put(buf.slice(index, length));
        }

        @Override
        public void putBytes(Buffer out) {
            out.put(buf.slice(index, length));
        }
    }

    final class OpaqueBufferImpl implements Opaque {
        private final Buffer buf;
        private final int length;

        private OpaqueBufferImpl(Buffer buf, int index, int length) {
            this.buf = Objects.requireNonNull(buf).slice(index, index + length).order(ByteOrder.BIG_ENDIAN);
            this.length = length;
        }

        @Override
        public byte[] toBytes() {
            byte[] bytes = new byte[length];
            Buffer buf2 = buf.slice(0, length);
            buf2.get(bytes);
            return bytes;
        }

        @Override
        public ByteBuffer asByteBuffer() {
            return buf.toByteBuffer().slice();
        }

        @Override
        public int numBytes() {
            return length;
        }

        @Override
        public String toBase64() {
            return Base64.getEncoder().withoutPadding().encodeToString(toBytes());
        }

        @Override
        public Opaque toImmutableOpaque() {
            return Opaque.forBytes(toBytes());
        }

        @Override
        public int hashCode() {
            int result = 1;
            for (int i = 0, n = length; i < n; i++) {
                byte element = buf.get(i);
                result = 31 * result + element;
            }

            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Opaque)) {
                return false;
            }
            if (length != ((Opaque) o).numBytes()) {
                return false;
            }

            if (o instanceof OpaqueImpl) {
                byte[] otherBytes = ((OpaqueImpl) o)._opaque;
                for (int i = 0, n = 0 + length, oi = 0; i < n; i++, oi++) {
                    if (buf.get(i) != otherBytes[oi]) {
                        return false;
                    }
                }
                return true;
            } else {
                Opaque other = (Opaque) o;
                for (int i = 0, n = 0 + length, oi = 0; i < n; i++, oi++) {
                    if (buf.get(i) != other.byteAt(oi)) {
                        return false;
                    }
                }
                return true;
            }
        }

        @Override
        public String toString() {
            return super.toString() + "[" + toBase16() + "]";
        }

        @Override
        public byte byteAt(int position) {
            return buf.get(position);
        }

        @Override
        public long longAt(int byteOffset) {
            return buf.getLong(byteOffset);
        }

        @Override
        public int intAt(int byteOffset) {
            return buf.getInt(byteOffset);
        }

        @Override
        public byte[] bytesAt(int byteOffset, int count) {
            byte[] bytes = new byte[count];
            Buffer buf2 = buf.slice(byteOffset, byteOffset + count);
            buf2.get(bytes);
            return bytes;
        }

        @Override
        public void putBytes(ByteBuffer out) {
            out.put(buf.toByteBuffer());
        }

        @Override
        public void putBytes(Buffer out) {
            out.put(buf);
        }
    }

    static final class OpaqueZero implements Opaque {
        private final int num;

        private OpaqueZero(int num) {
            this.num = num;
        }

        @Override
        public byte[] toBytes() {
            return new byte[num];
        }

        @Override
        public int numBytes() {
            return num;
        }

        @Override
        public Opaque toImmutableOpaque() {
            return this;
        }

        @Override
        public byte byteAt(int byteOffset) {
            return 0;
        }

        @Override
        public long longAt(int byteOffset) {
            return 0;
        }

        @Override
        public int intAt(int byteOffset) {
            return 0;
        }

        @Override
        public byte[] bytesAt(int byteOffset, int length) {
            return new byte[length];
        }

        @Override
        public void putBytes(ByteBuffer buf) {
            if (buf.remaining() < num) {
                throw new BufferOverflowException();
            }
            buf.slice(buf.position(), num).clear();
            buf.position(buf.position() + num);
        }

        @Override
        public void putBytes(Buffer buf) {
            if (buf.remaining() < num) {
                throw new BufferOverflowException();
            }
            int pos = buf.position();
            buf.slice(pos, pos + num).clear();
            buf.position(pos + num);
        }
    }
}
