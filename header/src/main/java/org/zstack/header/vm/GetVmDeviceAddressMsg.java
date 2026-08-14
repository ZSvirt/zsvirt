package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by MaJin on 2020/7/22.
 */
public class GetVmDeviceAddressMsg extends NeedReplyMessage implements HostMessage {
    private String vmInstanceUuid;
    private String hostUuid;
    private Map<String, List> inventories = new HashMap<>();

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public Map<String, List> getInventories() {
        return inventories;
    }

    public void putInventories(String resourceType, List inventories) {
        this.inventories.put(resourceType, inventories);
    }

    public void setInventories(Map<String, List> inventories) {
        this.inventories = inventories;
    }
}
