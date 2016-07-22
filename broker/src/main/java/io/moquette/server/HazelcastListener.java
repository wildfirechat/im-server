package io.moquette.server;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import io.moquette.interception.HazelcastMsg;
import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.parser.proto.messages.PublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Created by mackristof on 28/05/2016.
 */
public class HazelcastListener implements MessageListener<HazelcastMsg> {
    private static final Logger LOG = LoggerFactory.getLogger(HazelcastListener.class);

    private final Server server;

    public HazelcastListener(Server server){
        this.server = server;
    }

    @Override
    public void onMessage(Message<HazelcastMsg> msg) {
        try {
            if (!msg.getPublishingMember().equals(server.getHazelcastInstance().getCluster().getLocalMember())) {
                HazelcastMsg hzMsg = msg.getMessageObject();
                LOG.info("{} received from hazelcast for topic {} message: {}", hzMsg.getClientId(),
                        hzMsg.getTopic(), hzMsg.getPayload());
                PublishMessage publishMessage = new PublishMessage();
                publishMessage.setTopicName(hzMsg.getTopic());
                publishMessage.setQos(AbstractMessage.QOSType.valueOf(hzMsg.getQos()));
                publishMessage.setPayload(ByteBuffer.wrap(hzMsg.getPayload()));
                publishMessage.setLocal(false);
                publishMessage.setClientId(hzMsg.getClientId());
                server.internalPublish(publishMessage);
            }
        } catch (Exception ex) {
            LOG.error("error polling hazelcast msg queue", ex);
        }
    }
}
