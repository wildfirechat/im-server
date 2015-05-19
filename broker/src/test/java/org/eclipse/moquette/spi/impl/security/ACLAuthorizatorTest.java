package org.eclipse.moquette.spi.impl.security;

import org.junit.Test;

import static java.util.Arrays.*;
import static org.eclipse.moquette.spi.impl.security.Authorization.Permission.*;
import static org.junit.Assert.*;

public class ACLAuthorizatorTest {

    @Test
    public void testCanWriteSimpleTopic() {
        ACLAuthorizator authorizator = new ACLAuthorizator(asList(new Authorization("/sensors", WRITE)));

        //verify
        assertTrue(authorizator.canWrite("/sensors"));
    }

    @Test
    public void testCanReadSimpleTopic() {
        ACLAuthorizator authorizator = new ACLAuthorizator(asList(new Authorization("/sensors", READ)));

        //verify
        assertTrue(authorizator.canRead("/sensors"));
    }

    @Test
    public void testCanReadWriteMixedSimpleTopic() {
        Authorization topicWriteACL = new Authorization("/sensors", WRITE);
        Authorization topicReadACL = new Authorization("/sensors/anemometer", READ);
        ACLAuthorizator authorizator = new ACLAuthorizator(asList(topicWriteACL, topicReadACL));

        //verify
        assertTrue(authorizator.canWrite("/sensors"));
        assertFalse(authorizator.canRead("/sensors"));
    }

    @Test
    public void testCanWriteMultiMatherTopic() {
        ACLAuthorizator authorizator = new ACLAuthorizator(asList(new Authorization("/sensors/#", WRITE)));

        //verify
        assertTrue(authorizator.canWrite("/sensors/anemometer/wind"));
    }

    @Test
    public void testCanWriteSingleMatherTopic() {
        ACLAuthorizator authorizator = new ACLAuthorizator(asList(new Authorization("/sensors/+", WRITE)));

        //verify
        assertTrue(authorizator.canWrite("/sensors/anemometer"));
    }

}