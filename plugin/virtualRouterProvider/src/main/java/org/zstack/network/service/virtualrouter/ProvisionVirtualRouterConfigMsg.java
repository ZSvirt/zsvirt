package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * Created by shixin.ruan 2021/03/19
 */
public class ProvisionVirtualRouterConfigMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String virtualRouterVmUuid;

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return virtualRouterVmUuid;
    }
}
