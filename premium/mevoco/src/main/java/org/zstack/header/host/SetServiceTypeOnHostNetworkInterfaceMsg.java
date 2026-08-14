package org.zstack.header.host;

import org.zstack.core.db.Q;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;

import java.util.List;

public class SetServiceTypeOnHostNetworkInterfaceMsg extends NeedReplyMessage implements HostMessage {
    @APIParam(resourceType = HostNetworkInterfaceVO.class)
    private String interfaceUuid;

    @APIParam(required = false)
    private Integer vlanId;

    @APIParam(required = false, validValues = {"ManagementNetwork", "TenantNetwork", "StorageNetwork", "BackupNetwork", "MigrationNetwork"}, maxLength = 128)
    private List<String> serviceType;

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }

    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public List<String> getServiceType() {
        return serviceType;
    }

    public void setServiceType(List<String> serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public String getHostUuid() {
        return Q.New(HostNetworkInterfaceVO.class).select(HostNetworkInterfaceVO_.hostUuid)
                .eq(HostNetworkInterfaceVO_.uuid, interfaceUuid).findValue();

    }
}
