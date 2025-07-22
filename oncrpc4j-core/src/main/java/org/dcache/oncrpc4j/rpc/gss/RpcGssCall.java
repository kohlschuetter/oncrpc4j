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
import org.dcache.oncrpc4j.rpc.RpcAuthError;
import org.dcache.oncrpc4j.rpc.RpcAuthException;
import org.dcache.oncrpc4j.rpc.RpcAuthStat;
import org.dcache.oncrpc4j.rpc.RpcCall;
import org.dcache.oncrpc4j.rpc.RpcRejectStatus;
import org.dcache.oncrpc4j.util.Opaque;
import org.dcache.oncrpc4j.xdr.Xdr;
import org.dcache.oncrpc4j.xdr.XdrAble;
import org.dcache.oncrpc4j.xdr.XdrDecodingStream;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.MessageProp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An extention of {@link RpcCall} which Wrap/Unwrap the data according GSS QOS. The supported QOS are: NONE, INTEGRITY
 * and PRIVACY as specified in rfs 2203.
 *
 * @since 0.0.4
 */
public class RpcGssCall extends RpcCall {

    private final static Logger _log = LoggerFactory.getLogger(RpcGssCall.class);
    private final GSSContext _gssContext;
    private final MessageProp _mop;

    public RpcGssCall(RpcCall call, GSSContext gssContext, MessageProp mop) {
        super(call.getXid(), call.getProgram(), call.getProgramVersion(),
                call.getProcedure(), call.getCredential(), call.getXdr(), call.getTransport());
        _gssContext = gssContext;
        _mop = mop;
    }

    @Override
    public void retrieveCall(XdrAble args) throws OncRpcException, IOException {
        try {
            RpcAuthGss authGss = (RpcAuthGss) getCredential();
            _log.debug("Call with GSS service: {}", authGss.getService());
            XdrDecodingStream xdr;
            switch (authGss.getService()) {
                case RpcGssService.RPC_GSS_SVC_NONE:
                    super.retrieveCall(args);
                    break;
                case RpcGssService.RPC_GSS_SVC_INTEGRITY:
                    DataBodyIntegrity integData = new DataBodyIntegrity();
                    super.retrieveCall(integData);
                    byte[] integBytes = integData.getData().toBytes();
                    byte[] checksum = integData.getChecksum().toBytes();
                    _gssContext.verifyMIC(checksum, 0, checksum.length,
                            integBytes, 0, integBytes.length, _mop);

                    xdr = new Xdr(integBytes);
                    xdr.beginDecoding();
                    xdr.xdrDecodeInt(); // first 4 bytes of data is the sequence number. Skip it.
                    args.xdrDecode(xdr);
                    xdr.endDecoding();
                    break;
                case RpcGssService.RPC_GSS_SVC_PRIVACY:
                    DataBodyPrivacy privacyData = new DataBodyPrivacy();
                    super.retrieveCall(privacyData);
                    byte[] privacyBytes = privacyData.getData().toBytes();
                    byte[] rawData = _gssContext.unwrap(privacyBytes, 0, privacyBytes.length, _mop);

                    xdr = new Xdr(rawData);
                    xdr.beginDecoding();
                    xdr.xdrDecodeInt(); // first 4 bytes of data is the sequence number. Skip it.
                    args.xdrDecode(xdr);
                    xdr.endDecoding();
            }
        } catch (GSSException e) {
            _log.error("GSS error: {}", e.getMessage());
            throw new RpcAuthException("GSS error: " + e.getMessage(),
                    new RpcAuthError(RpcAuthStat.RPCSEC_GSS_CTXPROBLEM));
        }
    }

    @Override
    public void acceptedReply(int state, XdrAble reply) {
        try {
            RpcAuthGss authGss = (RpcAuthGss) getCredential();
            _log.debug("Reply with GSS service: {}", authGss.getService());
            switch (authGss.getService()) {
                case RpcGssService.RPC_GSS_SVC_NONE:
                    super.acceptedReply(state, reply);
                    break;
                case RpcGssService.RPC_GSS_SVC_INTEGRITY:
                    Opaque integBytes;
                    try (Xdr xdr = new Xdr(256 * 1024)) {
                        xdr.beginEncoding();
                        xdr.xdrEncodeInt(authGss.getSequence());
                        reply.xdrEncode(xdr);
                        xdr.endEncoding();
                        integBytes = xdr.toOpaque();
                    }

                    byte[] checksum = _gssContext.getMIC(integBytes.toBytes(), 0, integBytes.numBytes(), _mop);
                    DataBodyIntegrity integData = new DataBodyIntegrity(integBytes, Opaque
                            .forMutableByteArray(checksum));
                    super.acceptedReply(state, integData);
                    break;
                case RpcGssService.RPC_GSS_SVC_PRIVACY:
                    Opaque rawData;
                    try (Xdr xdr = new Xdr(256 * 1024)) {
                        xdr.beginEncoding();
                        xdr.xdrEncodeInt(authGss.getSequence());
                        reply.xdrEncode(xdr);
                        xdr.endEncoding();
                        rawData = xdr.toOpaque();
                    }

                    byte[] privacyBytes = _gssContext.wrap(rawData.toBytes(), 0, rawData.numBytes(), _mop);
                    DataBodyPrivacy privacyData = new DataBodyPrivacy(Opaque.forImmutableBytes(privacyBytes));
                    super.acceptedReply(state, privacyData);
                    break;
            }

        } catch (IOException e) {
            _log.error("IO error: {}", e.getMessage());
            super.reject(RpcRejectStatus.AUTH_ERROR, new RpcAuthError(RpcAuthStat.RPCSEC_GSS_CTXPROBLEM));
        } catch (GSSException e) {
            _log.error("GSS error: {}", e.getMessage());
            super.reject(RpcRejectStatus.AUTH_ERROR, new RpcAuthError(RpcAuthStat.RPCSEC_GSS_CTXPROBLEM));
        }
    }
}
