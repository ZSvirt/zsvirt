package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.L2NetworkMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/l2-networks/virtual-switch/{uuid}/uplink-bondings",
        method = HttpMethod.PUT,
        responseClass = APIUpdateVirtualSwitchUplinkBondingsEvent.class,
        isAction = true
)
public class APIUpdateVirtualSwitchUplinkBondingsMsg extends APIMessage implements L2NetworkMessage {
    @APIParam(resourceType = L2VirtualSwitchNetworkVO.class)
    private String uuid;

    @APIParam(required = false)
    private String bondingName;

    @APIParam(validValues = {HostNetworkBondingConstant.BONDING_MODE_LACP, HostNetworkBondingConstant.BONDING_MODE_AB})
    private String mode;

    @APIParam(required = false, validValues = {HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_TWO, HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_TWO_AND_THREE, HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_THREE_AND_FOUR})
    private String xmitHashPolicy;

    @Override
    public String getL2NetworkUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBondingName() {
        return bondingName;
    }

    public void setBondingName(String bondingName) {
        this.bondingName = bondingName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getXmitHashPolicy() {
        return xmitHashPolicy;
    }

    public void setXmitHashPolicy(String xmitHashPolicy) {
        this.xmitHashPolicy = xmitHashPolicy;
    }

    public static APIUpdateVirtualSwitchUplinkBondingsMsg __example__() {
        APIUpdateVirtualSwitchUplinkBondingsMsg msg = new APIUpdateVirtualSwitchUplinkBondingsMsg();
        msg.setUuid(uuid());
        msg.setMode("active-backup 1");
        msg.setXmitHashPolicy("layer2 0");
        return msg;
    }

}
