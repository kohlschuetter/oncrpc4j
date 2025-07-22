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
package org.dcache.oncrpc4j.rpc;

import java.io.IOException;

import javax.security.auth.Subject;

import org.dcache.oncrpc4j.util.Opaque;
import org.dcache.oncrpc4j.xdr.XdrAble;
import org.dcache.oncrpc4j.xdr.XdrDecodingStream;
import org.dcache.oncrpc4j.xdr.XdrEncodingStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcAuthTypeNone implements RpcAuth, XdrAble {

    private final int _type = RpcAuthType.NONE;
    private Opaque body;
    private RpcAuthVerifier _verifier = new RpcAuthVerifier(RpcAuthType.NONE, Opaque.EMPTY_OPAQUE);

    private static final Subject _subject;
    static {
        _subject = new Subject();
        _subject.setReadOnly();
    }

    private final static Logger _log = LoggerFactory.getLogger(RpcAuthTypeNone.class);

    public RpcAuthTypeNone() {
        this(Opaque.EMPTY_OPAQUE);
    }

    public RpcAuthTypeNone(Opaque body) {
        this.body = body;
    }

    @Override
    public Subject getSubject() {
        return _subject;
    }

    @Override
    public int type() {
        return _type;
    }

    @Override
    public RpcAuthVerifier getVerifier() {
        return _verifier;
    }

    @Override
    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
        body = xdr.xdrDecodeDynamicOpaque();
        _verifier = new RpcAuthVerifier(xdr);
    }

    @Override
    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
       xdr.xdrEncodeInt(_type);
       xdr.xdrEncodeDynamicOpaque(body);
       _verifier.xdrEncode(xdr);
    }

}
