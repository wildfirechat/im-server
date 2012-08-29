package org.dna.mqtt.moquette.server;

import java.util.concurrent.BlockingQueue;
import org.dna.mqtt.moquette.messaging.spi.INotifier;
import org.dna.mqtt.moquette.messaging.spi.impl.events.DisconnectEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.MessagingEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.NotifyEvent;
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
    
    Notifier(BlockingQueue<MessagingEvent> queue, INotifier notifier) {
        m_queue = queue;
        m_notifier = notifier;
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
                    m_notifier.notify(evtN.getClientId(), evtN.getTopic(), evtN.getQos(), evtN.getMessage(), evtN.isRetained());
                } else if (evt instanceof DisconnectEvent) {
                    DisconnectEvent evtD = (DisconnectEvent) evt;
                    m_notifier.disconnect(evtD.getSession());
                }
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }
        LOG.debug("Notifier stopped");
    }
    
}
