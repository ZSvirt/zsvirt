package org.zstack.header.host;

import org.zstack.core.db.Q;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;

import java.util.List;

public class SetServiceTypeOnHostNetworkBondingMsg extends NeedReplyMessage implements HostMessage {
    @APIParam(resourceType = HostNetworkBondingVO.class)
    private String bondingUuid;

    @APIParam(required = false)
    private Integer vlanId;

    @APIParam(required = false, validValues = {"ManagementNetwork", "TenantNetwork", "StorageNetwork", "BackupNetwork", "MigrationNetwork"}, maxLength = 128)
    private List<String> serviceType;

    public void setBondingUuid(String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }

    public String getBondingUuid() {
        return bondingUuid;
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
        return Q.New(HostNetworkBondingVO.class).select(HostNetworkBondingVO_.hostUuid)
                .eq(HostNetworkBondingVO_.uuid, bondingUuid).findValue();

    }
}
