package org.dcache.oncrpc4j.xdr;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.dcache.oncrpc4j.util.Opaque;
import org.junit.Test;

public class XdrOpaqueTest {

    @Test
    public void testEncode() throws IOException {

        XdrOpaque opaque = new XdrOpaque("some bytes".getBytes(StandardCharsets.UTF_8));

        try(Xdr xdr = new Xdr(128)) {

            xdr.beginEncoding();
            opaque.xdrEncode(xdr);
            xdr.endEncoding();

            xdr.beginDecoding();

            assertEquals("invalid opaque value", opaque.getOpaque(), xdr.xdrDecodeDynamicOpaque());
        }
    }

    @Test
    public void testDecode() throws IOException {

        Opaque value = Opaque.forUtf8Bytes("some bytes");

        try (Xdr xdr = new Xdr(128)) {

            xdr.beginEncoding();
            xdr.xdrEncodeDynamicOpaque(value);
            xdr.endEncoding();

            xdr.beginDecoding();

            XdrOpaque opaque = new XdrOpaque(xdr);

            assertEquals("invalid opaque value", value, opaque.getOpaque());
        }
    }

    @Test
    public void testSameMustBeEqual() throws IOException {

        XdrOpaque opaque = new XdrOpaque("some bytes".getBytes(StandardCharsets.UTF_8));

        assertEquals("opaques with equal values must be equal", opaque, opaque);
    }

    @Test
    public void testEqualsByValue() throws IOException {

        XdrOpaque opaque1 = new XdrOpaque("some bytes".getBytes(StandardCharsets.UTF_8));
        XdrOpaque opaque2 = new XdrOpaque("some bytes".getBytes(StandardCharsets.UTF_8));

        assertEquals("opaques with equal values must be equal", opaque1, opaque2);
        assertEquals("equal objects must have the same hashcode", opaque1.hashCode(), opaque2.hashCode());
    }

    @Test
    public void testDifferentTypesCantBeEqual() throws IOException {

        byte[] data = "some bytes".getBytes(StandardCharsets.UTF_8);
        XdrOpaque opaque = new XdrOpaque(data);

        assertNotEquals("opaques with equal values must be equal", opaque, data);
    }


    @Test
    public void testDifferentObjectDifferentStrings() {
        XdrOpaque opaque1 = new XdrOpaque("some bytes".getBytes(StandardCharsets.UTF_8));
        XdrOpaque opaque2 = new XdrOpaque("some other bytes".getBytes(StandardCharsets.UTF_8));
        assertNotNull("toString should not return null", opaque1.toString());
        assertNotNull("toString should not return null", opaque2.toString());

        assertNotEquals("different by value must return different strings",
                opaque1.toString(), opaque2.toString());
    }

    @Test
    public void testDecodeWellKnown() throws IOException {

        byte[] data = new byte[]{
            0x0, 0x0, 0x0, 0x08, // size
            0x0C, 0x0A, 0x0F, 0x0E, 0x0B, 0x0A, 0x0B, 0x0E // data
        };

        try (Xdr xdr = new Xdr(data)) {
            xdr.beginDecoding();

            XdrOpaque opaque = new XdrOpaque(xdr);
            // the data part must match (e.g without size)
            assertEquals(Opaque.forBytes(Arrays.copyOfRange(data, 4, 12)), opaque.getOpaque());
        }
    }

    @Test
    public void testDecodeWellKnownStandardConstructor() throws IOException {

        byte[] data = new byte[]{
            0x0, 0x0, 0x0, 0x08, // size
            0x0C, 0x0A, 0x0F, 0x0E, 0x0B, 0x0A, 0x0B, 0x0E // data
        };

        try ( Xdr xdr = new Xdr(data)) {
            xdr.beginDecoding();

            XdrOpaque opaque = new XdrOpaque();
            opaque.xdrDecode(xdr);
            // the data part must match (e.g without size)
            assertEquals(Opaque.forMutableByteArray(Arrays.copyOfRange(data, 4, 12)), opaque.getOpaque());
        }
    }

    @Test
    public void testEncodeWellKnown() throws IOException {

        byte[] data = new byte[]{
            0x0C, 0x0A, 0x0F, 0x0E, 0x0B, 0x0A, 0x0B, 0x0E // data
        };

        byte[] expected = new byte[]{
            0x0, 0x0, 0x0, 0x08, // size
            0x0C, 0x0A, 0x0F, 0x0E, 0x0B, 0x0A, 0x0B, 0x0E // data
        };

        try (Xdr xdr = new Xdr(16)) {
            xdr.beginEncoding();

            XdrOpaque opaque = new XdrOpaque(data);
            opaque.xdrEncode(xdr);

            xdr.endEncoding();

            // the data part must match (e.g without size)
            assertArrayEquals(expected, xdr.getBytes());
        }
    }

}
