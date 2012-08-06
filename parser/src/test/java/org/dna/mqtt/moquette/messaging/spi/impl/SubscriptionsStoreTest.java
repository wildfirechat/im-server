package org.dna.mqtt.moquette.messaging.spi.impl;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import org.dna.mqtt.moquette.messaging.spi.impl.SubscriptionsStore.Token;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

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
    public void testSplitTopic() throws ParseException {
        List tokens = store.splitTopic("finance/stock/ibm");
        assertEqualsSeq(asArray("finance", "stock", "ibm"), tokens);
        
        tokens = store.splitTopic("/finance/stock/ibm");
        assertEqualsSeq(asArray(Token.EMPTY, "finance", "stock", "ibm"), tokens);
        
        tokens = store.splitTopic("/");
        assertEqualsSeq(asArray(Token.EMPTY), tokens);
    }
    
    
    @Test(expected = ParseException.class)
    public void testSplitTopicTwinsSlashAvoided() throws ParseException {
        store.splitTopic("/finance//stock/ibm");
    }
    
    @Test
    public void testSplitTopicMultiValid() throws ParseException {
        List tokens = store.splitTopic("finance/stock/#");
        assertEqualsSeq(asArray("finance", "stock", Token.MULTI), tokens);
        
        tokens = store.splitTopic("#");
        assertEqualsSeq(asArray(Token.MULTI), tokens);
    }
    
    
    @Test(expected = ParseException.class)
    public void testSplitTopicMultiInTheMiddleNotValid() throws ParseException {
        store.splitTopic("finance/#/closingprice");
    }
    
    @Test(expected = ParseException.class)
    public void testSplitTopicMultiNotAferSeparatorNotValid() throws ParseException {
        store.splitTopic("finance#");
    }
    
    
    @Test
    public void testSplitTopicSingleValid() throws ParseException {
        List tokens = store.splitTopic("finance/stock/+");
        assertEqualsSeq(asArray("finance", "stock", Token.SINGLE), tokens);
        
        tokens = store.splitTopic("+");
        assertEqualsSeq(asArray(Token.SINGLE), tokens);
        
        tokens = store.splitTopic("finance/+/ibm");
        assertEqualsSeq(asArray("finance", Token.SINGLE, "ibm"), tokens);
    }
    
    @Test(expected = ParseException.class)
    public void testSplitTopicSingleNotAferSeparatorNotValid() throws ParseException {
        store.splitTopic("finance+");
    }
    
    @Test
    public void testMatchSimple() {
        store.add(new Subscription(null, "/", AbstractMessage.QOSType.MOST_ONE));
        assertTrue(store.matches("finance").isEmpty());
        
        store.add(new Subscription(null, "/finance", AbstractMessage.QOSType.MOST_ONE));
        assertTrue(store.matches("finance").isEmpty());
    }
    
    @Test
    public void testMatchSimpleMulti() {
        store.add(new Subscription(null, "#", AbstractMessage.QOSType.MOST_ONE));
        assertFalse(store.matches("finance").isEmpty());
        
        store.add(new Subscription(null, "finance/#", AbstractMessage.QOSType.MOST_ONE));
        assertFalse(store.matches("finance").isEmpty());
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
