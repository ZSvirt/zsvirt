package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 2019-09-28.
 */
public class GetVmFirstBootDeviceOnHypervisorReply extends MessageReply {
    private String firstBootDevice;

    public String getFirstBootDevice() {
        return firstBootDevice;
    }

    public void setFirstBootDevice(String firstBootDevice) {
        this.firstBootDevice = firstBootDevice;
    }
}
