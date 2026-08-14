package org.zstack.header.baremetal.instance;

import org.zstack.header.message.Message;

/**
 * Created by GuoYi on 7/9/18.
 */
public interface BaremetalInstance {
    void handleMessage(Message msg);
}
