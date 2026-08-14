package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.Message;

/**
 * Created by GuoYi on 2018-10-12.
 */
public interface BaremetalPxeServer {
    void handleMessage(Message msg);
}
