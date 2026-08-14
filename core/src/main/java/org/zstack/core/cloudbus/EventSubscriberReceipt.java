package org.zstack.core.cloudbus;

import org.zstack.header.message.Event;

/**
 */
public interface EventSubscriberReceipt {
    void unsubscribe(Event e);

    void unsubscribeAll();
}
