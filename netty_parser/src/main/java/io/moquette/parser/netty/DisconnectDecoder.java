/*
 * Copyright (c) 2012-2017 The original author or authorsgetRockQuestions()
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

import io.netty.buffer.ByteBuf;
import io.netty.util.AttributeMap;
import java.util.List;
import io.moquette.parser.proto.messages.DisconnectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
class DisconnectDecoder extends DemuxDecoder {

    private static Logger LOG = LoggerFactory.getLogger(DisconnectDecoder.class);

    @Override
    void decode(AttributeMap ctx, ByteBuf in, List<Object> out) throws Exception {
        //Common decoding part
        in.resetReaderIndex();
        DisconnectMessage message = new DisconnectMessage();
        if (!decodeCommonHeader(message, 0x00, in)) {
            in.resetReaderIndex();
            return;
        }
        LOG.debug("Decoding disconnect");
        out.add(message);
    }
    
}
