package org.dna.mqtt.moquette.server;

import java.util.concurrent.BlockingQueue;

import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.messaging.spi.INotifier;
import org.dna.mqtt.moquette.messaging.spi.impl.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
class Notifier implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(Notifier.class);

    BlockingQueue<MessagingEvent> m_queue;
    INotifier m_notifier;
    IMessaging m_messaging;
    
    Notifier(BlockingQueue<MessagingEvent> queue, INotifier notifier, IMessaging messaging) {
        m_queue = queue;
        m_notifier = notifier;
        m_messaging = messaging;
    }
    
    public void run() {
        LOG.debug("Notifier started");
        boolean interrupted = false;
        while (!interrupted) {
            try {
                MessagingEvent evt = m_queue.take();
                LOG.debug("Notifing event " + evt);
                if (evt instanceof NotifyEvent) {
                    NotifyEvent evtN = (NotifyEvent) evt;
                    m_notifier.notify(evtN);
                } else if (evt instanceof DisconnectEvent) {
                    DisconnectEvent evtD = (DisconnectEvent) evt;
                    m_notifier.disconnect(evtD.getSession());
                } else if (evt instanceof CleanInFlightEvent) {
                    m_messaging.refill(evt);
                } else if (evt instanceof PubAckEvent) {
                    m_notifier.sendPubAck((PubAckEvent) evt);
                }
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }
        LOG.debug("Notifier stopped");
    }
    
}
