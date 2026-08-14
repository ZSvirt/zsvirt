package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 * @ Author : yh.w
 * @ Date   : Created in 21:27 2021/2/2
 */
public class GetVmCapabilitiesReply extends MessageReply {
    private VmCapabilities capabilities;

    public VmCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(VmCapabilities capabilities) {
        this.capabilities = capabilities;
    }
}
