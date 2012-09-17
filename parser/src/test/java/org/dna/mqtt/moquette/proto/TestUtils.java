package org.dna.mqtt.moquette.proto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class TestUtils {
    
    static final class MockProtocolDecoderOutput<T extends AbstractMessage> implements ProtocolDecoderOutput {

        protected T m_connMessage;
        
        public T getMessage() {
            return m_connMessage;
        }
        
        public void write(Object message) {
            m_connMessage = (T) message;
        }

        public void flush(NextFilter nextFilter, IoSession session) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    
    
    static final class MockProtocolEncoderOutput implements ProtocolEncoderOutput {
        
        protected IoBuffer m_buffer;
        
        public IoBuffer getBuffer() {
            return m_buffer;
        }

        public void write(Object buffer) {
            m_buffer = (IoBuffer) buffer;
        }

        public void mergeAll() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public WriteFuture flush() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    
    /**
     * Verify the presence of the given string starting from the current position
     * inside the buffer.
     */
    static void verifyString(String str, IoBuffer buff) throws UnsupportedEncodingException {
        IoBuffer tmpBuff = IoBuffer.allocate(2).setAutoExpand(true);
        byte[] raw = str.getBytes("UTF-8");
        Utils.writeWord(tmpBuff, raw.length);
        tmpBuff.put(raw).flip();
        
        verifyIoBuffer(tmpBuff, buff);
    }
    
    static void verifyIoBuffer(IoBuffer expected, IoBuffer found) {
        while(expected.hasRemaining()) {
            assertEquals(expected.get(), found.get());
        }
    }
    
    /**
     * Verify that the given bytes buffer og the given numBytes length is present
     * in the buff starting from the current position.
     */
    static void verifyBuff(int numBytes, byte[] bytes, IoBuffer buff) {
        assertTrue(numBytes <= buff.remaining());
        byte[] toCheck = new byte[numBytes];
        buff.get(toCheck);
        
        for (int i = 0; i < numBytes; i++) {
            assertEquals(bytes[i], toCheck[i]);
        }
    }
    
    static void verifyEquals(byte[] expected, byte[] found) {
        assertEquals(expected.length, found.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], found[i]);
        }
    }
    
    
    public static IoBuffer generateRandomPayload(int size) {
        IoBuffer payloadBuffer = IoBuffer.allocate(size);
        for (int i = 0; i < size; i++) {
            payloadBuffer.put((byte) (Math.random() * 255));
        }
        payloadBuffer.flip();
        return payloadBuffer;
    }
    
    public static IoBuffer loadFile(String path) throws FileNotFoundException, IOException {
        File f = new File(path);
        long size = f.length();
        byte[] payload = new byte[(int) size];

        FileInputStream fr = new FileInputStream(f);
        fr.read(payload);

        IoBuffer payloadBuffer = IoBuffer.allocate((int) size);
        payloadBuffer.put(payload).flip();
        return payloadBuffer;
    }
}
