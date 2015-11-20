/*
 * Copyright (c) 2012-2015 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.moquette.parser.netty;

import io.moquette.proto.messages.MessageIDMessage;
import io.moquette.proto.messages.PubRelMessage;
import io.netty.buffer.ByteBuf;
import io.netty.util.AttributeMap;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 *
 * @author andrea
 */
class PubRelDecoder extends DemuxDecoder {
    
    @Override
    void decode(AttributeMap ctx, ByteBuf in, List<Object> out) throws UnsupportedEncodingException {
        in.resetReaderIndex();
        //Common decoding part
        MessageIDMessage message = new PubRelMessage();
        if (!decodeCommonHeader(message, 0x02, in)) {
            in.resetReaderIndex();
            return;
        }
        
        //read  messageIDs
        message.setMessageID(in.readUnsignedShort());
        out.add(message);
    }

}

