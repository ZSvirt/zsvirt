package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * @api create a l2VlanNetwork
 * @category l2network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l2.APICreateL2VlanNetworkMsg": {
 * "vlan": 10,
 * "name": "TestL2VlanNetwork",
 * "description": "Test",
 * "zoneUuid": "d81c3d3d008e46038b8a38fee595fe41",
 * "physicalInterface": "eth0",
 * "type": "L2VlanNetwork",
 * "session": {
 * "uuid": "8be9f1f0d55b4f1cb6a088c376dc8128"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l2.APICreateL2VlanNetworkMsg": {
 * "vlan": 10,
 * "name": "TestL2VlanNetwork",
 * "description": "Test",
 * "zoneUuid": "d81c3d3d008e46038b8a38fee595fe41",
 * "physicalInterface": "eth0",
 * "type": "L2VlanNetwork",
 * "session": {
 * "uuid": "8be9f1f0d55b4f1cb6a088c376dc8128"
 * },
 * "timeout": 1800000,
 * "id": "a0a5829f12fe4c45855967f6fa0c0afa",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APICreateL2VlanNetworkEvent`
 * @since 0.1.0
 */
@TagResourceType(L2NetworkVO.class)
@RestRequest(
        path = "/l2-networks/vlan",
        method = HttpMethod.POST,
        responseClass = APICreateL2VlanNetworkEvent.class,
        parameterName = "params"
)
public class APICreateL2VlanNetworkMsg extends APICreateL2NetworkMsg {
    /**
     * @desc vlan id
     */
    @APIParam(numberRange = {1, 4094})
    private Integer vlan;

    public int getVlan() {
        return vlan;
    }

    public void setVlan(int vlan) {
        this.vlan = vlan;
    }

    @Override
    public String getType() {
        return L2NetworkConstant.L2_VLAN_NETWORK_TYPE;
    }

    public static APICreateL2VlanNetworkMsg __example__() {
        APICreateL2VlanNetworkMsg msg = new APICreateL2VlanNetworkMsg();

        msg.setName("Test-Net");
        msg.setVlan(10);
        msg.setDescription("Test");
        msg.setZoneUuid(uuid());
        msg.setPhysicalInterface("eth0");

        return msg;
    }
}
