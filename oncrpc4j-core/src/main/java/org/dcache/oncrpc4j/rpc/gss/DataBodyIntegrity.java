/*
 * Copyright (c) 2009 - 2018 Deutsches Elektronen-Synchroton,
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
package org.dcache.oncrpc4j.rpc.gss;

import java.io.IOException;

import org.dcache.oncrpc4j.rpc.OncRpcException;
import org.dcache.oncrpc4j.util.Opaque;
import org.dcache.oncrpc4j.xdr.XdrAble;
import org.dcache.oncrpc4j.xdr.XdrDecodingStream;
import org.dcache.oncrpc4j.xdr.XdrEncodingStream;

/**
 * RPCGSS_SEC data body for integrity QOS as defined in RFC 2203
 */
public class DataBodyIntegrity implements XdrAble {

    private Opaque data;
    private Opaque checksum;

    public DataBodyIntegrity() {
    }

    public DataBodyIntegrity(Opaque data, Opaque checksum) {
        this.data = data;
        this.checksum = checksum;
    }

    @Override
    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
        data = xdr.xdrDecodeDynamicOpaque();
        checksum = xdr.xdrDecodeDynamicOpaque();
    }

    @Override
    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
        xdr.xdrEncodeDynamicOpaque(data);
        xdr.xdrEncodeDynamicOpaque(checksum);
    }

    public Opaque getChecksum() {
        return checksum;
    }

    public Opaque getData() {
        return data;
    }
}
