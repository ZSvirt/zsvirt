package org.zstack.header.bonding;

import org.springframework.http.HttpMethod;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/hosts/bondings/{uuid}/detach",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIDetachNicFromBondingEvent.class
)
public class APIDetachNicFromBondingMsg extends APIMessage implements BondingMessage {
    /**
     * @desc uuid of bonding which is going to update
     */
    @APIParam(resourceType = HostNetworkBondingVO.class)
    private String uuid;

    /**
     * @desc slave uuids, see :ref:`HostNetworkInterfaceInventory`
     */
    @APIParam(resourceType = HostNetworkInterfaceVO.class)
    private List<String> slaveUuids;

    /**
     * @desc type of vSwitch installed on the host
     */
    @APIParam(required = false, validValues = {HostNetworkBondingConstant.LINUX_BONDING_TYPE, HostNetworkBondingConstant.OVS_BONDING_TYPE})
    private String type = HostNetworkBondingConstant.LINUX_BONDING_TYPE;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static APIDetachNicFromBondingMsg __example__() {
        APIDetachNicFromBondingMsg msg = new APIDetachNicFromBondingMsg();
        msg.setUuid(uuid());
        msg.setSlaveUuids(Collections.singletonList(uuid()));
        return msg;
    }
}
