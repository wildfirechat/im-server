package io.moquette.interception;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mackristof on 28/05/2016.
 */
public class HazelcastInterceptHandler extends AbstractInterceptHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastInterceptHandler.class);
    private final HazelcastInstance hz;

    public HazelcastInterceptHandler(Server server){
        this.hz = server.getHazelcastInstance();
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        LOG.info("{} publish on {} message: {}", msg.getClientID(), msg.getTopicName(), new String(msg.getPayload().array()));
        ITopic<HazelcastMsg> topic = hz.getTopic("moquette");
        HazelcastMsg hazelcastMsg = new HazelcastMsg(msg);
        topic.publish(hazelcastMsg);
    }

}
