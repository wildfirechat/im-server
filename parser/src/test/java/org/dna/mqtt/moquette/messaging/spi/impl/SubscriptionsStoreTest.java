package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.Arrays;
import java.util.List;
import org.dna.mqtt.moquette.messaging.spi.impl.SubscriptionsStore.Token;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author andrea
 */
public class SubscriptionsStoreTest {
    
    private SubscriptionsStore store;
    
    public SubscriptionsStoreTest() {
    }
    
    @Before
    public void setUp() {
        store = new SubscriptionsStore();
    }

    @Test
    public void testSplitTopic() {
        List<Token> result = store.splitTopic("finance/stock/ibm");
        assertEqualsSeq(asArray("finance", "stock", "ibm"), result);
    }
    
    private static Token[] asArray(Object... l) {
        Token[] tokens = new Token[l.length];
        for (int i = 0; i < l.length; i++) {
            Object o = l[i];
            if (o instanceof Token) {
                tokens[i] = (Token)o;
            } else {
                tokens[i] = new Token(o.toString());
            }
        }
        
        return tokens;
    }
    
    private void assertEqualsSeq(Token[] exptected, List<Token> result) {
        List<Token> expectedList = Arrays.asList(exptected);
        assertEquals(expectedList, result);
    }
}
