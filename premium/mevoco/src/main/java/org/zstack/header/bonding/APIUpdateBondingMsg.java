package org.zstack.header.bonding;

import org.springframework.http.HttpMethod;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;


import java.util.List;

@RestRequest(
        path = "/hosts/bondings/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateBondingEvent.class
)
public class APIUpdateBondingMsg extends APIMessage implements BondingMessage {
    /**
     * @desc uuid of bonding which is going to update
     */
    @APIParam(resourceType = HostNetworkBondingVO.class)
    private String uuid;

    /**
     * @desc slave uuids, see :ref:`HostNetworkInterfaceInventory`
     */
    @APIParam(required = false, resourceType = HostNetworkInterfaceVO.class)
    private List<String> slaveUuids;

    /**
     * @desc slave names, for querying and filling in slave uuids
     */
    @APIParam(required = false)
    private List<String> slaveNames;

    /**
     * @desc type of vSwitch installed on the host
     */
    @APIParam(required = false, validValues = {HostNetworkBondingConstant.LINUX_BONDING_TYPE, HostNetworkBondingConstant.OVS_BONDING_TYPE})
    private String type;

    /**
     * @desc bonding mode
     */
    @APIParam(required = false,validValues = {HostNetworkBondingConstant.BONDING_MODE_LACP, HostNetworkBondingConstant.BONDING_MODE_AB})
    private String mode;

    /**
     * @desc bonding xmitHashPolicy
     */
    @APIParam(required = false,validValues = {HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_TWO, HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_TWO_AND_THREE, HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_THREE_AND_FOUR})
    private String xmitHashPolicy;

    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;

    @Override
    public String getBondingUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static APIUpdateBondingMsg __example__() {
        APIUpdateBondingMsg msg = new APIUpdateBondingMsg();
        msg.setUuid(uuid());
        msg.setMode("802.3ad");
        msg.setXmitHashPolicy("layer2+3");
        return msg;
    }
}
