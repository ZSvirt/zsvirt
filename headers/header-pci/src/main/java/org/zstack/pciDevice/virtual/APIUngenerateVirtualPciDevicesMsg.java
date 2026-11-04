package org.zstack.pciDevice.virtual;

import org.zstack.header.message.APIMessage;

/**
 * Created by GuoYi on 2019-04-24.
 */
public abstract class APIUngenerateVirtualPciDevicesMsg extends APIMessage {
    public abstract String getVirtTechType();
}
