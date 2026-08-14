package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.L2NetworkMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/l2-networks/virtual-switch/{uuid}/uplink-group",
        method = HttpMethod.PUT,
        responseClass = APIUpdateVirtualSwitchUplinkGroupEvent.class,
        isAction = true
)
public class APIUpdateVirtualSwitchUplinkGroupMsg extends APIMessage implements L2NetworkMessage {
    @APIParam(resourceType = L2VirtualSwitchNetworkVO.class)
    private String uuid;

    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(resourceType = HostNetworkInterfaceVO.class, required = false)
    private List<String> slaveUuids;

    @APIParam(required = false)
    private List<String> slaveNames;

    @APIParam(required = false, validValues = {HostNetworkBondingConstant.LINUX_BONDING_TYPE, HostNetworkBondingConstant.OVS_BONDING_TYPE})
    private String type = "LinuxBonding";

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String  getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<String> getSlaveUuids() {
        return slaveUuids;
    }

    public void setSlaveUuids(List<String> slaveUuids) {
        this.slaveUuids = slaveUuids;
    }

    public List<String> getSlaveNames() {
        return slaveNames;
    }

    public void setSlaveNames(List<String> slaveNames) {
        this.slaveNames = slaveNames;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getL2NetworkUuid() {
        return uuid;
    }

    public static APIUpdateVirtualSwitchUplinkGroupMsg __example__() {
        APIUpdateVirtualSwitchUplinkGroupMsg msg = new APIUpdateVirtualSwitchUplinkGroupMsg();
        msg.setUuid(uuid());
        msg.setHostUuid(uuid());
        msg.setSlaveNames(Collections.singletonList("eth0"));

        return msg;
    }
}
