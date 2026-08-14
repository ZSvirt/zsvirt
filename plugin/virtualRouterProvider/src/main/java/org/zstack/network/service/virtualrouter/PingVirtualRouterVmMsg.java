package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * Created by frank on 6/29/2015.
 */
public class PingVirtualRouterVmMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String virtualRouterVmUuid;

    @Override
    public String getVmInstanceUuid() {
        return virtualRouterVmUuid;
    }

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }
}
