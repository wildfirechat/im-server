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
                LOG.info(String.format("%s received from hazelcast for topic %s message : %s\n", msg.getMessageObject().getClientId(), msg.getMessageObject().getTopic(), msg.getMessageObject().getPayload()));
                PublishMessage publishMessage = new PublishMessage();
                publishMessage.setTopicName(msg.getMessageObject().getTopic());
                publishMessage.setQos(AbstractMessage.QOSType.valueOf(msg.getMessageObject().getQos()));
                publishMessage.setPayload(ByteBuffer.wrap(msg.getMessageObject().getPayload()));
                publishMessage.setLocal(false);
                publishMessage.setClientId(msg.getMessageObject().getClientId());
                server.internalPublish(publishMessage);

            }
        } catch (Exception e) {
            LOG.error("error while wait for polling hazelcast msg queue",e);
        }
    }
}
