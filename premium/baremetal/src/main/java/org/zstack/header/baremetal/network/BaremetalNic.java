package org.zstack.header.baremetal.network;

import org.zstack.header.message.Message;

public interface BaremetalNic {
    void handleMessage(Message msg);
}
