package org.zstack.pciDevice.virtual;

import org.zstack.header.message.Message;

/**
 * Created by GuoYi on 2019-04-24.
 */
public interface VirtualPciDevice {
    void handleMessage(Message msg);
}
