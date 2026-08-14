package org.zstack.core.cloudbus;

import org.zstack.header.message.Event;

public interface CloudBusEventListener {
    boolean handleEvent(Event e);
}
