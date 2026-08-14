package org.zstack.header.bonding;

import org.springframework.http.HttpMethod;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;


import java.util.*;

@RestRequest(
        path = "/hosts/bondings",
        method = HttpMethod.POST,
        responseClass = APICreateBondingEvent.class,
        parameterName = "params"
)
@TagResourceType(HostNetworkBondingVO.class)
public class APICreateBondingMsg extends APICreateMessage {
    /**
     * @desc uuid of host this bonding is going to create in
     */
    @APIParam(resourceType = HostVO.class)
    private List<String> hostUuids;

    /**
     * @desc max length of 15 characters
     */
    @APIParam(validRegexValues = HostNetworkBondingConstant.BONDING_NAME_REGEX, maxLength = HostNetworkBondingConstant.BONDING_NAME_MAX)
    private String bondingName;

    /**
     * @desc slave uuids, see :ref:`HostNetworkInterfaceInventory`
     */
    @APIParam(resourceType = HostNetworkInterfaceVO.class, required = false)
    private List<String> slaveUuids;

    /**
     * @desc slave names, for querying and filling in slave uuids
     */
    @APIParam(required = false)
    private List<String> slaveNames;

    @APINoSee
    private Map<String, List<String>> slavesMap;

    /**
     * @desc type of vSwitch installed on the host
     */
    @APIParam(validValues = {HostNetworkBondingConstant.LINUX_BONDING_TYPE, HostNetworkBondingConstant.OVS_BONDING_TYPE})
    private String type = "LinuxBonding";

    /**
     * @desc bonding mode. Chosen automatically if not set
     */
    @APIParam(validValues = {HostNetworkBondingConstant.BONDING_MODE_LACP, HostNetworkBondingConstant.BONDING_MODE_AB})
    private String mode = "active-backup";

    /**
     * @desc bonding xmitHashPolicy
     */
    @APIParam(validValues = {HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_TWO, HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_TWO_AND_THREE, HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_THREE_AND_FOUR}, required = false)
    private String xmitHashPolicy;

    /**
     * @desc max length of 2048 characters
     */
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public String getBondingName() {
        return bondingName;
    }

    public void setBondingName(String bondingName) {
        this.bondingName = bondingName;
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

    public Map<String, List<String>> getSlavesMap() {
        return slavesMap;
    }

    public void setSlavesMap(Map<String, List<String>> slavesMap) {
        this.slavesMap = slavesMap;
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

    public static APICreateBondingMsg __example__() {
        APICreateBondingMsg msg = new APICreateBondingMsg();
        msg.setHostUuids(Arrays.asList(uuid()));
        msg.setType(HostNetworkBondingConstant.LINUX_BONDING_TYPE);
        msg.setMode("802.3ad");
        msg.setXmitHashPolicy("layer2+3");
        return msg;
    }
}
